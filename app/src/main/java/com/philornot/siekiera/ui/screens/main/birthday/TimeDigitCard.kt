package com.philornot.siekiera.ui.screens.main.birthday

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Ulepszona karta z cyframi czasu z płynniejszymi animacjami. Dostosowana
 * do responsywności timera.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimeDigitCard(
    modifier: Modifier = Modifier,
    digits: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        // Karta z animowanymi cyframi
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(width = 56.dp, height = 72.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Animuj każdą cyfrę osobno z ulepszonym systemem animacji
                    for (i in digits.indices) {
                        AnimatedContent(
                            targetState = digits[i], transitionSpec = {
                                // Deterministyczne animacje bazowane na pozycji cyfry
                                val animationType = (i + digits[i].code) % 8

                                when (animationType) {
                                    0 -> {
                                        // Płynne przesunięcie pionowe w dół
                                        slideInVertically { height -> -height } + fadeIn(
                                            animationSpec = tween(250, easing = LinearEasing)
                                        ) togetherWith slideOutVertically { height -> height } + fadeOut(
                                            animationSpec = tween(200, easing = LinearEasing)
                                        )
                                    }

                                    1 -> {
                                        // Płynne przesunięcie pionowe w górę
                                        slideInVertically { height -> height } + fadeIn(
                                            animationSpec = tween(250, easing = LinearEasing)
                                        ) togetherWith slideOutVertically { height -> -height } + fadeOut(
                                            animationSpec = tween(200, easing = LinearEasing)
                                        )
                                    }

                                    2 -> {
                                        // Przesunięcie poziome z lewa
                                        slideInHorizontally { width -> -width } + fadeIn(
                                            animationSpec = tween(200, easing = LinearEasing)
                                        ) togetherWith slideOutHorizontally { width -> width } + fadeOut(
                                            animationSpec = tween(150, easing = LinearEasing)
                                        )
                                    }

                                    3 -> {
                                        // Przesunięcie poziome z prawa
                                        slideInHorizontally { width -> width } + fadeIn(
                                            animationSpec = tween(200, easing = LinearEasing)
                                        ) togetherWith slideOutHorizontally { width -> -width } + fadeOut(
                                            animationSpec = tween(150, easing = LinearEasing)
                                        )
                                    }

                                    4 -> {
                                        // Płynna animacja skalowania
                                        scaleIn(
                                            initialScale = 0.8f,
                                            animationSpec = tween(250, easing = LinearEasing)
                                        ) + fadeIn(
                                            animationSpec = tween(200, easing = LinearEasing)
                                        ) togetherWith scaleOut(
                                            targetScale = 1.2f,
                                            animationSpec = tween(200, easing = LinearEasing)
                                        ) + fadeOut(
                                            animationSpec = tween(150, easing = LinearEasing)
                                        )
                                    }

                                    5 -> {
                                        // Kombinacja przesunięcia i skalowania
                                        (slideInVertically { it / 3 } + scaleIn(
                                            initialScale = 0.9f,
                                            animationSpec = tween(250, easing = LinearEasing)
                                        ) + fadeIn(
                                            animationSpec = tween(200, easing = LinearEasing)
                                        )) togetherWith (slideOutVertically { -it / 3 } + scaleOut(
                                            targetScale = 1.1f,
                                            animationSpec = tween(200, easing = LinearEasing)
                                        ) + fadeOut(
                                            animationSpec = tween(150, easing = LinearEasing)
                                        ))
                                    }

                                    6 -> {
                                        // Szybka animacja dla sekund (częstych zmian)
                                        fadeIn(
                                            animationSpec = tween(150, easing = LinearEasing)
                                        ) togetherWith fadeOut(
                                            animationSpec = tween(100, easing = LinearEasing)
                                        )
                                    }

                                    else -> {
                                        // Animacja dyagonalna
                                        (slideInVertically { it / 2 } + slideInHorizontally { it / 2 } + fadeIn(
                                            animationSpec = tween(200, easing = LinearEasing)
                                        )) togetherWith (slideOutVertically { -it / 2 } + slideOutHorizontally { -it / 2 } + fadeOut(
                                            animationSpec = tween(150, easing = LinearEasing)
                                        ))
                                    }
                                }
                            }, label = "digitTransition_$i"
                        ) { digit ->
                            Text(
                                text = digit.toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Etykieta z subtelną animacją
        AnimatedContent(
            targetState = label, transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
            }, label = "labelTransition"
        ) { animatedLabel ->
            Text(
                text = animatedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}