package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.philornot.siekiera.ui.theme.AccentMauve
import com.philornot.siekiera.ui.theme.AccentPeriwinkle
import com.philornot.siekiera.ui.theme.LavenderLight
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti explosion effect when the gift button is clicked.
 * This creates a concentrated burst of colorful particles from a center point.
 *
 * @param modifier Modifier for the container
 * @param centerX X-coordinate of the explosion center point (0.0-1.0)
 * @param centerY Y-coordinate of the explosion center point (0.0-1.0)
 */
@Composable
fun ConfettiExplosionEffect(
    modifier: Modifier = Modifier,
    centerX: Float = 0.5f,
    centerY: Float = 0.5f
) {
    // Remember if animation is running
    var isAnimating by remember { mutableStateOf(true) }

    // Track explosion progress
    val explosionProgress = remember { Animatable(0f) }

    // Generate particles only once
    val particles = remember {
        List(150) {
            ConfettiExplosionParticle(
                angle = Random.nextFloat() * 2f * PI.toFloat(),
                distance = Random.nextFloat() * 0.5f + 0.1f,
                size = Random.nextFloat() * 10f + 5f,
                color = randomConfettiColor(),
                speed = Random.nextFloat() * 0.5f + 0.5f,
                rotationSpeed = Random.nextFloat() * 10f - 5f,
                fadeSpeed = Random.nextFloat() * 0.3f + 0.7f
            )
        }
    }

    // Start the explosion animation
    LaunchedEffect(Unit) {
        explosionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = LinearEasing)
        )
        delay(1500)
        isAnimating = false
    }

    // Only draw if animation is running
    if (!isAnimating) return

    // Draw the explosion
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Center point of the explosion
        val explosionCenterX = centerX * canvasWidth
        val explosionCenterY = centerY * canvasHeight

        particles.forEach { particle ->
            // Calculate current position based on progress and particle properties
            val progress = explosionProgress.value * particle.speed

            // Use easing for natural motion - fast start, then slow down
            val easedProgress = if (progress < 0.7f) {
                // Accelerate outward
                FastOutSlowInEasing.transform(progress / 0.7f)
            } else {
                // Slow down at end and start falling slightly
                val p = (progress - 0.7f) / 0.3f
                val outProgress = 1f - (p * 0.2f)
                outProgress
            }

            // Calculate position
            val distance = size.minDimension * particle.distance * easedProgress
            val x = explosionCenterX + cos(particle.angle) * distance
            val y = explosionCenterY + sin(particle.angle) * distance

            // Add gravity effect - particles fall down slightly as they explode
            val gravity = progress * progress * 50f

            // Alpha fades based on progress and particle properties
            val alpha = (1f - progress).coerceIn(0f, 1f) * particle.fadeSpeed

            translate(x, y + gravity) {
                rotate(progress * particle.rotationSpeed * 360f) {
                    // Draw different shapes
                    when (particle.shape) {
                        ConfettiShape.CIRCLE -> {
                            drawCircle(
                                color = particle.color.copy(alpha = alpha),
                                radius = particle.size * (1f - progress * 0.3f)
                            )
                        }
                        ConfettiShape.RECTANGLE -> {
                            drawRect(
                                color = particle.color.copy(alpha = alpha),
                                size = androidx.compose.ui.geometry.Size(
                                    particle.size * 2f * (1f - progress * 0.3f),
                                    particle.size * (1f - progress * 0.3f)
                                ),
                                topLeft = Offset(
                                    -particle.size,
                                    -particle.size / 2f
                                )
                            )
                        }
                        ConfettiShape.TRIANGLE -> {
                            val path = androidx.compose.ui.graphics.Path()
                            val size = particle.size * (1f - progress * 0.3f)
                            path.moveTo(0f, -size)
                            path.lineTo(size, size)
                            path.lineTo(-size, size)
                            path.close()

                            drawPath(
                                path = path,
                                color = particle.color.copy(alpha = alpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function to get random confetti color
private fun randomConfettiColor(): Color {
    return when (Random.nextInt(12)) {
        0 -> Color(0xFFFF5252) // Bright red
        1 -> Color(0xFFFFEB3B) // Bright yellow
        2 -> Color(0xFF2196F3) // Bright blue
        3 -> Color(0xFFE040FB) // Bright purple
        4 -> Color(0xFF76FF03) // Bright green
        5 -> Color(0xFFFF9800) // Bright orange
        6 -> Color(0xFFFF4081) // Pink
        7 -> Color(0xFF00BCD4) // Cyan
        8 -> LavenderLight     // Lavender (theme color)
        9 -> AccentMauve       // Mauve (theme color)
        10 -> AccentPeriwinkle  // Periwinkle (theme color)
        else -> Color.White     // White
    }
}

// Confetti explosion particle data class
private data class ConfettiExplosionParticle(
    val angle: Float,            // Angle in radians
    val distance: Float,         // Normalized max distance to travel
    val size: Float,             // Size of the particle
    val color: Color,            // Color of the particle
    val shape: ConfettiShape = ConfettiShape.entries.toTypedArray().random(), // Shape of particle
    val speed: Float,            // Speed multiplier (0.0-1.0)
    val rotationSpeed: Float,    // How fast it rotates
    val fadeSpeed: Float         // How quickly it fades (0.0-1.0)
)

// Confetti shape enum
private enum class ConfettiShape {
    CIRCLE, RECTANGLE, TRIANGLE
}