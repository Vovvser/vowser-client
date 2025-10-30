package com.vowser.client.desktop

import io.github.aakira.napier.Napier
import java.awt.GraphicsConfiguration
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.Toolkit

data class ScreenBounds(val x: Int, val y: Int, val width: Int, val height: Int)

object ScreenUtil {
    fun currentPointerScreenBounds(): ScreenBounds {
        return runCatching {
            val ge: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val pointer = runCatching { MouseInfo.getPointerInfo() }.getOrNull()
            val device: GraphicsDevice = pointer?.device ?: ge.defaultScreenDevice
            val gc: GraphicsConfiguration = device.defaultConfiguration
            val raw: Rectangle = gc.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
            val x = raw.x + insets.left
            val y = raw.y + insets.top
            val w = (raw.width - insets.left - insets.right).coerceAtLeast(100)
            val h = (raw.height - insets.top - insets.bottom).coerceAtLeast(100)
            ScreenBounds(x, y, w, h)
        }.onFailure {
            Napier.w("Failed to get pointer screen bounds: ${it.message}")
        }.getOrElse {
            primaryScreenBounds()
        }
    }


    fun primaryScreenBounds(): ScreenBounds {
        return runCatching {
            val ge: GraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val device: GraphicsDevice = ge.defaultScreenDevice
            val gc: GraphicsConfiguration = device.defaultConfiguration
            val raw: Rectangle = gc.bounds
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
            val x = raw.x + insets.left
            val y = raw.y + insets.top
            val w = (raw.width - insets.left - insets.right).coerceAtLeast(100)
            val h = (raw.height - insets.top - insets.bottom).coerceAtLeast(100)
            ScreenBounds(x, y, w, h)
        }.getOrElse {
            ScreenBounds(0, 0, 1280, 720)
        }
    }
}

