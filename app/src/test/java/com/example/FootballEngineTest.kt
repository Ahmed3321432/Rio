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
import kotlin.math.hypot

class FootballEngineTest {

    @Test
    fun createMatch_initializesTwentyTwoPlayersAndBallAtCenter() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        val state = FootballEngine.createNewMatch(home, away, 90)

        assertEquals(22, state.players.size)
        assertEquals(11, state.players.count { it.teamSide == TeamSide.HOME })
        assertEquals(11, state.players.count { it.teamSide == TeamSide.AWAY })
        assertEquals(FootballEngine.CENTER_X, state.ball.x, 0.01f)
        assertEquals(FootballEngine.CENTER_Y, state.ball.y, 0.01f)
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
                y = FootballEngine.CENTER_Y,
                vx = 70f,
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
                y = FootballEngine.CENTER_Y,
                vx = -70f,
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
                x = FootballEngine.CENTER_X,
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

        val updatedPlayers = state.players.map { p ->
            when (p.id) {
                "away_9" -> p.copy(x = FootballEngine.CENTER_X, y = FootballEngine.CENTER_Y)
                "home_9" -> p.copy(x = FootballEngine.CENTER_X - 2.2f, y = FootballEngine.CENTER_Y)
                else -> p
            }
        }
        state = state.copy(
            players = updatedPlayers,
            activeHomePlayerId = "home_9",
            ball = state.ball.copy(
                x = FootballEngine.CENTER_X - 0.8f,
                y = FootballEngine.CENTER_Y,
                ownerId = "away_9",
                lastTouchTeam = TeamSide.AWAY
            )
        )

        state = FootballEngine.step(
            state = state,
            dtRaw = 0.03f,
            input = InputSnapshot(isSprinting = true, isPressing = true),
            difficulty = Difficulty.MEDIUM,
            graphicsQuality = GraphicsQuality.HIGH,
            eventsOut = events
        )

        assertEquals("home_9", state.ball.ownerId)
        assertEquals(1, state.stats.homeTackles)
        assertTrue(events.any { it is EngineEvent.Tackle })
    }

    @Test
    fun opponentStealsBallWithinTwoSecondsIfPlayerStopsMoving() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        val placed = state.players.map { p ->
            when (p.id) {
                "home_9" -> p.copy(x = 128f, y = 90f, vx = 0f, vy = 0f, facingAngle = 0f)
                "away_9" -> p.copy(x = 152f, y = 90f, vx = 0f, vy = 0f)
                else -> p
            }
        }
        state = state.copy(
            players = placed,
            activeHomePlayerId = "home_9",
            ball = state.ball.copy(x = 130f, y = 90f, ownerId = "home_9", lastTouchTeam = TeamSide.HOME),
            lastBallOwnerId = "home_9",
            carrierIdleSeconds = 0f
        )

        // Simulate ~2.2 seconds standing completely still (zero joystick input)
        var stoleByAway = false
        var tacklingEvents = 0
        repeat(75) {
            if (!stoleByAway) {
                state = FootballEngine.step(
                    state = state,
                    dtRaw = 0.03f,
                    input = InputSnapshot(moveX = 0f, moveY = 0f),
                    difficulty = Difficulty.MEDIUM,
                    graphicsQuality = GraphicsQuality.MEDIUM,
                    eventsOut = events
                )
                if (state.ball.ownerId?.startsWith("away_") == true || state.stats.awayTackles >= 1) {
                    stoleByAway = true
                    tacklingEvents = state.stats.awayTackles
                }
            }
        }

        assertTrue(stoleByAway)
        assertTrue(tacklingEvents >= 1)
    }

    @Test
    fun doublePressBringsTwoHomeDefendersCloseAroundOpponentCarrier() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        val placed = state.players.map { p ->
            when (p.id) {
                "away_9" -> p.copy(x = 140f, y = 90f, vx = 0f, vy = 0f)
                "home_6" -> p.copy(x = 124f, y = 84f, vx = 0f, vy = 0f)
                "home_5" -> p.copy(x = 124f, y = 96f, vx = 0f, vy = 0f)
                else -> if (p.teamSide == TeamSide.HOME) p.copy(x = 25f, y = 90f, vx = 0f, vy = 0f) else p.copy(x = 230f, y = 90f, vx = 0f, vy = 0f)
            }
        }
        state = state.copy(
            players = placed,
            activeHomePlayerId = "home_6",
            ball = state.ball.copy(x = 138f, y = 90f, ownerId = "away_9", lastTouchTeam = TeamSide.AWAY)
        )

        repeat(15) {
            state = FootballEngine.step(
                state = state,
                dtRaw = 0.03f,
                input = InputSnapshot(isDoublePressing = true),
                difficulty = Difficulty.MEDIUM,
                graphicsQuality = GraphicsQuality.MEDIUM,
                eventsOut = events
            )
        }

        val defA = state.players.first { it.id == "home_6" }
        val defB = state.players.first { it.id == "home_5" }
        val distBetweenDefenders = hypot(defA.x - defB.x, defA.y - defB.y)
        assertTrue(distBetweenDefenders < 12f)
    }

    @Test
    fun aimedShortPassSelectsAndDeliversBallToAimedTeammate() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        val placed = state.players.map { p ->
            when (p.id) {
                "home_6" -> p.copy(x = 110f, y = 90f, vx = 0f, vy = 0f)
                "home_8" -> p.copy(x = 110f, y = 45f, vx = 0f, vy = 0f)
                "home_10" -> p.copy(x = 110f, y = 135f, vx = 0f, vy = 0f)
                else -> p.copy(x = 25f, y = 90f)
            }
        }
        state = state.copy(
            players = placed,
            activeHomePlayerId = "home_6",
            ball = state.ball.copy(x = 111f, y = 90f, ownerId = "home_6", lastTouchTeam = TeamSide.HOME)
        )

        state = FootballEngine.handleShortPass(state, InputSnapshot(moveX = 0f, moveY = -1f), events)
        assertEquals("home_8", state.activeHomePlayerId)
        assertEquals("home_8", state.passInFlightTargetId)

        repeat(45) {
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
    fun wingThroughActionLaunchesLoftedFastCrossIntoBox() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        val placed = state.players.map { p ->
            when (p.id) {
                "home_10" -> p.copy(x = 215f, y = 152f, facingAngle = 0f)
                "home_9" -> p.copy(x = 228f, y = 92f)
                else -> p
            }
        }
        state = state.copy(
            players = placed,
            activeHomePlayerId = "home_10",
            ball = state.ball.copy(x = 217f, y = 152f, ownerId = "home_10", lastTouchTeam = TeamSide.HOME)
        )

        val crossed = FootballEngine.handleThroughPass(state, InputSnapshot(), events)
        assertTrue(crossed.ball.vz >= 7.5f)
        assertTrue(crossed.ball.ownerId == null)
        assertEquals("home_9", crossed.activeHomePlayerId)
        assertTrue(events.any { it is EngineEvent.Pass })
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

    @Test
    fun staminaDepletesWhenSprintingAndRegeneratesWhenWalkingOrStandingStill() {
        val home = TeamCatalog.teams[0]
        val away = TeamCatalog.teams[1]
        var state = FootballEngine.createNewMatch(home, away, 90).copy(phase = MatchPhase.PLAYING)
        val events = mutableListOf<EngineEvent>()

        // Place active player in open space with the ball and full stamina so no tackle interrupts sprinting
        val activeId = state.activeHomePlayerId
        state = state.copy(
            ball = state.ball.copy(x = 80f, y = 90f, ownerId = activeId),
            players = state.players.map {
                if (it.id == activeId) it.copy(x = 80f, y = 90f, vx = 0f, vy = 0f, stamina = 1f)
                else it.copy(x = 240f, y = if (it.teamSide == TeamSide.AWAY) 150f else 30f, vx = 0f, vy = 0f)
            }
        )

        val sprintInput = InputSnapshot(moveX = 1f, moveY = 0f, isSprinting = true)
        repeat(20) {
            state = FootballEngine.step(
                state = state,
                dtRaw = 0.05f,
                input = sprintInput,
                difficulty = Difficulty.MEDIUM,
                graphicsQuality = GraphicsQuality.HIGH,
                eventsOut = events
            )
        }
        val afterSprint = state.players.first { it.id == activeId }.stamina
        assertTrue("Expected stamina to deplete while sprinting, got $afterSprint", afterSprint < 0.85f)

        // Reset stamina to 0.4f and test walking regeneration
        val walkStartState = state.copy(
            ball = state.ball.copy(x = 80f, y = 90f, ownerId = activeId),
            players = state.players.map {
                if (it.id == activeId) it.copy(x = 80f, y = 90f, vx = 0f, vy = 0f, stamina = 0.4f, stunTimer = 0f)
                else it.copy(x = 240f, y = if (it.teamSide == TeamSide.AWAY) 150f else 30f, vx = 0f, vy = 0f)
            }
        )
        var walkState = walkStartState
        val walkInput = InputSnapshot(moveX = 1f, moveY = 0f, isSprinting = false)
        repeat(20) {
            walkState = FootballEngine.step(
                state = walkState,
                dtRaw = 0.05f,
                input = walkInput,
                difficulty = Difficulty.MEDIUM,
                graphicsQuality = GraphicsQuality.HIGH,
                eventsOut = events
            )
        }
        val afterWalk = walkState.players.first { it.id == activeId }.stamina
        assertTrue("Expected stamina to regenerate while walking, got $afterWalk", afterWalk > 0.45f)

        // Compare with standing still from 0.4f (regenerates faster than walking)
        var idleState = walkStartState
        val idleInput = InputSnapshot(moveX = 0f, moveY = 0f, isSprinting = false)
        repeat(20) {
            idleState = FootballEngine.step(
                state = idleState,
                dtRaw = 0.05f,
                input = idleInput,
                difficulty = Difficulty.MEDIUM,
                graphicsQuality = GraphicsQuality.HIGH,
                eventsOut = events
            )
        }
        val afterIdle = idleState.players.first { it.id == activeId }.stamina
        assertTrue("Expected stamina to regenerate faster while standing still ($afterIdle) than walking ($afterWalk)", afterIdle > afterWalk)
    }
}
