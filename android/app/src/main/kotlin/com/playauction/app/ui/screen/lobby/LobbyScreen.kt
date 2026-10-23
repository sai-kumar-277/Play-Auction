package com.playauction.app.ui.screen.lobby

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.playauction.app.data.model.FranchiseDef
import com.playauction.app.data.model.Team
import com.playauction.app.domain.CurrencyFormatter
import com.playauction.app.ui.theme.*
import com.playauction.app.ui.viewmodel.LobbyEvent
import com.playauction.app.ui.viewmodel.LobbyViewModel
import com.playauction.app.ui.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    lobbyViewModel: LobbyViewModel,
    sessionViewModel: SessionViewModel,
    onNavigateToAuction: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by lobbyViewModel.state.collectAsState()
    val context = LocalContext.current
    val currentUserId = sessionViewModel.currentUserId

    var showRetentionSheet by remember { mutableStateOf(false) }

    // Listen to lobby events (e.g. auction started, toast)
    LaunchedEffect(Unit) {
        lobbyViewModel.events.collect { evt ->
            when (evt) {
                is LobbyEvent.NavigateToAuction -> onNavigateToAuction()
                is LobbyEvent.ShowToast -> {
                    Toast.makeText(context, evt.message, Toast.LENGTH_SHORT).show()
                }
                is LobbyEvent.RoomDisbanded -> {
                    Toast.makeText(context, "Room ended by host", Toast.LENGTH_LONG).show()
                    onNavigateBack()
                }
                else -> {}
            }
        }
    }

    val roomState = state.roomState
    val roomCode = roomState?.roomCode ?: "------"
    val isHost = currentUserId != null && currentUserId == roomState?.hostUserId

    val myTeam = roomState?.teams?.firstOrNull { t ->
        t.ownerUserId == currentUserId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ROOM $roomCode",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = GoldPrimary
                        )
                        Text(
                            text = "${state.selectedLeague.uppercase()} • ${roomState?.status ?: "Lobby"}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Copy Room Code Button
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", roomCode))
                        Toast.makeText(context, "Room Code Copied!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Code", tint = TextSecondary)
                    }
                    // Leave Room
                    TextButton(onClick = {
                        lobbyViewModel.leaveRoom()
                        onNavigateBack()
                    }) {
                        Text("Leave", color = OutbidRed, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            // Bottom Action Bar: Retention Setup or Start Auction
            Surface(
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (myTeam != null && state.myRetentionSquad != null && !state.retentionConfirmed) {
                        Button(
                            onClick = { showRetentionSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                "Configure Pre-Auction Retentions (${state.myRetentions.size})",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (isHost) {
                        Button(
                            onClick = { lobbyViewModel.startAuction() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "START AUCTION NOW",
                                color = DarkBackground,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        Text(
                            text = if (myTeam != null) "Waiting for host to start..." else "Select an open franchise to participate",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header instruction banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (myTeam != null) "Secured: ${myTeam.teamName}" else "Available Franchises",
                            color = if (myTeam != null) LiveGreen else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (myTeam != null) "Purse: ${CurrencyFormatter.format(myTeam.currentPurse, roomState?.currency ?: "inr")}" else "Tap a card to claim your team",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    val franchises = when {
                        state.availableTeams.isNotEmpty() -> state.availableTeams
                        !roomState?.availableTeams.isNullOrEmpty() -> roomState!!.availableTeams
                        else -> emptyList()
                    }
                    val claimedCount = roomState?.teams?.count { !it.ownerUserId.isNullOrBlank() } ?: 0
                    val totalTeams = if (franchises.isNotEmpty()) (franchises.size + claimedCount) else (roomState?.teams?.size ?: 0)
                    Text(
                        text = "$claimedCount / $totalTeams Claimed",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Teams Grid
            val franchises = when {
                state.availableTeams.isNotEmpty() -> state.availableTeams
                !roomState?.availableTeams.isNullOrEmpty() -> roomState!!.availableTeams
                else -> emptyList()
            }
            val teamsList = roomState?.teams ?: emptyList()

            if (franchises.isEmpty() && teamsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoldPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.isLoading) "Connecting to auction room..." else "Waiting for room data...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Show already claimed teams first
                    items(teamsList) { team ->
                        TeamClaimCard(
                            team = team,
                            currency = roomState?.currency ?: "inr",
                            isMyTeam = team.ownerUserId == currentUserId,
                            onClaim = {}
                        )
                    }
                    // Show available unclaimed franchises
                    items(franchises) { f ->
                        val displayTeam = Team(
                            franchiseId = f.shortName,
                            teamName = f.name,
                            teamThemeColor = f.primaryColor,
                            teamLogo = f.logoUrl,
                            currentPurse = f.purseLimit
                        )
                        TeamClaimCard(
                            team = displayTeam,
                            currency = roomState?.currency ?: "inr",
                            isMyTeam = false,
                            onClaim = {
                                lobbyViewModel.claimTeam(f.shortName)
                            }
                        )
                    }
                }
            }
        }
    }

    // Retention Bottom Sheet
    if (showRetentionSheet && state.myRetentionSquad != null) {
        ModalBottomSheet(
            onDismissRequest = { showRetentionSheet = false },
            containerColor = DarkSurface
        ) {
            RetentionSheetContent(
                squad = state.myRetentionSquad ?: emptyList(),
                selected = state.myRetentions,
                currency = roomState?.currency ?: "inr",
                onToggle = { name -> lobbyViewModel.toggleRetainedPlayer(name) },
                onSubmit = {
                    lobbyViewModel.submitRetentions()
                    showRetentionSheet = false
                }
            )
        }
    }
}

@Composable
fun TeamClaimCard(
    team: Team,
    currency: String,
    isMyTeam: Boolean,
    onClaim: () -> Unit
) {
    val isClaimed = !team.ownerUserId.isNullOrBlank()
    val teamColor = try {
        Color(android.graphics.Color.parseColor(team.teamThemeColor))
    } catch (_: Exception) {
        GoldPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(
                width = if (isMyTeam) 2.dp else 1.dp,
                color = if (isMyTeam) LiveGreen else BorderSubtle,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !isClaimed) { onClaim() }
            .padding(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Team Logo / Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(teamColor.copy(alpha = 0.2f))
                    .border(1.5.dp, teamColor, CircleShape)
            ) {
                if (!team.teamLogo.isNullOrBlank()) {
                    AsyncImage(
                        model = team.teamLogo,
                        contentDescription = team.teamName,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        text = team.teamName.take(2).uppercase(),
                        color = teamColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Franchise Name
            Text(
                text = team.teamName,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Purse
            Text(
                text = CurrencyFormatter.format(team.currentPurse, currency),
                color = GoldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Owner Status Pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isMyTeam -> LiveGreen.copy(alpha = 0.2f)
                            isClaimed -> DarkSurfaceVariant
                            else -> GoldPrimary.copy(alpha = 0.15f)
                        }
                    )
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        isMyTeam -> "MY FRANCHISE"
                        isClaimed -> team.ownerName ?: "Claimed"
                        else -> "CLAIM TEAM"
                    },
                    color = when {
                        isMyTeam -> LiveGreen
                        isClaimed -> TextSecondary
                        else -> GoldPrimary
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RetentionSheetContent(
    squad: List<com.playauction.app.data.model.RetentionPlayer>,
    selected: List<String>,
    currency: String,
    onToggle: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "PRE-AUCTION RETENTION",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = GoldPrimary
        )
        Text(
            text = "Select players to retain from last season. Cost will deduct from purse.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        squad.forEach { player ->
            val isSelected = selected.contains(player.name)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) GoldPrimary.copy(alpha = 0.15f) else DarkCard)
                    .border(1.dp, if (isSelected) GoldPrimary else BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { onToggle(player.name) }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = player.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val tag = if (player.isUncapped) "Uncapped" else if (player.isOverseas) "Overseas" else "Capped"
                    Text(text = tag, color = TextMuted, fontSize = 11.sp)
                }

                if (isSelected) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = GoldPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Confirm Retentions (${selected.size})", color = DarkBackground, fontWeight = FontWeight.Bold)
        }
    }
}
