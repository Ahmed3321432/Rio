package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CareerStats
import com.example.model.Difficulty
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kick_champions_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val VOLUME = floatPreferencesKey("volume")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val MATCH_DURATION = intPreferencesKey("match_duration")
        val HOME_TEAM_ID = stringPreferencesKey("home_team_id")
        val AWAY_TEAM_ID = stringPreferencesKey("away_team_id")
        val SHOW_PASS_INDICATOR = booleanPreferencesKey("show_pass_indicator")
        val MATCHES_PLAYED = intPreferencesKey("matches_played")
        val WINS = intPreferencesKey("wins")
        val TOTAL_GOALS = intPreferencesKey("total_goals")
    }

    val settingsFlow: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        val graphics = prefs[Keys.GRAPHICS_QUALITY]
            ?.let { name -> GraphicsQuality.entries.firstOrNull { it.name == name } }
            ?: GraphicsQuality.HIGH
        val difficulty = prefs[Keys.DIFFICULTY]
            ?.let { name -> Difficulty.entries.firstOrNull { it.name == name } }
            ?: Difficulty.MEDIUM

        GameSettings(
            volume = (prefs[Keys.VOLUME] ?: 0.75f).coerceIn(0f, 1f),
            isMuted = prefs[Keys.IS_MUTED] ?: false,
            graphicsQuality = graphics,
            difficulty = difficulty,
            matchDurationSeconds = prefs[Keys.MATCH_DURATION] ?: 90,
            homeTeamId = prefs[Keys.HOME_TEAM_ID] ?: "cairo_eagles",
            awayTeamId = prefs[Keys.AWAY_TEAM_ID] ?: "madrid_royals",
            showPassIndicator = prefs[Keys.SHOW_PASS_INDICATOR] ?: true
        )
    }

    val careerStatsFlow: Flow<CareerStats> = context.dataStore.data.map { prefs ->
        CareerStats(
            matchesPlayed = prefs[Keys.MATCHES_PLAYED] ?: 0,
            wins = prefs[Keys.WINS] ?: 0,
            totalGoals = prefs[Keys.TOTAL_GOALS] ?: 0
        )
    }

    suspend fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        context.dataStore.edit { prefs ->
            prefs[Keys.VOLUME] = clamped
            if (clamped > 0f) {
                prefs[Keys.IS_MUTED] = false
            }
        }
    }

    suspend fun setMuted(muted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_MUTED] = muted
        }
    }

    suspend fun setGraphicsQuality(quality: GraphicsQuality) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GRAPHICS_QUALITY] = quality.name
        }
    }

    suspend fun setDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DIFFICULTY] = difficulty.name
        }
    }

    suspend fun setMatchDuration(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MATCH_DURATION] = seconds
        }
    }

    suspend fun setTeams(homeId: String, awayId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HOME_TEAM_ID] = homeId
            prefs[Keys.AWAY_TEAM_ID] = awayId
        }
    }

    suspend fun setShowPassIndicator(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_PASS_INDICATOR] = show
        }
    }

    suspend fun recordMatchResult(homeGoals: Int, awayGoals: Int) {
        context.dataStore.edit { prefs ->
            val currentMatches = prefs[Keys.MATCHES_PLAYED] ?: 0
            val currentWins = prefs[Keys.WINS] ?: 0
            val currentGoals = prefs[Keys.TOTAL_GOALS] ?: 0
            prefs[Keys.MATCHES_PLAYED] = currentMatches + 1
            prefs[Keys.WINS] = currentWins + if (homeGoals > awayGoals) 1 else 0
            prefs[Keys.TOTAL_GOALS] = currentGoals + homeGoals
        }
    }
}
