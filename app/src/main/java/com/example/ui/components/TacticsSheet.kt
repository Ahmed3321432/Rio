package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.FormationType
import com.example.model.PlayerRole
import com.example.model.SquadPlayer
import com.example.model.Team
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfEmerald
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Interactive Landscape PES/EA-style Squad Tactics & Drag-and-Drop Substitution Board.
 */
@Composable
fun TacticsAndSubsOverlay(
    team: Team,
    formation: FormationType,
    starters: List<SquadPlayer>, // 11 starters matching formation.slots order
    bench: List<SquadPlayer>,    // 5 substitutes
    staminaByStarterIndex: List<Float>? = null,
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

    // Drag-and-Drop tracking in physical root coordinates (works identically in RTL & LTR)
    var overlayOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    val starterBoundsInRoot = remember { mutableStateMapOf<Int, Rect>() }
    var draggedBenchIdx by remember { mutableStateOf<Int?>(null) }
    var draggedStarterIdx by remember { mutableStateOf<Int?>(null) }
    var dragPointerInRoot by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current
    val snapRadiusPx = with(density) { 64.dp.toPx() }

    val subsRemaining = (maxSubs - subsUsed).coerceAtLeast(0)
    val canMakeSub = !isInMatch || subsRemaining > 0

    val isDragging = draggedBenchIdx != null || draggedStarterIdx != null

    val hoveredStarterIdx: Int? = if (!isDragging) {
        null
    } else {
        var bestIdx: Int? = null
        var bestDist = Float.MAX_VALUE
        starterBoundsInRoot.forEach { (idx, rect) ->
            if (idx != draggedStarterIdx) {
                val center = rect.center
                val dist = hypot(dragPointerInRoot.x - center.x, dragPointerInRoot.y - center.y)
                val expandedHit = rect.inflate(with(density) { 18.dp.toPx() }).contains(dragPointerInRoot)
                if ((expandedHit || dist <= snapRadiusPx) && dist < bestDist) {
                    bestDist = dist
                    bestIdx = idx
                }
            }
        }
        bestIdx
    }

    val finishDrag: () -> Unit = {
        val targetIdx = hoveredStarterIdx
        val bIdx = draggedBenchIdx
        val sIdx = draggedStarterIdx
        if (targetIdx != null) {
            when {
                bIdx != null && canMakeSub -> {
                    onSubstitute(targetIdx, bIdx)
                    selectedBenchIdx = null
                    selectedStarterIdx = null
                }
                sIdx != null && sIdx != targetIdx -> {
                    onSwapStarters(sIdx, targetIdx)
                    selectedStarterIdx = null
                    selectedBenchIdx = null
                }
            }
        }
        draggedBenchIdx = null
        draggedStarterIdx = null
    }

    val cancelDrag: () -> Unit = {
        draggedBenchIdx = null
        draggedStarterIdx = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .onGloballyPositioned { overlayOriginInRoot = it.positionInRoot() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            shape = RoundedCornerShape(22.dp),
            color = StadiumCard,
            border = BorderStroke(1.5.dp, TurfEmerald.copy(alpha = 0.50f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Single Compact Top Bar: Team + Formation Pills + Confirm + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TeamCrest(team = team, size = 30.dp)
                        Column {
                            Text(
                                text = "التشكيلة • ${team.nameAr}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isInMatch) {
                                    "متبقي $subsRemaining من $maxSubs تبديل"
                                } else {
                                    "اسحب البديل للملعب للتبديل فوراً"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (canMakeSub) TurfEmerald else CrimsonAlert
                            )
                        }
                    }

                    // Inline Formation Picker Pills
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
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
                                    .height(42.dp)
                                    .testTag("formation_${f.displayName}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) TurfEmerald else StadiumElevated,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) TurfEmerald else DividerSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = f.displayName,
                                        fontFamily = ChakraPetchFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selected) Color(0xFF042111) else Color.White
                                    )
                                    Text(
                                        text = f.styleAr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = if (selected) Color(0xFF062C17) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("tactics_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurfEmerald,
                            contentColor = Color(0xFF042111)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isInMatch) "تأكيد" else "حفظ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("tactics_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق الخطة",
                            tint = Color.White
                        )
                    }
                }

                // Main Widescreen Split: Horizontal Tactical Pitch + Drag-and-Drop Bench
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Horizontal Tactical Pitch Board (Left/Start or Right, takes ~64% width & FULL height!)
                    HorizontalTacticalPitchBoard(
                        team = team,
                        formation = formation,
                        starters = starters,
                        staminaByStarterIndex = staminaByStarterIndex,
                        selectedStarterIdx = selectedStarterIdx,
                        draggedStarterIdx = draggedStarterIdx,
                        hoveredStarterIdx = hoveredStarterIdx,
                        isDraggingBench = draggedBenchIdx != null,
                        onStarterBoundsChanged = { idx, rect -> starterBoundsInRoot[idx] = rect },
                        onStarterDragStart = { idx, rootPos ->
                            draggedStarterIdx = idx
                            draggedBenchIdx = null
                            dragPointerInRoot = rootPos
                        },
                        onStarterDragMove = { rootPos ->
                            dragPointerInRoot = rootPos
                        },
                        onStarterDragEnd = finishDrag,
                        onStarterDragCancel = cancelDrag,
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
                            .weight(1.52f)
                            .fillMaxHeight()
                    )

                    // Bench Panel (~36% width)
                    BenchColumnPanel(
                        team = team,
                        bench = bench,
                        selectedBenchIdx = selectedBenchIdx,
                        draggedBenchIdx = draggedBenchIdx,
                        canMakeSub = canMakeSub,
                        hintBannerText = when {
                            draggedBenchIdx != null && hoveredStarterIdx != null -> {
                                val target = starters.getOrNull(hoveredStarterIdx)
                                "اترك الإصبع للتبديل مع ${target?.name.orEmpty()}"
                            }
                            draggedBenchIdx != null -> "اسحب فوق أي لاعب في الملعب"
                            selectedBenchIdx != null -> "اضغط على لاعب في الملعب لينزل مكانه"
                            selectedStarterIdx != null -> "اضغط على بديل أو لاعب آخر للتبديل"
                            else -> "اسحب البديل إلى داخل التشكيلة"
                        },
                        onBenchDragStart = { idx, rootPos ->
                            if (canMakeSub) {
                                draggedBenchIdx = idx
                                draggedStarterIdx = null
                                dragPointerInRoot = rootPos
                            }
                        },
                        onBenchDragMove = { rootPos ->
                            dragPointerInRoot = rootPos
                        },
                        onBenchDragEnd = finishDrag,
                        onBenchDragCancel = cancelDrag,
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
                            .weight(0.88f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Floating Dragged Player Ghost Badge
        val draggedPlayer: SquadPlayer? = when {
            draggedBenchIdx != null -> bench.getOrNull(draggedBenchIdx!!)
            draggedStarterIdx != null -> starters.getOrNull(draggedStarterIdx!!)
            else -> null
        }
        if (draggedPlayer != null) {
            val localPos = dragPointerInRoot - overlayOriginInRoot
            val targetName = hoveredStarterIdx?.let { starters.getOrNull(it)?.name }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(20f)
                    .offset {
                        IntOffset(
                            (localPos.x - with(density) { 58.dp.toPx() }).roundToInt(),
                            (localPos.y - with(density) { 46.dp.toPx() }).roundToInt()
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                color = StadiumObsidian.copy(alpha = 0.96f),
                border = BorderStroke(
                    2.dp,
                    if (targetName != null) TurfEmerald else ChampionshipGold
                ),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (draggedPlayer.preferredRole == PlayerRole.GK) Color(0xFFFFB300) else team.primaryColor,
                        border = BorderStroke(1.5.dp, ChampionshipGold),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${draggedPlayer.jerseyNumber}",
                                fontFamily = ChakraPetchFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (team.primaryColor == Color(0xFFF5F7FA) && draggedPlayer.preferredRole != PlayerRole.GK) {
                                    Color(0xFF0E1726)
                                } else Color.White
                            )
                        }
                    }
                    Column {
                        Text(
                            text = draggedPlayer.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (targetName != null) "تبديل ⇄ $targetName" else "${roleLabelAr(draggedPlayer.preferredRole)} (${draggedPlayer.rating})",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (targetName != null) TurfEmerald else ChampionshipGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalTacticalPitchBoard(
    team: Team,
    formation: FormationType,
    starters: List<SquadPlayer>,
    staminaByStarterIndex: List<Float>?,
    selectedStarterIdx: Int?,
    draggedStarterIdx: Int?,
    hoveredStarterIdx: Int?,
    isDraggingBench: Boolean,
    onStarterBoundsChanged: (Int, Rect) -> Unit,
    onStarterDragStart: (Int, Offset) -> Unit,
    onStarterDragMove: (Offset) -> Unit,
    onStarterDragEnd: () -> Unit,
    onStarterDragCancel: () -> Unit,
    onStarterTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF092A18),
        border = BorderStroke(
            width = if (isDraggingBench) 2.dp else 1.dp,
            color = if (isDraggingBench) ChampionshipGold.copy(alpha = 0.7f) else TurfEmerald.copy(alpha = 0.4f)
        )
    ) {
        // Force LTR so GK is cleanly on Left and Attack is on Right, matching Engine pitch
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boardW = constraints.maxWidth.toFloat()
                val boardH = constraints.maxHeight.toFloat()

                // Horizontal Tactical Pitch Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stripeCount = 10
                    val stripeW = size.width / stripeCount
                    for (i in 0 until stripeCount) {
                        drawRect(
                            color = if (i % 2 == 0) Color(0xFF0E4B29) else Color(0xFF0B3D21),
                            topLeft = Offset(i * stripeW, 0f),
                            size = Size(stripeW + 1f, size.height)
                        )
                    }
                    val lineColor = Color.White.copy(alpha = 0.30f)
                    val pad = 12f
                    val fw = size.width - pad * 2
                    val fh = size.height - pad * 2
                    drawRect(lineColor, Offset(pad, pad), Size(fw, fh), style = Stroke(2.5f))
                    // Halfway line & center circle
                    drawLine(lineColor, Offset(size.width * 0.5f, pad), Offset(size.width * 0.5f, pad + fh), 2.5f)
                    drawCircle(lineColor, radius = fh * 0.18f, center = Offset(size.width * 0.5f, size.height * 0.5f), style = Stroke(2.5f))
                    // Left GK Penalty Box & Right Attacking Penalty Box
                    drawRect(lineColor, Offset(pad, size.height * 0.22f), Size(fw * 0.16f, size.height * 0.56f), style = Stroke(2.5f))
                    drawRect(lineColor, Offset(pad + fw * 0.84f, size.height * 0.22f), Size(fw * 0.16f, size.height * 0.56f), style = Stroke(2.5f))
                }

                // Direction Tag ("هجوم ➔")
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = StadiumObsidian.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = "الهجوم ←",
                        style = MaterialTheme.typography.labelSmall,
                        color = TurfEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Plot all 11 Starters cleanly across the full horizontal pitch
                formation.slots.forEachIndexed { idx, slot ->
                    val player = starters.getOrNull(idx) ?: return@forEachIndexed
                    val normX = ((slot.x - 4f) / 80f).coerceIn(0.08f, 0.92f)
                    val normY = ((slot.y - 6f) / 88f).coerceIn(0.11f, 0.89f)

                    val tokenW = 72.dp
                    val tokenH = 56.dp
                    val isSelected = selectedStarterIdx == idx
                    val isHovered = hoveredStarterIdx == idx
                    val isDraggedOut = draggedStarterIdx == idx
                    val stamina = staminaByStarterIndex?.getOrNull(idx)

                    var selfPosInRoot by remember(idx) { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .offset {
                                val halfW = (tokenW.toPx() * 0.5f)
                                val halfH = (tokenH.toPx() * 0.5f)
                                val px = (normX * boardW - halfW).roundToInt()
                                    .coerceIn(0, (boardW - tokenW.toPx()).roundToInt().coerceAtLeast(0))
                                val py = (normY * boardH - halfH).roundToInt()
                                    .coerceIn(0, (boardH - tokenH.toPx()).roundToInt().coerceAtLeast(0))
                                IntOffset(px, py)
                            }
                            .size(tokenW, tokenH)
                            .alpha(if (isDraggedOut) 0.35f else 1f)
                            .onGloballyPositioned { coords ->
                                selfPosInRoot = coords.positionInRoot()
                                onStarterBoundsChanged(idx, coords.boundsInRoot())
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(idx) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onStarterDragStart(idx, selfPosInRoot + localOffset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        onStarterDragMove(selfPosInRoot + change.position)
                                    },
                                    onDragEnd = onStarterDragEnd,
                                    onDragCancel = onStarterDragCancel
                                )
                            }
                            .clickable { onStarterTapped(idx) }
                            .testTag("tactics_starter_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val badgeColor = if (slot.role == PlayerRole.GK) Color(0xFFFFB300) else team.primaryColor
                                Surface(
                                    shape = CircleShape,
                                    color = badgeColor,
                                    border = BorderStroke(
                                        width = if (isHovered || isSelected) 3.dp else 1.5.dp,
                                        color = when {
                                            isHovered -> ChampionshipGold
                                            isSelected -> ElectricCyan
                                            else -> team.secondaryColor
                                        }
                                    ),
                                    modifier = Modifier.size(if (isHovered) 34.dp else 30.dp)
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

                                // Position slot tag pill
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 12.dp, y = (-2).dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isHovered) ChampionshipGold else StadiumObsidian.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = slot.code,
                                        fontFamily = ChakraPetchFontFamily,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHovered) Color(0xFF1F1600) else TurfEmerald,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            // Crisp Player Name + Optional Stamina Bar
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isHovered) ChampionshipGold else StadiumObsidian.copy(alpha = 0.90f),
                                border = if (isSelected || isHovered) {
                                    BorderStroke(1.dp, if (isHovered) ChampionshipGold else ElectricCyan)
                                } else null
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = player.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHovered) Color(0xFF1F1600) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (stamina != null) {
                                        Box(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .height(3.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.18f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(stamina.coerceIn(0f, 1f))
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
                                }
                            }
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
    draggedBenchIdx: Int?,
    canMakeSub: Boolean,
    hintBannerText: String,
    onBenchDragStart: (Int, Offset) -> Unit,
    onBenchDragMove: (Offset) -> Unit,
    onBenchDragEnd: () -> Unit,
    onBenchDragCancel: () -> Unit,
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
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دكة البدلاء (${bench.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = ChampionshipGold,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = TurfEmerald,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Live Drag / Tap Instruction Pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = StadiumObsidian.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.35f))
            ) {
                Text(
                    text = hintBannerText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD2E4DA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                bench.forEachIndexed { idx, player ->
                    val selected = selectedBenchIdx == idx
                    val isDraggingThis = draggedBenchIdx == idx
                    var cardPosInRoot by remember(idx) { mutableStateOf(Offset.Zero) }

                    Surface(
                        onClick = { onBenchTapped(idx) },
                        enabled = canMakeSub,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .alpha(if (isDraggingThis) 0.35f else if (canMakeSub) 1f else 0.55f)
                            .onGloballyPositioned { cardPosInRoot = it.positionInRoot() }
                            .pointerInput(idx, canMakeSub) {
                                if (!canMakeSub) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        onBenchDragStart(idx, cardPosInRoot + localOffset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        onBenchDragMove(cardPosInRoot + change.position)
                                    },
                                    onDragEnd = onBenchDragEnd,
                                    onDragCancel = onBenchDragCancel
                                )
                            }
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
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "اسحب اللاعب",
                                    tint = if (canMakeSub) ChampionshipGold.copy(alpha = 0.85f) else DividerSubtle,
                                    modifier = Modifier.size(18.dp)
                                )

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

                                Column(modifier = Modifier.weight(1f)) {
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
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.55f))
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
}

private fun roleLabelAr(role: PlayerRole): String = when (role) {
    PlayerRole.GK -> "حارس مرمى"
    PlayerRole.DEF -> "مدافع"
    PlayerRole.MID -> "وسط ملعب"
    PlayerRole.FWD -> "مهاجم"
}
