package com.philornot.siekiera.ui.screens.main.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Ulepszona komponent licznika timera z lepszymi animacjami. Pokazuje
 * godziny i minuty, reaguje na stan timera.
 *
 * @param modifier Modifier dla komponentu
 * @param minutes Liczba minut do wyświetlenia
 * @param isDragging Czy użytkownik przeciąga wartość
 * @param isActive Czy timer jest aktywny
 * @param isPaused Czy timer jest spauzowany
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimerDaysCounter(
    modifier: Modifier = Modifier,
    minutes: Int,
    isDragging: Boolean = false,
    isActive: Boolean = false,
    isPaused: Boolean = false,
) {
    // Obliczenie godzin i minut
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    // Animacje na podstawie stanu
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1.0f,
        animationSpec = tween(200, easing = LinearEasing),
        label = "drag_scale"
    )

    val pulseAnimation = remember { Animatable(1f) }
    val glowAnimation = remember { Animatable(0f) }

    // Animacje stanu timera
    LaunchedEffect(isActive, isPaused) {
        when {
            isActive && !isPaused -> {
                // Timer aktywny - subtelne pulsowanie
                launch {
                    pulseAnimation.animateTo(
                        targetValue = 1.02f, animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }
                // Delikatny efekt świecenia
                launch {
                    glowAnimation.animateTo(
                        targetValue = 0.3f, animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }
            }

            isPaused -> {
                // Timer spauzowany - powolne pulsowanie
                launch {
                    pulseAnimation.animateTo(
                        targetValue = 1.01f, animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }
                launch {
                    glowAnimation.animateTo(0.1f, animationSpec = tween(300))
                }
            }

            else -> {
                // Timer nieaktywny - reset animacji
                launch {
                    pulseAnimation.animateTo(1f, animationSpec = tween(300))
                }
                launch {
                    glowAnimation.animateTo(0f, animationSpec = tween(300))
                }
            }
        }
    }

    // Kolory na podstawie stanu
    val cardColor = when {
        isActive && !isPaused -> MaterialTheme.colorScheme.primaryContainer
        isPaused -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        isActive && !isPaused -> MaterialTheme.colorScheme.primary
        isPaused -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .size(width = 200.dp, height = 180.dp)
            .scale(dragScale)
            .graphicsLayer(
                scaleX = pulseAnimation.value, scaleY = pulseAnimation.value
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 8.dp else 4.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            // Efekt świecenia w tle dla aktywnego timera
            if (glowAnimation.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    textColor.copy(alpha = glowAnimation.value * 0.2f),
                                    textColor.copy(alpha = 0f)
                                )
                            ), shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                if (hours > 0) {
                    // Pokazuj godziny gdy timer > 60 minut
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Godziny
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(
                                targetState = hours, transitionSpec = {
                                    slideInVertically { height -> -height } + fadeIn() togetherWith slideOutVertically { height -> height } + fadeOut()
                                }, label = "hours_animation"
                            ) { animatedHours ->
                                Text(
                                    text = animatedHours.toString(),
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "h",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Minuty
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedContent(
                                targetState = remainingMinutes, transitionSpec = {
                                    slideInVertically { height -> -height } + fadeIn() togetherWith slideOutVertically { height -> height } + fadeOut()
                                }, label = "minutes_animation"
                            ) { animatedMinutes ->
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d", animatedMinutes),
                                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text(
                                text = "min",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Tylko minuty gdy timer < 60 minut
                    AnimatedContent(
                        targetState = minutes, transitionSpec = {
                            when {
                                targetState > initialState -> {
                                    // Zwiększanie - wjeżdża z góry
                                    slideInVertically { height -> -height } + scaleIn(initialScale = 0.9f) + fadeIn() togetherWith slideOutVertically { height -> height } + scaleOut(
                                        targetScale = 1.1f
                                    ) + fadeOut()
                                }

                                targetState < initialState -> {
                                    // Zmniejszanie - wjeżdża z dołu
                                    slideInVertically { height -> height } + scaleIn(initialScale = 0.9f) + fadeIn() togetherWith slideOutVertically { height -> -height } + scaleOut(
                                        targetScale = 1.1f
                                    ) + fadeOut()
                                }

                                else -> {
                                    // Bez zmiany
                                    fadeIn() togetherWith fadeOut()
                                }
                            }
                        }, label = "minutes_only_animation"
                    ) { animatedMinutes ->
                        Text(
                            text = animatedMinutes.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "minut",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                // Wskaźnik stanu timera
                if (isActive || isPaused) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .background(
                                color = when {
                                    isActive && !isPaused -> textColor
                                    isPaused -> textColor.copy(alpha = 0.5f)
                                    else -> textColor.copy(alpha = 0.3f)
                                }, shape = RoundedCornerShape(2.dp)
                            )
                            .alpha(
                                if (isActive && !isPaused) {
                                    // Pulsowanie dla aktywnego timera
                                    (0.5f + glowAnimation.value * 0.5f).coerceIn(0.3f, 1f)
                                } else {
                                    0.5f
                                }
                            )
                    )
                }
            }
        }
    }
}