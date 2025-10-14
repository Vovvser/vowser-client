package com.vowser.client.auth

interface AuthManager {
    fun login()
    fun startCallbackServer(onTokenReceived: (String, String) -> Unit)
    fun stopCallbackServer()
}