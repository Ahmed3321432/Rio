package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FootballEngine
import com.example.model.BallEntity
import com.example.model.CrestPattern
import com.example.model.GraphicsQuality
import com.example.model.MatchState
import com.example.model.PlayerEntity
import com.example.model.PlayerRole
import com.example.model.Team
import com.example.model.TeamSide
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TurfEmerald
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun PitchCanvas(
    matchState: MatchState,
    graphicsQuality: GraphicsQuality,
    showPassIndicator: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val crowdDots = remember {
        List(128) { idx ->
            val row = idx / 32
            val col = idx % 32
            Triple(row, col / 31f, idx)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height

        // Broadcast Camera: ~82 virtual meters horizontally in landscape
        val visiblePitchSpanX = if (canvasW > canvasH) 84f else 56f
        val scale = max(canvasW / visiblePitchSpanX, canvasH / 76f)

        val shakePx = matchState.cameraShake * 6f
        val shakeX = if (matchState.cameraShake > 0.01f) sin(matchState.remainingSeconds * 95f) * shakePx else 0f
        val shakeY = if (matchState.cameraShake > 0.01f) cos(matchState.remainingSeconds * 110f) * shakePx else 0f

        val halfCanvasW = canvasW * 0.5f
        val halfCanvasH = canvasH * 0.5f

        val minTranslateX = canvasW - FootballEngine.PITCH_W * scale
        val minTranslateY = canvasH - FootballEngine.PITCH_H * scale
        val rawTransX = halfCanvasW - matchState.cameraX * scale
        val rawTransY = halfCanvasH - matchState.cameraY * scale
        val transX = rawTransX.coerceIn(minTranslateX.coerceAtMost(0f), 0f) + shakeX
        val transY = rawTransY.coerceIn(minTranslateY.coerceAtMost(0f), 0f) + shakeY

        // Stadium Night Backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF030A06), Color(0xFF08190F), Color(0xFF030A06))
            )
        )

        withTransform({
            translate(left = transX, top = transY)
        }) {
            fun sx(vx: Float): Float = vx * scale
            fun sy(vy: Float): Float = vy * scale

            // Top & Bottom Stadium Grandstands
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF08100C), Color(0xFF102017))
                ),
                topLeft = Offset(0f, 0f),
                size = Size(sx(FootballEngine.PITCH_W), sy(FootballEngine.FIELD_TOP))
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF102017), Color(0xFF08100C))
                ),
                topLeft = Offset(0f, sy(FootballEngine.FIELD_BOTTOM)),
                size = Size(sx(FootballEngine.PITCH_W), sy(FootballEngine.PITCH_H - FootballEngine.FIELD_BOTTOM))
            )

            // LED Perimeter Ad-boards along top & bottom touchlines
            val ledCount = 12
            val ledW = sx(FootballEngine.FIELD_RIGHT - FootballEngine.FIELD_LEFT) / ledCount
            for (i in 0 until ledCount) {
                val ledColor = when {
                    i % 3 == 0 -> matchState.homeTeam.primaryColor
                    i % 3 == 1 -> matchState.awayTeam.primaryColor
                    else -> ChampionshipGold
                }
                drawRoundRect(
                    color = ledColor.copy(alpha = 0.80f),
                    topLeft = Offset(sx(FootballEngine.FIELD_LEFT) + i * ledW + 2f, sy(5.4f)),
                    size = Size(ledW - 4f, sy(1.6f)),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                if (graphicsQuality != GraphicsQuality.LOW) {
                    drawRoundRect(
                        color = ledColor.copy(alpha = 0.55f),
                        topLeft = Offset(sx(FootballEngine.FIELD_LEFT) + i * ledW + 2f, sy(FootballEngine.FIELD_BOTTOM + 1.0f)),
                        size = Size(ledW - 4f, sy(1.4f)),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }

            // Animated Cheering Crowd in Grandstand (High Graphics)
            if (graphicsQuality == GraphicsQuality.HIGH) {
                val goalBuzz = if (matchState.netRippleLeft > 0f || matchState.netRippleRight > 0f) 2.1f else 0.4f
                crowdDots.forEach { (row, t, idx) ->
                    val cheer = sin(matchState.remainingSeconds * 7f + idx) * goalBuzz
                    val color = when {
                        idx % 4 == 0 -> matchState.homeTeam.primaryColor.copy(alpha = 0.82f)
                        idx % 4 == 1 -> matchState.awayTeam.primaryColor.copy(alpha = 0.82f)
                        idx % 4 == 2 -> Color(0xFFFFD54F).copy(alpha = 0.65f)
                        else -> Color(0xFFE0ECE5).copy(alpha = 0.45f)
                    }
                    drawCircle(
                        color = color,
                        radius = 0.65f * scale,
                        center = Offset(sx(4f + t * 152f), sy(1.1f + row * 1.05f) + cheer)
                    )
                }
            }

            // Pitch Track / Apron
            drawRoundRect(
                color = Color(0xFF0A391E),
                topLeft = Offset(sx(1.5f), sy(7.0f)),
                size = Size(sx(FootballEngine.PITCH_W - 3f), sy(FootballEngine.PITCH_H - 9.0f)),
                cornerRadius = CornerRadius(2.4f * scale, 2.4f * scale)
            )

            // Playable Turf Field with rich mowed grass stripes
            val fieldL = sx(FootballEngine.FIELD_LEFT)
            val fieldT = sy(FootballEngine.FIELD_TOP)
            val fieldW = sx(FootballEngine.FIELD_RIGHT - FootballEngine.FIELD_LEFT)
            val fieldH = sy(FootballEngine.FIELD_BOTTOM - FootballEngine.FIELD_TOP)

            val stripeCount = if (graphicsQuality == GraphicsQuality.LOW) 8 else 18
            val stripeW = fieldW / stripeCount
            for (i in 0 until stripeCount) {
                drawRect(
                    color = if (i % 2 == 0) Color(0xFF178345) else Color(0xFF126E39),
                    topLeft = Offset(fieldL + i * stripeW, fieldT),
                    size = Size(stripeW + 0.8f, fieldH)
                )
            }

            // Cross-cut faint horizontal grass sheen on Medium/High
            if (graphicsQuality != GraphicsQuality.LOW) {
                val hBands = 10
                val bandH = fieldH / hBands
                for (r in 0 until hBands) {
                    if (r % 2 == 0) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.032f),
                            topLeft = Offset(fieldL, fieldT + r * bandH),
                            size = Size(fieldW, bandH)
                        )
                    }
                }
                // Subtle worn penalty-box turf shading
                drawOval(
                    color = Color(0xFF1A6032).copy(alpha = 0.28f),
                    topLeft = Offset(fieldL, sy(40f)),
                    size = Size(sx(9f), sy(20f))
                )
                drawOval(
                    color = Color(0xFF1A6032).copy(alpha = 0.28f),
                    topLeft = Offset(sx(FootballEngine.FIELD_RIGHT - 9f), sy(40f)),
                    size = Size(sx(9f), sy(20f))
                )
            }

            // Stadium Floodlight Beams & Sheens (Medium/High)
            if (graphicsQuality != GraphicsQuality.LOW) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF6FFF8).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(sx(38f), sy(14f)),
                        radius = sx(68f)
                    ),
                    topLeft = Offset(fieldL, fieldT),
                    size = Size(fieldW, fieldH)
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF6FFF8).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(sx(122f), sy(14f)),
                        radius = sx(68f)
                    ),
                    topLeft = Offset(fieldL, fieldT),
                    size = Size(fieldW, fieldH)
                )
            }
            if (graphicsQuality == GraphicsQuality.HIGH) {
                val beamLeft = Path().apply {
                    moveTo(sx(12f), sy(0f))
                    lineTo(sx(58f), sy(64f))
                    lineTo(sx(24f), sy(70f))
                    close()
                }
                drawPath(
                    beamLeft,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                val beamRight = Path().apply {
                    moveTo(sx(148f), sy(0f))
                    lineTo(sx(136f), sy(70f))
                    lineTo(sx(102f), sy(64f))
                    close()
                }
                drawPath(
                    beamRight,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
            }

            // Crisp Chalk-White Pitch Markings
            val lineColor = Color.White.copy(alpha = 0.90f)
            val lineW = (0.36f * scale).coerceAtLeast(1.8f)

            drawRect(
                color = lineColor,
                topLeft = Offset(fieldL, fieldT),
                size = Size(fieldW, fieldH),
                style = Stroke(width = lineW)
            )

            // Halfway line & Center Circle
            drawLine(
                color = lineColor,
                start = Offset(sx(80f), fieldT),
                end = Offset(sx(80f), fieldT + fieldH),
                strokeWidth = lineW
            )
            drawCircle(
                color = lineColor,
                radius = sx(12.5f),
                center = Offset(sx(80f), sy(50f)),
                style = Stroke(width = lineW)
            )
            drawCircle(color = lineColor, radius = sx(0.9f), center = Offset(sx(80f), sy(50f)))

            // Left Penalty Area & Six-Yard Box
            drawRect(
                color = lineColor,
                topLeft = Offset(fieldL, sy(26f)),
                size = Size(sx(22f), sy(48f)),
                style = Stroke(width = lineW)
            )
            drawRect(
                color = lineColor,
                topLeft = Offset(fieldL, sy(37f)),
                size = Size(sx(7.5f), sy(26f)),
                style = Stroke(width = lineW)
            )
            drawCircle(color = lineColor, radius = sx(0.65f), center = Offset(sx(22.5f), sy(50f)))
            drawArc(
                color = lineColor,
                startAngle = -55f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(sx(18.5f), sy(41f)),
                size = Size(sx(16f), sy(18f)),
                style = Stroke(width = lineW)
            )

            // Right Penalty Area & Six-Yard Box
            drawRect(
                color = lineColor,
                topLeft = Offset(sx(FootballEngine.FIELD_RIGHT - 22f), sy(26f)),
                size = Size(sx(22f), sy(48f)),
                style = Stroke(width = lineW)
            )
            drawRect(
                color = lineColor,
                topLeft = Offset(sx(FootballEngine.FIELD_RIGHT - 7.5f), sy(37f)),
                size = Size(sx(7.5f), sy(26f)),
                style = Stroke(width = lineW)
            )
            drawCircle(color = lineColor, radius = sx(0.65f), center = Offset(sx(FootballEngine.FIELD_RIGHT - 14.5f), sy(50f)))
            drawArc(
                color = lineColor,
                startAngle = 125f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(sx(FootballEngine.FIELD_RIGHT - 34.5f), sy(41f)),
                size = Size(sx(16f), sy(18f)),
                style = Stroke(width = lineW)
            )

            // Corner Arcs & Corner Flags
            val cR = sx(2.8f)
            drawArc(lineColor, 0f, 90f, false, Offset(fieldL - cR, fieldT - cR), Size(cR * 2, cR * 2), style = Stroke(lineW))
            drawArc(lineColor, 90f, 90f, false, Offset(fieldL + fieldW - cR, fieldT - cR), Size(cR * 2, cR * 2), style = Stroke(lineW))
            drawArc(lineColor, 270f, 90f, false, Offset(fieldL - cR, fieldT + fieldH - cR), Size(cR * 2, cR * 2), style = Stroke(lineW))
            drawArc(lineColor, 180f, 90f, false, Offset(fieldL + fieldW - cR, fieldT + fieldH - cR), Size(cR * 2, cR * 2), style = Stroke(lineW))

            val flutter = sin(matchState.remainingSeconds * 6f) * 0.4f * scale
            drawCornerFlag(fieldL, fieldT, scale, matchState.homeTeam.primaryColor, flutter)
            drawCornerFlag(fieldL, fieldT + fieldH, scale, matchState.homeTeam.primaryColor, flutter)
            drawCornerFlag(fieldL + fieldW, fieldT, scale, matchState.awayTeam.primaryColor, -flutter)
            drawCornerFlag(fieldL + fieldW, fieldT + fieldH, scale, matchState.awayTeam.primaryColor, -flutter)

            // Left & Right Goals with 3D stanchions & rippling nets
            drawHorizontalGoal(
                isLeft = true,
                ripple = matchState.netRippleLeft,
                timeSec = matchState.remainingSeconds,
                scale = scale,
                highQuality = graphicsQuality == GraphicsQuality.HIGH
            )
            drawHorizontalGoal(
                isLeft = false,
                ripple = matchState.netRippleRight,
                timeSec = matchState.remainingSeconds,
                scale = scale,
                highQuality = graphicsQuality == GraphicsQuality.HIGH
            )

            // Ball Motion Trail
            if (graphicsQuality != GraphicsQuality.LOW && matchState.ball.trail.isNotEmpty()) {
                val tSize = matchState.ball.trail.size
                matchState.ball.trail.forEachIndexed { i, (tx, ty) ->
                    val alpha = ((tSize - i).toFloat() / tSize) * 0.36f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = alpha), Color.Transparent)
                        ),
                        radius = sx(FootballEngine.BALL_RADIUS * (1.05f - i * 0.08f)).coerceAtLeast(1f),
                        center = Offset(sx(tx), sy(ty))
                    )
                }
            }

            // Pass-Aim Line & Ring (controlled by settings.showPassIndicator)
            if (showPassIndicator) {
                val activeHome = matchState.players.firstOrNull { it.id == matchState.activeHomePlayerId }
                val passTarget = matchState.passTargetHomeId?.let { tid -> matchState.players.firstOrNull { it.id == tid } }
                if (activeHome != null && passTarget != null && matchState.homeHasBall) {
                    drawLine(
                        color = ChampionshipGold.copy(alpha = 0.42f),
                        start = Offset(sx(activeHome.x), sy(activeHome.y + 1.4f)),
                        end = Offset(sx(passTarget.x), sy(passTarget.y + 1.4f)),
                        strokeWidth = (0.36f * scale).coerceAtLeast(1.8f),
                        cap = StrokeCap.Round
                    )
                    drawOval(
                        color = ChampionshipGold.copy(alpha = 0.65f),
                        topLeft = Offset(sx(passTarget.x - 2.6f), sy(passTarget.y + 0.8f)),
                        size = Size(sx(5.2f), sy(2.1f)),
                        style = Stroke(width = (0.30f * scale).coerceAtLeast(1.5f))
                    )
                }
            }

            // Players sorted by Y so lower players overlap naturally in 2.5D broadcast view
            matchState.players.sortedBy { it.y }.forEach { player ->
                val team = if (player.teamSide == TeamSide.HOME) matchState.homeTeam else matchState.awayTeam
                val isControlled = player.id == matchState.activeHomePlayerId
                val isPassTarget = showPassIndicator && (player.id == matchState.passTargetHomeId)
                drawPlayerBroadcast(
                    player = player,
                    team = team,
                    isControlled = isControlled,
                    isPassTarget = isPassTarget,
                    shotCharge = if (isControlled && matchState.isChargingShot) matchState.shotCharge else 0f,
                    scale = scale,
                    graphicsQuality = graphicsQuality,
                    textMeasurer = textMeasurer
                )
            }

            // Match Ball
            drawBallBroadcast(matchState.ball, scale, graphicsQuality)

            // Particles
            matchState.particles.forEach { pt ->
                val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
                drawCircle(
                    color = pt.color.copy(alpha = alpha),
                    radius = pt.size * scale,
                    center = Offset(sx(pt.x), sy(pt.y))
                )
            }
        }
    }
}

private fun DrawScope.drawCornerFlag(
    x: Float,
    y: Float,
    scale: Float,
    flagColor: Color,
    flutter: Float
) {
    val poleH = 2.4f * scale
    drawLine(
        color = Color(0xFFEAEFF2),
        start = Offset(x, y),
        end = Offset(x, y - poleH),
        strokeWidth = (0.18f * scale).coerceAtLeast(1.4f),
        cap = StrokeCap.Round
    )
    val flagPath = Path().apply {
        moveTo(x, y - poleH)
        lineTo(x + 1.3f * scale, y - poleH + 0.35f * scale + flutter)
        lineTo(x, y - poleH + 0.8f * scale)
        close()
    }
    drawPath(flagPath, color = flagColor)
}

@Composable
fun PitchRadarOverlay(
    matchState: MatchState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(156.dp)
            .height(92.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF07170E).copy(alpha = 0.74f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = 6f
            val rw = size.width - pad * 2
            val rh = size.height - pad * 2
            fun rx(vx: Float) = pad + ((vx - FootballEngine.FIELD_LEFT) / (FootballEngine.FIELD_RIGHT - FootballEngine.FIELD_LEFT)) * rw
            fun ry(vy: Float) = pad + ((vy - FootballEngine.FIELD_TOP) / (FootballEngine.FIELD_BOTTOM - FootballEngine.FIELD_TOP)) * rh

            drawRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(pad, pad),
                size = Size(rw, rh),
                style = Stroke(width = 1.2f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.30f),
                start = Offset(pad + rw * 0.5f, pad),
                end = Offset(pad + rw * 0.5f, pad + rh),
                strokeWidth = 1f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = rh * 0.14f,
                center = Offset(pad + rw * 0.5f, pad + rh * 0.5f),
                style = Stroke(width = 1f)
            )

            val camLeft = rx((matchState.cameraX - 38f).coerceAtLeast(FootballEngine.FIELD_LEFT))
            val camRight = rx((matchState.cameraX + 38f).coerceAtMost(FootballEngine.FIELD_RIGHT))
            drawRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = Offset(camLeft, pad),
                size = Size((camRight - camLeft).coerceAtLeast(10f), rh)
            )

            matchState.players.forEach { p ->
                val isActive = p.id == matchState.activeHomePlayerId
                val dotColor = if (p.teamSide == TeamSide.HOME) {
                    if (isActive) ElectricCyan else TurfEmerald
                } else {
                    ChampionshipGold
                }
                drawCircle(
                    color = dotColor,
                    radius = if (isActive) 4.2f else 2.8f,
                    center = Offset(rx(p.x), ry(p.y))
                )
            }

            drawCircle(
                color = Color.White,
                radius = 3.2f,
                center = Offset(rx(matchState.ball.x), ry(matchState.ball.y))
            )
        }
    }
}

private fun DrawScope.drawHorizontalGoal(
    isLeft: Boolean,
    ripple: Float,
    timeSec: Float,
    scale: Float,
    highQuality: Boolean
) {
    val gTop = FootballEngine.GOAL_TOP * scale
    val gBottom = FootballEngine.GOAL_BOTTOM * scale
    val gHeight = gBottom - gTop
    val depth = FootballEngine.GOAL_NET_DEPTH * scale
    val xLine = if (isLeft) FootballEngine.FIELD_LEFT * scale else FootballEngine.FIELD_RIGHT * scale
    val xBack = if (isLeft) xLine - depth else xLine + depth
    val wave = if (ripple > 0.01f) sin(timeSec * 38f) * ripple * 1.4f * scale else 0f

    // Ground net shadow
    val netRectLeft = kotlin.math.min(xLine, xBack)
    drawRect(
        brush = Brush.horizontalGradient(
            if (isLeft) listOf(Color.Black.copy(alpha = 0.25f), Color.White.copy(alpha = 0.16f + ripple * 0.16f))
            else listOf(Color.White.copy(alpha = 0.16f + ripple * 0.16f), Color.Black.copy(alpha = 0.25f)),
            startX = netRectLeft,
            endX = netRectLeft + depth
        ),
        topLeft = Offset(netRectLeft, gTop),
        size = Size(depth, gHeight)
    )

    val rows = if (highQuality) 11 else 6
    val stepY = gHeight / rows
    for (i in 1 until rows) {
        val y = gTop + i * stepY
        drawLine(
            color = Color.White.copy(alpha = 0.38f),
            start = Offset(xLine, y),
            end = Offset(xBack + wave, y + wave * 0.28f),
            strokeWidth = 1.1f
        )
    }
    for (c in 1..4) {
        val t = c / 4f
        val rx = xLine + (xBack - xLine) * t + wave * t
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(rx, gTop),
            end = Offset(rx, gBottom),
            strokeWidth = 1.1f
        )
    }

    // 3D Back stanchions & crisp metallic posts
    val frameStroke = (0.68f * scale).coerceAtLeast(3.0f)
    val framePath = Path().apply {
        moveTo(xLine, gTop)
        lineTo(xBack + wave * 0.4f, gTop + 0.5f * scale)
        lineTo(xBack + wave * 0.4f, gBottom - 0.5f * scale)
        lineTo(xLine, gBottom)
    }
    drawPath(framePath, color = Color(0xFFD6E0DA), style = Stroke(width = frameStroke * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(
        color = Color.White,
        start = Offset(xLine, gTop),
        end = Offset(xLine, gBottom),
        strokeWidth = frameStroke * 0.38f
    )
    drawCircle(color = Color(0xFFCFD8D3), radius = FootballEngine.POST_RADIUS * scale * 1.15f, center = Offset(xLine, gTop))
    drawCircle(color = Color.White, radius = FootballEngine.POST_RADIUS * scale * 0.82f, center = Offset(xLine, gTop))
    drawCircle(color = Color(0xFFCFD8D3), radius = FootballEngine.POST_RADIUS * scale * 1.15f, center = Offset(xLine, gBottom))
    drawCircle(color = Color.White, radius = FootballEngine.POST_RADIUS * scale * 0.82f, center = Offset(xLine, gBottom))
}

private fun DrawScope.drawPlayerBroadcast(
    player: PlayerEntity,
    team: Team,
    isControlled: Boolean,
    isPassTarget: Boolean,
    shotCharge: Float,
    scale: Float,
    graphicsQuality: GraphicsQuality,
    textMeasurer: TextMeasurer
) {
    val px = player.x * scale
    val py = player.y * scale
    val u = FootballEngine.PLAYER_RADIUS * scale * 1.06f

    val footY = py + u * 0.98f

    // Ground Contact Shadow
    if (graphicsQuality != GraphicsQuality.LOW) {
        drawOval(
            brush = Brush.radialGradient(
                listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent),
                center = Offset(px, footY + u * 0.05f),
                radius = u * 1.1f
            ),
            topLeft = Offset(px - u * 1.05f, footY - u * 0.30f),
            size = Size(u * 2.1f, u * 0.70f)
        )
    }

    // Active Player Selection Ring + Stamina Arc + Overhead Cursor + Shot Gauge
    if (isControlled) {
        drawOval(
            color = ElectricCyan.copy(alpha = 0.86f),
            topLeft = Offset(px - u * 1.38f, footY - u * 0.46f),
            size = Size(u * 2.76f, u * 1.08f),
            style = Stroke(width = (0.36f * scale).coerceAtLeast(2.1f))
        )
        drawArc(
            color = if (player.stamina > 0.28f) TurfEmerald else Color(0xFFFF455F),
            startAngle = 18f,
            sweepAngle = 144f * player.stamina,
            useCenter = false,
            topLeft = Offset(px - u * 1.60f, footY - u * 0.60f),
            size = Size(u * 3.2f, u * 1.34f),
            style = Stroke(width = (0.32f * scale).coerceAtLeast(2f), cap = StrokeCap.Round)
        )
        val cursorY = py - u * 2.02f
        val triPath = Path().apply {
            moveTo(px, cursorY + u * 0.52f)
            lineTo(px - u * 0.50f, cursorY - u * 0.22f)
            lineTo(px + u * 0.50f, cursorY - u * 0.22f)
            close()
        }
        drawPath(triPath, color = ElectricCyan)
        drawPath(triPath, color = Color(0xFF042228), style = Stroke(width = 1.2f))

        if (shotCharge > 0.02f) {
            val barW = u * 2.5f
            val barH = (u * 0.36f).coerceAtLeast(4.5f)
            val barX = px - barW * 0.5f
            val barY = cursorY - u * 0.76f
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.76f),
                topLeft = Offset(barX, barY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(4f, 4f)
            )
            val fillColor = if (shotCharge > 0.8f) Color(0xFFFF455F) else ChampionshipGold
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(barX, barY),
                size = Size(barW * shotCharge, barH),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    } else if (isPassTarget) {
        val cursorY = py - u * 1.92f
        val triPath = Path().apply {
            moveTo(px, cursorY + u * 0.44f)
            lineTo(px - u * 0.40f, cursorY - u * 0.18f)
            lineTo(px + u * 0.40f, cursorY - u * 0.18f)
            close()
        }
        drawPath(triPath, color = ChampionshipGold.copy(alpha = 0.92f))
    }

    // Kit & Appearance Palette
    val kitPrimary = if (player.role == PlayerRole.GK) {
        if (player.teamSide == TeamSide.HOME) Color(0xFFFFB300) else Color(0xFF9C27B0)
    } else team.primaryColor
    val kitSecondary = if (player.role == PlayerRole.GK) Color(0xFF1A1A1A) else team.secondaryColor
    val shortsColor = if (player.role == PlayerRole.GK) Color(0xFF161B22) else team.secondaryColor

    val skinColor = when (player.skinToneIndex % 3) {
        0 -> Color(0xFFF0C2A2)
        1 -> Color(0xFFD1956C)
        else -> Color(0xFF7E4F34)
    }
    val skinShade = when (player.skinToneIndex % 3) {
        0 -> Color(0xFFD59F7C)
        1 -> Color(0xFFB3764E)
        else -> Color(0xFF5D3721)
    }
    val hairColor = when (player.hairStyleIndex % 4) {
        2 -> Color(0xFF4A311D) // Chestnut
        else -> Color(0xFF171412) // Deep dark
    }

    val facingSign = if (cos(player.facingAngle) >= 0f) 1f else -1f
    val isSliding = player.slideTimer > 0f
    val isKicking = player.kickAnimTimer > 0f
    val bodyLeanDeg = when {
        isSliding -> -facingSign * 60f
        isKicking -> facingSign * 15f
        else -> facingSign * 6f
    }
    val strideSwing = if (isSliding) 0.58f else sin(player.stridePhase) * 0.52f

    withTransform({
        rotate(degrees = bodyLeanDeg, pivot = Offset(px, footY - u * 0.4f))
    }) {
        val hipY = py + u * 0.22f
        val torsoTopY = py - u * 0.64f
        val shoulderHalfW = u * 0.58f
        val waistHalfW = u * 0.40f

        // --- ARTICULATED LEGS, SOCKS & CLEATS ---
        val leftHipX = px - u * 0.20f
        val rightHipX = px + u * 0.20f
        val leftFootX = leftHipX + strideSwing * u * 0.74f
        val rightFootX = rightHipX - strideSwing * u * 0.74f + (if (isKicking) facingSign * u * 0.56f else 0f)
        val leftFootY = footY - (if (strideSwing > 0f) strideSwing * u * 0.18f else 0f)
        val rightFootY = footY - (if (isKicking) u * 0.30f else if (strideSwing < 0f) -strideSwing * u * 0.18f else 0f)

        val leftKneeX = (leftHipX + leftFootX) * 0.5f + facingSign * u * 0.10f
        val leftKneeY = (hipY + leftFootY) * 0.5f
        val rightKneeX = (rightHipX + rightFootX) * 0.5f + facingSign * u * 0.12f
        val rightKneeY = (hipY + rightFootY) * 0.5f

        val legW = (u * 0.24f).coerceAtLeast(2.4f)
        // Thighs
        drawLine(skinColor, Offset(leftHipX, hipY), Offset(leftKneeX, leftKneeY), strokeWidth = legW, cap = StrokeCap.Round)
        drawLine(skinColor, Offset(rightHipX, hipY), Offset(rightKneeX, rightKneeY), strokeWidth = legW, cap = StrokeCap.Round)
        // Team Socks + Cuff Stripe
        drawLine(kitPrimary, Offset(leftKneeX, leftKneeY), Offset(leftFootX, leftFootY - u * 0.08f), strokeWidth = legW * 1.08f, cap = StrokeCap.Round)
        drawLine(kitPrimary, Offset(rightKneeX, rightKneeY), Offset(rightFootX, rightFootY - u * 0.08f), strokeWidth = legW * 1.08f, cap = StrokeCap.Round)
        drawCircle(kitSecondary, radius = legW * 0.52f, center = Offset(leftKneeX, leftKneeY))
        drawCircle(kitSecondary, radius = legW * 0.52f, center = Offset(rightKneeX, rightKneeY))

        // Pro Football Cleats
        val bootColor = if (player.teamSide == TeamSide.HOME) Color(0xFF111824) else Color(0xFF1F1616)
        val bootAccent = if (player.teamSide == TeamSide.HOME) ElectricCyan else ChampionshipGold
        drawOval(bootColor, Offset(leftFootX - u * 0.16f, leftFootY - u * 0.15f), Size(u * 0.38f, u * 0.21f))
        drawOval(bootAccent.copy(alpha = 0.7f), Offset(leftFootX - u * 0.08f, leftFootY - u * 0.12f), Size(u * 0.14f, u * 0.08f))
        drawOval(bootColor, Offset(rightFootX - u * 0.16f, rightFootY - u * 0.15f), Size(u * 0.38f, u * 0.21f))
        drawOval(bootAccent.copy(alpha = 0.7f), Offset(rightFootX - u * 0.08f, rightFootY - u * 0.12f), Size(u * 0.14f, u * 0.08f))

        // --- SHORTS WITH SIDE STRIPE ---
        val shortsPath = Path().apply {
            moveTo(px - waistHalfW, hipY - u * 0.12f)
            lineTo(px + waistHalfW, hipY - u * 0.12f)
            lineTo(px + waistHalfW * 1.16f, hipY + u * 0.28f)
            lineTo(px + u * 0.04f, hipY + u * 0.28f)
            lineTo(px, hipY + u * 0.12f)
            lineTo(px - u * 0.04f, hipY + u * 0.28f)
            lineTo(px - waistHalfW * 1.16f, hipY + u * 0.28f)
            close()
        }
        drawPath(shortsPath, color = shortsColor)
        drawPath(shortsPath, color = Color.Black.copy(alpha = 0.45f), style = Stroke(width = 1.1f))

        // --- ARMS & RAGLAN SLEEVES ---
        val armSwing = -strideSwing * u * 0.52f
        val armW = (u * 0.20f).coerceAtLeast(2.2f)
        val lShoulder = Offset(px - shoulderHalfW * 0.90f, torsoTopY + u * 0.12f)
        val rShoulder = Offset(px + shoulderHalfW * 0.90f, torsoTopY + u * 0.12f)
        val lElbow = Offset(px - shoulderHalfW * 1.10f + armSwing * 0.45f, (torsoTopY + hipY) * 0.5f)
        val rElbow = Offset(px + shoulderHalfW * 1.10f - armSwing * 0.45f, (torsoTopY + hipY) * 0.5f)
        val lHand = Offset(px - shoulderHalfW * 1.20f + armSwing, hipY - u * 0.04f)
        val rHand = Offset(px + shoulderHalfW * 1.20f - armSwing, hipY - u * 0.04f)

        drawLine(skinColor, lElbow, lHand, strokeWidth = armW, cap = StrokeCap.Round)
        drawLine(skinColor, rElbow, rHand, strokeWidth = armW, cap = StrokeCap.Round)
        drawLine(kitPrimary, lShoulder, lElbow, strokeWidth = armW * 1.28f, cap = StrokeCap.Round)
        drawLine(kitPrimary, rShoulder, rElbow, strokeWidth = armW * 1.28f, cap = StrokeCap.Round)
        if (player.role == PlayerRole.GK) {
            drawCircle(color = Color(0xFFFFEA00), radius = u * 0.19f, center = lHand)
            drawCircle(color = Color(0xFFFFEA00), radius = u * 0.19f, center = rHand)
            drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = u * 0.19f, center = lHand, style = Stroke(1f))
            drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = u * 0.19f, center = rHand, style = Stroke(1f))
        }

        // --- TAPERED PRO JERSEY TORSO WITH CLUB KIT PATTERN ---
        val torsoPath = Path().apply {
            moveTo(px - shoulderHalfW, torsoTopY)
            lineTo(px + shoulderHalfW, torsoTopY)
            lineTo(px + waistHalfW, hipY - u * 0.06f)
            lineTo(px - waistHalfW, hipY - u * 0.06f)
            close()
        }
        clipPath(torsoPath) {
            drawRect(
                color = kitPrimary,
                topLeft = Offset(px - shoulderHalfW, torsoTopY),
                size = Size(shoulderHalfW * 2, hipY - torsoTopY)
            )
            if (player.role != PlayerRole.GK) {
                when (team.crestPattern) {
                    CrestPattern.STRIPES -> {
                        val sw = shoulderHalfW * 0.36f
                        drawRect(kitSecondary.copy(alpha = 0.85f), Offset(px - sw * 2.1f, torsoTopY), Size(sw, hipY - torsoTopY))
                        drawRect(kitSecondary.copy(alpha = 0.85f), Offset(px - sw * 0.5f, torsoTopY), Size(sw, hipY - torsoTopY))
                        drawRect(kitSecondary.copy(alpha = 0.85f), Offset(px + sw * 1.1f, torsoTopY), Size(sw, hipY - torsoTopY))
                    }
                    CrestPattern.SASH -> {
                        val sash = Path().apply {
                            moveTo(px - shoulderHalfW, torsoTopY)
                            lineTo(px - shoulderHalfW * 0.45f, torsoTopY)
                            lineTo(px + waistHalfW, hipY)
                            lineTo(px + waistHalfW * 0.45f, hipY)
                            close()
                        }
                        drawPath(sash, color = kitSecondary.copy(alpha = 0.88f))
                    }
                    CrestPattern.HALF -> {
                        drawRect(
                            color = kitSecondary.copy(alpha = 0.90f),
                            topLeft = Offset(px, torsoTopY),
                            size = Size(shoulderHalfW, hipY - torsoTopY)
                        )
                    }
                    CrestPattern.HOOPS -> {
                        val hh = (hipY - torsoTopY) * 0.26f
                        drawRect(kitSecondary.copy(alpha = 0.85f), Offset(px - shoulderHalfW, torsoTopY + hh), Size(shoulderHalfW * 2, hh * 0.72f))
                        drawRect(kitSecondary.copy(alpha = 0.85f), Offset(px - shoulderHalfW, torsoTopY + hh * 2.4f), Size(shoulderHalfW * 2, hh * 0.72f))
                    }
                    CrestPattern.CHEVRON -> {
                        val chev = Path().apply {
                            moveTo(px - shoulderHalfW, torsoTopY + u * 0.10f)
                            lineTo(px, torsoTopY + u * 0.42f)
                            lineTo(px + shoulderHalfW, torsoTopY + u * 0.10f)
                            lineTo(px + shoulderHalfW, torsoTopY + u * 0.28f)
                            lineTo(px, torsoTopY + u * 0.60f)
                            lineTo(px - shoulderHalfW, torsoTopY + u * 0.28f)
                            close()
                        }
                        drawPath(chev, color = kitSecondary.copy(alpha = 0.88f))
                    }
                    CrestPattern.STAR -> {
                        drawRect(
                            color = kitSecondary.copy(alpha = 0.82f),
                            topLeft = Offset(px - shoulderHalfW, torsoTopY + u * 0.26f),
                            size = Size(shoulderHalfW * 2, u * 0.20f)
                        )
                    }
                }
            }
            // V-Neck collar
            drawArc(
                color = skinColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(px - u * 0.18f, torsoTopY - u * 0.12f),
                size = Size(u * 0.36f, u * 0.24f)
            )
            // Subtle muscle/fabric torso gradient shadow
            if (graphicsQuality != GraphicsQuality.LOW) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.12f))
                    ),
                    topLeft = Offset(px - shoulderHalfW, torsoTopY),
                    size = Size(shoulderHalfW * 2, hipY - torsoTopY)
                )
            }
        }
        drawPath(
            torsoPath,
            color = if (player.stunTimer > 0f) Color(0xFFFF455F) else Color.Black.copy(alpha = 0.55f),
            style = Stroke(width = 1.4f)
        )

        // --- NECK, DETAILED HEAD & 4 DISTINCT HAIRSTYLES ---
        val headCenter = Offset(px + facingSign * u * 0.05f, torsoTopY - u * 0.36f)
        drawLine(skinShade, Offset(px, torsoTopY), headCenter, strokeWidth = (u * 0.24f).coerceAtLeast(2f))
        drawCircle(color = skinColor, radius = u * 0.36f, center = headCenter)
        drawArc(
            color = skinShade.copy(alpha = 0.45f),
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(headCenter.x - u * 0.34f, headCenter.y - u * 0.32f),
            size = Size(u * 0.68f, u * 0.66f),
            style = Stroke(width = 1.3f)
        )

        when (player.hairStyleIndex % 4) {
            0 -> {
                // Athletic fade
                drawArc(
                    color = hairColor,
                    startAngle = 175f,
                    sweepAngle = 190f,
                    useCenter = true,
                    topLeft = Offset(headCenter.x - u * 0.37f, headCenter.y - u * 0.40f),
                    size = Size(u * 0.74f, u * 0.66f)
                )
            }
            1 -> {
                // Voluminous curly hair
                drawCircle(hairColor, radius = u * 0.22f, center = Offset(headCenter.x - u * 0.16f, headCenter.y - u * 0.28f))
                drawCircle(hairColor, radius = u * 0.24f, center = Offset(headCenter.x, headCenter.y - u * 0.34f))
                drawCircle(hairColor, radius = u * 0.22f, center = Offset(headCenter.x + u * 0.16f, headCenter.y - u * 0.28f))
            }
            2 -> {
                // Side-parted swept hair
                drawArc(
                    color = hairColor,
                    startAngle = 165f,
                    sweepAngle = 205f,
                    useCenter = true,
                    topLeft = Offset(headCenter.x - u * 0.38f, headCenter.y - u * 0.39f),
                    size = Size(u * 0.76f, u * 0.62f)
                )
            }
            else -> {
                // Sports headband + top volume
                drawArc(
                    color = hairColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(headCenter.x - u * 0.35f, headCenter.y - u * 0.41f),
                    size = Size(u * 0.70f, u * 0.60f)
                )
                drawLine(
                    color = team.accentColor,
                    start = Offset(headCenter.x - u * 0.34f, headCenter.y - u * 0.13f),
                    end = Offset(headCenter.x + u * 0.34f, headCenter.y - u * 0.13f),
                    strokeWidth = (u * 0.11f).coerceAtLeast(1.8f),
                    cap = StrokeCap.Round
                )
            }
        }

        // Facing eye detail
        drawCircle(
            color = Color(0xFF1A1A1A),
            radius = (u * 0.055f).coerceAtLeast(1.1f),
            center = Offset(headCenter.x + facingSign * u * 0.16f, headCenter.y + u * 0.04f)
        )

        // Jersey Number Badge on torso
        if (graphicsQuality != GraphicsQuality.LOW) {
            val label = player.jerseyNumber.toString()
            val fontSizeSp = (u * 0.42f / density).coerceIn(6f, 11f).sp
            val isLightShirt = kitPrimary == Color(0xFFF5F7FA)
            val numColor = if (isLightShirt) Color(0xFF0E1726) else Color.White
            val style = TextStyle(
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSizeSp,
                color = numColor
            )
            val measured = textMeasurer.measure(label, style = style)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    px - measured.size.width * 0.5f,
                    (torsoTopY + hipY) * 0.47f - measured.size.height * 0.45f
                )
            )
        }
    }

    // Active Player Name Plate
    if (isControlled && graphicsQuality != GraphicsQuality.LOW) {
        val nameStyle = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = Color.White
        )
        val measuredName = textMeasurer.measure(player.name, style = nameStyle)
        val tagW = measuredName.size.width + 12f
        val tagH = measuredName.size.height + 2f
        val tagX = px - tagW * 0.5f
        val tagY = footY + u * 0.45f
        drawRoundRect(
            color = Color(0xFF06110B).copy(alpha = 0.84f),
            topLeft = Offset(tagX, tagY),
            size = Size(tagW, tagH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawText(
            textLayoutResult = measuredName,
            topLeft = Offset(px - measuredName.size.width * 0.5f, tagY + 1f)
        )
    }
}

private fun DrawScope.drawBallBroadcast(
    ball: BallEntity,
    scale: Float,
    graphicsQuality: GraphicsQuality
) {
    val bx = ball.x * scale
    val by = ball.y * scale
    val heightScale = 1f + (ball.z * 0.048f).coerceIn(0f, 0.48f)
    val r = FootballEngine.BALL_RADIUS * scale * heightScale

    // Ground Altitude Shadow
    if (graphicsQuality != GraphicsQuality.LOW) {
        val shadowOffset = (0.35f + ball.z * 0.45f) * scale
        drawOval(
            brush = Brush.radialGradient(
                listOf(
                    Color.Black.copy(alpha = (0.44f - ball.z * 0.025f).coerceIn(0.14f, 0.44f)),
                    Color.Transparent
                ),
                center = Offset(bx + shadowOffset * 0.35f, by + shadowOffset * 0.65f),
                radius = r * 1.15f
            ),
            topLeft = Offset(bx - r * 0.9f + shadowOffset * 0.35f, by + shadowOffset * 0.25f),
            size = Size(r * 1.8f, r * 0.95f)
        )
    }

    val drawY = by - ball.z * scale * 0.54f
    val ballClip = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                left = bx - r,
                top = drawY - r,
                right = bx + r,
                bottom = drawY + r
            )
        )
    }

    // 3D Specular Match Ball Sphere
    clipPath(ballClip) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE7EFEA), Color(0xFF94A59C)),
                center = Offset(bx - r * 0.30f, drawY - r * 0.32f),
                radius = r * 1.45f
            ),
            radius = r,
            center = Offset(bx, drawY)
        )

        // Geodesic Championship Star-Panel Pattern
        val panelColor = Color(0xFF102438)
        val seamColor = Color(0xFF284359).copy(alpha = 0.55f)
        val centerPent = Path().apply {
            for (k in 0 until 5) {
                val a = ball.spinAngle + k * (2f * PI.toFloat() / 5f)
                val px = bx + cos(a) * r * 0.32f
                val py = drawY + sin(a) * r * 0.32f
                if (k == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(centerPent, color = panelColor)

        for (k in 0 until 5) {
            val a = ball.spinAngle + k * (2f * PI.toFloat() / 5f)
            val innerX = bx + cos(a) * r * 0.30f
            val innerY = drawY + sin(a) * r * 0.30f
            val outerX = bx + cos(a) * r * 0.78f
            val outerY = drawY + sin(a) * r * 0.78f
            drawLine(
                color = seamColor,
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = (r * 0.08f).coerceAtLeast(1f)
            )
            // Outer star-panel patches
            val patchCenter = Offset(bx + cos(a) * r * 0.84f, drawY + sin(a) * r * 0.84f)
            drawCircle(
                color = panelColor,
                radius = r * 0.25f,
                center = patchCenter
            )
        }

        // Specular gloss highlight
        if (graphicsQuality != GraphicsQuality.LOW) {
            drawCircle(
                color = Color.White.copy(alpha = 0.42f),
                radius = r * 0.22f,
                center = Offset(bx - r * 0.34f, drawY - r * 0.34f)
            )
        }
    }

    drawCircle(
        color = Color(0xFF15201B),
        radius = r,
        center = Offset(bx, drawY),
        style = Stroke(width = (0.22f * scale).coerceAtLeast(1.1f))
    )
}
