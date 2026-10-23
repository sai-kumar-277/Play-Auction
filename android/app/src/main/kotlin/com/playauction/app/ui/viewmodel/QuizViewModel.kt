package com.playauction.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playauction.app.data.model.*
import com.playauction.app.data.socket.*
import com.playauction.app.data.socket.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── Quiz State ───────────────────────────────────────────────────────────────

enum class QuizPhase { INTRO, ACTIVE, FINISHED }

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.INTRO,
    val league: String = "ipl",
    val totalQuestions: Int = 10,
    val currentQuestion: QuizQuestion? = null,
    val leaderboard: List<QuizLeaderboardEntry> = emptyList(),
    val selectedAnswer: Int? = null,
    val resultData: QuizAnswerResult? = null,
    val revealData: QuizQuestionClosed? = null,
    val questionTimer: Int = 12,
    val finalLeaderboard: List<QuizLeaderboardEntry> = emptyList(),
    val countdownToEval: Int = 5
)

sealed class QuizEvent {
    data class NavigateToEvaluation(val roomCode: String) : QuizEvent()
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val socketManager: SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<QuizEvent>()
    val events: SharedFlow<QuizEvent> = _events.asSharedFlow()

    private var roomCode: String = ""

    fun init(roomCode: String) {
        this.roomCode = roomCode
        collectSocketEvents()
    }

    private fun collectSocketEvents() {
        viewModelScope.launch {
            socketManager.onQuizPhaseStarted.collect { evt ->
                _state.update { it.copy(phase = QuizPhase.ACTIVE, league = evt.league, totalQuestions = evt.totalQuestions) }
            }
        }
        viewModelScope.launch {
            socketManager.onQuizQuestion.collect { q ->
                _state.update { it.copy(phase = QuizPhase.ACTIVE, currentQuestion = q, selectedAnswer = null, resultData = null, revealData = null, questionTimer = q.timeLimit) }
            }
        }
        viewModelScope.launch {
            socketManager.onQuizTimerTick.collect { evt ->
                _state.update { it.copy(questionTimer = evt.remaining) }
            }
        }
        viewModelScope.launch {
            socketManager.onQuizLeaderboard.collect { lb ->
                _state.update { it.copy(leaderboard = lb) }
            }
        }
        viewModelScope.launch {
            socketManager.onQuizAnswerResult.collect { result ->
                _state.update { it.copy(resultData = result) }
            }
        }
        viewModelScope.launch {
            socketManager.onQuizQuestionClosed.collect { evt ->
                _state.update { it.copy(revealData = QuizQuestionClosed(evt.quizIndex, evt.correctIndex, evt.correctAnswer), currentQuestion = null) }
            }
        }
        viewModelScope.launch {
            socketManager.onQuizPhaseEnded.collect { lb ->
                _state.update { it.copy(phase = QuizPhase.FINISHED, finalLeaderboard = lb, currentQuestion = null) }
                startEvalCountdown()
            }
        }
    }

    private fun startEvalCountdown() {
        viewModelScope.launch {
            var remaining = 5
            while (remaining > 0) {
                _state.update { it.copy(countdownToEval = remaining) }
                delay(1000)
                remaining--
            }
            _events.emit(QuizEvent.NavigateToEvaluation(roomCode))
        }
    }

    fun submitAnswer(answerIndex: Int) {
        val s = _state.value
        if (s.selectedAnswer != null) return
        val q = s.currentQuestion ?: return
        _state.update { it.copy(selectedAnswer = answerIndex) }
        socketManager.submitQuizAnswer(roomCode, q.quizIndex, answerIndex)
    }
}


// ─── Evaluation ViewModel ─────────────────────────────────────────────────────

data class EvalUiState(
    val timer: Int = 90,
    val teams: List<Team> = emptyList()
)

@HiltViewModel
class EvaluationViewModel @Inject constructor(
    private val socketManager: SocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(EvalUiState())
    val state: StateFlow<EvalUiState> = _state.asStateFlow()

    private val _navigateToResults = MutableSharedFlow<Triple<String, List<Team>, List<QuizLeaderboardEntry>>>()
    val navigateToResults: SharedFlow<Triple<String, List<Team>, List<QuizLeaderboardEntry>>> = _navigateToResults.asSharedFlow()

    private var roomCode: String = ""

    fun init(roomCode: String, initialTimer: Int = 90) {
        this.roomCode = roomCode
        _state.update { it.copy(timer = initialTimer) }
        collectSocketEvents()
    }

    private fun collectSocketEvents() {
        viewModelScope.launch {
            socketManager.onEvaluationTimerTick.collect { t ->
                _state.update { it.copy(timer = t) }
            }
        }
        viewModelScope.launch {
            socketManager.onAuctionFinished.collect { evt ->
                _navigateToResults.emit(Triple(roomCode, evt.teams, evt.quizLeaderboard))
            }
        }
    }
}


// ─── Results ViewModel ────────────────────────────────────────────────────────

data class ResultsUiState(
    val teams: List<Team> = emptyList(),
    val quizLeaderboard: List<QuizLeaderboardEntry> = emptyList(),
    val selectedTeamIndex: Int = 0,
    val league: String = "ipl",
    val currency: String = "inr",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val api: com.playauction.app.data.network.ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(ResultsUiState())
    val state: StateFlow<ResultsUiState> = _state.asStateFlow()

    fun init(teams: List<Team>, quizLeaderboard: List<QuizLeaderboardEntry>, league: String, currency: String) {
        _state.update { it.copy(teams = teams.sortedByDescending { t -> t.evaluation?.overallScore ?: 0.0 }, quizLeaderboard = quizLeaderboard, league = league, currency = currency) }
    }

    fun loadFromServer(roomCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = api.getResults(roomCode)
                _state.update { it.copy(teams = response.teams, quizLeaderboard = response.quizLeaderboard, league = response.league, currency = response.currency, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun selectTeam(index: Int) = _state.update { it.copy(selectedTeamIndex = index) }
}
