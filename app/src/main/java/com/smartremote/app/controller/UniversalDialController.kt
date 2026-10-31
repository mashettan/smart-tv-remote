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

/**
 * Universal DIAL (Discovery and Launch) controller.
 * Supported by Google TV, Android TV, Samsung, LG, Sony, Philips, and other Smart TVs
 * to directly launch apps like YouTube and Netflix over Wi-Fi without proprietary pairing.
 */
class UniversalDialController(override val device: TvDevice) : TvController {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val dialBaseUrl: String
        get() {
            return if (!device.dialLocation.isNullOrBlank()) {
                device.dialLocation.removeSuffix("/")
            } else {
                "http://${device.ipAddress}:${device.port}/apps"
            }
        }

    private val emptyBody = "".toRequestBody("text/plain".toMediaType())

    override suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(dialBaseUrl)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 404
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendKey(key: RemoteKey): Result<Unit> = withContext(Dispatchers.IO) {
        // DIAL protocol is primarily an application launcher; for navigation keys,
        // it requires the target TV-specific protocol (e.g. Android TV Remote or TV WebSockets).
        Result.failure(UnsupportedOperationException("DIAL supports app launching; use TV-specific controller for D-pad navigation."))
    }

    override suspend fun launchApp(app: AppShortcut): Result<Unit> = withContext(Dispatchers.IO) {
        val appName = when (app) {
            AppShortcut.YOUTUBE -> "YouTube"
            AppShortcut.NETFLIX -> "Netflix"
            AppShortcut.PRIME_VIDEO -> "AmazonInstantVideo"
            AppShortcut.SPOTIFY -> "Spotify"
        }

        try {
            val url = if (dialBaseUrl.endsWith("/apps")) {
                "$dialBaseUrl/$appName"
            } else {
                "$dialBaseUrl/apps/$appName"
            }

            val request = Request.Builder()
                .url(url)
                .post(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 201) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("DIAL launch $appName returned HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
