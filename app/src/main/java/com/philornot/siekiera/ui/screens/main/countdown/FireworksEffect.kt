package com.philornot.siekiera.ui.screens.main.countdown

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.philornot.siekiera.ui.screens.main.effects.Firework
import com.philornot.siekiera.ui.screens.main.effects.FireworkPhase
import com.philornot.siekiera.ui.screens.main.effects.ShootingStar
import com.philornot.siekiera.ui.screens.main.effects.createFirework
import com.philornot.siekiera.ui.screens.main.effects.createShootingStar
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Vibrant, energetic fireworks effect with multiple explosions and
 * particles. Designed to create a celebratory experience for the user.
 *
 * @param modifier Modifier for the container
 */
@Composable
fun FireworksEffect(
    modifier: Modifier = Modifier,
) {
    // List to hold active fireworks
    val fireworks = remember { mutableListOf<Firework>() }

    // State to trigger redrawing
    var redrawTrigger by remember { mutableLongStateOf(0L) }

    // Animation scale factor to control overall intensity
    val scale by rememberInfiniteTransition(label = "scaleTransition").animateFloat(
        initialValue = 0.95f, targetValue = 1.05f, animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "scaleAnimation"
    )

    // Launch effect to continuously create new fireworks
    LaunchedEffect(Unit) {
        // Add initial fireworks immediately for dramatic effect
        repeat(3) {
            val centerX = Random.nextFloat() * 0.8f + 0.1f // Between 0.1 and 0.9
            val centerY = Random.nextFloat() * 0.5f + 0.1f // Higher in the screen
            fireworks.add(createFirework(centerX, centerY))
        }

        // Continue adding fireworks every few milliseconds
        while (true) {
            // Add new fireworks at random intervals
            if (Random.nextFloat() < 0.2f && fireworks.size < 10) { // Limit concurrent fireworks
                val centerX = Random.nextFloat() * 0.8f + 0.1f
                val centerY = Random.nextFloat() * 0.6f + 0.1f
                fireworks.add(createFirework(centerX, centerY))
            }

            // Update all fireworks and remove expired ones
            fireworks.removeAll { firework ->
                firework.update()
                firework.age > firework.lifetime
            }

            // Trigger redraw
            redrawTrigger = System.currentTimeMillis()

            delay(16) // Approximately 60fps
        }
    }

    // Draw the fireworks
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw each firework
        fireworks.forEach { firework ->
            // Skip fireworks that haven't exploded yet if they're in launch phase
            if (firework.phase == FireworkPhase.LAUNCH && firework.age < firework.launchDuration) {
                // Draw launch trail
                val progress = firework.age.toFloat() / firework.launchDuration
                val startY = canvasHeight * 0.9f
                val currentY = startY + (firework.centerY * canvasHeight - startY) * progress

                // Draw rocket trail
                for (i in 0 until 5) {
                    val trailProgress = i / 5f
                    val trailY = currentY + (startY - currentY) * trailProgress
                    val trailSize = 4f * (1 - trailProgress) * scale
                    drawCircle(
                        color = firework.color.copy(alpha = 0.7f * (1 - trailProgress)),
                        radius = trailSize,
                        center = Offset(firework.centerX * canvasWidth, trailY)
                    )
                }

                // Draw rocket
                drawCircle(
                    color = firework.color,
                    radius = 6f * scale,
                    center = Offset(firework.centerX * canvasWidth, currentY)
                )
                return@forEach
            }

            // If we're here, the firework has exploded or is in explosion phase
            if (firework.phase == FireworkPhase.LAUNCH && firework.age >= firework.launchDuration) {
                firework.phase = FireworkPhase.EXPLODE
                firework.age = 0 // Reset age for explosion phase
            }

            // Draw explosion
            val center = Offset(firework.centerX * canvasWidth, firework.centerY * canvasHeight)
            val explosionProgress = (firework.age.toFloat() / firework.lifetime).coerceIn(0f, 1f)

            // Maximum radius based on explosion size and screen dimensions
            val maxRadius = minOf(canvasWidth, canvasHeight) * firework.size * 0.3f

            // Current explosion radius
            val radius = maxRadius * explosionProgress

            // For each particle in the explosion
            for (i in 0 until firework.particleCount) {
                val particleAngle = (i.toFloat() / firework.particleCount) * 2 * PI
                val variance = Random.nextFloat() * 0.2f + 0.9f // 0.9 to 1.1

                // Particles follow slightly different paths
                (explosionProgress * variance).coerceIn(0f, 1f)

                // Particle movement follows an arc - accelerates out then falls slightly
                val distanceModifier = if (explosionProgress < 0.7f) {
                    // Accelerate outward
                    FastOutSlowInEasing.transform(explosionProgress / 0.7f)
                } else {
                    // Hold near max distance with slight gravity effect
                    1.0f - (explosionProgress - 0.7f) * 0.3f
                }

                val particleRadius = radius * distanceModifier

                // Calculate position with some variance
                val x = center.x + cos(particleAngle).toFloat() * particleRadius
                val y = center.y + sin(particleAngle).toFloat() * particleRadius

                // Particle size decreases over time
                val particleSize = (14f * (1f - explosionProgress) + 2f) * scale

                // Add twinkle effect
                val twinkle = 0.7f + (sin(explosionProgress * 12f + i) * 0.3f).toFloat()

                // Draw the particle
                drawCircle(
                    color = firework.color.copy(
                        alpha = (1f - explosionProgress) * twinkle
                    ), radius = particleSize, center = Offset(x, y)
                )

                // Add "sparkle" effects for some particles
                if (i % 3 == 0) {
                    val sparkleSize = particleSize * 0.6f
                    val sparkleAngle = (System.currentTimeMillis() % 360).toFloat() + i

                    translate(x, y) {
                        rotate(sparkleAngle) {
                            // Draw small lines extending from particle
                            drawLine(
                                color = Color.White.copy(alpha = (1f - explosionProgress) * 0.8f * twinkle),
                                start = Offset(0f, 0f),
                                end = Offset(sparkleSize * 2f, 0f),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = (1f - explosionProgress) * 0.8f * twinkle),
                                start = Offset(0f, 0f),
                                end = Offset(0f, sparkleSize * 2f),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shooting Star effect that creates a background of occasional shooting
 * stars For added visual interest in the celebration screen
 */
@Composable
fun ShootingStarsEffect(
    modifier: Modifier = Modifier,
) {
    // List to hold active shooting stars
    val shootingStars = remember { mutableListOf<ShootingStar>() }

    // State to trigger redrawing
    var redrawTrigger by remember { mutableLongStateOf(0L) }

    // Launch effect to create shooting stars
    LaunchedEffect(Unit) {
        // Continue adding shooting stars
        while (true) {
            // Occasionally add a new shooting star
            if (Random.nextFloat() < 0.05f && shootingStars.size < 3) {
                shootingStars.add(createShootingStar())
            }

            // Update all stars and remove completed ones
            shootingStars.removeAll { star ->
                star.update()
                star.progress >= 1f
            }

            // Trigger redraw
            redrawTrigger = System.currentTimeMillis()

            delay(16) // Approximately 60fps
        }
    }

    // Draw the shooting stars
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        shootingStars.forEach { star ->
            // Calculate current position
            val startX = star.startX * canvasWidth
            val startY = star.startY * canvasHeight
            val endX = star.endX * canvasWidth
            val endY = star.endY * canvasHeight

            val currentX = startX + (endX - startX) * star.progress
            val currentY = startY + (endY - startY) * star.progress

            // Draw the star head
            drawCircle(
                color = Color.White.copy(alpha = 0.8f * (1f - star.progress)),
                radius = 2f,
                center = Offset(currentX, currentY)
            )

            // Draw the trail
            for (i in 1..8) {
                val trailProgress = i / 8f
                val trailX = currentX - (endX - startX) * trailProgress * 0.15f
                val trailY = currentY - (endY - startY) * trailProgress * 0.15f

                drawCircle(
                    color = Color.White.copy(alpha = 0.5f * (1f - trailProgress) * (1f - star.progress)),
                    radius = 1.5f * (1f - trailProgress),
                    center = Offset(trailX, trailY)
                )
            }
        }
    }
}

/**
 * Explosively vibrant fireworks display that triggers when the gift is
 * clicked This is the main celebratory effect shown immediately after
 * interaction with the gift
 */
@Composable
fun ExplosiveFireworksDisplay(
    modifier: Modifier = Modifier,
) {
    // State to track whether the intense initial burst is active
    var showIntenseBurst by remember { mutableStateOf(true) }

    // After a short while, reduce intensity for a more subtle continuous effect
    LaunchedEffect(Unit) {
        showIntenseBurst = true
        delay(2500)  // Show intense burst for 2.5 seconds
        showIntenseBurst = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background shooting stars
        ShootingStarsEffect()

        // Normal fireworks effect continues throughout
        FireworksEffect()

        // Intense concentrated burst of fireworks when first displayed
        if (showIntenseBurst) {
            // Track extra fireworks for the intense burst
            val burstFireworks = remember { mutableListOf<Firework>() }
            var redrawTrigger by remember { mutableLongStateOf(0L) }

            // Create an intense burst of fireworks
            LaunchedEffect(Unit) {
                // Add many fireworks at once for dramatic effect
                repeat(15) {
                    // Concentrate more fireworks in the center
                    val centerX = 0.5f + (Random.nextFloat() - 0.5f) * 0.8f
                    val centerY = 0.4f + (Random.nextFloat() - 0.5f) * 0.6f

                    // Stagger the launch slightly
                    delay(Random.nextLong(0, 200))
                    burstFireworks.add(createFirework(centerX, centerY))
                }

                // Update the burst fireworks
                while (burstFireworks.isNotEmpty()) {
                    burstFireworks.removeAll { firework ->
                        firework.update()
                        firework.age > firework.lifetime
                    }

                    redrawTrigger = System.currentTimeMillis()
                    delay(16)
                }
            }

            // Draw the intense burst
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw each firework with the same logic as the primary FireworksEffect
                burstFireworks.forEach { firework ->
                    // Same drawing logic as FireworksEffect
                    if (firework.phase == FireworkPhase.LAUNCH && firework.age < firework.launchDuration) {
                        val progress = firework.age.toFloat() / firework.launchDuration
                        val startY = canvasHeight * 0.9f
                        val currentY =
                            startY + (firework.centerY * canvasHeight - startY) * progress

                        // Draw rocket trail
                        for (i in 0 until 5) {
                            val trailProgress = i / 5f
                            val trailY = currentY + (startY - currentY) * trailProgress
                            val trailSize = 4f * (1 - trailProgress)
                            drawCircle(
                                color = firework.color.copy(alpha = 0.7f * (1 - trailProgress)),
                                radius = trailSize,
                                center = Offset(firework.centerX * canvasWidth, trailY)
                            )
                        }

                        // Draw rocket
                        drawCircle(
                            color = firework.color,
                            radius = 6f,
                            center = Offset(firework.centerX * canvasWidth, currentY)
                        )
                        return@forEach
                    }

                    // Handle explosion phase
                    if (firework.phase == FireworkPhase.LAUNCH && firework.age >= firework.launchDuration) {
                        firework.phase = FireworkPhase.EXPLODE
                        firework.age = 0
                    }

                    // Draw explosion
                    val center =
                        Offset(firework.centerX * canvasWidth, firework.centerY * canvasHeight)
                    val explosionProgress =
                        (firework.age.toFloat() / firework.lifetime).coerceIn(0f, 1f)
                    val maxRadius = minOf(canvasWidth, canvasHeight) * firework.size * 0.3f
                    val radius = maxRadius * explosionProgress

                    for (i in 0 until firework.particleCount) {
                        val particleAngle = (i.toFloat() / firework.particleCount) * 2 * PI
                        val variance = Random.nextFloat() * 0.2f + 0.9f

                        (explosionProgress * variance).coerceIn(0f, 1f)

                        val distanceModifier = if (explosionProgress < 0.7f) {
                            FastOutSlowInEasing.transform(explosionProgress / 0.7f)
                        } else {
                            1.0f - (explosionProgress - 0.7f) * 0.3f
                        }

                        val particleRadius = radius * distanceModifier

                        val x = center.x + cos(particleAngle).toFloat() * particleRadius
                        val y = center.y + sin(particleAngle).toFloat() * particleRadius

                        val particleSize = (14f * (1f - explosionProgress) + 2f)

                        val twinkle = 0.7f + (sin(explosionProgress * 12f + i) * 0.3f).toFloat()

                        drawCircle(
                            color = firework.color.copy(
                                alpha = (1f - explosionProgress) * twinkle
                            ), radius = particleSize, center = Offset(x, y)
                        )

                        if (i % 3 == 0) {
                            val sparkleSize = particleSize * 0.6f
                            val sparkleAngle = (System.currentTimeMillis() % 360).toFloat() + i

                            translate(x, y) {
                                rotate(sparkleAngle) {
                                    drawLine(
                                        color = Color.White.copy(alpha = (1f - explosionProgress) * 0.8f * twinkle),
                                        start = Offset(0f, 0f),
                                        end = Offset(sparkleSize * 2f, 0f),
                                        strokeWidth = 1f
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = (1f - explosionProgress) * 0.8f * twinkle),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, sparkleSize * 2f),
                                        strokeWidth = 1f
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

