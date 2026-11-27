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
import com.example.ui.AppScreen
import com.example.ui.FootballViewModel
import com.example.ui.screens.HomeLobbyScreen
import com.example.ui.screens.MatchScreen
import com.example.ui.theme.KickChampionsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KickChampionsTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val vm: FootballViewModel = viewModel()
                    val screen by vm.currentScreen.collectAsStateWithLifecycle()
                    val settings by vm.settings.collectAsStateWithLifecycle()
                    val careerStats by vm.careerStats.collectAsStateWithLifecycle()
                    val matchState by vm.matchState.collectAsStateWithLifecycle()
                    val lobbyFormation by vm.lobbyFormation.collectAsStateWithLifecycle()
                    val lobbyLineup by vm.lobbyLineup.collectAsStateWithLifecycle()

                    Crossfade(targetState = screen, label = "screen_transition") { current ->
                        when (current) {
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
                                    onVolumeChange = vm::setVolume,
                                    onVolumeUp = vm::increaseVolume,
                                    onVolumeDown = vm::decreaseVolume,
                                    onToggleMute = vm::toggleMute,
                                    onGraphicsSelected = vm::setGraphicsQuality,
                                    onDifficultySelected = vm::setDifficulty,
                                    onShowPassIndicatorChange = vm::setShowPassIndicator,
                                    onMatchDurationSelected = vm::setMatchDuration,
                                    onStartMatch = vm::startMatch
                                )
                            }

                            AppScreen.MATCH -> {
                                MatchScreen(
                                    matchState = matchState,
                                    settings = settings,
                                    onJoystickMove = vm::updateJoystick,
                                    onSprintChanged = vm::setSprint,
                                    onPressChanged = vm::setPress,
                                    onShootPressed = vm::onShootPressed,
                                    onShootReleased = vm::onShootReleased,
                                    onShortPassOrSwitch = vm::onShortPassOrSwitch,
                                    onThroughPassOrSlide = vm::onThroughPassOrSlide,
                                    onSwitchPlayer = vm::onSwitchPlayer,
                                    onStartSecondHalf = vm::startSecondHalf,
                                    onPause = vm::pauseMatch,
                                    onResume = vm::resumeMatch,
                                    onRestart = vm::restartMatch,
                                    onExitToLobby = vm::exitToLobby,
                                    onVolumeChange = vm::setVolume,
                                    onVolumeUp = vm::increaseVolume,
                                    onVolumeDown = vm::decreaseVolume,
                                    onToggleMute = vm::toggleMute,
                                    onGraphicsSelected = vm::setGraphicsQuality,
                                    onDifficultySelected = vm::setDifficulty,
                                    onShowPassIndicatorChange = vm::setShowPassIndicator,
                                    onSelectFormation = vm::setMatchFormation,
                                    onSwapStarters = vm::swapMatchStarters,
                                    onSubstitutePlayer = vm::substituteMatchPlayer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
