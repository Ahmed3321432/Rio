package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CareerStats
import com.example.model.GameSettings
import com.example.model.TeamCatalog
import com.example.ui.components.TeamCrest
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfEmerald

@Composable
fun MainMenuScreen(
    settings: GameSettings,
    careerStats: CareerStats,
    onOpenQuickMatch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val favoriteTeam = remember(settings.homeTeamId) { TeamCatalog.findById(settings.homeTeamId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumObsidian)
    ) {
        // Full-bleed Widescreen Stadium Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_stadium_hero),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            StadiumObsidian.copy(alpha = 0.94f),
                            StadiumObsidian.copy(alpha = 0.78f),
                            StadiumObsidian.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StadiumCard.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = ChampionshipGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${careerStats.matchesPlayed} مباراة • ${careerStats.wins} فوز • ${careerStats.totalGoals} هدف",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StadiumCard.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TeamCrest(team = favoriteTeam, size = 24.dp)
                        Text(
                            text = favoriteTeam.nameAr,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }

            // Main Widescreen Hub: Title on Start, Game Modes on End
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 980.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hero Branding Column
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = TurfEmerald.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "أطوار اللعب",
                            style = MaterialTheme.typography.labelLarge,
                            color = TurfEmerald,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 36.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "اختر الطور المناسب وانطلق في مباريات 11 ضد 11 مع تحكم تكتيكي كامل.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD2E4DA)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill(
                            label = "الجودة: ${settings.graphicsQuality.labelAr}",
                            accent = ChampionshipGold
                        )
                        InfoPill(
                            label = "الصعوبة: ${settings.difficulty.labelAr}",
                            accent = ElectricCyan
                        )
                    }
                }

                // Active Modes Column (Quick Match + Setting)
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
                ) {
                    ModeSelectionCard(
                        titleAr = "مباراة سريعة",
                        badgeEn = "QUICK MATCH",
                        subtitle = "اختر فريقك والخصم وعدّل التشكيلة وابدأ اللعب فوراً",
                        icon = Icons.Default.SportsSoccer,
                        accentColor = TurfEmerald,
                        isPrimary = true,
                        onClick = onOpenQuickMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .testTag("mode_quick_match")
                    )

                    ModeSelectionCard(
                        titleAr = "الإعدادات",
                        badgeEn = "SETTING",
                        subtitle = "التحكم في الصوت، جودة الجرافيكس، ومستوى الصعوبة",
                        icon = Icons.Default.Tune,
                        accentColor = ChampionshipGold,
                        isPrimary = false,
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(94.dp)
                            .testTag("mode_settings")
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelectionCard(
    titleAr: String,
    badgeEn: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = if (isPrimary) StadiumCard.copy(alpha = 0.94f) else StadiumElevated.copy(alpha = 0.92f),
        border = BorderStroke(
            width = if (isPrimary) 2.dp else 1.2.dp,
            color = accentColor.copy(alpha = if (isPrimary) 0.8f else 0.45f)
        ),
        tonalElevation = if (isPrimary) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isPrimary) 0.22f else 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = titleAr,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = titleAr,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = badgeEn,
                                fontFamily = ChakraPetchFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFC4D6CC),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.20f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = StadiumCard.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, DividerSubtle)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
