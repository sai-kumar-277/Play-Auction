package com.playauction.app.data.model

import com.google.gson.annotations.SerializedName

// ─── League Enum ─────────────────────────────────────────────────────────────

enum class League(
    val id: String,
    val label: String,
    val subtitle: String,
    val purseLimit: Double,
    val defaultTeamCount: Int,
    val maxSquad: Int,
    val minSquad: Int,
    val maxOverseas: Int,
    val defaultCurrency: String,
    val accentColor: Long,
    val accentColorLight: Long
) {
    IPL("ipl","IPL","Indian Premier League",12000.0,15,25,18,8,"inr",0xFFD4AF37,0xFFFFE58F),
    WPL("wpl","WPL","Women's Premier League",1500.0,5,18,15,6,"inr",0xFFEC4899,0xFFF9A8D4),
    SA20("sa20","SA20","SA20 League",410.0,6,19,17,7,"zar",0xFF06B6D4,0xFF67E8F9);
    companion object { fun fromId(id: String?) = values().firstOrNull { it.id == id?.lowercase() } ?: IPL }
}

data class RetentionRules(val maxCapped: Int, val maxUncapped: Int, val maxTotal: Int, val cappedCosts: List<Double>, val uncappedCost: Double) {
    companion object {
        val WPL = RetentionRules(3,1,4, listOf(350.0,250.0,250.0),50.0)
        val SA20 = RetentionRules(4,1,5, listOf(90.0,70.0,45.0,35.0),5.0)
        fun forLeague(league: String) = when (league.lowercase()) { "wpl" -> WPL; "sa20" -> SA20; else -> null }
    }
}

data class RoleCounts(@SerializedName("bat") val bat: Int=0, @SerializedName("bowl") val bowl: Int=0, @SerializedName("ar") val ar: Int=0, @SerializedName("wk") val wk: Int=0, @SerializedName("fr") val fr: Int=0, @SerializedName("u23") val u23: Int=0, @SerializedName("uncapped") val uncapped: Int=0)

data class AcquiredPlayer(@SerializedName("player") val playerId: String?=null, @SerializedName("name") val name: String="", @SerializedName("role") val role: String?=null, @SerializedName("boughtFor") val boughtFor: Double=0.0, @SerializedName("isOverseas") val isOverseas: Boolean=false, @SerializedName("isRetained") val isRetained: Boolean=false, @SerializedName("isUncapped") val isUncapped: Boolean=false, @SerializedName("isU23") val isU23: Boolean=false, @SerializedName("age") val age: Int?=null, @SerializedName("poolID") val poolID: String?=null, @SerializedName("poolName") val poolName: String?=null, @SerializedName("image_path") val imagePath: String?=null, @SerializedName("imagepath") val imagepath: String?=null, @SerializedName("photoUrl") val photoUrl: String?=null) { val displayImage: String? get() = imagePath ?: imagepath ?: photoUrl }

data class EvalAnalysis(@SerializedName("batting_narrative") val battingNarrative: String?=null, @SerializedName("bowling_narrative") val bowlingNarrative: String?=null, @SerializedName("tactical_balance") val tacticalBalance: String?=null, @SerializedName("key_risks_and_fixes") val keyRisksAndFixes: String?=null)

data class TeamEvaluation(@SerializedName("score") val score: Double=0.0, @SerializedName("overallScore") val overallScore: Double=0.0, @SerializedName("titleProbability") val titleProbability: String="", @SerializedName("analysis") val analysis: EvalAnalysis?=null)

data class Team(
    @SerializedName("franchiseId") val franchiseId: String?=null,
    @SerializedName("teamName") val teamName: String="",
    @SerializedName("teamThemeColor") val teamThemeColor: String="#FFFFFF",
    @SerializedName("teamLogo") val teamLogo: String?=null,
    @SerializedName("ownerName") val ownerName: String?=null,
    @SerializedName("ownerUserId") val ownerUserId: String?=null,
    @SerializedName("ownerSocketId") val ownerSocketId: String?=null,
    @SerializedName("currentPurse") val currentPurse: Double=0.0,
    @SerializedName("startingPurse") val startingPurse: Double=0.0,
    @SerializedName("isBot") val isBot: Boolean=false,
    @SerializedName("acquiredCount") val acquiredCount: Int=0,
    @SerializedName("roleCounts") val roleCounts: RoleCounts?=null,
    @SerializedName("playersAcquired") val playersAcquired: List<AcquiredPlayer> = emptyList(),
    @SerializedName("rtmCards") val rtmCards: Int=0,
    @SerializedName("rtmUsedCount") val rtmUsedCount: Int=0,
    @SerializedName("evaluation") val evaluation: TeamEvaluation?=null,
    @SerializedName("playing11") val playing11: List<String> = emptyList(),
    @SerializedName("rank") val rank: Int?=null
) {
    fun isOwnedBy(userId: String?, socketId: String?) =
        (userId != null && ownerUserId == userId) || (socketId != null && ownerSocketId == socketId)
    val displayAcquiredCount: Int get() = if (playersAcquired.isNotEmpty()) playersAcquired.size else acquiredCount
}

data class Player(
    @SerializedName("_id") val _id: String?=null,
    @SerializedName("name") val name: String?=null,
    @SerializedName("player") val player: String?=null,
    @SerializedName("role") val role: String?=null,
    @SerializedName("nationality") val nationality: String?=null,
    @SerializedName("Country") val country: String?=null,
    @SerializedName("image_path") val imagePath: String?=null,
    @SerializedName("imagepath") val imagepath: String?=null,
    @SerializedName("photoUrl") val photoUrl: String?=null,
    @SerializedName("basePrice") val basePrice: Double=20.0,
    @SerializedName("base_price") val base_price: Double?=null,
    @SerializedName("poolID") val poolID: String?=null,
    @SerializedName("poolName") val poolName: String?=null,
    @SerializedName("isOverseas") val isOverseas: Boolean=false,
    @SerializedName("isU23") val isU23: Boolean=false,
    @SerializedName("isUncapped") val isUncapped: Boolean=false,
    @SerializedName("age") val age: Int?=null,
    @SerializedName("points") val points: Int?=null,
    @SerializedName("boughtFor") val boughtFor: Double?=null,
    @SerializedName("isRetained") val isRetained: Boolean=false
) {
    val displayName: String get() = name ?: player ?: "Unknown"
    val effectiveBasePrice: Double get() = if (basePrice > 0) basePrice else (base_price ?: 20.0)
    val displayImage: String? get() = imagePath ?: imagepath ?: photoUrl
    val displayNationality: String? get() = nationality ?: country
}

data class CurrentBid(
    @SerializedName("amount") val amount: Double=0.0,
    @SerializedName("teamId") val teamId: String?=null,
    @SerializedName("teamName") val teamName: String?=null,
    @SerializedName("teamColor") val teamColor: String?=null,
    @SerializedName("teamLogo") val teamLogo: String?=null,
    @SerializedName("ownerName") val ownerName: String?=null,
    @SerializedName("isRtm") val isRtm: Boolean=false
) { companion object { val EMPTY = CurrentBid() } }

data class BidHistoryEntry(val id: Long=System.currentTimeMillis(), val amount: Double, val teamName: String?, val teamColor: String?, val teamLogo: String?, val ownerName: String?, val time: String="")

data class ChatMessage(val id: String, val type: String="regular", val senderName: String?=null, val senderTeam: String?=null, val senderColor: String?=null, val senderLogo: String?=null, val message: String?=null, val playerName: String?=null, val playerImage: String?=null, val amount: Double?=null, val basePrice: Double?=null, val verdict: String?=null, val slogan: String?=null, val timestamp: String="", val teamShort: String?=null, val ownerName: String?=null, val poolID: String?=null)

data class RtmState(@SerializedName("playerName") val playerName: String="", @SerializedName("prevTeamName") val prevTeamName: String="", @SerializedName("prevTeamOwnerUserId") val prevTeamOwnerUserId: String?=null, @SerializedName("currentBid") val currentBid: CurrentBid=CurrentBid.EMPTY, @SerializedName("timer") val timer: Int=30)

data class SpectatorInfo(@SerializedName("socketId") val socketId: String="", @SerializedName("userId") val userId: String?=null, @SerializedName("name") val name: String?=null)

data class JoinRequest(@SerializedName("socketId") val socketId: String="", @SerializedName("userId") val userId: String?=null, @SerializedName("name") val name: String?=null, @SerializedName("time") val time: Long=0L)

data class QuizQuestion(@SerializedName("quizIndex") val quizIndex: Int=0, @SerializedName("text") val text: String="", @SerializedName("options") val options: List<String> = emptyList(), @SerializedName("correctIndex") val correctIndex: Int=0, @SerializedName("difficulty") val difficulty: String="easy", @SerializedName("points") val points: Int=1, @SerializedName("questionNumber") val questionNumber: Int=0, @SerializedName("totalQuestions") val totalQuestions: Int=10, @SerializedName("timeLimit") val timeLimit: Int=12)

data class QuizLeaderboardEntry(@SerializedName("userId") val userId: String="", @SerializedName("name") val name: String="", @SerializedName("score") val score: Int=0, @SerializedName("correctCount") val correctCount: Int=0, @SerializedName("rank") val rank: Int=0)

data class QuizAnswerResult(@SerializedName("quizIndex") val quizIndex: Int=0, @SerializedName("correct") val correct: Boolean=false, @SerializedName("pointsAdded") val pointsAdded: Int=0, @SerializedName("responseMs") val responseMs: Long=0, @SerializedName("correctIndex") val correctIndex: Int?=null)

data class RetentionPlayer(@SerializedName("name") val name: String="", @SerializedName("isUncapped") val isUncapped: Boolean=false, @SerializedName("isOverseas") val isOverseas: Boolean=false)

data class RetentionBreakdown(val name: String, val cost: Double, val isUncapped: Boolean, val slot: Int?)

data class AuctionFeedEntry(val key: String, val type: String, val playerName: String?, val playerImage: String?, val teamLogo: String?=null, val teamShort: String?=null, val teamColor: String?=null, val ownerName: String?=null, val amount: Double?=null, val slogan: String?=null, val timestamp: String="", val sortTime: String="")

data class SoldEvent(val type: String, val player: Player, val winningBid: CurrentBid?=null)

data class EndRequest(val roomCode: String="", val acknowledgedBy: List<String> = emptyList())

data class VotingSession(val active: Boolean=false, val players: List<VotingCandidate> = emptyList(), val timer: Int=240, val isFinal: Boolean=false)

data class VotingCandidate(val id: String, val name: String, val poolID: String?, val basePrice: Double?)

data class PublicRoom(@SerializedName("roomCode") val roomCode: String="", @SerializedName("hostName") val hostName: String="", @SerializedName("league") val league: String="ipl", @SerializedName("teamsCount") val teamsCount: Int=0, @SerializedName("maxTeams") val maxTeams: Int=15)

data class SessionData(@SerializedName("token") val token: String="", @SerializedName("userId") val userId: String="", @SerializedName("playerName") val playerName: String="")

data class FranchiseDef(@SerializedName("_id") val id: String?=null, @SerializedName("name") val name: String="", @SerializedName("shortName") val shortName: String="", @SerializedName("primaryColor") val primaryColor: String="#FFFFFF", @SerializedName("logoUrl") val logoUrl: String?=null, @SerializedName("purseLimit") val purseLimit: Double=12000.0)

data class RoomState(@SerializedName("roomCode") val roomCode: String="", @SerializedName("status") val status: String="Lobby", @SerializedName("league") val league: String="ipl", @SerializedName("currency") val currency: String="inr", @SerializedName("teams") val teams: List<Team> = emptyList(), @SerializedName("availableTeams") val availableTeams: List<FranchiseDef> = emptyList(), @SerializedName("currentPlayer") val currentPlayer: Player?=null, @SerializedName("activePlayer") val activePlayer: Player?=null, @SerializedName("currentBid") val currentBid: CurrentBid?=null, @SerializedName("activeBid") val activeBid: CurrentBid?=null, @SerializedName("timer") val timer: Int=10, @SerializedName("timerDuration") val timerDuration: Int=10, @SerializedName("currentIndex") val currentIndex: Int=0, @SerializedName("unsoldHistory") val unsoldHistory: List<Player> = emptyList(), @SerializedName("last5Bids") val last5Bids: List<CurrentBid> = emptyList(), @SerializedName("rtmState") val rtmState: RtmState?=null, @SerializedName("isAiMode") val isAiMode: Boolean=false, @SerializedName("roomType") val roomType: String="private", @SerializedName("auctionYear") val auctionYear: String?=null, @SerializedName("coHostUserIds") val coHostUserIds: List<String> = emptyList(), @SerializedName("hostUserId") val hostUserId: String?=null, @SerializedName("host") val host: String?=null, @SerializedName("hostName") val hostName: String?=null, @SerializedName("spectators") val spectators: List<SpectatorInfo> = emptyList(), @SerializedName("players") val players: List<Player> = emptyList(), @SerializedName("retentionSquads") val retentionSquads: Map<String,List<RetentionPlayer>>?=null, @SerializedName("retentionsByTeam") val retentionsByTeam: Map<String,List<String>>?=null, @SerializedName("teamCount") val teamCount: Int=15, @SerializedName("allowSpectators") val allowSpectators: Boolean=true, @SerializedName("maxSpectators") val maxSpectators: Int=10, @SerializedName("isPaused") val isPaused: Boolean=false, @SerializedName("quizLeaderboard") val quizLeaderboard: List<QuizLeaderboardEntry> = emptyList())

data class FeedbackRequest(val name: String, val userId: String?, val playerName: String?, val roomCode: String?, val league: String?, val page: String?, val category: String, val message: String, val contact: String?)

data class ResultsResponse(@SerializedName("teams") val teams: List<Team> = emptyList(), @SerializedName("league") val league: String="ipl", @SerializedName("currency") val currency: String="inr", @SerializedName("quizLeaderboard") val quizLeaderboard: List<QuizLeaderboardEntry> = emptyList())
