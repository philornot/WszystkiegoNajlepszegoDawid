package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
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
import kotlin.random.Random

/** Karta z cyframi czasu z animowanymi przejściami cyfr */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimeDigitCard(
    modifier: Modifier = Modifier.Companion,
    digits: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        // Karta z animowanymi cyframi
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.Companion.size(width = 56.dp, height = 72.dp)
        ) {
            Box(
                contentAlignment = Alignment.Companion.Center,
                modifier = Modifier.Companion.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Companion.CenterVertically,
                ) {
                    // Animuj każdą cyfrę osobno
                    for (i in digits.indices) {
                        AnimatedContent(
                            targetState = digits[i], transitionSpec = {
                                // Wybierz losowy typ animacji dla każdej zmiany cyfry
                                val animationType = Random.Default.nextInt(6)
                                when (animationType) {
                                    0 -> {
                                        // Przesunięcie pionowe
                                        val direction = if (Random.Default.nextBoolean()) 1 else -1
                                        slideInVertically { height -> direction * height } + fadeIn() togetherWith slideOutVertically { height -> -direction * height } + fadeOut()
                                    }

                                    1 -> {
                                        // Przesunięcie poziome
                                        val direction = if (Random.Default.nextBoolean()) 1 else -1
                                        slideInHorizontally { width -> direction * width } + fadeIn() togetherWith slideOutHorizontally { width -> -direction * width } + fadeOut()
                                    }

                                    2 -> {
                                        // Animacja skalowania
                                        scaleIn(initialScale = 0.8f) + fadeIn() togetherWith scaleOut(
                                            targetScale = 1.2f
                                        ) + fadeOut()
                                    }

                                    3 -> {
                                        // Animacja odbicia (zamiast rotacji)
                                        slideInVertically {
                                            if (Random.Default.nextBoolean()) it else -it
                                        } + fadeIn() togetherWith slideOutVertically {
                                            if (Random.Default.nextBoolean()) -it else it
                                        } + fadeOut()
                                    }

                                    4 -> {
                                        // Przenikanie
                                        fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith fadeOut(
                                            animationSpec = tween(durationMillis = 200)
                                        )
                                    }

                                    else -> {
                                        // Animacja łączona
                                        (slideInVertically { it / 2 } + fadeIn() + scaleIn(
                                            initialScale = 0.9f
                                        )) togetherWith (slideOutVertically { -it / 2 } + fadeOut() + scaleOut(
                                            targetScale = 1.1f
                                        ))
                                    }
                                }
                            }, label = "digitTransition"
                        ) { digit ->
                            Text(
                                text = digit.toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Companion.Bold,
                                textAlign = TextAlign.Companion.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.Companion.height(4.dp))

        // Etykieta
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Companion.Center
        )
    }
}