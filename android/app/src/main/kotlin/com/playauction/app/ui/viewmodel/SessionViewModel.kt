package com.playauction.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playauction.app.data.local.SessionDataStore
import com.playauction.app.data.model.SessionData
import com.playauction.app.data.repository.SessionRepository
import com.playauction.app.data.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionUiState {
    object Idle    : SessionUiState()
    object Loading : SessionUiState()
    data class Ready(val userId: String, val playerName: String, val token: String) : SessionUiState()
    data class Error(val message: String) : SessionUiState()
}

sealed class SessionEvent {
    data class SessionReady(val session: SessionData) : SessionEvent()
    data class ShowError(val message: String)        : SessionEvent()
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val socketManager: SocketManager,
    private val dataStore: SessionDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Idle)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>()
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    val playerNameFlow: Flow<String?> = dataStore.playerNameFlow
    val userIdFlow: Flow<String?>     = dataStore.userIdFlow
    val tokenFlow: Flow<String?>      = dataStore.tokenFlow

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val saved = sessionRepo.restoreSession()
            if (saved.isValid) {
                _uiState.value = SessionUiState.Ready(
                    userId     = saved.userId!!,
                    playerName = saved.playerName ?: "",
                    token      = saved.token!!
                )
            }
        }
    }

    fun initSession(playerName: String, serverUrl: String) {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Loading
            try {
                val session = sessionRepo.initSession(playerName)
                // (Re-)connect socket with fresh token
                socketManager.connect(serverUrl, session.token)
                _uiState.value = SessionUiState.Ready(
                    userId     = session.userId,
                    playerName = session.playerName,
                    token      = session.token
                )
                _events.emit(SessionEvent.SessionReady(session))
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to create session"
                _uiState.value = SessionUiState.Error(msg)
                _events.emit(SessionEvent.ShowError(msg))
            }
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            sessionRepo.clearSession()
            socketManager.disconnect()
            _uiState.value = SessionUiState.Idle
        }
    }

    /** Returns current userId synchronously from state (for other VMs) */
    val currentUserId: String? get() = (_uiState.value as? SessionUiState.Ready)?.userId
    val currentToken:  String? get() = (_uiState.value as? SessionUiState.Ready)?.token
    val currentName:   String? get() = (_uiState.value as? SessionUiState.Ready)?.playerName
}
