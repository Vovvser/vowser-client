package com.vowser.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vowser.client.data.AuthRepository
import com.vowser.client.auth.AuthManagerDesktop
import com.vowser.client.auth.DesktopTokenStorage
import com.vowser.client.config.AppConfig
import com.vowser.client.data.createHttpClient
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberAppViewModel(
    coroutineScope: CoroutineScope
): AppViewModel {
    return remember {
        val config = AppConfig.getInstance()
        config.printConfig() // 설정 출력

        val tokenStorage = DesktopTokenStorage()
        val httpClient = createHttpClient(tokenStorage, config.backendUrl)
        val authRepository = AuthRepository(httpClient, config.backendUrl)
        val authManager = AuthManagerDesktop(config.backendUrl, authRepository)

        AppViewModel(
            coroutineScope = coroutineScope,
            tokenStorage = tokenStorage,
            authRepository = authRepository,
            authManager = authManager
        )
    }
}