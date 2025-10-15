package com.vowser.client.data

import io.github.aakira.napier.Napier
import com.vowser.client.logging.Tags
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

class SpeechRepository(
    private val httpClient: HttpClient,
    backendUrl: String
) {

    private val baseUrl = backendUrl.trimEnd('/')

    suspend fun transcribeAudio(
        audioFileBytes: ByteArray,
        sessionId: String,
        selectedModes: Set<String> = setOf("general")
    ): Result<String> {
        return try {
            val endpoint = "$baseUrl/api/v1/speech/transcribe"

            Napier.i("Sending audio file to backend. Size: ${audioFileBytes.size} bytes, SessionId: $sessionId, Modes: $selectedModes", tag = Tags.NETWORK)

            val response: HttpResponse = httpClient.post(endpoint) {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("audioFile", audioFileBytes, Headers.build {
                            append(HttpHeaders.ContentType, "audio/wav")
                            append(HttpHeaders.ContentDisposition, "filename=\"recording.wav\"")
                        })
                        append("sessionId", sessionId)
                        append("enableGeneralMode", selectedModes.contains("general").toString())
                        append("enableNumberMode", selectedModes.contains("number").toString())
                        append("enableAlphabetMode", selectedModes.contains("alphabet").toString())
                        append("enableSnippetMode", selectedModes.contains("snippet").toString())
                    }
                ))
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val responseText = response.bodyAsText()
                    Napier.i("Audio transcription successful: $responseText", tag = Tags.NETWORK)
                    Result.success(responseText)
                }
                else -> {
                    val errorText = response.bodyAsText()
                    Napier.e("Audio transcription failed: ${response.status} - $errorText", tag = Tags.NETWORK)
                    Result.failure(Exception("HTTP ${response.status}: $errorText"))
                }
            }

        } catch (e: Exception) {
            Napier.e("Failed to transcribe audio: ${e.message}", tag = Tags.NETWORK)
            Result.failure(e)
        }
    }

    suspend fun uploadAudioForProcessing(audioFileBytes: ByteArray, sessionId: String): Result<String> {
        return try {
            val endpoint = "$baseUrl/api/v1/speech/process"
            Napier.i("Uploading audio for processing. Size: ${audioFileBytes.size} bytes, SessionId: $sessionId", tag = Tags.NETWORK)

            val response: HttpResponse = httpClient.post(endpoint) {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("audioFile", audioFileBytes, Headers.build {
                            append(HttpHeaders.ContentType, "audio/wav")
                            append(HttpHeaders.ContentDisposition, "filename=\"recording.wav\"")
                        })
                        append("sessionId", sessionId)
                    }
                ))
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val responseText = response.bodyAsText()
                    Napier.i("Audio processing successful: $responseText", tag = Tags.NETWORK)
                    Result.success(responseText)
                }
                else -> {
                    val errorText = response.bodyAsText()
                    Napier.e("Audio processing failed: ${response.status} - $errorText", tag = Tags.NETWORK)
                    Result.failure(Exception("HTTP ${response.status}: $errorText"))
                }
            }

        } catch (e: Exception) {
            Napier.e("Failed to process audio: ${e.message}", tag = Tags.NETWORK)
            Result.failure(e)
        }
    }

    fun close() {
        // HttpClient는 외부에서 관리합니다.
    }
}
