package com.smartremote.app.controller

import com.smartremote.app.model.AppShortcut
import com.smartremote.app.model.RemoteKey
import com.smartremote.app.model.TvBrand
import com.smartremote.app.model.TvDevice

interface TvController {
    val device: TvDevice
    suspend fun ping(): Boolean
    suspend fun sendKey(key: RemoteKey): Result<Unit>
    suspend fun launchApp(app: AppShortcut): Result<Unit>
    suspend fun disconnect() {}

    companion object {
        fun create(device: TvDevice): TvController {
            return when (device.brand) {
                TvBrand.ROKU -> RokuController(device)
                TvBrand.LG_WEBOS -> LgWebOsController(device)
                TvBrand.SAMSUNG_TIZEN -> SamsungTizenController(device)
                TvBrand.ANDROID_TV -> AndroidTvController(device)
                TvBrand.UNIVERSAL_DIAL -> UniversalDialController(device)
            }
        }
    }
}
