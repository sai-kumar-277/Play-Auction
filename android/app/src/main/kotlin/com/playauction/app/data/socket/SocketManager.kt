package com.playauction.app.data.socket

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.playauction.app.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SocketManager"

// ─── Exact Backend Socket.io Event Names ──────────────────────────────────────
private object Events {
    const val CONNECT = Socket.EVENT_CONNECT
    const val DISCONNECT = Socket.EVENT_DISCONNECT
    const val CONNECT_ERROR = Socket.EVENT_CONNECT_ERROR

    // Room / Lobby
    const val ROOM_CREATED = "room_created"
    const val ROOM_JOINED = "room_joined"
    const val AVAILABLE_TEAMS = "available_teams"
    const val LOBBY_UPDATE = "lobby_update"
    const val SPECTATOR_UPDATE = "spectator_update"
    const val AUCTION_STARTED = "auction_started"
    const val PUBLIC_ROOMS_UPDATE = "public_rooms_update"
    const val LOBBY_SETTINGS_UPDATED = "lobby_settings_updated"

    // Auction
    const val NEW_PLAYER = "new_player"
    const val BID_PLACED = "bid_placed"
    const val AUCTION_STATE_SYNC = "auction_state_sync"
    const val PLAYER_SOLD = "player_sold"
    const val PLAYER_UNSOLD = "player_unsold"
    const val TIMER_TICK = "timer_tick"
    const val AUCTION_PAUSED = "auction_paused"
    const val AUCTION_RESUMED = "auction_resumed"
    const val AUCTION_FINISHED = "auction_finished"
    const val SETTINGS_UPDATED = "settings_updated"
    const val AUCTION_END_REQUESTED = "auction_end_requested"
    const val END_ACKNOWLEDGEMENT_UPDATE = "end_acknowledgement_update"
    const val AUCTION_END_CANCELLED = "auction_end_cancelled"

    // Chat
    const val RECEIVE_CHAT_MESSAGE = "receive_chat_message"
    const val CHAT_MESSAGE = "chat_message"

    // Voting
    const val INTEREST_VOTING_STARTED = "interest_voting_started"
    const val INTEREST_VOTING_COMPLETED = "interest_voting_completed"

    // Quiz
    const val QUIZ_PHASE_STARTED = "quiz_phase_started"
    const val QUIZ_QUESTION = "quiz_question"
    const val QUIZ_TIMER_TICK = "quiz_timer_tick"
    const val QUIZ_LEADERBOARD = "quiz_leaderboard_update"
    const val QUIZ_ANSWER_RESULT = "quiz_answer_result"
    const val QUIZ_QUESTION_CLOSED = "quiz_question_closed"
    const val QUIZ_PHASE_ENDED = "quiz_phase_ended"

    // Evaluation / RTM
    const val EVALUATION_STARTED = "evaluation_started"
    const val EVALUATION_TIMER_TICK = "evaluation_timer_tick"
    const val RTM_STATE = "rtm_state"
    const val RTM_TRIGGERED = "rtm_triggered"

    // Permissions / Host
    const val ROOM_DISBANDED = "room_disbanded"
    const val ERROR = "error"
    const val NAME_TAKEN = "name_taken"
    const val KICKED_FROM_ROOM = "kicked_from_room"
    const val HOST_CHANGED = "host_changed"
    const val COHOSTS_UPDATED = "cohosts_updated"
    const val PLAYER_STATUS_UPDATE = "player_status_update"
    const val TEAM_CLAIMED_SUCCESS = "team_claimed_success"
    const val TEAM_ROSTER_DATA = "team_roster_data"
    const val JOIN_REQUESTS_UPDATE = "join_requests_update"
    const val PARTICIPATION_APPROVED = "participation_approved"
    const val PARTICIPATION_REJECTED = "participation_rejected"

    // Retention
    const val RETENTION_SQUADS_LOADED = "retention_squads_loaded"
    const val MY_TEAM_RETENTION_SQUAD = "my_team_retention_squad"
    const val RETENTIONS_CONFIRMED = "retentions_confirmed"
    const val RETENTIONS_ALREADY_SUBMITTED = "retentions_already_submitted"
    const val LOBBY_RETENTION_UPDATE = "lobby_retention_update"
}

// ─── Exact Backend Emits ──────────────────────────────────────────────────────
private object Emit {
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val CLAIM_TEAM = "claim_team"
    const val START_AUCTION = "start_auction"
    const val PLACE_BID = "place_bid"
    const val SKIP_PLAYER = "skip_player"
    const val SKIP_POOL = "skip_pool"
    const val SEND_CHAT = "send_chat_message"
    const val REQUEST_SYNC = "request_auction_sync"
    const val REQUEST_TEAM_ROSTER = "request_team_roster"
    const val SUBMIT_RETENTIONS = "submit_retentions"
    const val SUBMIT_INTEREST_VOTES = "submit_interest_votes"
    const val SUBMIT_QUIZ_ANSWER = "submit_quiz_answer"
    const val LEAVE_ROOM = "leave_room"
    const val FETCH_PUBLIC_ROOMS = "fetch_public_rooms"
    const val REQUEST_PARTICIPATION = "request_participation"
    const val APPROVE_PARTICIPATION = "approve_participation"
    const val REJECT_PARTICIPATION = "reject_participation"
    const val TOGGLE_CO_HOST = "toggle_cohost"
    const val CHANGE_OWNER_NAME = "change_owner_name"
    const val UPDATE_LOBBY_SETTINGS = "update_lobby_settings"
    const val UPDATE_TIMER = "update_timer"
    const val RTM_DECISION = "rtm_decision"
    const val PAUSE_AUCTION = "pause_auction"
    const val RESUME_AUCTION = "resume_auction"
    const val FORCE_END_AUCTION = "force_end_auction"
    const val CANCEL_FORCE_END = "cancel_force_end"
    const val ACKNOWLEDGE_FORCE_END = "acknowledge_force_end"
}

@Singleton
class SocketManager @Inject constructor(
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {
    private var socket: Socket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentSocketId: String? get() = socket?.id()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // ── Incoming event flows ─────────────────────────────────────────────────
    val onRoomCreated = MutableSharedFlow<RoomCreatedEvent>(replay = 1)
    val onRoomJoined = MutableSharedFlow<RoomState>(replay = 1)
    val onAvailableTeams = MutableSharedFlow<AvailableTeamsEvent>(replay = 1)
    val onLobbyUpdate = MutableSharedFlow<LobbyUpdateEvent>(replay = 0)
    val onSpectatorUpdate = MutableSharedFlow<SpectatorUpdateEvent>(replay = 0)
    val onAuctionStarted = MutableSharedFlow<RoomState>(replay = 1)
    val onPublicRoomsUpdate = MutableSharedFlow<List<PublicRoom>>(replay = 1)
    val onLobbySettingsUpdated = MutableSharedFlow<LobbySettingsEvent>(replay = 0)

    val onNewPlayer = MutableSharedFlow<NewPlayerEvent>(replay = 1)
    val onBidPlaced = MutableSharedFlow<BidPlacedEvent>(replay = 0)
    val onAuctionStateSync = MutableSharedFlow<AuctionStateSyncEvent>(replay = 1)
    val onPlayerSold = MutableSharedFlow<PlayerSoldEvent>(replay = 0)
    val onPlayerUnsold = MutableSharedFlow<PlayerUnsoldEvent>(replay = 0)
    val onTimerTick = MutableSharedFlow<Int>(replay = 0)
    val onAuctionPaused = MutableSharedFlow<Unit>(replay = 0)
    val onAuctionResumed = MutableSharedFlow<AuctionResumedEvent>(replay = 0)
    val onAuctionFinished = MutableSharedFlow<AuctionFinishedEvent>(replay = 1)
    val onSettingsUpdated = MutableSharedFlow<SettingsUpdatedEvent>(replay = 0)
    val onAuctionEndRequested = MutableSharedFlow<AuctionEndRequestEvent>(replay = 0)
    val onEndAcknowledgementUpdate = MutableSharedFlow<List<String>>(replay = 0)
    val onAuctionEndCancelled = MutableSharedFlow<Unit>(replay = 0)

    val onChatMessage = MutableSharedFlow<ChatMessage>(replay = 0)
    val onInterestVotingStarted = MutableSharedFlow<InterestVotingEvent>(replay = 1)
    val onInterestVotingCompleted = MutableSharedFlow<VotingCompletedEvent>(replay = 0)

    val onQuizPhaseStarted = MutableSharedFlow<QuizStartedEvent>(replay = 1)
    val onQuizQuestion = MutableSharedFlow<QuizQuestion>(replay = 1)
    val onQuizTimerTick = MutableSharedFlow<QuizTimerEvent>(replay = 0)
    val onQuizLeaderboard = MutableSharedFlow<List<QuizLeaderboardEntry>>(replay = 0)
    val onQuizAnswerResult = MutableSharedFlow<QuizAnswerResult>(replay = 0)
    val onQuizQuestionClosed = MutableSharedFlow<QuizQuestionClosed>(replay = 0)
    val onQuizPhaseEnded = MutableSharedFlow<List<QuizLeaderboardEntry>>(replay = 1)

    val onEvaluationStarted = MutableSharedFlow<Int>(replay = 1)
    val onEvaluationTimerTick = MutableSharedFlow<Int>(replay = 0)
    val onRtmState = MutableSharedFlow<RtmState?>(replay = 1)

    val onRoomDisbanded = MutableSharedFlow<Unit>(replay = 0)
    val onError = MutableSharedFlow<String>(replay = 0)
    val onNameTaken = MutableSharedFlow<String>(replay = 0)
    val onKickedFromRoom = MutableSharedFlow<Unit>(replay = 0)
    val onHostChanged = MutableSharedFlow<HostChangedEvent>(replay = 0)
    val onCohostsUpdated = MutableSharedFlow<List<String>>(replay = 0)
    val onPlayerStatusUpdate = MutableSharedFlow<Map<String, Boolean>>(replay = 0)
    val onTeamClaimedSuccess = MutableSharedFlow<Unit>(replay = 0)
    val onTeamRosterData = MutableSharedFlow<TeamRosterEvent>(replay = 0)
    val onJoinRequestsUpdate = MutableSharedFlow<JoinRequestsUpdateEvent>(replay = 0)
    val onParticipationApproved = MutableSharedFlow<Unit>(replay = 0)
    val onParticipationRejected = MutableSharedFlow<Unit>(replay = 0)

    val onRetentionSquadsLoaded = MutableSharedFlow<RetentionSquadsEvent>(replay = 1)
    val onMyTeamRetentionSquad = MutableSharedFlow<MyRetentionSquadEvent>(replay = 1)
    val onRetentionsConfirmed = MutableSharedFlow<RetentionsConfirmedEvent>(replay = 0)
    val onRetentionsAlreadySubmitted = MutableSharedFlow<RetentionsAlreadySubmittedEvent>(replay = 0)
    val onLobbyRetentionUpdate = MutableSharedFlow<LobbyRetentionUpdateEvent>(replay = 0)

    // ── Connect & Register Listeners ─────────────────────────────────────────

    fun connect(serverUrl: String, token: String? = null) {
        if (socket?.connected() == true) return
        try {
            val opts = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 15
                reconnectionDelay = 1000
                reconnectionDelayMax = 30000
                randomizationFactor = 0.5
                transports = arrayOf("websocket", "polling")
                if (!token.isNullOrBlank()) {
                    auth = mapOf("token" to token)
                }
            }

            socket = IO.socket(URI.create(serverUrl), opts).apply {
                on(Events.CONNECT) {
                    _isConnected.value = true
                    Log.d(TAG, "Connected. Socket ID: ${id()}")
                }
                on(Events.DISCONNECT) {
                    _isConnected.value = false
                    Log.d(TAG, "Disconnected")
                }
                on(Events.CONNECT_ERROR) { args ->
                    Log.e(TAG, "Connect error: ${args.firstOrNull()}")
                }

                registerEventListeners(this)
                connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize socket", e)
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _isConnected.value = false
    }

    private fun registerEventListeners(s: Socket) {
        // Room & Lobby
        s.on(Events.ROOM_CREATED) { args -> parseAndEmit<RoomCreatedEvent>(args, onRoomCreated) }
        s.on(Events.ROOM_JOINED) { args ->
            // Payload is { roomCode, state } -> extract state
            try {
                val jsonStr = args.firstOrNull()?.toString() ?: return@on
                val obj = JSONObject(jsonStr)
                val stateObj = obj.optJSONObject("state") ?: obj
                val state = gson.fromJson(stateObj.toString(), RoomState::class.java)
                scope.launch { onRoomJoined.emit(state) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing room_joined", e)
            }
        }
        s.on(Events.AVAILABLE_TEAMS) { args -> parseAndEmit<AvailableTeamsEvent>(args, onAvailableTeams) }
        s.on(Events.LOBBY_UPDATE) { args -> parseAndEmit<LobbyUpdateEvent>(args, onLobbyUpdate) }
        s.on(Events.SPECTATOR_UPDATE) { args -> parseAndEmit<SpectatorUpdateEvent>(args, onSpectatorUpdate) }
        s.on(Events.AUCTION_STARTED) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString() ?: return@on
                val obj = JSONObject(jsonStr)
                val stateObj = obj.optJSONObject("state") ?: obj
                val state = gson.fromJson(stateObj.toString(), RoomState::class.java)
                scope.launch { onAuctionStarted.emit(state) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing auction_started", e)
            }
        }
        s.on(Events.PUBLIC_ROOMS_UPDATE) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString() ?: return@on
                val obj = JSONObject(jsonStr)
                val roomsArr = obj.optJSONArray("rooms") ?: JSONArray()
                val type = object : TypeToken<List<PublicRoom>>() {}.type
                val rooms = gson.fromJson<List<PublicRoom>>(roomsArr.toString(), type)
                scope.launch { onPublicRoomsUpdate.emit(rooms) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing public_rooms_update", e)
            }
        }
        s.on(Events.LOBBY_SETTINGS_UPDATED) { args -> parseAndEmit<LobbySettingsEvent>(args, onLobbySettingsUpdated) }

        // Auction events
        s.on(Events.NEW_PLAYER) { args -> parseAndEmit<NewPlayerEvent>(args, onNewPlayer) }
        s.on(Events.BID_PLACED) { args -> parseAndEmit<BidPlacedEvent>(args, onBidPlaced) }
        s.on(Events.AUCTION_STATE_SYNC) { args -> parseAndEmit<AuctionStateSyncEvent>(args, onAuctionStateSync) }
        s.on(Events.PLAYER_SOLD) { args -> parseAndEmit<PlayerSoldEvent>(args, onPlayerSold) }
        s.on(Events.PLAYER_UNSOLD) { args -> parseAndEmit<PlayerUnsoldEvent>(args, onPlayerUnsold) }
        s.on(Events.TIMER_TICK) { args ->
            try {
                val raw = args.firstOrNull()
                val timer = when (raw) {
                    is Number -> raw.toInt()
                    is JSONObject -> raw.optInt("timer", raw.optInt("t", 10))
                    else -> raw?.toString()?.toIntOrNull() ?: 10
                }
                scope.launch { onTimerTick.emit(timer) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing timer_tick", e)
            }
        }
        s.on(Events.AUCTION_PAUSED) { scope.launch { onAuctionPaused.emit(Unit) } }
        s.on(Events.AUCTION_RESUMED) { args -> parseAndEmit<AuctionResumedEvent>(args, onAuctionResumed) }
        s.on(Events.AUCTION_FINISHED) { args -> parseAndEmit<AuctionFinishedEvent>(args, onAuctionFinished) }
        s.on(Events.SETTINGS_UPDATED) { args -> parseAndEmit<SettingsUpdatedEvent>(args, onSettingsUpdated) }
        s.on(Events.AUCTION_END_REQUESTED) { args -> parseAndEmit<AuctionEndRequestEvent>(args, onAuctionEndRequested) }
        s.on(Events.END_ACKNOWLEDGEMENT_UPDATE) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString() ?: return@on
                val obj = JSONObject(jsonStr)
                val arr = obj.optJSONArray("acknowledgedBy") ?: JSONArray()
                val type = object : TypeToken<List<String>>() {}.type
                val list = gson.fromJson<List<String>>(arr.toString(), type)
                scope.launch { onEndAcknowledgementUpdate.emit(list) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing end_acknowledgement_update", e)
            }
        }
        s.on(Events.AUCTION_END_CANCELLED) { scope.launch { onAuctionEndCancelled.emit(Unit) } }

        // Chat
        val chatHandler: Emitter.Listener = Emitter.Listener { args -> parseAndEmit<ChatMessage>(args, onChatMessage) }
        s.on(Events.RECEIVE_CHAT_MESSAGE, chatHandler)
        s.on(Events.CHAT_MESSAGE, chatHandler)

        // Voting
        s.on(Events.INTEREST_VOTING_STARTED) { args -> parseAndEmit<InterestVotingEvent>(args, onInterestVotingStarted) }
        s.on(Events.INTEREST_VOTING_COMPLETED) { args -> parseAndEmit<VotingCompletedEvent>(args, onInterestVotingCompleted) }

        // Quiz
        s.on(Events.QUIZ_PHASE_STARTED) { args -> parseAndEmit<QuizStartedEvent>(args, onQuizPhaseStarted) }
        s.on(Events.QUIZ_QUESTION) { args -> parseAndEmit<QuizQuestion>(args, onQuizQuestion) }
        s.on(Events.QUIZ_TIMER_TICK) { args -> parseAndEmit<QuizTimerEvent>(args, onQuizTimerTick) }
        s.on(Events.QUIZ_LEADERBOARD) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString() ?: return@on
                val obj = JSONObject(jsonStr)
                val arr = obj.optJSONArray("leaderboard") ?: JSONArray()
                val type = object : TypeToken<List<QuizLeaderboardEntry>>() {}.type
                val lb = gson.fromJson<List<QuizLeaderboardEntry>>(arr.toString(), type)
                scope.launch { onQuizLeaderboard.emit(lb) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing quiz leaderboard", e)
            }
        }
        s.on(Events.QUIZ_ANSWER_RESULT) { args -> parseAndEmit<QuizAnswerResult>(args, onQuizAnswerResult) }
        s.on(Events.QUIZ_QUESTION_CLOSED) { args -> parseAndEmit<QuizQuestionClosed>(args, onQuizQuestionClosed) }
        s.on(Events.QUIZ_PHASE_ENDED) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString() ?: return@on
                val obj = JSONObject(jsonStr)
                val arr = obj.optJSONArray("leaderboard") ?: JSONArray()
                val type = object : TypeToken<List<QuizLeaderboardEntry>>() {}.type
                val lb = gson.fromJson<List<QuizLeaderboardEntry>>(arr.toString(), type)
                scope.launch { onQuizPhaseEnded.emit(lb) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing quiz_phase_ended", e)
            }
        }

        // Evaluation & RTM
        s.on(Events.EVALUATION_STARTED) { args ->
            val timer = (args.firstOrNull() as? JSONObject)?.optInt("timer", 90) ?: 90
            scope.launch { onEvaluationStarted.emit(timer) }
        }
        s.on(Events.EVALUATION_TIMER_TICK) { args ->
            val timer = (args.firstOrNull() as? JSONObject)?.optInt("timer", 0) ?: 0
            scope.launch { onEvaluationTimerTick.emit(timer) }
        }
        s.on(Events.RTM_STATE) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString()
                val parsed = if (jsonStr != null) gson.fromJson(jsonStr, RtmState::class.java) else null
                scope.launch { onRtmState.emit(parsed) }
            } catch (_: Exception) {}
        }
        s.on(Events.RTM_TRIGGERED) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString()
                val parsed = if (jsonStr != null) gson.fromJson(jsonStr, RtmState::class.java) else null
                scope.launch { onRtmState.emit(parsed) }
            } catch (_: Exception) {}
        }

        // Host / Session controls
        s.on(Events.ROOM_DISBANDED) { scope.launch { onRoomDisbanded.emit(Unit) } }
        s.on(Events.ERROR) { args ->
            val msg = args.firstOrNull()?.toString() ?: "Unknown socket error"
            scope.launch { onError.emit(msg) }
        }
        s.on(Events.NAME_TAKEN) { args ->
            val msg = (args.firstOrNull() as? JSONObject)?.optString("message") ?: "Name already taken"
            scope.launch { onNameTaken.emit(msg) }
        }
        s.on(Events.KICKED_FROM_ROOM) { scope.launch { onKickedFromRoom.emit(Unit) } }
        s.on(Events.HOST_CHANGED) { args -> parseAndEmit<HostChangedEvent>(args, onHostChanged) }
        s.on(Events.COHOSTS_UPDATED) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString() ?: return@on
                val obj = JSONObject(jsonStr)
                val arr = obj.optJSONArray("coHostUserIds") ?: JSONArray()
                val type = object : TypeToken<List<String>>() {}.type
                val list = gson.fromJson<List<String>>(arr.toString(), type)
                scope.launch { onCohostsUpdated.emit(list) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing cohosts_updated", e)
            }
        }
        s.on(Events.PLAYER_STATUS_UPDATE) { args ->
            try {
                val jsonStr = args.firstOrNull()?.toString() ?: return@on
                val obj = JSONObject(jsonStr)
                val mapObj = obj.optJSONObject("onlineMap") ?: obj
                val type = object : TypeToken<Map<String, Boolean>>() {}.type
                val map = gson.fromJson<Map<String, Boolean>>(mapObj.toString(), type)
                scope.launch { onPlayerStatusUpdate.emit(map) }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing player_status_update", e)
            }
        }
        s.on(Events.TEAM_CLAIMED_SUCCESS) { scope.launch { onTeamClaimedSuccess.emit(Unit) } }
        s.on(Events.TEAM_ROSTER_DATA) { args -> parseAndEmit<TeamRosterEvent>(args, onTeamRosterData) }
        s.on(Events.JOIN_REQUESTS_UPDATE) { args -> parseAndEmit<JoinRequestsUpdateEvent>(args, onJoinRequestsUpdate) }
        s.on(Events.PARTICIPATION_APPROVED) { scope.launch { onParticipationApproved.emit(Unit) } }
        s.on(Events.PARTICIPATION_REJECTED) { scope.launch { onParticipationRejected.emit(Unit) } }

        // Retentions
        s.on(Events.RETENTION_SQUADS_LOADED) { args -> parseAndEmit<RetentionSquadsEvent>(args, onRetentionSquadsLoaded) }
        s.on(Events.MY_TEAM_RETENTION_SQUAD) { args -> parseAndEmit<MyRetentionSquadEvent>(args, onMyTeamRetentionSquad) }
        s.on(Events.RETENTIONS_CONFIRMED) { args -> parseAndEmit<RetentionsConfirmedEvent>(args, onRetentionsConfirmed) }
        s.on(Events.RETENTIONS_ALREADY_SUBMITTED) { args -> parseAndEmit<RetentionsAlreadySubmittedEvent>(args, onRetentionsAlreadySubmitted) }
        s.on(Events.LOBBY_RETENTION_UPDATE) { args -> parseAndEmit<LobbyRetentionUpdateEvent>(args, onLobbyRetentionUpdate) }
    }

    private inline fun <reified T> parseAndEmit(args: Array<Any>, flow: MutableSharedFlow<T>) {
        try {
            val jsonStr = args.firstOrNull()?.toString() ?: return
            val parsed = gson.fromJson(jsonStr, T::class.java)
            if (parsed != null) {
                scope.launch { flow.emit(parsed) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse ${T::class.simpleName}: ${e.message}")
        }
    }

    // ─── Outgoing Emits ──────────────────────────────────────────────────────

    fun createRoom(roomType: String, league: String, currency: String, auctionYear: String?) {
        val payload = JSONObject().apply {
            put("roomType", roomType)
            put("league", league)
            put("currency", currency)
            put("auctionYear", auctionYear ?: JSONObject.NULL)
        }
        socket?.emit(Emit.CREATE_ROOM, payload)
    }

    fun joinRoom(roomCode: String, asSpectator: Boolean = false) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            if (asSpectator) put("asSpectator", true)
        }
        socket?.emit(Emit.JOIN_ROOM, payload)
    }

    fun claimTeam(roomCode: String, teamId: String) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("teamId", teamId)
        }
        socket?.emit(Emit.CLAIM_TEAM, payload)
    }

    fun startAuction(roomCode: String) {
        socket?.emit(Emit.START_AUCTION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun placeBid(roomCode: String, amount: Double) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("amount", amount)
        }
        socket?.emit(Emit.PLACE_BID, payload)
    }

    fun skipPlayer(roomCode: String) {
        socket?.emit(Emit.SKIP_PLAYER, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun skipPool(roomCode: String) {
        socket?.emit(Emit.SKIP_POOL, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun sendChatMessage(roomCode: String, message: String) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("message", message)
        }
        socket?.emit(Emit.SEND_CHAT, payload)
    }

    fun requestAuctionSync(roomCode: String) {
        socket?.emit(Emit.REQUEST_SYNC, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun requestTeamRoster(teamId: String) {
        socket?.emit(Emit.REQUEST_TEAM_ROSTER, JSONObject().apply { put("teamId", teamId) })
    }

    fun submitRetentions(roomCode: String, retainedPlayers: List<String>, customPrices: Map<String, Double>) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("retainedPlayers", JSONArray(retainedPlayers))
            put("customPrices", JSONObject(customPrices))
        }
        socket?.emit(Emit.SUBMIT_RETENTIONS, payload)
    }

    fun submitInterestVotes(roomCode: String, playerIds: List<String>) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("playerIds", JSONArray(playerIds))
        }
        socket?.emit(Emit.SUBMIT_INTEREST_VOTES, payload)
    }

    fun submitQuizAnswer(roomCode: String, quizIndex: Int, answerIndex: Int) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("quizIndex", quizIndex)
            put("answerIndex", answerIndex)
        }
        socket?.emit(Emit.SUBMIT_QUIZ_ANSWER, payload)
    }

    fun leaveRoom(roomCode: String) {
        socket?.emit(Emit.LEAVE_ROOM, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun fetchPublicRooms(league: String? = null) {
        val payload = JSONObject().apply {
            if (!league.isNullOrBlank()) put("league", league)
        }
        socket?.emit(Emit.FETCH_PUBLIC_ROOMS, payload)
    }

    fun requestParticipation(roomCode: String) {
        socket?.emit(Emit.REQUEST_PARTICIPATION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun approveParticipation(roomCode: String, targetUserId: String?, targetSocketId: String?) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("targetUserId", targetUserId ?: JSONObject.NULL)
            put("targetSocketId", targetSocketId ?: JSONObject.NULL)
        }
        socket?.emit(Emit.APPROVE_PARTICIPATION, payload)
    }

    fun rejectParticipation(roomCode: String, targetUserId: String?, targetSocketId: String?) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("targetUserId", targetUserId ?: JSONObject.NULL)
            put("targetSocketId", targetSocketId ?: JSONObject.NULL)
        }
        socket?.emit(Emit.REJECT_PARTICIPATION, payload)
    }

    fun toggleCoHost(roomCode: String, userId: String) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("userId", userId)
        }
        socket?.emit(Emit.TOGGLE_CO_HOST, payload)
    }

    fun changeOwnerName(roomCode: String, newName: String) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("newName", newName)
        }
        socket?.emit(Emit.CHANGE_OWNER_NAME, payload)
    }

    fun updateLobbySettings(roomCode: String, allowSpectators: Boolean, maxSpectators: Int, teamCount: Int) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("allowSpectators", allowSpectators)
            put("maxSpectators", maxSpectators)
            put("teamCount", teamCount)
        }
        socket?.emit(Emit.UPDATE_LOBBY_SETTINGS, payload)
    }

    fun updateTimer(roomCode: String, timerDuration: Int) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("timerDuration", timerDuration)
        }
        socket?.emit(Emit.UPDATE_TIMER, payload)
    }

    fun rtmDecision(roomCode: String, useRtm: Boolean) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("useRtm", useRtm)
        }
        socket?.emit(Emit.RTM_DECISION, payload)
    }

    fun pauseAuction(roomCode: String) {
        socket?.emit(Emit.PAUSE_AUCTION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun resumeAuction(roomCode: String) {
        socket?.emit(Emit.RESUME_AUCTION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun forceEndAuction(roomCode: String) {
        socket?.emit(Emit.FORCE_END_AUCTION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun cancelForceEnd(roomCode: String) {
        socket?.emit(Emit.CANCEL_FORCE_END, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun acknowledgeForceEnd(roomCode: String) {
        socket?.emit(Emit.ACKNOWLEDGE_FORCE_END, JSONObject().apply { put("roomCode", roomCode) })
    }
}
