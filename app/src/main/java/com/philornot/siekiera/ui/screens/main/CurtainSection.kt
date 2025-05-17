package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philornot.siekiera.ui.theme.CurtainAccent
import com.philornot.siekiera.ui.theme.CurtainPrimary
import com.philornot.siekiera.ui.theme.CurtainSecondary
import com.philornot.siekiera.ui.theme.PurplePrimary

/**
 * Sekcja kurtyny lub prezentu w zależności od czasu.
 *
 * @param isTimeUp Czy nadszedł już czas urodzin
 * @param onGiftClicked Funkcja wywoływana po kliknięciu prezentu
 * @param modifier Modyfikator dla kontenera
 */
@Composable
fun CurtainSection(
    modifier: Modifier = Modifier,
    isTimeUp: Boolean,
    onGiftClicked: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("curtain_container"),
        contentAlignment = Alignment.Center
    ) {
        // Animacja kurtyny
        AnimatedVisibility(
            visible = !isTimeUp,
            enter = fadeIn(),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            ) + slideOutVertically(
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                ),
                targetOffsetY = { it }
            ),
            modifier = Modifier.testTag("curtain")
        ) {
            FancyCurtain(modifier = Modifier.fillMaxSize())
        }

        // Animacja prezentu
        AnimatedVisibility(
            visible = isTimeUp,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 1200,
                    delayMillis = 500,
                    easing = LinearEasing
                )
            ),
            modifier = Modifier.testTag("gift_container")
        ) {
            FancyGift(
                modifier = Modifier.size(280.dp),
                onClick = onGiftClicked
            )
        }
    }
}

/**
 * Animowana, elegancka kurtyna z efektami.
 */
@Composable
fun FancyCurtain(modifier: Modifier = Modifier) {
    // Animacja ruchu kurtyny
    val infiniteTransition = rememberInfiniteTransition(label = "curtainTransition")
    val curtainOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "curtainWave"
    )

    // Gradientowe kolory kurtyny
    val gradient = Brush.verticalGradient(
        colors = listOf(
            CurtainPrimary,
            CurtainSecondary,
            CurtainPrimary.copy(alpha = 0.8f),
            CurtainSecondary.copy(alpha = 0.9f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        // Rysujemy ozdobne linie na kurtynie
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2

            // Pionowa linia podziału
            drawLine(
                color = CurtainAccent.copy(alpha = 0.5f),
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = 5f
            )

            // Fałdy kurtyny
            val foldCount = 12
            val foldWidth = width / foldCount

            for (i in 0..foldCount) {
                val x = i * foldWidth
                val path = Path().apply {
                    moveTo(x, 0f)
                    // Dodajemy przesunięcie bazujące na animacji
                    val waveOffset = curtainOffset * 15 * (i % 3 - 1)
                    cubicTo(
                        x - 20f, height / 3,
                        x + 20f + waveOffset, 2 * height / 3,
                        x, height
                    )
                }

                drawPath(
                    path = path,
                    color = CurtainAccent.copy(alpha = 0.3f),
                    style = Stroke(width = 2f)
                )
            }

            // Dodatkowe ozdoby
            for (i in 0..5) {
                val y = height * i / 5
                val wavePath = Path().apply {
                    moveTo(0f, y)
                    val amplitude = 15f * (1 + (i % 3))
                    for (x in 0..width.toInt() step 50) {
                        val waveY = y + amplitude * kotlin.math.sin((x / 50 + curtainOffset) * kotlin.math.PI.toFloat())
                        lineTo(x.toFloat(), waveY)
                    }
                }

                drawPath(
                    path = wavePath,
                    color = CurtainAccent.copy(alpha = 0.2f),
                    style = Stroke(width = 1f)
                )
            }
        }

        // Dodajemy napis na kurtynie
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "WKRÓTCE",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .offset(y = 8.dp * curtainOffset)
                    .padding(20.dp)
            )
        }
    }
}

/**
 * Elegancki, animowany prezent.
 */
@Composable
fun FancyGift(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Animacje prezentu
    val infiniteTransition = rememberInfiniteTransition(label = "giftTransition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "giftPulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "giftRotate"
    )

    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(150),
        label = "giftPress"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 8.dp,
        animationSpec = tween(150),
        label = "giftElevation"
    )

    // Prezent z efektami
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale * pressScale)
            .rotate(rotation)
            .shadow(elevation, CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PurplePrimary,
                        PurplePrimary.copy(alpha = 0.9f)
                    )
                )
            )
            .clickable {
                isPressed = true
                onClick()
            }
            .testTag("gift")
    ) {
        // Efekt opóźnionego resetowania stanu
        LaunchedEffect(isPressed) {
            if (isPressed) {
                kotlinx.coroutines.delay(300)
                isPressed = false
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Ikona prezentu
            Icon(
                imageVector = Icons.Filled.CardGiftcard,
                contentDescription = "Prezent",
                tint = Color.White,
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            )

            // Napis
            Text(
                text = "Odbierz swój prezent!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 22.sp
            )
        }
    }
}