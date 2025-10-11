package com.vowser.client.auth

class AuthManagerIOS : AuthManager {
    override fun login() {
        // TODO: Implement iOS OAuth login
        println("AuthManagerIOS: Login not yet implemented for iOS")
    }

    override fun startCallbackServer(onTokenReceived: (String, String) -> Unit) {
        // TODO: Implement callback handling for iOS
        println("AuthManagerIOS: Callback server not yet implemented")
    }

    override fun stopCallbackServer() {
        println("AuthManagerIOS: No callback server to stop")
    }
}
