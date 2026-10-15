package com.playauction.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.playauction.app.data.model.SoldEvent
import com.playauction.app.domain.CurrencyFormatter
import com.playauction.app.ui.theme.*

@Composable
fun GavelSlamOverlay(
    soldEvent: SoldEvent?,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (soldEvent == null) return

    val isSold = soldEvent.type == "SOLD"
    val accentColor = if (isSold) SoldEmerald else TimerDanger

    var animTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(soldEvent) {
        animTrigger = true
    }

    val scaleAnim by animateFloatAsState(
        targetValue = if (animTrigger) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "gavelScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scaleAnim)
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.25f),
                            DarkCard.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(2.dp, accentColor.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp)
        ) {
            // Header Badge
            Text(
                text = if (isSold) "🔨 SOLD!" else "UNSOLD",
                color = accentColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player Photo
            val photoUrl = soldEvent.player.displayImage
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = soldEvent.player.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Player Name
            Text(
                text = soldEvent.player.displayName,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isSold && soldEvent.winningBid != null) {
                Spacer(modifier = Modifier.height(8.dp))

                // Winning Team
                Text(
                    text = soldEvent.winningBid.teamName ?: "Team",
                    color = GoldLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Winning Amount Pill
                val priceFormatted = CurrencyFormatter.format(
                    soldEvent.winningBid.amount,
                    currency
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.dp, accentColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = priceFormatted,
                        color = accentColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Player goes to Unsold Pool",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
