package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.SettingsRepository
import com.example.engine.EngineEvent
import com.example.engine.FootballEngine
import com.example.engine.InputSnapshot
import com.example.model.CareerStats
import com.example.model.Difficulty
import com.example.model.FormationType
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.model.MatchPhase
import com.example.model.MatchState
import com.example.model.SquadPlayer
import com.example.model.Team
import com.example.model.TeamCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    MATCH
}

class FootballViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application.applicationContext)
    private val soundEngine = SoundEngine()

    val settings: StateFlow<GameSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    val careerStats: StateFlow<CareerStats> = repository.careerStatsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, CareerStats())

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    private val _lobbyFormation = MutableStateFlow(FormationType.F_4_3_3)
    val lobbyFormation: StateFlow<FormationType> = _lobbyFormation.asStateFlow()

    private val _lobbyLineup = MutableStateFlow(TeamCatalog.teams.first().squad)
    val lobbyLineup: StateFlow<List<SquadPlayer>> = _lobbyLineup.asStateFlow()

    private var lastSyncedHomeTeamId: String = ""

    @Volatile
    private var inputSnapshot = InputSnapshot()

    private var gameLoopJob: Job? = null

    init {
        viewModelScope.launch {
            settings.collect { s ->
                soundEngine.updateVolume(s.effectiveVolume)
                if (s.homeTeamId != lastSyncedHomeTeamId) {
                    lastSyncedHomeTeamId = s.homeTeamId
                    _lobbyLineup.value = TeamCatalog.findById(s.homeTeamId).squad
                }
            }
        }
    }

    // Audio Controls
    fun setVolume(volume: Float) {
        viewModelScope.launch { repository.setVolume(volume) }
    }

    fun increaseVolume() {
        val next = (settings.value.volume + 0.1f).coerceAtMost(1f)
        soundEngine.updateVolume(next)
        soundEngine.playUiTap()
        setVolume(next)
    }

    fun decreaseVolume() {
        val next = (settings.value.volume - 0.1f).coerceAtLeast(0f)
        soundEngine.updateVolume(next)
        soundEngine.playUiTap()
        setVolume(next)
    }

    fun toggleMute() {
        val nextMuted = !settings.value.isMuted
        viewModelScope.launch { repository.setMuted(nextMuted) }
        if (!nextMuted) soundEngine.playUiTap()
    }

    // Graphics & Difficulty
    fun setGraphicsQuality(quality: GraphicsQuality) {
        soundEngine.playUiTap()
        viewModelScope.launch { repository.setGraphicsQuality(quality) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        soundEngine.playUiTap()
        viewModelScope.launch { repository.setDifficulty(difficulty) }
    }

    fun setMatchDuration(seconds: Int) {
        soundEngine.playUiTap()
        viewModelScope.launch { repository.setMatchDuration(seconds) }
    }

    fun setShowPassIndicator(show: Boolean) {
        soundEngine.playUiTap()
        viewModelScope.launch { repository.setShowPassIndicator(show) }
    }

    fun selectHomeTeam(team: Team) {
        soundEngine.playUiTap()
        val currentAway = settings.value.awayTeamId
        val nextAway = if (team.id == currentAway) TeamCatalog.teams.first { it.id != team.id }.id else currentAway
        lastSyncedHomeTeamId = team.id
        _lobbyLineup.value = team.squad
        viewModelScope.launch { repository.setTeams(team.id, nextAway) }
    }

    fun selectAwayTeam(team: Team) {
        soundEngine.playUiTap()
        val currentHome = settings.value.homeTeamId
        val nextHome = if (team.id == currentHome) TeamCatalog.teams.first { it.id != team.id }.id else currentHome
        if (nextHome != lastSyncedHomeTeamId) {
            lastSyncedHomeTeamId = nextHome
            _lobbyLineup.value = TeamCatalog.findById(nextHome).squad
        }
        viewModelScope.launch { repository.setTeams(nextHome, team.id) }
    }

    fun randomizeTeams() {
        soundEngine.playUiTap()
        val shuffled = TeamCatalog.teams.shuffled()
        lastSyncedHomeTeamId = shuffled[0].id
        _lobbyLineup.value = shuffled[0].squad
        viewModelScope.launch { repository.setTeams(shuffled[0].id, shuffled[1].id) }
    }

    // Lobby Tactics & Squad Rearrangement
    fun setLobbyFormation(formation: FormationType) {
        soundEngine.playUiTap()
        _lobbyFormation.value = formation
    }

    fun swapLobbyStarters(indexA: Int, indexB: Int) {
        soundEngine.playUiTap()
        _lobbyLineup.update { cur ->
            if (indexA !in 0..10 || indexB !in 0..10 || indexA == indexB || cur.size < 11) return@update cur
            cur.toMutableList().apply {
                val tmp = this[indexA]
                this[indexA] = this[indexB]
                this[indexB] = tmp
            }
        }
    }

    fun substituteLobbyPlayer(starterIndex: Int, benchIndex: Int) {
        soundEngine.playUiTap()
        val actualBenchIdx = 11 + benchIndex
        _lobbyLineup.update { cur ->
            if (starterIndex !in 0..10 || actualBenchIdx !in 11 until cur.size) return@update cur
            cur.toMutableList().apply {
                val tmp = this[starterIndex]
                this[starterIndex] = this[actualBenchIdx]
                this[actualBenchIdx] = tmp
            }
        }
    }

    // Live Match Tactics & Substitutions
    fun setMatchFormation(formation: FormationType) {
        soundEngine.playUiTap()
        _lobbyFormation.value = formation
        _matchState.update { state -> FootballEngine.applyFormationChange(state, formation) }
    }

    fun swapMatchStarters(indexA: Int, indexB: Int) {
        soundEngine.playUiTap()
        _matchState.update { state ->
            FootballEngine.swapHomeStarters(state, "home_$indexA", "home_$indexB")
        }
    }

    fun substituteMatchPlayer(starterIndex: Int, benchIndex: Int) {
        soundEngine.playUiTap()
        _matchState.update { state ->
            val benchId = state.homeBench.getOrNull(benchIndex)?.id ?: return@update state
            FootballEngine.substituteHomePlayer(state, "home_$starterIndex", benchId)
        }
    }

    // Match Lifecycle
    fun startMatch() {
        val s = settings.value
        val home = TeamCatalog.findById(s.homeTeamId)
        val away = TeamCatalog.findById(s.awayTeamId)
        inputSnapshot = InputSnapshot()
        _matchState.value = FootballEngine.createNewMatch(
            homeTeam = home,
            awayTeam = away,
            durationSeconds = s.matchDurationSeconds,
            homeFormation = _lobbyFormation.value,
            homeLineup = _lobbyLineup.value
        )
        _currentScreen.value = AppScreen.MATCH
        soundEngine.playWhistle()
        startGameLoop()
    }

    fun startSecondHalf() {
        soundEngine.playWhistle()
        _matchState.update { cur ->
            if (cur.phase == MatchPhase.HALF_TIME) FootballEngine.startSecondHalf(cur) else cur
        }
    }

    fun restartMatch() {
        val s = settings.value
        val home = TeamCatalog.findById(s.homeTeamId)
        val away = TeamCatalog.findById(s.awayTeamId)
        inputSnapshot = InputSnapshot()
        _matchState.value = FootballEngine.createNewMatch(
            homeTeam = home,
            awayTeam = away,
            durationSeconds = s.matchDurationSeconds,
            homeFormation = _lobbyFormation.value,
            homeLineup = _lobbyLineup.value
        )
        soundEngine.playWhistle()
        startGameLoop()
    }

    fun pauseMatch() {
        _matchState.update {
            if (it.phase == MatchPhase.PLAYING || it.phase == MatchPhase.PRE_KICKOFF) {
                it.copy(phase = MatchPhase.PAUSED, isChargingShot = false, shotCharge = 0f)
            } else it
        }
    }

    fun resumeMatch() {
        _matchState.update {
            if (it.phase == MatchPhase.PAUSED) it.copy(phase = MatchPhase.PLAYING) else it
        }
    }

    fun exitToLobby() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        inputSnapshot = InputSnapshot()
        _currentScreen.value = AppScreen.HOME
    }

    // PES / FIFA Controls
    fun updateJoystick(moveX: Float, moveY: Float) {
        inputSnapshot = inputSnapshot.copy(moveX = moveX, moveY = moveY)
    }

    fun setSprint(sprinting: Boolean) {
        inputSnapshot = inputSnapshot.copy(isSprinting = sprinting)
    }

    fun setPress(pressing: Boolean) {
        inputSnapshot = inputSnapshot.copy(isPressing = pressing)
    }

    fun onShootPressed() {
        _matchState.update { state ->
            if (state.phase != MatchPhase.PLAYING) return@update state
            if (state.homeHasBall) {
                state.copy(isChargingShot = true, shotCharge = 0.28f)
            } else {
                val events = mutableListOf<EngineEvent>()
                val next = FootballEngine.performSlideTackle(state, inputSnapshot, settings.value.graphicsQuality, events)
                dispatchEvents(events)
                next
            }
        }
    }

    fun onShootReleased() {
        _matchState.update { state ->
            if (state.phase != MatchPhase.PLAYING) return@update state.copy(isChargingShot = false, shotCharge = 0f)
            if (state.isChargingShot && state.homeHasBall) {
                val events = mutableListOf<EngineEvent>()
                val next = FootballEngine.handleShootRelease(state, inputSnapshot, settings.value.graphicsQuality, events)
                dispatchEvents(events)
                next
            } else {
                state.copy(isChargingShot = false, shotCharge = 0f)
            }
        }
    }

    fun onShortPassOrSwitch() {
        _matchState.update { state ->
            val events = mutableListOf<EngineEvent>()
            val next = FootballEngine.handleShortPass(state, inputSnapshot, events)
            dispatchEvents(events)
            next
        }
    }

    fun onThroughPassOrSlide() {
        _matchState.update { state ->
            if (state.phase != MatchPhase.PLAYING) return@update state
            val events = mutableListOf<EngineEvent>()
            val next = if (state.homeHasBall) {
                FootballEngine.handleThroughPass(state, inputSnapshot, events)
            } else {
                FootballEngine.performSlideTackle(state, inputSnapshot, settings.value.graphicsQuality, events)
            }
            dispatchEvents(events)
            next
        }
    }

    fun onSwitchPlayer() {
        _matchState.update { state -> FootballEngine.switchHomePlayer(state) }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastNanos = System.nanoTime()
            while (isActive && _currentScreen.value == AppScreen.MATCH) {
                val now = System.nanoTime()
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                lastNanos = now

                val s = settings.value
                val events = mutableListOf<EngineEvent>()
                _matchState.update { cur ->
                    FootballEngine.step(
                        state = cur,
                        dtRaw = dt,
                        input = inputSnapshot,
                        difficulty = s.difficulty,
                        graphicsQuality = s.graphicsQuality,
                        eventsOut = events
                    )
                }
                dispatchEvents(events)
                delay(16L)
            }
        }
    }

    private fun dispatchEvents(events: List<EngineEvent>) {
        for (event in events) {
            when (event) {
                is EngineEvent.Whistle -> soundEngine.playWhistle()
                is EngineEvent.Kick -> soundEngine.playKick(event.power)
                is EngineEvent.Pass -> soundEngine.playPass()
                is EngineEvent.Tackle -> soundEngine.playTackle()
                is EngineEvent.PostClang -> soundEngine.playPostClang()
                is EngineEvent.Goal -> soundEngine.playGoalHorn()
                is EngineEvent.HalfTime -> Unit
                is EngineEvent.MatchEnded -> {
                    viewModelScope.launch {
                        repository.recordMatchResult(event.homeScore, event.awayScore)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        soundEngine.release()
    }
}
