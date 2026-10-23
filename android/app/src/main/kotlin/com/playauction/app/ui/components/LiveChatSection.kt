package com.playauction.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playauction.app.data.model.ChatMessage
import com.playauction.app.domain.CurrencyFormatter
import com.playauction.app.ui.theme.*

@Composable
fun LiveChatSection(
    messages: List<ChatMessage>,
    currency: String,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface.copy(alpha = 0.9f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "LIVE ACTIVITY & CHAT",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(messages, key = { index, msg -> "${msg.id}_$index" }) { _, msg ->
                ChatItem(msg, currency)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Say something...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage(textInput.trim())
                        textInput = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                modifier = Modifier.height(46.dp)
            ) {
                Text("Send", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ChatItem(msg: ChatMessage, currency: String) {
    when (msg.type) {
        "bidding_war" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TimerDanger.copy(alpha = 0.15f))
                    .border(1.dp, TimerDanger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "🔥 BIDDING WAR! ${msg.playerName} reaches ${CurrencyFormatter.format(msg.amount ?: 0.0, currency)}",
                    color = TimerDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        "sold" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SoldEmerald.copy(alpha = 0.15f))
                    .border(1.dp, SoldEmerald.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "🔨 ${msg.playerName} SOLD to ${msg.senderTeam ?: "Team"} for ${CurrencyFormatter.format(msg.amount ?: 0.0, currency)}",
                        color = SoldEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!msg.verdict.isNullOrBlank()) {
                        Text(
                            text = msg.verdict,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        "shocking_unsold", "unsold" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCard)
                    .padding(8.dp)
            ) {
                Text(
                    text = "⚠️ ${msg.playerName} went UNSOLD (Base: ${CurrencyFormatter.format(msg.basePrice ?: 0.0, currency)})",
                    color = WarningAmber,
                    fontSize = 12.sp
                )
            }
        }
        else -> {
            // Regular user message
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${msg.senderName ?: "Spectator"}: ",
                    color = GoldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = msg.message ?: "",
                    color = TextPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
