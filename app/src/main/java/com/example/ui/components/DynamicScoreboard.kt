package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchPhase
import com.example.model.MatchState
import com.example.model.TeamSide
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfEmerald
import kotlinx.coroutines.delay

@Composable
fun DynamicScoreboard(
    matchState: MatchState,
    modifier: Modifier = Modifier
) {
    var homePulse by remember { mutableStateOf(false) }
    var awayPulse by remember { mutableStateOf(false) }

    LaunchedEffect(matchState.homeScore) {
        if (matchState.homeScore > 0) {
            homePulse = true
            delay(900L)
            homePulse = false
        }
    }

    LaunchedEffect(matchState.awayScore) {
        if (matchState.awayScore > 0) {
            awayPulse = true
            delay(900L)
            awayPulse = false
        }
    }

    val homeScale by animateFloatAsState(
        targetValue = if (homePulse) 1.24f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "home_score_scale"
    )
    val awayScale by animateFloatAsState(
        targetValue = if (awayPulse) 1.24f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "away_score_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            homePulse -> TurfEmerald
            awayPulse -> CrimsonAlert
            else -> DividerSubtle
        },
        label = "scoreboard_border_color"
    )

    val isGoalCelebration = matchState.phase == MatchPhase.GOAL_CELEBRATION

    Column(
        modifier = modifier.testTag("dynamic_scoreboard"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StadiumObsidian.copy(alpha = 0.88f),
            border = BorderStroke(if (homePulse || awayPulse) 1.8.dp else 1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Home team vertical kit color bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(matchState.homeTeam.primaryColor)
                )

                TeamCrest(team = matchState.homeTeam, size = 24.dp)
                Text(
                    text = matchState.homeTeam.shortCode,
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )

                // Animated Home Score Badge
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (homePulse) Color.White else TurfEmerald,
                    modifier = Modifier.graphicsLayer {
                        scaleX = homeScale
                        scaleY = homeScale
                    }
                ) {
                    AnimatedContent(
                        targetState = matchState.homeScore,
                        transitionSpec = {
                            (slideInVertically { -it } + fadeIn()) togetherWith
                                (slideOutVertically { it } + fadeOut())
                        },
                        label = "home_score_anim"
                    ) { score ->
                        Text(
                            text = "$score",
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF042111),
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                                .testTag("home_score_text")
                        )
                    }
                }

                Text(
                    text = "-",
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.65f)
                )

                // Animated Away Score Badge
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (awayPulse) Color.White else ChampionshipGold,
                    modifier = Modifier.graphicsLayer {
                        scaleX = awayScale
                        scaleY = awayScale
                    }
                ) {
                    AnimatedContent(
                        targetState = matchState.awayScore,
                        transitionSpec = {
                            (slideInVertically { -it } + fadeIn()) togetherWith
                                (slideOutVertically { it } + fadeOut())
                        },
                        label = "away_score_anim"
                    ) { score ->
                        Text(
                            text = "$score",
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF211700),
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                                .testTag("away_score_text")
                        )
                    }
                }

                Text(
                    text = matchState.awayTeam.shortCode,
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                TeamCrest(team = matchState.awayTeam, size = 24.dp)

                // Away team vertical kit color bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(matchState.awayTeam.primaryColor)
                )

                // Live Match Minute & Half
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StadiumElevated
                ) {
                    Text(
                        text = "${matchState.displayMatchMinute}' • ش${matchState.half}",
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ChampionshipGold,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("match_clock_text")
                    )
                }
            }
        }

        // Real-time Goal Flash Ticker attached to scoreboard
        AnimatedVisibility(
            visible = isGoalCelebration || homePulse || awayPulse,
            enter = slideInVertically { -it / 2 } + fadeIn(),
            exit = fadeOut()
        ) {
            val scorerColor = if (matchState.lastScorerTeam == TeamSide.HOME) TurfEmerald else CrimsonAlert
            val teamCode = if (matchState.lastScorerTeam == TeamSide.HOME) {
                matchState.homeTeam.shortCode
            } else {
                matchState.awayTeam.shortCode
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StadiumObsidian.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, scorerColor),
                modifier = Modifier.testTag("goal_flash_pill")
            ) {
                Text(
                    text = "هدف $teamCode • ${matchState.lastScorerName} (${matchState.displayMatchMinute}')",
                    style = MaterialTheme.typography.labelMedium,
                    color = scorerColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}
