package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TurfEmerald
import kotlin.math.hypot

@Composable
fun MatchTouchControls(
    homeHasBall: Boolean,
    shotCharge: Float,
    onJoystickMove: (Float, Float) -> Unit,
    onSprintChanged: (Boolean) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    onDoublePressChanged: (Boolean) -> Unit,
    onShootPressed: () -> Unit,
    onShootReleased: () -> Unit,
    onSlideTackle: () -> Unit,
    onShortPassOrSwitch: () -> Unit,
    onThroughPassOrSlide: () -> Unit,
    onSwitchPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left: PES/FIFA Analog Joystick
        VirtualJoystick(
            onMove = onJoystickMove,
            modifier = Modifier.testTag("virtual_joystick")
        )

        // Right: Classic 4-Button PES/FIFA Cluster matching Attack & Defense layout
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top-Left (Above Pass/Switch):
                // Attack -> CROSS / FAST BALL | Defense -> HOLD or SWIPE UP for 2-PLAYER PRESS
                if (homeHasBall) {
                    TapActionCircle(
                        label = "عرضية",
                        accentColor = ElectricCyan,
                        sizeDp = 60,
                        onTap = onThroughPassOrSlide,
                        modifier = Modifier.testTag("cross_through_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                            contentDescription = "عرضية أو تمريرة سريعة",
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    DoublePressActionCircle(
                        sizeDp = 60,
                        accentColor = ElectricCyan,
                        onActiveChanged = onDoublePressChanged,
                        modifier = Modifier.testTag("double_press_button")
                    )
                }

                // Bottom-Left (Beside Sprint/Press): PASS (Attack) / SWITCH PLAYER (Defense)
                if (homeHasBall) {
                    TapActionCircle(
                        label = "تمرير",
                        accentColor = TurfEmerald,
                        sizeDp = 64,
                        onTap = onShortPassOrSwitch,
                        modifier = Modifier.testTag("pass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "تمرير",
                            tint = TurfEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    TapActionCircle(
                        label = "تبديل",
                        accentColor = ChampionshipGold,
                        sizeDp = 64,
                        onTap = onSwitchPlayer,
                        modifier = Modifier.testTag("switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "تبديل اللاعب",
                            tint = ChampionshipGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top-Right (Above Sprint/Press): SHOOT (Attack) / SLIDE TACKLE (Defense)
                if (homeHasBall) {
                    HoldActionCircle(
                        label = "تسديد",
                        accentColor = ChampionshipGold,
                        chargeProgress = shotCharge,
                        sizeDp = 58,
                        onPress = onShootPressed,
                        onRelease = onShootReleased,
                        modifier = Modifier.testTag("shoot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = "تسديد",
                            tint = ChampionshipGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    TapActionCircle(
                        label = "زحلقة",
                        accentColor = CrimsonAlert,
                        sizeDp = 58,
                        onTap = onSlideTackle,
                        modifier = Modifier.testTag("slide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "تزحلق لقطع الكرة",
                            tint = CrimsonAlert,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom-Right Primary (Big):
                // Works for SPRINT both with & without ball, AND activates PRESS/STEAL in defense!
                HoldActionCircle(
                    label = if (homeHasBall) "جري" else "جري/ضغط",
                    accentColor = if (homeHasBall) Color.White else TurfEmerald,
                    sizeDp = 76,
                    onPress = {
                        onSprintChanged(true)
                        if (!homeHasBall) onPressChanged(true)
                    },
                    onRelease = {
                        onSprintChanged(false)
                        onPressChanged(false)
                    },
                    modifier = Modifier.testTag("sprint_press_button")
                ) {
                    Icon(
                        imageVector = if (homeHasBall) Icons.Default.Bolt else Icons.Default.Shield,
                        contentDescription = if (homeHasBall) "جري بالكرة" else "جري وضغط لقطع الكرة",
                        tint = if (homeHasBall) Color.White else TurfEmerald,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DoublePressActionCircle(
    sizeDp: Int,
    accentColor: Color,
    onActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    var swipedUp by remember { mutableStateOf(false) }
    val active = pressed || swipedUp

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                if (swipedUp) ChampionshipGold.copy(alpha = 0.36f)
                else if (pressed) accentColor.copy(alpha = 0.32f)
                else Color(0xFF091710).copy(alpha = 0.62f)
            )
            .border(
                width = if (swipedUp) 2.4.dp else 1.8.dp,
                color = if (swipedUp) ChampionshipGold else accentColor.copy(alpha = 0.85f),
                shape = CircleShape
            )
            .pointerInput(onActiveChanged) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val startY = down.position.y
                    pressed = true
                    swipedUp = false
                    onActiveChanged(true)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        change.consume()
                        if (startY - change.position.y > 14f) {
                            swipedUp = true
                        }
                    }
                    pressed = false
                    swipedUp = false
                    onActiveChanged(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = "ضغط ثنائي بالضغط أو السحب لأعلى",
                    tint = if (swipedUp) ChampionshipGold else accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (swipedUp) ChampionshipGold else accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = if (active) "ضغط ×2" else "ثنائي ↑",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun VirtualJoystick(
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isActive by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(124.dp)
            .pointerInput(Unit) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width * 0.36f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isActive = true
                    fun updatePos(pos: Offset) {
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = hypot(dx, dy)
                        val clamped = if (dist > maxRadius && dist > 0f) {
                            Offset(dx / dist * maxRadius, dy / dist * maxRadius)
                        } else {
                            Offset(dx, dy)
                        }
                        knobOffset = clamped
                        onMove(
                            (clamped.x / maxRadius).coerceIn(-1f, 1f),
                            (clamped.y / maxRadius).coerceIn(-1f, 1f)
                        )
                    }
                    updatePos(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        change.consume()
                        updatePos(change.position)
                    }
                    isActive = false
                    knobOffset = Offset.Zero
                    onMove(0f, 0f)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension * 0.43f
            drawCircle(
                color = Color(0xFF0A1B12).copy(alpha = if (isActive) 0.58f else 0.34f),
                radius = outerR,
                center = c
            )
            drawCircle(
                color = if (isActive) ElectricCyan.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.32f),
                radius = outerR,
                center = c,
                style = Stroke(width = 2.6f)
            )
            drawCircle(
                color = if (isActive) ElectricCyan else Color.White.copy(alpha = 0.72f),
                radius = outerR * 0.40f,
                center = c + knobOffset
            )
        }
    }
}

@Composable
private fun HoldActionCircle(
    label: String,
    accentColor: Color,
    sizeDp: Int,
    chargeProgress: Float = 0f,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                if (pressed) accentColor.copy(alpha = 0.32f)
                else Color(0xFF091710).copy(alpha = 0.62f)
            )
            .border(1.8.dp, accentColor.copy(alpha = 0.85f), CircleShape)
            .pointerInput(onPress, onRelease) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    onPress()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        change.consume()
                    }
                    pressed = false
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (chargeProgress > 0.01f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * chargeProgress,
                    useCenter = false,
                    style = Stroke(width = 7f)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            iconContent()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TapActionCircle(
    label: String,
    accentColor: Color,
    sizeDp: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                if (pressed) accentColor.copy(alpha = 0.32f)
                else Color(0xFF091710).copy(alpha = 0.62f)
            )
            .border(1.8.dp, accentColor.copy(alpha = 0.85f), CircleShape)
            .pointerInput(onTap) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    onTap()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        change.consume()
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            iconContent()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
