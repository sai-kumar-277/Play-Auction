package com.playauction.app.ui.screen.auction

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playauction.app.domain.CurrencyFormatter
import com.playauction.app.ui.components.*
import com.playauction.app.ui.theme.*
import com.playauction.app.ui.viewmodel.AuctionEvent
import com.playauction.app.ui.viewmodel.AuctionViewModel
import com.playauction.app.util.HapticManager
import com.playauction.app.util.SoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen(
    auctionViewModel: AuctionViewModel,
    soundManager: SoundManager,
    hapticManager: HapticManager,
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToEvaluating: (String) -> Unit,
    onNavigateToResults: () -> Unit,
    onLeaveAuction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by auctionViewModel.state.collectAsState()
    val context = LocalContext.current

    // Listen to one-shot auction events
    LaunchedEffect(Unit) {
        auctionViewModel.events.collect { evt ->
            when (evt) {
                is AuctionEvent.PlayBidSound -> {
                    soundManager.playBidSound()
                    hapticManager.bidPlaced()
                }
                is AuctionEvent.PlayWarnSound -> {
                    soundManager.playWarningBeep()
                    hapticManager.timerWarning()
                }
                is AuctionEvent.PlayGavelSlam -> {
                    soundManager.playGavelSlam(evt.type)
                    hapticManager.hammerSlam()
                }
                is AuctionEvent.HapticFeedback -> {
                    hapticManager.timerWarning()
                }
                is AuctionEvent.ShowToast -> {
                    Toast.makeText(context, evt.message, Toast.LENGTH_SHORT).show()
                }
                is AuctionEvent.NavigateToQuiz -> {
                    onNavigateToQuiz(evt.roomCode)
                }
                is AuctionEvent.NavigateToEvaluating -> {
                    onNavigateToEvaluating(evt.roomCode)
                }
                is AuctionEvent.NavigateToResults -> {
                    onNavigateToResults()
                }
                is AuctionEvent.RoomDisbanded -> {
                    Toast.makeText(context, "Auction room closed", Toast.LENGTH_LONG).show()
                    onLeaveAuction()
                }
                else -> {}
            }
        }
    }

    val bidOptions = remember(state.currentBid.amount, state.currentPlayer) {
        auctionViewModel.getNextBidAmounts()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ROOM ${state.roomCode}",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = GoldPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (state.isPaused) WarningAmber.copy(alpha = 0.2f) else LiveGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (state.isPaused) "PAUSED" else "LIVE",
                                    color = if (state.isPaused) WarningAmber else LiveGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        // Host Controls Pill
                        if (state.isHost || state.isCoHost) {
                            TextButton(onClick = {
                                if (state.isPaused) auctionViewModel.resumeAuction() else auctionViewModel.pauseAuction()
                            }) {
                                Text(
                                    text = if (state.isPaused) "Resume" else "Pause",
                                    color = if (state.isPaused) LiveGreen else WarningAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (state.isAiMode) {
                                TextButton(onClick = { auctionViewModel.skipPlayer() }) {
                                    Text("Skip", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            },
            bottomBar = {
                // Bid Controls
                BidControls(
                    currentBid = state.currentBid,
                    myTeam = state.myTeam,
                    options = bidOptions,
                    currency = state.currency,
                    isPaused = state.isPaused,
                    onPlaceBid = { amt -> auctionViewModel.placeBid(amt) }
                )
            },
            containerColor = DarkBackground
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Timer & Leading Bid Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Countdown Timer
                        AuctionTimer(
                            timer = state.timer,
                            timerDuration = state.timerDuration,
                            size = 72.dp
                        )

                        // Current Bid Info Box
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "CURRENT BID",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = if (state.currentBid.amount > 0)
                                    CurrencyFormatter.format(state.currentBid.amount, state.currency)
                                else
                                    "No Bids Yet",
                                color = if (state.currentBid.amount > 0) GoldLight else TextMuted,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (!state.currentBid.teamName.isNullOrBlank()) {
                                Text(
                                    text = "by ${state.currentBid.teamName}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Current Player Card
                item {
                    PlayerCard(
                        player = state.currentPlayer,
                        currency = state.currency
                    )
                }

                // Live Activity & Chat (Fixed height embedded inside scroll)
                item {
                    LiveChatSection(
                        messages = state.chatMessages,
                        currency = state.currency,
                        onSendMessage = { text -> auctionViewModel.sendChat(text) },
                        modifier = Modifier.height(260.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // RTM Decision Modal Dialog
        val rtm = state.rtmState
        if (rtm != null) {
            val isMyRtm = rtm.prevTeamOwnerUserId == state.myTeam?.ownerUserId
            AlertDialog(
                onDismissRequest = { /* Modal blocks */ },
                title = { Text("RIGHT TO MATCH (RTM) CARD", fontWeight = FontWeight.Black, color = GoldPrimary) },
                text = {
                    Column {
                        Text(
                            text = "${rtm.playerName} has reached highest bid of ${CurrencyFormatter.format(rtm.currentBid.amount, state.currency)} by ${rtm.currentBid.teamName}.",
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isMyRtm)
                                "You previously owned this player. Do you want to match this bid and reclaim them?"
                            else
                                "Waiting for previous owner (${rtm.prevTeamName}) to decide on RTM (${rtm.timer}s)...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    if (isMyRtm) {
                        Button(
                            onClick = { auctionViewModel.rtmDecision(useRtm = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveGreen)
                        ) {
                            Text("Use RTM Card", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    if (isMyRtm) {
                        TextButton(onClick = { auctionViewModel.rtmDecision(useRtm = false) }) {
                            Text("Decline RTM", color = OutbidRed)
                        }
                    }
                },
                containerColor = DarkSurface
            )
        }

        // Gavel Slam Announcement Overlay
        GavelSlamOverlay(
            soldEvent = state.soldEvent,
            currency = state.currency
        )
    }
}
