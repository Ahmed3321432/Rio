package com.example

import com.example.engine.EngineEvent
import com.example.engine.FootballEngine
import com.example.engine.InputSnapshot
import com.example.model.Difficulty
import com.example.model.FormationType
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.model.MatchPhase
import com.example.model.PlayerRole
import com.example.model.SetPieceType
import com.example.model.TeamCatalog
import com.example.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FootballEngineTest {

    @Test
    fun createMatch_initializesTwentyTwoPlayersAndBallAtCenter() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        val state = FootballEngine.createNewMatch(home, away, 90)

        assertEquals(22, state.players.size)
        assertEquals(11, state.players.count { it.teamSide == TeamSide.HOME })
        assertEquals(11, state.players.count { it.teamSide == TeamSide.AWAY })
        assertEquals(80f, state.ball.x, 0.01f)
        assertEquals(50f, state.ball.y, 0.01f)
        assertEquals(MatchPhase.PRE_KICKOFF, state.phase)
    }

    @Test
    fun preKickoffTransitionsToPlayingAndBallScoresGoalInRightNet() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90)
        val events = mutableListOf<EngineEvent>()

        repeat(40) {
            state = FootballEngine.step(
                state = state,
                dtRaw = 0.033f,
                input = InputSnapshot(),
                difficulty = Difficulty.MEDIUM,
                graphicsQuality = GraphicsQuality.HIGH,
                eventsOut = events
            )
        }
        assertEquals(MatchPhase.PLAYING, state.phase)
        assertTrue(events.any { it is EngineEvent.Whistle })

        events.clear()
        state = state.copy(
            ball = state.ball.copy(
                x = FootballEngine.FIELD_RIGHT - 0.5f,
                y = 50f,
                vx = 60f,
                vy = 0f,
                ownerId = null,
                lastTouchTeam = TeamSide.HOME,
                lastTouchPlayerName = "صلاح"
            )
        )
        state = FootballEngine.step(
            state = state,
            dtRaw = 0.03f,
            input = InputSnapshot(),
            difficulty = Difficulty.MEDIUM,
            graphicsQuality = GraphicsQuality.HIGH,
            eventsOut = events
        )

        assertEquals(1, state.homeScore)
        assertEquals(MatchPhase.GOAL_CELEBRATION, state.phase)
        assertTrue(events.any { it is EngineEvent.Goal && it.scoringSide == TeamSide.HOME })
    }

    @Test
    fun ballScoringInLeftNetIncrementsAwayScoreAndRecordsScorer() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        state = state.copy(
            ball = state.ball.copy(
                x = FootballEngine.FIELD_LEFT + 0.5f,
                y = 50f,
                vx = -60f,
                vy = 0f,
                ownerId = null,
                lastTouchTeam = TeamSide.AWAY,
                lastTouchPlayerName = "مبابي"
            )
        )
        state = FootballEngine.step(
            state = state,
            dtRaw = 0.03f,
            input = InputSnapshot(),
            difficulty = Difficulty.MEDIUM,
            graphicsQuality = GraphicsQuality.HIGH,
            eventsOut = events
        )

        assertEquals(0, state.homeScore)
        assertEquals(1, state.awayScore)
        assertEquals(TeamSide.AWAY, state.lastScorerTeam)
        assertEquals("مبابي", state.lastScorerName)
        assertEquals(MatchPhase.GOAL_CELEBRATION, state.phase)
    }

    @Test
    fun ballCrossingTouchlineTriggersThrowInSetPiece() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        state = state.copy(
            ball = state.ball.copy(
                x = 80f,
                y = FootballEngine.FIELD_TOP - 0.5f,
                vx = 0f,
                vy = -40f,
                ownerId = null,
                lastTouchTeam = TeamSide.HOME
            )
        )
        state = FootballEngine.step(
            state = state,
            dtRaw = 0.03f,
            input = InputSnapshot(),
            difficulty = Difficulty.MEDIUM,
            graphicsQuality = GraphicsQuality.MEDIUM,
            eventsOut = events
        )

        assertEquals(MatchPhase.SET_PIECE, state.phase)
        assertEquals(SetPieceType.THROW_IN, state.setPieceType)
        assertEquals(TeamSide.AWAY, state.setPieceTeam)
    }

    @Test
    fun humanDefenderStealsBallWhenPressingNearOpponentCarrier() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        // Give away_9 (ST) the ball right next to home_9 (ST)
        val updatedPlayers = state.players.map { p ->
            when (p.id) {
                "away_9" -> p.copy(x = 80f, y = 50f)
                "home_9" -> p.copy(x = 77.6f, y = 50f)
                else -> p
            }
        }
        state = state.copy(
            players = updatedPlayers,
            activeHomePlayerId = "home_9",
            ball = state.ball.copy(x = 79f, y = 50f, ownerId = "away_9", lastTouchTeam = TeamSide.AWAY)
        )

        state = FootballEngine.step(
            state = state,
            dtRaw = 0.03f,
            input = InputSnapshot(isPressing = true),
            difficulty = Difficulty.MEDIUM,
            graphicsQuality = GraphicsQuality.HIGH,
            eventsOut = events
        )

        assertEquals("home_9", state.ball.ownerId)
        assertEquals(1, state.stats.homeTackles)
        assertTrue(events.any { it is EngineEvent.Tackle })
    }

    @Test
    fun aimedShortPassSelectsAndDeliversBallToAimedTeammate() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        // Place active passer (home_6 CDM) at center, with home_8 (LW) directly UP and home_10 (RW) directly DOWN
        val placed = state.players.map { p ->
            when (p.id) {
                "home_6" -> p.copy(x = 65f, y = 50f, vx = 0f, vy = 0f)
                "home_8" -> p.copy(x = 65f, y = 24f, vx = 0f, vy = 0f)
                "home_10" -> p.copy(x = 65f, y = 76f, vx = 0f, vy = 0f)
                else -> p.copy(x = 20f, y = 50f) // keep others out of the way
            }
        }
        state = state.copy(
            players = placed,
            activeHomePlayerId = "home_6",
            ball = state.ball.copy(x = 66f, y = 50f, ownerId = "home_6", lastTouchTeam = TeamSide.HOME)
        )

        // Aim UP (moveY = -1f) and pass -> must select home_8 and deliver ball to home_8
        state = FootballEngine.handleShortPass(state, InputSnapshot(moveX = 0f, moveY = -1f), events)
        assertEquals("home_8", state.activeHomePlayerId)
        assertEquals("home_8", state.passInFlightTargetId)

        // Step simulation until pass arrives cleanly at home_8
        repeat(30) {
            state = FootballEngine.step(
                state = state,
                dtRaw = 0.03f,
                input = InputSnapshot(moveX = 0f, moveY = -1f),
                difficulty = Difficulty.MEDIUM,
                graphicsQuality = GraphicsQuality.MEDIUM,
                eventsOut = events
            )
        }
        assertEquals("home_8", state.ball.ownerId)
    }

    @Test
    fun applyFormationChangeUpdatesRolesAndAnchorsForAllElevenHomePlayers() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        val initial = FootballEngine.createNewMatch(home, away, 90, homeFormation = FormationType.F_4_3_3)

        val updated = FootballEngine.applyFormationChange(initial, FormationType.F_3_5_2)
        assertEquals(FormationType.F_3_5_2, updated.homeFormation)

        val homePlayers = updated.players.filter { it.teamSide == TeamSide.HOME }
        assertEquals(11, homePlayers.size)
        assertEquals(1, homePlayers.count { it.role == PlayerRole.GK })
        assertEquals(3, homePlayers.count { it.role == PlayerRole.DEF })
        assertEquals(5, homePlayers.count { it.role == PlayerRole.MID })
        assertEquals(2, homePlayers.count { it.role == PlayerRole.FWD })
    }

    @Test
    fun substituteHomePlayerSwapsBenchAndStarterAndResetsStamina() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        val tiredPlayers = FootballEngine.createNewMatch(home, away, 90).let { base ->
            base.copy(
                players = base.players.map { if (it.id == "home_9") it.copy(stamina = 0.2f) else it }
            )
        }
        val outgoingName = tiredPlayers.players.first { it.id == "home_9" }.name
        val incomingBench = tiredPlayers.homeBench.last()

        val afterSub = FootballEngine.substituteHomePlayer(tiredPlayers, "home_9", incomingBench.id)
        val newStarter = afterSub.players.first { it.id == "home_9" }

        assertEquals(incomingBench.name, newStarter.name)
        assertEquals(1f, newStarter.stamina, 0.001f)
        assertEquals(1, afterSub.subsUsed)
        assertTrue(afterSub.homeBench.any { it.name == outgoingName })
        assertNotEquals(outgoingName, newStarter.name)
    }

    @Test
    fun swapHomeStartersExchangesIdentitiesBetweenSlots() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        val initial = FootballEngine.createNewMatch(home, away, 90)
        val name8 = initial.players.first { it.id == "home_8" }.name
        val name10 = initial.players.first { it.id == "home_10" }.name

        val swapped = FootballEngine.swapHomeStarters(initial, "home_8", "home_10")
        assertEquals(name10, swapped.players.first { it.id == "home_8" }.name)
        assertEquals(name8, swapped.players.first { it.id == "home_10" }.name)
    }

    @Test
    fun gameSettings_effectiveVolumeRespectsMuteAndClamping() {
        val loud = GameSettings(volume = 1.4f, isMuted = false)
        assertEquals(1f, loud.effectiveVolume, 0.001f)

        val muted = GameSettings(volume = 0.8f, isMuted = true)
        assertEquals(0f, muted.effectiveVolume, 0.001f)
    }
}
