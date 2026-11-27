package com.example.model

import androidx.compose.ui.graphics.Color

enum class TeamSide {
    HOME,
    AWAY
}

enum class PlayerRole {
    GK,
    DEF,
    MID,
    FWD
}

enum class MatchPhase {
    PRE_KICKOFF,
    PLAYING,
    SET_PIECE,
    GOAL_CELEBRATION,
    HALF_TIME,
    PAUSED,
    FULL_TIME
}

enum class SetPieceType {
    NONE,
    THROW_IN,
    GOAL_KICK,
    CORNER,
    FREE_KICK,
    PENALTY
}

data class PlayerEntity(
    val id: String,
    val squadId: String = "",
    val teamSide: TeamSide,
    val role: PlayerRole,
    val slotCode: String = "",
    val jerseyNumber: Int,
    val name: String,
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val facingAngle: Float = 0f,
    val stamina: Float = 1f,
    val homeX: Float,
    val homeY: Float,
    val rating: Int = 85,
    val paceRating: Int = 85,
    val shootingRating: Int = 85,
    val skinToneIndex: Int = 0,
    val hairStyleIndex: Int = 0,
    val stunTimer: Float = 0f,
    val tackleCooldown: Float = 0f,
    val slideTimer: Float = 0f,
    val kickAnimTimer: Float = 0f,
    val stridePhase: Float = 0f
)

data class BallEntity(
    val x: Float = 135f,
    val y: Float = 90f,
    val z: Float = 0f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val vz: Float = 0f,
    val spinAngle: Float = 0f,
    val ownerId: String? = null,
    val lastTouchTeam: TeamSide? = null,
    val lastTouchPlayerName: String = "",
    val trail: List<Pair<Float, Float>> = emptyList()
)

data class PitchParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val life: Float,
    val maxLife: Float
)

data class MatchStats(
    val homeShots: Int = 0,
    val awayShots: Int = 0,
    val homeShotsOnTarget: Int = 0,
    val awayShotsOnTarget: Int = 0,
    val homeTackles: Int = 0,
    val awayTackles: Int = 0,
    val homeFouls: Int = 0,
    val awayFouls: Int = 0,
    val homeCorners: Int = 0,
    val awayCorners: Int = 0,
    val homePossessionTicks: Int = 1,
    val awayPossessionTicks: Int = 1
) {
    val homePossessionPercent: Int
        get() {
            val total = (homePossessionTicks + awayPossessionTicks).coerceAtLeast(1)
            return ((homePossessionTicks * 100f) / total).toInt().coerceIn(5, 95)
        }
    val awayPossessionPercent: Int get() = 100 - homePossessionPercent
}

data class MatchState(
    val homeTeam: Team = TeamCatalog.teams[0],
    val awayTeam: Team = TeamCatalog.teams[1],
    val homeFormation: FormationType = FormationType.F_4_3_3,
    val homeBench: List<SquadPlayer> = TeamCatalog.teams[0].squad.drop(11),
    val subsUsed: Int = 0,
    val maxSubs: Int = 5,
    val phase: MatchPhase = MatchPhase.PRE_KICKOFF,
    val half: Int = 1,
    val remainingSeconds: Float = 90f,
    val totalDurationSeconds: Int = 90,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val players: List<PlayerEntity> = emptyList(),
    val ball: BallEntity = BallEntity(),
    val activeHomePlayerId: String = "home_9",
    val passTargetHomeId: String? = null,
    val passInFlightTargetId: String? = null,
    val passInFlightTimer: Float = 0f,
    val shotCharge: Float = 0f,
    val isChargingShot: Boolean = false,
    val passCharge: Float = 0f,
    val isChargingPass: Boolean = false,
    val cameraX: Float = 135f,
    val cameraY: Float = 90f,
    val setPieceType: SetPieceType = SetPieceType.NONE,
    val setPieceTeam: TeamSide = TeamSide.HOME,
    val setPieceTimer: Float = 0f,
    val setPieceX: Float = 135f,
    val setPieceY: Float = 90f,
    val carrierIdleSeconds: Float = 0f,
    val lastBallOwnerId: String? = null,
    val lastScorerTeam: TeamSide? = null,
    val lastScorerName: String = "",
    val celebrationTimer: Float = 0f,
    val preKickoffTimer: Float = 1.2f,
    val netRippleLeft: Float = 0f,
    val netRippleRight: Float = 0f,
    val cameraShake: Float = 0f,
    val bannerMessage: String? = null,
    val bannerTimer: Float = 0f,
    val particles: List<PitchParticle> = emptyList(),
    val stats: MatchStats = MatchStats()
) {
    val homeHasBall: Boolean
        get() = ball.ownerId?.startsWith("home_") == true

    val homeAttacksRight: Boolean
        get() = half == 1

    val activeHomePlayer: PlayerEntity?
        get() = players.firstOrNull { it.id == activeHomePlayerId }

    val displayMatchMinute: Int
        get() {
            val total = totalDurationSeconds.coerceAtLeast(2)
            val elapsed = (total - remainingSeconds).coerceIn(0f, total.toFloat())
            val halfDur = total / 2f
            return if (half == 1) {
                ((elapsed / halfDur) * 45f).toInt().coerceIn(0, 45)
            } else {
                val secondElapsed = (elapsed - halfDur).coerceAtLeast(0f)
                (45 + ((secondElapsed / halfDur) * 45f).toInt()).coerceIn(45, 90)
            }
        }
}
