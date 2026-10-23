package com.playauction.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playauction.app.data.model.*
import com.playauction.app.data.socket.*
import com.playauction.app.data.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── State ────────────────────────────────────────────────────────────────────

data class LobbyUiState(
    val selectedLeague: String = "ipl",
    val selectedYear: String? = null,
    val roomState: RoomState? = null,
    val availableTeams: List<FranchiseDef> = emptyList(),
    val isJoined: Boolean = false,
    val isLoading: Boolean = false,
    val isSpectator: Boolean = false,
    val isPendingApproval: Boolean = false,
    val spectators: List<SpectatorInfo> = emptyList(),
    val joinRequests: List<JoinRequest> = emptyList(),
    val onlineMap: Map<String, Boolean> = emptyMap(),
    val coHostUserIds: List<String> = emptyList(),
    val myRetentions: List<String> = emptyList(),
    val myRetentionSquad: List<RetentionPlayer>? = null,
    val retentionConfirmed: Boolean = false,
    val lobbyRetentionStatus: Map<String, List<String>?> = emptyMap(),
    val customPrices: Map<String, Double> = emptyMap(),
    val lobbyCustomPrices: Map<String, Map<String, Double>> = emptyMap(),
    val publicRooms: List<PublicRoom> = emptyList(),
    val error: String? = null,
    val toast: ToastMessage? = null,
    val creatingRoomType: String = "private",
    val selectedCurrency: String = ""
)

data class ToastMessage(val message: String, val type: String = "info") // type: success/error/warning/info

// ─── One-shot Events ──────────────────────────────────────────────────────────

sealed class LobbyEvent {
    data class NavigateToAuction(val roomCode: String, val roomState: RoomState, val isSpectator: Boolean) : LobbyEvent()
    data class NavigateToJoinRoom(val roomCode: String) : LobbyEvent()
    object RoomDisbanded : LobbyEvent()
    data class ShowToast(val message: String, val type: String = "info") : LobbyEvent()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val socketManager: SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(LobbyUiState())
    val state: StateFlow<LobbyUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LobbyEvent>()
    val events: SharedFlow<LobbyEvent> = _events.asSharedFlow()

    init {
        collectSocketEvents()
    }

    // ─── Socket Event Collectors ─────────────────────────────────────────────

    private fun collectSocketEvents() {
        viewModelScope.launch {
            socketManager.onRoomCreated.collect { evt ->
                val roomCode = evt.roomCode ?: evt.state?.roomCode ?: return@collect
                _state.update { it.copy(roomState = evt.state, isJoined = true, isLoading = false, error = null) }
                _events.emit(LobbyEvent.NavigateToJoinRoom(roomCode))
            }
        }
        viewModelScope.launch {
            socketManager.onRoomJoined.collect { roomState ->
                val amSpectator = roomState.spectators.any { s ->
                    s.socketId == socketManager.currentSocketId
                }
                _state.update { s ->
                    s.copy(
                        roomState = roomState,
                        isJoined = true,
                        isLoading = false,
                        isSpectator = amSpectator,
                        coHostUserIds = roomState.coHostUserIds,
                        selectedYear = roomState.auctionYear,
                        lobbyRetentionStatus = roomState.retentionsByTeam?.mapValues { it.value } ?: emptyMap(),
                        error = null
                    )
                }
                when (roomState.status) {
                    "Auctioning", "Paused" ->
                        _events.emit(LobbyEvent.NavigateToAuction(roomState.roomCode, roomState, amSpectator))
                    "Quiz"       -> _events.emit(LobbyEvent.NavigateToAuction(roomState.roomCode, roomState, amSpectator))
                    "Evaluating" -> _events.emit(LobbyEvent.NavigateToAuction(roomState.roomCode, roomState, amSpectator))
                    "Finished"   -> _events.emit(LobbyEvent.NavigateToAuction(roomState.roomCode, roomState, amSpectator))
                }
            }
        }
        viewModelScope.launch {
            socketManager.onAvailableTeams.collect { evt ->
                _state.update { it.copy(availableTeams = evt.teams) }
            }
        }
        viewModelScope.launch {
            socketManager.onLobbyUpdate.collect { evt ->
                _state.update { s ->
                    s.copy(roomState = s.roomState?.copy(teams = evt.teams) ?: return@update s)
                }
            }
        }
        viewModelScope.launch {
            socketManager.onSpectatorUpdate.collect { evt ->
                val amSpectator = evt.spectators.any { it.socketId == socketManager.currentSocketId }
                _state.update { it.copy(spectators = evt.spectators, isSpectator = amSpectator) }
            }
        }
        viewModelScope.launch {
            socketManager.onAuctionStarted.collect { state ->
                val amSpectator = _state.value.isSpectator
                _events.emit(LobbyEvent.NavigateToAuction(state.roomCode, state, amSpectator))
            }
        }
        viewModelScope.launch {
            socketManager.onRoomDisbanded.collect {
                _state.update { it.copy(isJoined = false, roomState = null, error = "Host has ended this room.") }
                _events.emit(LobbyEvent.RoomDisbanded)
            }
        }
        viewModelScope.launch {
            socketManager.onError.collect { msg ->
                _state.update { it.copy(error = msg, isLoading = false, isPendingApproval = if (msg.contains("PLAYER_JOIN_DISABLED") || msg.contains("SPECTATOR_APPROVAL_REQUIRED")) it.isPendingApproval else false) }
            }
        }
        viewModelScope.launch {
            socketManager.onNameTaken.collect { msg ->
                _state.update { it.copy(error = msg, isLoading = false) }
            }
        }
        viewModelScope.launch {
            socketManager.onTeamClaimedSuccess.collect {
                _state.update { it.copy(isLoading = false) }
                _events.emit(LobbyEvent.ShowToast("Franchise secured!", "success"))
            }
        }
        viewModelScope.launch {
            socketManager.onJoinRequestsUpdate.collect { evt ->
                _state.update { it.copy(joinRequests = evt.requests) }
            }
        }
        viewModelScope.launch {
            socketManager.onParticipationApproved.collect {
                _state.update { it.copy(isPendingApproval = false) }
                _events.emit(LobbyEvent.ShowToast("Access approved!", "success"))
            }
        }
        viewModelScope.launch {
            socketManager.onParticipationRejected.collect {
                _state.update { it.copy(isPendingApproval = false, error = "Your request was declined by the host.") }
            }
        }
        viewModelScope.launch {
            socketManager.onMyTeamRetentionSquad.collect { evt ->
                _state.update { it.copy(myRetentionSquad = evt.squad, retentionConfirmed = false, myRetentions = emptyList(), customPrices = emptyMap()) }
            }
        }
        viewModelScope.launch {
            socketManager.onRetentionsConfirmed.collect { evt ->
                _state.update { it.copy(retentionConfirmed = true) }
                _events.emit(LobbyEvent.ShowToast("Retentions submitted! Total: ${evt.totalCost}", "success"))
            }
        }
        viewModelScope.launch {
            socketManager.onRetentionsAlreadySubmitted.collect { evt ->
                _state.update { it.copy(retentionConfirmed = true, myRetentions = evt.retainedPlayers, customPrices = evt.customPrices) }
            }
        }
        viewModelScope.launch {
            socketManager.onLobbyRetentionUpdate.collect { evt ->
                _state.update { it.copy(lobbyRetentionStatus = evt.retentionStatus.mapValues { e -> e.value }, lobbyCustomPrices = evt.customPricesByTeam) }
            }
        }
        viewModelScope.launch {
            socketManager.onPublicRoomsUpdate.collect { rooms ->
                _state.update { it.copy(publicRooms = rooms) }
            }
        }
        viewModelScope.launch {
            socketManager.onCohostsUpdated.collect { ids ->
                _state.update { it.copy(coHostUserIds = ids, roomState = it.roomState?.copy(coHostUserIds = ids)) }
            }
        }
        viewModelScope.launch {
            socketManager.onLobbySettingsUpdated.collect { evt ->
                _state.update { s ->
                    s.copy(roomState = s.roomState?.copy(teamCount = evt.teamCount, allowSpectators = evt.allowSpectators, maxSpectators = evt.maxSpectators))
                }
            }
        }
        viewModelScope.launch {
            socketManager.onSettingsUpdated.collect { evt ->
                _state.update { s -> s.copy(roomState = s.roomState) }
            }
        }
        viewModelScope.launch {
            socketManager.onKickedFromRoom.collect {
                _state.update { it.copy(isJoined = false, roomState = null, error = "You have been removed from the room.") }
            }
        }
    }

    // ─── Outgoing Actions ────────────────────────────────────────────────────

    fun setSelectedLeague(league: String) {
        val defaultCurrency = if (league == "sa20") "zar" else "inr"
        _state.update { it.copy(selectedLeague = league, selectedCurrency = "", selectedYear = null) }
    }

    fun setSelectedYear(year: String?) = _state.update { it.copy(selectedYear = year) }

    fun setRoomType(type: String) = _state.update { it.copy(creatingRoomType = type) }

    fun setSelectedCurrency(currency: String) = _state.update { it.copy(selectedCurrency = currency) }

    fun createRoom() {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        val defaultCurrency = if (s.selectedLeague == "sa20") "zar" else "inr"
        socketManager.createRoom(
            roomType    = s.creatingRoomType,
            league      = s.selectedLeague,
            currency    = s.selectedCurrency.ifBlank { defaultCurrency },
            auctionYear = s.selectedYear
        )
    }

    fun joinRoom(roomCode: String, asSpectator: Boolean = false) {
        _state.update { it.copy(isLoading = true, error = null) }
        socketManager.joinRoom(roomCode.uppercase(), asSpectator)
    }

    fun claimTeam(teamId: String) {
        val roomCode = _state.value.roomState?.roomCode ?: return
        _state.update { it.copy(isLoading = true) }
        socketManager.claimTeam(roomCode, teamId)
    }

    fun startAuction() {
        val roomCode = _state.value.roomState?.roomCode ?: return
        socketManager.startAuction(roomCode)
    }

    fun leaveRoom() {
        val roomCode = _state.value.roomState?.roomCode ?: return
        socketManager.leaveRoom(roomCode)
        _state.update { it.copy(isJoined = false, roomState = null, myRetentions = emptyList(), myRetentionSquad = null, retentionConfirmed = false) }
    }

    fun requestAccess(roomCode: String) {
        socketManager.requestParticipation(roomCode)
        _state.update { it.copy(isPendingApproval = true) }
    }

    fun approveParticipation(roomCode: String, userId: String?, socketId: String?) =
        socketManager.approveParticipation(roomCode, userId, socketId)

    fun rejectParticipation(roomCode: String, userId: String?, socketId: String?) =
        socketManager.rejectParticipation(roomCode, userId, socketId)

    fun toggleCoHost(targetUserId: String) {
        val roomCode = _state.value.roomState?.roomCode ?: return
        socketManager.toggleCoHost(roomCode, targetUserId)
    }

    fun updateLobbySettings(allowSpectators: Boolean, maxSpectators: Int, teamCount: Int) {
        val roomCode = _state.value.roomState?.roomCode ?: return
        socketManager.updateLobbySettings(roomCode, allowSpectators, maxSpectators, teamCount)
    }

    fun fetchPublicRooms(league: String? = null) = socketManager.fetchPublicRooms(league)

    fun clearError() = _state.update { it.copy(error = null) }

    // ─── Retention Logic (mirrors Lobby.jsx) ─────────────────────────────────

    fun toggleRetainedPlayer(playerName: String) {
        val s = _state.value
        val league = s.roomState?.league ?: s.selectedLeague
        val rules = RetentionRules.forLeague(league) ?: return
        val squad = s.myRetentionSquad ?: return
        val playerObj = squad.find { it.name == playerName }

        if (s.myRetentions.contains(playerName)) {
            _state.update { st ->
                st.copy(
                    myRetentions = st.myRetentions - playerName,
                    customPrices = st.customPrices - playerName
                )
            }
        } else {
            val isUncapped = playerObj?.isUncapped ?: false
            val isOverseas = playerObj?.isOverseas ?: false
            val currentCapped    = s.myRetentions.count { n -> squad.find { it.name == n }?.isUncapped == false }
            val currentUncapped  = s.myRetentions.count { n -> squad.find { it.name == n }?.isUncapped == true }
            val currentOverseas  = s.myRetentions.count { n -> squad.find { it.name == n }?.isOverseas == true }

            if (isOverseas && currentOverseas >= 2) {
                viewModelScope.launch { _events.emit(LobbyEvent.ShowToast("Max 2 overseas players can be retained.", "warning")) }
                return
            }
            if (isUncapped && currentUncapped >= rules.maxUncapped) {
                viewModelScope.launch { _events.emit(LobbyEvent.ShowToast("Max ${rules.maxUncapped} uncapped players can be retained.", "warning")) }
                return
            }
            if (!isUncapped && currentCapped >= rules.maxCapped) {
                viewModelScope.launch { _events.emit(LobbyEvent.ShowToast("Max ${rules.maxCapped} capped players can be retained.", "warning")) }
                return
            }
            if (s.myRetentions.size >= rules.maxTotal) {
                viewModelScope.launch { _events.emit(LobbyEvent.ShowToast("Max ${rules.maxTotal} total players can be retained.", "warning")) }
                return
            }
            _state.update { it.copy(myRetentions = it.myRetentions + playerName) }
        }
    }

    fun updateCustomPrice(playerName: String, valueStr: String) {
        val league = _state.value.roomState?.league ?: _state.value.selectedLeague
        val multiplier = if (league == "wpl") 100.0 else 10.0
        val valD = valueStr.toDoubleOrNull()?.let { Math.round(it * multiplier).toDouble() }
        if (valD != null && valD >= 0) {
            _state.update { it.copy(customPrices = it.customPrices + (playerName to valD)) }
        } else if (valueStr.isBlank()) {
            _state.update { it.copy(customPrices = it.customPrices - playerName) }
        }
    }

    fun submitRetentions() {
        val s = _state.value
        val roomCode = s.roomState?.roomCode ?: return
        val league = s.roomState.league
        val rules = RetentionRules.forLeague(league) ?: return
        val squad = s.myRetentionSquad ?: return

        val breakdown = calculateRetentionBreakdown(s.myRetentions, squad, rules, s.customPrices)
        val submittedPrices = breakdown.associate { it.name to it.cost }

        socketManager.submitRetentions(roomCode, s.myRetentions, submittedPrices)
    }

    private fun calculateRetentionBreakdown(
        selected: List<String>,
        squad: List<RetentionPlayer>,
        rules: RetentionRules,
        customPrices: Map<String, Double>
    ): List<RetentionBreakdown> {
        val capped   = selected.filter { n -> squad.find { it.name == n }?.isUncapped == false }
        val uncapped = selected.filter { n -> squad.find { it.name == n }?.isUncapped == true }
        val breakdown = mutableListOf<RetentionBreakdown>()
        capped.forEachIndexed { idx, name ->
            val default = rules.cappedCosts.getOrElse(idx) { rules.cappedCosts.last() }
            breakdown.add(RetentionBreakdown(name, customPrices[name] ?: default, false, idx + 1))
        }
        uncapped.firstOrNull()?.let { name ->
            breakdown.add(RetentionBreakdown(name, customPrices[name] ?: rules.uncappedCost, true, null))
        }
        return breakdown
    }
}
