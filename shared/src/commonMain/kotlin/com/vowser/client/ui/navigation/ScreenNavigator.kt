package com.vowser.client.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vowser.client.ui.screens.AppScreen

/**
 * Simple navigation controller that manages [AppScreen] stack for Compose screens.
 */
interface ScreenNavigator {
    fun push(screen: AppScreen)
    fun pop()
    fun replaceAll(vararg screens: AppScreen)
    fun current(): AppScreen
}

/**
 * CompositionLocal used by screens/components to obtain the current [ScreenNavigator].
 */
val LocalScreenNavigator = staticCompositionLocalOf<ScreenNavigator> {
    error("ScreenNavigator not provided")
}

internal class StackScreenNavigator(
    private val screenStack: SnapshotStateList<AppScreen>
) : ScreenNavigator {

    override fun push(screen: AppScreen) {
        screenStack.add(screen)
    }

    override fun pop() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }

    override fun replaceAll(vararg screens: AppScreen) {
        screenStack.clear()
        if (screens.isNotEmpty()) {
            screenStack.addAll(screens.toList())
        } else {
            screenStack.add(AppScreen.HOME)
        }
    }

    override fun current(): AppScreen = screenStack.last()
}
