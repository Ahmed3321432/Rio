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
import androidx.compose.ui.geometry.Rect
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
import com.example.model.CameraDistance
import com.example.model.CrestPattern
import com.example.model.GraphicsQuality
import com.example.model.MatchPhase
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
)

private class RenderPart3D(
    val depth: Float,
    val draw: () -> Unit
)

private fun Color.shaded(factor: Float): Color = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.lit(factor: Float): Color = Color(
    red = (red + (1f - red) * factor).coerceIn(0f, 1f),
    green = (green + (1f - green) * factor).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
    alpha = alpha
)

private fun project3D(
    wx: Float,
    wy: Float,
    wz: Float,
    camX: Float,
    camY: Float,
    scale: Float
): Offset {
    val dx = (wx - camX).coerceIn(-96f, 96f)
    val dy = (wy - camY).coerceIn(-68f, 68f)
    val px = wx + dx * (wz * 0.0085f)
    val py = wy - wz * 0.92f + dy * (wz * 0.0040f)
    return Offset(px * scale, py * scale)
}

private fun project3D(
    p: Vec3,
    camX: Float,
    camY: Float,
    scale: Float
): Offset = project3D(p.x, p.y, p.z, camX, camY, scale)

private fun lerpVec3(a: Vec3, b: Vec3, t: Float): Vec3 = Vec3(
    x = a.x + (b.x - a.x) * t,
    y = a.y + (b.y - a.y) * t,
    z = a.z + (b.z - a.z) * t
)

private fun DrawScope.drawCylinder3D(
    pStart: Offset,
    pEnd: Offset,
    rStart: Float,
    rEnd: Float,
    baseColor: Color,
    enableLighting: Boolean = true
) {
    val dx = pEnd.x - pStart.x
    val dy = pEnd.y - pStart.y
    val len = hypot(dx, dy).coerceAtLeast(0.001f)
    val nx = -dy / len
    val ny = dx / len
    val bodyPath = Path().apply {
        moveTo(pStart.x + nx * rStart, pStart.y + ny * rStart)
        lineTo(pEnd.x + nx * rEnd, pEnd.y + ny * rEnd)
        lineTo(pEnd.x - nx * rEnd, pEnd.y - ny * rEnd)
        lineTo(pStart.x - nx * rStart, pStart.y - ny * rStart)
        close()
    }
    if (enableLighting) {
        val midX = (pStart.x + pEnd.x) * 0.5f
        val midY = (pStart.y + pEnd.y) * 0.5f
        val spanR = max(rStart, rEnd).coerceAtLeast(1.5f)
        // Orient shading roughly from upper-left floodlights
        val lightDir = if (nx + ny <= 0f) 1f else -1f
        val lx = nx * lightDir * spanR
        val ly = ny * lightDir * spanR
        drawPath(
            path = bodyPath,
            brush = Brush.linearGradient(
                colors = listOf(baseColor.lit(0.28f), baseColor, baseColor.shaded(0.66f)),
                start = Offset(midX + lx, midY + ly),
                end = Offset(midX - lx, midY - ly)
            )
        )
    } else {
        drawPath(bodyPath, color = baseColor)
    }
    drawCircle(color = baseColor, radius = rStart, center = pStart)
    drawCircle(color = baseColor, radius = rEnd, center = pEnd)
}

private data class CrowdSpectator(
    val row: Int,
    val normX: Float,
    val phaseOffset: Int,
    val colorSlot: Int
)

private data class TurfBladeDot(
    val normX: Float,
    val normY: Float,
    val light: Boolean
)

@Composable
fun PitchCanvas(
    matchState: MatchState,
    graphicsQuality: GraphicsQuality,
    cameraDistance: CameraDistance = CameraDistance.MEDIUM,
    showPassIndicator: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val crowdSpectators = remember {
        List(300) { idx ->
            val row = idx / 75
            val col = idx % 75
            val jitter = ((idx * 37) % 10) / 750f
            CrowdSpectator(
                row = row,
                normX = (col / 74f + jitter).coerceIn(0f, 1f),
                phaseOffset = idx,
                colorSlot = (idx * 7 + row * 3) % 5
            )
        }
    }

    val turfDots = remember {
        List(560) { idx ->
            val hx = ((idx * 73 + 19) % 1000) / 1000f
            val hy = ((idx * 137 + 41) % 1000) / 1000f
            TurfBladeDot(normX = hx, normY = hy, light = idx % 2 == 0)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height

        // PES 2013 Broadcast Camera Framing with selectable Close / Medium / Far zoom
        val visiblePitchSpanX = if (canvasW > canvasH) {
            cameraDistance.spanXLandscape
        } else {
            cameraDistance.spanXPortrait
        }
        val scale = max(canvasW / visiblePitchSpanX, canvasH / cameraDistance.spanY)

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

        // Stadium Night Sky Backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF04090F), Color(0xFF0B1926), Color(0xFF04090F))
            )
        )

        withTransform({
            translate(left = transX, top = transY)
        }) {
            fun sx(vx: Float): Float = vx * scale
            fun sy(vy: Float): Float = vy * scale

            val pitchTotalW = sx(FootballEngine.PITCH_W)
            val fieldL = sx(FootballEngine.FIELD_LEFT)
            val fieldT = sy(FootballEngine.FIELD_TOP)
            val fieldR = sx(FootballEngine.FIELD_RIGHT)
            val fieldB = sy(FootballEngine.FIELD_BOTTOM)
            val fieldW = fieldR - fieldL
            val fieldH = fieldB - fieldT

            // =========================================================================
            // 1. PES 2013 STADIUM GRANDSTANDS, ROOF OVERHANG, FLOODLIGHTS & LED BOARDS
            // =========================================================================
            // Upper Tiered Grandstand
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF070C12), Color(0xFF131D28), Color(0xFF1A2532))
                ),
                topLeft = Offset(0f, 0f),
                size = Size(pitchTotalW, sy(FootballEngine.FIELD_TOP))
            )
            // Lower Tiered Grandstand
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF18222E), Color(0xFF0E151E), Color(0xFF060A10))
                ),
                topLeft = Offset(0f, sy(FootballEngine.FIELD_BOTTOM)),
                size = Size(pitchTotalW, sy(FootballEngine.PITCH_H - FootballEngine.FIELD_BOTTOM))
            )

            // Tier concrete step dividers & stadium aisles
            if (graphicsQuality != GraphicsQuality.LOW) {
                for (step in 1..4) {
                    val stepY = sy(step * 1.45f)
                    drawLine(
                        color = Color(0xFF2B3A4D).copy(alpha = 0.55f),
                        start = Offset(0f, stepY),
                        end = Offset(pitchTotalW, stepY),
                        strokeWidth = 1.2f
                    )
                }
                for (aisle in 1..9) {
                    val ax = sx(aisle * (FootballEngine.PITCH_W / 10f))
                    drawRect(
                        color = Color(0xFF283444).copy(alpha = 0.45f),
                        topLeft = Offset(ax, 0f),
                        size = Size(sx(1.5f), sy(6.2f))
                    )
                }
            }

            // Animated PES Crowd Silhouettes + Stadium Camera Flashes (Medium / High / Ultra)
            if (graphicsQuality != GraphicsQuality.LOW) {
                val goalBuzz = if (matchState.netRippleLeft > 0f || matchState.netRippleRight > 0f) 2.8f else 0.42f
                val stepFilter = when (graphicsQuality) {
                    GraphicsQuality.ULTRA -> 1
                    GraphicsQuality.HIGH -> 1
                    GraphicsQuality.MEDIUM -> 2
                    GraphicsQuality.LOW -> 4
                }
                val isUltra = graphicsQuality == GraphicsQuality.ULTRA
                val isHighOrUltra = graphicsQuality == GraphicsQuality.HIGH || isUltra
                crowdSpectators.forEachIndexed { i, spec ->
                    if (i % stepFilter == 0 && (isUltra || i < 240)) {
                        val cheer = sin(matchState.remainingSeconds * 7.8f + spec.phaseOffset) * goalBuzz
                        val shirtColor = when (spec.colorSlot) {
                            0 -> matchState.homeTeam.primaryColor.copy(alpha = 0.92f)
                            1 -> matchState.awayTeam.primaryColor.copy(alpha = 0.90f)
                            2 -> Color(0xFFE6ECF2).copy(alpha = 0.72f)
                            3 -> ChampionshipGold.copy(alpha = 0.78f)
                            else -> Color(0xFF475B70).copy(alpha = 0.82f)
                        }
                        val cx = sx(4.5f + spec.normX * (FootballEngine.PITCH_W - 9f))
                        val cy = sy(1.05f + spec.row * 1.25f) + cheer
                        drawCircle(color = shirtColor, radius = 0.66f * scale, center = Offset(cx, cy))
                        if (isHighOrUltra) {
                            drawCircle(
                                color = Color(0xFFE2BA9B).copy(alpha = 0.78f),
                                radius = 0.28f * scale,
                                center = Offset(cx, cy - 0.44f * scale)
                            )
                            if (isUltra && spec.phaseOffset % 5 == 0) {
                                val scarfColor = if (spec.colorSlot == 1) {
                                    matchState.awayTeam.accentColor
                                } else {
                                    matchState.homeTeam.accentColor
                                }
                                drawLine(
                                    color = scarfColor.copy(alpha = 0.88f),
                                    start = Offset(cx - 0.55f * scale, cy - 0.80f * scale - cheer * 0.4f),
                                    end = Offset(cx + 0.55f * scale, cy - 0.85f * scale + cheer * 0.4f),
                                    strokeWidth = (0.14f * scale).coerceAtLeast(1.4f),
                                    cap = StrokeCap.Round
                                )
                            }
                            // Occasional stadium camera flash in the stands
                            val flashWave = sin(matchState.remainingSeconds * 11.5f + spec.phaseOffset * 1.7f)
                            if (flashWave > 0.992f && (i % 7 == 0)) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color.White, Color.White.copy(alpha = 0.35f), Color.Transparent),
                                        center = Offset(cx, cy - 0.2f * scale),
                                        radius = 1.6f * scale
                                    ),
                                    radius = 1.6f * scale,
                                    center = Offset(cx, cy - 0.2f * scale)
                                )
                            }
                        }
                    }
                }
            }

            // Stadium Roof Edge Shadow & Floodlight Gantries
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent)
                ),
                topLeft = Offset(0f, 0f),
                size = Size(pitchTotalW, sy(2.2f))
            )

            // Illuminated PES-style Perimeter LED Ad-Boards (top & bottom touchlines)
            val ledCount = 18
            val ledW = fieldW / ledCount
            for (i in 0 until ledCount) {
                val boardLeft = fieldL + i * ledW + 1.5f
                val boardW = (ledW - 3f).coerceAtLeast(4f)
                val primaryGlow = when (i % 4) {
                    0 -> matchState.homeTeam.primaryColor
                    1 -> Color(0xFF0A4D9C)
                    2 -> matchState.awayTeam.primaryColor
                    else -> ChampionshipGold
                }
                val secondaryGlow = when (i % 4) {
                    0 -> matchState.homeTeam.accentColor
                    1 -> Color(0xFF1995FF)
                    2 -> matchState.awayTeam.accentColor
                    else -> Color(0xFFFFF1A8)
                }

                // Top LED panel + turf spill light
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(primaryGlow.copy(alpha = 0.88f), secondaryGlow.copy(alpha = 0.90f))),
                    topLeft = Offset(boardLeft, sy(6.55f)),
                    size = Size(boardW, sy(1.65f)),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(boardLeft, sy(8.05f)),
                    size = Size(boardW, sy(0.28f))
                )
                if (graphicsQuality != GraphicsQuality.LOW) {
                    if (graphicsQuality == GraphicsQuality.ULTRA) {
                        // Crisp metallic bezel + subtle LED matrix sheen on 4K
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.26f),
                            topLeft = Offset(boardLeft, sy(6.55f)),
                            size = Size(boardW, sy(0.22f)),
                            cornerRadius = CornerRadius(1.5f, 1.5f)
                        )
                    }
                    val spillAlpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.20f else 0.14f
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(primaryGlow.copy(alpha = spillAlpha), Color.Transparent)
                        ),
                        topLeft = Offset(boardLeft, sy(8.35f)),
                        size = Size(boardW, sy(1.85f))
                    )
                    // Bottom touchline LED backs
                    drawRoundRect(
                        color = primaryGlow.copy(alpha = 0.56f),
                        topLeft = Offset(boardLeft, sy(FootballEngine.FIELD_BOTTOM + 1.35f)),
                        size = Size(boardW, sy(1.45f)),
                        cornerRadius = CornerRadius(2.5f, 2.5f)
                    )
                }
            }

            // Stadium Outer Track / Artificial Turf Apron (PES signature deep stadium apron)
            drawRoundRect(
                color = Color(0xFF0E3A21),
                topLeft = Offset(sx(1.5f), sy(8.3f)),
                size = Size(sx(FootballEngine.PITCH_W - 3.0f), sy(FootballEngine.PITCH_H - 10.6f)),
                cornerRadius = CornerRadius(2.4f * scale, 2.4f * scale)
            )
            // Inner dark green astroturf frame around pitch lines
            drawRect(
                color = Color(0xFF14522D),
                topLeft = Offset(fieldL - sx(2.6f), fieldT - sy(1.2f)),
                size = Size(fieldW + sx(5.2f), fieldH + sy(2.6f))
            )

            // =========================================================================
            // 2. PES 2013 NATURAL EMERALD-OLIVE MOWED TURF, WEAR PATCHES & FLOODLIGHTS
            // =========================================================================
            val stripeCount = if (graphicsQuality == GraphicsQuality.LOW) 12 else 26
            val stripeW = fieldW / stripeCount
            for (i in 0 until stripeCount) {
                val isLightStripe = i % 2 == 0
                val topCol = if (isLightStripe) Color(0xFF289648) else Color(0xFF1F7D3B)
                val botCol = if (isLightStripe) Color(0xFF20843D) else Color(0xFF196E33)
                drawRect(
                    brush = Brush.verticalGradient(listOf(topCol, botCol)),
                    topLeft = Offset(fieldL + i * stripeW, fieldT),
                    size = Size(stripeW + 0.9f, fieldH)
                )
            }

            // Subtle Cross-Mow Pattern & Organic Turf Grain
            if (graphicsQuality != GraphicsQuality.LOW) {
                val hBands = 16
                val bandH = fieldH / hBands
                for (r in 0 until hBands) {
                    if (r % 2 == 0) {
                        drawRect(
                            color = Color(0xFFA7F5B6).copy(alpha = 0.036f),
                            topLeft = Offset(fieldL, fieldT + r * bandH),
                            size = Size(fieldW, bandH)
                        )
                    }
                }
                // Micro grass blade variation & 4K stadium cross-cut diamond sheen
                if (graphicsQuality == GraphicsQuality.ULTRA) {
                    val diagCount = 12
                    val diagStep = fieldW / diagCount
                    for (d in -2..diagCount + 1 step 2) {
                        val x0 = fieldL + d * diagStep
                        val diagBand = Path().apply {
                            moveTo(x0, fieldT)
                            lineTo(x0 + diagStep, fieldT)
                            lineTo(x0 + diagStep - fieldH * 0.24f, fieldB)
                            lineTo(x0 - fieldH * 0.24f, fieldB)
                            close()
                        }
                        drawPath(diagBand, color = Color(0xFF9DF8B2).copy(alpha = 0.032f))
                    }
                }
                if (graphicsQuality == GraphicsQuality.HIGH || graphicsQuality == GraphicsQuality.ULTRA) {
                    val bladeAlphaLight = if (graphicsQuality == GraphicsQuality.ULTRA) 0.095f else 0.060f
                    val bladeAlphaDark = if (graphicsQuality == GraphicsQuality.ULTRA) 0.105f else 0.072f
                    val dotLimit = if (graphicsQuality == GraphicsQuality.ULTRA) turfDots.size else 220
                    val dotRadius = if (graphicsQuality == GraphicsQuality.ULTRA) (0.24f * scale).coerceAtLeast(1.0f) else (0.21f * scale).coerceAtLeast(0.85f)
                    for (idx in 0 until dotLimit) {
                        val dot = turfDots[idx]
                        drawCircle(
                            color = if (dot.light) Color(0xFFB5FFBE).copy(alpha = bladeAlphaLight) else Color(0xFF0A3818).copy(alpha = bladeAlphaDark),
                            radius = dotRadius,
                            center = Offset(fieldL + dot.normX * fieldW, fieldT + dot.normY * fieldH)
                        )
                    }
                }

                // Authentic Worn Goalmouth & Penalty Spot Turf Shading
                val mouthWearH = (FootballEngine.GOAL_BOTTOM - FootballEngine.GOAL_TOP) + 7f
                val mouthWearY = sy(FootballEngine.GOAL_TOP - 3.5f)
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF4C5E2D).copy(alpha = 0.34f), Color.Transparent)
                    ),
                    topLeft = Offset(fieldL - sx(0.8f), mouthWearY),
                    size = Size(sx(11.5f), sy(mouthWearH))
                )
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF4C5E2D).copy(alpha = 0.34f), Color.Transparent)
                    ),
                    topLeft = Offset(fieldR - sx(10.7f), mouthWearY),
                    size = Size(sx(11.5f), sy(mouthWearH))
                )
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF4B622F).copy(alpha = 0.24f), Color.Transparent)
                    ),
                    topLeft = Offset(sx(FootballEngine.CENTER_X - 11f), sy(FootballEngine.CENTER_Y - 11f)),
                    size = Size(sx(22f), sy(22f))
                )
            }

            // PES 2013 Stadium Floodlight Pools & Volumetric Night Beams
            if (graphicsQuality != GraphicsQuality.LOW) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF6FFE8).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(sx(FootballEngine.FIELD_LEFT + 62f), sy(40f)),
                        radius = sx(105f)
                    ),
                    topLeft = Offset(fieldL, fieldT),
                    size = Size(fieldW, fieldH)
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF6FFE8).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(sx(FootballEngine.FIELD_RIGHT - 62f), sy(40f)),
                        radius = sx(105f)
                    ),
                    topLeft = Offset(fieldL, fieldT),
                    size = Size(fieldW, fieldH)
                )
                // Edge vignette inside pitch corners for broadcast focus
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.14f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.16f)
                        )
                    ),
                    topLeft = Offset(fieldL, fieldT),
                    size = Size(fieldW, fieldH)
                )
            }
            if (graphicsQuality == GraphicsQuality.HIGH || graphicsQuality == GraphicsQuality.ULTRA) {
                val beamAlpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.125f else 0.09f
                val beamLeft = Path().apply {
                    moveTo(sx(24f), sy(0f))
                    lineTo(sx(112f), sy(126f))
                    lineTo(sx(34f), sy(134f))
                    close()
                }
                drawPath(
                    beamLeft,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFFCEB).copy(alpha = beamAlpha), Color.Transparent)
                    )
                )
                val beamRight = Path().apply {
                    moveTo(sx(FootballEngine.PITCH_W - 24f), sy(0f))
                    lineTo(sx(FootballEngine.PITCH_W - 34f), sy(134f))
                    lineTo(sx(FootballEngine.PITCH_W - 112f), sy(126f))
                    close()
                }
                drawPath(
                    beamRight,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFFCEB).copy(alpha = beamAlpha), Color.Transparent)
                    )
                )
                if (graphicsQuality == GraphicsQuality.ULTRA) {
                    // Center pitch floodlight sheen, dual cross beams & corner stadium lamp arrays
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.115f), Color.Transparent),
                            center = Offset(sx(FootballEngine.CENTER_X), sy(FootballEngine.CENTER_Y)),
                            radius = sx(102f)
                        ),
                        topLeft = Offset(fieldL, fieldT),
                        size = Size(fieldW, fieldH)
                    )
                    listOf(sx(24f), sx(FootballEngine.PITCH_W - 24f)).forEach { lampX ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFFFFFCEB).copy(alpha = 0.65f), Color.Transparent),
                                center = Offset(lampX, sy(1.2f)),
                                radius = sx(12.5f)
                            ),
                            radius = sx(12.5f),
                            center = Offset(lampX, sy(1.2f))
                        )
                        // High-intensity stadium LED gantry bulbs
                        for (b in -2..2) {
                            drawCircle(
                                color = Color(0xFFFFFEF6).copy(alpha = 0.92f),
                                radius = (0.36f * scale).coerceAtLeast(1.8f),
                                center = Offset(lampX + b * sx(1.35f), sy(1.1f))
                            )
                        }
                    }
                }
            }

            // Crisp Chalk-White Pitch Lines (FIFA Regulation Ratios on 250u x 160u Pitch)
            val lineGlow = Color.White.copy(alpha = 0.20f)
            val lineColor = Color(0xFFF7FBF8).copy(alpha = 0.92f)
            val lineW = (0.38f * scale).coerceAtLeast(1.8f)

            drawRect(
                color = lineGlow,
                topLeft = Offset(fieldL, fieldT),
                size = Size(fieldW, fieldH),
                style = Stroke(width = lineW * 1.7f)
            )
            drawRect(
                color = lineColor,
                topLeft = Offset(fieldL, fieldT),
                size = Size(fieldW, fieldH),
                style = Stroke(width = lineW)
            )

            // Halfway Line & Center Circle (radius 21.8u ~ 9.15m)
            val centerX = FootballEngine.CENTER_X
            val centerY = FootballEngine.CENTER_Y
            drawLine(
                color = lineColor,
                start = Offset(sx(centerX), fieldT),
                end = Offset(sx(centerX), fieldB),
                strokeWidth = lineW
            )
            drawCircle(
                color = lineGlow,
                radius = sx(21.8f),
                center = Offset(sx(centerX), sy(centerY)),
                style = Stroke(width = lineW * 1.6f)
            )
            drawCircle(
                color = lineColor,
                radius = sx(21.8f),
                center = Offset(sx(centerX), sy(centerY)),
                style = Stroke(width = lineW)
            )
            drawCircle(color = lineColor, radius = sx(0.95f), center = Offset(sx(centerX), sy(centerY)))

            // Left Penalty Area (39.3u x 94u) & Six-Yard Box (13.2u x 43u)
            drawRect(
                color = lineColor,
                topLeft = Offset(fieldL, sy(43f)),
                size = Size(sx(39.3f), sy(94f)),
                style = Stroke(width = lineW)
            )
            drawRect(
                color = lineColor,
                topLeft = Offset(fieldL, sy(68.5f)),
                size = Size(sx(13.2f), sy(43f)),
                style = Stroke(width = lineW)
            )
            drawCircle(
                color = lineColor,
                radius = sx(0.72f),
                center = Offset(sx(FootballEngine.FIELD_LEFT + 26.2f), sy(centerY))
            )
            drawArc(
                color = lineColor,
                startAngle = -53f,
                sweepAngle = 106f,
                useCenter = false,
                topLeft = Offset(sx(FootballEngine.FIELD_LEFT + 26.2f - 21.8f), sy(centerY - 21.8f)),
                size = Size(sx(43.6f), sy(43.6f)),
                style = Stroke(width = lineW)
            )

            // Right Penalty Area & Six-Yard Box
            drawRect(
                color = lineColor,
                topLeft = Offset(sx(FootballEngine.FIELD_RIGHT - 39.3f), sy(43f)),
                size = Size(sx(39.3f), sy(94f)),
                style = Stroke(width = lineW)
            )
            drawRect(
                color = lineColor,
                topLeft = Offset(sx(FootballEngine.FIELD_RIGHT - 13.2f), sy(68.5f)),
                size = Size(sx(13.2f), sy(43f)),
                style = Stroke(width = lineW)
            )
            drawCircle(
                color = lineColor,
                radius = sx(0.72f),
                center = Offset(sx(FootballEngine.FIELD_RIGHT - 26.2f), sy(centerY))
            )
            drawArc(
                color = lineColor,
                startAngle = 127f,
                sweepAngle = 106f,
                useCenter = false,
                topLeft = Offset(sx(FootballEngine.FIELD_RIGHT - 26.2f - 21.8f), sy(centerY - 21.8f)),
                size = Size(sx(43.6f), sy(43.6f)),
                style = Stroke(width = lineW)
            )

            // Corner Arcs & Shadowed Corner Flags
            val cR = sx(3.4f)
            drawArc(lineColor, 0f, 90f, false, Offset(fieldL - cR, fieldT - cR), Size(cR * 2, cR * 2), style = Stroke(lineW))
            drawArc(lineColor, 90f, 90f, false, Offset(fieldL + fieldW - cR, fieldT - cR), Size(cR * 2, cR * 2), style = Stroke(lineW))
            drawArc(lineColor, 270f, 90f, false, Offset(fieldL - cR, fieldT + fieldH - cR), Size(cR * 2, cR * 2), style = Stroke(lineW))
            drawArc(lineColor, 180f, 90f, false, Offset(fieldL + fieldW - cR, fieldT + fieldH - cR), Size(cR * 2, cR * 2), style = Stroke(lineW))

            val flutter = sin(matchState.remainingSeconds * 6.5f) * 0.42f * scale
            drawCornerFlag(fieldL, fieldT, scale, matchState.homeTeam.primaryColor, flutter)
            drawCornerFlag(fieldL, fieldT + fieldH, scale, matchState.homeTeam.primaryColor, flutter)
            drawCornerFlag(fieldL + fieldW, fieldT, scale, matchState.awayTeam.primaryColor, -flutter)
            drawCornerFlag(fieldL + fieldW, fieldT + fieldH, scale, matchState.awayTeam.primaryColor, -flutter)

            // =========================================================================
            // 3A. TRUE 3D PERSPECTIVE GOALS — BACK PASS (Behind Players & Ball)
            // =========================================================================
            drawGoal3DBack(
                isLeft = true,
                ripple = matchState.netRippleLeft,
                timeSec = matchState.remainingSeconds,
                camX = matchState.cameraX,
                camY = matchState.cameraY,
                scale = scale,
                graphicsQuality = graphicsQuality,
                netAccent = matchState.homeTeam.primaryColor
            )
            drawGoal3DBack(
                isLeft = false,
                ripple = matchState.netRippleRight,
                timeSec = matchState.remainingSeconds,
                camX = matchState.cameraX,
                camY = matchState.cameraY,
                scale = scale,
                graphicsQuality = graphicsQuality,
                netAccent = matchState.awayTeam.primaryColor
            )

            // Ball Motion Streak Trail
            if (graphicsQuality != GraphicsQuality.LOW && matchState.ball.trail.isNotEmpty()) {
                val tSize = matchState.ball.trail.size
                val isUltra = graphicsQuality == GraphicsQuality.ULTRA
                matchState.ball.trail.forEachIndexed { i, (tx, ty) ->
                    val falloff = ((tSize - i).toFloat() / tSize)
                    val alpha = falloff * (if (isUltra) 0.34f else 0.28f)
                    val centerPt = Offset(sx(tx), sy(ty))
                    if (isUltra) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(ChampionshipGold.copy(alpha = alpha * 0.40f), Color.Transparent)
                            ),
                            radius = sx(0.76f * (1.02f - i * 0.055f)).coerceAtLeast(1.5f),
                            center = centerPt
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White.copy(alpha = alpha), Color.Transparent)
                        ),
                        radius = sx(0.54f * (1.02f - i * (if (isUltra) 0.055f else 0.09f))).coerceAtLeast(1f),
                        center = centerPt
                    )
                }
            }

            // Pass-Aim Line & Target Ring
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

            // =========================================================================
            // 4. TRUE 3D ARTICULATED & Z-SORTED BROADCAST PLAYERS
            // =========================================================================
            matchState.players.sortedBy { it.y }.forEach { player ->
                val team = if (player.teamSide == TeamSide.HOME) matchState.homeTeam else matchState.awayTeam
                val isControlled = player.id == matchState.activeHomePlayerId
                val isPassTarget = showPassIndicator && (player.id == matchState.passTargetHomeId)
                drawPlayerBroadcast(
                    player = player,
                    team = team,
                    matchState = matchState,
                    isControlled = isControlled,
                    isPassTarget = isPassTarget,
                    shotCharge = if (isControlled && matchState.isChargingShot) matchState.shotCharge else 0f,
                    camX = matchState.cameraX,
                    camY = matchState.cameraY,
                    scale = scale,
                    graphicsQuality = graphicsQuality,
                    textMeasurer = textMeasurer
                )
            }

            // =========================================================================
            // 5. PES 2013 3D GEODESIC MATCH BALL
            // =========================================================================
            drawBallBroadcast(matchState.ball, matchState.cameraX, matchState.cameraY, scale, graphicsQuality)

            // =========================================================================
            // 3B. TRUE 3D PERSPECTIVE GOALS — FRONT PASS (Crossbar, Roof Net & South Post)
            // =========================================================================
            drawGoal3DFront(
                isLeft = true,
                ripple = matchState.netRippleLeft,
                timeSec = matchState.remainingSeconds,
                camX = matchState.cameraX,
                camY = matchState.cameraY,
                scale = scale,
                graphicsQuality = graphicsQuality,
                netAccent = matchState.homeTeam.primaryColor
            )
            drawGoal3DFront(
                isLeft = false,
                ripple = matchState.netRippleRight,
                timeSec = matchState.remainingSeconds,
                camX = matchState.cameraX,
                camY = matchState.cameraY,
                scale = scale,
                graphicsQuality = graphicsQuality,
                netAccent = matchState.awayTeam.primaryColor
            )

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
    val poleH = 2.6f * scale
    // Floodlight pole shadow on grass
    drawLine(
        color = Color.Black.copy(alpha = 0.35f),
        start = Offset(x, y),
        end = Offset(x + 0.85f * scale, y + 0.45f * scale),
        strokeWidth = (0.16f * scale).coerceAtLeast(1.2f),
        cap = StrokeCap.Round
    )
    // Pole
    drawLine(
        color = Color(0xFFF1F5F8),
        start = Offset(x, y),
        end = Offset(x, y - poleH),
        strokeWidth = (0.20f * scale).coerceAtLeast(1.5f),
        cap = StrokeCap.Round
    )
    // Pennant with crease shadow
    val flagPath = Path().apply {
        moveTo(x, y - poleH)
        lineTo(x + 1.45f * scale, y - poleH + 0.35f * scale + flutter)
        lineTo(x, y - poleH + 0.86f * scale)
        close()
    }
    drawPath(flagPath, color = flagColor)
    drawPath(flagPath, color = Color.Black.copy(alpha = 0.25f), style = Stroke(width = 0.9f))
}

@Composable
fun PitchRadarOverlay(
    matchState: MatchState,
    cameraDistance: CameraDistance = CameraDistance.MEDIUM,
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

            val halfSpan = cameraDistance.spanXLandscape * 0.45f
            val camLeft = rx((matchState.cameraX - halfSpan).coerceAtLeast(FootballEngine.FIELD_LEFT))
            val camRight = rx((matchState.cameraX + halfSpan).coerceAtMost(FootballEngine.FIELD_RIGHT))
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

private class GoalCage3D(
    val xLine: Float,
    val xBack: Float,
    val fTopBase: Vec3,
    val fBotBase: Vec3,
    val fTopBar: Vec3,
    val fBotBar: Vec3,
    val bTopRoof: Vec3,
    val bBotRoof: Vec3,
    val bTopBase: Vec3,
    val bBotBase: Vec3,
    val wave: Float,
    val dir: Float
)

private fun buildGoalCage3D(isLeft: Boolean, ripple: Float, timeSec: Float): GoalCage3D {
    val gTop = FootballEngine.GOAL_TOP
    val gBot = FootballEngine.GOAL_BOTTOM
    val depth = FootballEngine.GOAL_NET_DEPTH
    val dir = if (isLeft) -1f else 1f
    // Exact FIFA regulation goal ratio: 18u wide x 6.0u tall = 3.0 : 1 (7.32m x 2.44m)
    val goalH = 6.0f
    val roofBackH = 5.35f
    val xLine = if (isLeft) FootballEngine.FIELD_LEFT else FootballEngine.FIELD_RIGHT
    val xBack = xLine + dir * depth
    val wave = if (ripple > 0.01f) sin(timeSec * 42f) * ripple * 1.45f else 0f

    return GoalCage3D(
        xLine = xLine,
        xBack = xBack,
        fTopBase = Vec3(xLine, gTop, 0f),
        fBotBase = Vec3(xLine, gBot, 0f),
        fTopBar = Vec3(xLine, gTop, goalH),
        fBotBar = Vec3(xLine, gBot, goalH),
        bTopRoof = Vec3(xBack + dir * 0.15f, gTop + 0.35f, roofBackH),
        bBotRoof = Vec3(xBack + dir * 0.15f, gBot - 0.35f, roofBackH),
        bTopBase = Vec3(xBack, gTop + 0.35f, 0f),
        bBotBase = Vec3(xBack, gBot - 0.35f, 0f),
        wave = wave,
        dir = dir
    )
}

private fun DrawScope.drawGoal3DBack(
    isLeft: Boolean,
    ripple: Float,
    timeSec: Float,
    camX: Float,
    camY: Float,
    scale: Float,
    graphicsQuality: GraphicsQuality,
    netAccent: Color
) {
    val g = buildGoalCage3D(isLeft, ripple, timeSec)
    fun p3(v: Vec3): Offset = project3D(v, camX, camY, scale)

    val pFTopBase = p3(g.fTopBase)
    val pFBotBase = p3(g.fBotBase)
    val pBTopBase = p3(g.bTopBase)
    val pBBotBase = p3(g.bBotBase)
    val pFTopBar = p3(g.fTopBar)
    val pBTopRoof = p3(g.bTopRoof)
    val pBBotRoof = p3(g.bBotRoof)

    // 1) Cast 3D Shadow on Turf (posts, crossbar & net interior shadow)
    val floorShadow = Path().apply {
        moveTo(pFTopBase.x, pFTopBase.y)
        lineTo(pBTopBase.x, pBTopBase.y)
        lineTo(pBBotBase.x, pBBotBase.y)
        lineTo(pFBotBase.x, pFBotBase.y)
        close()
    }
    drawPath(
        path = floorShadow,
        brush = Brush.horizontalGradient(
            colors = if (isLeft) {
                listOf(Color.Black.copy(alpha = 0.42f), Color.Black.copy(alpha = 0.14f))
            } else {
                listOf(Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.42f))
            },
            startX = min(pFTopBase.x, pBTopBase.x),
            endX = max(pFTopBase.x, pBTopBase.x)
        )
    )
    // Post + crossbar cast shadows onto the grass along floodlight diagonal
    val lightShadowTop = Offset(pFTopBase.x + 2.2f * scale, pFTopBase.y + 1.15f * scale)
    val lightShadowBot = Offset(pFBotBase.x + 2.2f * scale, pFBotBase.y + 1.15f * scale)
    val shadowStroke = (0.52f * scale).coerceAtLeast(2.2f)
    drawLine(Color.Black.copy(alpha = 0.30f), pFTopBase, lightShadowTop, strokeWidth = shadowStroke, cap = StrokeCap.Round)
    drawLine(Color.Black.copy(alpha = 0.30f), pFBotBase, lightShadowBot, strokeWidth = shadowStroke, cap = StrokeCap.Round)
    drawLine(Color.Black.copy(alpha = 0.22f), lightShadowTop, lightShadowBot, strokeWidth = shadowStroke * 0.9f, cap = StrokeCap.Round)

    // 2) North (Top) Side Net Curtain & Back Net Panel Fill
    val netFill = Color.White.copy(alpha = 0.11f + ripple * 0.16f)
    val northSideFill = Path().apply {
        moveTo(pFTopBar.x, pFTopBar.y)
        lineTo(pBTopRoof.x, pBTopRoof.y)
        lineTo(pBTopBase.x, pBTopBase.y)
        lineTo(pFTopBase.x, pFTopBase.y)
        close()
    }
    val backPanelFill = Path().apply {
        moveTo(pBTopRoof.x, pBTopRoof.y)
        lineTo(pBTopBase.x, pBTopBase.y)
        lineTo(pBBotBase.x, pBBotBase.y)
        lineTo(pBBotRoof.x, pBBotRoof.y)
        close()
    }
    drawPath(northSideFill, color = netFill)
    drawPath(backPanelFill, color = netFill.copy(alpha = netFill.alpha * 1.15f))

    // 3) 3D Lattice Mesh on Back Panel & North Side Curtain (with curved bulge + goal ripple!)
    val meshColor = Color(0xFFF3F8F5).copy(alpha = if (graphicsQuality == GraphicsQuality.LOW) 0.44f else 0.58f)
    val meshAccent = netAccent.copy(alpha = 0.17f)
    val rows = when (graphicsQuality) {
        GraphicsQuality.ULTRA -> 20
        GraphicsQuality.HIGH -> 14
        GraphicsQuality.MEDIUM -> 10
        GraphicsQuality.LOW -> 6
    }
    val heightSteps = when (graphicsQuality) {
        GraphicsQuality.ULTRA -> 10
        GraphicsQuality.HIGH -> 7
        GraphicsQuality.MEDIUM -> 5
        GraphicsQuality.LOW -> 3
    }
    val meshStroke = if (graphicsQuality == GraphicsQuality.LOW) 0.9f else 1.05f

    fun backPoint3D(tY: Float, tZ: Float): Offset {
        val roofPt = lerpVec3(g.bTopRoof, g.bBotRoof, tY)
        val basePt = lerpVec3(g.bTopBase, g.bBotBase, tY)
        val pt = lerpVec3(roofPt, basePt, tZ)
        val sagBulge = g.dir * sin(tY * PI.toFloat()) * sin(tZ * PI.toFloat()) * (0.45f + abs(g.wave) * 1.4f)
        return p3(Vec3(pt.x + sagBulge + g.wave * sin(tY * PI.toFloat()), pt.y, pt.z))
    }

    // Back net vertical & horizontal cords
    for (i in 1 until rows) {
        val tY = i / rows.toFloat()
        var prev = backPoint3D(tY, 0f)
        for (h in 1..heightSteps) {
            val cur = backPoint3D(tY, h / heightSteps.toFloat())
            drawLine(meshColor, prev, cur, strokeWidth = meshStroke)
            prev = cur
        }
    }
    for (h in 1 until heightSteps) {
        val tZ = h / heightSteps.toFloat()
        var prev = backPoint3D(0f, tZ)
        for (i in 1..rows) {
            val cur = backPoint3D(i / rows.toFloat(), tZ)
            drawLine(meshColor, prev, cur, strokeWidth = meshStroke)
            prev = cur
        }
    }

    // North side curtain mesh
    for (s in 1..4) {
        val t = s / 5f
        drawLine(
            meshColor,
            p3(lerpVec3(g.fTopBar, g.fTopBase, t)),
            p3(lerpVec3(g.bTopRoof, g.bTopBase, t)),
            strokeWidth = meshStroke
        )
        drawLine(
            meshColor,
            p3(lerpVec3(g.fTopBar, g.bTopRoof, t)),
            p3(lerpVec3(g.fTopBase, g.bTopBase, t)),
            strokeWidth = meshStroke
        )
    }

    // Subtle diamond tension accents on High/Medium
    if (graphicsQuality != GraphicsQuality.LOW) {
        for (i in 0 until rows step 2) {
            val t0 = i / rows.toFloat()
            val t1 = ((i + 2).coerceAtMost(rows)) / rows.toFloat()
            drawLine(meshAccent, backPoint3D(t0, 0f), backPoint3D(t1, 1f), strokeWidth = 0.85f)
            drawLine(meshAccent, backPoint3D(t1, 0f), backPoint3D(t0, 1f), strokeWidth = 0.85f)
        }
    }

    // 4) Back Anchor Ground Frame & Angled Stanchion Support Poles
    val stanchionR = (0.20f * scale).coerceAtLeast(1.4f)
    drawCylinder3D(pFTopBase, pBTopBase, stanchionR, stanchionR, Color(0xFF9FB0A7), enableLighting = false)
    drawCylinder3D(pBTopBase, pBBotBase, stanchionR, stanchionR, Color(0xFF9FB0A7), enableLighting = false)
    drawCylinder3D(pBBotBase, pFBotBase, stanchionR, stanchionR, Color(0xFF9FB0A7), enableLighting = false)

    // Rear angled D-stanchion poles
    drawCylinder3D(pFTopBar, pBTopRoof, stanchionR, stanchionR, Color(0xFFC8D5CE))
    drawCylinder3D(pBTopRoof, pBTopBase, stanchionR, stanchionR, Color(0xFFB8C7BF))
    drawCylinder3D(pBTopRoof, pBBotRoof, stanchionR * 0.9f, stanchionR * 0.9f, Color(0xFFD6E2DC))

    // 5) North (Far/Top) Cylindrical Goalpost (behind players inside box)
    val postR = (FootballEngine.POST_RADIUS * scale * 0.95f).coerceAtLeast(2.6f)
    drawCylinder3D(pFTopBase, pFTopBar, postR, postR, Color(0xFFE5ECE8))
    drawCircle(color = Color(0xFFB5C2BC), radius = postR * 1.1f, center = pFTopBase)
    drawCircle(color = Color.White, radius = postR * 0.65f, center = pFTopBase + Offset(-postR * 0.18f, -postR * 0.18f))
}

private fun DrawScope.drawGoal3DFront(
    isLeft: Boolean,
    ripple: Float,
    timeSec: Float,
    camX: Float,
    camY: Float,
    scale: Float,
    graphicsQuality: GraphicsQuality,
    netAccent: Color
) {
    val g = buildGoalCage3D(isLeft, ripple, timeSec)
    fun p3(v: Vec3): Offset = project3D(v, camX, camY, scale)

    val pFBotBase = p3(g.fBotBase)
    val pBBotBase = p3(g.bBotBase)
    val pFTopBar = p3(g.fTopBar)
    val pFBotBar = p3(g.fBotBar)
    val pBTopRoof = p3(g.bTopRoof)
    val pBBotRoof = p3(g.bBotRoof)

    val netFill = Color.White.copy(alpha = 0.09f + ripple * 0.14f)
    val roofFill = Path().apply {
        moveTo(pFTopBar.x, pFTopBar.y)
        lineTo(pBTopRoof.x, pBTopRoof.y)
        lineTo(pBBotRoof.x, pBBotRoof.y)
        lineTo(pFBotBar.x, pFBotBar.y)
        close()
    }
    val southSideFill = Path().apply {
        moveTo(pFBotBar.x, pFBotBar.y)
        lineTo(pBBotRoof.x, pBBotRoof.y)
        lineTo(pBBotBase.x, pBBotBase.y)
        lineTo(pFBotBase.x, pFBotBase.y)
        close()
    }
    drawPath(roofFill, color = netFill)
    drawPath(southSideFill, color = netFill)

    // Roof Net 3D Grid + South Curtain
    val meshColor = Color(0xFFF3F8F5).copy(alpha = if (graphicsQuality == GraphicsQuality.LOW) 0.42f else 0.56f)
    val meshAccent = netAccent.copy(alpha = 0.15f)
    val rows = when (graphicsQuality) {
        GraphicsQuality.ULTRA -> 20
        GraphicsQuality.HIGH -> 14
        GraphicsQuality.MEDIUM -> 10
        GraphicsQuality.LOW -> 6
    }
    val depthSteps = when (graphicsQuality) {
        GraphicsQuality.ULTRA -> 8
        GraphicsQuality.HIGH -> 5
        GraphicsQuality.MEDIUM -> 4
        GraphicsQuality.LOW -> 2
    }
    val meshStroke = if (graphicsQuality == GraphicsQuality.LOW) 0.9f else 1.05f

    fun roofPoint3D(tY: Float, tD: Float): Offset {
        val frontPt = lerpVec3(g.fTopBar, g.fBotBar, tY)
        val backPt = lerpVec3(g.bTopRoof, g.bBotRoof, tY)
        val pt = lerpVec3(frontPt, backPt, tD)
        val sag = -sin(tD * PI.toFloat()) * 0.22f
        val impact = g.wave * tD * sin(tY * PI.toFloat())
        return p3(Vec3(pt.x + impact, pt.y, pt.z + sag))
    }

    for (i in 1 until rows) {
        val tY = i / rows.toFloat()
        var prev = roofPoint3D(tY, 0f)
        for (d in 1..depthSteps) {
            val cur = roofPoint3D(tY, d / depthSteps.toFloat())
            drawLine(meshColor, prev, cur, strokeWidth = meshStroke)
            prev = cur
        }
    }
    for (d in 1 until depthSteps) {
        val tD = d / depthSteps.toFloat()
        var prev = roofPoint3D(0f, tD)
        for (i in 1..rows) {
            val cur = roofPoint3D(i / rows.toFloat(), tD)
            drawLine(meshColor, prev, cur, strokeWidth = meshStroke)
            prev = cur
        }
    }
    for (s in 1..4) {
        val t = s / 5f
        drawLine(meshColor, p3(lerpVec3(g.fBotBar, g.fBotBase, t)), p3(lerpVec3(g.bBotRoof, g.bBotBase, t)), strokeWidth = meshStroke)
        drawLine(meshColor, p3(lerpVec3(g.fBotBar, g.bBotRoof, t)), p3(lerpVec3(g.fBotBase, g.bBotBase, t)), strokeWidth = meshStroke)
    }
    if (graphicsQuality != GraphicsQuality.LOW) {
        for (i in 0 until rows step 2) {
            val t0 = i / rows.toFloat()
            val t1 = ((i + 2).coerceAtMost(rows)) / rows.toFloat()
            drawLine(meshAccent, roofPoint3D(t0, 0f), roofPoint3D(t1, 1f), strokeWidth = 0.85f)
            drawLine(meshAccent, roofPoint3D(t1, 0f), roofPoint3D(t0, 1f), strokeWidth = 0.85f)
        }
    }

    // South Stanchion Frame
    val stanchionR = (0.20f * scale).coerceAtLeast(1.4f)
    drawCylinder3D(pFBotBar, pBBotRoof, stanchionR, stanchionR, Color(0xFFC8D5CE))
    drawCylinder3D(pBBotRoof, pBBotBase, stanchionR, stanchionR, Color(0xFFB8C7BF))

    // South (Near) Cylindrical Post + Elevated 3D Crossbar
    val postR = (FootballEngine.POST_RADIUS * scale * 0.98f).coerceAtLeast(2.7f)
    drawCylinder3D(pFBotBase, pFBotBar, postR, postR, Color(0xFFEBF2EE))
    drawCylinder3D(pFTopBar, pFBotBar, postR * 0.98f, postR * 0.98f, Color(0xFFF3F7F5))
    if (graphicsQuality == GraphicsQuality.ULTRA) {
        drawLine(
            color = Color.White.copy(alpha = 0.72f),
            start = pFBotBase + Offset(-postR * 0.25f, 0f),
            end = pFBotBar + Offset(-postR * 0.25f, 0f),
            strokeWidth = (postR * 0.34f).coerceAtLeast(1.1f),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.78f),
            start = pFTopBar + Offset(0f, -postR * 0.25f),
            end = pFBotBar + Offset(0f, -postR * 0.25f),
            strokeWidth = (postR * 0.34f).coerceAtLeast(1.1f),
            cap = StrokeCap.Round
        )
    }

    // Glossy metallic post elbow joints & south base flange
    drawCircle(color = Color(0xFFB5C2BC), radius = postR * 1.1f, center = pFBotBase)
    drawCircle(color = Color.White, radius = postR * 0.65f, center = pFBotBase + Offset(-postR * 0.18f, -postR * 0.18f))
    listOf(pFTopBar, pFBotBar).forEach { corner ->
        drawCircle(color = Color(0xFFDDE7E2), radius = postR * 1.08f, center = corner)
        drawCircle(color = Color.White, radius = postR * 0.62f, center = corner + Offset(-postR * 0.22f, -postR * 0.22f))
    }
}

private fun DrawScope.drawPlayerBroadcast(
    player: PlayerEntity,
    team: Team,
    matchState: MatchState,
    isControlled: Boolean,
    isPassTarget: Boolean,
    shotCharge: Float,
    camX: Float,
    camY: Float,
    scale: Float,
    graphicsQuality: GraphicsQuality,
    textMeasurer: TextMeasurer
) {
    val groundPt = project3D(player.x, player.y, 0f, camX, camY, scale)
    val px = groundPt.x
    val py = groundPt.y

    // Realistic individual human physique variation (tall vs short, slim vs athletic build)
    val bodyHash = abs(player.name.hashCode() + player.jerseyNumber * 31)
    val heightScale = when (player.role) {
        PlayerRole.GK -> 1.07f + (bodyHash % 5) * 0.015f
        PlayerRole.DEF -> 1.00f + (bodyHash % 7) * 0.018f
        PlayerRole.MID -> 0.90f + (bodyHash % 9) * 0.022f
        PlayerRole.FWD -> 0.89f + (bodyHash % 10) * 0.023f
    }
    val buildWidth = when (player.role) {
        PlayerRole.GK -> 0.96f + (bodyHash % 4) * 0.025f
        PlayerRole.DEF -> 0.90f + (bodyHash % 5) * 0.032f
        PlayerRole.MID -> 0.80f + (bodyHash % 6) * 0.028f
        PlayerRole.FWD -> 0.78f + (bodyHash % 7) * 0.030f
    }
    val h = heightScale * 0.78f
    val w = buildWidth * 0.80f
    val u = 1.34f * scale * (0.94f + w * 0.06f)

    // Determine action states from MatchState & PlayerEntity
    val timeSec = matchState.remainingSeconds
    val speedMag = hypot(player.vx, player.vy)
    val runWeight = ((speedMag - 0.8f) / 4.8f).coerceIn(0f, 1f)
    val hasBall = matchState.ball.ownerId == player.id
    val isSliding = player.slideTimer > 0f
    val isKicking = player.kickAnimTimer > 0f
    val kickArc = if (isKicking) {
        val p = 1f - (player.kickAnimTimer / 0.22f).coerceIn(0f, 1f)
        sin(p * PI.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isCelebrating = matchState.phase == MatchPhase.GOAL_CELEBRATION && player.teamSide == matchState.lastScorerTeam
    val isGkDiving = player.role == PlayerRole.GK && (speedMag > 9.2f || (isKicking && speedMag > 4.0f))
    val isAerialHeader = !isSliding && !isGkDiving && player.role != PlayerRole.GK &&
        matchState.ball.z > 2.2f && hypot(player.x - matchState.ball.x, player.y - matchState.ball.y) < 4.0f && isKicking

    val jumpZ = when {
        isAerialHeader -> kickArc * 1.45f
        isCelebrating -> abs(sin(timeSec * 8.5f + player.jerseyNumber)) * 0.95f
        isGkDiving -> 0.85f
        else -> 0f
    }

    // 1) 3D Ground Shadow (scales & shifts smoothly as player jumps or dives!)
    if (graphicsQuality != GraphicsQuality.LOW) {
        val shadowShift = (0.42f + jumpZ * 0.42f) * scale
        val shadowScale = (1f - jumpZ * 0.14f).coerceIn(0.60f, 1f)
        val shadowAlpha = (0.36f - jumpZ * 0.08f).coerceIn(0.14f, 0.36f)
        if (graphicsQuality == GraphicsQuality.ULTRA) {
            drawOval(
                brush = Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = shadowAlpha * 0.55f), Color.Transparent),
                    center = Offset(px + shadowShift * 1.15f, py + u * 0.16f + jumpZ * 0.18f * scale),
                    radius = u * 1.56f * shadowScale
                ),
                topLeft = Offset(px - u * 0.78f * shadowScale + shadowShift * 1.15f, py - u * 0.24f),
                size = Size(u * 2.34f * shadowScale, u * 0.80f * shadowScale)
            )
        }
        drawOval(
            brush = Brush.radialGradient(
                listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                center = Offset(px + shadowShift, py + u * 0.14f + jumpZ * 0.18f * scale),
                radius = u * 1.24f * shadowScale
            ),
            topLeft = Offset(px - u * 0.62f * shadowScale + shadowShift, py - u * 0.18f),
            size = Size(u * 1.85f * shadowScale, u * 0.64f * shadowScale)
        )
        if (jumpZ < 0.25f) {
            drawOval(
                color = Color.Black.copy(alpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.48f else 0.42f),
                topLeft = Offset(px - u * 0.58f, py - u * 0.14f),
                size = Size(u * 1.16f, u * 0.30f)
            )
        }
    }

    // 2) Classic PES 2013 Controlled Player Base Ring, Facing Arrow & Stamina Arc
    if (isControlled) {
        drawOval(
            color = ElectricCyan.copy(alpha = 0.24f),
            topLeft = Offset(px - u * 1.38f, py - u * 0.44f),
            size = Size(u * 2.76f, u * 1.00f),
            style = Stroke(width = (0.48f * scale).coerceAtLeast(2.6f))
        )
        drawOval(
            color = ElectricCyan.copy(alpha = 0.92f),
            topLeft = Offset(px - u * 1.28f, py - u * 0.38f),
            size = Size(u * 2.56f, u * 0.88f),
            style = Stroke(width = (0.24f * scale).coerceAtLeast(1.6f))
        )
        val dirCos = cos(player.facingAngle)
        val dirSin = sin(player.facingAngle)
        val tipX = px + dirCos * u * 1.50f
        val tipY = (py + u * 0.06f) + dirSin * u * 0.50f
        val perpX = -dirSin * u * 0.23f
        val perpY = dirCos * u * 0.10f
        val baseMidX = px + dirCos * u * 1.18f
        val baseMidY = (py + u * 0.06f) + dirSin * u * 0.39f
        val dirArrow = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseMidX + perpX, baseMidY + perpY)
            lineTo(baseMidX - perpX, baseMidY - perpY)
            close()
        }
        drawPath(dirArrow, color = ElectricCyan)

        drawArc(
            color = if (player.stamina > 0.28f) TurfEmerald else Color(0xFFFF455F),
            startAngle = 22f,
            sweepAngle = 136f * player.stamina,
            useCenter = false,
            topLeft = Offset(px - u * 1.52f, py - u * 0.52f),
            size = Size(u * 3.04f, u * 1.16f),
            style = Stroke(width = (0.26f * scale).coerceAtLeast(1.8f), cap = StrokeCap.Round)
        )
    }

    // Kit & Appearance Colors
    val kitPrimary = if (player.role == PlayerRole.GK) {
        if (player.teamSide == TeamSide.HOME) Color(0xFFFFB300) else Color(0xFF9C27B0)
    } else team.primaryColor
    val kitSecondary = if (player.role == PlayerRole.GK) Color(0xFF1A1A1A) else team.secondaryColor
    val shortsColor = if (player.role == PlayerRole.GK) Color(0xFF161B22) else team.secondaryColor
    val sockColor = if (player.role == PlayerRole.GK) Color(0xFF21262E) else kitPrimary

    val skinHighlight = when (player.skinToneIndex % 3) {
        0 -> Color(0xFFF6CBB0)
        1 -> Color(0xFFDC9F77)
        else -> Color(0xFF915D3E)
    }
    val skinColor = when (player.skinToneIndex % 3) {
        0 -> Color(0xFFEDBA98)
        1 -> Color(0xFFCC8E64)
        else -> Color(0xFF7B4B30)
    }
    val skinShade = when (player.skinToneIndex % 3) {
        0 -> Color(0xFFC58A66)
        1 -> Color(0xFFA2653E)
        else -> Color(0xFF53301B)
    }
    val hairColor = when (player.hairStyleIndex % 4) {
        2 -> Color(0xFF4F331E)
        3 -> Color(0xFF281B14)
        else -> Color(0xFF151210)
    }

    // 3D Local Coordinate Frame -> World Space `(x, y, z)`
    val fx = cos(player.facingAngle)
    val fy = sin(player.facingAngle)
    val rx = -fy
    val ry = fx
    val facingSign = if (fx >= 0f) 1f else -1f
    val facingBack = fy < -0.25f

    fun worldPt(fwd: Float, side: Float, zUp: Float): Vec3 = Vec3(
        x = player.x + fx * fwd + rx * side,
        y = player.y + fy * fwd + ry * side,
        z = zUp
    )

    fun p3(v: Vec3): Offset = project3D(v, camX, camY, scale)

    fun depthOf(v: Vec3): Float = v.y + v.z * 0.35f

    // 3D Gait, Twist & Pose Parameters
    val gaitPhase = player.stridePhase * 1.48f
    val swingWave = sin(gaitPhase) * runWeight
    val dribbleStepAmp = if (hasBall) 0.78f else 1.0f
    val bounceZ = if (runWeight < 0.25f && !isSliding && !isGkDiving) {
        sin(timeSec * 3.4f + player.jerseyNumber * 0.9f) * 0.05f
    } else {
        abs(sin(gaitPhase)) * runWeight * 0.16f
    }

    // Realistic ~7-head Athletic Proportions scaled by per-player heightScale (h) & buildWidth (w)
    val baseZ = jumpZ + bounceZ - (if (isSliding) 1.65f * h else 0f) - (if (player.role == PlayerRole.GK && !isGkDiving && runWeight < 0.35f) 0.22f else 0f)
    val hipZ = (2.72f * h + baseZ).coerceAtLeast(0.45f)
    val chestZ = (4.54f * h + baseZ - (if (isSliding) 0.75f * h else 0f)).coerceAtLeast(1.00f)
    val headCenterZ = (5.26f * h + baseZ - (if (isSliding) 0.98f * h else 0f)).coerceAtLeast(1.42f)

    val torsoFwdLean = when {
        isSliding -> -1.45f * h
        isGkDiving -> 1.00f * h
        isKicking -> -0.35f * kickArc + 0.18f
        player.stunTimer > 0f -> -0.45f * sin(timeSec * 22f)
        hasBall -> 0.40f * runWeight
        else -> 0.30f * runWeight
    }
    val pelvisYaw = if (isKicking) -0.32f * kickArc else swingWave * 0.18f
    val torsoYaw = if (isKicking) 0.36f * kickArc else -swingWave * 0.24f

    val parts = ArrayList<RenderPart3D>(18)
    val enableLights = graphicsQuality != GraphicsQuality.LOW

    // 3) Build 3D Lower Body (Slim Athletic Thighs, Knees, Calves, Socks & Boots)
    val hipWidth = 0.33f * w
    for (s in listOf(-1f, 1f)) {
        val hipFwd = -s * pelvisYaw * 0.30f
        val hip3 = worldPt(hipFwd, s * hipWidth, hipZ)

        val legSwing = if (s > 0f) swingWave else -swingWave
        val footFwd = when {
            isSliding -> if (s > 0f) 2.55f * h else -0.45f * h
            isGkDiving -> -1.40f * h + s * 0.22f
            isKicking && s > 0f -> 2.05f * h * kickArc - (1f - kickArc) * 0.35f
            isKicking && s < 0f -> -0.25f * h
            isAerialHeader -> -0.35f + s * 0.22f
            else -> legSwing * 1.62f * h * dribbleStepAmp
        }
        val footZ = when {
            isSliding -> if (s > 0f) 0.16f else 0.52f
            isGkDiving -> 0.78f + (if (s > 0f) 0.25f else 0f)
            isKicking && s > 0f -> 0.14f + kickArc * 1.35f * h
            isAerialHeader -> jumpZ * 0.85f + 0.25f
            isCelebrating -> 0.12f + jumpZ * 0.9f
            legSwing > 0f -> 0.12f + legSwing * 0.72f * h
            else -> 0.12f + (-legSwing) * 0.38f * h
        }
        val footSide = s * (if (player.role == PlayerRole.GK && !isGkDiving && runWeight < 0.35f) 0.56f * w else 0.36f * w)
        val foot3 = worldPt(footFwd, footSide, footZ)

        val kneeBendFwd = when {
            isSliding && s > 0f -> 0.10f
            isSliding && s < 0f -> 0.62f * h
            isGkDiving -> 0.35f
            isKicking && s > 0f -> 0.52f * kickArc
            legSwing > 0f -> (0.38f + legSwing * 0.24f) * h
            else -> 0.24f * h
        }
        val kneeZ = ((hip3.z + foot3.z) * 0.5f + 0.16f * h).coerceAtLeast(0.35f)
        val knee3 = worldPt((hipFwd + footFwd) * 0.5f + kneeBendFwd, footSide, kneeZ)

        val ankle3 = lerpVec3(knee3, foot3, 0.89f)
        val pHip = p3(hip3)
        val pKnee = p3(knee3)
        val pAnkle = p3(ankle3)
        val pToe = p3(Vec3(foot3.x + fx * 0.38f * h, foot3.y + fy * 0.38f * h, (foot3.z - 0.04f).coerceAtLeast(0.06f)))
        val pHeel = p3(Vec3(foot3.x - fx * 0.16f * h, foot3.y - fy * 0.16f * h, foot3.z))

        val bootColor = if (player.teamSide == TeamSide.HOME) Color(0xFF0E1622) else Color(0xFF1F1215)
        val bootAccent = if (player.teamSide == TeamSide.HOME) ElectricCyan else ChampionshipGold

        // Thigh segment (tapered athletic quad)
        parts += RenderPart3D(depth = depthOf(lerpVec3(hip3, knee3, 0.5f))) {
            drawCylinder3D(pHip, pKnee, 0.175f * w * scale, 0.132f * w * scale, skinColor, enableLights)
        }
        // Calf / Sock + Sleek Boot segment
        parts += RenderPart3D(depth = depthOf(lerpVec3(knee3, foot3, 0.55f))) {
            drawCylinder3D(pKnee, pAnkle, 0.136f * w * scale, 0.088f * w * scale, sockColor, enableLights)
            val cuffPt = p3(lerpVec3(knee3, ankle3, 0.14f))
            drawCircle(color = kitSecondary, radius = 0.130f * w * scale, center = cuffPt)
            if (graphicsQuality == GraphicsQuality.ULTRA) {
                // Ribbed sock band & subtle shin-guard specular ridge
                val shinMid = p3(lerpVec3(knee3, ankle3, 0.48f))
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = pKnee + Offset(-0.02f * scale, 0.06f * scale),
                    end = shinMid,
                    strokeWidth = (0.045f * scale).coerceAtLeast(0.8f),
                    cap = StrokeCap.Round
                )
            }
            drawCylinder3D(pHeel, pToe, 0.115f * w * scale, 0.084f * w * scale, bootColor, enableLights)
            drawLine(
                color = bootAccent.copy(alpha = 0.86f),
                start = Offset((pHeel.x + pToe.x) * 0.5f, (pHeel.y + pToe.y) * 0.5f - 0.03f * scale),
                end = pToe,
                strokeWidth = (0.085f * scale).coerceAtLeast(1.0f),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.72f),
                start = pHeel + Offset(0f, 0.07f * scale),
                end = pToe + Offset(0f, 0.06f * scale),
                strokeWidth = 0.9f,
                cap = StrokeCap.Round
            )
            if (graphicsQuality == GraphicsQuality.ULTRA) {
                drawCircle(color = Color(0xFFD8E4EC), radius = 0.032f * scale, center = pHeel + Offset(0f, 0.08f * scale))
                drawCircle(color = Color(0xFFD8E4EC), radius = 0.032f * scale, center = pToe + Offset(0f, 0.07f * scale))
            }
        }
    }

    // 4) Tailored 3D Athletic Shorts & Narrow Waist
    val shortsCenter3 = worldPt(torsoFwdLean * 0.18f, 0f, hipZ + 0.06f * h)
    val waistL3 = worldPt(torsoFwdLean * 0.22f - pelvisYaw * 0.25f, -0.41f * w, hipZ + 0.24f * h)
    val waistR3 = worldPt(torsoFwdLean * 0.22f + pelvisYaw * 0.25f, 0.41f * w, hipZ + 0.24f * h)
    val hemL3 = worldPt(-pelvisYaw * 0.28f, -0.49f * w, hipZ - 0.54f * h)
    val hemR3 = worldPt(pelvisYaw * 0.28f, 0.49f * w, hipZ - 0.54f * h)
    val hemMid3 = worldPt(0f, 0f, hipZ - 0.28f * h)

    parts += RenderPart3D(depth = depthOf(shortsCenter3)) {
        val wL = p3(waistL3)
        val wR = p3(waistR3)
        val hL = p3(hemL3)
        val hR = p3(hemR3)
        val hM = p3(hemMid3)
        val sPath = Path().apply {
            moveTo(wL.x, wL.y)
            lineTo(wR.x, wR.y)
            lineTo(hR.x, hR.y)
            lineTo(hM.x, hM.y)
            lineTo(hL.x, hL.y)
            close()
        }
        drawPath(sPath, color = shortsColor)
        if (enableLights) {
            drawPath(
                sPath,
                brush = Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = 0.24f), Color.Transparent, Color.White.copy(alpha = 0.14f)),
                    startX = min(hL.x, hR.x),
                    endX = max(hL.x, hR.x)
                )
            )
        }
        drawPath(sPath, color = Color.Black.copy(alpha = 0.46f), style = Stroke(width = 1.0f))
    }

    // 5) 3D Slim Athletic Upper Arms, Fitted Sleeves, Forearms & Natural Hands
    val shWidth = 0.63f * w
    for (s in listOf(-1f, 1f)) {
        val shFwd = torsoFwdLean + s * torsoYaw * 0.42f
        val shSide = s * shWidth
        val sh3 = worldPt(shFwd, shSide, chestZ - 0.10f * h)

        val rawArmSwing = if (s > 0f) -swingWave else swingWave
        val armSwing = when {
            isSliding -> if (s > 0f) 0.85f else -0.95f
            isGkDiving -> 1.1f
            isKicking -> if (s > 0f) -0.75f else 0.95f * kickArc
            else -> rawArmSwing
        }

        val isGkReady = player.role == PlayerRole.GK && !isGkDiving && runWeight < 0.35f && !isSliding
        val elFwd = when {
            isCelebrating -> shFwd + 0.24f * h
            isGkDiving -> shFwd + 1.05f * h
            isGkReady -> shFwd + 0.42f * h
            else -> shFwd + armSwing * 0.72f * h + 0.06f
        }
        val elSide = when {
            isCelebrating -> s * 1.05f * w
            isGkReady -> s * 0.95f * w
            else -> s * (0.76f * w + (if (hasBall) 0.08f else 0f))
        }
        val elZ = when {
            isCelebrating -> chestZ + 0.52f * h
            isGkDiving -> chestZ + 0.16f * h
            isGkReady -> chestZ - 0.58f * h
            else -> chestZ - 0.86f * h + abs(armSwing) * 0.16f * h
        }
        val el3 = worldPt(elFwd, elSide, elZ)

        val wrFwd = when {
            isCelebrating -> elFwd + 0.15f * h
            isGkDiving -> elFwd + 0.96f * h
            isGkReady -> elFwd + 0.60f * h
            isSliding -> elFwd + 0.36f * h
            else -> elFwd + 0.28f * h + ((armSwing + 1f) * 0.5f) * 0.56f * h * runWeight
        }
        val wrSide = when {
            isCelebrating -> s * 1.18f * w
            isGkReady -> s * 0.82f * w
            else -> s * 0.69f * w
        }
        val wrZ = when {
            isCelebrating -> chestZ + 1.38f * h
            isGkDiving -> chestZ + 0.38f * h
            isGkReady -> chestZ - 0.34f * h
            else -> (elZ - 0.72f * h + ((armSwing + 0.25f).coerceAtLeast(0f) * 0.54f * h * runWeight)).coerceAtLeast(0.30f)
        }
        val wr3 = worldPt(wrFwd, wrSide, wrZ)

        val pSh = p3(sh3)
        val pEl = p3(el3)
        val pWr = p3(wr3)
        val sleeveEnd3 = lerpVec3(sh3, el3, 0.56f)
        val pSleeveEnd = p3(sleeveEnd3)

        // Upper arm + fitted jersey sleeve (+ captain armband on skipper)
        val isCaptainArm = s < 0f && (player.id.endsWith("_6") || player.id.endsWith("_4"))
        parts += RenderPart3D(depth = depthOf(lerpVec3(sh3, el3, 0.5f))) {
            drawCylinder3D(pSleeveEnd, pEl, 0.116f * w * scale, 0.098f * w * scale, skinColor, enableLights)
            drawCylinder3D(pSh, pSleeveEnd, 0.168f * w * scale, 0.132f * w * scale, kitPrimary, enableLights)
            drawCircle(color = kitSecondary, radius = 0.126f * w * scale, center = pSleeveEnd)
            if (isCaptainArm && enableLights) {
                val armbandPt = p3(lerpVec3(sh3, sleeveEnd3, 0.62f))
                drawCircle(color = ChampionshipGold, radius = 0.145f * w * scale, center = armbandPt)
            }
        }

        // Slim Forearm + Natural 3D Hand / GK Glove
        parts += RenderPart3D(depth = depthOf(lerpVec3(el3, wr3, 0.65f))) {
            drawCylinder3D(pEl, pWr, 0.098f * w * scale, 0.072f * w * scale, skinColor, enableLights)

            val dx = pWr.x - pEl.x
            val dy = pWr.y - pEl.y
            val len = hypot(dx, dy).coerceAtLeast(0.001f)
            val ux = dx / len
            val uy = dy / len

            if (player.role == PlayerRole.GK) {
                val pGloveEnd = Offset(pWr.x + ux * 0.24f * h * scale, pWr.y + uy * 0.24f * h * scale)
                drawCylinder3D(pWr, pGloveEnd, 0.145f * w * scale, 0.125f * w * scale, Color(0xFFFFEE58), enableLights)
                drawCircle(color = Color(0xFF1B1F26), radius = 0.120f * w * scale, center = pWr, style = Stroke(1.0f))
            } else {
                val pFingerEnd = Offset(pWr.x + ux * 0.20f * h * scale, pWr.y + uy * 0.20f * h * scale)
                drawCylinder3D(pWr, pFingerEnd, 0.082f * w * scale, 0.056f * w * scale, skinColor, enableLights)
                val pThumb = Offset(
                    pWr.x + ux * 0.08f * scale - uy * 0.055f * w * scale * facingSign,
                    pWr.y + uy * 0.08f * scale - 0.035f * scale
                )
                drawCircle(color = skinColor, radius = 0.050f * w * scale, center = pThumb)
            }
        }
    }

    // 6) 3D Athletic V-Taper Torso (Tailored Shoulders -> Narrow Waist + Kit Pattern + Number)
    val shL3 = worldPt(torsoFwdLean - torsoYaw * 0.42f, -shWidth, chestZ)
    val shR3 = worldPt(torsoFwdLean + torsoYaw * 0.42f, shWidth, chestZ)
    val torsoCenter3 = worldPt(torsoFwdLean * 0.55f, 0f, (hipZ + chestZ) * 0.52f)
    val collarCenter3 = worldPt(torsoFwdLean, 0f, chestZ + 0.10f * h)

    parts += RenderPart3D(depth = depthOf(torsoCenter3)) {
        val sL = p3(shL3)
        val sR = p3(shR3)
        val wL = p3(waistL3)
        val wR = p3(waistR3)
        val topMid = p3(collarCenter3)

        val leftX = min(sL.x, sR.x) - 0.05f * scale
        val rightX = max(sL.x, sR.x) + 0.05f * scale
        val topY = min(sL.y, sR.y) - 0.10f * scale
        val botY = max(wL.y, wR.y) + 0.06f * scale

        val tPath = Path().apply {
            val lTop = if (sL.x <= sR.x) sL else sR
            val rTop = if (sL.x <= sR.x) sR else sL
            val lBot = if (wL.x <= wR.x) wL else wR
            val rBot = if (wL.x <= wR.x) wR else wL
            moveTo(lTop.x - 0.04f * w * scale, lTop.y)
            quadraticTo(topMid.x, topMid.y - 0.10f * h * scale, rTop.x + 0.04f * w * scale, rTop.y)
            lineTo(rBot.x + 0.02f * w * scale, rBot.y)
            lineTo(lBot.x - 0.02f * w * scale, lBot.y)
            close()
        }

        clipPath(tPath) {
            val spanW = (rightX - leftX).coerceAtLeast(0.5f * scale)
            val spanH = (botY - topY).coerceAtLeast(0.5f * scale)
            drawRect(color = kitPrimary, topLeft = Offset(leftX, topY), size = Size(spanW, spanH))

            if (player.role != PlayerRole.GK) {
                when (team.crestPattern) {
                    CrestPattern.STRIPES -> {
                        val sw = spanW * 0.18f
                        drawRect(kitSecondary.copy(alpha = 0.88f), Offset(topMid.x - sw * 2.1f, topY), Size(sw, spanH))
                        drawRect(kitSecondary.copy(alpha = 0.88f), Offset(topMid.x - sw * 0.5f, topY), Size(sw, spanH))
                        drawRect(kitSecondary.copy(alpha = 0.88f), Offset(topMid.x + sw * 1.1f, topY), Size(sw, spanH))
                    }
                    CrestPattern.SASH -> {
                        val sash = Path().apply {
                            moveTo(leftX, topY)
                            lineTo(leftX + spanW * 0.30f, topY)
                            lineTo(rightX, botY)
                            lineTo(rightX - spanW * 0.30f, botY)
                            close()
                        }
                        drawPath(sash, color = kitSecondary.copy(alpha = 0.90f))
                    }
                    CrestPattern.HALF -> {
                        drawRect(kitSecondary.copy(alpha = 0.92f), Offset(topMid.x, topY), Size(spanW * 0.6f, spanH))
                    }
                    CrestPattern.HOOPS -> {
                        val hh = spanH * 0.25f
                        drawRect(kitSecondary.copy(alpha = 0.88f), Offset(leftX, topY + hh), Size(spanW, hh * 0.72f))
                        drawRect(kitSecondary.copy(alpha = 0.88f), Offset(leftX, topY + hh * 2.4f), Size(spanW, hh * 0.72f))
                    }
                    CrestPattern.CHEVRON -> {
                        val chev = Path().apply {
                            moveTo(leftX, topY + spanH * 0.18f)
                            lineTo(topMid.x, topY + spanH * 0.48f)
                            lineTo(rightX, topY + spanH * 0.18f)
                            lineTo(rightX, topY + spanH * 0.35f)
                            lineTo(topMid.x, topY + spanH * 0.65f)
                            lineTo(leftX, topY + spanH * 0.35f)
                            close()
                        }
                        drawPath(chev, color = kitSecondary.copy(alpha = 0.90f))
                    }
                    CrestPattern.STAR -> {
                        drawRect(kitSecondary.copy(alpha = 0.84f), Offset(leftX, topY + spanH * 0.30f), Size(spanW, spanH * 0.18f))
                    }
                }
            }

            // V-neck collar / back collar
            if (!facingBack) {
                drawArc(
                    color = kitSecondary,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(topMid.x - 0.24f * w * scale, topMid.y - 0.08f * scale),
                    size = Size(0.48f * w * scale, 0.30f * scale)
                )
                drawArc(
                    color = skinColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(topMid.x - 0.17f * w * scale, topMid.y - 0.09f * scale),
                    size = Size(0.34f * w * scale, 0.22f * scale)
                )
            } else {
                drawArc(
                    color = kitSecondary,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(topMid.x - 0.21f * w * scale, topMid.y - 0.06f * scale),
                    size = Size(0.42f * w * scale, 0.16f * scale),
                    style = Stroke(width = (0.08f * scale).coerceAtLeast(1.0f))
                )
            }

            // 3D Cylindrical Fabric Shading, Shoulder Piping & Club Crest
            if (enableLights) {
                val rimAlpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.30f else 0.18f
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.White.copy(alpha = rimAlpha),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.24f)
                        ),
                        startX = leftX,
                        endX = rightX
                    ),
                    topLeft = Offset(leftX, topY),
                    size = Size(spanW, spanH)
                )
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.26f))),
                    topLeft = Offset(leftX, botY - spanH * 0.22f),
                    size = Size(spanW, spanH * 0.22f)
                )
                if (graphicsQuality == GraphicsQuality.HIGH || graphicsQuality == GraphicsQuality.ULTRA) {
                    drawLine(
                        color = Color.White.copy(alpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.32f else 0.24f),
                        start = Offset(leftX + spanW * 0.12f, topY + spanH * 0.08f),
                        end = Offset(rightX - spanW * 0.12f, topY + spanH * 0.08f),
                        strokeWidth = (0.09f * scale).coerceAtLeast(1.0f),
                        cap = StrokeCap.Round
                    )
                }
                if (graphicsQuality == GraphicsQuality.ULTRA && !facingBack) {
                    // Embroidered club crest badge on left chest
                    val crestCenter = Offset(topMid.x + spanW * 0.22f, topY + spanH * 0.34f)
                    val crestR = (0.11f * w * scale).coerceAtLeast(1.6f)
                    drawCircle(color = team.accentColor, radius = crestR, center = crestCenter)
                    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = crestR * 0.52f, center = crestCenter)
                }
            }
        }
        drawPath(
            tPath,
            color = if (player.stunTimer > 0f) Color(0xFFFF455F) else Color.Black.copy(alpha = 0.50f),
            style = Stroke(width = 1.15f)
        )

        // Jersey Number
        if (enableLights) {
            val label = player.jerseyNumber.toString()
            val fontSizeSp = ((if (facingBack) u * 0.36f else u * 0.25f) * w / density).coerceIn(5.0f, 10.0f).sp
            val isLightShirt = kitPrimary == Color(0xFFF5F7FA) || kitPrimary == Color(0xFFFFFFFF)
            val numColor = if (isLightShirt) Color(0xFF0E1726) else Color.White
            val style = TextStyle(
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSizeSp,
                color = numColor.copy(alpha = if (facingBack) 0.96f else 0.78f)
            )
            val measured = textMeasurer.measure(label, style = style)
            val centerTorso = p3(torsoCenter3)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    centerTorso.x - measured.size.width * 0.5f,
                    centerTorso.y - measured.size.height * 0.45f
                )
            )
        }
    }

    // 7) 3D Natural-Sized Sculpted Head, Neck, Ears, Face Features & Layered Hair
    val neckBase3 = collarCenter3
    val head3 = worldPt(torsoFwdLean * 1.10f + 0.06f, 0f, headCenterZ)
    parts += RenderPart3D(depth = depthOf(head3)) {
        val pNeck = p3(neckBase3)
        val hc = p3(head3)
        drawCylinder3D(pNeck, hc + Offset(0f, 0.20f * scale), 0.145f * w * scale, 0.122f * w * scale, skinShade, enableLights)

        val headRx = 0.34f * (0.92f + 0.08f * w) * scale
        val headRy = 0.43f * (0.94f + 0.06f * h) * scale

        // Ears
        if (abs(fy) < 0.75f) {
            drawCircle(
                color = skinShade,
                radius = 0.075f * scale,
                center = Offset(hc.x - fx * headRx * 0.65f, hc.y + 0.04f * scale)
            )
        } else {
            drawCircle(skinShade, 0.072f * scale, Offset(hc.x - headRx * 0.96f, hc.y + 0.04f * scale))
            drawCircle(skinShade, 0.072f * scale, Offset(hc.x + headRx * 0.96f, hc.y + 0.04f * scale))
        }

        val headPath = Path().apply {
            moveTo(hc.x - headRx, hc.y - headRy * 0.15f)
            quadraticTo(hc.x - headRx, hc.y - headRy, hc.x, hc.y - headRy)
            quadraticTo(hc.x + headRx, hc.y - headRy, hc.x + headRx, hc.y - headRy * 0.15f)
            quadraticTo(hc.x + headRx * 0.90f, hc.y + headRy * 0.68f, hc.x + fx * 0.05f * scale, hc.y + headRy)
            quadraticTo(hc.x - headRx * 0.90f, hc.y + headRy * 0.68f, hc.x - headRx, hc.y - headRy * 0.15f)
            close()
        }
        drawPath(
            headPath,
            brush = Brush.radialGradient(
                colors = listOf(skinHighlight, skinColor, skinShade),
                center = Offset(hc.x - headRx * 0.26f, hc.y - headRy * 0.30f),
                radius = headRy * 1.48f
            )
        )
        drawPath(headPath, color = Color.Black.copy(alpha = 0.32f), style = Stroke(width = 0.9f))

        if (facingBack) {
            drawOval(
                color = hairColor,
                topLeft = Offset(hc.x - headRx * 1.04f, hc.y - headRy * 1.05f),
                size = Size(headRx * 2.08f, headRy * 1.70f)
            )
            if (player.hairStyleIndex % 4 == 3) {
                drawLine(
                    color = team.accentColor,
                    start = Offset(hc.x - headRx * 1.02f, hc.y - headRy * 0.25f),
                    end = Offset(hc.x + headRx * 1.02f, hc.y - headRy * 0.25f),
                    strokeWidth = (0.12f * scale).coerceAtLeast(1.3f),
                    cap = StrokeCap.Round
                )
            }
        } else {
            when (player.hairStyleIndex % 4) {
                0 -> {
                    drawArc(
                        color = hairColor,
                        startAngle = 178f,
                        sweepAngle = 184f,
                        useCenter = true,
                        topLeft = Offset(hc.x - headRx * 1.04f, hc.y - headRy * 1.08f),
                        size = Size(headRx * 2.08f, headRy * 1.24f)
                    )
                }
                1 -> {
                    drawCircle(hairColor, radius = headRx * 0.56f, center = Offset(hc.x - headRx * 0.52f, hc.y - headRy * 0.68f))
                    drawCircle(hairColor, radius = headRx * 0.62f, center = Offset(hc.x, hc.y - headRy * 0.80f))
                    drawCircle(hairColor, radius = headRx * 0.56f, center = Offset(hc.x + headRx * 0.52f, hc.y - headRy * 0.68f))
                }
                2 -> {
                    drawArc(
                        color = hairColor,
                        startAngle = 165f,
                        sweepAngle = 210f,
                        useCenter = true,
                        topLeft = Offset(hc.x - headRx * 1.06f, hc.y - headRy * 1.06f),
                        size = Size(headRx * 2.12f, headRy * 1.18f)
                    )
                }
                else -> {
                    drawArc(
                        color = hairColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(hc.x - headRx * 1.02f, hc.y - headRy * 1.10f),
                        size = Size(headRx * 2.04f, headRy * 1.12f)
                    )
                    drawLine(
                        color = team.accentColor,
                        start = Offset(hc.x - headRx * 1.02f, hc.y - headRy * 0.38f),
                        end = Offset(hc.x + headRx * 1.02f, hc.y - headRy * 0.38f),
                        strokeWidth = (0.12f * scale).coerceAtLeast(1.3f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Eyes, Brows, Nose & Mouth
            val eyeOffsetX = fx * headRx * 0.52f
            val eyeY = hc.y - 0.015f * scale
            val browY = eyeY - 0.08f * scale
            if (abs(fx) < 0.55f) {
                val eyeSep = headRx * 0.36f
                listOf(hc.x + eyeOffsetX * 0.45f - eyeSep, hc.x + eyeOffsetX * 0.45f + eyeSep).forEach { ex ->
                    drawLine(hairColor.copy(alpha = 0.80f), Offset(ex - 0.065f * scale, browY), Offset(ex + 0.065f * scale, browY), strokeWidth = 1.0f, cap = StrokeCap.Round)
                    drawCircle(Color.White, (0.056f * scale).coerceAtLeast(0.9f), Offset(ex, eyeY))
                    drawCircle(Color(0xFF151515), (0.034f * scale).coerceAtLeast(0.7f), Offset(ex + fx * 0.018f * scale, eyeY))
                }
                drawLine(skinShade, Offset(hc.x + eyeOffsetX * 0.35f, eyeY + 0.03f * scale), Offset(hc.x + eyeOffsetX * 0.45f, eyeY + 0.13f * scale), strokeWidth = 0.9f, cap = StrokeCap.Round)
                drawLine(skinShade.copy(alpha = 0.90f), Offset(hc.x + eyeOffsetX * 0.35f - 0.07f * scale, hc.y + headRy * 0.54f), Offset(hc.x + eyeOffsetX * 0.35f + 0.07f * scale, hc.y + headRy * 0.54f), strokeWidth = 1.0f, cap = StrokeCap.Round)
            } else {
                val ex = hc.x + fx * headRx * 0.48f
                drawLine(hairColor.copy(alpha = 0.80f), Offset(ex - 0.065f * scale, browY), Offset(ex + 0.065f * scale, browY), strokeWidth = 1.0f, cap = StrokeCap.Round)
                drawCircle(Color.White, (0.056f * scale).coerceAtLeast(0.9f), Offset(ex, eyeY))
                drawCircle(Color(0xFF151515), (0.036f * scale).coerceAtLeast(0.7f), Offset(ex + fx * 0.020f * scale, eyeY))
                drawLine(skinShade.copy(alpha = 0.88f), Offset(hc.x + fx * headRx * 0.55f, hc.y + headRy * 0.52f), Offset(hc.x + fx * headRx * 0.82f, hc.y + headRy * 0.50f), strokeWidth = 1.0f, cap = StrokeCap.Round)
            }
        }

        if (enableLights) {
            drawArc(
                color = Color.White.copy(alpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.28f else 0.18f),
                startAngle = 205f,
                sweepAngle = 95f,
                useCenter = false,
                topLeft = Offset(hc.x - headRx * 0.72f, hc.y - headRy * 0.86f),
                size = Size(headRx * 1.44f, headRy * 0.76f),
                style = Stroke(width = if (graphicsQuality == GraphicsQuality.ULTRA) 1.35f else 1.1f, cap = StrokeCap.Round)
            )
            if (graphicsQuality == GraphicsQuality.ULTRA && !facingBack) {
                drawArc(
                    color = skinShade.copy(alpha = 0.42f),
                    startAngle = 25f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(hc.x - headRx * 0.82f, hc.y - headRy * 0.05f),
                    size = Size(headRx * 1.64f, headRy * 0.96f),
                    style = Stroke(width = 1.0f, cap = StrokeCap.Round)
                )
            }
        }
    }

    // Execute Painter's 3D Z-Sorted Render Queue!
    parts.sortBy { it.depth }
    parts.forEach { it.draw() }

    // 8) Overhead Stamina Bar, Cursors, Shot Charge Bar & Active Name Plate (Always Crisp Above Head)
    val headTopPos = p3(Vec3(head3.x, head3.y, head3.z + 0.90f))

    // Overhead Stamina Bar above every player
    val staminaFrac = player.stamina.coerceIn(0f, 1f)
    val staminaBarW = (3.2f * scale).coerceIn(20f, 54f)
    val staminaBarH = (0.38f * scale).coerceIn(3.0f, 5.5f)
    val staminaBarX = headTopPos.x - staminaBarW * 0.5f
    val staminaBarY = headTopPos.y - (0.34f * scale).coerceIn(3f, 8f) - staminaBarH
    val staminaRadius = CornerRadius(staminaBarH * 0.5f, staminaBarH * 0.5f)

    drawRoundRect(
        color = Color(0xFF06110B).copy(alpha = 0.80f),
        topLeft = Offset(staminaBarX, staminaBarY),
        size = Size(staminaBarW, staminaBarH),
        cornerRadius = staminaRadius
    )
    if (staminaFrac > 0.01f) {
        val staminaColors = when {
            staminaFrac > 0.55f -> listOf(TurfEmerald, Color(0xFF56F08D))
            staminaFrac > 0.25f -> listOf(Color(0xFFE09F1F), ChampionshipGold)
            else -> listOf(Color(0xFFD92638), Color(0xFFFF455F))
        }
        drawRoundRect(
            brush = Brush.horizontalGradient(staminaColors),
            topLeft = Offset(staminaBarX, staminaBarY),
            size = Size(staminaBarW * staminaFrac, staminaBarH),
            cornerRadius = staminaRadius
        )
    }
    drawRoundRect(
        color = if (isControlled) ElectricCyan.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.28f),
        topLeft = Offset(staminaBarX, staminaBarY),
        size = Size(staminaBarW, staminaBarH),
        cornerRadius = staminaRadius,
        style = Stroke(width = if (isControlled) 1.2f else 0.8f)
    )

    if (isControlled) {
        val cursorBottomY = staminaBarY - (0.24f * scale).coerceIn(2.5f, 6f)
        val triH = (0.76f * scale).coerceIn(6f, 16f)
        val triHalfW = (0.50f * scale).coerceIn(4f, 11f)
        val triPath = Path().apply {
            moveTo(headTopPos.x, cursorBottomY)
            lineTo(headTopPos.x - triHalfW, cursorBottomY - triH)
            lineTo(headTopPos.x + triHalfW, cursorBottomY - triH)
            close()
        }
        drawPath(triPath, brush = Brush.verticalGradient(listOf(Color(0xFF7AF2FF), ElectricCyan)))
        drawPath(triPath, color = Color(0xFF042228), style = Stroke(width = 1.2f))

        if (shotCharge > 0.02f) {
            val barW = 4.2f * scale
            val barH = (0.68f * scale).coerceAtLeast(4.2f)
            val barX = headTopPos.x - barW * 0.5f
            val barY = cursorBottomY - triH - (0.35f * scale).coerceIn(3f, 8f) - barH
            drawRoundRect(Color.Black.copy(alpha = 0.82f), Offset(barX, barY), Size(barW, barH), CornerRadius(4f, 4f))
            val fillColor = if (shotCharge > 0.82f) Color(0xFFFF455F) else ChampionshipGold
            drawRoundRect(Brush.horizontalGradient(listOf(TurfEmerald, fillColor)), Offset(barX, barY), Size(barW * shotCharge, barH), CornerRadius(4f, 4f))
            drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(barX, barY), Size(barW, barH), CornerRadius(4f, 4f), style = Stroke(1f))
        }
    } else if (isPassTarget) {
        val cursorBottomY = staminaBarY - (0.20f * scale).coerceIn(2f, 5f)
        val triH = (0.64f * scale).coerceIn(5f, 13f)
        val triHalfW = (0.42f * scale).coerceIn(3.5f, 9f)
        val triPath = Path().apply {
            moveTo(headTopPos.x, cursorBottomY)
            lineTo(headTopPos.x - triHalfW, cursorBottomY - triH)
            lineTo(headTopPos.x + triHalfW, cursorBottomY - triH)
            close()
        }
        drawPath(triPath, color = ChampionshipGold.copy(alpha = 0.92f))
        drawPath(triPath, color = Color(0xFF332300), style = Stroke(width = 1f))
    }

    if (isControlled && enableLights) {
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
        val tagY = py + u * 0.56f
        drawRoundRect(Color(0xFF06110B).copy(alpha = 0.86f), Offset(tagX, tagY), Size(tagW, tagH), CornerRadius(6f, 6f))
        drawRoundRect(ElectricCyan.copy(alpha = 0.45f), Offset(tagX, tagY), Size(tagW, tagH), CornerRadius(6f, 6f), style = Stroke(1f))
        drawText(textLayoutResult = measuredName, topLeft = Offset(px - measuredName.size.width * 0.5f, tagY + 1f))
    }
}

private fun DrawScope.drawBallBroadcast(
    ball: BallEntity,
    camX: Float,
    camY: Float,
    scale: Float,
    graphicsQuality: GraphicsQuality
) {
    val groundPos = project3D(ball.x, ball.y, 0f, camX, camY, scale)
    val bx = groundPos.x
    val by = groundPos.y
    val heightScale = 1f + (ball.z * 0.032f).coerceIn(0f, 0.36f)
    // Realistic FIFA size-5 football scale relative to pro footballers & 6u goal
    val r = 0.45f * scale * heightScale

    // Altitude-aware turf shadow
    if (graphicsQuality != GraphicsQuality.LOW) {
        val shadowOffset = (0.24f + ball.z * 0.45f) * scale
        val shadowAlpha = (0.48f - ball.z * 0.025f).coerceIn(0.15f, 0.48f)
        drawOval(
            brush = Brush.radialGradient(
                listOf(Color.Black.copy(alpha = shadowAlpha), Color.Transparent),
                center = Offset(bx + shadowOffset * 0.35f, by + shadowOffset * 0.60f),
                radius = r * 1.25f
            ),
            topLeft = Offset(bx - r * 0.96f + shadowOffset * 0.35f, by + shadowOffset * 0.25f),
            size = Size(r * 1.92f, r * 0.96f)
        )
    }

    val ballPos3D = project3D(ball.x, ball.y, ball.z * 0.60f, camX, camY, scale)
    val drawX = ballPos3D.x
    val drawY = ballPos3D.y
    val ballClip = Path().apply {
        addOval(
            Rect(
                left = drawX - r,
                top = drawY - r,
                right = drawX + r,
                bottom = drawY + r
            )
        )
    }

    // PES 2013 Spherical Pearl-White Match Ball with Curved Geodesic Panels
    clipPath(ballClip) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFEBEFEF), Color(0xFF83968B)),
                center = Offset(drawX - r * 0.32f, drawY - r * 0.34f),
                radius = r * 1.52f
            ),
            radius = r,
            center = Offset(drawX, drawY)
        )

        val panelDark = Color(0xFF102235)
        val panelMid = Color(0xFF1E3E5C)
        val seamColor = Color(0xFF284256).copy(alpha = 0.65f)

        val centerPent = Path().apply {
            for (k in 0 until 5) {
                val a = ball.spinAngle + k * (2f * PI.toFloat() / 5f)
                val px = drawX + cos(a) * r * 0.34f
                val py = drawY + sin(a) * r * 0.34f
                if (k == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(
            centerPent,
            brush = Brush.radialGradient(
                listOf(panelMid, panelDark),
                center = Offset(drawX - r * 0.1f, drawY - r * 0.1f),
                radius = r * 0.45f
            )
        )

        for (k in 0 until 5) {
            val a = ball.spinAngle + k * (2f * PI.toFloat() / 5f)
            val innerX = drawX + cos(a) * r * 0.33f
            val innerY = drawY + sin(a) * r * 0.33f
            val outerX = drawX + cos(a) * r * 0.80f
            val outerY = drawY + sin(a) * r * 0.80f
            drawLine(
                color = seamColor,
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = (r * 0.085f).coerceAtLeast(0.9f)
            )
            val patchCenter = Offset(drawX + cos(a) * r * 0.86f, drawY + sin(a) * r * 0.86f)
            drawCircle(color = panelDark, radius = r * 0.26f, center = patchCenter)
            if (graphicsQuality == GraphicsQuality.HIGH || graphicsQuality == GraphicsQuality.ULTRA) {
                val midA = a + (PI.toFloat() / 5f)
                drawLine(
                    color = ChampionshipGold.copy(alpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.75f else 0.58f),
                    start = Offset(drawX + cos(midA) * r * 0.48f, drawY + sin(midA) * r * 0.48f),
                    end = Offset(drawX + cos(midA) * r * 0.78f, drawY + sin(midA) * r * 0.78f),
                    strokeWidth = (r * 0.075f).coerceAtLeast(0.8f),
                    cap = StrokeCap.Round
                )
            }
        }

        if (graphicsQuality != GraphicsQuality.LOW) {
            drawCircle(
                color = Color.White.copy(alpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.64f else 0.48f),
                radius = r * (if (graphicsQuality == GraphicsQuality.ULTRA) 0.28f else 0.24f),
                center = Offset(drawX - r * 0.34f, drawY - r * 0.36f)
            )
            if (graphicsQuality == GraphicsQuality.ULTRA) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.88f),
                    radius = r * 0.11f,
                    center = Offset(drawX - r * 0.37f, drawY - r * 0.39f)
                )
                drawPath(
                    centerPent,
                    color = Color.White.copy(alpha = 0.22f),
                    style = Stroke(width = (r * 0.06f).coerceAtLeast(0.6f))
                )
            }
            drawArc(
                color = Color(0xFF9DF0B0).copy(alpha = if (graphicsQuality == GraphicsQuality.ULTRA) 0.32f else 0.22f),
                startAngle = 35f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(drawX - r * 0.92f, drawY - r * 0.92f),
                size = Size(r * 1.84f, r * 1.84f),
                style = Stroke(width = (r * 0.10f).coerceAtLeast(0.8f))
            )
        }
    }

    drawCircle(
        color = Color(0xFF0E1813),
        radius = r,
        center = Offset(drawX, drawY),
        style = Stroke(width = (0.14f * scale).coerceAtLeast(1.0f))
    )
}
