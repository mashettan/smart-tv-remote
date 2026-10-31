package com.smartremote.app.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.smartremote.app.model.TvBrand
import com.smartremote.app.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class TvDiscoveryManager(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    suspend fun discoverDevices(): List<TvDevice> = withContext(Dispatchers.IO) {
        val foundDevices = mutableMapOf<String, TvDevice>()

        // 1. Acquire Multicast Lock if available
        var multicastLock: WifiManager.MulticastLock? = null
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("smart_tv_remote_lock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Exception) {}

        // 2. Perform SSDP Discovery
        try {
            val ssdpResults = runSsdpSearch()
            ssdpResults.forEach { device ->
                foundDevices[device.ipAddress] = device
            }
        } catch (_: Exception) {}

        // 3. Fallback: Quick Subnet Probe if nothing found or to complement
        if (foundDevices.isEmpty()) {
            val probed = probeLocalSubnet()
            probed.forEach { device ->
                foundDevices[device.ipAddress] = device
            }
        }

        try {
            multicastLock?.release()
        } catch (_: Exception) {}

        foundDevices.values.toList()
    }

    private fun runSsdpSearch(): List<TvDevice> {
        val devices = mutableListOf<TvDevice>()
        val multicastIp = "239.255.255.250"
        val port = 1900

        val queries = listOf(
            "M-SEARCH * HTTP/1.1\r\nHOST: $multicastIp:$port\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: roku:ecp\r\n\r\n",
            "M-SEARCH * HTTP/1.1\r\nHOST: $multicastIp:$port\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: urn:dial-multiscreen-org:service:dial:1\r\n\r\n",
            "M-SEARCH * HTTP/1.1\r\nHOST: $multicastIp:$port\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n"
        )

        val socket = DatagramSocket()
        socket.soTimeout = 2500

        try {
            val group = InetAddress.getByName(multicastIp)
            for (query in queries) {
                val data = query.toByteArray()
                val packet = DatagramPacket(data, data.size, group, port)
                socket.send(packet)
            }

            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 2500) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val ip = receivePacket.address.hostAddress ?: continue

                    val device = parseSsdpResponse(ip, response)
                    if (device != null && devices.none { it.ipAddress == device.ipAddress }) {
                        devices.add(device)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        } finally {
            socket.close()
        }

        return devices
    }

    private fun parseSsdpResponse(ip: String, response: String): TvDevice? {
        val lines = response.lines()
        val headers = mutableMapOf<String, String>()
        for (line in lines) {
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                headers[parts[0].trim().uppercase()] = parts[1].trim()
            }
        }

        val location = headers["LOCATION"] ?: ""
        val st = headers["ST"] ?: ""
        val server = headers["SERVER"] ?: ""

        return when {
            st.contains("roku:ecp", ignoreCase = true) || server.contains("Roku", ignoreCase = true) -> {
                TvDevice(
                    name = "Roku TV",
                    ipAddress = ip,
                    port = 8060,
                    brand = TvBrand.ROKU,
                    dialLocation = location
                )
            }
            server.contains("WebOS", ignoreCase = true) -> {
                TvDevice(
                    name = "LG webOS TV",
                    ipAddress = ip,
                    port = 3000,
                    brand = TvBrand.LG_WEBOS,
                    dialLocation = location
                )
            }
            server.contains("Tizen", ignoreCase = true) || server.contains("Samsung", ignoreCase = true) -> {
                TvDevice(
                    name = "Samsung Smart TV",
                    ipAddress = ip,
                    port = 8001,
                    brand = TvBrand.SAMSUNG_TIZEN,
                    dialLocation = location
                )
            }
            st.contains("dial", ignoreCase = true) || location.contains("8008") -> {
                TvDevice(
                    name = "Smart TV (DIAL)",
                    ipAddress = ip,
                    port = 8008,
                    brand = TvBrand.UNIVERSAL_DIAL,
                    dialLocation = location
                )
            }
            else -> null
        }
    }

    private suspend fun probeLocalSubnet(): List<TvDevice> = withContext(Dispatchers.IO) {
        val found = mutableListOf<TvDevice>()
        val baseSubnet = getLocalSubnetPrefix() ?: return@withContext emptyList()

        // Scan common IPs on local subnet for ports 8060 (Roku), 8008 (DIAL/Android TV), 8001 (Samsung), 3000 (LG)
        val candidateIps = (1..254).map { "$baseSubnet.$it" }

        val jobs = candidateIps.map { ip ->
            async {
                probeIp(ip)
            }
        }

        val results = jobs.awaitAll().filterNotNull()
        found.addAll(results)
        found
    }

    private fun probeIp(ip: String): TvDevice? {
        // Quick probe port 8060 (Roku)
        if (isPortOpen(ip, 8060, 200)) {
            return TvDevice("Roku TV ($ip)", ip, 8060, TvBrand.ROKU)
        }
        // Quick probe port 8008 (DIAL / Android TV / Google TV / Chromecast)
        if (isPortOpen(ip, 8008, 200)) {
            return TvDevice("Android TV / Google TV ($ip)", ip, 8008, TvBrand.ANDROID_TV)
        }
        // Quick probe port 8001 (Samsung Tizen)
        if (isPortOpen(ip, 8001, 200)) {
            return TvDevice("Samsung Smart TV ($ip)", ip, 8001, TvBrand.SAMSUNG_TIZEN)
        }
        // Quick probe port 3000 (LG webOS)
        if (isPortOpen(ip, 3000, 200)) {
            return TvDevice("LG webOS TV ($ip)", ip, 3000, TvBrand.LG_WEBOS)
        }
        return null
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalSubnetPrefix(): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val ip = wifi.connectionInfo?.ipAddress ?: 0
        if (ip == 0) return null

        val ipStr = String.format(
            "%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff
        )
        return ipStr
    }
}
