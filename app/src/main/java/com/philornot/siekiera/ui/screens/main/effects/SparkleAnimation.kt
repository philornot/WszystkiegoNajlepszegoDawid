package com.philornot.siekiera.ui.screens.main.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Sparkle animation that can be applied to specific elements */
@Composable
fun SparkleAnimation(
    modifier: Modifier = Modifier,
) {
    // Track sparkle animations
    val sparkles = remember { List(8) { Animatable(0f) } }

    // Launch animations for sparkles
    LaunchedEffect(Unit) {
        sparkles.forEachIndexed { index, animatable ->
            launch {
                delay(index * 100L) // Stagger start times

                // Pulse animation
                while (true) {
                    animatable.animateTo(
                        targetValue = 1f, animationSpec = tween(
                            durationMillis = 600, easing = LinearEasing
                        )
                    )
                    animatable.animateTo(
                        targetValue = 0f, animationSpec = tween(
                            durationMillis = 600, easing = LinearEasing
                        )
                    )

                    // Random delay between pulses
                    delay(Random.Default.nextLong(100, 500))
                }
            }
        }
    }

    // Draw the sparkles
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw each sparkle
        sparkles.forEachIndexed { index, animatable ->
            val progress = animatable.value

            if (progress > 0) {
                // Calculate position based on index
                val angle = (index.toFloat() / sparkles.size) * 2 * PI
                val distance = canvasWidth * 0.45f // Just inside the container

                val x = canvasWidth / 2 + cos(angle).toFloat() * distance
                val y = canvasHeight / 2 + sin(angle).toFloat() * distance

                // Draw sparkle
                val sparkleSize = 5f * progress

                drawCircle(
                    color = Color.Companion.White.copy(alpha = progress * 0.7f),
                    radius = sparkleSize,
                    center = Offset(x, y)
                )

                // Draw sparkle rays
                val rayLength = sparkleSize * 2
                for (ray in 0 until 4) {
                    val rayAngle = ray * (PI / 2)
                    val endX = x + cos(rayAngle).toFloat() * rayLength
                    val endY = y + sin(rayAngle).toFloat() * rayLength

                    drawLine(
                        color = Color.Companion.White.copy(alpha = progress * 0.5f),
                        start = Offset(x, y),
                        end = Offset(endX, endY),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}