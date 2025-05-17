package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.theme.AccentBlue
import com.philornot.siekiera.ui.theme.AccentPink
import com.philornot.siekiera.ui.theme.AccentTeal
import com.philornot.siekiera.ui.theme.PurpleLight
import com.philornot.siekiera.ui.theme.PurplePastel
import com.philornot.siekiera.ui.theme.PurplePrimary
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Efekt wystrzału konfetti kiedy czas się skończy i prezent zostanie
 * ujawniony.
 */
@Composable
fun ConfettiEffect(
    particleCount: Int = 150,
    durationMillis: Int = 3000,
) {
    // Wymiary kontenera
    var size by remember { mutableStateOf(IntSize.Zero) }

    // Stan animacji
    var isPlaying by remember { mutableStateOf(false) }

    // Uruchom efekt konfetti
    LaunchedEffect(Unit) {
        isPlaying = true
        delay(durationMillis.toLong())
        isPlaying = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                size = coordinates.size
            }) {
        // Generuj cząsteczki konfetti
        repeat(particleCount) { index ->
            if (size != IntSize.Zero) {
                ConfettiParticle(
                    containerSize = size,
                    isPlaying = isPlaying,
                    durationMillis = durationMillis
                )
            }
        }
    }
}

/** Pojedyncza cząsteczka konfetti. */
@Composable
private fun ConfettiParticle(
    containerSize: IntSize,
    isPlaying: Boolean,
    durationMillis: Int,
) {
    // Losowe opóźnienie dla każdej cząsteczki
    val delay = remember { Random.nextInt(0, 1500) }
    val isVisible = remember { mutableStateOf(false) }

    // Uruchom cząsteczkę z opóźnieniem
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            delay(delay.toLong())
            isVisible.value = true
            delay(durationMillis.toLong())
            isVisible.value = false
        } else {
            isVisible.value = false
        }
    }

    // Losowe parametry cząsteczki
    with(LocalDensity.current) { containerSize.width.toDp() }
    with(LocalDensity.current) { containerSize.height.toDp() }

    val startX = remember { Random.nextInt(0, containerSize.width) }
    val startY = remember { -30 }
    val targetX = remember { startX + Random.nextInt(-100, 100) }
    val targetY = remember { containerSize.height + Random.nextInt(30, 100) }

    // Rozmiar i kolor cząsteczki
    val size = remember { Random.nextInt(8, 16).dp }
    val color = remember {
        when (Random.nextInt(0, 6)) {
            0 -> PurplePrimary
            1 -> PurpleLight
            2 -> AccentPink
            3 -> AccentBlue
            4 -> AccentTeal
            else -> PurplePastel
        }
    }

    // Losowy typ cząsteczki
    val particleType = remember {
        when (Random.nextInt(0, 3)) {
            0 -> ParticleType.CIRCLE
            1 -> ParticleType.SQUARE
            else -> ParticleType.TRIANGLE
        }
    }

    // Animacja pozycji i obrotu
    val animProgress by animateFloatAsState(
        targetValue = if (isVisible.value) 1f else 0f, animationSpec = tween(
            durationMillis = durationMillis, easing = FastOutSlowInEasing
        ), label = "confettiProgress"
    )

    // Animacja ruchu cząsteczki w pionie i poziomie
    val currentX = startX + (targetX - startX) * animProgress
    val currentY = startY + (targetY - startY) * animProgress

    // Animacja dodatkowego ruchu bocznego (falowanie)
    val infiniteTransition = rememberInfiniteTransition(label = "confettiWave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutLinearInEasing), repeatMode = RepeatMode.Reverse
        ), label = "wave"
    )

    // Amplituda bocznego ruchu
    val waveAmplitude = remember { Random.nextInt(5, 30) }
    val additionalX = waveOffset * waveAmplitude

    // Rotacja cząsteczki
    val infiniteRotation = rememberInfiniteTransition(label = "confettiRotation")
    val rotation by infiniteRotation.animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = Random.nextInt(1000, 3000), easing = LinearEasing
            )
        ), label = "rotation"
    )

    // Renderowanie cząsteczki
    if (isVisible.value) {
        Box(
            modifier = Modifier
                .offset(
                    x = with(LocalDensity.current) { (currentX + additionalX).toDp() },
                    y = with(LocalDensity.current) { currentY.toDp() })
                .rotate(rotation)
                .scale(
                    animateFloatAsState(
                        targetValue = if (animProgress > 0.8f) 1f - (animProgress - 0.8f) * 5f else 1f,
                        label = "scaleOut"
                    ).value
                )
                .alpha(
                    animateFloatAsState(
                        targetValue = if (animProgress > 0.8f) 1f - (animProgress - 0.8f) * 5f else 1f,
                        label = "fadeOut"
                    ).value
                )) {
            when (particleType) {
                ParticleType.CIRCLE -> {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(
                                color = color, shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }

                ParticleType.SQUARE -> {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(
                                color = color,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            )
                    )
                }

                ParticleType.TRIANGLE -> {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(color, color.copy(alpha = 0.7f)),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.value, size.value)
                                ), shape = androidx.compose.ui.graphics.RectangleShape
                            )
                            .rotate(45f)
                    )
                }
            }
        }
    }
}

private enum class ParticleType {
    CIRCLE, SQUARE, TRIANGLE
}