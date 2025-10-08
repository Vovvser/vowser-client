package com.vowser.client

import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags

actual suspend fun startPlatformRecording(): Boolean {
    // TODO: iOS 오디오 녹음 구현
    Napier.w("Audio recording not implemented for iOS platform", tag = Tags.MEDIA_RECORDING)
    return false
}

actual suspend fun stopPlatformRecording(): ByteArray? {
    Napier.w("Audio recording not implemented for iOS platform", tag = Tags.MEDIA_RECORDING)
    return null
}

actual fun openUrlInBrowser(url: String) {
    Napier.w("openUrlInBrowser not implemented for iOS platform", tag = Tags.AUTH)
}