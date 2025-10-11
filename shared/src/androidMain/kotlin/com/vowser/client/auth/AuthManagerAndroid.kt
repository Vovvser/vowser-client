package com.vowser.client.auth

class AuthManagerAndroid : AuthManager {
    override fun login() {
        // TODO: Implement Android OAuth login
        println("AuthManagerAndroid: Login not yet implemented for Android")
    }

    override fun startCallbackServer(onTokenReceived: (String, String) -> Unit) {
        // TODO: Implement callback handling for Android
        println("AuthManagerAndroid: Callback server not yet implemented")
    }

    override fun stopCallbackServer() {
        println("AuthManagerAndroid: No callback server to stop")
    }
}
