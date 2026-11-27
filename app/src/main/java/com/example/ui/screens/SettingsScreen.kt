package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CameraDistance
import com.example.model.Difficulty
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.ui.PreferencesUiState
import com.example.ui.PreferencesViewModel
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfEmerald
import kotlin.math.roundToInt

private enum class SettingsCategory(
    val titleAr: String,
    val subtitleAr: String,
    val icon: ImageVector,
    val accent: Color
) {
    ALL("كل الإعدادات", "عرض شامل ومنظم", Icons.Default.Tune, TurfEmerald),
    AUDIO("الصوت والتعليق", "مستوى الصوت والمعلق", Icons.AutoMirrored.Filled.VolumeUp, TurfEmerald),
    VISUALS("الجرافيكس والكاميرا", "الدقة وزاوية الملعب", Icons.Default.Hd, ChampionshipGold),
    GAMEPLAY("اللعب والمباراة", "الصعوبة والوقت والمساعد", Icons.Default.Speed, ElectricCyan)
}

@Composable
fun SettingsScreen(
    preferencesViewModel: PreferencesViewModel,
    onTestCommentary: () -> Unit = {},
    onBack: () -> Unit
) {
    val uiState by preferencesViewModel.uiState.collectAsStateWithLifecycle()
    val settings by preferencesViewModel.settings.collectAsStateWithLifecycle()
    val errorMessage by preferencesViewModel.errorMessage.collectAsStateWithLifecycle()

    SettingsScreenContent(
        uiState = uiState,
        settings = settings,
        errorMessage = errorMessage,
        onVolumeChange = preferencesViewModel::setVolume,
        onVolumeUp = preferencesViewModel::increaseVolume,
        onVolumeDown = preferencesViewModel::decreaseVolume,
        onToggleMute = preferencesViewModel::toggleMute,
        onCommentaryEnabledChange = preferencesViewModel::setCommentaryEnabled,
        onTestCommentary = onTestCommentary,
        onGraphicsSelected = preferencesViewModel::setGraphicsQuality,
        onDifficultySelected = preferencesViewModel::setDifficulty,
        onCameraDistanceSelected = preferencesViewModel::setCameraDistance,
        onMatchDurationSelected = preferencesViewModel::setMatchDuration,
        onShowPassIndicatorChange = preferencesViewModel::setShowPassIndicator,
        onResetPreferences = preferencesViewModel::resetPreferences,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: PreferencesUiState,
    settings: GameSettings,
    errorMessage: String?,
    onVolumeChange: (Float) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onCommentaryEnabledChange: (Boolean) -> Unit = {},
    onTestCommentary: () -> Unit = {},
    onGraphicsSelected: (GraphicsQuality) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onCameraDistanceSelected: (CameraDistance) -> Unit = {},
    onMatchDurationSelected: (Int) -> Unit,
    onShowPassIndicatorChange: (Boolean) -> Unit,
    onResetPreferences: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }

    var graphicsExpanded by remember { mutableStateOf(false) }
    var difficultyExpanded by remember { mutableStateOf(false) }
    var cameraExpanded by remember { mutableStateOf(false) }

    var isDraggingVolume by remember { mutableStateOf(false) }
    var sliderVolume by remember(settings.volume, settings.isMuted) {
        mutableFloatStateOf(if (settings.isMuted) 0f else settings.volume)
    }
    val displayedVolume = if (isDraggingVolume) sliderVolume else if (settings.isMuted) 0f else settings.volume

    var isDraggingDuration by remember { mutableStateOf(false) }
    var sliderDuration by remember(settings.matchDurationSeconds) {
        mutableFloatStateOf(settings.matchDurationSeconds.toFloat().coerceIn(60f, 180f))
    }
    val displayedDurationSec = if (isDraggingDuration) {
        ((sliderDuration / 30f).roundToInt() * 30).coerceIn(60, 180)
    } else {
        settings.matchDurationSeconds
    }

    Scaffold(
        containerColor = StadiumObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TurfEmerald.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = TurfEmerald,
                                modifier = Modifier.padding(6.dp).size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مركز الإعدادات",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "تحكم في الصوت، الجرافيكس، وأسلوب اللعب بشكل مستقل",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "العودة",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onResetPreferences,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .height(40.dp)
                            .testTag("settings_reset_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = ChampionshipGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "الافتراضي",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampionshipGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StadiumObsidian
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(StadiumObsidian, Color(0xFF0A141E), StadiumObsidian)
                    )
                )
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            val isWide = maxWidth >= 600.dp

            // Individual property cards defined cleanly so each setting has its own isolated home
            val volumeCard: @Composable (Modifier) -> Unit = { mod ->
                SettingStandaloneCard(
                    title = stringResource(R.string.volume_label),
                    subtitle = "مستوى صوت الصفارة والركل والجمهور",
                    icon = if (displayedVolume <= 0.01f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    accent = TurfEmerald,
                    badgeText = "${(displayedVolume * 100).roundToInt()}%",
                    modifier = mod
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(
                            onClick = onVolumeDown,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_volume_down"),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = StadiumElevated,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "توطية الصوت")
                        }

                        Slider(
                            value = displayedVolume,
                            onValueChange = { next ->
                                isDraggingVolume = true
                                sliderVolume = next
                                onVolumeChange(next)
                            },
                            onValueChangeFinished = {
                                isDraggingVolume = false
                                onVolumeChange(sliderVolume)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_volume_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = TurfEmerald,
                                activeTrackColor = TurfEmerald,
                                inactiveTrackColor = StadiumElevated
                            )
                        )

                        FilledIconButton(
                            onClick = onVolumeUp,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_volume_up"),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = StadiumElevated,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "تعلية الصوت")
                        }
                    }

                    Surface(
                        onClick = onToggleMute,
                        shape = RoundedCornerShape(12.dp),
                        color = StadiumElevated,
                        border = BorderStroke(1.dp, if (settings.isMuted) CrimsonAlert.copy(alpha = 0.55f) else DividerSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.isMuted) "الصوت مكتوم حالياً" else "تفعيل المؤثرات الصوتية",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (settings.isMuted) CrimsonAlert else MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = !settings.isMuted,
                                onCheckedChange = { onToggleMute() },
                                modifier = Modifier.testTag("settings_mute_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF042111),
                                    checkedTrackColor = TurfEmerald,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = StadiumCard
                                )
                            )
                        }
                    }
                }
            }

            val commentaryCard: @Composable (Modifier) -> Unit = { mod ->
                SettingStandaloneCard(
                    title = stringResource(R.string.commentary_label),
                    subtitle = "تعليق عربي حماسي للأهداف والفرص أثناء المباراة",
                    icon = if (settings.isCommentaryEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    accent = ElectricCyan,
                    badgeText = if (settings.isCommentaryEnabled) "مُفعّل" else "متوقف",
                    modifier = mod
                ) {
                    Surface(
                        onClick = { onCommentaryEnabledChange(!settings.isCommentaryEnabled) },
                        shape = RoundedCornerShape(12.dp),
                        color = StadiumElevated,
                        border = BorderStroke(1.dp, if (settings.isCommentaryEnabled) ElectricCyan.copy(alpha = 0.45f) else DividerSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.isCommentaryEnabled) "صوت المعلق يعمل أثناء المباراة" else "تم إيقاف صوت المعلق",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = settings.isCommentaryEnabled,
                                onCheckedChange = onCommentaryEnabledChange,
                                modifier = Modifier.testTag("settings_commentary_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF031E24),
                                    checkedTrackColor = ElectricCyan,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = StadiumCard
                                )
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onTestCommentary,
                        enabled = settings.isCommentaryEnabled && !settings.isMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_commentary_test_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ElectricCyan.copy(alpha = 0.18f),
                            contentColor = ElectricCyan
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تجربة صوت المعلق المباشر",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val graphicsCard: @Composable (Modifier) -> Unit = { mod ->
                SettingStandaloneCard(
                    title = stringResource(R.string.graphics_label),
                    subtitle = "تحكم في جودة العشب، الظلال، الإضاءة، واللاعبين 3D",
                    icon = Icons.Default.Hd,
                    accent = ChampionshipGold,
                    badgeText = settings.graphicsQuality.labelAr,
                    modifier = mod
                ) {
                    ExposedDropdownMenuBox(
                        expanded = graphicsExpanded,
                        onExpandedChange = { graphicsExpanded = !graphicsExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${settings.graphicsQuality.labelAr} — ${graphicsDescription(settings.graphicsQuality)}",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = graphicsExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("settings_graphics_dropdown"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChampionshipGold,
                                unfocusedBorderColor = DividerSubtle,
                                focusedContainerColor = StadiumElevated,
                                unfocusedContainerColor = StadiumElevated,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = graphicsExpanded,
                            onDismissRequest = { graphicsExpanded = false },
                            containerColor = StadiumElevated
                        ) {
                            GraphicsQuality.entries.forEach { quality ->
                                val isSelected = quality == settings.graphicsQuality
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = quality.labelAr,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSelected) ChampionshipGold else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = graphicsDescription(quality),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ChampionshipGold
                                            )
                                        }
                                    },
                                    onClick = {
                                        onGraphicsSelected(quality)
                                        graphicsExpanded = false
                                    },
                                    modifier = Modifier.testTag("settings_graphics_item_${quality.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GraphicsQuality.entries.forEach { q ->
                            val selected = q == settings.graphicsQuality
                            Surface(
                                onClick = { onGraphicsSelected(q) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) ChampionshipGold else StadiumElevated,
                                border = if (selected) null else BorderStroke(1.dp, DividerSubtle)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text(
                                        text = q.labelAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color(0xFF1F1600) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val cameraCard: @Composable (Modifier) -> Unit = { mod ->
                SettingStandaloneCard(
                    title = "مسافة الكاميرا",
                    subtitle = "تعديل بعد الكاميرا وزاوية الرؤية للملعب",
                    icon = Icons.Default.Videocam,
                    accent = TurfEmerald,
                    badgeText = settings.cameraDistance.labelAr,
                    modifier = mod
                ) {
                    ExposedDropdownMenuBox(
                        expanded = cameraExpanded,
                        onExpandedChange = { cameraExpanded = !cameraExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${settings.cameraDistance.labelAr} — ${cameraDescription(settings.cameraDistance)}",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = cameraExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("settings_camera_dropdown"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TurfEmerald,
                                unfocusedBorderColor = DividerSubtle,
                                focusedContainerColor = StadiumElevated,
                                unfocusedContainerColor = StadiumElevated,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = cameraExpanded,
                            onDismissRequest = { cameraExpanded = false },
                            containerColor = StadiumElevated
                        ) {
                            CameraDistance.entries.forEach { camDist ->
                                val isSelected = camDist == settings.cameraDistance
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = camDist.labelAr,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSelected) TurfEmerald else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = cameraDescription(camDist),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = TurfEmerald
                                            )
                                        }
                                    },
                                    onClick = {
                                        onCameraDistanceSelected(camDist)
                                        cameraExpanded = false
                                    },
                                    modifier = Modifier.testTag("settings_camera_item_${camDist.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CameraDistance.entries.forEach { cam ->
                            val selected = cam == settings.cameraDistance
                            Surface(
                                onClick = { onCameraDistanceSelected(cam) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) TurfEmerald else StadiumElevated,
                                border = if (selected) null else BorderStroke(1.dp, DividerSubtle)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = cam.labelAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color(0xFF042111) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val difficultyCard: @Composable (Modifier) -> Unit = { mod ->
                SettingStandaloneCard(
                    title = stringResource(R.string.difficulty_label),
                    subtitle = "سرعة رد فعل الخصم وضغطه التكتيكي",
                    icon = Icons.Default.Speed,
                    accent = ElectricCyan,
                    badgeText = settings.difficulty.labelAr,
                    modifier = mod
                ) {
                    ExposedDropdownMenuBox(
                        expanded = difficultyExpanded,
                        onExpandedChange = { difficultyExpanded = !difficultyExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${settings.difficulty.labelAr} — ${difficultyDescription(settings.difficulty)}",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("settings_difficulty_dropdown"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = DividerSubtle,
                                focusedContainerColor = StadiumElevated,
                                unfocusedContainerColor = StadiumElevated,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = difficultyExpanded,
                            onDismissRequest = { difficultyExpanded = false },
                            containerColor = StadiumElevated
                        ) {
                            Difficulty.entries.forEach { diff ->
                                val isSelected = diff == settings.difficulty
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = diff.labelAr,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = difficultyDescription(diff),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ElectricCyan
                                            )
                                        }
                                    },
                                    onClick = {
                                        onDifficultySelected(diff)
                                        difficultyExpanded = false
                                    },
                                    modifier = Modifier.testTag("settings_difficulty_item_${diff.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Difficulty.entries.forEach { diff ->
                            val selected = diff == settings.difficulty
                            Surface(
                                onClick = { onDifficultySelected(diff) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) ElectricCyan else StadiumElevated,
                                border = if (selected) null else BorderStroke(1.dp, DividerSubtle)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = diff.labelAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color(0xFF031E24) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val durationCard: @Composable (Modifier) -> Unit = { mod ->
                SettingStandaloneCard(
                    title = stringResource(R.string.match_time_label),
                    subtitle = "المدة الكلية للمباراة (شوطان متساويان)",
                    icon = Icons.Default.Timer,
                    accent = ChampionshipGold,
                    badgeText = "$displayedDurationSec ثانية",
                    modifier = mod
                ) {
                    Slider(
                        value = if (isDraggingDuration) sliderDuration else settings.matchDurationSeconds.toFloat().coerceIn(60f, 180f),
                        onValueChange = { raw ->
                            isDraggingDuration = true
                            sliderDuration = raw
                            val snapped = ((raw / 30f).roundToInt() * 30).coerceIn(60, 180)
                            onMatchDurationSelected(snapped)
                        },
                        onValueChangeFinished = {
                            isDraggingDuration = false
                            val snapped = ((sliderDuration / 30f).roundToInt() * 30).coerceIn(60, 180)
                            onMatchDurationSelected(snapped)
                        },
                        valueRange = 60f..180f,
                        steps = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_duration_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = ChampionshipGold,
                            activeTrackColor = ChampionshipGold,
                            inactiveTrackColor = StadiumElevated
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(60, 90, 120, 150, 180).forEach { sec ->
                            val selected = settings.matchDurationSeconds == sec
                            Surface(
                                onClick = { onMatchDurationSelected(sec) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) ChampionshipGold else StadiumElevated,
                                border = if (selected) null else BorderStroke(1.dp, DividerSubtle)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${sec}ث",
                                        fontFamily = ChakraPetchFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selected) Color(0xFF1F1600) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val passIndicatorCard: @Composable (Modifier) -> Unit = { mod ->
                SettingStandaloneCard(
                    title = "علامة التمرير",
                    subtitle = "مؤشر ذهبي يظهر فوق زميلك المستهدف عند التمرير",
                    icon = if (settings.showPassIndicator) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    accent = TurfEmerald,
                    badgeText = if (settings.showPassIndicator) "ظاهرة" else "مخفية",
                    modifier = mod
                ) {
                    Surface(
                        onClick = { onShowPassIndicatorChange(!settings.showPassIndicator) },
                        shape = RoundedCornerShape(12.dp),
                        color = StadiumElevated,
                        border = BorderStroke(1.dp, if (settings.showPassIndicator) TurfEmerald.copy(alpha = 0.45f) else DividerSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (settings.showPassIndicator) {
                                    "إظهار مسار التمرير والمثلث الذهبي"
                                } else {
                                    "إخفاء المؤشر للعب باحترافية"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = settings.showPassIndicator,
                                onCheckedChange = onShowPassIndicatorChange,
                                modifier = Modifier.testTag("settings_pass_indicator_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF042111),
                                    checkedTrackColor = TurfEmerald,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = StadiumCard
                                )
                            )
                        }
                    }
                }
            }

            if (isWide) {
                // Landscape / Tablet: Organized 2-Pane Layout (Category Sidebar + Dedicated Standalone Property Cards)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left Category Rail
                    Surface(
                        modifier = Modifier
                            .width(210.dp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(22.dp),
                        color = StadiumCard,
                        border = BorderStroke(1.dp, DividerSubtle)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "الأقسام",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            SettingsCategory.entries.forEach { category ->
                                val selected = category == selectedCategory
                                Surface(
                                    onClick = { selectedCategory = category },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_category_${category.name.lowercase()}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) category.accent.copy(alpha = 0.16f) else Color.Transparent,
                                    border = if (selected) BorderStroke(1.dp, category.accent.copy(alpha = 0.6f)) else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (selected) category.accent else StadiumElevated
                                        ) {
                                            Icon(
                                                imageVector = category.icon,
                                                contentDescription = null,
                                                tint = if (selected) Color(0xFF06130D) else category.accent,
                                                modifier = Modifier.padding(6.dp).size(18.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = category.titleAr,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                            )
                                            Text(
                                                text = category.subtitleAr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Content Pane: Each Property in its Own Separate Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsStatusBanners(uiState = uiState, errorMessage = errorMessage)

                        AnimatedContent(targetState = selectedCategory, label = "settings_category_content") { cat ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                when (cat) {
                                    SettingsCategory.ALL -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            volumeCard(Modifier.weight(1f))
                                            commentaryCard(Modifier.weight(1f))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            graphicsCard(Modifier.weight(1f))
                                            cameraCard(Modifier.weight(1f))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            difficultyCard(Modifier.weight(1f))
                                            durationCard(Modifier.weight(1f))
                                        }
                                        passIndicatorCard(Modifier.fillMaxWidth())
                                    }

                                    SettingsCategory.AUDIO -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            volumeCard(Modifier.weight(1f))
                                            commentaryCard(Modifier.weight(1f))
                                        }
                                    }

                                    SettingsCategory.VISUALS -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            graphicsCard(Modifier.weight(1f))
                                            cameraCard(Modifier.weight(1f))
                                        }
                                    }

                                    SettingsCategory.GAMEPLAY -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            difficultyCard(Modifier.weight(1f))
                                            durationCard(Modifier.weight(1f))
                                        }
                                        passIndicatorCard(Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Compact Portrait: Horizontal Category Tabs + Single-Column Individual Property Cards
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .align(Alignment.TopCenter)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SettingsCategory.entries) { category ->
                            val selected = category == selectedCategory
                            Surface(
                                onClick = { selectedCategory = category },
                                modifier = Modifier.height(42.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) category.accent else StadiumCard,
                                border = BorderStroke(1.dp, if (selected) category.accent else DividerSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (selected) Color(0xFF06130D) else category.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = category.titleAr,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) Color(0xFF06130D) else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsStatusBanners(uiState = uiState, errorMessage = errorMessage)
                        when (selectedCategory) {
                            SettingsCategory.ALL -> {
                                volumeCard(Modifier.fillMaxWidth())
                                commentaryCard(Modifier.fillMaxWidth())
                                graphicsCard(Modifier.fillMaxWidth())
                                cameraCard(Modifier.fillMaxWidth())
                                difficultyCard(Modifier.fillMaxWidth())
                                durationCard(Modifier.fillMaxWidth())
                                passIndicatorCard(Modifier.fillMaxWidth())
                            }
                            SettingsCategory.AUDIO -> {
                                volumeCard(Modifier.fillMaxWidth())
                                commentaryCard(Modifier.fillMaxWidth())
                            }
                            SettingsCategory.VISUALS -> {
                                graphicsCard(Modifier.fillMaxWidth())
                                cameraCard(Modifier.fillMaxWidth())
                            }
                            SettingsCategory.GAMEPLAY -> {
                                difficultyCard(Modifier.fillMaxWidth())
                                durationCard(Modifier.fillMaxWidth())
                                passIndicatorCard(Modifier.fillMaxWidth())
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsStatusBanners(
    uiState: PreferencesUiState,
    errorMessage: String?
) {
    if (uiState is PreferencesUiState.Loading) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StadiumCard
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = TurfEmerald
                )
                Text(
                    text = "جاري تحميل التفضيلات المحفوظة...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    val activeError = errorMessage ?: (uiState as? PreferencesUiState.Error)?.message
    if (!activeError.isNullOrBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CrimsonAlert.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f))
        ) {
            Text(
                text = activeError,
                style = MaterialTheme.typography.bodyMedium,
                color = CrimsonAlert,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun SettingStandaloneCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    badgeText: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = StadiumCard,
        border = BorderStroke(1.dp, DividerSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accent,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.40f))
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            content()
        }
    }
}

private fun graphicsDescription(quality: GraphicsQuality): String = when (quality) {
    GraphicsQuality.LOW -> "سلاسة قصوى وتوفير للبطارية"
    GraphicsQuality.MEDIUM -> "توازن مثالي بين الأداء والوضوح"
    GraphicsQuality.HIGH -> "إضاءة ملعب كاملة وتفاصيل فائقة"
    GraphicsQuality.ULTRA -> "جرافيكس سينمائي: كشافات وأضواء وجماهير عالية الدقة"
}

private fun difficultyDescription(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.EASY -> "سرعة ورد فعل هادئ للخصم (${(difficulty.aiSpeedScale * 100).toInt()}%)"
    Difficulty.MEDIUM -> "تحدي متوازن وممتع (${(difficulty.aiSpeedScale * 100).toInt()}%)"
    Difficulty.HARD -> "ضغط عالٍ وذكاء تكتيكي متقدم (${(difficulty.aiSpeedScale * 100).toInt()}%)"
}

private fun cameraDescription(distance: CameraDistance): String = when (distance) {
    CameraDistance.CLOSE -> "زوم قريب لإظهار تفاصيل اللاعبين والكرة"
    CameraDistance.MEDIUM -> "كاميرا بث متوازنة ومناسبة لكل الأوضاع"
    CameraDistance.FAR -> "رؤية واسعة للملعب وكشف كامل التحركات"
}
