package com.localmusic.player.settings

data class OpenSourceLicense(
    val name: String,
    val license: String,
    val url: String
)

object OpenSourceLicenses {
    val dependencies = listOf(
        OpenSourceLicense("AndroidX", "Apache-2.0", "https://developer.android.com/jetpack/androidx"),
        OpenSourceLicense("Kotlin", "Apache-2.0", "https://kotlinlang.org"),
        OpenSourceLicense("Media3", "Apache-2.0", "https://developer.android.com/media/media3"),
        OpenSourceLicense("Room", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/room")
    )
}