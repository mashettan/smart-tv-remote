package com.smartremote.app.controller

import com.smartremote.app.model.AppShortcut
import com.smartremote.app.model.RemoteKey
import com.smartremote.app.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LgWebOsController(override val device: TvDevice) : TvController {

    private val client = OkHttpClient.Builder()
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val commandId = AtomicInteger(1)
    private var isPaired = false

    private fun connectWebSocket() {
        if (webSocket != null) return

        val request = Request.Builder()
            .url("ws://${device.ipAddress}:${device.port}")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Send pairing registration handshake
                val registerPayload = JSONObject().apply {
                    put("type", "register")
                    put("id", "reg_1")
                    put("payload", JSONObject().apply {
                        put("forcePairing", false)
                        put("pairingType", "PROMPT")
                        put("manifest", JSONObject().apply {
                            put("manifestVersion", 1)
                            put("appVersion", "1.0")
                            put("signed", JSONObject().apply {
                                put("created", "20240101")
                                put("appId", "com.smartremote.app")
                                put("vendorId", "com.smartremote")
                                put("localizedAppNames", JSONObject().apply {
                                    put("", "Smart TV Remote")
                                })
                                put("permissions", org.json.JSONArray(listOf(
                                    "LAUNCH", "CONTROL_AUDIO", "CONTROL_POWER", "READ_INSTALLED_APPS"
                                )))
                            })
                        })
                    })
                }
                webSocket.send(registerPayload.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("registered")) {
                    isPaired = true
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@LgWebOsController.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@LgWebOsController.webSocket = null
            }
        })
    }

    override suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            connectWebSocket()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sendSsapCommand(uri: String, payload: JSONObject = JSONObject()): Boolean {
        connectWebSocket()
        val ws = webSocket ?: return false
        val id = commandId.getAndIncrement()

        val json = JSONObject().apply {
            put("type", "request")
            put("id", "req_$id")
            put("uri", uri)
            put("payload", payload)
        }
        return ws.send(json.toString())
    }

    override suspend fun sendKey(key: RemoteKey): Result<Unit> = withContext(Dispatchers.IO) {
        val success = when (key) {
            RemoteKey.VOL_UP -> sendSsapCommand("ssap://audio/volumeUp")
            RemoteKey.VOL_DOWN -> sendSsapCommand("ssap://audio/volumeDown")
            RemoteKey.MUTE -> sendSsapCommand("ssap://audio/setMute", JSONObject().put("mute", true))
            RemoteKey.POWER -> sendSsapCommand("ssap://system/turnOff")
            RemoteKey.HOME -> sendSsapCommand("ssap://system.launcher/open", JSONObject().put("target", "home"))
            RemoteKey.BACK -> sendSsapCommand("ssap://system.launcher/open", JSONObject().put("target", "back"))
            RemoteKey.PLAY_PAUSE -> sendSsapCommand("ssap://media.controls/play")
            else -> {
                // LG requires a special pointer input socket for D-Pad, or fallback
                sendSsapCommand("ssap://system.launcher/open")
            }
        }
        if (success) Result.success(Unit) else Result.failure(Exception("Failed to send key to LG TV"))
    }

    override suspend fun launchApp(app: AppShortcut): Result<Unit> = withContext(Dispatchers.IO) {
        val appId = when (app) {
            AppShortcut.YOUTUBE -> "youtube.leanback.v4"
            AppShortcut.NETFLIX -> "netflix"
            AppShortcut.PRIME_VIDEO -> "amazon"
            AppShortcut.SPOTIFY -> "spotify-beehive"
        }

        val success = sendSsapCommand("ssap://system.launcher/launch", JSONObject().put("id", appId))
        if (success) Result.success(Unit) else Result.failure(Exception("Failed to launch $appId on LG TV"))
    }

    override suspend fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
