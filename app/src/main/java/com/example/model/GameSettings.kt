package com.example.model

enum class GraphicsQuality(val labelAr: String, val levelIndex: Int) {
    LOW("منخفض", 0),
    MEDIUM("متوسط", 1),
    HIGH("فائق", 2)
}

enum class Difficulty(val labelAr: String, val aiReactionScale: Float, val aiSpeedScale: Float) {
    EASY("سهل", 0.65f, 0.80f),
    MEDIUM("متوسط", 0.88f, 0.94f),
    HARD("محترف", 1.12f, 1.06f)
}

data class GameSettings(
    val volume: Float = 0.75f,
    val isMuted: Boolean = false,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val matchDurationSeconds: Int = 90,
    val homeTeamId: String = "cairo_eagles",
    val awayTeamId: String = "madrid_royals",
    val showPassIndicator: Boolean = true
) {
    val effectiveVolume: Float get() = if (isMuted) 0f else volume.coerceIn(0f, 1f)
}

data class CareerStats(
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val totalGoals: Int = 0
)
