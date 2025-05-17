package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.philornot.siekiera.ui.theme.Black
import com.philornot.siekiera.ui.theme.CurtainAccent
import com.philornot.siekiera.ui.theme.PurpleDark
import com.philornot.siekiera.ui.theme.PurpleLight
import com.philornot.siekiera.ui.theme.PurplePastel
import com.philornot.siekiera.ui.theme.PurplePrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animowane tło, które zmienia się w zależności od tego czy czas już
 * minął.
 *
 * @param isTimeUp Czy czas już upłynął.
 */
@Composable
fun AnimatedBackground(
    isTimeUp: Boolean,
    modifier: Modifier = Modifier,
) {
    // Wymiary kontenera
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Animacje tła
    val infiniteTransition = rememberInfiniteTransition(label = "backgroundTransition")

    // Animacja obrotu
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 60000, easing = LinearEasing
            )
        ), label = "backgroundRotation"
    )

    // Animacja pulsu
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f, animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000, easing = LinearEasing
            ), repeatMode = RepeatMode.Reverse
        ), label = "backgroundPulse"
    )

    // Animacja fali
    val wave by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(), animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10000, easing = LinearEasing
            )
        ), label = "backgroundWave"
    )

    // Główny gradient tła
    val backgroundGradient = if (isTimeUp) {
        // Świąteczny gradient
        Brush.radialGradient(
            colors = listOf(
                PurplePastel.copy(alpha = 0.5f),
                PurpleLight.copy(alpha = 0.3f),
                PurplePrimary.copy(alpha = 0.2f),
                Black.copy(alpha = 0.9f)
            )
        )
    } else {
        // Spokojny gradient oczekiwania
        Brush.radialGradient(
            colors = listOf(
                PurpleDark.copy(alpha = 0.7f), Black.copy(alpha = 0.8f), Black.copy(alpha = 0.9f)
            )
        )
    }

    // Kontener tła
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black) // Kolor tła pod gradientem
            .onGloballyPositioned { coordinates ->
                size = coordinates.size
            }) {
        // Rysowanie tła
        if (size != IntSize.Zero) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundGradient)
            ) {
                // Rysowanie dekoracyjnych elementów tła
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = (size.width.coerceAtMost(size.height) / 2f) * pulse

                    // Rysuj tło w zależności od stanu
                    if (isTimeUp) {
                        // Świąteczne tło z promienistymi elementami
                        rotate(rotation) {
                            translate(centerX, centerY) {
                                // Promieniste linie
                                val rayCount = 12
                                for (i in 0 until rayCount) {
                                    val angle = (i * 2 * PI / rayCount).toFloat()
                                    val startX = 0f
                                    val startY = 0f
                                    val endX = cos(angle) * radius * 1.2f
                                    val endY = sin(angle) * radius * 1.2f

                                    drawLine(
                                        color = when (i % 3) {
                                            0 -> PurplePrimary.copy(alpha = 0.3f)
                                            1 -> PurplePastel.copy(alpha = 0.3f)
                                            else -> CurtainAccent.copy(alpha = 0.3f)
                                        },
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = 10f * (0.5f + sin(wave + i) * 0.5f)
                                    )
                                }

                                // Kilka okręgów
                                for (i in 0 until 3) {
                                    val circleRadius =
                                        radius * (0.5f + i * 0.25f) * (0.9f + 0.1f * sin(wave * i))
                                    drawCircle(
                                        color = when (i) {
                                            0 -> PurplePrimary.copy(alpha = 0.1f)
                                            1 -> PurplePastel.copy(alpha = 0.1f)
                                            else -> PurpleLight.copy(alpha = 0.1f)
                                        },
                                        radius = circleRadius,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 5f * (0.7f + 0.3f * cos(wave + i))
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Tło oczekiwania z delikatnymi wzorami
                        rotate(rotation / 2) {
                            translate(centerX, centerY) {
                                // Kilka okręgów pulssujących
                                for (i in 0 until 3) {
                                    val circleRadius =
                                        radius * (0.4f + i * 0.2f) * (0.9f + 0.1f * sin(wave + i))
                                    drawCircle(
                                        color = when (i) {
                                            0 -> PurpleDark.copy(alpha = 0.1f)
                                            1 -> PurplePrimary.copy(alpha = 0.1f)
                                            else -> PurplePastel.copy(alpha = 0.1f)
                                        },
                                        radius = circleRadius,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 2f
                                        )
                                    )
                                }

                                // Delikatna siatka
                                val gridSize = 10
                                val cellSize = radius * 2 / gridSize
                                for (i in -gridSize / 2..gridSize / 2) {
                                    for (j in -gridSize / 2..gridSize / 2) {
                                        val x = i * cellSize
                                        val y = j * cellSize
                                        val distance = kotlin.math.sqrt(x * x + y * y)
                                        if (distance < radius) {
                                            drawCircle(
                                                color = PurplePrimary.copy(
                                                    alpha = 0.05f * (1f - distance / radius)
                                                ),
                                                radius = 2f + 1f * sin(wave + i + j),
                                                center = Offset(x, y)
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