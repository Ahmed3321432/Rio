package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CameraDistance
import com.example.model.Difficulty
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.TurfEmerald

@Composable
fun AudioAndGraphicsPanel(
    settings: GameSettings,
    onVolumeChange: (Float) -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onCommentaryEnabledChange: (Boolean) -> Unit = {},
    onGraphicsSelected: (GraphicsQuality) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onCameraDistanceSelected: (CameraDistance) -> Unit = {},
    onShowPassIndicatorChange: (Boolean) -> Unit,
    onResetPreferences: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = StadiumCard,
        border = BorderStroke(1.dp, DividerSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Volume Controls (أعلي وأوطي الصوت)
            var isDraggingSlider by remember { mutableStateOf(false) }
            var sliderValue by remember(settings.volume, settings.isMuted) {
                mutableFloatStateOf(if (settings.isMuted) 0f else settings.volume)
            }
            val displayedVolume = if (isDraggingSlider) sliderValue else if (settings.isMuted) 0f else settings.volume

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (displayedVolume <= 0.01f) {
                                Icons.AutoMirrored.Filled.VolumeOff
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = stringResource(R.string.volume_label),
                            tint = TurfEmerald
                        )
                        Text(
                            text = stringResource(R.string.volume_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val pct = (displayedVolume * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StadiumElevated
                    ) {
                        Text(
                            text = "$pct%",
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TurfEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = onVolumeDown,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("volume_down_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = StadiumElevated,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "توطية الصوت"
                        )
                    }

                    Slider(
                        value = displayedVolume,
                        onValueChange = {
                            isDraggingSlider = true
                            sliderValue = it
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onVolumeChange(sliderValue)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("volume_slider"),
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
                            .testTag("volume_up_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = StadiumElevated,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "تعلية الصوت"
                        )
                    }

                    FilledIconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("mute_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (settings.isMuted) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else StadiumElevated,
                            contentColor = if (settings.isMuted) MaterialTheme.colorScheme.error else TurfEmerald
                        )
                    ) {
                        Icon(
                            imageVector = if (settings.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "كتم الصوت"
                        )
                    }
                }
            }

            // Commentary Toggle (المعلق الصوتي المباشر)
            Surface(
                onClick = { onCommentaryEnabledChange(!settings.isCommentaryEnabled) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("commentary_toggle_row"),
                shape = RoundedCornerShape(14.dp),
                color = StadiumElevated,
                border = BorderStroke(1.dp, DividerSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (settings.isCommentaryEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.commentary_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (settings.isCommentaryEnabled) "تعليق مباشر أثناء المباراة" else "متوقف حالياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = settings.isCommentaryEnabled,
                        onCheckedChange = onCommentaryEnabledChange,
                        modifier = Modifier.testTag("commentary_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF031E24),
                            checkedTrackColor = ElectricCyan,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = StadiumCard
                        )
                    )
                }
            }

            // Graphics Quality Selector (أعلي وأوطي الجرافيكس)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hd,
                        contentDescription = stringResource(R.string.graphics_label),
                        tint = ChampionshipGold
                    )
                    Text(
                        text = stringResource(R.string.graphics_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GraphicsQuality.entries.forEach { quality ->
                        val selected = settings.graphicsQuality == quality
                        Surface(
                            onClick = { onGraphicsSelected(quality) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("graphics_${quality.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) ChampionshipGold else StadiumElevated,
                            border = if (selected) null else BorderStroke(1.dp, DividerSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quality.labelAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF1F1600) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Difficulty Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = stringResource(R.string.difficulty_label),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = stringResource(R.string.difficulty_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        val selected = settings.difficulty == diff
                        Surface(
                            onClick = { onDifficultySelected(diff) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("difficulty_${diff.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) TurfEmerald else StadiumElevated,
                            border = if (selected) null else BorderStroke(1.dp, DividerSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = diff.labelAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color(0xFF042111) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Camera Distance Selector (قريبة / متوسطة / بعيدة)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "مسافة الكاميرا",
                        tint = ElectricCyan
                    )
                    Text(
                        text = "مسافة الكاميرا",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CameraDistance.entries.forEach { camDist ->
                        val selected = settings.cameraDistance == camDist
                        Surface(
                            onClick = { onCameraDistanceSelected(camDist) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("camera_distance_${camDist.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) ElectricCyan else StadiumElevated,
                            border = if (selected) null else BorderStroke(1.dp, DividerSubtle)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = camDist.labelAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color(0xFF031E24) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Pass Indicator Toggle (إظهار أو إخفاء علامة التمرير)
            Surface(
                onClick = { onShowPassIndicatorChange(!settings.showPassIndicator) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pass_indicator_row"),
                shape = RoundedCornerShape(14.dp),
                color = StadiumElevated,
                border = BorderStroke(1.dp, DividerSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (settings.showPassIndicator) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = ChampionshipGold
                        )
                        Column {
                            Text(
                                text = "علامة التمرير",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (settings.showPassIndicator) "ظاهرة على اللاعب المستهدف" else "مخفية (التمريرة تصل للاعب تلقائياً)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = settings.showPassIndicator,
                        onCheckedChange = onShowPassIndicatorChange,
                        modifier = Modifier.testTag("pass_indicator_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF042111),
                            checkedTrackColor = TurfEmerald,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = StadiumCard
                        )
                    )
                }
            }

            if (onResetPreferences != null) {
                OutlinedButton(
                    onClick = onResetPreferences,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_preferences_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DividerSubtle)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = ChampionshipGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "استعادة الإعدادات الافتراضية",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
