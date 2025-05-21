package com.philornot.siekiera.ui.screens.main.effects

import androidx.compose.ui.graphics.Color
import com.philornot.siekiera.ui.theme.AccentBlueishLavender
import com.philornot.siekiera.ui.theme.AccentMauve
import com.philornot.siekiera.ui.theme.AccentPeriwinkle
import com.philornot.siekiera.ui.theme.LavenderLight
import com.philornot.siekiera.ui.theme.LavenderPrimary
import com.philornot.siekiera.ui.theme.SuccessGreen
import com.philornot.siekiera.ui.theme.WarningAmber
import kotlin.random.Random

/** Firework phases */
enum class FireworkPhase {
    LAUNCH, EXPLODE
}

/** Firework data class */
internal class Firework(
    val centerX: Float,
    val centerY: Float,
    val color: Color,
    val size: Float,
    val particleCount: Int,
    val lifetime: Long,
    val launchDuration: Long = 500,
    var phase: FireworkPhase = FireworkPhase.LAUNCH,
    var age: Long = 0,
) {
    fun update(): Boolean {
        age += 16 // Approximately 16ms per frame at 60fps
        return age <= lifetime
    }
}

/** Function to create a random firework */
internal fun createFirework(x: Float, y: Float): Firework {
    // Create vibrant, celebratory colors
    val colors = listOf(
        Color(0xFFFF5252), // Bright red
        Color(0xFFFFEB3B), // Bright yellow
        Color(0xFF2196F3), // Bright blue
        Color(0xFFE040FB), // Bright purple
        Color(0xFF76FF03), // Bright green
        Color(0xFFFF9800), // Bright orange
        LavenderLight,     // Lavender (theme color)
        AccentMauve,       // Mauve (theme color)
        AccentPeriwinkle,  // Periwinkle (theme color)
        LavenderPrimary,   // Purple (theme color)
        AccentBlueishLavender, // Blueish lavender (theme color)
        SuccessGreen,      // Green (theme color)
        WarningAmber       // Amber (theme color)
    )

    return Firework(
        centerX = x,
        centerY = y,
        color = colors.random(),
        size = Random.nextFloat() * 0.5f + 0.5f, // Size varies 0.5 to 1.0
        particleCount = Random.nextInt(12, 24),
        lifetime = Random.nextLong(800, 1500),
        launchDuration = Random.nextLong(300, 700) // Time to launch before explosion
    )
}

/** Shooting star data class */
internal class ShootingStar(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val duration: Long,
    var progress: Float = 0f,
    var lastUpdateTime: Long = System.currentTimeMillis(),
) {
    fun update(): Boolean {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastUpdateTime
        progress += elapsed / duration.toFloat()
        lastUpdateTime = currentTime
        return progress < 1f
    }
}

/** Function to create a random shooting star */
internal fun createShootingStar(): ShootingStar {
    // Start from top or right edge
    val startFromTop = Random.nextBoolean()

    val startX = if (startFromTop) Random.nextFloat() else 1f
    val startY = if (startFromTop) 0f else Random.nextFloat() * 0.5f

    // End somewhere lower and to the left
    val endX = startX - Random.nextFloat() * 0.5f - 0.2f
    val endY = startY + Random.nextFloat() * 0.5f + 0.2f

    return ShootingStar(
        startX = startX,
        startY = startY,
        endX = endX.coerceAtLeast(0f),
        endY = endY.coerceAtMost(1f),
        duration = Random.nextLong(800, 1500)
    )
}