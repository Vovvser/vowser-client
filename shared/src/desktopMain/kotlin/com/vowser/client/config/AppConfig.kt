package com.vowser.client.config

import com.vowser.client.logging.Tags
import io.github.aakira.napier.Napier
import java.io.File
import java.io.FileInputStream
import java.util.Properties
class AppConfig private constructor() {
    private val properties = Properties()

    val backendUrl: String
        get() = properties.getProperty("backend.url", DEFAULT_BACKEND_URL)

    val browserZoom: Double
        get() = properties.getProperty("browser.zoom")
            ?.toDoubleOrNull()
            ?.let { it.coerceIn(0.25, 3.0) }
            ?: 1.0

    val chromiumDeviceScaleFactor: Double
        get() = properties.getProperty("chromium.deviceScaleFactor")
            ?.toDoubleOrNull()
            ?.takeIf { it > 0 } ?: 1.0

    val chromiumStartFullscreen: Boolean
        get() = properties.getProperty("chromium.startFullscreen")?.toBoolean() ?: false

    val browserFitToWindow: Boolean
        get() = properties.getProperty("browser.fitToWindow")?.toBoolean() ?: false



    val chromiumWindowSize: Pair<Int, Int>
        get() {
            val raw = properties.getProperty("chromium.windowSize")?.trim()
            if (!raw.isNullOrBlank()) {
                val parts = raw.lowercase().replace("*", "x").split("x").mapNotNull { it.trim().toIntOrNull() }
                if (parts.size == 2 && parts[0] > 0 && parts[1] > 0) return parts[0] to parts[1]
            }
            return 1920 to 1080
        }

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
            val currentDirConfig = File(CONFIG_FILENAME)
            if (currentDirConfig.exists()) {
                FileInputStream(currentDirConfig).use { properties.load(it) }
                Napier.i("Loaded from current directory: ${currentDirConfig.absolutePath}", tag = Tags.CONFIG)
                return
            }

            val resourceStream = javaClass.classLoader.getResourceAsStream(CONFIG_FILENAME)
            if (resourceStream != null) {
                resourceStream.use { properties.load(it) }
                Napier.i("Loaded from resources", tag = Tags.CONFIG)
                return
            }

            Napier.i("No config file found, using defaults (backend.url=$DEFAULT_BACKEND_URL)", tag = Tags.CONFIG)
        } catch (e: Exception) {
            Napier.e("Error loading config: ${e.message}, using defaults", e, tag = Tags.CONFIG)
        }
    }

    fun printConfig() {
        Napier.i("=== AppConfig ===", tag = Tags.CONFIG)
        Napier.i("backend.url: $backendUrl", tag = Tags.CONFIG)
        Napier.i("browser.zoom: $browserZoom", tag = Tags.CONFIG)
        Napier.i("chromium.deviceScaleFactor: $chromiumDeviceScaleFactor", tag = Tags.CONFIG)
        Napier.i("chromium.startFullscreen: $chromiumStartFullscreen", tag = Tags.CONFIG)
        Napier.i("browser.fitToWindow: $browserFitToWindow", tag = Tags.CONFIG)

        Napier.i("chromium.windowSize: ${chromiumWindowSize.first}x${chromiumWindowSize.second}", tag = Tags.CONFIG)
        Napier.i("=================", tag = Tags.CONFIG)
    }
}
