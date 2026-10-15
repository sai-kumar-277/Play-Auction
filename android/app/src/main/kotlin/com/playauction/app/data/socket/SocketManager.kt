package com.playauction.app.data.socket

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SocketManager"

// ─── Incoming event name constants ──────────────────────────────────────────
private object Events {
    // Connection
    const val CONNECT = Socket.EVENT_CONNECT
    const val DISCONNECT = Socket.EVENT_DISCONNECT
    const val CONNECT_ERROR = Socket.EVENT_CONNECT_ERROR

    // Room
    const val ROOM_CREATED = "roomCreated"
    const val ROOM_JOINED = "roomJoined"
    const val AVAILABLE_TEAMS = "availableTeams"
    const val LOBBY_UPDATE = "lobbyUpdate"
    const val SPECTATOR_UPDATE = "spectatorUpdate"
    const val AUCTION_STARTED = "auctionStarted"
    const val PUBLIC_ROOMS_UPDATE = "publicRoomsUpdate"
    const val LOBBY_SETTINGS_UPDATED = "lobbySettingsUpdated"

    // Auction
    const val NEW_PLAYER = "newPlayer"
    const val BID_PLACED = "bidPlaced"
    const val AUCTION_STATE_SYNC = "auctionStateSync"
    const val PLAYER_SOLD = "playerSold"
    const val PLAYER_UNSOLD = "playerUnsold"
    const val TIMER_TICK = "timerTick"
    const val AUCTION_PAUSED = "auctionPaused"
    const val AUCTION_RESUMED = "auctionResumed"
    const val AUCTION_FINISHED = "auctionFinished"
    const val SETTINGS_UPDATED = "settingsUpdated"
    const val AUCTION_END_REQUESTED = "auctionEndRequested"
    const val END_ACKNOWLEDGEMENT_UPDATE = "endAcknowledgementUpdate"
    const val AUCTION_END_CANCELLED = "auctionEndCancelled"

    // Chat
    const val CHAT_MESSAGE = "chatMessage"

    // Interest Voting
    const val INTEREST_VOTING_STARTED = "interestVotingStarted"
    const val INTEREST_VOTING_COMPLETED = "interestVotingCompleted"

    // Quiz
    const val QUIZ_PHASE_STARTED = "quizPhaseStarted"
    const val QUIZ_QUESTION = "quizQuestion"
    const val QUIZ_TIMER_TICK = "quizTimerTick"
    const val QUIZ_LEADERBOARD = "quizLeaderboard"
    const val QUIZ_ANSWER_RESULT = "quizAnswerResult"
    const val QUIZ_QUESTION_CLOSED = "quizQuestionClosed"
    const val QUIZ_PHASE_ENDED = "quizPhaseEnded"

    // Evaluation / RTM
    const val EVALUATION_STARTED = "evaluationStarted"
    const val EVALUATION_TIMER_TICK = "evaluationTimerTick"
    const val RTM_STATE = "rtmState"

    // Permissions / Host
    const val ROOM_DISBANDED = "roomDisbanded"
    const val ERROR = "error"
    const val NAME_TAKEN = "nameTaken"
    const val KICKED_FROM_ROOM = "kickedFromRoom"
    const val HOST_CHANGED = "hostChanged"
    const val COHOSTS_UPDATED = "coHostsUpdated"
    const val PLAYER_STATUS_UPDATE = "playerStatusUpdate"
    const val TEAM_CLAIMED_SUCCESS = "teamClaimedSuccess"
    const val TEAM_ROSTER_DATA = "teamRosterData"
    const val JOIN_REQUESTS_UPDATE = "joinRequestsUpdate"
    const val PARTICIPATION_APPROVED = "participationApproved"
    const val PARTICIPATION_REJECTED = "participationRejected"

    // Retention
    const val RETENTION_SQUADS_LOADED = "retentionSquadsLoaded"
    const val MY_TEAM_RETENTION_SQUAD = "myTeamRetentionSquad"
    const val RETENTIONS_CONFIRMED = "retentionsConfirmed"
    const val RETENTIONS_ALREADY_SUBMITTED = "retentionsAlreadySubmitted"
    const val LOBBY_RETENTION_UPDATE = "lobbyRetentionUpdate"
}

// ─── Outgoing event name constants ──────────────────────────────────────────
private object Emit {
    const val CREATE_ROOM = "createRoom"
    const val JOIN_ROOM = "joinRoom"
    const val CLAIM_TEAM = "claimTeam"
    const val START_AUCTION = "startAuction"
    const val PLACE_BID = "placeBid"
    const val SKIP_PLAYER = "skipPlayer"
    const val SKIP_POOL = "skipPool"
    const val SEND_CHAT = "sendChatMessage"
    const val REQUEST_SYNC = "requestAuctionSync"
    const val REQUEST_TEAM_ROSTER = "requestTeamRoster"
    const val SUBMIT_RETENTIONS = "submitRetentions"
    const val SUBMIT_INTEREST_VOTES = "submitInterestVotes"
    const val SUBMIT_QUIZ_ANSWER = "submitQuizAnswer"
    const val LEAVE_ROOM = "leaveRoom"
    const val FETCH_PUBLIC_ROOMS = "fetchPublicRooms"
    const val REQUEST_PARTICIPATION = "requestParticipation"
    const val APPROVE_PARTICIPATION = "approveParticipation"
    const val REJECT_PARTICIPATION = "rejectParticipation"
    const val TOGGLE_CO_HOST = "toggleCoHost"
    const val CHANGE_OWNER_NAME = "changeOwnerName"
    const val UPDATE_LOBBY_SETTINGS = "updateLobbySettings"
    const val UPDATE_TIMER = "updateTimer"
    const val RTM_DECISION = "rtmDecision"
    const val PAUSE_AUCTION = "pauseAuction"
    const val RESUME_AUCTION = "resumeAuction"
    const val FORCE_END_AUCTION = "forceEndAuction"
    const val CANCEL_FORCE_END = "cancelForceEnd"
    const val ACKNOWLEDGE_FORCE_END = "acknowledgeForceEnd"
}

@Singleton
class SocketManager @Inject constructor(
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {

    private var socket: Socket? = null

    // Dedicated scope for emitting to flows from the socket thread
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Connection state ─────────────────────────────────────────────────────
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // ── Incoming event flows ─────────────────────────────────────────────────

    // Room / Lobby
    private val _onRoomCreated = MutableSharedFlow<RoomCreatedEvent>(replay = 1)
    val onRoomCreated: SharedFlow<RoomCreatedEvent> = _onRoomCreated.asSharedFlow()

    private val _onRoomJoined = MutableSharedFlow<RoomState>(replay = 1)
    val onRoomJoined: SharedFlow<RoomState> = _onRoomJoined.asSharedFlow()

    private val _onAvailableTeams = MutableSharedFlow<AvailableTeamsEvent>(replay = 1)
    val onAvailableTeams: SharedFlow<AvailableTeamsEvent> = _onAvailableTeams.asSharedFlow()

    private val _onLobbyUpdate = MutableSharedFlow<LobbyUpdateEvent>(replay = 0)
    val onLobbyUpdate: SharedFlow<LobbyUpdateEvent> = _onLobbyUpdate.asSharedFlow()

    private val _onSpectatorUpdate = MutableSharedFlow<SpectatorUpdateEvent>(replay = 0)
    val onSpectatorUpdate: SharedFlow<SpectatorUpdateEvent> = _onSpectatorUpdate.asSharedFlow()

    private val _onAuctionStarted = MutableSharedFlow<RoomState>(replay = 1)
    val onAuctionStarted: SharedFlow<RoomState> = _onAuctionStarted.asSharedFlow()

    private val _onNewPlayer = MutableSharedFlow<NewPlayerEvent>(replay = 1)
    val onNewPlayer: SharedFlow<NewPlayerEvent> = _onNewPlayer.asSharedFlow()

    private val _onBidPlaced = MutableSharedFlow<BidPlacedEvent>(replay = 0)
    val onBidPlaced: SharedFlow<BidPlacedEvent> = _onBidPlaced.asSharedFlow()

    private val _onAuctionStateSync = MutableSharedFlow<AuctionStateSyncEvent>(replay = 1)
    val onAuctionStateSync: SharedFlow<AuctionStateSyncEvent> = _onAuctionStateSync.asSharedFlow()

    private val _onPlayerSold = MutableSharedFlow<PlayerSoldEvent>(replay = 0)
    val onPlayerSold: SharedFlow<PlayerSoldEvent> = _onPlayerSold.asSharedFlow()

    private val _onPlayerUnsold = MutableSharedFlow<PlayerUnsoldEvent>(replay = 0)
    val onPlayerUnsold: SharedFlow<PlayerUnsoldEvent> = _onPlayerUnsold.asSharedFlow()

    private val _onTimerTick = MutableSharedFlow<Int>(replay = 0)
    val onTimerTick: SharedFlow<Int> = _onTimerTick.asSharedFlow()

    private val _onChatMessage = MutableSharedFlow<ChatMessage>(replay = 0)
    val onChatMessage: SharedFlow<ChatMessage> = _onChatMessage.asSharedFlow()

    private val _onInterestVotingStarted = MutableSharedFlow<InterestVotingEvent>(replay = 1)
    val onInterestVotingStarted: SharedFlow<InterestVotingEvent> = _onInterestVotingStarted.asSharedFlow()

    private val _onInterestVotingCompleted = MutableSharedFlow<VotingCompletedEvent>(replay = 0)
    val onInterestVotingCompleted: SharedFlow<VotingCompletedEvent> = _onInterestVotingCompleted.asSharedFlow()

    private val _onQuizPhaseStarted = MutableSharedFlow<QuizStartedEvent>(replay = 1)
    val onQuizPhaseStarted: SharedFlow<QuizStartedEvent> = _onQuizPhaseStarted.asSharedFlow()

    private val _onQuizQuestion = MutableSharedFlow<QuizQuestion>(replay = 1)
    val onQuizQuestion: SharedFlow<QuizQuestion> = _onQuizQuestion.asSharedFlow()

    private val _onQuizTimerTick = MutableSharedFlow<QuizTimerEvent>(replay = 0)
    val onQuizTimerTick: SharedFlow<QuizTimerEvent> = _onQuizTimerTick.asSharedFlow()

    private val _onQuizLeaderboard = MutableSharedFlow<List<QuizLeaderboardEntry>>(replay = 0)
    val onQuizLeaderboard: SharedFlow<List<QuizLeaderboardEntry>> = _onQuizLeaderboard.asSharedFlow()

    private val _onQuizAnswerResult = MutableSharedFlow<QuizAnswerResult>(replay = 0)
    val onQuizAnswerResult: SharedFlow<QuizAnswerResult> = _onQuizAnswerResult.asSharedFlow()

    private val _onQuizQuestionClosed = MutableSharedFlow<QuizQuestionClosed>(replay = 0)
    val onQuizQuestionClosed: SharedFlow<QuizQuestionClosed> = _onQuizQuestionClosed.asSharedFlow()

    private val _onQuizPhaseEnded = MutableSharedFlow<List<QuizLeaderboardEntry>>(replay = 1)
    val onQuizPhaseEnded: SharedFlow<List<QuizLeaderboardEntry>> = _onQuizPhaseEnded.asSharedFlow()

    private val _onEvaluationStarted = MutableSharedFlow<Int>(replay = 1)
    val onEvaluationStarted: SharedFlow<Int> = _onEvaluationStarted.asSharedFlow()

    private val _onEvaluationTimerTick = MutableSharedFlow<Int>(replay = 0)
    val onEvaluationTimerTick: SharedFlow<Int> = _onEvaluationTimerTick.asSharedFlow()

    private val _onAuctionFinished = MutableSharedFlow<AuctionFinishedEvent>(replay = 1)
    val onAuctionFinished: SharedFlow<AuctionFinishedEvent> = _onAuctionFinished.asSharedFlow()

    private val _onRoomDisbanded = MutableSharedFlow<Unit>(replay = 0)
    val onRoomDisbanded: SharedFlow<Unit> = _onRoomDisbanded.asSharedFlow()

    private val _onError = MutableSharedFlow<String>(replay = 0)
    val onError: SharedFlow<String> = _onError.asSharedFlow()

    private val _onNameTaken = MutableSharedFlow<String>(replay = 0)
    val onNameTaken: SharedFlow<String> = _onNameTaken.asSharedFlow()

    private val _onKickedFromRoom = MutableSharedFlow<Unit>(replay = 0)
    val onKickedFromRoom: SharedFlow<Unit> = _onKickedFromRoom.asSharedFlow()

    private val _onHostChanged = MutableSharedFlow<HostChangedEvent>(replay = 0)
    val onHostChanged: SharedFlow<HostChangedEvent> = _onHostChanged.asSharedFlow()

    private val _onCohostsUpdated = MutableSharedFlow<List<String>>(replay = 0)
    val onCohostsUpdated: SharedFlow<List<String>> = _onCohostsUpdated.asSharedFlow()

    private val _onPlayerStatusUpdate = MutableSharedFlow<Map<String, Boolean>>(replay = 0)
    val onPlayerStatusUpdate: SharedFlow<Map<String, Boolean>> = _onPlayerStatusUpdate.asSharedFlow()

    private val _onTeamClaimedSuccess = MutableSharedFlow<Unit>(replay = 0)
    val onTeamClaimedSuccess: SharedFlow<Unit> = _onTeamClaimedSuccess.asSharedFlow()

    private val _onTeamRosterData = MutableSharedFlow<TeamRosterEvent>(replay = 0)
    val onTeamRosterData: SharedFlow<TeamRosterEvent> = _onTeamRosterData.asSharedFlow()

    private val _onJoinRequestsUpdate = MutableSharedFlow<JoinRequestsUpdateEvent>(replay = 0)
    val onJoinRequestsUpdate: SharedFlow<JoinRequestsUpdateEvent> = _onJoinRequestsUpdate.asSharedFlow()

    private val _onParticipationApproved = MutableSharedFlow<Unit>(replay = 0)
    val onParticipationApproved: SharedFlow<Unit> = _onParticipationApproved.asSharedFlow()

    private val _onParticipationRejected = MutableSharedFlow<Unit>(replay = 0)
    val onParticipationRejected: SharedFlow<Unit> = _onParticipationRejected.asSharedFlow()

    private val _onRetentionSquadsLoaded = MutableSharedFlow<RetentionSquadsEvent>(replay = 1)
    val onRetentionSquadsLoaded: SharedFlow<RetentionSquadsEvent> = _onRetentionSquadsLoaded.asSharedFlow()

    private val _onMyTeamRetentionSquad = MutableSharedFlow<MyRetentionSquadEvent>(replay = 1)
    val onMyTeamRetentionSquad: SharedFlow<MyRetentionSquadEvent> = _onMyTeamRetentionSquad.asSharedFlow()

    private val _onRetentionsConfirmed = MutableSharedFlow<RetentionsConfirmedEvent>(replay = 0)
    val onRetentionsConfirmed: SharedFlow<RetentionsConfirmedEvent> = _onRetentionsConfirmed.asSharedFlow()

    private val _onRetentionsAlreadySubmitted = MutableSharedFlow<RetentionsAlreadySubmittedEvent>(replay = 0)
    val onRetentionsAlreadySubmitted: SharedFlow<RetentionsAlreadySubmittedEvent> = _onRetentionsAlreadySubmitted.asSharedFlow()

    private val _onLobbyRetentionUpdate = MutableSharedFlow<LobbyRetentionUpdateEvent>(replay = 0)
    val onLobbyRetentionUpdate: SharedFlow<LobbyRetentionUpdateEvent> = _onLobbyRetentionUpdate.asSharedFlow()

    private val _onPublicRoomsUpdate = MutableSharedFlow<List<PublicRoom>>(replay = 1)
    val onPublicRoomsUpdate: SharedFlow<List<PublicRoom>> = _onPublicRoomsUpdate.asSharedFlow()

    private val _onAuctionPaused = MutableSharedFlow<Unit>(replay = 0)
    val onAuctionPaused: SharedFlow<Unit> = _onAuctionPaused.asSharedFlow()

    private val _onAuctionResumed = MutableSharedFlow<AuctionResumedEvent>(replay = 0)
    val onAuctionResumed: SharedFlow<AuctionResumedEvent> = _onAuctionResumed.asSharedFlow()

    private val _onSettingsUpdated = MutableSharedFlow<SettingsUpdatedEvent>(replay = 0)
    val onSettingsUpdated: SharedFlow<SettingsUpdatedEvent> = _onSettingsUpdated.asSharedFlow()

    private val _onLobbySettingsUpdated = MutableSharedFlow<LobbySettingsEvent>(replay = 0)
    val onLobbySettingsUpdated: SharedFlow<LobbySettingsEvent> = _onLobbySettingsUpdated.asSharedFlow()

    private val _onAuctionEndRequested = MutableSharedFlow<AuctionEndRequestEvent>(replay = 0)
    val onAuctionEndRequested: SharedFlow<AuctionEndRequestEvent> = _onAuctionEndRequested.asSharedFlow()

    private val _onEndAcknowledgementUpdate = MutableSharedFlow<List<String>>(replay = 0)
    val onEndAcknowledgementUpdate: SharedFlow<List<String>> = _onEndAcknowledgementUpdate.asSharedFlow()

    private val _onAuctionEndCancelled = MutableSharedFlow<Unit>(replay = 0)
    val onAuctionEndCancelled: SharedFlow<Unit> = _onAuctionEndCancelled.asSharedFlow()

    private val _onRtmState = MutableSharedFlow<RtmState?>(replay = 1)
    val onRtmState: SharedFlow<RtmState?> = _onRtmState.asSharedFlow()

    // ── Connection ────────────────────────────────────────────────────────────

    /**
     * Connect to the Socket.io server.
     *
     * @param serverUrl  Full server URL, e.g. "https://api.playauction.com"
     * @param token      JWT bearer token; sent in socket handshake auth map.
     */
    fun connect(serverUrl: String, token: String?) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected — skipping reconnect")
            return
        }

        try {
            val opts = IO.Options().apply {
                auth = buildMap<String, String> {
                    token?.let { put("token", it) }
                }.toMutableMap<String, Any>()
                reconnection = true
                reconnectionAttempts = 15
                reconnectionDelay = 1_000          // 1 s initial delay
                reconnectionDelayMax = 30_000      // 30 s ceiling
                randomizationFactor = 0.5          // jitter factor
                timeout = 20_000
                transports = arrayOf("websocket")
            }

            socket = IO.socket(URI.create(serverUrl), opts).also { s ->
                registerListeners(s)
                s.connect()
                Log.d(TAG, "Socket connecting to $serverUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create socket: ${e.message}", e)
            emit(_onError, "Failed to connect: ${e.message}")
        }
    }

    /** Gracefully disconnect and clean up all listeners. */
    fun disconnect() {
        socket?.let { s ->
            s.off()          // remove all listeners
            s.disconnect()
        }
        socket = null
        _isConnected.value = false
        Log.d(TAG, "Socket disconnected")
    }

    // ── Listener registration ─────────────────────────────────────────────────

    private fun registerListeners(s: Socket) {

        // ── Lifecycle ──────────────────────────────────────────────────────
        s.on(Events.CONNECT) {
            Log.d(TAG, "Socket connected: ${s.id()}")
            _isConnected.value = true
        }

        s.on(Events.DISCONNECT) { args ->
            val reason = args.firstOrNull()?.toString() ?: "unknown"
            Log.d(TAG, "Socket disconnected: $reason")
            _isConnected.value = false
        }

        s.on(Events.CONNECT_ERROR) { args ->
            val err = args.firstOrNull()?.toString() ?: "unknown error"
            Log.e(TAG, "Socket connect error: $err")
            emit(_onError, "Connection error: $err")
        }

        // ── Room / Lobby ───────────────────────────────────────────────────
        s.on(Events.ROOM_CREATED) { args ->
            parseAndEmit<RoomCreatedEvent>(args, _onRoomCreated)
        }
        s.on(Events.ROOM_JOINED) { args ->
            parseAndEmit<RoomState>(args, _onRoomJoined)
        }
        s.on(Events.AVAILABLE_TEAMS) { args ->
            parseAndEmit<AvailableTeamsEvent>(args, _onAvailableTeams)
        }
        s.on(Events.LOBBY_UPDATE) { args ->
            parseAndEmit<LobbyUpdateEvent>(args, _onLobbyUpdate)
        }
        s.on(Events.SPECTATOR_UPDATE) { args ->
            parseAndEmit<SpectatorUpdateEvent>(args, _onSpectatorUpdate)
        }
        s.on(Events.AUCTION_STARTED) { args ->
            parseAndEmit<RoomState>(args, _onAuctionStarted)
        }
        s.on(Events.PUBLIC_ROOMS_UPDATE) { args ->
            parseListAndEmit<PublicRoom>(args, _onPublicRoomsUpdate)
        }
        s.on(Events.LOBBY_SETTINGS_UPDATED) { args ->
            parseAndEmit<LobbySettingsEvent>(args, _onLobbySettingsUpdated)
        }

        // ── Auction ────────────────────────────────────────────────────────
        s.on(Events.NEW_PLAYER) { args ->
            parseAndEmit<NewPlayerEvent>(args, _onNewPlayer)
        }
        s.on(Events.BID_PLACED) { args ->
            parseAndEmit<BidPlacedEvent>(args, _onBidPlaced)
        }
        s.on(Events.AUCTION_STATE_SYNC) { args ->
            parseAndEmit<AuctionStateSyncEvent>(args, _onAuctionStateSync)
        }
        s.on(Events.PLAYER_SOLD) { args ->
            parseAndEmit<PlayerSoldEvent>(args, _onPlayerSold)
        }
        s.on(Events.PLAYER_UNSOLD) { args ->
            parseAndEmit<PlayerUnsoldEvent>(args, _onPlayerUnsold)
        }
        s.on(Events.TIMER_TICK) { args ->
            val seconds = (args.firstOrNull() as? Int) ?: (args.firstOrNull()?.toString()?.toIntOrNull()) ?: 0
            emit(_onTimerTick, seconds)
        }
        s.on(Events.AUCTION_PAUSED) {
            emit(_onAuctionPaused, Unit)
        }
        s.on(Events.AUCTION_RESUMED) { args ->
            parseAndEmit<AuctionResumedEvent>(args, _onAuctionResumed)
        }
        s.on(Events.AUCTION_FINISHED) { args ->
            parseAndEmit<AuctionFinishedEvent>(args, _onAuctionFinished)
        }
        s.on(Events.SETTINGS_UPDATED) { args ->
            parseAndEmit<SettingsUpdatedEvent>(args, _onSettingsUpdated)
        }
        s.on(Events.AUCTION_END_REQUESTED) { args ->
            parseAndEmit<AuctionEndRequestEvent>(args, _onAuctionEndRequested)
        }
        s.on(Events.END_ACKNOWLEDGEMENT_UPDATE) { args ->
            parseListAndEmit<String>(args, _onEndAcknowledgementUpdate)
        }
        s.on(Events.AUCTION_END_CANCELLED) {
            emit(_onAuctionEndCancelled, Unit)
        }

        // ── Chat ───────────────────────────────────────────────────────────
        s.on(Events.CHAT_MESSAGE) { args ->
            parseAndEmit<ChatMessage>(args, _onChatMessage)
        }

        // ── Interest Voting ────────────────────────────────────────────────
        s.on(Events.INTEREST_VOTING_STARTED) { args ->
            parseAndEmit<InterestVotingEvent>(args, _onInterestVotingStarted)
        }
        s.on(Events.INTEREST_VOTING_COMPLETED) { args ->
            parseAndEmit<VotingCompletedEvent>(args, _onInterestVotingCompleted)
        }

        // ── Quiz ───────────────────────────────────────────────────────────
        s.on(Events.QUIZ_PHASE_STARTED) { args ->
            parseAndEmit<QuizStartedEvent>(args, _onQuizPhaseStarted)
        }
        s.on(Events.QUIZ_QUESTION) { args ->
            parseAndEmit<QuizQuestion>(args, _onQuizQuestion)
        }
        s.on(Events.QUIZ_TIMER_TICK) { args ->
            parseAndEmit<QuizTimerEvent>(args, _onQuizTimerTick)
        }
        s.on(Events.QUIZ_LEADERBOARD) { args ->
            parseListAndEmit<QuizLeaderboardEntry>(args, _onQuizLeaderboard)
        }
        s.on(Events.QUIZ_ANSWER_RESULT) { args ->
            parseAndEmit<QuizAnswerResult>(args, _onQuizAnswerResult)
        }
        s.on(Events.QUIZ_QUESTION_CLOSED) { args ->
            parseAndEmit<QuizQuestionClosed>(args, _onQuizQuestionClosed)
        }
        s.on(Events.QUIZ_PHASE_ENDED) { args ->
            parseListAndEmit<QuizLeaderboardEntry>(args, _onQuizPhaseEnded)
        }

        // ── Evaluation / RTM ───────────────────────────────────────────────
        s.on(Events.EVALUATION_STARTED) { args ->
            val seconds = (args.firstOrNull() as? Int) ?: (args.firstOrNull()?.toString()?.toIntOrNull()) ?: 0
            emit(_onEvaluationStarted, seconds)
        }
        s.on(Events.EVALUATION_TIMER_TICK) { args ->
            val seconds = (args.firstOrNull() as? Int) ?: (args.firstOrNull()?.toString()?.toIntOrNull()) ?: 0
            emit(_onEvaluationTimerTick, seconds)
        }
        s.on(Events.RTM_STATE) { args ->
            val json = args.firstOrNull()
            if (json == null || json.toString() == "null") {
                emit(_onRtmState, null)
            } else {
                val event = safeParse<RtmState>(json.toString())
                if (event != null) emit(_onRtmState, event)
            }
        }

        // ── Permissions / Host ─────────────────────────────────────────────
        s.on(Events.ROOM_DISBANDED) {
            emit(_onRoomDisbanded, Unit)
        }
        s.on(Events.ERROR) { args ->
            val msg = extractErrorMessage(args)
            emit(_onError, msg)
        }
        s.on(Events.NAME_TAKEN) { args ->
            val msg = extractErrorMessage(args)
            emit(_onNameTaken, msg)
        }
        s.on(Events.KICKED_FROM_ROOM) {
            emit(_onKickedFromRoom, Unit)
        }
        s.on(Events.HOST_CHANGED) { args ->
            parseAndEmit<HostChangedEvent>(args, _onHostChanged)
        }
        s.on(Events.COHOSTS_UPDATED) { args ->
            parseListAndEmit<String>(args, _onCohostsUpdated)
        }
        s.on(Events.PLAYER_STATUS_UPDATE) { args ->
            val json = args.firstOrNull()?.toString() ?: return@on
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            val map = runCatching { gson.fromJson<Map<String, Boolean>>(json, type) }.getOrNull()
            if (map != null) emit(_onPlayerStatusUpdate, map)
        }
        s.on(Events.TEAM_CLAIMED_SUCCESS) {
            emit(_onTeamClaimedSuccess, Unit)
        }
        s.on(Events.TEAM_ROSTER_DATA) { args ->
            parseAndEmit<TeamRosterEvent>(args, _onTeamRosterData)
        }
        s.on(Events.JOIN_REQUESTS_UPDATE) { args ->
            parseAndEmit<JoinRequestsUpdateEvent>(args, _onJoinRequestsUpdate)
        }
        s.on(Events.PARTICIPATION_APPROVED) {
            emit(_onParticipationApproved, Unit)
        }
        s.on(Events.PARTICIPATION_REJECTED) {
            emit(_onParticipationRejected, Unit)
        }

        // ── Retention ──────────────────────────────────────────────────────
        s.on(Events.RETENTION_SQUADS_LOADED) { args ->
            parseAndEmit<RetentionSquadsEvent>(args, _onRetentionSquadsLoaded)
        }
        s.on(Events.MY_TEAM_RETENTION_SQUAD) { args ->
            parseAndEmit<MyRetentionSquadEvent>(args, _onMyTeamRetentionSquad)
        }
        s.on(Events.RETENTIONS_CONFIRMED) { args ->
            parseAndEmit<RetentionsConfirmedEvent>(args, _onRetentionsConfirmed)
        }
        s.on(Events.RETENTIONS_ALREADY_SUBMITTED) { args ->
            parseAndEmit<RetentionsAlreadySubmittedEvent>(args, _onRetentionsAlreadySubmitted)
        }
        s.on(Events.LOBBY_RETENTION_UPDATE) { args ->
            parseAndEmit<LobbyRetentionUpdateEvent>(args, _onLobbyRetentionUpdate)
        }
    }

    // ── Outgoing events ───────────────────────────────────────────────────────

    fun createRoom(
        roomType: String,
        league: String,
        currency: String,
        auctionYear: String?
    ) {
        val payload = JSONObject().apply {
            put("roomType", roomType)
            put("league", league)
            put("currency", currency)
            auctionYear?.let { put("auctionYear", it) }
        }
        emitEvent(Emit.CREATE_ROOM, payload)
    }

    fun joinRoom(roomCode: String, asSpectator: Boolean = false) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("asSpectator", asSpectator)
        }
        emitEvent(Emit.JOIN_ROOM, payload)
    }

    fun claimTeam(roomCode: String, teamId: String) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("teamId", teamId)
        }
        emitEvent(Emit.CLAIM_TEAM, payload)
    }

    fun startAuction(roomCode: String) {
        emitEvent(Emit.START_AUCTION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun placeBid(roomCode: String, amount: Double) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("amount", amount)
        }
        emitEvent(Emit.PLACE_BID, payload)
    }

    fun skipPlayer(roomCode: String) {
        emitEvent(Emit.SKIP_PLAYER, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun skipPool(roomCode: String) {
        emitEvent(Emit.SKIP_POOL, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun sendChatMessage(roomCode: String, message: String) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("message", message)
        }
        emitEvent(Emit.SEND_CHAT, payload)
    }

    fun requestAuctionSync(roomCode: String) {
        emitEvent(Emit.REQUEST_SYNC, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun requestTeamRoster(teamId: String) {
        emitEvent(Emit.REQUEST_TEAM_ROSTER, JSONObject().apply { put("teamId", teamId) })
    }

    fun submitRetentions(
        roomCode: String,
        retainedPlayers: List<String>,
        customPrices: Map<String, Double>
    ) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("retainedPlayers", JSONArray(retainedPlayers))
            put("customPrices", JSONObject(customPrices))
        }
        emitEvent(Emit.SUBMIT_RETENTIONS, payload)
    }

    fun submitInterestVotes(roomCode: String, playerIds: List<String>) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("playerIds", JSONArray(playerIds))
        }
        emitEvent(Emit.SUBMIT_INTEREST_VOTES, payload)
    }

    fun submitQuizAnswer(roomCode: String, quizIndex: Int, answerIndex: Int) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("quizIndex", quizIndex)
            put("answerIndex", answerIndex)
        }
        emitEvent(Emit.SUBMIT_QUIZ_ANSWER, payload)
    }

    fun leaveRoom(roomCode: String) {
        emitEvent(Emit.LEAVE_ROOM, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun fetchPublicRooms(league: String?) {
        val payload = JSONObject().apply {
            league?.let { put("league", it) }
        }
        emitEvent(Emit.FETCH_PUBLIC_ROOMS, payload)
    }

    fun requestParticipation(roomCode: String) {
        emitEvent(Emit.REQUEST_PARTICIPATION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun approveParticipation(
        roomCode: String,
        targetUserId: String?,
        targetSocketId: String?
    ) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            targetUserId?.let { put("targetUserId", it) }
            targetSocketId?.let { put("targetSocketId", it) }
        }
        emitEvent(Emit.APPROVE_PARTICIPATION, payload)
    }

    fun rejectParticipation(
        roomCode: String,
        targetUserId: String?,
        targetSocketId: String?
    ) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            targetUserId?.let { put("targetUserId", it) }
            targetSocketId?.let { put("targetSocketId", it) }
        }
        emitEvent(Emit.REJECT_PARTICIPATION, payload)
    }

    fun toggleCoHost(roomCode: String, userId: String) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("userId", userId)
        }
        emitEvent(Emit.TOGGLE_CO_HOST, payload)
    }

    fun changeOwnerName(roomCode: String, newName: String) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("newName", newName)
        }
        emitEvent(Emit.CHANGE_OWNER_NAME, payload)
    }

    fun updateLobbySettings(
        roomCode: String,
        allowSpectators: Boolean,
        maxSpectators: Int,
        teamCount: Int
    ) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("allowSpectators", allowSpectators)
            put("maxSpectators", maxSpectators)
            put("teamCount", teamCount)
        }
        emitEvent(Emit.UPDATE_LOBBY_SETTINGS, payload)
    }

    fun updateTimer(roomCode: String, timerDuration: Int) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("timerDuration", timerDuration)
        }
        emitEvent(Emit.UPDATE_TIMER, payload)
    }

    fun rtmDecision(roomCode: String, useRtm: Boolean) {
        val payload = JSONObject().apply {
            put("roomCode", roomCode)
            put("useRtm", useRtm)
        }
        emitEvent(Emit.RTM_DECISION, payload)
    }

    fun pauseAuction(roomCode: String) {
        emitEvent(Emit.PAUSE_AUCTION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun resumeAuction(roomCode: String) {
        emitEvent(Emit.RESUME_AUCTION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun forceEndAuction(roomCode: String) {
        emitEvent(Emit.FORCE_END_AUCTION, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun cancelForceEnd(roomCode: String) {
        emitEvent(Emit.CANCEL_FORCE_END, JSONObject().apply { put("roomCode", roomCode) })
    }

    fun acknowledgeForceEnd(roomCode: String) {
        emitEvent(Emit.ACKNOWLEDGE_FORCE_END, JSONObject().apply { put("roomCode", roomCode) })
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Safely emit a Socket.io event with a JSONObject payload. */
    private fun emitEvent(event: String, payload: JSONObject) {
        val s = socket
        if (s == null || !s.connected()) {
            Log.w(TAG, "Cannot emit '$event' — socket not connected")
            return
        }
        s.emit(event, payload)
        Log.d(TAG, "Emitted '$event': $payload")
    }

    /**
     * Parse the first arg of a Socket.io event as [T] and emit it to [flow].
     * Silently drops malformed payloads (logs the error).
     */
    private inline fun <reified T> parseAndEmit(
        args: Array<out Any>,
        flow: MutableSharedFlow<T>
    ) {
        val json = args.firstOrNull()?.toString() ?: return
        val value = safeParse<T>(json) ?: return
        emit(flow, value)
    }

    /**
     * Parse the first arg as a JSON array of [T] elements and emit to [flow].
     */
    private inline fun <reified T> parseListAndEmit(
        args: Array<out Any>,
        flow: MutableSharedFlow<List<T>>
    ) {
        val raw = args.firstOrNull() ?: return
        val jsonStr = raw.toString()
        val type = object : TypeToken<List<T>>() {}.type
        val list = runCatching { gson.fromJson<List<T>>(jsonStr, type) }.getOrElse {
            // Try wrapping a JSONArray in case the server sends it directly
            runCatching {
                val jsonArray = if (raw is JSONArray) raw else JSONArray(jsonStr)
                gson.fromJson<List<T>>(jsonArray.toString(), type)
            }.getOrNull()
        } ?: return
        emit(flow, list)
    }

    /** Inline helper to launch a coroutine on the IO scope and emit a value. */
    private fun <T> emit(flow: MutableSharedFlow<T>, value: T) {
        scope.launch { flow.emit(value) }
    }

    /** Deserialise JSON string to [T] using Gson, returning null on failure. */
    private inline fun <reified T> safeParse(json: String): T? =
        runCatching { gson.fromJson(json, T::class.java) }.getOrElse { e ->
            Log.e(TAG, "Failed to parse ${T::class.simpleName}: $json", e)
            null
        }

    /**
     * Extract a human-readable error message from a socket event args array.
     * The server may send { message: "..." } or a plain string.
     */
    private fun extractErrorMessage(args: Array<out Any>): String {
        val raw = args.firstOrNull()?.toString() ?: "Unknown error"
        return runCatching {
            JSONObject(raw).optString("message", raw)
        }.getOrDefault(raw)
    }
}
