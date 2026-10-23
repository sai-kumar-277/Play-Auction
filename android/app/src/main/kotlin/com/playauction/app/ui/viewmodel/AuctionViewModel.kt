package com.playauction.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playauction.app.data.model.*
import com.playauction.app.data.socket.*
import com.playauction.app.data.socket.SocketManager
import com.playauction.app.domain.BidRulesEngine
import com.playauction.app.domain.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── State ────────────────────────────────────────────────────────────────────

data class AuctionUiState(
    val roomCode: String = "",
    val status: String = "Lobby",
    val league: String = "ipl",
    val currency: String = "inr",
    val isAiMode: Boolean = false,
    val currentPlayer: Player? = null,
    val currentBid: CurrentBid = CurrentBid.EMPTY,
    val timer: Int = 10,
    val timerDuration: Int = 10,
    val teams: List<Team> = emptyList(),
    val myTeam: Team? = null,
    val isHost: Boolean = false,
    val isCoHost: Boolean = false,
    val coHostUserIds: List<String> = emptyList(),
    val hostUserId: String? = null,
    val bidHistory: List<BidHistoryEntry> = emptyList(),
    val last5Bids: List<BidHistoryEntry> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val upcomingPlayers: List<Player> = emptyList(),
    val unsoldHistory: List<Player> = emptyList(),
    val auctionFeed: List<AuctionFeedEntry> = emptyList(),
    val soldEvent: SoldEvent? = null,
    val isPaused: Boolean = false,
    val rtmState: RtmState? = null,
    val votingSession: VotingSession? = null,
    val endRequest: EndRequest? = null,
    val humanOwners: List<String> = emptyList(),
    val onlineMap: Map<String, Boolean> = emptyMap(),
    val legendaryPlayer: Player? = null,
    val expandedTeamId: String? = null,
    val teamRosters: Map<String, List<AcquiredPlayer>> = emptyMap(),
    val spectators: List<SpectatorInfo> = emptyList(),
    val joinRequests: List<JoinRequest> = emptyList(),
    val showPoolModal: Boolean = false,
    val poolTab: String = "live",  // live | sold | unsold
    val evalTimer: Int = 90,
    val error: String? = null
)

// ─── One-shot Events ──────────────────────────────────────────────────────────

sealed class AuctionEvent {
    object PlayBidSound                                  : AuctionEvent()
    object PlayWarnSound                                 : AuctionEvent()
    data class PlayGavelSlam(val type: String, val teamName: String?) : AuctionEvent()
    object HapticFeedback                                : AuctionEvent()
    data class ShowToast(val message: String, val type: String = "info") : AuctionEvent()
    data class NavigateToQuiz(val roomCode: String)      : AuctionEvent()
    data class NavigateToEvaluating(val roomCode: String): AuctionEvent()
    data class NavigateToResults(val roomCode: String, val teams: List<Team>, val quizLeaderboard: List<QuizLeaderboardEntry>) : AuctionEvent()
    object RoomDisbanded                                 : AuctionEvent()
    data class LegendaryPlayerEntrance(val player: Player) : AuctionEvent()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AuctionViewModel @Inject constructor(
    private val socketManager: SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuctionUiState())
    val state: StateFlow<AuctionUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AuctionEvent>()
    val events: SharedFlow<AuctionEvent> = _events.asSharedFlow()

    /** Called once when the screen opens with the room code and (optionally) initial state */
    fun initRoom(roomCode: String, initialState: RoomState?, userId: String?, socketId: String?) {
        _state.update { s ->
            val myTeam = initialState?.teams?.firstOrNull { it.isOwnedBy(userId, socketId) }
            val isHost = userId != null && userId == initialState?.hostUserId
            s.copy(
                roomCode     = roomCode,
                status       = initialState?.status ?: "Lobby",
                league       = initialState?.league ?: "ipl",
                currency     = initialState?.currency ?: "inr",
                isAiMode     = initialState?.isAiMode ?: false,
                teams        = initialState?.teams ?: emptyList(),
                myTeam       = myTeam,
                isHost       = isHost,
                coHostUserIds= initialState?.coHostUserIds ?: emptyList(),
                hostUserId   = initialState?.hostUserId,
                timerDuration= initialState?.timerDuration ?: 10,
                timer        = initialState?.timer ?: 10,
                isPaused     = initialState?.isPaused ?: false,
                rtmState     = initialState?.rtmState,
                unsoldHistory= initialState?.unsoldHistory ?: emptyList()
            )
        }
        // Emit join_room so server re-syncs us
        socketManager.joinRoom(roomCode, false)
        collectSocketEvents(userId, socketId)
    }

    // ─── Socket Collectors ────────────────────────────────────────────────────

    private fun collectSocketEvents(userId: String?, socketId: String?) {
        viewModelScope.launch {
            socketManager.onRoomJoined.collect { roomState ->
                applyRoomJoined(roomState, userId, socketId)
            }
        }
        viewModelScope.launch {
            socketManager.onNewPlayer.collect { evt ->
                val player = evt.player ?: return@collect
                val isLegend = !_state.value.isAiMode && isLegendPlayer(player.displayName, _state.value.league)
                _state.update { s ->
                    s.copy(
                        currentPlayer  = player,
                        currentBid     = CurrentBid.EMPTY,
                        soldEvent      = null,
                        rtmState       = null,
                        bidHistory     = emptyList(),
                        upcomingPlayers= evt.nextPlayers ?: emptyList()
                    )
                }
                if (isLegend) _events.emit(AuctionEvent.LegendaryPlayerEntrance(player))
            }
        }
        viewModelScope.launch {
            socketManager.onBidPlaced.collect { evt ->
                val bid = BidHistoryEntry(
                    amount    = evt.currentBid.amount,
                    teamName  = evt.currentBid.teamName,
                    teamColor = evt.currentBid.teamColor,
                    teamLogo  = evt.currentBid.teamLogo,
                    ownerName = evt.currentBid.ownerName
                )
                _state.update { s ->
                    s.copy(
                        currentBid  = evt.currentBid,
                        timer       = evt.timer,
                        bidHistory  = listOf(bid) + s.bidHistory,
                        last5Bids   = (listOf(bid) + s.last5Bids).take(5)
                    )
                }
                _events.emit(AuctionEvent.PlayBidSound)
                checkBiddingWarThreshold(evt.currentBid.amount)
            }
        }
        viewModelScope.launch {
            socketManager.onAuctionStateSync.collect { evt ->
                _state.update { s ->
                    val myTeam = evt.teams.firstOrNull { it.isOwnedBy(userId, socketId) }
                    s.copy(
                        status     = evt.status,
                        currentBid = evt.currentBid ?: s.currentBid,
                        timer      = evt.timer,
                        teams      = evt.teams,
                        myTeam     = myTeam,
                        last5Bids  = evt.last5Bids.map { BidHistoryEntry(amount = it.amount, teamName = it.teamName, teamColor = it.teamColor, teamLogo = it.teamLogo, ownerName = it.ownerName) },
                        rtmState   = evt.rtmState,
                        isPaused   = evt.status == "Paused"
                    )
                }
            }
        }
        viewModelScope.launch {
            socketManager.onTimerTick.collect { t ->
                _state.update { it.copy(timer = t) }
                if (t in 1..3) _events.emit(AuctionEvent.PlayWarnSound)
                if (t in 1..3) _events.emit(AuctionEvent.HapticFeedback)
            }
        }
        viewModelScope.launch {
            socketManager.onPlayerSold.collect { evt ->
                val sold = SoldEvent("SOLD", evt.player, evt.winningBid)
                val feedEntry = AuctionFeedEntry(
                    key        = "sold-${evt.player._id}-${System.currentTimeMillis()}",
                    type       = "sold",
                    playerName = evt.player.displayName,
                    playerImage= evt.player.displayImage,
                    teamLogo   = evt.winningBid?.teamLogo,
                    teamShort  = resolveTeamShort(evt.winningBid?.teamName),
                    teamColor  = evt.winningBid?.teamColor,
                    ownerName  = evt.winningBid?.ownerName,
                    amount     = evt.winningBid?.amount,
                    slogan     = if (evt.winningBid?.isRtm == true) "Acquired via RTM" else null,
                    timestamp  = currentTimeString()
                )
                val chatMsg = buildSoldChatMessage(evt)
                _state.update { s ->
                    val myTeam = evt.teams.firstOrNull { it.isOwnedBy(userId, socketId) }
                    s.copy(
                        soldEvent   = sold,
                        teams       = evt.teams,
                        myTeam      = myTeam,
                        auctionFeed = (listOf(feedEntry) + s.auctionFeed).take(50),
                        chatMessages= (s.chatMessages + chatMsg).takeLast(200)
                    )
                }
                _events.emit(AuctionEvent.PlayGavelSlam("SOLD", evt.winningBid?.teamName))
            }
        }
        viewModelScope.launch {
            socketManager.onPlayerUnsold.collect { evt ->
                val unsold = SoldEvent("UNSOLD", evt.player, null)
                val feedEntry = AuctionFeedEntry(
                    key        = "unsold-${evt.player._id}-${System.currentTimeMillis()}",
                    type       = "unsold",
                    playerName = evt.player.displayName,
                    playerImage= evt.player.displayImage,
                    timestamp  = currentTimeString()
                )
                val chatMsg = buildUnsoldChatMessage(evt.player)
                _state.update { s ->
                    s.copy(
                        soldEvent    = unsold,
                        unsoldHistory= evt.updatedUnsoldHistory ?: s.unsoldHistory,
                        auctionFeed  = (listOf(feedEntry) + s.auctionFeed).take(50),
                        chatMessages = (s.chatMessages + chatMsg).takeLast(200)
                    )
                }
                _events.emit(AuctionEvent.PlayGavelSlam("UNSOLD", null))
            }
        }
        viewModelScope.launch {
            socketManager.onChatMessage.collect { msg ->
                _state.update { s -> s.copy(chatMessages = (s.chatMessages + msg).takeLast(200)) }
            }
        }
        viewModelScope.launch {
            socketManager.onInterestVotingStarted.collect { evt ->
                _state.update { s ->
                    s.copy(votingSession = VotingSession(
                        active  = true,
                        players = evt.players.map { VotingCandidate(it.id, it.name, it.poolID, it.basePrice) },
                        timer   = evt.timer,
                        isFinal = evt.isFinal
                    ))
                }
            }
        }
        viewModelScope.launch {
            socketManager.onInterestVotingCompleted.collect {
                _state.update { s -> s.copy(votingSession = s.votingSession?.copy(active = false)) }
            }
        }
        viewModelScope.launch {
            socketManager.onQuizPhaseStarted.collect { evt ->
                _events.emit(AuctionEvent.NavigateToQuiz(_state.value.roomCode))
            }
        }
        viewModelScope.launch {
            socketManager.onEvaluationStarted.collect { timer ->
                _state.update { it.copy(evalTimer = timer) }
                _events.emit(AuctionEvent.NavigateToEvaluating(_state.value.roomCode))
            }
        }
        viewModelScope.launch {
            socketManager.onAuctionFinished.collect { evt ->
                _events.emit(AuctionEvent.NavigateToResults(_state.value.roomCode, evt.teams, evt.quizLeaderboard))
            }
        }
        viewModelScope.launch {
            socketManager.onRoomDisbanded.collect { _events.emit(AuctionEvent.RoomDisbanded) }
        }
        viewModelScope.launch {
            socketManager.onError.collect { msg ->
                _state.update { it.copy(error = msg) }
                _events.emit(AuctionEvent.ShowToast(msg, "error"))
            }
        }
        viewModelScope.launch {
            socketManager.onLobbyUpdate.collect { evt ->
                _state.update { s ->
                    val myTeam = evt.teams.firstOrNull { it.isOwnedBy(userId, socketId) }
                    s.copy(teams = evt.teams, myTeam = myTeam)
                }
            }
        }
        viewModelScope.launch {
            socketManager.onSpectatorUpdate.collect { evt ->
                _state.update { it.copy(spectators = evt.spectators) }
            }
        }
        viewModelScope.launch {
            socketManager.onJoinRequestsUpdate.collect { evt ->
                _state.update { it.copy(joinRequests = evt.requests) }
            }
        }
        viewModelScope.launch {
            socketManager.onCohostsUpdated.collect { ids ->
                _state.update { s ->
                    val isCo = userId != null && ids.contains(userId)
                    s.copy(coHostUserIds = ids, isCoHost = isCo)
                }
            }
        }
        viewModelScope.launch {
            socketManager.onHostChanged.collect { evt ->
                _state.update { s ->
                    s.copy(
                        hostUserId = evt.newHostUserId,
                        isHost = userId != null && userId == evt.newHostUserId
                    )
                }
                _events.emit(AuctionEvent.ShowToast("Auctioneer changed: ${evt.newHostName}", "info"))
            }
        }
        viewModelScope.launch {
            socketManager.onPlayerStatusUpdate.collect { map ->
                _state.update { it.copy(onlineMap = map) }
            }
        }
        viewModelScope.launch {
            socketManager.onTeamRosterData.collect { evt ->
                _state.update { s -> s.copy(teamRosters = s.teamRosters + (evt.teamId to evt.playersAcquired)) }
            }
        }
        viewModelScope.launch {
            socketManager.onSettingsUpdated.collect { evt ->
                _state.update { it.copy(timerDuration = evt.timerDuration, timer = evt.timer ?: it.timer) }
            }
        }
        viewModelScope.launch {
            socketManager.onAuctionEndRequested.collect { evt ->
                _state.update { it.copy(endRequest = EndRequest(it.roomCode, evt.acknowledgedBy), humanOwners = evt.humanOwners) }
            }
        }
        viewModelScope.launch {
            socketManager.onEndAcknowledgementUpdate.collect { acknowledgedBy ->
                _state.update { it.copy(endRequest = it.endRequest?.copy(acknowledgedBy = acknowledgedBy)) }
            }
        }
        viewModelScope.launch {
            socketManager.onAuctionEndCancelled.collect {
                _state.update { it.copy(endRequest = null) }
            }
        }
        viewModelScope.launch {
            socketManager.onRtmState.collect { rtm ->
                _state.update { it.copy(rtmState = rtm) }
            }
        }
        viewModelScope.launch {
            socketManager.onAuctionPaused.collect {
                _state.update { it.copy(isPaused = true, status = "Paused") }
            }
        }
        viewModelScope.launch {
            socketManager.onAuctionResumed.collect { evt ->
                _state.update { it.copy(isPaused = false, status = "Auctioning", timer = evt.timer ?: it.timer) }
            }
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private fun applyRoomJoined(roomState: RoomState, userId: String?, socketId: String?) {
        val myTeam = roomState.teams.firstOrNull { it.isOwnedBy(userId, socketId) }
        val isHost = userId != null && userId == roomState.hostUserId
        val isCo   = userId != null && roomState.coHostUserIds.contains(userId)
        val activePlayer = roomState.activePlayer ?: roomState.currentPlayer
        _state.update { s ->
            s.copy(
                status       = roomState.status,
                league       = roomState.league,
                currency     = roomState.currency,
                isAiMode     = roomState.isAiMode,
                teams        = roomState.teams,
                myTeam       = myTeam,
                isHost       = isHost,
                isCoHost     = isCo,
                coHostUserIds= roomState.coHostUserIds,
                hostUserId   = roomState.hostUserId,
                timerDuration= roomState.timerDuration,
                timer        = roomState.timer,
                isPaused     = roomState.isPaused || roomState.status == "Paused",
                rtmState     = roomState.rtmState,
                unsoldHistory= roomState.unsoldHistory,
                currentPlayer= activePlayer,
                currentBid   = roomState.activeBid ?: roomState.currentBid ?: CurrentBid.EMPTY,
                upcomingPlayers = if (roomState.players.isNotEmpty()) roomState.players.drop((roomState.currentIndex + 1).coerceAtLeast(0)) else emptyList()
            )
        }
        // Reconstruct auction feed from join data
        val feedItems = buildAuctionFeedFromJoin(roomState)
        if (feedItems.isNotEmpty()) _state.update { it.copy(auctionFeed = feedItems) }
    }

    private fun buildAuctionFeedFromJoin(roomState: RoomState): List<AuctionFeedEntry> {
        val sold = roomState.teams.flatMap { t ->
            t.playersAcquired.map { p ->
                AuctionFeedEntry(
                    key        = "sold-${p.playerId ?: p.name}",
                    type       = "sold",
                    playerName = p.name,
                    playerImage= p.displayImage,
                    teamLogo   = t.teamLogo,
                    teamShort  = resolveTeamShort(t.teamName),
                    teamColor  = t.teamThemeColor,
                    ownerName  = t.ownerName,
                    amount     = p.boughtFor
                )
            }
        }
        val unsold = roomState.unsoldHistory.map { p ->
            AuctionFeedEntry(key = "unsold-${p._id ?: p.displayName}", type = "unsold", playerName = p.displayName, playerImage = p.displayImage)
        }
        return (sold + unsold).take(50)
    }

    private var bidWarFiredForCurrentPlayer = false

    private fun checkBiddingWarThreshold(amount: Double) {
        if (bidWarFiredForCurrentPlayer) return
        val player = _state.value.currentPlayer ?: return
        val poolID = player.poolID?.lowercase() ?: ""
        val threshold = when {
            poolID.startsWith("marquee")   -> 1000.0
            poolID.contains("pool1")       -> 700.0
            poolID.contains("pool2")       -> 400.0
            poolID.contains("pool3")       -> 200.0
            poolID.contains("emerging")    -> 400.0
            else                           -> null
        } ?: return
        if (amount >= threshold) {
            bidWarFiredForCurrentPlayer = true
            val chatMsg = ChatMessage(
                id          = "bidwar-${System.currentTimeMillis()}",
                type        = "bidding_war",
                playerName  = player.displayName,
                playerImage = player.displayImage,
                poolID      = player.poolID,
                amount      = amount,
                timestamp   = currentTimeString()
            )
            _state.update { s -> s.copy(chatMessages = (s.chatMessages + chatMsg).takeLast(200)) }
        }
    }

    private fun buildSoldChatMessage(evt: com.playauction.app.data.socket.PlayerSoldEvent): ChatMessage {
        val price   = evt.winningBid?.amount ?: 0.0
        val poolID  = evt.player.poolID?.lowercase() ?: ""
        val verdict = when {
            poolID.startsWith("marquee") || poolID.contains("pool1") -> when {
                price < 500  -> "Steal buy! 💎"
                price <= 1000-> "Good buy! ✅"
                else         -> "Huge investment! 🔥"
            }
            poolID.contains("emerging") -> when {
                price < 200  -> "Good investment! ✅"
                price <= 500 -> "Future asset! 🌟"
                else         -> "Huge investment! 🔥"
            }
            else -> if (price < 400) "Good buy! ✅" else "Huge investment! 🔥"
        }
        return ChatMessage(
            id          = "sold-${System.currentTimeMillis()}",
            type        = "sold",
            senderName  = evt.winningBid?.ownerName,
            senderTeam  = evt.winningBid?.teamName,
            senderColor = evt.winningBid?.teamColor,
            senderLogo  = evt.winningBid?.teamLogo,
            playerName  = evt.player.displayName,
            playerImage = evt.player.displayImage,
            amount      = price,
            basePrice   = evt.player.effectiveBasePrice,
            verdict     = verdict,
            slogan      = if (evt.winningBid?.isRtm == true) "Acquired via RTM" else null,
            teamShort   = resolveTeamShort(evt.winningBid?.teamName),
            ownerName   = evt.winningBid?.ownerName,
            timestamp   = currentTimeString()
        )
    }

    private fun buildUnsoldChatMessage(player: Player): ChatMessage {
        val poolID = player.poolID?.lowercase() ?: ""
        val isHighValue = poolID.startsWith("marquee") || poolID.contains("pool1")
        return ChatMessage(
            id          = "${if (isHighValue) "shocking-" else ""}unsold-${System.currentTimeMillis()}",
            type        = if (isHighValue) "shocking_unsold" else "unsold",
            playerName  = player.displayName,
            playerImage = player.displayImage,
            poolID      = player.poolID,
            basePrice   = player.effectiveBasePrice,
            timestamp   = currentTimeString()
        )
    }

    private fun resolveTeamShort(teamName: String?): String? {
        if (teamName == null) return null
        val shorts = mapOf(
            "Mumbai Indians" to "MI", "Chennai Super Kings" to "CSK",
            "Royal Challengers Bengaluru" to "RCB", "Royal Challengers Bangalore" to "RCB",
            "Kolkata Knight Riders" to "KKR", "Delhi Capitals" to "DC",
            "Punjab Kings" to "PBKS", "Rajasthan Royals" to "RR",
            "Sunrisers Hyderabad" to "SRH", "Lucknow Super Giants" to "LSG",
            "Gujarat Titans" to "GT", "Deccan Chargers" to "DCG",
            "Gujarat Giants" to "GG", "UP Warriorz" to "UPW",
            "Durban's Super Giants" to "DSG", "Joburg Super Kings" to "JSK",
            "MI Cape Town" to "MICT", "Paarl Royals" to "PR",
            "Pretoria Capitals" to "PC", "Sunrisers Eastern Cape" to "SEC"
        )
        return shorts[teamName] ?: teamName.take(3).uppercase()
    }

    private fun isLegendPlayer(name: String, league: String): Boolean {
        val legends = setOf(
            "Sachin Tendulkar","MS Dhoni","Virat Kohli","Rohit Sharma","AB de Villiers",
            "Chris Gayle","Lasith Malinga","Suresh Raina","Harbhajan Singh","Yuvraj Singh",
            "Quinton de Kock","Kagiso Rabada","Faf du Plessis"
        )
        return legends.any { it.equals(name, ignoreCase = true) }
    }

    private fun currentTimeString(): String {
        val cal = java.util.Calendar.getInstance()
        return String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
    }

    // ─── Outgoing Actions ────────────────────────────────────────────────────

    fun placeBid(amount: Double) {
        socketManager.placeBid(_state.value.roomCode, amount)
    }

    fun getNextBidAmounts(): List<Double> {
        val s = _state.value
        val player = s.currentPlayer ?: return emptyList()
        return BidRulesEngine.getBidOptions(s.currentBid.amount, player.effectiveBasePrice, player.poolID ?: "", s.league)
    }

    fun sendChat(message: String) = socketManager.sendChatMessage(_state.value.roomCode, message)

    fun pauseAuction()  = socketManager.pauseAuction(_state.value.roomCode)
    fun resumeAuction() = socketManager.resumeAuction(_state.value.roomCode)
    fun skipPlayer()    = socketManager.skipPlayer(_state.value.roomCode)
    fun skipPool()      = socketManager.skipPool(_state.value.roomCode)

    fun forceEndAuction()   = socketManager.forceEndAuction(_state.value.roomCode)
    fun cancelForceEnd()    = socketManager.cancelForceEnd(_state.value.roomCode)
    fun acknowledgeForceEnd()= socketManager.acknowledgeForceEnd(_state.value.roomCode)

    fun submitVotes(playerIds: List<String>) = socketManager.submitInterestVotes(_state.value.roomCode, playerIds)

    fun requestTeamRoster(teamId: String) {
        _state.update { it.copy(expandedTeamId = teamId) }
        if (_state.value.teamRosters[teamId] == null) socketManager.requestTeamRoster(teamId)
    }

    fun rtmDecision(useRtm: Boolean) = socketManager.rtmDecision(_state.value.roomCode, useRtm)

    fun updateTimer(duration: Int) = socketManager.updateTimer(_state.value.roomCode, duration)

    fun approveParticipation(userId: String?, socketId: String?) =
        socketManager.approveParticipation(_state.value.roomCode, userId, socketId)

    fun rejectParticipation(userId: String?, socketId: String?) =
        socketManager.rejectParticipation(_state.value.roomCode, userId, socketId)

    fun toggleCoHost(userId: String) = socketManager.toggleCoHost(_state.value.roomCode, userId)

    fun requestSync() = socketManager.requestAuctionSync(_state.value.roomCode)

    fun clearError() = _state.update { it.copy(error = null) }

    fun setShowPoolModal(show: Boolean) = _state.update { it.copy(showPoolModal = show) }

    fun setPoolTab(tab: String) = _state.update { it.copy(poolTab = tab) }
}
