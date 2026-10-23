package com.playauction.app.data.socket

import com.google.gson.annotations.SerializedName
import com.playauction.app.data.model.*

// ─── Socket Event Payloads ───────────────────────────────────────────────────

data class RoomCreatedEvent(
    @SerializedName("roomCode") val roomCode: String? = null,
    @SerializedName("state") val state: RoomState? = null
)

data class AvailableTeamsEvent(
    @SerializedName("teams") val teams: List<FranchiseDef> = emptyList(),
    @SerializedName("presignedByTeam") val presignedByTeam: Map<String, String>? = null
)

data class LobbyUpdateEvent(
    @SerializedName("teams") val teams: List<Team> = emptyList()
)

data class SpectatorUpdateEvent(
    @SerializedName("spectators") val spectators: List<SpectatorInfo> = emptyList()
)

data class NewPlayerEvent(
    @SerializedName("player") val player: Player? = null,
    @SerializedName("nextPlayers") val nextPlayers: List<Player>? = null,
    @SerializedName("timer") val timer: Int = 10,
    @SerializedName("skippedHistory") val skippedHistory: List<Player>? = null
)

data class CompactBid(
    @SerializedName("a") val amount: Double = 0.0,
    @SerializedName("tid") val teamId: String? = null,
    @SerializedName("tn") val teamName: String? = null,
    @SerializedName("tc") val teamColor: String? = null,
    @SerializedName("tl") val teamLogo: String? = null,
    @SerializedName("on") val ownerName: String? = null,
    @SerializedName("isRtm") val isRtm: Boolean = false
) {
    fun toCurrentBid(): CurrentBid = CurrentBid(
        amount = amount,
        teamId = teamId,
        teamName = teamName,
        teamColor = teamColor,
        teamLogo = teamLogo,
        ownerName = ownerName,
        isRtm = isRtm
    )
}

data class BidPlacedEvent(
    @SerializedName("currentBid") val legacyBid: CurrentBid? = null,
    @SerializedName("cb") val compactBid: CompactBid? = null,
    @SerializedName("timer") val legacyTimer: Int? = null,
    @SerializedName("t") val compactTimer: Int? = null
) {
    val currentBid: CurrentBid
        get() = compactBid?.toCurrentBid() ?: legacyBid ?: CurrentBid.EMPTY

    val timer: Int
        get() = compactTimer ?: legacyTimer ?: 10
}

data class AuctionStateSyncEvent(
    @SerializedName("status") val status: String = "Auctioning",
    @SerializedName("currentIndex") val currentIndex: Int = 0,
    @SerializedName("currentPlayer") val currentPlayer: Player? = null,
    @SerializedName("currentBid") val currentBid: CurrentBid? = null,
    @SerializedName("timer") val timer: Int = 10,
    @SerializedName("timerDuration") val timerDuration: Int = 10,
    @SerializedName("teams") val teams: List<Team> = emptyList(),
    @SerializedName("last5Bids") val last5Bids: List<CurrentBid> = emptyList(),
    @SerializedName("rtmState") val rtmState: RtmState? = null
)

data class PlayerSoldEvent(
    @SerializedName("player") val player: Player,
    @SerializedName("winningBid") val winningBid: CurrentBid? = null,
    @SerializedName("teams") val teams: List<Team> = emptyList()
)

data class PlayerUnsoldEvent(
    @SerializedName("player") val player: Player,
    @SerializedName("unsoldHistory") val updatedUnsoldHistory: List<Player>? = null
)

data class InterestVotingEvent(
    @SerializedName("players") val players: List<VotingCandidateDef> = emptyList(),
    @SerializedName("timer") val timer: Int = 240,
    @SerializedName("isFinal") val isFinal: Boolean = false
)

data class VotingCandidateDef(
    @SerializedName("_id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("poolID") val poolID: String? = null,
    @SerializedName("basePrice") val basePrice: Double? = null
)

data class VotingCompletedEvent(
    @SerializedName("skippedCount") val skippedCount: Int = 0,
    @SerializedName("isFinal") val isFinal: Boolean = false,
    @SerializedName("message") val message: String? = null
)

data class QuizStartedEvent(
    @SerializedName("roomCode") val roomCode: String = "",
    @SerializedName("league") val league: String = "ipl",
    @SerializedName("totalQuestions") val totalQuestions: Int = 10
)

data class QuizTimerEvent(
    @SerializedName("remaining") val remaining: Int = 0,
    @SerializedName("questionNumber") val questionNumber: Int = 0,
    @SerializedName("totalQuestions") val totalQuestions: Int = 10
)

data class AuctionFinishedEvent(
    @SerializedName("teams") val teams: List<Team> = emptyList(),
    @SerializedName("quizLeaderboard") val quizLeaderboard: List<QuizLeaderboardEntry> = emptyList()
)

data class HostChangedEvent(
    @SerializedName("newHostUserId") val newHostUserId: String? = null,
    @SerializedName("newHostName") val newHostName: String? = null,
    @SerializedName("socketId") val socketId: String? = null
)

data class TeamRosterEvent(
    @SerializedName("teamId") val teamId: String = "",
    @SerializedName("playersAcquired") val playersAcquired: List<AcquiredPlayer> = emptyList()
)

data class JoinRequestsUpdateEvent(
    @SerializedName("roomCode") val roomCode: String = "",
    @SerializedName("requests") val requests: List<JoinRequest> = emptyList()
)

data class RetentionSquadsEvent(
    @SerializedName("squads") val squads: Map<String, List<RetentionPlayer>> = emptyMap(),
    @SerializedName("auctionYear") val auctionYear: String? = null,
    @SerializedName("league") val league: String? = null
)

data class MyRetentionSquadEvent(
    @SerializedName("squad") val squad: List<RetentionPlayer> = emptyList(),
    @SerializedName("auctionYear") val auctionYear: String? = null,
    @SerializedName("league") val league: String? = null
)

data class RetentionsConfirmedEvent(
    @SerializedName("teamName") val teamName: String = "",
    @SerializedName("retentionDetails") val retentionDetails: List<Any>? = null,
    @SerializedName("totalCost") val totalCost: Double = 0.0
)

data class RetentionsAlreadySubmittedEvent(
    @SerializedName("retainedPlayers") val retainedPlayers: List<String> = emptyList(),
    @SerializedName("customPrices") val customPrices: Map<String, Double> = emptyMap()
)

data class LobbyRetentionUpdateEvent(
    @SerializedName("retentionStatus") val retentionStatus: Map<String, List<String>?> = emptyMap(),
    @SerializedName("customPricesByTeam") val customPricesByTeam: Map<String, Map<String, Double>> = emptyMap()
)

data class SettingsUpdatedEvent(
    @SerializedName("timerDuration") val timerDuration: Int = 10,
    @SerializedName("timer") val timer: Int? = null
)

data class LobbySettingsEvent(
    @SerializedName("allowSpectators") val allowSpectators: Boolean = true,
    @SerializedName("maxSpectators") val maxSpectators: Int = 10,
    @SerializedName("teamCount") val teamCount: Int = 15
)

data class AuctionEndRequestEvent(
    @SerializedName("endRequest") val endRequest: Map<String, Any>? = null,
    @SerializedName("acknowledgedBy") val acknowledgedBy: List<String> = emptyList(),
    @SerializedName("humanOwners") val humanOwners: List<String> = emptyList()
)

data class AuctionResumedEvent(
    @SerializedName("timer") val timer: Int? = null
)

data class QuizQuestionClosed(
    @SerializedName("quizIndex") val quizIndex: Int = 0,
    @SerializedName("correctIndex") val correctIndex: Int = 0,
    @SerializedName("correctAnswer") val correctAnswer: String = ""
)
