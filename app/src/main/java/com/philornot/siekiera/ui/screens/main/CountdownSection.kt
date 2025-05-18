package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Redesigned countdown section with cleaner animations.
 *
 * @param modifier Modifier for the container
 * @param timeRemaining Remaining time in milliseconds
 * @param isTimeUp Whether time is up
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CountdownSection(
    modifier: Modifier = Modifier,
    timeRemaining: Long,
    isTimeUp: Boolean,
) {
    // Format the time string
    val formattedTime = com.philornot.siekiera.utils.TimeUtils.formatRemainingTime(timeRemaining)
    val (days, time) = if ("," in formattedTime) {
        formattedTime.split(", ")
    } else {
        listOf("0 dni", "00:00:00")
    }

    // Split time components
    val hoursPart = if (time.length >= 2) time.substring(0, 2) else "00"
    val minutesPart = if (time.length >= 5) time.substring(3, 5) else "00"
    val secondsPart = if (time.length >= 8) time.substring(6, 8) else "00"

    // Countdown UI
    AnimatedVisibility(
        visible = !isTimeUp,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .testTag("countdown_container")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = "Czas do urodzin:",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Days counter
            DaysCounter(days = days)

            Spacer(modifier = Modifier.height(16.dp))

            // Time (hours:minutes:seconds) with animated digits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hours
                TimeDigitCard(
                    digits = hoursPart, label = "Godzin"
                )

                // Separator
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Minutes
                TimeDigitCard(
                    digits = minutesPart, label = "Minut"
                )

                // Separator
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Seconds
                TimeDigitCard(
                    digits = secondsPart, label = "Sekund"
                )
            }
        }
    }

    // Message after countdown ends
    AnimatedVisibility(
        visible = isTimeUp, enter = fadeIn(), modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .testTag("countdown")
        ) {
            // Positive message after countdown ends
            Text(
                text = "Wszystkiego Najlepszego!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "18 lat to wyjątkowy moment!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Days counter component with a clean design */
@Composable
fun DaysCounter(
    modifier: Modifier = Modifier,
    days: String,
) {
    val dayText = days.split(" ")[0]
    val daysLabel = days.replace(dayText, "").trim()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            // Days count with centered text
            Text(
                text = dayText,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Days label with centered text
            Text(
                text = daysLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Time digit card with animated digit transitions */
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
        // Card with animated digits
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(width = 56.dp, height = 72.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Animate each digit separately
                    for (i in digits.indices) {
                        AnimatedContent(
                            targetState = digits[i], transitionSpec = {
                                // Choose a random animation type for each digit change
                                val animationType = Random.nextInt(6)
                                when (animationType) {
                                    0 -> {
                                        // Vertical slide
                                        val direction = if (Random.nextBoolean()) 1 else -1
                                        slideInVertically { height -> direction * height } + fadeIn() togetherWith slideOutVertically { height -> -direction * height } + fadeOut()
                                    }

                                    1 -> {
                                        // Horizontal slide
                                        val direction = if (Random.nextBoolean()) 1 else -1
                                        slideInHorizontally { width -> direction * width } + fadeIn() togetherWith slideOutHorizontally { width -> -direction * width } + fadeOut()
                                    }

                                    2 -> {
                                        // Scale animation
                                        scaleIn(initialScale = 0.8f) + fadeIn() togetherWith scaleOut(
                                            targetScale = 1.2f
                                        ) + fadeOut()
                                    }

                                    3 -> {
                                        // Bounce animation (instead of rotation)
                                        slideInVertically {
                                            if (Random.nextBoolean()) it else -it
                                        } + fadeIn() togetherWith slideOutVertically {
                                            if (Random.nextBoolean()) -it else it
                                        } + fadeOut()
                                    }

                                    4 -> {
                                        // Crossfade
                                        fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith fadeOut(
                                            animationSpec = tween(durationMillis = 200)
                                        )
                                    }

                                    else -> {
                                        // Combined animation
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
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}