package com.vowser.client.auth

import java.util.prefs.Preferences

class DesktopTokenStorage : TokenStorage {
    private val prefs = Preferences.userNodeForPackage(DesktopTokenStorage::class.java)

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.put(ACCESS_TOKEN, accessToken)
        prefs.put(REFRESH_TOKEN, refreshToken)
    }

    override fun getAccessToken(): String? {
        return prefs.get(ACCESS_TOKEN, null)
    }

    override fun getRefreshToken(): String? {
        return prefs.get(REFRESH_TOKEN, null)
    }

    override fun clearTokens() {
        prefs.remove(ACCESS_TOKEN)
        prefs.remove(REFRESH_TOKEN)
    }
}