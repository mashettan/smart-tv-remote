package com.smartremote.app.controller

import android.util.Base64
import com.smartremote.app.model.AppShortcut
import com.smartremote.app.model.RemoteKey
import com.smartremote.app.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SamsungTizenController(override val device: TvDevice) : TvController {

    private val client = OkHttpClient.Builder()
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private fun connectWebSocket() {
        if (webSocket != null) return

        val appNameBase64 = Base64.encodeToString("SmartRemote".toByteArray(), Base64.NO_WRAP)
        val url = "ws://${device.ipAddress}:${device.port}/api/v2/channels/samsung.remote.control?name=$appNameBase64"

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@SamsungTizenController.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@SamsungTizenController.webSocket = null
            }
        })
    }

    override suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("http://${device.ipAddress}:8001/api/v2/")
                .get()
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendKey(key: RemoteKey): Result<Unit> = withContext(Dispatchers.IO) {
        connectWebSocket()
        val ws = webSocket ?: return@withContext Result.failure(Exception("WebSocket not connected to Samsung TV"))

        val samsungKey = when (key) {
            RemoteKey.POWER -> "KEY_POWER"
            RemoteKey.HOME -> "KEY_HOME"
            RemoteKey.BACK -> "KEY_RETURN"
            RemoteKey.UP -> "KEY_UP"
            RemoteKey.DOWN -> "KEY_DOWN"
            RemoteKey.LEFT -> "KEY_LEFT"
            RemoteKey.RIGHT -> "KEY_RIGHT"
            RemoteKey.SELECT -> "KEY_ENTER"
            RemoteKey.VOL_UP -> "KEY_VOLUP"
            RemoteKey.VOL_DOWN -> "KEY_VOLDOWN"
            RemoteKey.MUTE -> "KEY_MUTE"
            RemoteKey.PLAY_PAUSE -> "KEY_PLAY"
        }

        val json = JSONObject().apply {
            put("method", "ms.remote.control")
            put("params", JSONObject().apply {
                put("Cmd", "Click")
                put("DataOfCmd", samsungKey)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            })
        }

        val sent = ws.send(json.toString())
        if (sent) Result.success(Unit) else Result.failure(Exception("Failed to send key to Samsung TV"))
    }

    override suspend fun launchApp(app: AppShortcut): Result<Unit> = withContext(Dispatchers.IO) {
        // Samsung Tizen App IDs:
        // YouTube: 111299001912, Netflix: 3201512006785, Prime Video: 3201512006785 (or DIAL)
        val appId = when (app) {
            AppShortcut.YOUTUBE -> "111299001912"
            AppShortcut.NETFLIX -> "3201512006785"
            AppShortcut.PRIME_VIDEO -> "3201512006785"
            AppShortcut.SPOTIFY -> "3201606009684"
        }

        try {
            val emptyBody = "".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("http://${device.ipAddress}:8001/api/v2/applications/$appId")
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201) {
                    Result.success(Unit)
                } else {
                    // Fallback to DIAL
                    UniversalDialController(device).launchApp(app)
                }
            }
        } catch (e: Exception) {
            // Fallback to DIAL
            UniversalDialController(device).launchApp(app)
        }
    }

    override suspend fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
