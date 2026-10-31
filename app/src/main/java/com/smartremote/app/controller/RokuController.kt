package com.smartremote.app.controller

import com.smartremote.app.model.AppShortcut
import com.smartremote.app.model.RemoteKey
import com.smartremote.app.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class RokuController(override val device: TvDevice) : TvController {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "http://${device.ipAddress}:${device.port}"
    private val emptyBody = "".toRequestBody("text/plain".toMediaType())

    override suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/query/device-info")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendKey(key: RemoteKey): Result<Unit> = withContext(Dispatchers.IO) {
        val rokuKey = when (key) {
            RemoteKey.POWER -> "Power"
            RemoteKey.HOME -> "Home"
            RemoteKey.BACK -> "Back"
            RemoteKey.UP -> "Up"
            RemoteKey.DOWN -> "Down"
            RemoteKey.LEFT -> "Left"
            RemoteKey.RIGHT -> "Right"
            RemoteKey.SELECT -> "Select"
            RemoteKey.VOL_UP -> "VolumeUp"
            RemoteKey.VOL_DOWN -> "VolumeDown"
            RemoteKey.MUTE -> "VolumeMute"
            RemoteKey.PLAY_PAUSE -> "Play"
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/keypress/$rokuKey")
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun launchApp(app: AppShortcut): Result<Unit> = withContext(Dispatchers.IO) {
        // Known Roku channel IDs:
        // YouTube: 837, Netflix: 12, Prime Video: 13, Spotify: 22297
        val channelId = when (app) {
            AppShortcut.YOUTUBE -> "837"
            AppShortcut.NETFLIX -> "12"
            AppShortcut.PRIME_VIDEO -> "13"
            AppShortcut.SPOTIFY -> "22297"
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/launch/$channelId")
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("HTTP ${response.code} launching $channelId"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
