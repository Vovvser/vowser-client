package com.vowser.client

import com.vowser.client.media.MicrophoneRecorder
import io.github.aakira.napier.Napier

private object DesktopRecorderManager {
    val recorder = MicrophoneRecorder()
}

actual suspend fun startPlatformRecording(): Boolean {
    return try {
        val success = DesktopRecorderManager.recorder.startRecording()
        if (success) {
            Napier.i("Desktop recording started", tag = "DesktopRecording")
        } else {
            Napier.e("Failed to start desktop recording", tag = "DesktopRecording")
        }
        success
    } catch (e: Exception) {
        Napier.e("Error starting desktop recording: ${e.message}", e, tag = "DesktopRecording")
        false
    }
}

actual suspend fun stopPlatformRecording(): ByteArray? {
    return try {
        val audioFile = DesktopRecorderManager.recorder.stopRecording()
        if (audioFile != null && audioFile.exists()) {
            val audioBytes = audioFile.readBytes()
            audioFile.delete()
            Napier.i("Desktop recording stopped. Audio size: ${audioBytes.size} bytes", tag = "DesktopRecording")
            audioBytes
        } else {
            Napier.w("No audio file found after stopping recording", tag = "DesktopRecording")
            null
        }
    } catch (e: Exception) {
        Napier.e("Error stopping desktop recording: ${e.message}", e, tag = "DesktopRecording")
        null
    }
}