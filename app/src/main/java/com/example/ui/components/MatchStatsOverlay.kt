package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.MatchPhase
import com.example.model.MatchState
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurfEmerald

@Composable
fun LiveStatsMiniStrip(
    matchState: MatchState,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = matchState.stats
    val homePoss = stats.homePossessionPercent
    val awayPoss = stats.awayPossessionPercent
    val animatedHomeRatio by animateFloatAsState(
        targetValue = (homePoss / 100f).coerceIn(0.08f, 0.92f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mini_poss_ratio"
    )

    Surface(
        onClick = onExpandClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("live_stats_mini_strip"),
        shape = RoundedCornerShape(12.dp),
        color = StadiumObsidian.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = stringResource(R.string.stats_toggle_cd),
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$homePoss%-$awayPoss%",
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.testTag("mini_stats_possession")
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(11.dp)
                        .background(DividerSubtle)
                )

                Text(
                    text = "${stringResource(R.string.stats_shots_on_target)}: ${stats.homeShotsOnTarget}-${stats.awayShotsOnTarget}",
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = TurfEmerald,
                    modifier = Modifier.testTag("mini_stats_shots_on_target")
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(11.dp)
                        .background(DividerSubtle)
                )

                Text(
                    text = "${stringResource(R.string.stats_fouls)}: ${stats.homeFouls}-${stats.awayFouls}",
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = ChampionshipGold,
                    modifier = Modifier.testTag("mini_stats_fouls")
                )

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Live split possession bar
            Box(
                modifier = Modifier
                    .width(248.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ChampionshipGold.copy(alpha = 0.85f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedHomeRatio)
                        .fillMaxHeight()
                        .background(TurfEmerald)
                )
            }
        }
    }
}

@Composable
fun MatchStatsOverlay(
    matchState: MatchState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = matchState.stats
    val isLive = matchState.phase == MatchPhase.PLAYING || matchState.phase == MatchPhase.SET_PIECE
    val infinitePulse = rememberInfiniteTransition(label = "live_stats_pulse")
    val liveDotAlpha by infinitePulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("match_stats_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(24.dp),
            color = StadiumCard.copy(alpha = 0.96f),
            border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.48f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Live Telemetry Badge + Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isLive) CrimsonAlert.copy(alpha = 0.18f) else StadiumElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isLive) CrimsonAlert.copy(alpha = 0.65f) else DividerSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isLive) CrimsonAlert.copy(alpha = liveDotAlpha)
                                            else ChampionshipGold
                                        )
                                )
                                Text(
                                    text = if (isLive) stringResource(R.string.stats_live_badge) else "${matchState.displayMatchMinute}'",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.match_stats_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("close_stats_overlay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.stats_close_cd),
                            tint = Color.White
                        )
                    }
                }

                // Team Header Strip (Score & Crests)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = StadiumElevated,
                        border = BorderStroke(1.dp, DividerSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TeamCrest(team = matchState.homeTeam, size = 30.dp)
                                Column {
                                    Text(
                                        text = matchState.homeTeam.shortCode,
                                        fontFamily = ChakraPetchFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TurfEmerald
                                    )
                                    Text(
                                        text = matchState.homeTeam.nameAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StadiumObsidian
                            ) {
                                Text(
                                    text = "${matchState.homeScore}  -  ${matchState.awayScore}  (${matchState.displayMatchMinute}')",
                                    fontFamily = ChakraPetchFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = matchState.awayTeam.shortCode,
                                        fontFamily = ChakraPetchFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ChampionshipGold
                                    )
                                    Text(
                                        text = matchState.awayTeam.nameAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                TeamCrest(team = matchState.awayTeam, size = 30.dp)
                            }
                        }
                    }
                }

                // Real-time Stat Comparison Rows
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = StadiumObsidian.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, DividerSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatComparisonBarRow(
                            label = stringResource(R.string.stats_possession),
                            homeDisplay = "${stats.homePossessionPercent}%",
                            awayDisplay = "${stats.awayPossessionPercent}%",
                            homeValue = stats.homePossessionPercent.toFloat(),
                            awayValue = stats.awayPossessionPercent.toFloat(),
                            homeColor = TurfEmerald,
                            awayColor = ChampionshipGold,
                            rowTestTag = "stats_possession_row"
                        )

                        HorizontalDivider(color = DividerSubtle.copy(alpha = 0.5f))

                        StatComparisonBarRow(
                            label = stringResource(R.string.stats_shots_on_target),
                            homeDisplay = "${stats.homeShotsOnTarget}",
                            awayDisplay = "${stats.awayShotsOnTarget}",
                            homeValue = stats.homeShotsOnTarget.toFloat(),
                            awayValue = stats.awayShotsOnTarget.toFloat(),
                            homeColor = ElectricCyan,
                            awayColor = ChampionshipGold,
                            rowTestTag = "stats_shots_on_target_row"
                        )

                        HorizontalDivider(color = DividerSubtle.copy(alpha = 0.5f))

                        StatComparisonBarRow(
                            label = stringResource(R.string.stats_fouls),
                            homeDisplay = "${stats.homeFouls}",
                            awayDisplay = "${stats.awayFouls}",
                            homeValue = stats.homeFouls.toFloat(),
                            awayValue = stats.awayFouls.toFloat(),
                            homeColor = CrimsonAlert,
                            awayColor = Color(0xFFFF9F43),
                            rowTestTag = "stats_fouls_row"
                        )

                        HorizontalDivider(color = DividerSubtle.copy(alpha = 0.5f))

                        StatComparisonBarRow(
                            label = stringResource(R.string.stats_total_shots),
                            homeDisplay = "${stats.homeShots}",
                            awayDisplay = "${stats.awayShots}",
                            homeValue = stats.homeShots.toFloat(),
                            awayValue = stats.awayShots.toFloat(),
                            homeColor = TurfEmerald,
                            awayColor = ChampionshipGold,
                            rowTestTag = "stats_total_shots_row"
                        )

                        HorizontalDivider(color = DividerSubtle.copy(alpha = 0.5f))

                        StatComparisonBarRow(
                            label = stringResource(R.string.stats_tackles),
                            homeDisplay = "${stats.homeTackles}",
                            awayDisplay = "${stats.awayTackles}",
                            homeValue = stats.homeTackles.toFloat(),
                            awayValue = stats.awayTackles.toFloat(),
                            homeColor = ElectricCyan,
                            awayColor = ChampionshipGold,
                            rowTestTag = "stats_tackles_row"
                        )

                        HorizontalDivider(color = DividerSubtle.copy(alpha = 0.5f))

                        StatComparisonBarRow(
                            label = stringResource(R.string.stats_corners),
                            homeDisplay = "${stats.homeCorners}",
                            awayDisplay = "${stats.awayCorners}",
                            homeValue = stats.homeCorners.toFloat(),
                            awayValue = stats.awayCorners.toFloat(),
                            homeColor = TurfEmerald,
                            awayColor = ChampionshipGold,
                            rowTestTag = "stats_corners_row"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatComparisonBarRow(
    label: String,
    homeDisplay: String,
    awayDisplay: String,
    homeValue: Float,
    awayValue: Float,
    homeColor: Color,
    awayColor: Color,
    rowTestTag: String
) {
    val total = homeValue + awayValue
    val rawHomeFraction = if (total <= 0.01f) 0.5f else (homeValue / total).coerceIn(0f, 1f)
    val animatedHomeFraction by animateFloatAsState(
        targetValue = rawHomeFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "${rowTestTag}_bar"
    )
    val animatedAwayColor by animateColorAsState(targetValue = awayColor, label = "${rowTestTag}_away")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(rowTestTag),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = homeDisplay,
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = homeColor,
                    modifier = Modifier.widthIn(min = 42.dp),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = awayDisplay,
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = animatedAwayColor,
                    modifier = Modifier.widthIn(min = 42.dp),
                    textAlign = TextAlign.End
                )
            }

            // Dual animated comparison bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(animatedAwayColor.copy(alpha = if (total <= 0.01f) 0.28f else 0.88f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedHomeFraction)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(homeColor.copy(alpha = 0.78f), homeColor)
                            )
                        )
                )
            }
        }
    }
}
