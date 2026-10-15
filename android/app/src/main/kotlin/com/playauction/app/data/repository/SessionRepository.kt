package com.playauction.app.data.repository

import com.playauction.app.data.local.SessionDataStore
import com.playauction.app.data.model.SessionData
import com.playauction.app.data.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val api: ApiService,
    private val dataStore: SessionDataStore
) {
    val tokenFlow: Flow<String?> = dataStore.tokenFlow
    val userIdFlow: Flow<String?> = dataStore.userIdFlow
    val playerNameFlow: Flow<String?> = dataStore.playerNameFlow

    data class CurrentSession(
        val token: String?,
        val userId: String?,
        val playerName: String?
    ) {
        val isValid: Boolean get() = !token.isNullOrBlank() && !userId.isNullOrBlank()
    }

    val sessionFlow: Flow<CurrentSession> = combine(
        dataStore.tokenFlow,
        dataStore.userIdFlow,
        dataStore.playerNameFlow
    ) { token, userId, playerName -> CurrentSession(token, userId, playerName) }

    /**
     * Initialise / refresh session.
     * If a valid, non-expired token already exists with the SAME name, reuses it
     * without hitting the network (mirrors web SessionContext.initSession).
     */
    suspend fun initSession(playerName: String): SessionData = withContext(Dispatchers.IO) {
        val savedToken = dataStore.getToken()
        if (!savedToken.isNullOrBlank()) {
            val payload = decodeJwt(savedToken)
            if (payload != null && payload.optString("playerName") == playerName.trim()) {
                val userId = payload.getString("userId")
                val name   = payload.getString("playerName")
                return@withContext SessionData(savedToken, userId, name)
            }
        }

        // Network call — create new session
        val response = api.initSession(mapOf("playerName" to playerName.trim()))
        dataStore.saveSession(response.token, response.userId, response.playerName)
        response
    }

    suspend fun restoreSession(): CurrentSession {
        val token = dataStore.getToken() ?: return CurrentSession(null, null, null)
        val payload = decodeJwt(token) ?: run {
            dataStore.clearSession()
            return CurrentSession(null, null, null)
        }
        val userId     = payload.optString("userId", null)
        val playerName = payload.optString("playerName", null)
        return CurrentSession(token, userId, playerName)
    }

    suspend fun clearSession() = dataStore.clearSession()

    private fun decodeJwt(token: String): JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            val json = JSONObject(payload)
            val exp = json.optLong("exp", 0L)
            if (exp > 0 && System.currentTimeMillis() / 1000 > exp) null else json
        } catch (e: Exception) {
            null
        }
    }
}
