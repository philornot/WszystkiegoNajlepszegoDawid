package com.philornot.siekiera.ui.screens.main.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Modifier that applies a screen shake effect with increasing intensity.
 * Useful for creating tension as the countdown approaches zero.
 *
 * @param timeRemaining Time remaining in milliseconds
 * @param enabled Whether the effect is enabled
 * @param maxShakeIntensity Maximum shake intensity (default 5f)
 */
fun Modifier.shakeEffect(
    timeRemaining: Long,
    enabled: Boolean = true,
    maxShakeIntensity: Float = 5f,
): Modifier = composed {
    if (!enabled) return@composed this

    // Calculate the shake intensity - increases as time remaining decreases
    val secondsRemaining = timeRemaining / 1000

    // Only shake when less than 10 seconds remain
    if (secondsRemaining >= 10) return@composed this

    // Calculate intensity - the closer to 0, the more intense
    // Map from [0, 10] seconds to [0.1, 1.0] intensity factor
    // For 10 seconds, intensity = 0.1 * maxShakeIntensity
    // For 0 seconds, intensity = 1.0 * maxShakeIntensity
    val intensityFactor = 1.0f - (secondsRemaining.toFloat() / 10f) * 0.9f

    // Apply an extra burst of intensity in the final 3 seconds
    val finalBurstFactor = if (secondsRemaining <= 3) {
        1.5f - (secondsRemaining.toFloat() / 3f) * 0.5f
    } else {
        1.0f
    }

    val finalIntensity = maxShakeIntensity * intensityFactor * finalBurstFactor

    // Calculate shake frequency - increases as time remaining decreases
    1.0f + (1.0f - (secondsRemaining.toFloat() / 10f)) * 2.0f

    // Current shake offsets
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    // Randomize the next shake values
    LaunchedEffect(timeRemaining) {
        // Generate random values every time the time changes
        val randomAngle = Random.nextFloat() * 2f * Math.PI.toFloat()
        val randomDistance = Random.nextFloat() * finalIntensity

        offsetX = sin(randomAngle) * randomDistance
        offsetY = sin(randomAngle + 1.5f) * randomDistance
        rotation = (Random.nextFloat() - 0.5f) * finalIntensity * 0.5f

        // Add slight scale pulsing
        scale = if (secondsRemaining <= 3) {
            1f + (Random.nextFloat() - 0.5f) * 0.015f * finalIntensity
        } else {
            1f
        }

        // Add a short pause then reset to center for next frame
        delay(50)
        offsetX = 0f
        offsetY = 0f
        rotation = 0f
        scale = 1f
    }

    // Apply the shake effect
    this.graphicsLayer {
        translationX = offsetX
        translationY = offsetY
        rotationZ = rotation
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin.Center
    }
}

/**
 * Modifier extension to create a screen flash effect, typically used for
 * important countdown milestones.
 */
fun Modifier.flashEffect(
    timeRemaining: Long,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this

    // Current alpha value for the flash overlay
    val flashAlpha = remember { Animatable(0f) }

    // Only flash at specific milestones
    LaunchedEffect(timeRemaining) {
        // Convert Long to Int for safe comparison with the Int list
        val secondsRemaining = (timeRemaining / 1000).toInt()

        // Flash on specific second marks - explicitly using Int list for clarity
        val milestones = listOf<Int>(10, 5, 3, 2, 1)
        if (milestones.contains(secondsRemaining)) {
            // Quickly flash the screen
            flashAlpha.snapTo(0.3f) // Start slightly visible
            flashAlpha.animateTo(
                targetValue = 0f, animationSpec = tween(
                    durationMillis = 200, easing = LinearEasing
                )
            )

            // For the final second, add extra flashes
            if (secondsRemaining <= 3) {
                delay(100)
                flashAlpha.snapTo(0.2f)
                flashAlpha.animateTo(
                    targetValue = 0f, animationSpec = tween(
                        durationMillis = 150, easing = LinearEasing
                    )
                )
            }
        }
    }

    // Apply the flash effect
    this.drawWithContent {
        drawContent()
        // Draw a white overlay with the current alpha
        if (flashAlpha.value > 0) {
            drawRect(
                color = Color.White, alpha = flashAlpha.value
            )
        }
    }
}