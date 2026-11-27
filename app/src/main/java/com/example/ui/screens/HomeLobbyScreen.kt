package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.model.CameraDistance
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
    onCommentaryEnabledChange: (Boolean) -> Unit = {},
    onGraphicsSelected: (GraphicsQuality) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onCameraDistanceSelected: (CameraDistance) -> Unit = {},
    onShowPassIndicatorChange: (Boolean) -> Unit,
    onMatchDurationSelected: (Int) -> Unit,
    onResetPreferences: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBackToMenu: () -> Unit = {},
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

    BackHandler(enabled = true) {
        when {
            showAiCoach -> {
                showAiCoach = false
                onResetAiCoach()
            }
            showTactics -> showTactics = false
            else -> onBackToMenu()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumObsidian)
    ) {
        // Subtle Stadium Atmosphere Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_stadium_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            StadiumObsidian.copy(alpha = 0.88f),
                            StadiumObsidian.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact Widescreen Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = StadiumCard.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, DividerSubtle)
                    ) {
                        IconButton(
                            onClick = onBackToMenu,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("lobby_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "العودة للأطوار",
                                tint = Color.White
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "مباراة سريعة • Quick Match",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "اختر الفرق وعدّل التشكيلة وانطلق للملعب",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC2D6CB)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StadiumCard.copy(alpha = 0.88f),
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
                        color = TurfEmerald.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "${settings.graphicsQuality.labelAr} • ${settings.difficulty.labelAr}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TurfEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = StadiumCard.copy(alpha = 0.9f),
                        border = BorderStroke(1.dp, DividerSubtle)
                    ) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Two-Column Widescreen Body
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 1040.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left/Start Column: Matchup VS Card + Tactics + AI Coach
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                }

                // Right/End Column: Teams Strip + Duration + Quick Audio/Diff + Kick Off CTA
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Team Selection Strip
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = StadiumCard,
                            border = BorderStroke(1.dp, DividerSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectingForHome) "اختر فريقك من القائمة" else "اختر الفريق الخصم",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${TeamCatalog.teams.size} فرق",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TurfEmerald
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        }

                        // Match Duration Picker
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = StadiumCard,
                            border = BorderStroke(1.dp, DividerSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = TurfEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.match_time_label),
                                        style = MaterialTheme.typography.titleSmall,
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
                                                .height(44.dp)
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

                        AudioAndGraphicsPanel(
                            settings = settings,
                            onVolumeChange = onVolumeChange,
                            onVolumeUp = onVolumeUp,
                            onVolumeDown = onVolumeDown,
                            onToggleMute = onToggleMute,
                            onCommentaryEnabledChange = onCommentaryEnabledChange,
                            onGraphicsSelected = onGraphicsSelected,
                            onDifficultySelected = onDifficultySelected,
                            onCameraDistanceSelected = onCameraDistanceSelected,
                            onShowPassIndicatorChange = onShowPassIndicatorChange,
                            onResetPreferences = onResetPreferences
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Prominent Kick-Off Button pinned at bottom of Right Column
                    Button(
                        onClick = onStartMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("kick_off_button"),
                        shape = RoundedCornerShape(16.dp),
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
        shape = RoundedCornerShape(20.dp),
        color = StadiumCard,
        border = BorderStroke(1.dp, DividerSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPickHomeSlot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_team_slot"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectingForHome) TurfEmerald.copy(alpha = 0.14f) else StadiumElevated,
                    border = BorderStroke(
                        width = if (selectingForHome) 2.dp else 1.dp,
                        color = if (selectingForHome) TurfEmerald else DividerSubtle
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.your_team),
                            style = MaterialTheme.typography.labelSmall,
                            color = TurfEmerald
                        )
                        TeamCrest(team = homeTeam, size = 44.dp)
                        Text(
                            text = homeTeam.nameAr,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "تقييم ${homeTeam.overall}",
                            fontFamily = ChakraPetchFontFamily,
                            fontSize = 11.sp,
                            color = ChampionshipGold
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = StadiumElevated,
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "VS",
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ChampionshipGold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                    )
                }

                Surface(
                    onClick = onPickAwaySlot,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("away_team_slot"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (!selectingForHome) ChampionshipGold.copy(alpha = 0.14f) else StadiumElevated,
                    border = BorderStroke(
                        width = if (!selectingForHome) 2.dp else 1.dp,
                        color = if (!selectingForHome) ChampionshipGold else DividerSubtle
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.opponent_team),
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampionshipGold
                        )
                        TeamCrest(team = awayTeam, size = 44.dp)
                        Text(
                            text = awayTeam.nameAr,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "تقييم ${awayTeam.overall}",
                            fontFamily = ChakraPetchFontFamily,
                            fontSize = 11.sp,
                            color = TurfEmerald
                        )
                    }
                }
            }

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
                        .weight(1.2f)
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
                        .weight(0.8f)
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
            modifier = Modifier.width(24.dp)
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
            modifier = Modifier.width(44.dp)
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
            modifier = Modifier.width(24.dp)
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
            .width(114.dp)
            .testTag("team_card_${team.id}"),
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlighted) StadiumElevated else StadiumCard,
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) badgeColor else DividerSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                TeamCrest(team = team, size = 40.dp)
                if (badgeText != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor
                    ) {
                        Text(
                            text = if (badgeText == stringResource(R.string.your_team)) "أنت" else "خصم",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 9.sp,
                            color = Color(0xFF0A140E),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                text = team.nameAr,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.sp,
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
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(team.primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(team.secondaryColor)
                )
                Text(
                    text = "${team.overall}",
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = ChampionshipGold
                )
            }
        }
    }
}
