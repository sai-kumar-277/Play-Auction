package com.playauction.app.data.socket

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// Room / Lobby events
// ─────────────────────────────────────────────────────────────────────────────

data class RoomCreatedEvent(
    @SerializedName("roomCode") val roomCode: String,
    @SerializedName("roomState") val roomState: RoomState
)

data class RoomState(
    @SerializedName("roomCode") val roomCode: String,
    @SerializedName("roomType") val roomType: String,
    @SerializedName("league") val league: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("auctionYear") val auctionYear: String?,
    @SerializedName("hostId") val hostId: String,
    @SerializedName("hostName") val hostName: String,
    @SerializedName("coHosts") val coHosts: List<String> = emptyList(),
    @SerializedName("participants") val participants: List<Participant> = emptyList(),
    @SerializedName("spectators") val spectators: List<Spectator> = emptyList(),
    @SerializedName("teams") val teams: List<TeamInfo> = emptyList(),
    @SerializedName("phase") val phase: String,
    @SerializedName("timerDuration") val timerDuration: Int,
    @SerializedName("allowSpectators") val allowSpectators: Boolean,
    @SerializedName("maxSpectators") val maxSpectators: Int,
    @SerializedName("isPublic") val isPublic: Boolean,
    @SerializedName("currentPlayerPool") val currentPlayerPool: List<AuctionPlayer>?,
    @SerializedName("currentPlayer") val currentPlayer: AuctionPlayer?,
    @SerializedName("currentBid") val currentBid: Double?,
    @SerializedName("currentBidder") val currentBidder: String?,
    @SerializedName("requiresApproval") val requiresApproval: Boolean = false,
    @SerializedName("rtmState") val rtmState: RtmState? = null
)

data class Participant(
    @SerializedName("userId") val userId: String,
    @SerializedName("socketId") val socketId: String,
    @SerializedName("name") val name: String,
    @SerializedName("teamId") val teamId: String?,
    @SerializedName("teamName") val teamName: String?,
    @SerializedName("budget") val budget: Double,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("isHost") val isHost: Boolean,
    @SerializedName("isCoHost") val isCoHost: Boolean
)

data class Spectator(
    @SerializedName("userId") val userId: String,
    @SerializedName("socketId") val socketId: String,
    @SerializedName("name") val name: String,
    @SerializedName("isOnline") val isOnline: Boolean
)

data class TeamInfo(
    @SerializedName("teamId") val teamId: String,
    @SerializedName("teamName") val teamName: String,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("logoUrl") val logoUrl: String?,
    @SerializedName("ownerId") val ownerId: String?,
    @SerializedName("ownerName") val ownerName: String?,
    @SerializedName("budget") val budget: Double,
    @SerializedName("players") val players: List<AuctionPlayer> = emptyList()
)

data class AuctionPlayer(
    @SerializedName("playerId") val playerId: String,
    @SerializedName("playerName") val playerName: String,
    @SerializedName("role") val role: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("soldPrice") val soldPrice: Double?,
    @SerializedName("soldTo") val soldTo: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("isCapped") val isCapped: Boolean = false,
    @SerializedName("battingStyle") val battingStyle: String?,
    @SerializedName("bowlingStyle") val bowlingStyle: String?
)

data class AvailableTeamsEvent(
    @SerializedName("teams") val teams: List<TeamInfo>
)

data class LobbyUpdateEvent(
    @SerializedName("participants") val participants: List<Participant>,
    @SerializedName("spectators") val spectators: List<Spectator>,
    @SerializedName("teams") val teams: List<TeamInfo>
)

data class SpectatorUpdateEvent(
    @SerializedName("spectators") val spectators: List<Spectator>
)

data class PublicRoom(
    @SerializedName("roomCode") val roomCode: String,
    @SerializedName("league") val league: String,
    @SerializedName("roomType") val roomType: String,
    @SerializedName("hostName") val hostName: String,
    @SerializedName("participantCount") val participantCount: Int,
    @SerializedName("maxParticipants") val maxParticipants: Int,
    @SerializedName("phase") val phase: String,
    @SerializedName("requiresApproval") val requiresApproval: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Auction events
// ─────────────────────────────────────────────────────────────────────────────

data class NewPlayerEvent(
    @SerializedName("player") val player: AuctionPlayer,
    @SerializedName("pool") val pool: List<AuctionPlayer>?,
    @SerializedName("poolIndex") val poolIndex: Int?,
    @SerializedName("poolSize") val poolSize: Int?
)

data class BidPlacedEvent(
    @SerializedName("bidderId") val bidderId: String,
    @SerializedName("bidderName") val bidderName: String,
    @SerializedName("teamName") val teamName: String?,
    @SerializedName("amount") val amount: Double,
    @SerializedName("playerId") val playerId: String
)

data class AuctionStateSyncEvent(
    @SerializedName("currentPlayer") val currentPlayer: AuctionPlayer?,
    @SerializedName("currentPool") val currentPool: List<AuctionPlayer>?,
    @SerializedName("currentBid") val currentBid: Double?,
    @SerializedName("currentBidder") val currentBidder: String?,
    @SerializedName("currentBidderName") val currentBidderName: String?,
    @SerializedName("timerRemaining") val timerRemaining: Int,
    @SerializedName("phase") val phase: String,
    @SerializedName("teams") val teams: List<TeamInfo>,
    @SerializedName("participants") val participants: List<Participant>
)

data class PlayerSoldEvent(
    @SerializedName("player") val player: AuctionPlayer,
    @SerializedName("soldTo") val soldTo: String,
    @SerializedName("soldToName") val soldToName: String,
    @SerializedName("soldPrice") val soldPrice: Double,
    @SerializedName("remainingBudget") val remainingBudget: Double
)

data class PlayerUnsoldEvent(
    @SerializedName("player") val player: AuctionPlayer,
    @SerializedName("reason") val reason: String?
)

data class AuctionResumedEvent(
    @SerializedName("currentPlayer") val currentPlayer: AuctionPlayer?,
    @SerializedName("currentBid") val currentBid: Double?,
    @SerializedName("timerRemaining") val timerRemaining: Int
)

data class AuctionFinishedEvent(
    @SerializedName("teams") val teams: List<TeamInfo>,
    @SerializedName("soldPlayers") val soldPlayers: List<AuctionPlayer>,
    @SerializedName("unsoldPlayers") val unsoldPlayers: List<AuctionPlayer>
)

data class AuctionEndRequestEvent(
    @SerializedName("requestedBy") val requestedBy: String,
    @SerializedName("requestedByName") val requestedByName: String,
    @SerializedName("acknowledgements") val acknowledgements: List<String>
)

// ─────────────────────────────────────────────────────────────────────────────
// Chat
// ─────────────────────────────────────────────────────────────────────────────

data class ChatMessage(
    @SerializedName("senderId") val senderId: String,
    @SerializedName("senderName") val senderName: String,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("isSystem") val isSystem: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Interest Voting
// ─────────────────────────────────────────────────────────────────────────────

data class InterestVotingEvent(
    @SerializedName("players") val players: List<AuctionPlayer>,
    @SerializedName("durationSeconds") val durationSeconds: Int,
    @SerializedName("maxVotes") val maxVotes: Int
)

data class VotingCompletedEvent(
    @SerializedName("selectedPlayers") val selectedPlayers: List<AuctionPlayer>,
    @SerializedName("voteCounts") val voteCounts: Map<String, Int>
)

// ─────────────────────────────────────────────────────────────────────────────
// Quiz Phase
// ─────────────────────────────────────────────────────────────────────────────

data class QuizStartedEvent(
    @SerializedName("totalQuestions") val totalQuestions: Int,
    @SerializedName("durationPerQuestion") val durationPerQuestion: Int,
    @SerializedName("leagueName") val leagueName: String
)

data class QuizQuestion(
    @SerializedName("questionIndex") val questionIndex: Int,
    @SerializedName("questionText") val questionText: String,
    @SerializedName("options") val options: List<String>,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("totalQuestions") val totalQuestions: Int
)

data class QuizTimerEvent(
    @SerializedName("questionIndex") val questionIndex: Int,
    @SerializedName("secondsRemaining") val secondsRemaining: Int
)

data class QuizLeaderboardEntry(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("score") val score: Int,
    @SerializedName("correctAnswers") val correctAnswers: Int,
    @SerializedName("rank") val rank: Int,
    @SerializedName("budgetBonus") val budgetBonus: Double
)

data class QuizAnswerResult(
    @SerializedName("questionIndex") val questionIndex: Int,
    @SerializedName("isCorrect") val isCorrect: Boolean,
    @SerializedName("correctAnswerIndex") val correctAnswerIndex: Int,
    @SerializedName("pointsEarned") val pointsEarned: Int
)

data class QuizQuestionClosed(
    @SerializedName("questionIndex") val questionIndex: Int,
    @SerializedName("correctAnswerIndex") val correctAnswerIndex: Int,
    @SerializedName("correctAnswerText") val correctAnswerText: String
)

// ─────────────────────────────────────────────────────────────────────────────
// RTM (Right To Match)
// ─────────────────────────────────────────────────────────────────────────────

data class RtmState(
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("eligibleTeamId") val eligibleTeamId: String?,
    @SerializedName("eligibleTeamName") val eligibleTeamName: String?,
    @SerializedName("player") val player: AuctionPlayer?,
    @SerializedName("matchAmount") val matchAmount: Double?,
    @SerializedName("decisionDeadline") val decisionDeadline: Long?
)

// ─────────────────────────────────────────────────────────────────────────────
// Retention
// ─────────────────────────────────────────────────────────────────────────────

data class RetentionSquadsEvent(
    @SerializedName("squads") val squads: List<TeamRetentionSquad>
)

data class TeamRetentionSquad(
    @SerializedName("teamId") val teamId: String,
    @SerializedName("teamName") val teamName: String,
    @SerializedName("ownerId") val ownerId: String?,
    @SerializedName("players") val players: List<AuctionPlayer>,
    @SerializedName("hasSubmitted") val hasSubmitted: Boolean,
    @SerializedName("retainedCount") val retainedCount: Int
)

data class MyRetentionSquadEvent(
    @SerializedName("teamId") val teamId: String,
    @SerializedName("players") val players: List<AuctionPlayer>,
    @SerializedName("maxRetentions") val maxRetentions: Int,
    @SerializedName("retentionCosts") val retentionCosts: Map<String, Double>
)

data class RetentionsConfirmedEvent(
    @SerializedName("teamId") val teamId: String,
    @SerializedName("retainedPlayers") val retainedPlayers: List<AuctionPlayer>,
    @SerializedName("remainingBudget") val remainingBudget: Double
)

data class RetentionsAlreadySubmittedEvent(
    @SerializedName("teamId") val teamId: String,
    @SerializedName("message") val message: String
)

data class LobbyRetentionUpdateEvent(
    @SerializedName("teamId") val teamId: String,
    @SerializedName("teamName") val teamName: String,
    @SerializedName("hasSubmitted") val hasSubmitted: Boolean,
    @SerializedName("submittedCount") val submittedCount: Int,
    @SerializedName("totalCount") val totalCount: Int
)

// ─────────────────────────────────────────────────────────────────────────────
// Permissions / Host
// ─────────────────────────────────────────────────────────────────────────────

data class HostChangedEvent(
    @SerializedName("newHostId") val newHostId: String,
    @SerializedName("newHostName") val newHostName: String
)

data class JoinRequestsUpdateEvent(
    @SerializedName("pendingRequests") val pendingRequests: List<PendingJoinRequest>
)

data class PendingJoinRequest(
    @SerializedName("userId") val userId: String?,
    @SerializedName("socketId") val socketId: String,
    @SerializedName("name") val name: String,
    @SerializedName("requestedAt") val requestedAt: Long
)

data class TeamRosterEvent(
    @SerializedName("teamId") val teamId: String,
    @SerializedName("teamName") val teamName: String,
    @SerializedName("players") val players: List<AuctionPlayer>,
    @SerializedName("budget") val budget: Double
)

data class SettingsUpdatedEvent(
    @SerializedName("timerDuration") val timerDuration: Int?,
    @SerializedName("allowSpectators") val allowSpectators: Boolean?,
    @SerializedName("maxSpectators") val maxSpectators: Int?,
    @SerializedName("teamCount") val teamCount: Int?
)

data class LobbySettingsEvent(
    @SerializedName("allowSpectators") val allowSpectators: Boolean,
    @SerializedName("maxSpectators") val maxSpectators: Int,
    @SerializedName("teamCount") val teamCount: Int,
    @SerializedName("timerDuration") val timerDuration: Int,
    @SerializedName("requiresApproval") val requiresApproval: Boolean
)
