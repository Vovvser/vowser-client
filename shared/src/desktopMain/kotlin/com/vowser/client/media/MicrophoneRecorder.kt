package com.vowser.client.media

import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.*

class MicrophoneRecorder {
    private var targetDataLine: TargetDataLine? = null
    private var audioFile: File? = null
    private var recordingThread: Thread? = null
    private var audioOutputStream: ByteArrayOutputStream? = null
    private val audioFormat = AudioFormat(16000f, 16, 1, true, false)
    private val isRecording = AtomicBoolean(false)

    fun startRecording(): Boolean {
        if (!isRecording.compareAndSet(false, true)) {
            Napier.w("Already recording", tag = Tags.MEDIA_RECORDING)
            return false
        }

        return try {
            val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
            if (!AudioSystem.isLineSupported(info)) {
                Napier.e("Audio line not supported", tag = Tags.MEDIA_RECORDING)
                isRecording.set(false)
                false
            } else {
                val line = AudioSystem.getLine(info) as TargetDataLine
                line.open(audioFormat)
                line.start()
                targetDataLine = line

                audioOutputStream = ByteArrayOutputStream(768 * 1024)
                audioFile = File.createTempFile("vowser_recording_", ".wav")

                recordingThread = Thread {
                    val buf = ByteArray(8 * 1024)
                    try {
                        while (isRecording.get()) {
                            val bytesRead = line.read(buf, 0, buf.size)
                            if (bytesRead > 0) {
                                audioOutputStream?.write(buf, 0, bytesRead)
                            } else if (bytesRead < 0) {
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Napier.e("Recording loop error: ${e.message}", e, tag = Tags.MEDIA_RECORDING)
                    }
                }.also { it.isDaemon = true; it.start() }

                Napier.i("Recording started to ${audioFile?.absolutePath}", tag = Tags.MEDIA_RECORDING)
                true
            }
        } catch (e: Exception) {
            Napier.e("Failed to start recording: ${e.message}", e, tag = Tags.MEDIA_RECORDING)
            safeCloseLine()
            audioOutputStream = null
            audioFile = null
            isRecording.set(false)
            false
        }
    }

    fun stopRecording(timeoutMs: Long = 1500): File? {
        if (!isRecording.compareAndSet(true, false)) {
            Napier.w("Not currently recording", tag = Tags.MEDIA_RECORDING)
            return null
        }

        try {
            targetDataLine?.stop()
            targetDataLine?.close()

            recordingThread?.join(timeoutMs)

            val recordedBytesNullable = audioOutputStream?.toByteArray()
            val outFile = audioFile

            if (recordedBytesNullable == null || recordedBytesNullable.isEmpty() || outFile == null) {
                Napier.w("Recorded audio is empty or file missing.", tag = Tags.MEDIA_RECORDING)
                return null
            }

            val recordedBytes: ByteArray = recordedBytesNullable
            val frameLength = recordedBytes.size.toLong() / audioFormat.frameSize
            val ais = AudioInputStream(ByteArrayInputStream(recordedBytes), audioFormat, frameLength)

            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outFile)
            Napier.i(
                "Recording stopped. File: ${outFile.absolutePath}, Size: ${recordedBytes.size} bytes",
                tag = Tags.MEDIA_RECORDING
            )
            return outFile
        } catch (e: Exception) {
            Napier.e("Error stopping recording: ${e.message}", e, tag = Tags.MEDIA_RECORDING)
            return null
        } finally {
            audioOutputStream?.close()
            audioOutputStream = null
            recordingThread = null
            targetDataLine = null
        }
    }

    private fun safeCloseLine() {
        try {
            targetDataLine?.stop()
        } catch (_: Exception) {}
        try {
            targetDataLine?.close()
        } catch (_: Exception) {}
        targetDataLine = null
    }
}