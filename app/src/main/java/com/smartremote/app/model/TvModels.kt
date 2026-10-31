package com.smartremote.app.model

enum class TvBrand(val displayName: String) {
    ROKU("Roku TV"),
    ANDROID_TV("Android TV / Google TV"),
    LG_WEBOS("LG webOS TV"),
    SAMSUNG_TIZEN("Samsung Tizen TV"),
    UNIVERSAL_DIAL("Universal DIAL TV")
}

enum class RemoteKey {
    POWER,
    HOME,
    BACK,
    UP,
    DOWN,
    LEFT,
    RIGHT,
    SELECT,
    VOL_UP,
    VOL_DOWN,
    MUTE,
    PLAY_PAUSE
}

enum class AppShortcut(val displayName: String, val colorHex: Long) {
    YOUTUBE("YouTube", 0xFFFF0000),
    NETFLIX("Netflix", 0xFFE50914),
    PRIME_VIDEO("Prime Video", 0xFF00A8E1),
    SPOTIFY("Spotify", 0xFF1DB954)
}

data class TvDevice(
    val name: String,
    val ipAddress: String,
    val port: Int,
    val brand: TvBrand,
    val modelName: String = "",
    val dialLocation: String? = null
) {
    val displayTitle: String
        get() = if (name.isNotBlank()) name else "${brand.displayName} ($ipAddress)"
}
