package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SettingsRepository
import com.example.model.CameraDistance
import com.example.model.Difficulty
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PreferencesUiState {
    data object Loading : PreferencesUiState
    data class Success(val settings: GameSettings) : PreferencesUiState
    data class Error(val message: String) : PreferencesUiState
}

class PreferencesViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val settings: StateFlow<GameSettings> = repository.settingsFlow
        .catch { e ->
            Log.e(TAG, "Failed to read preferences from DataStore", e)
            _errorMessage.value = e.localizedMessage ?: "تعذر قراءة الإعدادات المحفوظة."
            emit(GameSettings())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = GameSettings()
        )

    val uiState: StateFlow<PreferencesUiState> = repository.settingsFlow
        .map<GameSettings, PreferencesUiState> { PreferencesUiState.Success(it) }
        .catch { e ->
            Log.e(TAG, "Preferences stream failed", e)
            emit(PreferencesUiState.Error(e.localizedMessage ?: "تعذر تحميل الإعدادات."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PreferencesUiState.Loading
        )

    fun setVolume(volume: Float) {
        updatePreference {
            repository.setVolume(volume.coerceIn(0f, 1f))
        }
    }

    fun increaseVolume() {
        val current = settings.value
        val base = if (current.isMuted) 0f else current.volume
        val next = (base + VOLUME_STEP).coerceAtMost(1f)
        updatePreference {
            repository.setVolume(next)
        }
    }

    fun decreaseVolume() {
        val current = settings.value
        val next = (current.volume - VOLUME_STEP).coerceAtLeast(0f)
        updatePreference {
            repository.setVolume(next)
            if (next <= 0.001f) {
                repository.setMuted(true)
            }
        }
    }

    fun toggleMute() {
        val nextMuted = !settings.value.isMuted
        updatePreference {
            repository.setMuted(nextMuted)
        }
    }

    fun setCommentaryEnabled(enabled: Boolean) {
        updatePreference {
            repository.setCommentaryEnabled(enabled)
        }
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        updatePreference {
            repository.setGraphicsQuality(quality)
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        updatePreference {
            repository.setDifficulty(difficulty)
        }
    }

    fun setCameraDistance(cameraDistance: CameraDistance) {
        updatePreference {
            repository.setCameraDistance(cameraDistance)
        }
    }

    fun setMatchDuration(seconds: Int) {
        updatePreference {
            repository.setMatchDuration(seconds)
        }
    }

    fun setShowPassIndicator(show: Boolean) {
        updatePreference {
            repository.setShowPassIndicator(show)
        }
    }

    fun resetPreferences() {
        updatePreference {
            repository.resetUserPreferences()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun updatePreference(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save preference to DataStore", e)
                _errorMessage.value = e.localizedMessage ?: "تعذر حفظ الإعدادات."
            }
        }
    }

    companion object {
        private const val TAG = "PreferencesViewModel"
        private const val VOLUME_STEP = 0.1f

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                PreferencesViewModel(SettingsRepository(app.applicationContext))
            }
        }
    }
}
