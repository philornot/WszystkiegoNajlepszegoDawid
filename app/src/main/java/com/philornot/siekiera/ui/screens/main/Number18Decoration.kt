package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.theme.PurplePrimary
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.graphics.toColorInt

/**
 * Animowana dekoracja 18-tki dla ekranu urodzinowego. Rysuje olbrzymie
 * "18" z efektami świetlnymi w tle.
 *
 * @param isVisible czy element jest widoczny
 * @param modifier modyfikator do dostosowania kontenera
 */
@Composable
fun Number18Decoration(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    if (!isVisible) return

    // Wymiary kontenera
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Animacje dekoracji
    val infiniteTransition = rememberInfiniteTransition(label = "number18Transition")

    // Animacja rotacji
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 60000, easing = LinearEasing
            )
        ), label = "number18Rotation"
    )

    // Animacja pulsu
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f, animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000, easing = LinearEasing
            ), repeatMode = RepeatMode.Reverse
        ), label = "number18Pulse"
    )

    // Animacja promieni
    val rayScale by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1.3f, animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000, easing = LinearEasing
            ), repeatMode = RepeatMode.Reverse
        ), label = "rayScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                size = coordinates.size
            }, contentAlignment = Alignment.Center
    ) {
        // Rysowanie cyfry 18 z efektami
        if (size != IntSize.Zero) {
            Canvas(
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp)
            ) {
                val canvasWidth = this.size.width
                val canvasHeight = this.size.height
                val centerX = canvasWidth / 2
                val centerY = canvasHeight / 2
                val radius = min(canvasWidth, canvasHeight) / 2 * pulse

                // Rysuj promienie
                rotate(rotation) {
                    val rayCount = 16
                    for (i in 0 until rayCount) {
                        val angle = (i * 2 * kotlin.math.PI / rayCount).toFloat()
                        val startX = centerX + cos(angle) * radius * 0.5f
                        val startY = centerY + sin(angle) * radius * 0.5f
                        val endX = centerX + cos(angle) * radius * rayScale
                        val endY = centerY + sin(angle) * radius * rayScale

                        drawLine(
                            color = PurplePrimary.copy(alpha = 0.2f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 8f * (0.5f + 0.5f * pulse)
                        )
                    }
                }

                // Rysuj okręgi
                for (i in 0 until 3) {
                    drawCircle(
                        color = PurplePrimary.copy(alpha = 0.1f * (3 - i)),
                        radius = radius * (0.7f + 0.1f * i) * pulse,
                        style = Stroke(width = 3f)
                    )
                }

                // Rysuj "18" - używając natywnego Canvas dla lepszego renderowania tekstu
                var textSize = radius * 1.2f
                val paint = Paint().asFrameworkPaint().apply {
                    color = "#8B5CF6".toColorInt() // PurplePrimary
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    setShadowLayer(
                        10f,
                        0f,
                        0f,
                        "#A78BFA".toColorInt()
                    ) // PurpleLight
                }

                drawContext.canvas.nativeCanvas.drawText(
                    "18",
                    centerX,
                    centerY + textSize / 3, // Korekta do wyśrodkowania tekstu w pionie
                    paint
                )
            }
        }
    }
}