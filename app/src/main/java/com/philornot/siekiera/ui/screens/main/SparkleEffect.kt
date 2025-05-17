package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.philornot.siekiera.ui.theme.PurpleLight
import com.philornot.siekiera.ui.theme.PurplePrimary
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Efekt błyszczących gwiazd i iskier do podkreślenia uroczystego
 * charakteru.
 *
 * @param isVisible czy efekt jest widoczny
 * @param modifier modyfikator dla kontenera
 */
@Composable
fun SparkleEffect(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
) {
    if (!isVisible) return

    // Stan do kontrolowania animacji
    var sparkles by remember { mutableStateOf(generateSparkles()) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = LinearEasing
        ),
        label = "sparkleAlpha"
    )

    // Efekt odświeżania iskier
    LaunchedEffect(isVisible) {
        while (isVisible) {
            delay(2000) // Regeneruj co 2s
            sparkles = generateSparkles()
        }
    }

    // Kontener z efektem iskrzenia
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                // Najpierw rysuj oryginalną zawartość
                drawContent()

                // Następnie rysuj iskry
                if (alpha > 0f) {
                    sparkles.forEach { sparkle ->
                        // Limit widoczności dla lepszej wydajności
                        if (sparkle.x < size.width && sparkle.y < size.height) {
                            // Rozmiar iskry
                            val sparkSize = sparkle.size * alpha

                            when (sparkle.type) {
                                SparkleType.STAR -> {
                                    // Rysuj gwiazdę
                                    val points = 5
                                    val outerRadius = sparkSize
                                    val innerRadius = sparkSize * 0.4f

                                    val path = androidx.compose.ui.graphics.Path()
                                    for (i in 0 until points * 2) {
                                        val radius = if (i % 2 == 0) outerRadius else innerRadius
                                        val angle = kotlin.math.PI.toFloat() * i / points
                                        val x = sparkle.x + cos(angle) * radius
                                        val y = sparkle.y + sin(angle) * radius

                                        if (i == 0) {
                                            path.moveTo(x, y)
                                        } else {
                                            path.lineTo(x, y)
                                        }
                                    }
                                    path.close()

                                    drawPath(
                                        path = path,
                                        color = sparkle.color.copy(alpha = sparkle.alpha * alpha),
                                        style = androidx.compose.ui.graphics.drawscope.Fill
                                    )
                                }

                                SparkleType.DOT -> {
                                    // Rysuj kropkę
                                    drawCircle(
                                        color = sparkle.color.copy(alpha = sparkle.alpha * alpha),
                                        radius = sparkSize,
                                        center = Offset(sparkle.x, sparkle.y)
                                    )
                                }

                                SparkleType.LINE -> {
                                    // Rysuj linię/promień
                                    val angle = sparkle.angle
                                    val endX = sparkle.x + cos(angle) * sparkSize * 3
                                    val endY = sparkle.y + sin(angle) * sparkSize * 3

                                    drawLine(
                                        color = sparkle.color.copy(alpha = sparkle.alpha * alpha),
                                        start = Offset(sparkle.x, sparkle.y),
                                        end = Offset(endX, endY),
                                        strokeWidth = sparkSize / 3,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Tu będą osadzone inne elementy (które są dziećmi tego komponentu)
    }
}

/** Generuje listę różnych iskier do animacji. */
private fun generateSparkles(count: Int = 40): List<Sparkle> {
    val sparkles = mutableListOf<Sparkle>()

    repeat(count) {
        val x = Random.nextFloat() * 1000f
        val y = Random.nextFloat() * 2000f
        val size = Random.nextFloat() * 5f + 2f
        val alpha = Random.nextFloat() * 0.5f + 0.3f
        val color = if (Random.nextBoolean()) {
            PurplePrimary
        } else {
            PurpleLight
        }

        val type = when (Random.nextInt(10)) {
            0, 1, 2, 3 -> SparkleType.DOT
            4, 5, 6 -> SparkleType.STAR
            else -> SparkleType.LINE
        }

        val angle = Random.nextFloat() * 2 * kotlin.math.PI.toFloat()

        sparkles.add(
            Sparkle(
                x = x,
                y = y,
                size = size,
                alpha = alpha,
                color = color,
                type = type,
                angle = angle
            )
        )
    }

    return sparkles
}

/** Klasa reprezentująca pojedynczą iskrę. */
private data class Sparkle(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val color: Color,
    val type: SparkleType,
    val angle: Float,
)

/** Typ iskry. */
private enum class SparkleType {
    STAR, DOT, LINE
}