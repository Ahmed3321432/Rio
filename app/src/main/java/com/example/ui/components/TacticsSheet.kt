package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FormationType
import com.example.model.PlayerRole
import com.example.model.SquadPlayer
import com.example.model.Team
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfEmerald
import kotlin.math.roundToInt

/**
 * Interactive PES/FIFA-style Squad Tactics & Substitution Board.
 * Used both in HomeLobbyScreen (free squad rearranging) and MatchScreen (live subs & formation).
 */
@Composable
fun TacticsAndSubsOverlay(
    team: Team,
    formation: FormationType,
    starters: List<SquadPlayer>, // exactly 11 starters matching formation.slots order
    bench: List<SquadPlayer>,    // 5 substitutes
    staminaByStarterIndex: List<Float>? = null, // null in Lobby, 0..1 in-match
    isInMatch: Boolean,
    subsUsed: Int,
    maxSubs: Int,
    onSelectFormation: (FormationType) -> Unit,
    onSwapStarters: (indexA: Int, indexB: Int) -> Unit,
    onSubstitute: (starterIndex: Int, benchIndex: Int) -> Unit,
    onClose: () -> Unit
) {
    var selectedStarterIdx by remember { mutableStateOf<Int?>(null) }
    var selectedBenchIdx by remember { mutableStateOf<Int?>(null) }

    val subsRemaining = (maxSubs - subsUsed).coerceAtLeast(0)
    val canMakeSub = !isInMatch || subsRemaining > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 780.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = StadiumCard,
            border = BorderStroke(1.5.dp, TurfEmerald.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TeamCrest(team = team, size = 34.dp)
                        Column {
                            Text(
                                text = "الخطة والتشكيل • ${team.nameAr}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isInMatch) {
                                    "التبديلات المتبقية: $subsRemaining من $maxSubs"
                                } else {
                                    "اختر الخطة وبدّل بين الأساسيين والاحتياط"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (canMakeSub) TurfEmerald else CrimsonAlert
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tactics_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق الخطة",
                            tint = Color.White
                        )
                    }
                }

                // Formation Picker Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormationType.entries.forEach { f ->
                        val selected = f == formation
                        Surface(
                            onClick = {
                                selectedStarterIdx = null
                                selectedBenchIdx = null
                                onSelectFormation(f)
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("formation_${f.displayName}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) TurfEmerald else StadiumElevated,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) TurfEmerald else DividerSubtle
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = f.displayName,
                                    fontFamily = ChakraPetchFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selected) Color(0xFF042111) else Color.White
                                )
                                Text(
                                    text = f.styleAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (selected) Color(0xFF062C17) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Selection Hint Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = StadiumObsidian.copy(alpha = 0.72f),
                    border = BorderStroke(
                        1.dp,
                        when {
                            selectedStarterIdx != null -> ElectricCyan.copy(alpha = 0.6f)
                            selectedBenchIdx != null -> ChampionshipGold.copy(alpha = 0.6f)
                            else -> DividerSubtle
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = ChampionshipGold,
                            modifier = Modifier.size(18.dp)
                        )
                        val hintText = when {
                            selectedStarterIdx != null -> {
                                val p = starters.getOrNull(selectedStarterIdx!!)
                                "تم تحديد ${p?.name.orEmpty()} — اضغط على لاعب آخر للتبديل أو التغيير من الدكة"
                            }
                            selectedBenchIdx != null -> {
                                val b = bench.getOrNull(selectedBenchIdx!!)
                                "تم تحديد البديل ${b?.name.orEmpty()} — اضغط على لاعب في الملعب لينزل مكانه"
                            }
                            else -> "اضغط على أي لاعب في الملعب لتغيير مركزه أو استبداله بلاعب من الدكة"
                        }
                        Text(
                            text = hintText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Main Content: Adaptive Pitch + Bench
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val isWide = maxWidth >= 560.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TacticalPitchBoard(
                                team = team,
                                formation = formation,
                                starters = starters,
                                staminaByStarterIndex = staminaByStarterIndex,
                                selectedStarterIdx = selectedStarterIdx,
                                onStarterTapped = { idx ->
                                    val bIdx = selectedBenchIdx
                                    val sIdx = selectedStarterIdx
                                    when {
                                        bIdx != null && canMakeSub -> {
                                            onSubstitute(idx, bIdx)
                                            selectedBenchIdx = null
                                            selectedStarterIdx = null
                                        }
                                        sIdx == null -> selectedStarterIdx = idx
                                        sIdx == idx -> selectedStarterIdx = null
                                        else -> {
                                            onSwapStarters(sIdx, idx)
                                            selectedStarterIdx = null
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.25f)
                                    .fillMaxHeight()
                            )

                            BenchColumnPanel(
                                team = team,
                                bench = bench,
                                selectedBenchIdx = selectedBenchIdx,
                                canMakeSub = canMakeSub,
                                onBenchTapped = { bIdx ->
                                    if (!canMakeSub) return@BenchColumnPanel
                                    val sIdx = selectedStarterIdx
                                    if (sIdx != null) {
                                        onSubstitute(sIdx, bIdx)
                                        selectedStarterIdx = null
                                        selectedBenchIdx = null
                                    } else {
                                        selectedBenchIdx = if (selectedBenchIdx == bIdx) null else bIdx
                                    }
                                },
                                modifier = Modifier
                                    .weight(0.95f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TacticalPitchBoard(
                                team = team,
                                formation = formation,
                                starters = starters,
                                staminaByStarterIndex = staminaByStarterIndex,
                                selectedStarterIdx = selectedStarterIdx,
                                onStarterTapped = { idx ->
                                    val bIdx = selectedBenchIdx
                                    val sIdx = selectedStarterIdx
                                    when {
                                        bIdx != null && canMakeSub -> {
                                            onSubstitute(idx, bIdx)
                                            selectedBenchIdx = null
                                            selectedStarterIdx = null
                                        }
                                        sIdx == null -> selectedStarterIdx = idx
                                        sIdx == idx -> selectedStarterIdx = null
                                        else -> {
                                            onSwapStarters(sIdx, idx)
                                            selectedStarterIdx = null
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(330.dp)
                            )

                            BenchColumnPanel(
                                team = team,
                                bench = bench,
                                selectedBenchIdx = selectedBenchIdx,
                                canMakeSub = canMakeSub,
                                onBenchTapped = { bIdx ->
                                    if (!canMakeSub) return@BenchColumnPanel
                                    val sIdx = selectedStarterIdx
                                    if (sIdx != null) {
                                        onSubstitute(sIdx, bIdx)
                                        selectedStarterIdx = null
                                        selectedBenchIdx = null
                                    } else {
                                        selectedBenchIdx = if (selectedBenchIdx == bIdx) null else bIdx
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("tactics_confirm_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfEmerald,
                        contentColor = Color(0xFF042111)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isInMatch) "تأكيد ومتابعة المباراة" else "حفظ التشكيل",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TacticalPitchBoard(
    team: Team,
    formation: FormationType,
    starters: List<SquadPlayer>,
    staminaByStarterIndex: List<Float>?,
    selectedStarterIdx: Int?,
    onStarterTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF092A18),
        border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.35f))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boardW = constraints.maxWidth.toFloat()
            val boardH = constraints.maxHeight.toFloat()

            // Tactical Pitch Lines (Vertical orientation: Attack UP, GK at BOTTOM)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stripeCount = 8
                val stripeH = size.height / stripeCount
                for (i in 0 until stripeCount) {
                    drawRect(
                        color = if (i % 2 == 0) Color(0xFF0E4928) else Color(0xFF0B3C20),
                        topLeft = Offset(0f, i * stripeH),
                        size = Size(size.width, stripeH + 1f)
                    )
                }
                val lineColor = Color.White.copy(alpha = 0.28f)
                val pad = 12f
                val fw = size.width - pad * 2
                val fh = size.height - pad * 2
                drawRect(lineColor, Offset(pad, pad), Size(fw, fh), style = Stroke(2f))
                // Halfway line
                drawLine(lineColor, Offset(pad, size.height * 0.5f), Offset(pad + fw, size.height * 0.5f), 2f)
                drawCircle(lineColor, radius = fw * 0.14f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(2f))
                // Top attacking box & Bottom defending GK box
                drawRect(lineColor, Offset(size.width * 0.24f, pad), Size(size.width * 0.52f, fh * 0.16f), style = Stroke(2f))
                drawRect(lineColor, Offset(size.width * 0.24f, pad + fh * 0.84f), Size(size.width * 0.52f, fh * 0.16f), style = Stroke(2f))
            }

            // Plot the 11 starters cleanly
            formation.slots.forEachIndexed { idx, slot ->
                val player = starters.getOrNull(idx) ?: return@forEachIndexed
                // Convert horizontal Engine coords (x: 11..77 GK->FWD, y: 14..86 Left->Right)
                // into Vertical Tactical Board coords (GK bottom ~0.91, FWD top ~0.12)
                val normX = ((slot.y - 8f) / 84f).coerceIn(0.08f, 0.92f)
                val normY = (1f - ((slot.x - 4f) / 82f)).coerceIn(0.10f, 0.90f)

                val tokenW = 54.dp
                val tokenH = 54.dp
                val isSelected = selectedStarterIdx == idx
                val stamina = staminaByStarterIndex?.getOrNull(idx)

                Box(
                    modifier = Modifier
                        .offset {
                            val px = (normX * boardW).roundToInt() - (27.dp.toPx()).roundToInt()
                            val py = (normY * boardH).roundToInt() - (27.dp.toPx()).roundToInt()
                            IntOffset(px.coerceIn(0, (boardW - 54.dp.toPx()).roundToInt().coerceAtLeast(0)), py.coerceIn(0, (boardH - 54.dp.toPx()).roundToInt().coerceAtLeast(0)))
                        }
                        .size(tokenW, tokenH)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onStarterTapped(idx) }
                        .testTag("tactics_starter_$idx"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val badgeColor = if (slot.role == PlayerRole.GK) Color(0xFFFFB300) else team.primaryColor
                            Surface(
                                shape = CircleShape,
                                color = badgeColor,
                                border = BorderStroke(
                                    width = if (isSelected) 2.5.dp else 1.5.dp,
                                    color = if (isSelected) ElectricCyan else team.secondaryColor
                                ),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${player.jerseyNumber}",
                                        fontFamily = ChakraPetchFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (badgeColor == Color(0xFFF5F7FA)) Color(0xFF0E1726) else Color.White
                                    )
                                }
                            }
                            if (stamina != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                stamina > 0.6f -> TurfEmerald
                                                stamina > 0.3f -> ChampionshipGold
                                                else -> CrimsonAlert
                                            }
                                        )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StadiumObsidian.copy(alpha = 0.86f)
                        ) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isSelected) ElectricCyan else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenchColumnPanel(
    team: Team,
    bench: List<SquadPlayer>,
    selectedBenchIdx: Int?,
    canMakeSub: Boolean,
    onBenchTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = StadiumElevated,
        border = BorderStroke(1.dp, DividerSubtle)
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "دكة البدلاء (${bench.size} لاعبين)",
                style = MaterialTheme.typography.titleSmall,
                color = ChampionshipGold,
                fontWeight = FontWeight.Bold
            )

            bench.forEachIndexed { idx, player ->
                val selected = selectedBenchIdx == idx
                Surface(
                    onClick = { onBenchTapped(idx) },
                    enabled = canMakeSub,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("tactics_bench_$idx"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) ChampionshipGold.copy(alpha = 0.22f) else StadiumCard,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ChampionshipGold else DividerSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (player.preferredRole == PlayerRole.GK) Color(0xFFFFB300) else team.primaryColor,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${player.jerseyNumber}",
                                        fontFamily = ChakraPetchFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (team.primaryColor == Color(0xFFF5F7FA) && player.preferredRole != PlayerRole.GK) {
                                            Color(0xFF0E1726)
                                        } else Color.White
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = roleLabelAr(player.preferredRole),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Brush.horizontalGradient(
                                listOf(TurfEmerald.copy(alpha = 0.22f), ChampionshipGold.copy(alpha = 0.22f))
                            ).let { Color.Transparent },
                            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${player.rating}",
                                fontFamily = ChakraPetchFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ChampionshipGold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun roleLabelAr(role: PlayerRole): String = when (role) {
    PlayerRole.GK -> "حارس مرمى"
    PlayerRole.DEF -> "مدافع"
    PlayerRole.MID -> "وسط ملعب"
    PlayerRole.FWD -> "مهاجم"
}
