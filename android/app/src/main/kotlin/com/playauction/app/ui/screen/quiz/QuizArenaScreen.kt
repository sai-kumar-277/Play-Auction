package com.playauction.app.ui.screen.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playauction.app.ui.theme.*
import com.playauction.app.ui.viewmodel.QuizEvent
import com.playauction.app.ui.viewmodel.QuizPhase
import com.playauction.app.ui.viewmodel.QuizViewModel

@Composable
fun QuizArenaScreen(
    roomCode: String,
    quizViewModel: QuizViewModel,
    onNavigateToEvaluation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by quizViewModel.state.collectAsState()

    LaunchedEffect(roomCode) {
        quizViewModel.init(roomCode)
    }

    LaunchedEffect(Unit) {
        quizViewModel.events.collect { evt ->
            when (evt) {
                is QuizEvent.NavigateToEvaluation -> onNavigateToEvaluation(evt.roomCode)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Quiz Header
            Text(
                text = "POST-AUCTION TRIVIA ARENA",
                style = MaterialTheme.typography.labelSmall,
                color = CyanSa20Light
            )

            Text(
                text = "Cricket Knowledge Showdown",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (state.phase) {
                QuizPhase.INTRO -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkCard)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🧠",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Bidding Finished!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "10 rapid-fire questions to earn bonus team points.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
                QuizPhase.ACTIVE -> {
                    val q = state.currentQuestion
                    if (q != null) {
                        // Question Number & Timer Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${q.questionNumber} of ${q.totalQuestions}",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            // Question Timer
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (state.questionTimer <= 3) TimerDanger.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${state.questionTimer}s",
                                    color = if (state.questionTimer <= 3) TimerDanger else GoldLight,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Question Prompt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                .padding(18.dp)
                        ) {
                            Text(
                                text = q.text,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Answer Options
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            q.options.forEachIndexed { idx, opt ->
                                val isSelected = state.selectedAnswer == idx
                                val isCorrect = state.resultData?.correct == true && isSelected
                                val isWrong = state.resultData?.correct == false && isSelected
                                val isRevealedCorrect = state.revealData?.correctIndex == idx

                                val cardBg by animateColorAsState(
                                    targetValue = when {
                                        isCorrect || isRevealedCorrect -> SoldEmerald.copy(alpha = 0.25f)
                                        isWrong -> TimerDanger.copy(alpha = 0.25f)
                                        isSelected -> GoldPrimary.copy(alpha = 0.2f)
                                        else -> DarkCard
                                    },
                                    label = "optBg"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardBg)
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldPrimary else BorderSubtle,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = state.selectedAnswer == null) {
                                            quizViewModel.submitAnswer(idx)
                                        }
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${('A' + idx)}. ",
                                            fontWeight = FontWeight.Bold,
                                            color = GoldLight,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = opt,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else if (state.revealData != null) {
                        // Reveal Screen between questions
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceVariant)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Answer", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = state.revealData?.correctAnswer ?: "",
                                    color = LiveGreen,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Next question incoming...", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Leaderboard Pill List
                    Text(
                        text = "LEADERBOARD",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.leaderboard) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurface)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${entry.rank}. ${entry.name}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${entry.score} pts",
                                    color = GoldLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                QuizPhase.FINISHED -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkCard)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Trivia Complete!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Heading to AI squad evaluation in ${state.countdownToEval}s",
                                color = CyanSa20Light,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
