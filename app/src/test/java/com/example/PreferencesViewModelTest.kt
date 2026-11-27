package com.example

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.data.SettingsRepository
import com.example.model.CameraDistance
import com.example.model.Difficulty
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.ui.PreferencesUiState
import com.example.ui.PreferencesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createRepository(scope: TestScope, fileName: String = "prefs_test.preferences_pb"): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = { tmpFolder.newFile(fileName) }
        )
        return SettingsRepository(dataStore)
    }

    @Test
    fun initialSettingsEmitDefaultsAndSuccessUiState() = runTest(testDispatcher) {
        val repository = createRepository(this)
        val viewModel = PreferencesViewModel(repository)

        val current = viewModel.settings.value
        assertEquals(0.75f, current.volume, 0.001f)
        assertFalse(current.isMuted)
        assertEquals(GraphicsQuality.HIGH, current.graphicsQuality)
        assertEquals(Difficulty.MEDIUM, current.difficulty)
        assertEquals(CameraDistance.MEDIUM, current.cameraDistance)

        val state = viewModel.uiState.value
        assertTrue(state is PreferencesUiState.Success)
        assertEquals(current, (state as PreferencesUiState.Success).settings)
    }

    @Test
    fun volumeUpdatesClampMuteAndStepProperly() = runTest(testDispatcher) {
        val repository = createRepository(this)
        val viewModel = PreferencesViewModel(repository)

        viewModel.setVolume(1.5f)
        assertEquals(1f, viewModel.settings.value.volume, 0.001f)
        assertFalse(viewModel.settings.value.isMuted)

        viewModel.decreaseVolume()
        assertEquals(0.9f, viewModel.settings.value.volume, 0.001f)

        viewModel.toggleMute()
        assertTrue(viewModel.settings.value.isMuted)
        assertEquals(0f, viewModel.settings.value.effectiveVolume, 0.001f)

        viewModel.increaseVolume()
        assertFalse(viewModel.settings.value.isMuted)
        assertEquals(0.1f, viewModel.settings.value.volume, 0.001f)
    }

    @Test
    fun graphicsQualityAndDifficultyPersistAcrossRepositoryReads() = runTest(testDispatcher) {
        val prefsFile = tmpFolder.newFile("persist_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { prefsFile }
        )
        val repo1 = SettingsRepository(dataStore)
        val vm1 = PreferencesViewModel(repo1)

        vm1.setGraphicsQuality(GraphicsQuality.LOW)
        vm1.setDifficulty(Difficulty.HARD)
        vm1.setCameraDistance(CameraDistance.FAR)
        vm1.setVolume(0.45f)

        val repo2 = SettingsRepository(dataStore)
        val persisted: GameSettings = repo2.settingsFlow.first()
        assertEquals(GraphicsQuality.LOW, persisted.graphicsQuality)
        assertEquals(Difficulty.HARD, persisted.difficulty)
        assertEquals(CameraDistance.FAR, persisted.cameraDistance)
        assertEquals(0.45f, persisted.volume, 0.001f)
    }

    @Test
    fun resetPreferencesRestoresDefaults() = runTest(testDispatcher) {
        val repository = createRepository(this)
        val viewModel = PreferencesViewModel(repository)

        viewModel.setVolume(0.2f)
        viewModel.toggleMute()
        viewModel.setGraphicsQuality(GraphicsQuality.LOW)
        viewModel.setDifficulty(Difficulty.EASY)
        viewModel.setCameraDistance(CameraDistance.CLOSE)

        viewModel.resetPreferences()

        val restored = viewModel.settings.value
        assertEquals(0.75f, restored.volume, 0.001f)
        assertFalse(restored.isMuted)
        assertEquals(GraphicsQuality.HIGH, restored.graphicsQuality)
        assertEquals(Difficulty.MEDIUM, restored.difficulty)
        assertEquals(CameraDistance.MEDIUM, restored.cameraDistance)
    }

    @Test
    fun matchDurationAndPassIndicatorUpdateInRealTime() = runTest(testDispatcher) {
        val repository = createRepository(this, "duration_test.preferences_pb")
        val viewModel = PreferencesViewModel(repository)

        viewModel.setMatchDuration(150)
        viewModel.setShowPassIndicator(false)
        viewModel.setCommentaryEnabled(false)
        viewModel.setGraphicsQuality(GraphicsQuality.ULTRA)

        assertEquals(150, viewModel.settings.value.matchDurationSeconds)
        assertFalse(viewModel.settings.value.showPassIndicator)
        assertFalse(viewModel.settings.value.isCommentaryEnabled)
        assertEquals(GraphicsQuality.ULTRA, viewModel.settings.value.graphicsQuality)

        viewModel.resetPreferences()
        assertTrue(viewModel.settings.value.isCommentaryEnabled)
        assertEquals(GraphicsQuality.HIGH, viewModel.settings.value.graphicsQuality)
    }
}
