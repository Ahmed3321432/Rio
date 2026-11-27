package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.AiCoachUiState
import com.example.model.Difficulty
import com.example.model.FormationType
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.model.MatchPhase
import com.example.model.MatchState
import com.example.model.SquadPlayer
import com.example.model.TeamSide
import com.example.ui.components.AiCoachOverlay
import com.example.ui.components.AudioAndGraphicsPanel
import com.example.ui.components.DynamicScoreboard
import com.example.ui.components.MatchTouchControls
import com.example.ui.components.PitchCanvas
import com.example.ui.components.PitchRadarOverlay
import com.example.ui.components.TacticsAndSubsOverlay
import com.example.ui.components.TeamCrest
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfEmerald

@Composable
fun MatchScreen(
    matchState: MatchState,
    settings: GameSettings,
    onJoystickMove: (Float, Float) -> Unit,
    onSprintChanged: (Boolean) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    onShootPressed: () -> Unit,
    onShootReleased: () -> Unit,
    onShortPassOrSwitch: () -> Unit,
    onThroughPassOrSlide: () -> Unit,
    onSwitchPlayer: () -> Unit,
    onStartSecondHalf: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToLobby: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onGraphicsSelected: (GraphicsQuality) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onShowPassIndicatorChange: (Boolean) -> Unit,
    onSelectFormation: (FormationType) -> Unit,
    onSwapStarters: (Int, Int) -> Unit,
    onSubstitutePlayer: (Int, Int) -> Unit,
    aiCoachState: AiCoachUiState,
    onAnalyzeMatchWithAi: () -> Unit,
    onAskAiCoach: (String) -> Unit,
    onRetryAiCoach: () -> Unit,
    onResetAiCoach: () -> Unit
) {
    var showTactics by remember { mutableStateOf(false) }
    var showAiCoach by remember { mutableStateOf(false) }

    // Switch to widescreen Landscape for authentic PES/FIFA gameplay
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = prevOrientation
        }
    }

    BackHandler {
        when {
            showAiCoach -> {
                showAiCoach = false
                onResetAiCoach()
            }
            showTactics -> showTactics = false
            matchState.phase == MatchPhase.PAUSED -> onResume()
            matchState.phase == MatchPhase.FULL_TIME -> onExitToLobby()
            else -> onPause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumObsidian)
    ) {
        // Full-Bleed Broadcast Stadium Canvas
        PitchCanvas(
            matchState = matchState,
            graphicsQuality = settings.graphicsQuality,
            showPassIndicator = settings.showPassIndicator,
            modifier = Modifier.fillMaxSize()
        )

        // Broadcast Top HUD (Scoreboard on Left, Audio, Tactics & Pause on Right)
        MatchBroadcastTopHud(
            matchState = matchState,
            settings = settings,
            onVolumeDown = onVolumeDown,
            onVolumeUp = onVolumeUp,
            onToggleMute = onToggleMute,
            onOpenTactics = {
                onPause()
                showTactics = true
            },
            onPause = onPause,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Goal / Set-Piece / Kickoff Banner
        AnimatedVisibility(
            visible = matchState.bannerMessage != null,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 66.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StadiumObsidian.copy(alpha = 0.88f),
                border = BorderStroke(
                    2.dp,
                    when (matchState.lastScorerTeam) {
                        TeamSide.HOME -> TurfEmerald
                        TeamSide.AWAY -> CrimsonAlert
                        null -> ChampionshipGold
                    }
                )
            ) {
                Text(
                    text = matchState.bannerMessage.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }

        // Bottom-Center Tactical Radar + Active Player Name Pill (just like PES/FIFA!)
        if (matchState.phase == MatchPhase.PLAYING || matchState.phase == MatchPhase.PRE_KICKOFF || matchState.phase == MatchPhase.SET_PIECE) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                matchState.activeHomePlayer?.let { active ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StadiumObsidian.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${active.name}  #${active.jerseyNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
                PitchRadarOverlay(matchState = matchState)
            }

            // Touch Controls (Fixed LTR so Joystick is always on Left and 4 PES/FIFA buttons on Right)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MatchTouchControls(
                    homeHasBall = matchState.homeHasBall,
                    shotCharge = matchState.shotCharge,
                    onJoystickMove = onJoystickMove,
                    onSprintChanged = onSprintChanged,
                    onPressChanged = onPressChanged,
                    onShootPressed = onShootPressed,
                    onShootReleased = onShootReleased,
                    onShortPassOrSwitch = onShortPassOrSwitch,
                    onThroughPassOrSlide = onThroughPassOrSlide,
                    onSwitchPlayer = onSwitchPlayer,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Halftime Overlay
        if (matchState.phase == MatchPhase.HALF_TIME) {
            HalfTimeOverlay(
                matchState = matchState,
                onStartSecondHalf = onStartSecondHalf
            )
        }

        // Pause & Settings Overlay
        if (matchState.phase == MatchPhase.PAUSED && !showTactics) {
            PauseOverlay(
                matchState = matchState,
                settings = settings,
                onOpenTactics = { showTactics = true },
                onResume = onResume,
                onRestart = onRestart,
                onExitToLobby = onExitToLobby,
                onVolumeChange = onVolumeChange,
                onVolumeUp = onVolumeUp,
                onVolumeDown = onVolumeDown,
                onToggleMute = onToggleMute,
                onGraphicsSelected = onGraphicsSelected,
                onDifficultySelected = onDifficultySelected,
                onShowPassIndicatorChange = onShowPassIndicatorChange
            )
        }

        // In-Match Tactics & Substitutions Overlay
        if (showTactics) {
            val homeStartersOrdered = remember(matchState.players) {
                (0..10).mapNotNull { idx ->
                    val p = matchState.players.firstOrNull { it.id == "home_$idx" }
                    p?.let {
                        SquadPlayer(
                            id = it.squadId.ifEmpty { it.id },
                            name = it.name,
                            jerseyNumber = it.jerseyNumber,
                            preferredRole = it.role,
                            rating = it.rating,
                            pace = it.paceRating,
                            shooting = it.shootingRating,
                            skinToneIndex = it.skinToneIndex,
                            hairStyleIndex = it.hairStyleIndex
                        )
                    }
                }
            }
            val staminaList = remember(matchState.players) {
                (0..10).map { idx ->
                    matchState.players.firstOrNull { it.id == "home_$idx" }?.stamina ?: 1f
                }
            }
            TacticsAndSubsOverlay(
                team = matchState.homeTeam,
                formation = matchState.homeFormation,
                starters = homeStartersOrdered,
                bench = matchState.homeBench,
                staminaByStarterIndex = staminaList,
                isInMatch = true,
                subsUsed = matchState.subsUsed,
                maxSubs = matchState.maxSubs,
                onSelectFormation = onSelectFormation,
                onSwapStarters = onSwapStarters,
                onSubstitute = onSubstitutePlayer,
                onClose = {
                    showTactics = false
                    onResume()
                }
            )
        }

        // Full-Time Results Overlay
        if (matchState.phase == MatchPhase.FULL_TIME) {
            FullTimeOverlay(
                matchState = matchState,
                onAnalyzeMatchWithAi = {
                    showAiCoach = true
                    onAnalyzeMatchWithAi()
                },
                onRestart = onRestart,
                onExitToLobby = onExitToLobby
            )
        }

        if (showAiCoach) {
            AiCoachOverlay(
                homeTeam = matchState.homeTeam,
                awayTeam = matchState.awayTeam,
                formation = matchState.homeFormation,
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
private fun MatchBroadcastTopHud(
    matchState: MatchState,
    settings: GameSettings,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenTactics: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real-Time Dynamic Broadcast Scoreboard
            DynamicScoreboard(matchState = matchState)

            // Right: Live Volume -/Mute/+ & Pause
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = StadiumObsidian.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, DividerSubtle)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onVolumeDown,
                            modifier = Modifier.size(44.dp).testTag("hud_volume_down")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "توطية الصوت", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.size(44.dp).testTag("hud_mute")
                        ) {
                            Icon(
                                imageVector = if (settings.isMuted || settings.volume <= 0.01f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "كتم الصوت",
                                tint = if (settings.isMuted) CrimsonAlert else TurfEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (settings.isMuted) "0%" else "${(settings.volume * 100).toInt()}%",
                            fontFamily = ChakraPetchFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = onVolumeUp,
                            modifier = Modifier.size(44.dp).testTag("hud_volume_up")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "تعلية الصوت", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = StadiumObsidian.copy(alpha = 0.84f),
                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.55f))
                ) {
                    IconButton(
                        onClick = onOpenTactics,
                        modifier = Modifier.size(48.dp).testTag("hud_tactics_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "الخطة والتبديلات", tint = TurfEmerald)
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = StadiumObsidian.copy(alpha = 0.84f),
                    border = BorderStroke(1.dp, DividerSubtle)
                ) {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier.size(48.dp).testTag("pause_button")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.pause_title), tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun HalfTimeOverlay(
    matchState: MatchState,
    onStartSecondHalf: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.74f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = StadiumCard,
            border = BorderStroke(1.5.dp, ChampionshipGold.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "نهاية الشوط الأول",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ChampionshipGold
                )
                Text(
                    text = "${matchState.homeTeam.shortCode}  ${matchState.homeScore}  -  ${matchState.awayScore}  ${matchState.awayTeam.shortCode}",
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White
                )
                Text(
                    text = "سيتم تبديل اتجاه الملعب في الشوط الثاني",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onStartSecondHalf,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("start_second_half_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = Color(0xFF042111))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("بدء الشوط الثاني", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    matchState: MatchState,
    settings: GameSettings,
    onOpenTactics: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToLobby: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onGraphicsSelected: (GraphicsQuality) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onShowPassIndicatorChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = StadiumCard,
            border = BorderStroke(1.dp, DividerSubtle)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.pause_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                OutlinedButton(
                    onClick = onOpenTactics,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_tactics_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.65f))
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = TurfEmerald)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "الخطة والتبديلات (${matchState.homeFormation.displayName}) • متبقي ${matchState.maxSubs - matchState.subsUsed}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TurfEmerald
                    )
                }

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

                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = Color(0xFF042111))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.resume_game), style = MaterialTheme.typography.labelLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("restart_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.restart_match), color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onExitToLobby,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("exit_lobby_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = CrimsonAlert)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.exit_lobby), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FullTimeOverlay(
    matchState: MatchState,
    onAnalyzeMatchWithAi: () -> Unit,
    onRestart: () -> Unit,
    onExitToLobby: () -> Unit
) {
    val resultTitle = when {
        matchState.homeScore > matchState.awayScore -> stringResource(R.string.you_win)
        matchState.homeScore < matchState.awayScore -> stringResource(R.string.you_lose)
        else -> stringResource(R.string.draw_result)
    }
    val resultColor = when {
        matchState.homeScore > matchState.awayScore -> TurfEmerald
        matchState.homeScore < matchState.awayScore -> CrimsonAlert
        else -> ChampionshipGold
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = StadiumCard,
            border = BorderStroke(1.5.dp, resultColor.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = ChampionshipGold,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = stringResource(R.string.full_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = resultTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = resultColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TeamCrest(team = matchState.homeTeam, size = 44.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(matchState.homeTeam.nameAr, style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                    Text(
                        text = "${matchState.homeScore}  -  ${matchState.awayScore}",
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color.White
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TeamCrest(team = matchState.awayTeam, size = 44.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(matchState.awayTeam.nameAr, style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = StadiumElevated
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FullTimeStatLine("الاستحواذ", "${matchState.stats.homePossessionPercent}%", "${matchState.stats.awayPossessionPercent}%")
                        FullTimeStatLine("التسديدات", "${matchState.stats.homeShots}", "${matchState.stats.awayShots}")
                        FullTimeStatLine("الركنيات", "${matchState.stats.homeCorners}", "${matchState.stats.awayCorners}")
                        FullTimeStatLine("الافتكاك", "${matchState.stats.homeTackles}", "${matchState.stats.awayTackles}")
                    }
                }

                Button(
                    onClick = onAnalyzeMatchWithAi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("fulltime_ai_analysis_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan.copy(alpha = 0.18f),
                        contentColor = ElectricCyan
                    ),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تحليل المدرب الذكي", style = MaterialTheme.typography.labelLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("rematch_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = Color(0xFF042111))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.restart_match), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExitToLobby,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("fulltime_lobby_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = ChampionshipGold)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.exit_lobby), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FullTimeStatLine(label: String, homeValue: String, awayValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = homeValue, fontFamily = ChakraPetchFontFamily, fontWeight = FontWeight.Bold, color = TurfEmerald)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = awayValue, fontFamily = ChakraPetchFontFamily, fontWeight = FontWeight.Bold, color = ChampionshipGold)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
