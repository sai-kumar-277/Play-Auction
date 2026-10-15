package com.playauction.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playauction.app.ui.theme.*

@Composable
fun AuctionTimer(
    timer: Int,
    timerDuration: Int,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val totalTime = if (timerDuration > 0) timerDuration.toFloat() else 10f
    val progress = (timer.toFloat() / totalTime).coerceIn(0f, 1f)

    val targetColor = when {
        timer <= 3 -> TimerDanger
        timer <= 5 -> WarningAmber
        else -> GoldPrimary
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "timerColor"
    )

    // Pulse animation when timer is critical
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (timer in 1..3) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        rememberUpdatedState(1f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            // Track background
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = (size.toPx() - strokeWidth) / 2f,
                style = Stroke(width = strokeWidth)
            )
            // Progress arc
            drawArc(
                color = animatedColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = timer.coerceAtLeast(0).toString(),
            color = animatedColor,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-1).sp
        )
    }
}
