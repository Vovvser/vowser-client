package com.vowser.client.data

import com.vowser.client.logging.VowserLogger
import com.vowser.client.logging.Tags
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SpeechRepository(private val httpClient: HttpClient? = null) {
    
    private val client = httpClient ?: HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun transcribeAudio(
        audioFileBytes: ByteArray,
        sessionId: String,
        selectedModes: Set<String> = setOf("general")
    ): Result<String> {
        return try {
            val backendUrl = "http://localhost:8080/api/v1/speech/transcribe"

            VowserLogger.info("Sending audio file to backend. Size: ${audioFileBytes.size} bytes, SessionId: $sessionId, Modes: $selectedModes", Tags.NETWORK)

            val response: HttpResponse = client.post(backendUrl) {
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
                    VowserLogger.info("Audio transcription successful: $responseText", Tags.NETWORK)
                    Result.success(responseText)
                }
                else -> {
                    val errorText = response.bodyAsText()
                    VowserLogger.error("Audio transcription failed: ${response.status} - $errorText", Tags.NETWORK)
                    Result.failure(Exception("HTTP ${response.status}: $errorText"))
                }
            }

        } catch (e: Exception) {
            VowserLogger.error("Failed to transcribe audio: ${e.message}", Tags.NETWORK)
            Result.failure(e)
        }
    }

    suspend fun uploadAudioForProcessing(audioFileBytes: ByteArray, sessionId: String): Result<String> {
        return try {
            val backendUrl = "http://localhost:8080/api/v1/speech/process"
            
            VowserLogger.info("Uploading audio for processing. Size: ${audioFileBytes.size} bytes, SessionId: $sessionId", Tags.NETWORK)

            val response: HttpResponse = client.post(backendUrl) {
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
                    VowserLogger.info("Audio processing successful: $responseText", Tags.NETWORK)
                    Result.success(responseText)
                }
                else -> {
                    val errorText = response.bodyAsText()
                    VowserLogger.error("Audio processing failed: ${response.status} - $errorText", Tags.NETWORK)
                    Result.failure(Exception("HTTP ${response.status}: $errorText"))
                }
            }

        } catch (e: Exception) {
            VowserLogger.error("Failed to process audio: ${e.message}", Tags.NETWORK)
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}