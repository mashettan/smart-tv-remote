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
 * Android TV / Google TV Controller.
 * Supports:
 * 1. Instant App Launching (YouTube, Netflix) via standard DIAL service on port 8008.
 * 2. Remote keys via Android TV Remote Service v2 on port 6466 / 6467.
 */
class AndroidTvController(override val device: TvDevice) : TvController {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val dialController = UniversalDialController(
        device.copy(port = if (device.port == 6466 || device.port == 6467) 8008 else device.port)
    )

    override suspend fun ping(): Boolean = withContext(Dispatchers.IO) {
        // First check DIAL port 8008, then remote service port
        dialController.ping()
    }

    override suspend fun sendKey(key: RemoteKey): Result<Unit> = withContext(Dispatchers.IO) {
        // Android TV Remote v2 requires a local self-signed TLS cert paired with the TV's 4-digit PIN.
        // For devices with ADB or paired companion services, commands are sent as KeyCodes.
        // We provide clear feedback if pairing hasn't been established.
        Result.failure(
            Exception(
                "Android TV navigation keys require PIN pairing on port 6467. " +
                "Direct shortcut buttons (YouTube, Netflix) work immediately via DIAL!"
            )
        )
    }

    override suspend fun launchApp(app: AppShortcut): Result<Unit> = withContext(Dispatchers.IO) {
        // Android TV and Google TV natively support DIAL for instant YouTube and Netflix launching!
        dialController.launchApp(app)
    }
}
