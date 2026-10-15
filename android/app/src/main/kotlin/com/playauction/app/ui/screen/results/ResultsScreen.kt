package com.playauction.app.ui.screen.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.playauction.app.data.model.QuizLeaderboardEntry
import com.playauction.app.data.model.Team
import com.playauction.app.ui.theme.*
import com.playauction.app.ui.viewmodel.ResultsViewModel

@Composable
fun ResultsScreen(
    resultsViewModel: ResultsViewModel,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by resultsViewModel.state.collectAsState()
    val teams = state.teams
    val selectedTeam = teams.getOrNull(state.selectedTeamIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Title
                Text(
                    text = "AUCTION RESULTS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = GoldPrimary
                )
                Text(
                    text = "Final Squad Evaluations & League Standings",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Team Selector Horizontal Strip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(teams) { idx, team ->
                        val isSel = state.selectedTeamIndex == idx
                        val teamColor = try {
                            Color(android.graphics.Color.parseColor(team.teamThemeColor))
                        } catch (_: Exception) {
                            GoldPrimary
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) teamColor.copy(alpha = 0.25f) else DarkSurface)
                                .border(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) teamColor else BorderSubtle,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { resultsViewModel.selectTeam(idx) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!team.teamLogo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = team.teamLogo,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = team.teamName,
                                    color = if (isSel) teamColor else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Selected Team AI Squad Report Card
            if (selectedTeam != null) {
                item {
                    val eval = selectedTeam.evaluation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkCard)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedTeam.teamName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Owner: ${selectedTeam.ownerName ?: "AI Bot"}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                // Overall Score Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GoldDim)
                                        .border(1.dp, GoldPrimary, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${eval?.overallScore ?: eval?.score ?: 0.0} / 100",
                                        color = GoldLight,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            if (!eval?.titleProbability.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Title Probability: ${eval?.titleProbability}",
                                    color = CyanSa20Light,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // AI Analysis Narratives
                            val analysis = eval?.analysis
                            if (analysis != null) {
                                Spacer(modifier = Modifier.height(14.dp))

                                analysis.battingNarrative?.let {
                                    AnalysisSection("🏏 Batting Narrative", it)
                                }
                                analysis.bowlingNarrative?.let {
                                    AnalysisSection("🎯 Bowling Narrative", it)
                                }
                                analysis.tacticalBalance?.let {
                                    AnalysisSection("⚖️ Tactical Balance", it)
                                }
                                analysis.keyRisksAndFixes?.let {
                                    AnalysisSection("⚠️ Key Risks & Fixes", it)
                                }
                            }

                            // Recommended Playing XI
                            if (selectedTeam.playing11.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "RECOMMENDED PLAYING XI",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                selectedTeam.playing11.forEachIndexed { i, player ->
                                    Text(
                                        text = "${i + 1}. $player",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quiz Podium Section
            if (state.quizLeaderboard.isNotEmpty()) {
                item {
                    Text(
                        text = "QUIZ ARENA CHAMPIONS",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurface)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.quizLeaderboard.take(3).forEachIndexed { i, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${when (i) { 0 -> "🥇"; 1 -> "🥈"; else -> "🥉" }} ${entry.name}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${entry.score} pts",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Return Home Button
            item {
                Button(
                    onClick = onNavigateHome,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Return to Main Menu", color = DarkBackground, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AnalysisSection(title: String, text: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = title, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(text = text, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
