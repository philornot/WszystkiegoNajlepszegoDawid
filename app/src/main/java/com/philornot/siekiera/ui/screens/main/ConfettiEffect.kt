package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.philornot.siekiera.ui.theme.AccentMauve
import com.philornot.siekiera.ui.theme.LavenderPastel
import com.philornot.siekiera.ui.theme.LavenderPrimary
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * A subtle confetti effect that appears once when the time is up. This
 * effect is minimal and non-distracting.
 */
@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f, animationSpec = tween(
            durationMillis = if (isVisible) 500 else 1000, easing = LinearEasing
        ), label = "confettiAlpha"
    )

    // Generate the confetti particles only once
    val particles = remember { generateParticles(40) }

    // Automatically hide confetti after 3 seconds
    LaunchedEffect(Unit) {
        delay(3000)
        isVisible = false
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
    ) {
        // Draw the confetti particles
        particles.forEach { particle ->
            translate(particle.x * size.width, particle.y * size.height) {
                rotate(particle.rotation) {
                    when (particle.shape) {
                        ConfettiShape.CIRCLE -> {
                            drawCircle(
                                color = particle.color, radius = particle.size * 5, alpha = 0.7f
                            )
                        }

                        ConfettiShape.RECTANGLE -> {
                            drawRect(
                                color = particle.color, size = androidx.compose.ui.geometry.Size(
                                    particle.size * 10, particle.size * 4
                                ), alpha = 0.7f
                            )
                        }
                    }
                }
            }
        }
    }
}

// Confetti particle data class
private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val rotation: Float,
    val color: Color,
    val shape: ConfettiShape,
)

// Confetti shape enum
private enum class ConfettiShape {
    CIRCLE, RECTANGLE
}

// Generate random confetti particles
private fun generateParticles(count: Int): List<ConfettiParticle> {
    val particles = mutableListOf<ConfettiParticle>()
    repeat(count) {
        particles.add(
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.5f, // Only in top half of the screen
                size = Random.nextFloat() * 0.5f + 0.5f, // 0.5 to 1.0
                rotation = Random.nextFloat() * 360f,
                color = when (Random.nextInt(3)) {
                    0 -> LavenderPrimary
                    1 -> LavenderPastel
                    else -> AccentMauve
                },
                shape = if (Random.nextBoolean()) ConfettiShape.CIRCLE else ConfettiShape.RECTANGLE
            )
        )
    }
    return particles
}