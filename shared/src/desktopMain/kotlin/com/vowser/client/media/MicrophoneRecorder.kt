package com.vowser.client.media

import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags
import java.io.File
import javax.sound.sampled.*
import kotlin.concurrent.thread

class MicrophoneRecorder {
    private var targetDataLine: TargetDataLine? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    fun startRecording(): Boolean {
        if (isRecording) {
            VowserLogger.warn("Already recording", Tags.MEDIA_RECORDING)
            return false
        }

        try {
            val format = AudioFormat(16000f, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)

            if (!AudioSystem.isLineSupported(info)) {
                VowserLogger.error("Audio line not supported", Tags.MEDIA_RECORDING)
                return false
            }

            targetDataLine = AudioSystem.getLine(info) as TargetDataLine
            targetDataLine?.open(format)
            targetDataLine?.start()

            // 임시 파일 생성
            audioFile = File.createTempFile("vowser_recording_", ".wav")
            
            isRecording = true

            // 별도 스레드에서 녹음 실행
            recordingThread = thread(start = true) {
                try {
                    targetDataLine?.let { line ->
                        audioFile?.let { file ->
                            AudioSystem.write(
                                AudioInputStream(line), 
                                AudioFileFormat.Type.WAVE, 
                                file
                            )
                        }
                    }
                } catch (e: Exception) {
                    VowserLogger.error("Error during recording: ${e.message}", Tags.MEDIA_RECORDING, e)
                }
            }

            VowserLogger.info("Recording started to ${audioFile?.absolutePath}", Tags.MEDIA_RECORDING)
            return true

        } catch (e: Exception) {
            VowserLogger.error("Failed to start recording: ${e.message}", Tags.MEDIA_RECORDING, e)
            isRecording = false
            return false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) {
            VowserLogger.warn("Not currently recording", Tags.MEDIA_RECORDING)
            return null
        }

        try {
            isRecording = false
            
            targetDataLine?.stop()
            targetDataLine?.close()
            
            // 녹음 스레드가 완료될 때까지 잠시 대기
            recordingThread?.join(1000) // 최대 1초 대기
            
            VowserLogger.info("Recording stopped. File: ${audioFile?.absolutePath}", Tags.MEDIA_RECORDING)
            return audioFile
            
        } catch (e: Exception) {
            VowserLogger.error("Error stopping recording: ${e.message}", Tags.MEDIA_RECORDING, e)
            return null
        }
    }

    fun isRecording(): Boolean = isRecording

    fun cleanup() {
        if (isRecording) {
            stopRecording()
        }
        audioFile?.delete()
        audioFile = null
    }
}