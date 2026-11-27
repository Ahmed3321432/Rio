package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiCoachUiState
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
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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

    private val geminiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text("أنت مدرب كرة قدم محترف ومحلل تكتيكي للعبة ركلة الأبطال. أجب باللغة العربية بأسلوب عملي ومختصر وواضح.")
            }
        )
    }

    private val _aiCoachState = MutableStateFlow<AiCoachUiState>(AiCoachUiState.Idle)
    val aiCoachState: StateFlow<AiCoachUiState> = _aiCoachState.asStateFlow()

    private var aiJob: Job? = null
    private var lastAiPrompt: String? = null

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

    // Gemini AI Tactical Coach
    fun askAiCoach(userPrompt: String) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty()) return

        val s = settings.value
        val home = TeamCatalog.findById(s.homeTeamId)
        val away = TeamCatalog.findById(s.awayTeamId)
        val lineup = _lobbyLineup.value
        val roleLabel: (com.example.model.PlayerRole) -> String = { role ->
            when (role) {
                com.example.model.PlayerRole.GK -> "حارس"
                com.example.model.PlayerRole.DEF -> "مدافع"
                com.example.model.PlayerRole.MID -> "وسط"
                com.example.model.PlayerRole.FWD -> "مهاجم"
            }
        }
        val startersSummary = lineup.take(11).joinToString("، ") { "${it.name} (${roleLabel(it.preferredRole)})" }
        val benchSummary = lineup.drop(11).joinToString("، ") { "${it.name} (${roleLabel(it.preferredRole)})" }

        val fullPrompt = buildString {
            appendLine("بيانات المباراة الحالية:")
            appendLine("- فريقي: ${home.nameAr} (هجوم ${home.attack}، دفاع ${home.defense}، سرعة ${home.speed})")
            appendLine("- الخصم: ${away.nameAr} (هجوم ${away.attack}، دفاع ${away.defense}، سرعة ${away.speed})")
            appendLine("- التشكيلة المختارة: ${_lobbyFormation.value.displayName}")
            if (startersSummary.isNotBlank()) appendLine("- الأساسيون: $startersSummary")
            if (benchSummary.isNotBlank()) appendLine("- الاحتياط: $benchSummary")
            appendLine()
            appendLine("سؤال اللاعب: $trimmed")
        }

        launchAiPrompt(fullPrompt)
    }

    fun analyzeMatchWithAi() {
        val ms = _matchState.value
        val fullPrompt = buildString {
            appendLine("قم بتحليل نتيجة وإحصائيات هذه المباراة وقدّم ٣ نصائح عملية للمباراة القادمة:")
            appendLine("- النتيجة النهائية: ${ms.homeTeam.nameAr} ${ms.homeScore} - ${ms.awayScore} ${ms.awayTeam.nameAr}")
            appendLine("- التشكيلة المستخدمة: ${ms.homeFormation.displayName}")
            appendLine("- الاستحواذ: ${ms.stats.homePossessionPercent}% مقابل ${ms.stats.awayPossessionPercent}%")
            appendLine("- التسديدات: ${ms.stats.homeShots} مقابل ${ms.stats.awayShots}")
            appendLine("- الركنيات: ${ms.stats.homeCorners} مقابل ${ms.stats.awayCorners}")
            appendLine("- الافتكاك: ${ms.stats.homeTackles} مقابل ${ms.stats.awayTackles}")
        }

        launchAiPrompt(fullPrompt)
    }

    fun retryAiCoach() {
        val prompt = lastAiPrompt ?: return
        launchAiPrompt(prompt)
    }

    fun resetAiCoach() {
        aiJob?.cancel()
        aiJob = null
        _aiCoachState.value = AiCoachUiState.Idle
    }

    private fun launchAiPrompt(fullPrompt: String) {
        soundEngine.playUiTap()
        lastAiPrompt = fullPrompt
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _aiCoachState.value = AiCoachUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    geminiModel.generateContentStream(fullPrompt).collect { chunk ->
                        val textChunk = chunk.text.orEmpty()
                        if (textChunk.isNotEmpty()) {
                            accumulated.append(textChunk)
                            _aiCoachState.value = AiCoachUiState.Streaming(accumulated.toString())
                        }
                    }
                }
                if (accumulated.isNotBlank()) {
                    _aiCoachState.value = AiCoachUiState.Success(accumulated.toString())
                } else {
                    _aiCoachState.value = AiCoachUiState.Error("لم يصل رد من المدرب الذكي، حاول مرة أخرى.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("FootballViewModel", "Gemini AI request timed out", e)
                _aiCoachState.value = AiCoachUiState.Error("انتهت مهلة الاتصال بالمدرب الذكي بعد 60 ثانية. تأكد من الإنترنت وحاول مجدداً.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FootballViewModel", "Gemini AI request failed", e)
                _aiCoachState.value = AiCoachUiState.Error(
                    e.localizedMessage ?: "تعذر الاتصال بالمدرب الذكي حالياً."
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        aiJob?.cancel()
        soundEngine.release()
    }
}
