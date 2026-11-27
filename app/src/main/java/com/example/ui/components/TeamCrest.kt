package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.CrestPattern
import com.example.model.Team
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TeamCrest(
    team: Team,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.90f, h * 0.16f)
            cubicTo(
                w * 0.90f, h * 0.56f,
                w * 0.74f, h * 0.80f,
                w * 0.50f, h * 0.95f
            )
            cubicTo(
                w * 0.26f, h * 0.80f,
                w * 0.10f, h * 0.56f,
                w * 0.10f, h * 0.16f
            )
            close()
        }

        clipPath(shieldPath) {
            drawRect(color = team.primaryColor)

            when (team.crestPattern) {
                CrestPattern.STRIPES -> {
                    val stripeW = w * 0.18f
                    drawRect(
                        color = team.secondaryColor,
                        topLeft = Offset(w * 0.20f, 0f),
                        size = Size(stripeW, h)
                    )
                    drawRect(
                        color = team.secondaryColor,
                        topLeft = Offset(w * 0.62f, 0f),
                        size = Size(stripeW, h)
                    )
                }

                CrestPattern.SASH -> {
                    val sash = Path().apply {
                        moveTo(0f, h * 0.75f)
                        lineTo(w * 0.75f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h * 0.25f)
                        lineTo(w * 0.25f, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(sash, color = team.secondaryColor)
                }

                CrestPattern.CHEVRON -> {
                    val chev = Path().apply {
                        moveTo(0f, h * 0.35f)
                        lineTo(w * 0.5f, h * 0.62f)
                        lineTo(w, h * 0.35f)
                        lineTo(w, h * 0.58f)
                        lineTo(w * 0.5f, h * 0.84f)
                        lineTo(0f, h * 0.58f)
                        close()
                    }
                    drawPath(chev, color = team.secondaryColor)
                }

                CrestPattern.HALF -> {
                    drawRect(
                        color = team.secondaryColor,
                        topLeft = Offset(w * 0.5f, 0f),
                        size = Size(w * 0.5f, h)
                    )
                }

                CrestPattern.STAR -> {
                    drawCircle(
                        color = team.secondaryColor.copy(alpha = 0.28f),
                        radius = w * 0.32f,
                        center = Offset(w * 0.5f, h * 0.48f)
                    )
                    val starPath = Path()
                    val cx = w * 0.5f
                    val cy = h * 0.47f
                    val outerR = w * 0.22f
                    val innerR = w * 0.095f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outerR else innerR
                        val angle = (-PI / 2.0 + i * PI / 5.0)
                        val px = cx + (cos(angle) * r).toFloat()
                        val py = cy + (sin(angle) * r).toFloat()
                        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                    }
                    starPath.close()
                    drawPath(starPath, color = team.accentColor)
                }

                CrestPattern.HOOPS -> {
                    drawRect(
                        color = team.secondaryColor,
                        topLeft = Offset(0f, h * 0.30f),
                        size = Size(w, h * 0.16f)
                    )
                    drawRect(
                        color = team.secondaryColor,
                        topLeft = Offset(0f, h * 0.60f),
                        size = Size(w, h * 0.16f)
                    )
                }
            }

            // Gloss highlight across top-left of shield
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.24f), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.7f, h * 0.7f)
                )
            )
        }

        // Metallic rim
        drawPath(
            path = shieldPath,
            color = team.accentColor,
            style = Stroke(width = (w * 0.065f).coerceAtLeast(2f))
        )
    }
}
