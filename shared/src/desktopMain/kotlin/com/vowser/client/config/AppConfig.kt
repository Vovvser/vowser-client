package com.vowser.client.config

import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * 애플리케이션 설정 관리
 */
class AppConfig private constructor() {
    private val properties = Properties()

    val backendUrl: String
        get() = properties.getProperty("backend.url", DEFAULT_BACKEND_URL)

    companion object {
        private const val DEFAULT_BACKEND_URL = "http://localhost:8080"
        private const val CONFIG_FILENAME = "config.properties"

        @Volatile
        private var instance: AppConfig? = null

        fun getInstance(): AppConfig {
            return instance ?: synchronized(this) {
                instance ?: AppConfig().also {
                    it.loadConfig()
                    instance = it
                }
            }
        }
    }

    private fun loadConfig() {
        try {
            // 1. 현재 디렉토리에서 찾기
            val currentDirConfig = File(CONFIG_FILENAME)
            if (currentDirConfig.exists()) {
                FileInputStream(currentDirConfig).use { properties.load(it) }
                println("AppConfig: Loaded from current directory: ${currentDirConfig.absolutePath}")
                return
            }

            // 2. resources 폴더에서 찾기
            val resourceStream = javaClass.classLoader.getResourceAsStream(CONFIG_FILENAME)
            if (resourceStream != null) {
                resourceStream.use { properties.load(it) }
                println("AppConfig: Loaded from resources")
                return
            }

            // 3. 설정 파일이 없으면 기본값 사용
            println("AppConfig: No config file found, using defaults (backend.url=$DEFAULT_BACKEND_URL)")
        } catch (e: Exception) {
            println("AppConfig: Error loading config: ${e.message}, using defaults")
            e.printStackTrace()
        }
    }

    fun printConfig() {
        println("=== AppConfig ===")
        println("backend.url: $backendUrl")
        println("=================")
    }
}
