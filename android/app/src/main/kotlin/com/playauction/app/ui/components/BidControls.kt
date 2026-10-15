package com.playauction.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playauction.app.data.model.CurrentBid
import com.playauction.app.data.model.Team
import com.playauction.app.domain.CurrencyFormatter
import com.playauction.app.ui.theme.*

@Composable
fun BidControls(
    currentBid: CurrentBid,
    myTeam: Team?,
    options: List<Double>,
    currency: String,
    isPaused: Boolean,
    onPlaceBid: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val myPurse = myTeam?.currentPurse ?: 0.0
    val isLeading = myTeam != null && currentBid.teamName == myTeam.teamName

    val teamColor = try {
        Color(android.graphics.Color.parseColor(myTeam?.teamThemeColor ?: "#D4AF37"))
    } catch (_: Exception) {
        GoldPrimary
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(DarkSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(16.dp)
    ) {
        // Status Row (Purse + Leading Badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REMAINING PURSE",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = CurrencyFormatter.format(myPurse, currency),
                    color = if (myPurse > 0) TextPrimary else OutbidRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLeading) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LiveGreen.copy(alpha = 0.15f))
                        .border(1.dp, LiveGreen, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "YOU ARE LEADING",
                        color = LiveGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else if (currentBid.amount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BorderSubtle)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Leading: ${currentBid.teamName ?: currentBid.ownerName ?: "None"}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Increment Pills
        if (options.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.drop(1).take(3).forEach { bidVal ->
                    val canAfford = bidVal <= myPurse
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canAfford) DarkSurfaceVariant else DarkCard)
                            .border(1.dp, if (canAfford) BorderSubtle else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable(enabled = canAfford && !isPaused && !isLeading) {
                                onPlaceBid(bidVal)
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = CurrencyFormatter.format(bidVal, currency),
                            color = if (canAfford && !isLeading) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Primary Minimum Bid Action Button
        val nextMinBid = options.firstOrNull() ?: (currentBid.amount + 25.0)
        val canBid = !isPaused && !isLeading && (myTeam != null) && (nextMinBid <= myPurse)

        val btnBgColor by animateColorAsState(
            targetValue = when {
                isLeading -> LiveGreen.copy(alpha = 0.3f)
                canBid -> teamColor
                else -> DarkCard
            },
            label = "bidBtnColor"
        )

        Button(
            onClick = { if (canBid) onPlaceBid(nextMinBid) },
            enabled = canBid,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = btnBgColor,
                disabledContainerColor = DarkSurfaceVariant
            )
        ) {
            Text(
                text = when {
                    isPaused -> "AUCTION PAUSED"
                    isLeading -> "HOLDING HIGHEST BID"
                    myTeam == null -> "SPECTATOR MODE"
                    nextMinBid > myPurse -> "INSUFFICIENT FUNDS"
                    else -> "BID ${CurrencyFormatter.format(nextMinBid, currency)}"
                },
                color = if (canBid) DarkBackground else TextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}
