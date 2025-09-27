package com.vowser.client

import com.vowser.client.media.MicrophoneRecorder
import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags

private object DesktopRecorderManager {
    val recorder = MicrophoneRecorder()
}

actual suspend fun startPlatformRecording(): Boolean {
    return try {
        val success = DesktopRecorderManager.recorder.startRecording()
        if (success) {
            VowserLogger.info("Desktop recording started", Tags.MEDIA_RECORDING)
        } else {
            VowserLogger.error("Failed to start desktop recording", Tags.MEDIA_RECORDING)
        }
        success
    } catch (e: Exception) {
        VowserLogger.error("Error starting desktop recording: ${e.message}", Tags.MEDIA_RECORDING, e)
        false
    }
}

actual suspend fun stopPlatformRecording(): ByteArray? {
    return try {
        val audioFile = DesktopRecorderManager.recorder.stopRecording()
        if (audioFile != null && audioFile.exists()) {
            val audioBytes = audioFile.readBytes()
            audioFile.delete()
            VowserLogger.info("Desktop recording stopped. Audio size: ${audioBytes.size} bytes", Tags.MEDIA_RECORDING)
            audioBytes
        } else {
            VowserLogger.warn("No audio file found after stopping recording", Tags.MEDIA_RECORDING)
            null
        }
    } catch (e: Exception) {
        VowserLogger.error("Error stopping desktop recording: ${e.message}", Tags.MEDIA_RECORDING, e)
        null
    }
}