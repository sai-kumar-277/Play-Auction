package com.playauction.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "playauction_prefs")

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_TOKEN       = stringPreferencesKey("session_token")
        val KEY_USER_ID     = stringPreferencesKey("user_id")
        val KEY_PLAYER_NAME = stringPreferencesKey("player_name")
        val KEY_SERVER_URL  = stringPreferencesKey("server_url")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val playerNameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_PLAYER_NAME] }
    val serverUrlFlow: Flow<String?> = context.dataStore.data.map { it[KEY_SERVER_URL] }

    suspend fun saveSession(token: String, userId: String, playerName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN]       = token
            prefs[KEY_USER_ID]     = userId
            prefs[KEY_PLAYER_NAME] = playerName
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_PLAYER_NAME)
        }
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url }
    }

    /** Blocking read for OkHttp interceptor (called on IO thread) */
    fun getTokenBlocking(): String? = runBlocking { tokenFlow.first() }

    suspend fun getToken(): String? = tokenFlow.first()
    suspend fun getUserId(): String? = userIdFlow.first()
    suspend fun getPlayerName(): String? = playerNameFlow.first()
}
