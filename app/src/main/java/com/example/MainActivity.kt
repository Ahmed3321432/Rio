package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.ui.AppScreen
import com.example.ui.FootballViewModel
import com.example.ui.PreferencesViewModel
import com.example.ui.screens.HomeLobbyScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MatchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.KickChampionsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            KickChampionsTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val vm: FootballViewModel = viewModel()
                    val prefsVm: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory)
                    val screen by vm.currentScreen.collectAsStateWithLifecycle()
                    val settings by prefsVm.settings.collectAsStateWithLifecycle()
                    val careerStats by vm.careerStats.collectAsStateWithLifecycle()
                    val matchState by vm.matchState.collectAsStateWithLifecycle()
                    val lobbyFormation by vm.lobbyFormation.collectAsStateWithLifecycle()
                    val lobbyLineup by vm.lobbyLineup.collectAsStateWithLifecycle()
                    val aiCoachState by vm.aiCoachState.collectAsStateWithLifecycle()
                    val liveCommentary by vm.liveCommentary.collectAsStateWithLifecycle()

                    Crossfade(targetState = screen, label = "screen_transition") { current ->
                        when (current) {
                            AppScreen.MENU -> {
                                MainMenuScreen(
                                    settings = settings,
                                    careerStats = careerStats,
                                    onOpenQuickMatch = vm::openQuickMatch,
                                    onOpenSettings = vm::openSettings
                                )
                            }

                            AppScreen.HOME -> {
                                HomeLobbyScreen(
                                    settings = settings,
                                    careerStats = careerStats,
                                    lobbyFormation = lobbyFormation,
                                    lobbyLineup = lobbyLineup,
                                    onSelectHomeTeam = vm::selectHomeTeam,
                                    onSelectAwayTeam = vm::selectAwayTeam,
                                    onRandomizeTeams = vm::randomizeTeams,
                                    onSelectFormation = vm::setLobbyFormation,
                                    onSwapStarters = vm::swapLobbyStarters,
                                    onSubstitutePlayer = vm::substituteLobbyPlayer,
                                    onVolumeChange = prefsVm::setVolume,
                                    onVolumeUp = prefsVm::increaseVolume,
                                    onVolumeDown = prefsVm::decreaseVolume,
                                    onToggleMute = prefsVm::toggleMute,
                                    onCommentaryEnabledChange = prefsVm::setCommentaryEnabled,
                                    onGraphicsSelected = prefsVm::setGraphicsQuality,
                                    onDifficultySelected = prefsVm::setDifficulty,
                                    onCameraDistanceSelected = prefsVm::setCameraDistance,
                                    onShowPassIndicatorChange = prefsVm::setShowPassIndicator,
                                    onMatchDurationSelected = prefsVm::setMatchDuration,
                                    onResetPreferences = prefsVm::resetPreferences,
                                    onOpenSettings = vm::openSettings,
                                    onBackToMenu = vm::backToMenu,
                                    aiCoachState = aiCoachState,
                                    onAskAiCoach = vm::askAiCoach,
                                    onRetryAiCoach = vm::retryAiCoach,
                                    onResetAiCoach = vm::resetAiCoach,
                                    onStartMatch = vm::startMatch
                                )
                            }

                            AppScreen.MATCH -> {
                                MatchScreen(
                                    matchState = matchState,
                                    settings = settings,
                                    liveCommentary = liveCommentary,
                                    onJoystickMove = vm::updateJoystick,
                                    onSprintChanged = vm::setSprint,
                                    onPressChanged = vm::setPress,
                                    onDoublePressChanged = vm::setDoublePress,
                                    onShootPressed = vm::onShootPressed,
                                    onShootReleased = vm::onShootReleased,
                                    onSlideTackle = vm::onSlideTackle,
                                    onShortPassOrSwitch = vm::onShortPassOrSwitch,
                                    onThroughPassOrSlide = vm::onThroughPassOrSlide,
                                    onSwitchPlayer = vm::onSwitchPlayer,
                                    onStartSecondHalf = vm::startSecondHalf,
                                    onPause = vm::pauseMatch,
                                    onResume = vm::resumeMatch,
                                    onRestart = vm::restartMatch,
                                    onExitToLobby = vm::exitToLobby,
                                    onVolumeChange = prefsVm::setVolume,
                                    onVolumeUp = prefsVm::increaseVolume,
                                    onVolumeDown = prefsVm::decreaseVolume,
                                    onToggleMute = prefsVm::toggleMute,
                                    onCommentaryEnabledChange = prefsVm::setCommentaryEnabled,
                                    onGraphicsSelected = prefsVm::setGraphicsQuality,
                                    onDifficultySelected = prefsVm::setDifficulty,
                                    onCameraDistanceSelected = prefsVm::setCameraDistance,
                                    onShowPassIndicatorChange = prefsVm::setShowPassIndicator,
                                    onSelectFormation = vm::setMatchFormation,
                                    onSwapStarters = vm::swapMatchStarters,
                                    onSubstitutePlayer = vm::substituteMatchPlayer,
                                    onResetPreferences = prefsVm::resetPreferences,
                                    aiCoachState = aiCoachState,
                                    onAnalyzeMatchWithAi = vm::analyzeMatchWithAi,
                                    onAskAiCoach = vm::askAiCoach,
                                    onRetryAiCoach = vm::retryAiCoach,
                                    onResetAiCoach = vm::resetAiCoach
                                )
                            }

                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    preferencesViewModel = prefsVm,
                                    onTestCommentary = vm::testCommentary,
                                    onBack = vm::closeSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
