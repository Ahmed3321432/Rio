package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.BallEntity
import com.example.model.Difficulty
import com.example.model.FormationType
import com.example.model.GraphicsQuality
import com.example.model.MatchPhase
import com.example.model.MatchState
import com.example.model.MatchStats
import com.example.model.PitchParticle
import com.example.model.PlayerEntity
import com.example.model.PlayerRole
import com.example.model.SetPieceType
import com.example.model.SquadPlayer
import com.example.model.Team
import com.example.model.TeamSide
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class InputSnapshot(
    val moveX: Float = 0f,
    val moveY: Float = 0f,
    val isSprinting: Boolean = false,
    val isPressing: Boolean = false,
    val isDoublePressing: Boolean = false
)

sealed interface EngineEvent {
    data object Whistle : EngineEvent
    data class Kick(val power: Float) : EngineEvent
    data object Pass : EngineEvent
    data object Tackle : EngineEvent
    data object PostClang : EngineEvent
    data class Goal(val scoringSide: TeamSide, val scorerName: String) : EngineEvent
    data object HalfTime : EngineEvent
    data class MatchEnded(val homeScore: Int, val awayScore: Int) : EngineEvent
}

object FootballEngine {
    const val PITCH_W = 270f
    const val PITCH_H = 180f
    const val CENTER_X = PITCH_W * 0.5f
    const val CENTER_Y = PITCH_H * 0.5f

    const val FIELD_LEFT = 10f
    const val FIELD_RIGHT = 260f
    const val FIELD_TOP = 10f
    const val FIELD_BOTTOM = 170f

    // Regulation FIFA goal width: 18u / 160u pitch width (~11.25%, matching 7.32m / 68m = 10.76%)
    // Drawn height in 3D is 6.0u -> exact 3.0 : 1 FIFA goal width-to-height ratio!
    const val GOAL_TOP = 81f
    const val GOAL_BOTTOM = 99f
    const val GOAL_NET_DEPTH = 6.2f

    const val PLAYER_RADIUS = 1.65f
    const val BALL_RADIUS = 0.85f
    const val POST_RADIUS = 0.72f

    private val rng = Random(System.currentTimeMillis())

    private fun slotToFieldX(rawSlotX: Float): Float =
        FIELD_LEFT + ((rawSlotX - 8f) / 144f) * (FIELD_RIGHT - FIELD_LEFT)

    private fun slotToFieldY(rawSlotY: Float): Float =
        FIELD_TOP + ((rawSlotY - 8f) / 84f) * (FIELD_BOTTOM - FIELD_TOP)

    fun createNewMatch(
        homeTeam: Team,
        awayTeam: Team,
        durationSeconds: Int,
        homeFormation: FormationType = FormationType.F_4_3_3,
        homeLineup: List<SquadPlayer> = homeTeam.squad
    ): MatchState {
        val effectiveHomeLineup = if (homeLineup.size >= 11) homeLineup else homeTeam.squad
        val players = createPlayersForMatch(
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeFormation = homeFormation,
            homeStarters = effectiveHomeLineup.take(11),
            awayStarters = awayTeam.squad.take(11),
            homeAttacksRight = true
        )
        return MatchState(
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeFormation = homeFormation,
            homeBench = effectiveHomeLineup.drop(11),
            subsUsed = 0,
            maxSubs = 5,
            phase = MatchPhase.PRE_KICKOFF,
            half = 1,
            remainingSeconds = durationSeconds.toFloat(),
            totalDurationSeconds = durationSeconds,
            homeScore = 0,
            awayScore = 0,
            players = players,
            ball = BallEntity(x = CENTER_X, y = CENTER_Y),
            activeHomePlayerId = defaultActiveHomeId(players),
            cameraX = CENTER_X,
            cameraY = CENTER_Y,
            carrierIdleSeconds = 0f,
            lastBallOwnerId = null,
            preKickoffTimer = 1.15f,
            bannerMessage = "بداية الشوط الأول!",
            bannerTimer = 1.3f,
            stats = MatchStats()
        )
    }

    fun startSecondHalf(state: MatchState): MatchState {
        val resetPlayers = resetPositionsForKickoff(
            players = state.players,
            homeFormation = state.homeFormation,
            awayFormation = FormationType.F_4_3_3,
            homeAttacksRight = false
        )
        return state.copy(
            phase = MatchPhase.PRE_KICKOFF,
            half = 2,
            players = resetPlayers,
            ball = BallEntity(x = CENTER_X, y = CENTER_Y),
            activeHomePlayerId = defaultActiveHomeId(resetPlayers),
            passTargetHomeId = null,
            shotCharge = 0f,
            isChargingShot = false,
            passCharge = 0f,
            isChargingPass = false,
            cameraX = CENTER_X,
            cameraY = CENTER_Y,
            carrierIdleSeconds = 0f,
            lastBallOwnerId = null,
            preKickoffTimer = 1.15f,
            bannerMessage = "بداية الشوط الثاني!",
            bannerTimer = 1.3f
        )
    }

    fun applyFormationChange(state: MatchState, newFormation: FormationType): MatchState {
        val slots = newFormation.slots
        val homeAttacksRight = state.homeAttacksRight
        val updatedPlayers = state.players.map { p ->
            if (p.teamSide != TeamSide.HOME) return@map p
            val idx = p.id.removePrefix("home_").toIntOrNull() ?: return@map p
            val slot = slots.getOrNull(idx) ?: return@map p
            val sx = slotToFieldX(slot.x)
            val sy = slotToFieldY(slot.y)
            val hx = if (homeAttacksRight) sx else (PITCH_W - sx)
            val hy = if (homeAttacksRight) sy else (PITCH_H - sy)
            val moveImmediately = state.phase == MatchPhase.PRE_KICKOFF
            p.copy(
                role = slot.role,
                slotCode = slot.code,
                homeX = hx,
                homeY = hy,
                x = if (moveImmediately) hx else p.x,
                y = if (moveImmediately) hy else p.y
            )
        }
        return state.copy(
            homeFormation = newFormation,
            players = updatedPlayers,
            bannerMessage = "الخطة: ${newFormation.displayName}",
            bannerTimer = 1.3f
        )
    }

    fun swapHomeStarters(state: MatchState, idA: String, idB: String): MatchState {
        if (idA == idB) return state
        val players = state.players.toMutableList()
        val idxA = players.indexOfFirst { it.id == idA && it.teamSide == TeamSide.HOME }
        val idxB = players.indexOfFirst { it.id == idB && it.teamSide == TeamSide.HOME }
        if (idxA < 0 || idxB < 0) return state
        val a = players[idxA]
        val b = players[idxB]
        players[idxA] = a.copy(
            squadId = b.squadId,
            jerseyNumber = b.jerseyNumber,
            name = b.name,
            rating = b.rating,
            paceRating = b.paceRating,
            shootingRating = b.shootingRating,
            skinToneIndex = b.skinToneIndex,
            hairStyleIndex = b.hairStyleIndex,
            stamina = b.stamina
        )
        players[idxB] = b.copy(
            squadId = a.squadId,
            jerseyNumber = a.jerseyNumber,
            name = a.name,
            rating = a.rating,
            paceRating = a.paceRating,
            shootingRating = a.shootingRating,
            skinToneIndex = a.skinToneIndex,
            hairStyleIndex = a.hairStyleIndex,
            stamina = a.stamina
        )
        return state.copy(players = players)
    }

    fun substituteHomePlayer(state: MatchState, starterPlayerId: String, benchSquadId: String): MatchState {
        if (state.subsUsed >= state.maxSubs) return state
        val players = state.players.toMutableList()
        val starterIdx = players.indexOfFirst { it.id == starterPlayerId && it.teamSide == TeamSide.HOME }
        val benchIdx = state.homeBench.indexOfFirst { it.id == benchSquadId }
        if (starterIdx < 0 || benchIdx < 0) return state

        val starter = players[starterIdx]
        val incoming = state.homeBench[benchIdx]
        val outgoingAsBench = SquadPlayer(
            id = starter.squadId.ifEmpty { starter.id },
            name = starter.name,
            jerseyNumber = starter.jerseyNumber,
            preferredRole = starter.role,
            rating = starter.rating,
            pace = starter.paceRating,
            shooting = starter.shootingRating,
            skinToneIndex = starter.skinToneIndex,
            hairStyleIndex = starter.hairStyleIndex
        )
        val nextBench = state.homeBench.toMutableList().apply {
            this[benchIdx] = outgoingAsBench
        }
        players[starterIdx] = starter.copy(
            squadId = incoming.id,
            jerseyNumber = incoming.jerseyNumber,
            name = incoming.name,
            rating = incoming.rating,
            paceRating = incoming.pace,
            shootingRating = incoming.shooting,
            skinToneIndex = incoming.skinToneIndex,
            hairStyleIndex = incoming.hairStyleIndex,
            stamina = 1f,
            stunTimer = 0f,
            tackleCooldown = 0f,
            slideTimer = 0f,
            kickAnimTimer = 0f
        )
        return state.copy(
            players = players,
            homeBench = nextBench,
            subsUsed = state.subsUsed + 1,
            bannerMessage = "تبديل: دخول ${incoming.name} ⬆",
            bannerTimer = 1.5f
        )
    }

    private fun defaultActiveHomeId(players: List<PlayerEntity>): String {
        return players.firstOrNull { it.teamSide == TeamSide.HOME && it.slotCode == "ST" }?.id
            ?: players.lastOrNull { it.teamSide == TeamSide.HOME && it.role == PlayerRole.FWD }?.id
            ?: "home_9"
    }

    private fun createPlayersForMatch(
        homeTeam: Team,
        awayTeam: Team,
        homeFormation: FormationType,
        homeStarters: List<SquadPlayer>,
        awayStarters: List<SquadPlayer>,
        homeAttacksRight: Boolean
    ): List<PlayerEntity> {
        val list = mutableListOf<PlayerEntity>()
        homeFormation.slots.forEachIndexed { i, slot ->
            val sp = homeStarters.getOrNull(i) ?: homeTeam.squad.getOrNull(i)
            val sx = slotToFieldX(slot.x)
            val sy = slotToFieldY(slot.y)
            val hx = if (homeAttacksRight) sx else (PITCH_W - sx)
            val hy = if (homeAttacksRight) sy else (PITCH_H - sy)
            val angle = if (homeAttacksRight) 0f else PI.toFloat()
            list += PlayerEntity(
                id = "home_$i",
                squadId = sp?.id ?: "home_sq_$i",
                teamSide = TeamSide.HOME,
                role = slot.role,
                slotCode = slot.code,
                jerseyNumber = sp?.jerseyNumber ?: (i + 1),
                name = sp?.name ?: "لاعب ${i + 1}",
                x = hx,
                y = hy,
                facingAngle = angle,
                homeX = hx,
                homeY = hy,
                rating = sp?.rating ?: homeTeam.overall,
                paceRating = sp?.pace ?: homeTeam.speed,
                shootingRating = sp?.shooting ?: homeTeam.attack,
                skinToneIndex = sp?.skinToneIndex ?: (i % 3),
                hairStyleIndex = sp?.hairStyleIndex ?: (i % 4)
            )
        }

        val awayAttacksRight = !homeAttacksRight
        FormationType.F_4_3_3.slots.forEachIndexed { i, slot ->
            val sp = awayStarters.getOrNull(i) ?: awayTeam.squad.getOrNull(i)
            val sx = slotToFieldX(slot.x)
            val sy = slotToFieldY(slot.y)
            val ax = if (awayAttacksRight) sx else (PITCH_W - sx)
            val ay = if (awayAttacksRight) sy else (PITCH_H - sy)
            val angle = if (awayAttacksRight) 0f else PI.toFloat()
            list += PlayerEntity(
                id = "away_$i",
                squadId = sp?.id ?: "away_sq_$i",
                teamSide = TeamSide.AWAY,
                role = slot.role,
                slotCode = slot.code,
                jerseyNumber = sp?.jerseyNumber ?: (i + 1),
                name = sp?.name ?: "خصم ${i + 1}",
                x = ax,
                y = ay,
                facingAngle = angle,
                homeX = ax,
                homeY = ay,
                rating = sp?.rating ?: awayTeam.overall,
                paceRating = sp?.pace ?: awayTeam.speed,
                shootingRating = sp?.shooting ?: awayTeam.attack,
                skinToneIndex = sp?.skinToneIndex ?: ((i + 1) % 3),
                hairStyleIndex = sp?.hairStyleIndex ?: ((i + 2) % 4)
            )
        }
        return list
    }

    private fun resetPositionsForKickoff(
        players: List<PlayerEntity>,
        homeFormation: FormationType,
        awayFormation: FormationType,
        homeAttacksRight: Boolean
    ): List<PlayerEntity> {
        return players.map { p ->
            val isHome = p.teamSide == TeamSide.HOME
            val idx = p.id.substringAfter('_').toIntOrNull() ?: 0
            val slot = if (isHome) homeFormation.slots.getOrNull(idx) else awayFormation.slots.getOrNull(idx)
            val attacksRight = if (isHome) homeAttacksRight else !homeAttacksRight
            val baseX = slot?.let { slotToFieldX(it.x) } ?: p.homeX
            val baseY = slot?.let { slotToFieldY(it.y) } ?: p.homeY
            val nx = if (attacksRight) baseX else (PITCH_W - baseX)
            val ny = if (attacksRight) baseY else (PITCH_H - baseY)
            val angle = if (attacksRight) 0f else PI.toFloat()
            p.copy(
                role = slot?.role ?: p.role,
                slotCode = slot?.code ?: p.slotCode,
                x = nx,
                y = ny,
                vx = 0f,
                vy = 0f,
                facingAngle = angle,
                homeX = nx,
                homeY = ny,
                stunTimer = 0f,
                tackleCooldown = 0f,
                slideTimer = 0f,
                kickAnimTimer = 0f
            )
        }
    }

    fun step(
        state: MatchState,
        dtRaw: Float,
        input: InputSnapshot,
        difficulty: Difficulty,
        graphicsQuality: GraphicsQuality,
        eventsOut: MutableList<EngineEvent>
    ): MatchState {
        val dt = dtRaw.coerceIn(0.001f, 0.045f)

        var nextParticles = updateParticles(state.particles, dt)
        val nextShake = (state.cameraShake - dt * 2.8f).coerceAtLeast(0f)
        val nextNetLeft = (state.netRippleLeft - dt * 1.5f).coerceAtLeast(0f)
        val nextNetRight = (state.netRippleRight - dt * 1.5f).coerceAtLeast(0f)
        val nextBannerTimer = (state.bannerTimer - dt).coerceAtLeast(0f)
        val nextBannerMsg = if (nextBannerTimer > 0f) state.bannerMessage else null

        // Smooth broadcast camera tracking scaled to 270x180 stadium
        val targetCamX = (state.ball.x + state.ball.vx * 0.14f).coerceIn(48f, PITCH_W - 48f)
        val targetCamY = (state.ball.y + state.ball.vy * 0.08f).coerceIn(34f, PITCH_H - 34f)
        val camBlend = (dt * 5.5f).coerceIn(0f, 1f)
        val nextCamX = state.cameraX + (targetCamX - state.cameraX) * camBlend
        val nextCamY = state.cameraY + (targetCamY - state.cameraY) * camBlend

        if (state.phase == MatchPhase.PAUSED || state.phase == MatchPhase.HALF_TIME || state.phase == MatchPhase.FULL_TIME) {
            return state.copy(
                particles = nextParticles,
                cameraShake = nextShake,
                netRippleLeft = nextNetLeft,
                netRippleRight = nextNetRight,
                bannerTimer = nextBannerTimer,
                bannerMessage = nextBannerMsg,
                cameraX = nextCamX,
                cameraY = nextCamY
            )
        }

        if (state.phase == MatchPhase.PRE_KICKOFF) {
            val timer = state.preKickoffTimer - dt
            return if (timer <= 0f) {
                eventsOut += EngineEvent.Whistle
                state.copy(
                    phase = MatchPhase.PLAYING,
                    preKickoffTimer = 0f,
                    particles = nextParticles,
                    bannerTimer = nextBannerTimer,
                    bannerMessage = nextBannerMsg,
                    cameraX = nextCamX,
                    cameraY = nextCamY
                )
            } else {
                state.copy(
                    preKickoffTimer = timer,
                    particles = nextParticles,
                    bannerTimer = nextBannerTimer,
                    bannerMessage = nextBannerMsg,
                    cameraX = nextCamX,
                    cameraY = nextCamY
                )
            }
        }

        if (state.phase == MatchPhase.SET_PIECE) {
            val timer = state.setPieceTimer - dt
            if (timer > 0f) {
                return state.copy(
                    setPieceTimer = timer,
                    particles = nextParticles,
                    bannerTimer = nextBannerTimer,
                    bannerMessage = nextBannerMsg,
                    cameraX = nextCamX,
                    cameraY = nextCamY
                )
            }
            // Execute automated set piece restart cleanly into play
            return executeSetPieceRestart(state, eventsOut).copy(
                particles = nextParticles,
                cameraX = nextCamX,
                cameraY = nextCamY
            )
        }

        if (state.phase == MatchPhase.GOAL_CELEBRATION) {
            val timer = state.celebrationTimer - dt
            return if (timer <= 0f) {
                eventsOut += EngineEvent.Whistle
                val resetPlayers = resetPositionsForKickoff(
                    players = state.players,
                    homeFormation = state.homeFormation,
                    awayFormation = FormationType.F_4_3_3,
                    homeAttacksRight = state.homeAttacksRight
                )
                state.copy(
                    phase = MatchPhase.PLAYING,
                    players = resetPlayers,
                    ball = BallEntity(x = CENTER_X, y = CENTER_Y),
                    activeHomePlayerId = defaultActiveHomeId(resetPlayers),
                    shotCharge = 0f,
                    isChargingShot = false,
                    passCharge = 0f,
                    isChargingPass = false,
                    celebrationTimer = 0f,
                    carrierIdleSeconds = 0f,
                    lastBallOwnerId = null,
                    particles = nextParticles,
                    netRippleLeft = nextNetLeft,
                    netRippleRight = nextNetRight,
                    cameraShake = nextShake,
                    cameraX = CENTER_X,
                    cameraY = CENTER_Y,
                    bannerMessage = null,
                    bannerTimer = 0f
                )
            } else {
                state.copy(
                    celebrationTimer = timer,
                    particles = nextParticles,
                    netRippleLeft = nextNetLeft,
                    netRippleRight = nextNetRight,
                    cameraShake = nextShake,
                    bannerTimer = nextBannerTimer,
                    bannerMessage = nextBannerMsg,
                    cameraX = nextCamX,
                    cameraY = nextCamY
                )
            }
        }

        // PLAYING PHASE
        val nextRemaining = (state.remainingSeconds - dt).coerceAtLeast(0f)

        // Check Halftime
        if (state.half == 1 && nextRemaining <= state.totalDurationSeconds / 2f) {
            eventsOut += EngineEvent.Whistle
            eventsOut += EngineEvent.HalfTime
            return state.copy(
                phase = MatchPhase.HALF_TIME,
                remainingSeconds = state.totalDurationSeconds / 2f,
                isChargingShot = false,
                shotCharge = 0f,
                isChargingPass = false,
                passCharge = 0f,
                particles = nextParticles,
                cameraX = nextCamX,
                cameraY = nextCamY
            )
        }

        // Check Full Time
        if (nextRemaining <= 0f) {
            eventsOut += EngineEvent.Whistle
            eventsOut += EngineEvent.MatchEnded(state.homeScore, state.awayScore)
            return state.copy(
                phase = MatchPhase.FULL_TIME,
                remainingSeconds = 0f,
                isChargingShot = false,
                shotCharge = 0f,
                isChargingPass = false,
                passCharge = 0f,
                particles = nextParticles,
                cameraShake = nextShake,
                netRippleLeft = nextNetLeft,
                netRippleRight = nextNetRight,
                cameraX = nextCamX,
                cameraY = nextCamY
            )
        }

        val nextShotCharge = if (state.isChargingShot && state.homeHasBall) {
            (state.shotCharge + dt * 1.65f).coerceAtMost(1f)
        } else 0f

        var ball = state.ball
        val players = state.players.toMutableList()
        var activeHomeId = state.activeHomePlayerId
        var stats = state.stats

        // Possession tick
        val ownerTeam = ball.ownerId?.let { if (it.startsWith("home_")) TeamSide.HOME else TeamSide.AWAY }
        stats = when (ownerTeam) {
            TeamSide.HOME -> stats.copy(homePossessionTicks = stats.homePossessionTicks + 1)
            TeamSide.AWAY -> stats.copy(awayPossessionTicks = stats.awayPossessionTicks + 1)
            null -> stats
        }

        val homeOutfield = players.filter { it.teamSide == TeamSide.HOME && it.role != PlayerRole.GK }
        val awayOutfield = players.filter { it.teamSide == TeamSide.AWAY && it.role != PlayerRole.GK }

        val nearestHomeId = homeOutfield.minByOrNull { hypot(it.x - ball.x, it.y - ball.y) }?.id ?: activeHomeId
        val sortedAwayByBall = awayOutfield.sortedBy { hypot(it.x - ball.x, it.y - ball.y) }
        val nearestAwayId = sortedAwayByBall.firstOrNull()?.id ?: "away_9"
        val secondAwayId = sortedAwayByBall.getOrNull(1)?.takeIf {
            hypot(it.x - ball.x, it.y - ball.y) < 54f
        }?.id

        if (ball.ownerId != null && ball.ownerId!!.startsWith("home_")) {
            activeHomeId = ball.ownerId!!
        }

        // Double-Press (2nd teammate closely pinches the ball carrier with active user defender!)
        val doublePressMateId = if (!state.homeHasBall && input.isDoublePressing) {
            homeOutfield
                .filter { it.id != activeHomeId }
                .minByOrNull { hypot(it.x - ball.x, it.y - ball.y) }
                ?.id
        } else null

        // Track how long the human player stands still holding the ball
        val joyMag = hypot(input.moveX, input.moveY)
        val currentHomeCarrier = if (ball.ownerId?.startsWith("home_") == true) {
            players.firstOrNull { it.id == ball.ownerId }
        } else null
        var nextCarrierIdleSeconds = if (
            currentHomeCarrier != null &&
            ball.ownerId == state.lastBallOwnerId &&
            joyMag <= 0.12f
        ) {
            state.carrierIdleSeconds + dt
        } else {
            0f
        }

        // Highlight potential pass target when Home has ball
        val activePlayer = players.firstOrNull { it.id == activeHomeId }
        val passTargetId = if (activePlayer != null && ball.ownerId == activePlayer.id) {
            findBestPassTarget(activePlayer, players, input.moveX, input.moveY)?.id
        } else null

        var passInFlightId = if (ball.ownerId == null && state.passInFlightTimer > 0f) state.passInFlightTargetId else null
        val nextPassInFlightTimer = if (passInFlightId != null) (state.passInFlightTimer - dt).coerceAtLeast(0f) else 0f
        if (nextPassInFlightTimer <= 0f) passInFlightId = null
        val ballCarrier = ball.ownerId?.let { id -> players.firstOrNull { it.id == id } }

        // Step all 22 players
        for (idx in players.indices) {
            val p = players[idx]
            val baseSpeed = 18.8f + (p.paceRating - 80) * 0.28f
            val isHuman = p.id == activeHomeId

            val nextStun = (p.stunTimer - dt).coerceAtLeast(0f)
            val nextTackleCd = (p.tackleCooldown - dt).coerceAtLeast(0f)
            val nextSlide = (p.slideTimer - dt).coerceAtLeast(0f)
            val nextKickAnim = (p.kickAnimTimer - dt).coerceAtLeast(0f)

            var desiredVx = 0f
            var desiredVy = 0f
            var stamina = p.stamina

            if (nextSlide > 0f) {
                desiredVx = p.vx * 0.96f
                desiredVy = p.vy * 0.96f
            } else if (nextStun > 0f) {
                desiredVx = p.vx * 0.82f
                desiredVy = p.vy * 0.82f
            } else if (isHuman && p.id == passInFlightId && ball.ownerId == null) {
                // Smoothly step to meet and trap the incoming pass so it reaches the player cleanly
                val meetVel = seek(p.x, p.y, ball.x + ball.vx * 0.08f, ball.y + ball.vy * 0.08f, baseSpeed * 1.28f)
                desiredVx = meetVel.first
                desiredVy = meetVel.second
                stamina = (stamina + dt * 0.20f).coerceAtMost(1f)
            } else if (isHuman) {
                val mag = hypot(input.moveX, input.moveY)
                if (mag > 0.08f) {
                    val nx = input.moveX / mag.coerceAtLeast(1f)
                    val ny = input.moveY / mag.coerceAtLeast(1f)
                    val canSprint = input.isSprinting && stamina > 0.05f
                    val speedMult = if (canSprint) 1.44f else 1.0f
                    val dribbleDrag = if (ball.ownerId == p.id) 0.93f else 1.0f
                    desiredVx = nx * baseSpeed * speedMult * dribbleDrag
                    desiredVy = ny * baseSpeed * speedMult * dribbleDrag
                    stamina = if (canSprint) {
                        (stamina - dt * 0.28f).coerceAtLeast(0f)
                    } else {
                        (stamina + dt * 0.14f).coerceAtMost(1f)
                    }
                } else if ((input.isPressing || input.isDoublePressing || input.isSprinting) && !state.homeHasBall) {
                    // Defense Press / Sprint button without joystick: auto-close onto the ball carrier!
                    val canSprintPress = (input.isSprinting || input.isDoublePressing) && stamina > 0.05f
                    val pressMult = if (canSprintPress) 1.32f else 1.05f
                    val pressVel = seek(p.x, p.y, ball.x, ball.y, baseSpeed * pressMult)
                    desiredVx = pressVel.first
                    desiredVy = pressVel.second
                    stamina = if (canSprintPress) {
                        (stamina - dt * 0.22f).coerceAtLeast(0f)
                    } else {
                        (stamina + dt * 0.12f).coerceAtMost(1f)
                    }
                } else {
                    stamina = (stamina + dt * 0.26f).coerceAtMost(1f)
                }
            } else if (p.id == doublePressMateId && !state.homeHasBall) {
                // Coordinated 2-Player Press: teammate sprints tight alongside human defender to pinch the carrier!
                val dirToHomeGoalX = (if (state.homeAttacksRight) FIELD_LEFT else FIELD_RIGHT) - ball.x
                val dirToHomeGoalY = CENTER_Y - ball.y
                val gLen = hypot(dirToHomeGoalX, dirToHomeGoalY).coerceAtLeast(1f)
                val perpX = -dirToHomeGoalY / gLen
                val perpY = dirToHomeGoalX / gLen
                val activeDef = players.firstOrNull { it.id == activeHomeId }
                val sideSign = if (activeDef != null && ((activeDef.x - ball.x) * perpX + (activeDef.y - ball.y) * perpY) >= 0f) -1f else 1f
                val targetX = (ball.x + (dirToHomeGoalX / gLen) * 1.4f + perpX * sideSign * 2.1f)
                    .coerceIn(FIELD_LEFT + 1.5f, FIELD_RIGHT - 1.5f)
                val targetY = (ball.y + (dirToHomeGoalY / gLen) * 1.4f + perpY * sideSign * 2.1f)
                    .coerceIn(FIELD_TOP + 1.5f, FIELD_BOTTOM - 1.5f)
                val sprintMult = if (stamina > 0.08f) 1.24f else 0.96f
                val pressVel = seek(p.x, p.y, targetX, targetY, baseSpeed * sprintMult)
                desiredVx = pressVel.first
                desiredVy = pressVel.second
                val pressSpeed = hypot(desiredVx, desiredVy)
                stamina = if (pressSpeed > baseSpeed * 1.05f && stamina > 0.05f) {
                    (stamina - dt * 0.18f).coerceAtLeast(0f)
                } else if (pressSpeed < 1.2f) {
                    (stamina + dt * 0.24f).coerceAtMost(1f)
                } else {
                    (stamina + dt * 0.12f).coerceAtMost(1f)
                }
            } else {
                val homeHasBall = ball.ownerId?.startsWith("home_") == true
                val isAwayAttackerPress = p.teamSide == TeamSide.AWAY && homeHasBall &&
                    (p.id == nearestAwayId || p.id == secondAwayId)
                val aiVel = computeAiDesiredVelocity(
                    player = p,
                    allPlayers = players,
                    ball = ball,
                    carrier = ballCarrier,
                    homeAttacksRight = state.homeAttacksRight,
                    isChaser = (p.id == nearestHomeId || p.id == nearestAwayId || isAwayAttackerPress),
                    isLeadAwayChaser = (p.id == nearestAwayId && homeHasBall),
                    carrierIdleSeconds = nextCarrierIdleSeconds,
                    timeSec = state.remainingSeconds,
                    difficulty = difficulty,
                    baseSpeed = baseSpeed
                )
                desiredVx = aiVel.first
                desiredVy = aiVel.second
                val aiTargetSpeed = hypot(desiredVx, desiredVy)
                stamina = when {
                    aiTargetSpeed > baseSpeed * 1.06f -> (stamina - dt * 0.14f).coerceAtLeast(0.05f)
                    aiTargetSpeed < baseSpeed * 0.42f -> (stamina + dt * 0.24f).coerceAtMost(1f)
                    else -> (stamina + dt * 0.12f).coerceAtMost(1f)
                }

                if (ball.ownerId == p.id) {
                    if (p.teamSide == TeamSide.AWAY) {
                        val awayAttacksRight = !state.homeAttacksRight
                        val decision = evaluateAwayBallAction(p, players, difficulty, state.awayTeam, awayAttacksRight)
                        if (decision == AiBallDecision.SHOOT) {
                            val goalX = if (awayAttacksRight) FIELD_RIGHT + 1f else FIELD_LEFT - 1f
                            val homeGk = players.firstOrNull { it.teamSide == TeamSide.HOME && it.role == PlayerRole.GK }
                            val farCornerY = if (homeGk != null && homeGk.y < CENTER_Y) {
                                GOAL_BOTTOM - 2.0f - rng.nextFloat() * 3.5f
                            } else {
                                GOAL_TOP + 2.0f + rng.nextFloat() * 3.5f
                            }
                            ball = launchShot(p, goalX, farCornerY, 0.82f, state.awayTeam)
                            val onTarget = farCornerY in GOAL_TOP..GOAL_BOTTOM
                            stats = stats.copy(
                                awayShots = stats.awayShots + 1,
                                awayShotsOnTarget = stats.awayShotsOnTarget + if (onTarget) 1 else 0
                            )
                            eventsOut += EngineEvent.Kick(0.88f)
                            players[idx] = p.copy(kickAnimTimer = 0.22f, tackleCooldown = 0.4f)
                            continue
                        } else if (decision == AiBallDecision.PASS) {
                            val mate = findBestAiPassTarget(p, players, awayAttacksRight)
                            if (mate != null) {
                                ball = launchPass(p, mate)
                                eventsOut += EngineEvent.Pass
                                players[idx] = p.copy(kickAnimTimer = 0.18f, tackleCooldown = 0.4f)
                                continue
                            }
                        }
                    } else if (p.role == PlayerRole.GK) {
                        // Home GK distributes smartly to an open defender/midfielder
                        val mate = findBestAiPassTarget(p, players, state.homeAttacksRight)
                            ?: players.filter { it.teamSide == TeamSide.HOME && it.role != PlayerRole.GK }
                                .minByOrNull { hypot(it.x - p.x, it.y - p.y) }
                        if (mate != null) {
                            ball = launchPass(p, mate)
                            eventsOut += EngineEvent.Pass
                            players[idx] = p.copy(kickAnimTimer = 0.18f, tackleCooldown = 0.4f)
                            continue
                        }
                    }
                } else if (p.teamSide == TeamSide.AWAY && p.role != PlayerRole.GK && ball.ownerId?.startsWith("home_") == true) {
                    val distToBall = hypot(p.x - ball.x, p.y - ball.y)
                    val idleTakeoverReady = nextCarrierIdleSeconds >= 2.0f && p.id == nearestAwayId &&
                        (distToBall < 9.5f || nextCarrierIdleSeconds >= 2.08f)
                    val aggressiveTackle = nextTackleCd <= 0f && (
                        distToBall < 2.65f ||
                            (distToBall < 3.65f && rng.nextFloat() < dt * 10.5f * difficulty.aiReactionScale)
                        )
                    if (idleTakeoverReady || aggressiveTackle) {
                        val holderIdx = players.indexOfFirst { it.id == ball.ownerId }
                        if (holderIdx >= 0) {
                            val holder = players[holderIdx]
                            val distHolder = hypot(p.x - holder.x, p.y - holder.y)
                            val awayLateFoul = !idleTakeoverReady &&
                                distToBall > 3.25f &&
                                distHolder < 1.9f &&
                                rng.nextFloat() < 0.06f
                            if (awayLateFoul) {
                                eventsOut += EngineEvent.Whistle
                                val inAwayPenaltyBox = isInsidePenaltyBox(holder.x, holder.y, isLeftBox = !state.homeAttacksRight)
                                return state.copy(
                                    phase = MatchPhase.SET_PIECE,
                                    setPieceType = if (inAwayPenaltyBox) SetPieceType.PENALTY else SetPieceType.FREE_KICK,
                                    setPieceTeam = TeamSide.HOME,
                                    setPieceTimer = 1.15f,
                                    setPieceX = holder.x,
                                    setPieceY = holder.y,
                                    remainingSeconds = nextRemaining,
                                    carrierIdleSeconds = 0f,
                                    lastBallOwnerId = null,
                                    bannerMessage = if (inAwayPenaltyBox) "ركلة جزاء لك!" else "خطأ على الخصم! ركلة حرة",
                                    bannerTimer = 1.25f,
                                    stats = stats.copy(awayFouls = stats.awayFouls + 1),
                                    particles = nextParticles,
                                    cameraX = nextCamX,
                                    cameraY = nextCamY
                                )
                            }
                            players[holderIdx] = holder.copy(stunTimer = 0.72f, tackleCooldown = 0.5f)
                            // If taking over cleanly after 2s idle pause, snap attacker cleanly onto ball spot
                            if (idleTakeoverReady && distToBall > 2.2f) {
                                players[idx] = p.copy(
                                    x = (ball.x - cos(p.facingAngle) * 1.4f).coerceIn(FIELD_LEFT + 1f, FIELD_RIGHT - 1f),
                                    y = (ball.y - sin(p.facingAngle) * 1.4f).coerceIn(FIELD_TOP + 1f, FIELD_BOTTOM - 1f)
                                )
                            }
                            ball = ball.copy(
                                ownerId = p.id,
                                lastTouchTeam = TeamSide.AWAY,
                                lastTouchPlayerName = p.name
                            )
                            nextCarrierIdleSeconds = 0f
                            stats = stats.copy(awayTackles = stats.awayTackles + 1)
                            eventsOut += EngineEvent.Tackle
                        }
                    }
                }
            }

            val blend = if (nextSlide > 0f) 0.15f else (dt * 12f).coerceIn(0f, 1f)
            val vx = p.vx + (desiredVx - p.vx) * blend
            val vy = p.vy + (desiredVy - p.vy) * blend
            val speedMag = hypot(vx, vy)

            val minX = if (p.role == PlayerRole.GK) {
                val gkLeftSide = (p.teamSide == TeamSide.HOME && state.homeAttacksRight) ||
                    (p.teamSide == TeamSide.AWAY && !state.homeAttacksRight)
                if (gkLeftSide) FIELD_LEFT + 0.8f else FIELD_RIGHT - 22f
            } else FIELD_LEFT + 0.8f

            val maxX = if (p.role == PlayerRole.GK) {
                val gkLeftSide = (p.teamSide == TeamSide.HOME && state.homeAttacksRight) ||
                    (p.teamSide == TeamSide.AWAY && !state.homeAttacksRight)
                if (gkLeftSide) FIELD_LEFT + 22f else FIELD_RIGHT - 0.8f
            } else FIELD_RIGHT - 0.8f

            val nx = (p.x + vx * dt).coerceIn(minX, maxX)
            val ny = (p.y + vy * dt).coerceIn(FIELD_TOP + 1f, FIELD_BOTTOM - 1f)
            val newFacing = when {
                nextSlide > 0f -> p.facingAngle
                speedMag > 1.8f -> atan2(vy, vx)
                !isHuman -> atan2(ball.y - ny, ball.x - nx)
                speedMag > 0.6f -> atan2(vy, vx)
                else -> p.facingAngle
            }
            val nextStride = if (speedMag > 1.4f && nextSlide <= 0f) p.stridePhase + dt * speedMag * 0.45f else p.stridePhase

            // Human standing tackle / interception on contact or while holding Sprint+Press or Double-Press
            if (isHuman && nextStun <= 0f && nextTackleCd <= 0f && ball.ownerId?.startsWith("away_") == true) {
                val holderIdx = players.indexOfFirst { it.id == ball.ownerId }
                if (holderIdx >= 0) {
                    val holder = players[holderIdx]
                    val distBall = hypot(nx - ball.x, ny - ball.y)
                    val distHolder = hypot(nx - holder.x, ny - holder.y)
                    val stealRadius = when {
                        nextSlide > 0f -> 4.6f
                        input.isPressing || input.isDoublePressing -> 4.15f
                        else -> 3.10f
                    }
                    if (distBall < stealRadius) {
                        // Cleanly cut/steal the ball!
                        players[holderIdx] = holder.copy(stunTimer = 0.85f, tackleCooldown = 0.6f)
                        ball = ball.copy(
                            ownerId = p.id,
                            lastTouchTeam = TeamSide.HOME,
                            lastTouchPlayerName = p.name
                        )
                        nextCarrierIdleSeconds = 0f
                        stats = stats.copy(homeTackles = stats.homeTackles + 1)
                        eventsOut += EngineEvent.Tackle
                    } else if (nextSlide > 0f && distHolder < 2.2f && distBall > 4.9f) {
                        // Reckless slide far behind the ball -> Foul / Penalty
                        eventsOut += EngineEvent.Whistle
                        val inHomePenaltyBox = isInsidePenaltyBox(holder.x, holder.y, isLeftBox = state.homeAttacksRight)
                        return state.copy(
                            phase = MatchPhase.SET_PIECE,
                            setPieceType = if (inHomePenaltyBox) SetPieceType.PENALTY else SetPieceType.FREE_KICK,
                            setPieceTeam = TeamSide.AWAY,
                            setPieceTimer = 1.25f,
                            setPieceX = holder.x,
                            setPieceY = holder.y,
                            remainingSeconds = nextRemaining,
                            carrierIdleSeconds = 0f,
                            lastBallOwnerId = null,
                            bannerMessage = if (inHomePenaltyBox) "ركلة جزاء للخصم!" else "خطأ! ركلة حرة",
                            bannerTimer = 1.3f,
                            stats = stats.copy(homeFouls = stats.homeFouls + 1),
                            particles = nextParticles,
                            cameraX = nextCamX,
                            cameraY = nextCamY
                        )
                    }
                }
            }

            // AI Home teammate interception (boosted during Double-Press!)
            if (!isHuman && p.teamSide == TeamSide.HOME && p.role != PlayerRole.GK && nextStun <= 0f && nextTackleCd <= 0f && ball.ownerId?.startsWith("away_") == true) {
                val distBall = hypot(nx - ball.x, ny - ball.y)
                val isPartnerPressing = (p.id == doublePressMateId)
                val canSteal = if (isPartnerPressing) distBall < 3.65f else (distBall < 2.75f && rng.nextFloat() < dt * 4.5f)
                if (canSteal) {
                    val holderIdx = players.indexOfFirst { it.id == ball.ownerId }
                    if (holderIdx >= 0) {
                        players[holderIdx] = players[holderIdx].copy(stunTimer = 0.78f, tackleCooldown = 0.55f)
                        ball = ball.copy(ownerId = p.id, lastTouchTeam = TeamSide.HOME, lastTouchPlayerName = p.name)
                        activeHomeId = p.id
                        nextCarrierIdleSeconds = 0f
                        stats = stats.copy(homeTackles = stats.homeTackles + 1)
                        eventsOut += EngineEvent.Tackle
                    }
                }
            }

            players[idx] = p.copy(
                x = nx,
                y = ny,
                vx = vx,
                vy = vy,
                facingAngle = newFacing,
                stamina = stamina,
                stunTimer = nextStun,
                tackleCooldown = nextTackleCd,
                slideTimer = nextSlide,
                kickAnimTimer = nextKickAnim,
                stridePhase = nextStride
            )
        }

        // Soft player separation (allow the 2 Double-Pressing defenders to stay tightly paired!)
        for (i in 0 until players.size) {
            for (j in i + 1 until players.size) {
                val a = players[i]
                val b = players[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = hypot(dx, dy)
                val doublePressPair = doublePressMateId != null &&
                    ((a.id == activeHomeId && b.id == doublePressMateId) ||
                        (b.id == activeHomeId && a.id == doublePressMateId))
                val minDist = if (doublePressPair) PLAYER_RADIUS * 1.25f else PLAYER_RADIUS * 1.85f
                if (dist in 0.001f..minDist) {
                    val push = (minDist - dist) * 0.45f
                    val ux = dx / dist
                    val uy = dy / dist
                    players[i] = a.copy(
                        x = (a.x - ux * push).coerceIn(FIELD_LEFT + 1f, FIELD_RIGHT - 1f),
                        y = (a.y - uy * push).coerceIn(FIELD_TOP + 1f, FIELD_BOTTOM - 1f)
                    )
                    players[j] = b.copy(
                        x = (b.x + ux * push).coerceIn(FIELD_LEFT + 1f, FIELD_RIGHT - 1f),
                        y = (b.y + uy * push).coerceIn(FIELD_TOP + 1f, FIELD_BOTTOM - 1f)
                    )
                }
            }
        }

        // Ball Physics
        val owner = ball.ownerId?.let { id -> players.firstOrNull { it.id == id } }
        if (owner != null) {
            val dribbleDist = PLAYER_RADIUS + BALL_RADIUS + 0.45f
            val bx = owner.x + cos(owner.facingAngle) * dribbleDist
            val by = owner.y + sin(owner.facingAngle) * dribbleDist
            ball = ball.copy(
                x = bx.coerceIn(FIELD_LEFT + 0.5f, FIELD_RIGHT - 0.5f),
                y = by.coerceIn(FIELD_TOP + 0.5f, FIELD_BOTTOM - 0.5f),
                z = 0f,
                vx = owner.vx,
                vy = owner.vy,
                vz = 0f,
                spinAngle = ball.spinAngle + hypot(owner.vx, owner.vy) * dt * 0.4f,
                trail = emptyList()
            )
        } else {
            var bx = ball.x + ball.vx * dt
            var by = ball.y + ball.vy * dt
            var bz = ball.z + ball.vz * dt
            var bvx = ball.vx
            var bvy = ball.vy
            var bvz = ball.vz - 26f * dt

            if (bz <= 0f) {
                bz = 0f
                bvz = if (bvz < -3.5f) -bvz * 0.45f else 0f
                val friction = (1f - 1.75f * dt).coerceAtLeast(0.84f)
                bvx *= friction
                bvy *= friction
            } else {
                val drag = (1f - 0.32f * dt).coerceAtLeast(0.95f)
                bvx *= drag
                bvy *= drag
            }

            // 4 Goalposts collision
            val posts = listOf(
                FIELD_LEFT to GOAL_TOP,
                FIELD_LEFT to GOAL_BOTTOM,
                FIELD_RIGHT to GOAL_TOP,
                FIELD_RIGHT to GOAL_BOTTOM
            )
            var hitPost = false
            for ((px, py) in posts) {
                val dx = bx - px
                val dy = by - py
                val d = hypot(dx, dy)
                val minD = BALL_RADIUS + POST_RADIUS
                if (d < minD && d > 0.001f) {
                    val nx = dx / d
                    val ny = dy / d
                    bx = px + nx * minD
                    by = py + ny * minD
                    val dot = bvx * nx + bvy * ny
                    if (dot < 0f) {
                        bvx = (bvx - 2f * dot * nx) * 0.78f
                        bvy = (bvy - 2f * dot * ny) * 0.78f
                        hitPost = true
                    }
                }
            }
            if (hitPost) {
                eventsOut += EngineEvent.PostClang
            }

            // GOAL DETECTION (Left or Right goal mouth!)
            val inGoalMouthY = by in (GOAL_TOP + 0.4f)..(GOAL_BOTTOM - 0.4f)
            if (inGoalMouthY && bx > FIELD_RIGHT + 0.6f) {
                val scoringSide = if (state.homeAttacksRight) TeamSide.HOME else TeamSide.AWAY
                return handleGoalScored(
                    state, scoringSide, isRightGoal = true, bx = FIELD_RIGHT + 2f, by = by,
                    nextRemaining = nextRemaining, players = players, ball = ball,
                    stats = stats, graphicsQuality = graphicsQuality, nextParticles = nextParticles,
                    eventsOut = eventsOut
                )
            } else if (inGoalMouthY && bx < FIELD_LEFT - 0.6f) {
                val scoringSide = if (state.homeAttacksRight) TeamSide.AWAY else TeamSide.HOME
                return handleGoalScored(
                    state, scoringSide, isRightGoal = false, bx = FIELD_LEFT - 2f, by = by,
                    nextRemaining = nextRemaining, players = players, ball = ball,
                    stats = stats, graphicsQuality = graphicsQuality, nextParticles = nextParticles,
                    eventsOut = eventsOut
                )
            }

            // OUT OF BOUNDS: Throw-In, Corner Kick, or Goal Kick!
            if (by < FIELD_TOP - BALL_RADIUS || by > FIELD_BOTTOM + BALL_RADIUS) {
                val restartTeam = if (ball.lastTouchTeam == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME
                val clampedX = bx.coerceIn(FIELD_LEFT + 6f, FIELD_RIGHT - 6f)
                val clampedY = if (by < FIELD_TOP) FIELD_TOP + 0.5f else FIELD_BOTTOM - 0.5f
                eventsOut += EngineEvent.Whistle
                return state.copy(
                    phase = MatchPhase.SET_PIECE,
                    setPieceType = SetPieceType.THROW_IN,
                    setPieceTeam = restartTeam,
                    setPieceTimer = 0.85f,
                    setPieceX = clampedX,
                    setPieceY = clampedY,
                    ball = ball.copy(x = clampedX, y = clampedY, z = 0f, vx = 0f, vy = 0f, ownerId = null),
                    players = players,
                    remainingSeconds = nextRemaining,
                    bannerMessage = if (restartTeam == TeamSide.HOME) "رمية تماس لك" else "رمية تماس للخصم",
                    bannerTimer = 0.95f,
                    particles = nextParticles,
                    cameraX = nextCamX,
                    cameraY = nextCamY,
                    stats = stats
                )
            }

            if (!inGoalMouthY && (bx < FIELD_LEFT - BALL_RADIUS || bx > FIELD_RIGHT + BALL_RADIUS)) {
                val wentLeft = bx < FIELD_LEFT
                val leftSideOwner = if (state.homeAttacksRight) TeamSide.HOME else TeamSide.AWAY
                val defendingSide = if (wentLeft) leftSideOwner else (if (leftSideOwner == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME)
                val attackingSide = if (defendingSide == TeamSide.HOME) TeamSide.AWAY else TeamSide.HOME

                eventsOut += EngineEvent.Whistle
                if (ball.lastTouchTeam == defendingSide) {
                    // Corner Kick!
                    val cornerX = if (wentLeft) FIELD_LEFT + 1.5f else FIELD_RIGHT - 1.5f
                    val cornerY = if (by < CENTER_Y) FIELD_TOP + 1.5f else FIELD_BOTTOM - 1.5f
                    val nextStats = if (attackingSide == TeamSide.HOME) stats.copy(homeCorners = stats.homeCorners + 1)
                    else stats.copy(awayCorners = stats.awayCorners + 1)
                    return state.copy(
                        phase = MatchPhase.SET_PIECE,
                        setPieceType = SetPieceType.CORNER,
                        setPieceTeam = attackingSide,
                        setPieceTimer = 1.0f,
                        setPieceX = cornerX,
                        setPieceY = cornerY,
                        ball = ball.copy(x = cornerX, y = cornerY, z = 0f, vx = 0f, vy = 0f, ownerId = null),
                        players = players,
                        remainingSeconds = nextRemaining,
                        carrierIdleSeconds = 0f,
                        lastBallOwnerId = null,
                        bannerMessage = if (attackingSide == TeamSide.HOME) "ركلة ركنية لك!" else "ركنية للخصم",
                        bannerTimer = 1.1f,
                        particles = nextParticles,
                        cameraX = nextCamX,
                        cameraY = nextCamY,
                        stats = nextStats
                    )
                } else {
                    // Goal Kick!
                    val gkX = if (wentLeft) FIELD_LEFT + 12f else FIELD_RIGHT - 12f
                    val gkY = CENTER_Y
                    return state.copy(
                        phase = MatchPhase.SET_PIECE,
                        setPieceType = SetPieceType.GOAL_KICK,
                        setPieceTeam = defendingSide,
                        setPieceTimer = 0.9f,
                        setPieceX = gkX,
                        setPieceY = gkY,
                        ball = ball.copy(x = gkX, y = gkY, z = 0f, vx = 0f, vy = 0f, ownerId = null),
                        players = players,
                        remainingSeconds = nextRemaining,
                        carrierIdleSeconds = 0f,
                        lastBallOwnerId = null,
                        bannerMessage = "ركلة مرمى",
                        bannerTimer = 0.95f,
                        particles = nextParticles,
                        cameraX = nextCamX,
                        cameraY = nextCamY,
                        stats = stats
                    )
                }
            }

            val nextTrail = if (graphicsQuality == GraphicsQuality.LOW) emptyList()
            else if (hypot(bvx, bvy) > 26f) {
                val trailMax = when (graphicsQuality) {
                    GraphicsQuality.LOW -> 0
                    GraphicsQuality.MEDIUM -> 5
                    GraphicsQuality.HIGH -> 10
                    GraphicsQuality.ULTRA -> 16
                }
                (listOf(bx to by) + ball.trail).take(trailMax)
            } else {
                ball.trail.dropLast(1)
            }

            ball = ball.copy(
                x = bx,
                y = by,
                z = bz,
                vx = bvx,
                vy = bvy,
                vz = bvz,
                spinAngle = ball.spinAngle + hypot(bvx, bvy) * dt * 0.45f,
                trail = nextTrail
            )

            // Free ball pickup / pass interception / GK save
            if (bz < 5.2f) {
                for (p in players) {
                    if (p.stunTimer > 0f || p.kickAnimTimer > 0f) continue
                    val dist = hypot(p.x - ball.x, p.y - ball.y)
                    val pickupR = when {
                        p.role == PlayerRole.GK -> PLAYER_RADIUS + BALL_RADIUS + 1.35f
                        p.id == activeHomeId -> PLAYER_RADIUS + BALL_RADIUS + 1.55f
                        else -> PLAYER_RADIUS + BALL_RADIUS + 0.70f
                    }
                    if (dist < pickupR) {
                        val bSpeed = hypot(ball.vx, ball.vy)
                        if (p.role == PlayerRole.GK && bSpeed > 44f) {
                            val clearDir = if (p.x < CENTER_X) 1f else -1f
                            ball = ball.copy(
                                vx = clearDir * 30f,
                                vy = (rng.nextFloat() - 0.5f) * 32f,
                                vz = 6.2f,
                                ownerId = null,
                                lastTouchTeam = p.teamSide,
                                lastTouchPlayerName = p.name
                            )
                            eventsOut += EngineEvent.Pass
                            return state.copy(
                                remainingSeconds = nextRemaining,
                                players = players,
                                ball = ball,
                                activeHomePlayerId = activeHomeId,
                                passTargetHomeId = passTargetId,
                                shotCharge = nextShotCharge,
                                carrierIdleSeconds = 0f,
                                lastBallOwnerId = null,
                                bannerMessage = "تصدي رائع من ${p.name}!",
                                bannerTimer = 1.15f,
                                particles = nextParticles,
                                cameraShake = nextShake,
                                cameraX = nextCamX,
                                cameraY = nextCamY,
                                netRippleLeft = nextNetLeft,
                                netRippleRight = nextNetRight,
                                stats = stats
                            )
                        } else {
                            ball = ball.copy(
                                ownerId = p.id,
                                z = 0f,
                                vz = 0f,
                                lastTouchTeam = p.teamSide,
                                lastTouchPlayerName = p.name
                            )
                            if (p.teamSide == TeamSide.HOME) {
                                activeHomeId = p.id
                            }
                            passInFlightId = null
                            break
                        }
                    }
                }
            }
        }

        return state.copy(
            remainingSeconds = nextRemaining,
            players = players,
            ball = ball,
            activeHomePlayerId = activeHomeId,
            passTargetHomeId = passTargetId,
            passInFlightTargetId = passInFlightId,
            passInFlightTimer = if (passInFlightId != null) nextPassInFlightTimer else 0f,
            shotCharge = nextShotCharge,
            carrierIdleSeconds = nextCarrierIdleSeconds,
            lastBallOwnerId = ball.ownerId,
            particles = nextParticles,
            cameraShake = nextShake,
            cameraX = nextCamX,
            cameraY = nextCamY,
            netRippleLeft = nextNetLeft,
            netRippleRight = nextNetRight,
            bannerMessage = nextBannerMsg,
            bannerTimer = nextBannerTimer,
            stats = stats
        )
    }

    private fun handleGoalScored(
        state: MatchState,
        scoringSide: TeamSide,
        isRightGoal: Boolean,
        bx: Float,
        by: Float,
        nextRemaining: Float,
        players: List<PlayerEntity>,
        ball: BallEntity,
        stats: MatchStats,
        graphicsQuality: GraphicsQuality,
        nextParticles: List<PitchParticle>,
        eventsOut: MutableList<EngineEvent>
    ): MatchState {
        val team = if (scoringSide == TeamSide.HOME) state.homeTeam else state.awayTeam
        val scorerName = ball.lastTouchPlayerName.ifEmpty { team.playerNames.last() }
        eventsOut += EngineEvent.Goal(scoringSide, scorerName)
        val confettiCount = when (graphicsQuality) {
            GraphicsQuality.LOW -> 0
            GraphicsQuality.MEDIUM -> 24
            GraphicsQuality.HIGH -> 60
            GraphicsQuality.ULTRA -> 88
        }
        val particlesWithConfetti = spawnGoalConfetti(nextParticles, bx, by, team, confettiCount)
        return state.copy(
            phase = MatchPhase.GOAL_CELEBRATION,
            homeScore = state.homeScore + if (scoringSide == TeamSide.HOME) 1 else 0,
            awayScore = state.awayScore + if (scoringSide == TeamSide.AWAY) 1 else 0,
            remainingSeconds = nextRemaining,
            ball = ball.copy(x = bx, y = by.coerceIn(GOAL_TOP + 1.5f, GOAL_BOTTOM - 1.5f), vx = 0f, vy = 0f, z = 0f),
            players = players,
            lastScorerTeam = scoringSide,
            lastScorerName = scorerName,
            celebrationTimer = 2.2f,
            netRippleLeft = if (!isRightGoal) 1.0f else state.netRippleLeft,
            netRippleRight = if (isRightGoal) 1.0f else state.netRippleRight,
            cameraShake = if (graphicsQuality == GraphicsQuality.LOW) 0f else 1.0f,
            isChargingShot = false,
            shotCharge = 0f,
            bannerMessage = if (scoringSide == TeamSide.HOME) "هدف عالمي! $scorerName" else "هدف للخصم! $scorerName",
            bannerTimer = 2.15f,
            particles = particlesWithConfetti,
            stats = stats
        )
    }

    private fun executeSetPieceRestart(state: MatchState, eventsOut: MutableList<EngineEvent>): MatchState {
        eventsOut += EngineEvent.Whistle
        val prefix = if (state.setPieceTeam == TeamSide.HOME) "home_" else "away_"
        val players = state.players.toMutableList()
        val takerIdx = players.indexOfFirst { it.id.startsWith(prefix) && it.role != PlayerRole.GK }
            .takeIf { it >= 0 } ?: 0
        val taker = players[takerIdx]

        val attackDir = if (
            (state.setPieceTeam == TeamSide.HOME && state.homeAttacksRight) ||
            (state.setPieceTeam == TeamSide.AWAY && !state.homeAttacksRight)
        ) 1f else -1f

        // Move taker to set-piece spot
        players[takerIdx] = taker.copy(
            x = state.setPieceX,
            y = state.setPieceY,
            vx = 0f,
            vy = 0f,
            kickAnimTimer = 0.22f
        )

        val ball: BallEntity = when (state.setPieceType) {
            SetPieceType.PENALTY -> {
                val goalX = if (attackDir > 0f) FIELD_RIGHT + 1f else FIELD_LEFT - 1f
                val aimY = if (rng.nextBoolean()) GOAL_TOP + 2.5f else GOAL_BOTTOM - 2.5f
                launchShot(players[takerIdx], goalX, aimY, 0.92f, if (state.setPieceTeam == TeamSide.HOME) state.homeTeam else state.awayTeam)
            }
            SetPieceType.CORNER -> {
                val boxX = if (state.setPieceX > CENTER_X) FIELD_RIGHT - 22f else FIELD_LEFT + 22f
                val boxY = CENTER_Y + (rng.nextFloat() - 0.5f) * 20f
                val dx = boxX - state.setPieceX
                val dy = boxY - state.setPieceY
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                BallEntity(
                    x = state.setPieceX,
                    y = state.setPieceY,
                    z = 0.6f,
                    vx = (dx / dist) * 58f,
                    vy = (dy / dist) * 58f,
                    vz = 11.0f,
                    lastTouchTeam = state.setPieceTeam,
                    lastTouchPlayerName = taker.name
                )
            }
            else -> {
                val mate = players.filter { it.id.startsWith(prefix) && it.id != taker.id }
                    .minByOrNull { hypot(it.x - state.setPieceX, it.y - state.setPieceY) }
                if (mate != null) {
                    launchPass(players[takerIdx], mate)
                } else {
                    BallEntity(x = state.setPieceX, y = state.setPieceY, vx = attackDir * 36f, vy = 0f)
                }
            }
        }
        eventsOut += EngineEvent.Pass

        val nextActiveHome = if (state.setPieceTeam == TeamSide.HOME) {
            players.filter { it.teamSide == TeamSide.HOME && it.role != PlayerRole.GK }
                .minByOrNull { hypot(it.x - ball.x, it.y - ball.y) }?.id ?: state.activeHomePlayerId
        } else state.activeHomePlayerId

        return state.copy(
            phase = MatchPhase.PLAYING,
            setPieceType = SetPieceType.NONE,
            players = players,
            ball = ball,
            activeHomePlayerId = nextActiveHome,
            carrierIdleSeconds = 0f,
            lastBallOwnerId = null
        )
    }

    fun handleShootRelease(
        state: MatchState,
        input: InputSnapshot,
        graphicsQuality: GraphicsQuality,
        eventsOut: MutableList<EngineEvent>
    ): MatchState {
        if (state.phase != MatchPhase.PLAYING) return state.copy(isChargingShot = false, shotCharge = 0f)
        val active = state.players.firstOrNull { it.id == state.activeHomePlayerId }
            ?: return state.copy(isChargingShot = false, shotCharge = 0f)

        if (state.ball.ownerId == active.id) {
            val power = state.shotCharge.coerceIn(0.38f, 1.0f)
            val goalX = if (state.homeAttacksRight) FIELD_RIGHT + 1f else FIELD_LEFT - 1f
            val aimY = (CENTER_Y + input.moveY * 9.0f).coerceIn(GOAL_TOP + 1.2f, GOAL_BOTTOM - 1.2f)
            val shotBall = launchShot(active, goalX, aimY, power, state.homeTeam)
            eventsOut += EngineEvent.Kick(power)
            val updatedPlayers = state.players.map {
                if (it.id == active.id) it.copy(kickAnimTimer = 0.22f) else it
            }
            return state.copy(
                ball = shotBall,
                players = updatedPlayers,
                isChargingShot = false,
                shotCharge = 0f,
                carrierIdleSeconds = 0f,
                lastBallOwnerId = null,
                stats = state.stats.copy(
                    homeShots = state.stats.homeShots + 1,
                    homeShotsOnTarget = state.stats.homeShotsOnTarget + 1
                )
            )
        }
        return state.copy(isChargingShot = false, shotCharge = 0f)
    }

    fun handleShortPass(
        state: MatchState,
        input: InputSnapshot,
        eventsOut: MutableList<EngineEvent>
    ): MatchState {
        if (state.phase != MatchPhase.PLAYING) return state
        val active = state.players.firstOrNull { it.id == state.activeHomePlayerId } ?: return state
        if (state.ball.ownerId != active.id) return switchHomePlayer(state)

        val target = findBestPassTarget(active, state.players, input.moveX, input.moveY)
            ?: return state
        val passedBall = launchPass(active, target)
        eventsOut += EngineEvent.Pass
        val updatedPlayers = state.players.map {
            if (it.id == active.id) it.copy(kickAnimTimer = 0.18f) else it
        }
        return state.copy(
            ball = passedBall,
            players = updatedPlayers,
            activeHomePlayerId = target.id,
            passInFlightTargetId = target.id,
            passInFlightTimer = 0.95f,
            isChargingShot = false,
            shotCharge = 0f,
            carrierIdleSeconds = 0f,
            lastBallOwnerId = null
        )
    }

    fun handleThroughPass(
        state: MatchState,
        input: InputSnapshot,
        eventsOut: MutableList<EngineEvent>
    ): MatchState {
        if (state.phase != MatchPhase.PLAYING) return state
        val active = state.players.firstOrNull { it.id == state.activeHomePlayerId } ?: return state
        if (state.ball.ownerId != active.id) return switchHomePlayer(state)

        val attackDir = if (state.homeAttacksRight) 1f else -1f
        val aimMag = hypot(input.moveX, input.moveY)
        val dirX = if (aimMag > 0.15f) input.moveX / aimMag else cos(active.facingAngle)
        val dirY = if (aimMag > 0.15f) input.moveY / aimMag else sin(active.facingAngle)

        val inAttackHalf = if (attackDir > 0f) active.x > CENTER_X + 10f else active.x < CENTER_X - 10f
        val onWing = abs(active.y - CENTER_Y) > 30f
        val isCross = inAttackHalf && onWing

        val mates = state.players.filter {
            it.teamSide == TeamSide.HOME && it.id != active.id && it.role != PlayerRole.GK
        }

        val runner = if (isCross) {
            val boxX = if (attackDir > 0f) FIELD_RIGHT - 24f else FIELD_LEFT + 24f
            mates.minByOrNull { hypot(it.x - boxX, it.y - CENTER_Y) }
        } else {
            mates.maxByOrNull { mate ->
                val dx = mate.x - active.x
                val dy = mate.y - active.y
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                val dot = (dx / dist) * dirX + (dy / dist) * dirY
                dot * 3.8f + ((mate.x - active.x) * attackDir) * 0.03f - dist * 0.008f
            }
        } ?: return state

        val leadX = if (isCross) {
            val targetBoxX = if (attackDir > 0f) FIELD_RIGHT - 22f else FIELD_LEFT + 22f
            (runner.x * 0.65f + targetBoxX * 0.35f).coerceIn(FIELD_LEFT + 8f, FIELD_RIGHT - 8f)
        } else {
            (runner.x + dirX * 16f).coerceIn(FIELD_LEFT + 8f, FIELD_RIGHT - 8f)
        }
        val leadY = if (isCross) {
            (runner.y * 0.60f + CENTER_Y * 0.40f).coerceIn(FIELD_TOP + 12f, FIELD_BOTTOM - 12f)
        } else {
            (runner.y + dirY * 10f).coerceIn(FIELD_TOP + 6f, FIELD_BOTTOM - 6f)
        }

        val dx = leadX - active.x
        val dy = leadY - active.y
        val dist = hypot(dx, dy).coerceAtLeast(1f)
        val speed = if (isCross) {
            (dist * 1.45f + 22f).coerceIn(54f, 88f)
        } else {
            (dist * 1.65f + 24f).coerceIn(52f, 86f)
        }
        val loftVz = if (isCross) 8.8f else 3.4f

        val throughOrCrossBall = BallEntity(
            x = active.x + (dx / dist) * 2.0f,
            y = active.y + (dy / dist) * 2.0f,
            z = 0.25f,
            vx = (dx / dist) * speed,
            vy = (dy / dist) * speed,
            vz = loftVz,
            ownerId = null,
            lastTouchTeam = TeamSide.HOME,
            lastTouchPlayerName = active.name
        )
        eventsOut += EngineEvent.Pass
        val updatedPlayers = state.players.map {
            if (it.id == active.id) it.copy(kickAnimTimer = 0.20f) else it
        }
        return state.copy(
            ball = throughOrCrossBall,
            players = updatedPlayers,
            activeHomePlayerId = runner.id,
            passInFlightTargetId = runner.id,
            passInFlightTimer = if (isCross) 1.15f else 1.0f,
            isChargingShot = false,
            shotCharge = 0f,
            carrierIdleSeconds = 0f,
            lastBallOwnerId = null
        )
    }

    fun performSlideTackle(
        state: MatchState,
        input: InputSnapshot,
        graphicsQuality: GraphicsQuality,
        eventsOut: MutableList<EngineEvent>
    ): MatchState {
        if (state.phase != MatchPhase.PLAYING) return state
        val activeIdx = state.players.indexOfFirst { it.id == state.activeHomePlayerId }
        if (activeIdx < 0) return state
        val active = state.players[activeIdx]
        if (active.tackleCooldown > 0f) return state

        val inMag = hypot(input.moveX, input.moveY)
        val dirX: Float
        val dirY: Float
        if (inMag > 0.15f) {
            dirX = input.moveX / inMag
            dirY = input.moveY / inMag
        } else {
            val bx = state.ball.x - active.x
            val by = state.ball.y - active.y
            val bDist = hypot(bx, by).coerceAtLeast(0.01f)
            dirX = bx / bDist
            dirY = by / bDist
        }

        val players = state.players.toMutableList()
        players[activeIdx] = active.copy(
            vx = dirX * 44f,
            vy = dirY * 44f,
            facingAngle = atan2(dirY, dirX),
            slideTimer = 0.38f,
            tackleCooldown = 0.55f
        )
        eventsOut += EngineEvent.Tackle

        // Immediate front lunge steal if ball is right in front of defender
        var ball = state.ball
        var stats = state.stats
        val reachX = active.x + dirX * 2.5f
        val reachY = active.y + dirY * 2.5f
        if (hypot(reachX - ball.x, reachY - ball.y) < 4.9f && ball.z < 4.5f) {
            val prevOwnerId = ball.ownerId
            if (prevOwnerId != null && prevOwnerId.startsWith("away_")) {
                val prevIdx = players.indexOfFirst { it.id == prevOwnerId }
                if (prevIdx >= 0) {
                    players[prevIdx] = players[prevIdx].copy(stunTimer = 0.85f, tackleCooldown = 0.6f)
                }
                stats = stats.copy(homeTackles = stats.homeTackles + 1)
            }
            ball = ball.copy(
                ownerId = active.id,
                vx = 0f,
                vy = 0f,
                z = 0f,
                lastTouchTeam = TeamSide.HOME,
                lastTouchPlayerName = active.name
            )
        }

        return state.copy(
            players = players,
            ball = ball,
            stats = stats,
            carrierIdleSeconds = 0f
        )
    }

    fun switchHomePlayer(state: MatchState): MatchState {
        if (state.phase != MatchPhase.PLAYING) return state
        val candidates = state.players.filter {
            it.teamSide == TeamSide.HOME && it.role != PlayerRole.GK && it.id != state.activeHomePlayerId
        }
        val next = candidates.minByOrNull { hypot(it.x - state.ball.x, it.y - state.ball.y) }
        return if (next != null) state.copy(activeHomePlayerId = next.id) else state
    }

    private fun isInsidePenaltyBox(x: Float, y: Float, isLeftBox: Boolean): Boolean {
        val inY = y in 43f..137f
        return if (isLeftBox) inY && x in FIELD_LEFT..(FIELD_LEFT + 39.3f)
        else inY && x in (FIELD_RIGHT - 39.3f)..FIELD_RIGHT
    }

    private fun launchShot(shooter: PlayerEntity, targetX: Float, targetY: Float, power: Float, team: Team): BallEntity {
        val dx = targetX - shooter.x
        val dy = targetY - shooter.y
        val dist = hypot(dx, dy).coerceAtLeast(1f)
        val shotSkill = ((shooter.shootingRating + team.attack) * 0.5f)
        val speed = 68f + power * 44f + (shotSkill - 85f) * 0.85f
        return BallEntity(
            x = shooter.x + (dx / dist) * 2.0f,
            y = shooter.y + (dy / dist) * 2.0f,
            z = 0.3f,
            vx = (dx / dist) * speed,
            vy = (dy / dist) * speed,
            vz = 4.6f + power * 6.8f,
            ownerId = null,
            lastTouchTeam = shooter.teamSide,
            lastTouchPlayerName = shooter.name
        )
    }

    private fun launchPass(passer: PlayerEntity, target: PlayerEntity): BallEntity {
        val tx = (target.x + target.vx * 0.14f).coerceIn(FIELD_LEFT + 3f, FIELD_RIGHT - 3f)
        val ty = (target.y + target.vy * 0.14f).coerceIn(FIELD_TOP + 3f, FIELD_BOTTOM - 3f)
        val dx = tx - passer.x
        val dy = ty - passer.y
        val dist = hypot(dx, dy).coerceAtLeast(1f)
        val passSpeed = (dist * 1.75f + 24f).coerceIn(44f, 86f)
        return BallEntity(
            x = passer.x + (dx / dist) * 2.0f,
            y = passer.y + (dy / dist) * 2.0f,
            z = 0.12f,
            vx = (dx / dist) * passSpeed,
            vy = (dy / dist) * passSpeed,
            vz = 1.4f,
            ownerId = null,
            lastTouchTeam = passer.teamSide,
            lastTouchPlayerName = passer.name
        )
    }

    private fun findBestPassTarget(
        passer: PlayerEntity,
        players: List<PlayerEntity>,
        aimX: Float,
        aimY: Float
    ): PlayerEntity? {
        val mates = players.filter { it.teamSide == passer.teamSide && it.id != passer.id }
        val aimMag = hypot(aimX, aimY)
        val dirX = if (aimMag > 0.15f) aimX / aimMag else cos(passer.facingAngle)
        val dirY = if (aimMag > 0.15f) aimY / aimMag else sin(passer.facingAngle)

        return mates.maxByOrNull { mate ->
            val dx = mate.x - passer.x
            val dy = mate.y - passer.y
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            val ux = dx / dist
            val uy = dy / dist
            val dot = ux * dirX + uy * dirY
            // Strongly prioritize the teammate in the exact aimed/facing direction
            val coneBonus = if (dot > 0.55f) dot * 5.2f else dot * 3.0f
            val gkPenalty = if (mate.role == PlayerRole.GK && dot < 0.7f) -1.6f else 0f
            val distScore = -dist * 0.010f
            coneBonus + distScore + gkPenalty
        }
    }

    private fun findBestAiPassTarget(
        passer: PlayerEntity,
        players: List<PlayerEntity>,
        teamAttacksRight: Boolean
    ): PlayerEntity? {
        val attackDir = if (teamAttacksRight) 1f else -1f
        val opponents = players.filter { it.teamSide != passer.teamSide }
        val candidates = players.filter {
            it.teamSide == passer.teamSide && it.id != passer.id && it.role != PlayerRole.GK
        }
        return candidates.maxByOrNull { mate ->
            val dx = mate.x - passer.x
            val dy = mate.y - passer.y
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            val forwardGain = (dx * attackDir)
            val nearestOppDist = opponents.minOfOrNull { hypot(it.x - mate.x, it.y - mate.y) } ?: 25f
            val spaceScore = nearestOppDist.coerceAtMost(22f) * 1.4f
            val idealDistScore = if (dist in 16f..68f) 14f else -abs(dist - 40f) * 0.25f
            val roleAttackBonus = when (mate.role) {
                PlayerRole.FWD -> 8f
                PlayerRole.MID -> 5f
                else -> 0f
            }
            forwardGain * 0.55f + spaceScore + idealDistScore + roleAttackBonus
        }
    }

    private fun computeAiDesiredVelocity(
        player: PlayerEntity,
        allPlayers: List<PlayerEntity>,
        ball: BallEntity,
        carrier: PlayerEntity?,
        homeAttacksRight: Boolean,
        isChaser: Boolean,
        isLeadAwayChaser: Boolean,
        carrierIdleSeconds: Float,
        timeSec: Float,
        difficulty: Difficulty,
        baseSpeed: Float
    ): Pair<Float, Float> {
        val speedScale = if (player.teamSide == TeamSide.AWAY) difficulty.aiSpeedScale else 0.92f
        val maxV = baseSpeed * speedScale
        val teamAttacksRight = if (player.teamSide == TeamSide.HOME) homeAttacksRight else !homeAttacksRight
        val shiftDir = if (teamAttacksRight) 1f else -1f
        val oppGoalX = if (teamAttacksRight) FIELD_RIGHT else FIELD_LEFT
        val ownGoalX = if (teamAttacksRight) FIELD_LEFT else FIELD_RIGHT
        val opponents = allPlayers.filter { it.teamSide != player.teamSide && it.role != PlayerRole.GK }

        if (player.role == PlayerRole.GK) {
            val goalX = if (teamAttacksRight) FIELD_LEFT + 4.8f else FIELD_RIGHT - 4.8f
            val ballInOwnHalf = if (teamAttacksRight) ball.x < CENTER_X else ball.x > CENTER_X
            val sweepStep = if (ballInOwnHalf) shiftDir * 2.2f else 0f
            val rushZone = if (teamAttacksRight) ball.x < FIELD_LEFT + 28f else ball.x > FIELD_RIGHT - 28f
            val targetY = if (rushZone && ball.ownerId == null) {
                ball.y.coerceIn(GOAL_TOP - 3f, GOAL_BOTTOM + 3f)
            } else {
                (CENTER_Y + (ball.y - CENTER_Y) * 0.26f).coerceIn(GOAL_TOP + 1.8f, GOAL_BOTTOM - 1.8f)
            }
            val targetX = if (rushZone && ball.ownerId == null) ball.x else (goalX + sweepStep)
            return seek(player.x, player.y, targetX, targetY, maxV * 0.96f)
        }

        // Ball carrier (Away AI): weave around oncoming defenders instead of running straight into them
        if (ball.ownerId == player.id) {
            val goalX = if (teamAttacksRight) FIELD_RIGHT - 12f else FIELD_LEFT + 12f
            var targetY = when {
                player.y < CENTER_Y - 18f -> CENTER_Y - 10f
                player.y > CENTER_Y + 18f -> CENTER_Y + 10f
                else -> player.y
            }
            val blocker = opponents
                .filter { ((it.x - player.x) * shiftDir) in -2f..15f && abs(it.y - player.y) < 9.5f }
                .minByOrNull { hypot(it.x - player.x, it.y - player.y) }
            if (blocker != null) {
                val dodgeSign = if (player.y <= blocker.y) -1f else 1f
                targetY = (player.y + dodgeSign * 14f).coerceIn(FIELD_TOP + 12f, FIELD_BOTTOM - 12f)
            }
            return seek(player.x, player.y, goalX, targetY, maxV * 0.95f)
        }

        val teamHasBall = ball.ownerId?.startsWith(if (player.teamSide == TeamSide.HOME) "home_" else "away_") == true
        if (isChaser && !teamHasBall) {
            val pressUrgency = if (isLeadAwayChaser) {
                1.24f + (carrierIdleSeconds * 0.45f).coerceAtMost(0.95f)
            } else {
                1.08f
            }
            return seek(player.x, player.y, ball.x, ball.y, maxV * pressUrgency)
        }

        // Dynamic role-based off-ball movement (never stand frozen!)
        val code = player.slotCode.uppercase()
        val phaseSeed = (player.jerseyNumber * 1.37f + player.homeY * 0.09f)
        val organicX = sin(timeSec * 1.45f + phaseSeed)
        val organicY = cos(timeSec * 1.75f + phaseSeed)
        val ballFactorX = (ball.x - CENTER_X) * 0.42f
        val ballFactorY = (ball.y - CENTER_Y) * 0.28f
        val isLeftFlank = player.homeY < CENTER_Y
        val ballOnMyFlank = (isLeftFlank && ball.y < CENTER_Y - 10f) || (!isLeftFlank && ball.y > CENTER_Y + 10f)
        val carrierX = carrier?.x ?: ball.x

        var targetX: Float
        var targetY: Float
        var roleUrgency = 0.90f

        if (teamHasBall) {
            // ==================== IN POSSESSION (ATTACKING MOVEMENT BY ROLE) ====================
            when {
                // Center backs hold a steady progressive line & spread slightly for back-pass angles
                code == "CB" || code == "LCB" || code == "RCB" -> {
                    val pushLimit = if (teamAttacksRight) CENTER_X + 8f else CENTER_X - 8f
                    val rawX = player.homeX + shiftDir * 24f + ballFactorX * 0.55f
                    targetX = if (teamAttacksRight) rawX.coerceAtMost(pushLimit) else rawX.coerceAtLeast(pushLimit)
                    targetY = player.homeY + (ball.y - CENTER_Y) * 0.16f + organicY * 2.2f
                    roleUrgency = 0.80f
                }

                // Fullbacks & Wingbacks: overlap aggressively on ball-side flank, tuck in on weak side
                code == "LB" || code == "RB" || code == "LWB" || code == "RWB" -> {
                    val isWingBack = code.endsWith("WB")
                    if (ballOnMyFlank) {
                        // Overlapping wing run ahead of or parallel to ball
                        targetX = maxOf(player.homeX + shiftDir * 44f, carrierX + shiftDir * (if (isWingBack) 26f else 18f))
                            .let { if (teamAttacksRight) it else minOf(player.homeX + shiftDir * 44f, carrierX + shiftDir * (if (isWingBack) 26f else 18f)) }
                        val flankY = if (isLeftFlank) FIELD_TOP + 11f else FIELD_BOTTOM - 11f
                        targetY = flankY + organicY * 2.5f
                        roleUrgency = 0.96f
                    } else {
                        targetX = player.homeX + shiftDir * 30f + ballFactorX * 0.45f
                        targetY = (player.homeY * 0.78f + CENTER_Y * 0.22f) + (ball.y - CENTER_Y) * 0.18f + organicY * 2.2f
                        roleUrgency = 0.84f
                    }
                }

                // Holding Midfielders: orbit behind the ball as a safe central pivot in passing triangles
                code == "CDM" || code == "LDM" || code == "RDM" -> {
                    targetX = (carrierX - shiftDir * 18f + organicX * 3.5f)
                        .let { raw -> (raw * 0.55f + (player.homeX + shiftDir * 28f + ballFactorX * 0.4f) * 0.45f) }
                    targetY = player.homeY + (ball.y - player.homeY) * 0.34f + organicY * 4.5f
                    roleUrgency = 0.88f
                }

                // Central & Attacking Midfielders: move into open half-spaces / passing lanes ahead of the ball
                player.role == PlayerRole.MID && !code.startsWith("L") && !code.startsWith("R") -> {
                    val pushAhead = if (code == "CAM") 28f else 16f
                    targetX = (carrierX + shiftDir * pushAhead + organicX * 5.5f)
                        .let { (it * 0.50f + (player.homeX + shiftDir * 42f + ballFactorX * 0.5f) * 0.50f) }
                    targetY = player.homeY + (ball.y - CENTER_Y) * 0.30f + organicY * 7.5f
                    roleUrgency = 0.93f
                }

                // Wide Midfielders / Wingers (LM, RM, LAM, RAM, LW, RW): stretch touchline or cut inside to far post
                code == "LM" || code == "RM" || code == "LAM" || code == "RAM" || code == "LW" || code == "RW" -> {
                    val wideBaseY = if (isLeftFlank) FIELD_TOP + 13f else FIELD_BOTTOM - 13f
                    val inFinalThird = abs(oppGoalX - carrierX) < 72f
                    if (ballOnMyFlank) {
                        // Burst forward down channel or diagonally inward near the box
                        targetX = carrierX + shiftDir * 26f + organicX * 4f
                        targetY = if (inFinalThird) {
                            (wideBaseY * 0.65f + CENTER_Y * 0.35f) + organicY * 3.5f
                        } else {
                            wideBaseY + organicY * 3.0f
                        }
                    } else if (inFinalThird) {
                        // Weak-side winger attacks the back post!
                        targetX = oppGoalX - shiftDir * 20f + organicX * 4f
                        targetY = (if (isLeftFlank) GOAL_TOP - 4f else GOAL_BOTTOM + 4f) + organicY * 3.5f
                    } else {
                        // Hold wide width to stretch defense and invite switch of play
                        targetX = maxOf(player.homeX + shiftDir * 44f, carrierX + shiftDir * 14f)
                            .let { if (teamAttacksRight) it else minOf(player.homeX + shiftDir * 44f, carrierX + shiftDir * 14f) }
                        targetY = wideBaseY + organicY * 4.5f
                    }
                    roleUrgency = 0.98f
                }

                // Strikers (ST, LST, RST): time penetrating runs off the defensive line + check into pockets
                else -> {
                    val oppDefX = opponents
                        .filter { it.role == PlayerRole.DEF }
                        .let { defs ->
                            if (defs.isEmpty()) oppGoalX - shiftDir * 46f
                            else if (teamAttacksRight) defs.maxOf { it.x } else defs.minOf { it.x }
                        }
                    val sprintInBehind = sin(timeSec * 1.15f + phaseSeed) > -0.15f
                    val shoulderOffset = if (sprintInBehind) shiftDir * 9f else -shiftDir * 6f
                    val desiredLineX = oppDefX + shoulderOffset + organicX * 4f
                    val deepMinX = carrierX + shiftDir * 18f
                    targetX = if (teamAttacksRight) {
                        maxOf(desiredLineX, deepMinX).coerceAtMost(oppGoalX - 10f)
                    } else {
                        minOf(desiredLineX, deepMinX).coerceAtLeast(oppGoalX + 10f)
                    }
                    // Drift into open channel away from closest center back
                    val nearestMarker = opponents.minByOrNull { hypot(it.x - player.x, it.y - player.y) }
                    val evadeY = if (nearestMarker != null && hypot(nearestMarker.x - player.x, nearestMarker.y - player.y) < 12f) {
                        if (player.y <= nearestMarker.y) -7f else 7f
                    } else 0f
                    targetY = (player.homeY + (ball.y - CENTER_Y) * 0.22f + evadeY + organicY * 6.5f)
                    roleUrgency = 1.00f
                }
            }
        } else {
            // ==================== OUT OF POSSESSION (DEFENSIVE SHAPE & TRACKING BY ROLE) ====================
            when {
                // Center backs maintain a compact central backline & mark attackers threatening the box
                code == "CB" || code == "LCB" || code == "RCB" -> {
                    val goalGuardX = ownGoalX + shiftDir * 30f
                    val rawLineX = player.homeX - shiftDir * 16f + ballFactorX * 0.55f
                    targetX = if (teamAttacksRight) rawLineX.coerceAtLeast(goalGuardX) else rawLineX.coerceAtMost(goalGuardX)
                    val ballPullY = (ball.y - CENTER_Y) * 0.34f
                    val dangerousAttacker = opponents.firstOrNull { opp ->
                        abs(opp.x - targetX) < 16f && abs(opp.y - (player.homeY + ballPullY)) < 14f
                    }
                    targetY = if (dangerousAttacker != null) {
                        (dangerousAttacker.y * 0.55f + (player.homeY + ballPullY) * 0.45f) + organicY * 1.4f
                    } else {
                        (player.homeY * 0.88f + CENTER_Y * 0.12f) + ballPullY + organicY * 1.8f
                    }
                    roleUrgency = 0.88f
                }

                // Fullbacks tuck inside defensively & track wide threats
                code == "LB" || code == "RB" || code == "LWB" || code == "RWB" -> {
                    targetX = player.homeX - shiftDir * 18f + ballFactorX * 0.58f
                    val compactY = (player.homeY * 0.72f + CENTER_Y * 0.28f) + (ball.y - CENTER_Y) * 0.30f
                    targetY = compactY + organicY * 2.0f
                    roleUrgency = 0.89f
                }

                // Midfielders screen central Zone 14 & shift horizontally toward ball side
                player.role == PlayerRole.MID -> {
                    val screenDepth = if (code.contains("DM")) -shiftDir * 24f else -shiftDir * 14f
                    targetX = player.homeX + screenDepth + ballFactorX * 0.55f + organicX * 2.8f
                    val compactWidth = if (code.startsWith("L") || code.startsWith("R")) 0.80f else 0.90f
                    targetY = (player.homeY * compactWidth + CENTER_Y * (1f - compactWidth)) +
                        (ball.y - CENTER_Y) * 0.38f + organicY * 3.0f
                    roleUrgency = 0.90f
                }

                // Forwards hold halfway outlet / shadow opponent build-up pivot for counter-attacks
                else -> {
                    val midLineBias = CENTER_X + shiftDir * 12f
                    targetX = ((player.homeX - shiftDir * 22f) * 0.55f + midLineBias * 0.45f) +
                        ballFactorX * 0.40f + organicX * 4.2f
                    targetY = player.homeY + (ball.y - CENTER_Y) * 0.26f + organicY * 4.5f
                    roleUrgency = 0.82f
                }
            }
        }

        val clampedX = targetX.coerceIn(FIELD_LEFT + 8f, FIELD_RIGHT - 8f)
        val clampedY = targetY.coerceIn(FIELD_TOP + 7f, FIELD_BOTTOM - 7f)
        val dx = clampedX - player.x
        val dy = clampedY - player.y
        val dist = hypot(dx, dy)
        if (dist < 1.15f) {
            // Subtle athletic micro-jockey so off-ball players stay responsive on their toes instead of freezing
            val microX = cos(timeSec * 3.1f + phaseSeed) * (maxV * 0.14f)
            val microY = sin(timeSec * 2.6f + phaseSeed) * (maxV * 0.14f)
            return microX to microY
        }
        return seek(player.x, player.y, clampedX, clampedY, maxV * roleUrgency)
    }

    private enum class AiBallDecision { DRIBBLE, PASS, SHOOT }

    private fun evaluateAwayBallAction(
        carrier: PlayerEntity,
        players: List<PlayerEntity>,
        difficulty: Difficulty,
        awayTeam: Team,
        awayAttacksRight: Boolean
    ): AiBallDecision {
        val distToGoal = if (awayAttacksRight) FIELD_RIGHT - carrier.x else carrier.x - FIELD_LEFT
        if (distToGoal < 56f && abs(carrier.y - CENTER_Y) < 38f) {
            val shootChance = (0.052f + (awayTeam.attack - 80) * 0.002f) * difficulty.aiReactionScale
            if (rng.nextFloat() < shootChance) return AiBallDecision.SHOOT
        }
        val attackDir = if (awayAttacksRight) 1f else -1f
        val pressured = players.any {
            it.teamSide == TeamSide.HOME && hypot(it.x - carrier.x, it.y - carrier.y) < 12.5f
        }
        if (pressured && rng.nextFloat() < 0.095f * difficulty.aiReactionScale) {
            return AiBallDecision.PASS
        }
        // Proactive progressive passing when a forward or winger makes an open run ahead
        val hasOpenForwardRunner = players.any { mate ->
            mate.teamSide == TeamSide.AWAY &&
                mate.id != carrier.id &&
                mate.role != PlayerRole.GK &&
                (mate.x - carrier.x) * attackDir > 18f &&
                players.none { opp -> opp.teamSide == TeamSide.HOME && hypot(opp.x - mate.x, opp.y - mate.y) < 10f }
        }
        if (hasOpenForwardRunner && rng.nextFloat() < 0.036f * difficulty.aiReactionScale) {
            return AiBallDecision.PASS
        }
        return AiBallDecision.DRIBBLE
    }

    private fun seek(x: Float, y: Float, tx: Float, ty: Float, maxSpeed: Float): Pair<Float, Float> {
        val dx = tx - x
        val dy = ty - y
        val dist = hypot(dx, dy)
        if (dist < 0.9f) return 0f to 0f
        val factor = (dist / 3.5f).coerceIn(0.25f, 1f)
        return (dx / dist) * maxSpeed * factor to (dy / dist) * maxSpeed * factor
    }

    private fun updateParticles(particles: List<PitchParticle>, dt: Float): List<PitchParticle> {
        if (particles.isEmpty()) return emptyList()
        return particles.mapNotNull { pt ->
            val nextLife = pt.life - dt
            if (nextLife <= 0f) null
            else pt.copy(
                x = pt.x + pt.vx * dt,
                y = pt.y + pt.vy * dt,
                vx = pt.vx * (1f - 2.2f * dt),
                vy = pt.vy * (1f - 2.2f * dt),
                life = nextLife
            )
        }
    }

    private fun spawnDust(existing: List<PitchParticle>, x: Float, y: Float, color: Color, count: Int): List<PitchParticle> {
        val added = List(count) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val spd = 4f + rng.nextFloat() * 12f
            PitchParticle(
                x = x,
                y = y,
                vx = cos(angle) * spd,
                vy = sin(angle) * spd,
                size = 0.5f + rng.nextFloat() * 0.6f,
                color = color,
                life = 0.35f + rng.nextFloat() * 0.25f,
                maxLife = 0.6f
            )
        }
        return (existing + added).takeLast(90)
    }

    private fun spawnGoalConfetti(
        existing: List<PitchParticle>,
        x: Float,
        y: Float,
        team: Team,
        count: Int
    ): List<PitchParticle> {
        if (count <= 0) return existing
        val palette = listOf(team.primaryColor, team.secondaryColor, team.accentColor, Color(0xFFFFD700), Color.White)
        val added = List(count) {
            val angle = rng.nextFloat() * 2f * PI.toFloat()
            val spd = 6f + rng.nextFloat() * 28f
            PitchParticle(
                x = x + (rng.nextFloat() - 0.5f) * 6f,
                y = y + (rng.nextFloat() - 0.5f) * 16f,
                vx = cos(angle) * spd,
                vy = sin(angle) * spd,
                size = 0.7f + rng.nextFloat() * 0.9f,
                color = palette[it % palette.size],
                life = 1.1f + rng.nextFloat() * 0.8f,
                maxLife = 1.9f
            )
        }
        return (existing + added).takeLast(120)
    }
}
