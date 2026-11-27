package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CameraDistance
import com.example.model.CareerStats
import com.example.model.Difficulty
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kick_champions_prefs")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.dataStore)

    private object Keys {
        val VOLUME = floatPreferencesKey("volume")
        val IS_MUTED = booleanPreferencesKey("is_muted")
        val IS_COMMENTARY_ENABLED = booleanPreferencesKey("is_commentary_enabled")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val CAMERA_DISTANCE = stringPreferencesKey("camera_distance")
        val MATCH_DURATION = intPreferencesKey("match_duration")
        val HOME_TEAM_ID = stringPreferencesKey("home_team_id")
        val AWAY_TEAM_ID = stringPreferencesKey("away_team_id")
        val SHOW_PASS_INDICATOR = booleanPreferencesKey("show_pass_indicator")
        val MATCHES_PLAYED = intPreferencesKey("matches_played")
        val WINS = intPreferencesKey("wins")
        val TOTAL_GOALS = intPreferencesKey("total_goals")
    }

    private val safePreferencesFlow: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    val settingsFlow: Flow<GameSettings> = safePreferencesFlow.map { prefs ->
        val graphics = prefs[Keys.GRAPHICS_QUALITY]
            ?.let { name -> GraphicsQuality.entries.firstOrNull { it.name == name } }
            ?: GraphicsQuality.HIGH
        val difficulty = prefs[Keys.DIFFICULTY]
            ?.let { name -> Difficulty.entries.firstOrNull { it.name == name } }
            ?: Difficulty.MEDIUM
        val cameraDistance = prefs[Keys.CAMERA_DISTANCE]
            ?.let { name -> CameraDistance.entries.firstOrNull { it.name == name } }
            ?: CameraDistance.MEDIUM

        GameSettings(
            volume = (prefs[Keys.VOLUME] ?: 0.75f).coerceIn(0f, 1f),
            isMuted = prefs[Keys.IS_MUTED] ?: false,
            isCommentaryEnabled = prefs[Keys.IS_COMMENTARY_ENABLED] ?: true,
            graphicsQuality = graphics,
            difficulty = difficulty,
            cameraDistance = cameraDistance,
            matchDurationSeconds = prefs[Keys.MATCH_DURATION] ?: 90,
            homeTeamId = prefs[Keys.HOME_TEAM_ID] ?: "cairo_eagles",
            awayTeamId = prefs[Keys.AWAY_TEAM_ID] ?: "madrid_royals",
            showPassIndicator = prefs[Keys.SHOW_PASS_INDICATOR] ?: true
        )
    }

    val careerStatsFlow: Flow<CareerStats> = safePreferencesFlow.map { prefs ->
        CareerStats(
            matchesPlayed = prefs[Keys.MATCHES_PLAYED] ?: 0,
            wins = prefs[Keys.WINS] ?: 0,
            totalGoals = prefs[Keys.TOTAL_GOALS] ?: 0
        )
    }

    suspend fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        dataStore.edit { prefs ->
            prefs[Keys.VOLUME] = clamped
            if (clamped > 0f) {
                prefs[Keys.IS_MUTED] = false
            }
        }
    }

    suspend fun setMuted(muted: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_MUTED] = muted
        }
    }

    suspend fun setCommentaryEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_COMMENTARY_ENABLED] = enabled
        }
    }

    suspend fun setGraphicsQuality(quality: GraphicsQuality) {
        dataStore.edit { prefs ->
            prefs[Keys.GRAPHICS_QUALITY] = quality.name
        }
    }

    suspend fun setDifficulty(difficulty: Difficulty) {
        dataStore.edit { prefs ->
            prefs[Keys.DIFFICULTY] = difficulty.name
        }
    }

    suspend fun setCameraDistance(cameraDistance: CameraDistance) {
        dataStore.edit { prefs ->
            prefs[Keys.CAMERA_DISTANCE] = cameraDistance.name
        }
    }

    suspend fun setMatchDuration(seconds: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.MATCH_DURATION] = seconds.coerceIn(30, 600)
        }
    }

    suspend fun setTeams(homeId: String, awayId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.HOME_TEAM_ID] = homeId
            prefs[Keys.AWAY_TEAM_ID] = awayId
        }
    }

    suspend fun setShowPassIndicator(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_PASS_INDICATOR] = show
        }
    }

    suspend fun resetUserPreferences() {
        val defaults = GameSettings()
        dataStore.edit { prefs ->
            prefs[Keys.VOLUME] = defaults.volume
            prefs[Keys.IS_MUTED] = defaults.isMuted
            prefs[Keys.IS_COMMENTARY_ENABLED] = defaults.isCommentaryEnabled
            prefs[Keys.GRAPHICS_QUALITY] = defaults.graphicsQuality.name
            prefs[Keys.DIFFICULTY] = defaults.difficulty.name
            prefs[Keys.CAMERA_DISTANCE] = defaults.cameraDistance.name
            prefs[Keys.MATCH_DURATION] = defaults.matchDurationSeconds
            prefs[Keys.SHOW_PASS_INDICATOR] = defaults.showPassIndicator
        }
    }

    suspend fun recordMatchResult(homeGoals: Int, awayGoals: Int) {
        dataStore.edit { prefs ->
            val currentMatches = prefs[Keys.MATCHES_PLAYED] ?: 0
            val currentWins = prefs[Keys.WINS] ?: 0
            val currentGoals = prefs[Keys.TOTAL_GOALS] ?: 0
            prefs[Keys.MATCHES_PLAYED] = currentMatches + 1
            prefs[Keys.WINS] = currentWins + if (homeGoals > awayGoals) 1 else 0
            prefs[Keys.TOTAL_GOALS] = currentGoals + homeGoals
        }
    }
}
