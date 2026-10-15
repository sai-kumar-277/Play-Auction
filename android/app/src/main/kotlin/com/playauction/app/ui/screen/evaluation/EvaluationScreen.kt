package com.playauction.app.ui.screen.evaluation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playauction.app.data.model.QuizLeaderboardEntry
import com.playauction.app.data.model.Team
import com.playauction.app.ui.theme.*
import com.playauction.app.ui.viewmodel.EvaluationViewModel

@Composable
fun EvaluationScreen(
    roomCode: String,
    evaluationViewModel: EvaluationViewModel,
    onNavigateToResults: (List<Team>, List<QuizLeaderboardEntry>) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by evaluationViewModel.state.collectAsState()

    LaunchedEffect(roomCode) {
        evaluationViewModel.init(roomCode)
    }

    LaunchedEffect(Unit) {
        evaluationViewModel.navigateToResults.collect { (_, teams, quizLb) ->
            onNavigateToResults(teams, quizLb)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rotating AI Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .rotate(rotation)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(GoldPrimary, CyanSa20, PinkWpl, GoldPrimary)
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${state.timer}s",
                        color = GoldLight,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "AI SQUAD EVALUATION",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = TextPrimary
            )

            Text(
                text = "Powered by Google Gemini 2.5 Flash",
                fontSize = 13.sp,
                color = CyanSa20Light,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Analysis Steps Status Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EvaluationStepItem("Analyzing squad balance & depth...", active = state.timer > 60)
                EvaluationStepItem("Evaluating Playing XI combinations...", active = state.timer in 30..60)
                EvaluationStepItem("Generating tactical recommendations & ratings...", active = state.timer < 30)
            }
        }
    }
}

@Composable
fun EvaluationStepItem(text: String, active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) DarkSurfaceVariant else DarkCard)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (active) LiveGreen else TextMuted)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = if (active) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
