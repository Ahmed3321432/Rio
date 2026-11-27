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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Difficulty
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DividerSubtle
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
    onGraphicsSelected: (GraphicsQuality) -> Unit,
    onDifficultySelected: (Difficulty) -> Unit,
    onShowPassIndicatorChange: (Boolean) -> Unit,
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
                            imageVector = if (settings.isMuted || settings.volume <= 0.01f) {
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

                    val pct = if (settings.isMuted) 0 else (settings.volume * 100).toInt()
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
                        value = if (settings.isMuted) 0f else settings.volume,
                        onValueChange = onVolumeChange,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                modifier = Modifier.padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quality.labelAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color(0xFF1F1600) else MaterialTheme.colorScheme.onSurface
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
        }
    }
}
