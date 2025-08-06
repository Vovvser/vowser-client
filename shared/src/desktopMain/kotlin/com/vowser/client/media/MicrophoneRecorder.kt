package com.vowser.client.media

import io.github.aakira.napier.Napier
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
            Napier.w("Already recording", tag = "MicrophoneRecorder")
            return false
        }

        try {
            val format = AudioFormat(16000f, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)

            if (!AudioSystem.isLineSupported(info)) {
                Napier.e("Audio line not supported", tag = "MicrophoneRecorder")
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
                    Napier.e("Error during recording: ${e.message}", e, tag = "MicrophoneRecorder")
                }
            }

            Napier.i("Recording started to ${audioFile?.absolutePath}", tag = "MicrophoneRecorder")
            return true

        } catch (e: Exception) {
            Napier.e("Failed to start recording: ${e.message}", e, tag = "MicrophoneRecorder")
            isRecording = false
            return false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) {
            Napier.w("Not currently recording", tag = "MicrophoneRecorder")
            return null
        }

        try {
            isRecording = false
            
            targetDataLine?.stop()
            targetDataLine?.close()
            
            // 녹음 스레드가 완료될 때까지 잠시 대기
            recordingThread?.join(1000) // 최대 1초 대기
            
            Napier.i("Recording stopped. File: ${audioFile?.absolutePath}", tag = "MicrophoneRecorder")
            return audioFile
            
        } catch (e: Exception) {
            Napier.e("Error stopping recording: ${e.message}", e, tag = "MicrophoneRecorder")
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