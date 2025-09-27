package com.vowser.client

import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags

actual suspend fun startPlatformRecording(): Boolean {
    VowserLogger.warn("Audio recording not implemented for Android platform", Tags.MEDIA_RECORDING)
    return false
}

actual suspend fun stopPlatformRecording(): ByteArray? {
    VowserLogger.warn("Audio recording not implemented for Android platform", Tags.MEDIA_RECORDING)
    return null
}