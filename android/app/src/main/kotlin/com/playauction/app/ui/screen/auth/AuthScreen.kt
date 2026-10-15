package com.playauction.app.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playauction.app.data.model.League
import com.playauction.app.ui.theme.*
import com.playauction.app.ui.viewmodel.LobbyViewModel
import com.playauction.app.ui.viewmodel.SessionViewModel

@Composable
fun AuthScreen(
    sessionViewModel: SessionViewModel,
    lobbyViewModel: LobbyViewModel,
    onNavigateToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember { mutableStateOf("") }
    var roomCodeInput by remember { mutableStateOf("") }
    var selectedLeague by remember { mutableStateOf(League.IPL) }
    var serverUrl by remember { mutableStateOf("https://playauction.onrender.com/") }
    var showServerDialog by remember { mutableStateOf(false) }

    val savedName by sessionViewModel.playerNameFlow.collectAsState(initial = "")
    LaunchedEffect(savedName) {
        if (!savedName.isNullOrBlank() && nameInput.isBlank()) {
            nameInput = savedName ?: ""
        }
    }

    val lobbyState by lobbyViewModel.state.collectAsState()

    // Fetch public rooms on open
    LaunchedEffect(Unit) {
        lobbyViewModel.fetchPublicRooms()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(36.dp))

                // Settings icon button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { showServerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Server Settings",
                            tint = TextSecondary
                        )
                    }
                }

                // Brand Header
                Text(
                    text = "PLAYAUCTION",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = GoldPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Real-Time Multi-League Cricket Auction",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Player Name Field
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Your Manager Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // League Selector Pills
                Text(
                    text = "SELECT TOURNAMENT",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    League.values().forEach { lg ->
                        val isSel = selectedLeague == lg
                        val accentColor = Color(lg.accentColor)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) accentColor.copy(alpha = 0.2f) else DarkSurface)
                                .border(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) accentColor else BorderSubtle,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedLeague = lg
                                    lobbyViewModel.setSelectedLeague(lg.id)
                                }
                                .padding(vertical = 14.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = lg.label,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = if (isSel) accentColor else TextSecondary
                                )
                                Text(
                                    text = "${lg.defaultTeamCount} Teams",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Join Existing Room
                OutlinedTextField(
                    value = roomCodeInput,
                    onValueChange = { roomCodeInput = it.uppercase().take(6) },
                    label = { Text("6-Digit Room Code") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanSa20,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = CyanSa20,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Join Button
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank() && roomCodeInput.isNotBlank()) {
                                sessionViewModel.initSession(nameInput.trim(), serverUrl)
                                lobbyViewModel.joinRoom(roomCodeInput.trim())
                                onNavigateToLobby()
                            }
                        },
                        enabled = nameInput.isNotBlank() && roomCodeInput.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanSa20),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Join Room", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }

                    // Watch / Spectate Button
                    OutlinedButton(
                        onClick = {
                            if (nameInput.isNotBlank() && roomCodeInput.isNotBlank()) {
                                sessionViewModel.initSession(nameInput.trim(), serverUrl)
                                lobbyViewModel.joinRoom(roomCodeInput.trim(), asSpectator = true)
                                onNavigateToLobby()
                            }
                        },
                        enabled = nameInput.isNotBlank() && roomCodeInput.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text("Watch / Spectate", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OR Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                    Text(" OR ", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Create Room Button
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            sessionViewModel.initSession(nameInput.trim(), serverUrl)
                            lobbyViewModel.setSelectedLeague(selectedLeague.id)
                            lobbyViewModel.createRoom()
                            onNavigateToLobby()
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Create ${selectedLeague.label} Room",
                        color = DarkBackground,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Public Rooms Section
                if (lobbyState.publicRooms.isNotEmpty()) {
                    Text(
                        text = "PUBLIC ROOMS",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Public Rooms List
            items(lobbyState.publicRooms) { room ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            if (nameInput.isNotBlank()) {
                                sessionViewModel.initSession(nameInput.trim(), serverUrl)
                                lobbyViewModel.joinRoom(room.roomCode)
                                onNavigateToLobby()
                            }
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Room ${room.roomCode} • ${room.league.uppercase()}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Host: ${room.hostName}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "${room.teamsCount}/${room.maxTeams} Teams",
                            color = GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Server Config Dialog
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Server Backend URL") },
            text = {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Backend URL") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Save")
                }
            }
        )
    }
}
