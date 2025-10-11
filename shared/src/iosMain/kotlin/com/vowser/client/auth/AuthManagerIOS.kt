package com.vowser.client.auth

import com.vowser.client.logging.Tags
import io.github.aakira.napier.Napier

class AuthManagerIOS : AuthManager {
    override fun login() {
        Napier.w("Login not yet implemented for iOS", tag = Tags.AUTH)
    }

    override fun startCallbackServer(onTokenReceived: (String, String) -> Unit) {
        Napier.w("Callback server not yet implemented for iOS", tag = Tags.AUTH)
    }

    override fun stopCallbackServer() {
        Napier.d("No callback server to stop", tag = Tags.AUTH)
    }
}
