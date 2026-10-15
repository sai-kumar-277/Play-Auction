package com.playauction.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.playauction.app.ui.screen.auction.AuctionScreen
import com.playauction.app.ui.screen.auth.AuthScreen
import com.playauction.app.ui.screen.evaluation.EvaluationScreen
import com.playauction.app.ui.screen.lobby.LobbyScreen
import com.playauction.app.ui.screen.quiz.QuizArenaScreen
import com.playauction.app.ui.screen.results.ResultsScreen
import com.playauction.app.ui.viewmodel.*
import com.playauction.app.util.HapticManager
import com.playauction.app.util.SoundManager

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Lobby : Screen("lobby")
    object Auction : Screen("auction")
    object Quiz : Screen("quiz/{roomCode}") {
        fun createRoute(roomCode: String) = "quiz/$roomCode"
    }
    object Evaluation : Screen("evaluation/{roomCode}") {
        fun createRoute(roomCode: String) = "evaluation/$roomCode"
    }
    object Results : Screen("results")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    soundManager: SoundManager,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier
) {
    // Shared ViewModels at NavGraph level
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val lobbyViewModel: LobbyViewModel = hiltViewModel()
    val auctionViewModel: AuctionViewModel = hiltViewModel()
    val resultsViewModel: ResultsViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route,
        modifier = modifier
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                sessionViewModel = sessionViewModel,
                lobbyViewModel = lobbyViewModel,
                onNavigateToLobby = {
                    navController.navigate(Screen.Lobby.route)
                }
            )
        }

        composable(Screen.Lobby.route) {
            LobbyScreen(
                lobbyViewModel = lobbyViewModel,
                sessionViewModel = sessionViewModel,
                onNavigateToAuction = {
                    val roomState = lobbyViewModel.state.value.roomState
                    val code = roomState?.roomCode ?: ""
                    auctionViewModel.initRoom(
                        roomCode = code,
                        initialState = roomState,
                        userId = sessionViewModel.currentUserId,
                        socketId = null
                    )
                    navController.navigate(Screen.Auction.route) {
                        popUpTo(Screen.Lobby.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Auction.route) {
            AuctionScreen(
                auctionViewModel = auctionViewModel,
                soundManager = soundManager,
                hapticManager = hapticManager,
                onNavigateToQuiz = { roomCode ->
                    navController.navigate(Screen.Quiz.createRoute(roomCode)) {
                        popUpTo(Screen.Auction.route) { inclusive = true }
                    }
                },
                onNavigateToEvaluating = { roomCode ->
                    navController.navigate(Screen.Evaluation.createRoute(roomCode)) {
                        popUpTo(Screen.Auction.route) { inclusive = true }
                    }
                },
                onNavigateToResults = {
                    navController.navigate(Screen.Results.route) {
                        popUpTo(Screen.Auction.route) { inclusive = true }
                    }
                },
                onLeaveAuction = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Quiz.route,
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            val quizViewModel: QuizViewModel = hiltViewModel()
            QuizArenaScreen(
                roomCode = roomCode,
                quizViewModel = quizViewModel,
                onNavigateToEvaluation = { code ->
                    navController.navigate(Screen.Evaluation.createRoute(code)) {
                        popUpTo(Screen.Quiz.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Evaluation.route,
            arguments = listOf(navArgument("roomCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            val evaluationViewModel: EvaluationViewModel = hiltViewModel()
            EvaluationScreen(
                roomCode = roomCode,
                evaluationViewModel = evaluationViewModel,
                onNavigateToResults = { teams, quizLb ->
                    resultsViewModel.init(
                        teams = teams,
                        quizLeaderboard = quizLb,
                        league = auctionViewModel.state.value.league,
                        currency = auctionViewModel.state.value.currency
                    )
                    navController.navigate(Screen.Results.route) {
                        popUpTo(Screen.Evaluation.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Results.route) {
            ResultsScreen(
                resultsViewModel = resultsViewModel,
                onNavigateHome = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
