package com.vowser.client

import io.github.aakira.napier.Napier

actual suspend fun startPlatformRecording(): Boolean {
    Napier.w("Audio recording not implemented for Android platform", tag = "AndroidRecording")
    return false
}

actual suspend fun stopPlatformRecording(): ByteArray? {
    Napier.w("Audio recording not implemented for Android platform", tag = "AndroidRecording")
    return null
}