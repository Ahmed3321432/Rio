package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.AiCoachUiState
import com.example.model.CareerStats
import com.example.model.Difficulty
import com.example.model.FormationType
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.model.SquadPlayer
import com.example.model.Team
import com.example.model.TeamCatalog
import com.example.ui.components.AiCoachOverlay
import com.example.ui.components.AudioAndGraphicsPanel
import com.example.ui.components.TacticsAndSubsOverlay
import com.example.ui.components.TeamCrest
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfEmerald

@Composable
fun HomeLobbyScreen(
    settings: GameSettings,
    careerStats: CareerStats,
    lobbyFormation: FormationType,
    lobbyLineup: List<SquadPlayer>,
    onSelectHomeTeam: (Team) -> Unit,
    onSelectAwayTeam: (Team) -> Unit,
    onRandomizeTeams: () -> Unit,
    onSelectFormation: (FormationType) -> Unit,
    onSwapStarters: (Int, Int) -> Unit,
    onSubstitutePlayer: (Int, Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onGraphicsSelected: (GraphicsQuality) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onShowPassIndicatorChange: (Boolean) -> Unit,
    onMatchDurationSelected: (Int) -> Unit,
    aiCoachState: AiCoachUiState,
    onAskAiCoach: (String) -> Unit,
    onRetryAiCoach: () -> Unit,
    onResetAiCoach: () -> Unit,
    onStartMatch: () -> Unit
) {
    val homeTeam = remember(settings.homeTeamId) { TeamCatalog.findById(settings.homeTeamId) }
    val awayTeam = remember(settings.awayTeamId) { TeamCatalog.findById(settings.awayTeamId) }
    var selectingForHome by remember { mutableStateOf(true) }
    var showTactics by remember { mutableStateOf(false) }
    var showAiCoach by remember { mutableStateOf(false) }

    BackHandler(enabled = showAiCoach || showTactics) {
        if (showAiCoach) {
            showAiCoach = false
        } else {
            showTactics = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

    Scaffold(
        containerColor = StadiumObsidian,
        bottomBar = {
            Surface(
                color = StadiumObsidian.copy(alpha = 0.96f),
                tonalElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onStartMatch,
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("kick_off_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurfEmerald,
                            contentColor = Color(0xFF042111)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.kick_off),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Stadium Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(215.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_stadium_hero),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        StadiumObsidian.copy(alpha = 0.55f),
                                        StadiumObsidian.copy(alpha = 0.25f),
                                        StadiumObsidian
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StadiumCard.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = ChampionshipGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${careerStats.wins} فوز • ${careerStats.totalGoals} هدف",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TurfEmerald.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "جودة: ${settings.graphicsQuality.labelAr}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TurfEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White
                            )
                            Text(
                                text = "اختر فريقك، اضبط الصوت والجرافيكس، وانطلق للملعب",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD2E4DA)
                            )
                        }
                    }
                }

                // Matchup VS Card
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MatchupPreviewCard(
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        lobbyFormation = lobbyFormation,
                        selectingForHome = selectingForHome,
                        onPickHomeSlot = { selectingForHome = true },
                        onPickAwaySlot = { selectingForHome = false },
                        onRandomize = onRandomizeTeams,
                        onOpenTactics = { showTactics = true },
                        onOpenAiCoach = { showAiCoach = true }
                    )

                    // Team Selection Strip
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectingForHome) "اختر فريقك من القائمة" else "اختر الفريق الخصم",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${TeamCatalog.teams.size} فرق",
                                style = MaterialTheme.typography.labelMedium,
                                color = TurfEmerald
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(TeamCatalog.teams, key = { it.id }) { team ->
                                val isHomeSelected = team.id == homeTeam.id
                                val isAwaySelected = team.id == awayTeam.id
                                val isCurrentTarget = if (selectingForHome) isHomeSelected else isAwaySelected

                                TeamSelectionCard(
                                    team = team,
                                    isHighlighted = isCurrentTarget,
                                    badgeText = when {
                                        isHomeSelected -> stringResource(R.string.your_team)
                                        isAwaySelected -> stringResource(R.string.opponent_team)
                                        else -> null
                                    },
                                    badgeColor = if (isHomeSelected) TurfEmerald else ChampionshipGold,
                                    onClick = {
                                        if (selectingForHome) onSelectHomeTeam(team) else onSelectAwayTeam(team)
                                    }
                                )
                            }
                        }
                    }

                    // Audio Volume, Graphics Quality & Difficulty Panel
                    AudioAndGraphicsPanel(
                        settings = settings,
                        onVolumeChange = onVolumeChange,
                        onVolumeUp = onVolumeUp,
                        onVolumeDown = onVolumeDown,
                        onToggleMute = onToggleMute,
                        onGraphicsSelected = onGraphicsSelected,
                        onDifficultySelected = onDifficultySelected,
                        onShowPassIndicatorChange = onShowPassIndicatorChange
                    )

                    // Match Duration Picker
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = StadiumCard,
                        border = BorderStroke(1.dp, DividerSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = TurfEmerald
                                )
                                Text(
                                    text = stringResource(R.string.match_time_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(60 to "٦٠ ثانية", 90 to "٩٠ ثانية", 120 to "١٢٠ ثانية").forEach { (sec, label) ->
                                    val selected = settings.matchDurationSeconds == sec
                                    Surface(
                                        onClick = { onMatchDurationSelected(sec) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("duration_$sec"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) TurfEmerald else StadiumElevated,
                                        border = if (selected) null else BorderStroke(1.dp, DividerSubtle)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) Color(0xFF042111) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showTactics) {
        TacticsAndSubsOverlay(
            team = homeTeam,
            formation = lobbyFormation,
            starters = lobbyLineup.take(11),
            bench = lobbyLineup.drop(11),
            staminaByStarterIndex = null,
            isInMatch = false,
            subsUsed = 0,
            maxSubs = 5,
            onSelectFormation = onSelectFormation,
            onSwapStarters = onSwapStarters,
            onSubstitute = onSubstitutePlayer,
            onClose = { showTactics = false }
        )
    }
    if (showAiCoach) {
        AiCoachOverlay(
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            formation = lobbyFormation,
            aiState = aiCoachState,
            onAskQuestion = onAskAiCoach,
            onRetryLast = onRetryAiCoach,
            onClose = {
                showAiCoach = false
                onResetAiCoach()
            }
        )
    }
    }
}

@Composable
private fun MatchupPreviewCard(
    homeTeam: Team,
    awayTeam: Team,
    lobbyFormation: FormationType,
    selectingForHome: Boolean,
    onPickHomeSlot: () -> Unit,
    onPickAwaySlot: () -> Unit,
    onRandomize: () -> Unit,
    onOpenTactics: () -> Unit,
    onOpenAiCoach: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = StadiumCard,
        border = BorderStroke(1.dp, DividerSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Slot
                Surface(
                    onClick = onPickHomeSlot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_team_slot"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectingForHome) TurfEmerald.copy(alpha = 0.14f) else StadiumElevated,
                    border = BorderStroke(
                        width = if (selectingForHome) 2.dp else 1.dp,
                        color = if (selectingForHome) TurfEmerald else DividerSubtle
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.your_team),
                            style = MaterialTheme.typography.labelMedium,
                            color = TurfEmerald
                        )
                        TeamCrest(team = homeTeam, size = 54.dp)
                        Text(
                            text = homeTeam.nameAr,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "تقييم ${homeTeam.overall}",
                            fontFamily = ChakraPetchFontFamily,
                            fontSize = 12.sp,
                            color = ChampionshipGold
                        )
                    }
                }

                // VS Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = StadiumElevated,
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "VS",
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ChampionshipGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                // Away Slot
                Surface(
                    onClick = onPickAwaySlot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("away_team_slot"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (!selectingForHome) ChampionshipGold.copy(alpha = 0.14f) else StadiumElevated,
                    border = BorderStroke(
                        width = if (!selectingForHome) 2.dp else 1.dp,
                        color = if (!selectingForHome) ChampionshipGold else DividerSubtle
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.opponent_team),
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampionshipGold
                        )
                        TeamCrest(team = awayTeam, size = 54.dp)
                        Text(
                            text = awayTeam.nameAr,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "تقييم ${awayTeam.overall}",
                            fontFamily = ChakraPetchFontFamily,
                            fontSize = 12.sp,
                            color = TurfEmerald
                        )
                    }
                }
            }

            // Team Stat Comparison Bars
            StatCompareRow("الهجوم", homeTeam.attack, awayTeam.attack)
            StatCompareRow("الدفاع", homeTeam.defense, awayTeam.defense)
            StatCompareRow("السرعة", homeTeam.speed, awayTeam.speed)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenTactics,
                    modifier = Modifier
                        .weight(1.15f)
                        .height(48.dp)
                        .testTag("open_lobby_tactics_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfEmerald.copy(alpha = 0.20f),
                        contentColor = TurfEmerald
                    ),
                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.65f))
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الخطة (${lobbyFormation.displayName})",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onRandomize,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("randomize_teams_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DividerSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = ChampionshipGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "عشوائي",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            Button(
                onClick = onOpenAiCoach,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_ai_coach_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan.copy(alpha = 0.16f),
                    contentColor = ElectricCyan
                ),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "المدرب الذكي (خطة ونصائح المباراة)",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatCompareRow(label: String, homeVal: Int, awayVal: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$homeVal",
            fontFamily = ChakraPetchFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TurfEmerald,
            modifier = Modifier.width(26.dp)
        )
        LinearProgressIndicator(
            progress = { (homeVal / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape),
            color = TurfEmerald,
            trackColor = StadiumElevated
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(46.dp)
        )
        LinearProgressIndicator(
            progress = { (awayVal / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape),
            color = ChampionshipGold,
            trackColor = StadiumElevated
        )
        Text(
            text = "$awayVal",
            fontFamily = ChakraPetchFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = ChampionshipGold,
            modifier = Modifier.width(26.dp)
        )
    }
}

@Composable
private fun TeamSelectionCard(
    team: Team,
    isHighlighted: Boolean,
    badgeText: String?,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(126.dp)
            .testTag("team_card_${team.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlighted) StadiumElevated else StadiumCard,
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) badgeColor else DividerSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                TeamCrest(team = team, size = 46.dp)
                if (badgeText != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = if (badgeText == stringResource(R.string.your_team)) "أنت" else "خصم",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 10.sp,
                            color = Color(0xFF0A140E),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                text = team.nameAr,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(team.primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(team.secondaryColor)
                )
                Text(
                    text = "${team.overall}",
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ChampionshipGold
                )
            }
        }
    }
}
