package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.theme.PurpleDark
import com.philornot.siekiera.ui.theme.PurpleLight
import com.philornot.siekiera.ui.theme.PurplePrimary
import com.philornot.siekiera.ui.theme.White
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sekcja odliczania do urodzin, z poprawioną animacją i wycentrowanym
 * tekstem. Teraz z losowymi, nieprzewidywalnymi animacjami dla lepszego
 * efektu.
 *
 * @param modifier Modyfikator dla kontenera
 * @param timeRemaining Pozostały czas w milisekundach
 * @param isTimeUp Czy czas już minął
 */
@Composable
fun CountdownSection(
    modifier: Modifier = Modifier,
    timeRemaining: Long,
    isTimeUp: Boolean,
) {
    // Formatowanie czasu na części
    val formattedTime = com.philornot.siekiera.utils.TimeUtils.formatRemainingTime(timeRemaining)
    val (days, time) = if ("," in formattedTime) {
        formattedTime.split(", ")
    } else {
        listOf("0 dni", "00:00:00")
    }

    // Podział czasu na komponenty
    val hoursPart = if (time.length >= 2) time.substring(0, 2) else "00"
    val minutesPart = if (time.length >= 5) time.substring(3, 5) else "00"
    val secondsPart = if (time.length >= 8) time.substring(6, 8) else "00"

    // Losowy seed do animacji - zmienia się co 10 sekund dla nieprzewidywalności
    val animationSeed = remember { mutableStateOf(Random.nextInt(0, 1000)) }

    // Zmiana seed'a co 10 sekund dla różnorodności animacji
    LaunchedEffect(timeRemaining / 10000) {
        animationSeed.value = Random.nextInt(0, 1000)
    }

    // Animacja widoczności licznika
    AnimatedVisibility(
        visible = !isTimeUp, enter = fadeIn(), exit = fadeOut(
            animationSpec = tween(
                durationMillis = 800, easing = LinearEasing
            )
        ), modifier = modifier
            .fillMaxWidth()
            .testTag("countdown_container")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Tytuł odliczania
            Text(
                text = "Czas do urodzin:",
                style = MaterialTheme.typography.titleLarge,
                color = PurplePrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Licznik dni z animacją
            DaysCounter(
                days = days, animationSeed = animationSeed.value
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Czas (godziny:minuty:sekundy) z animacją
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Godziny
                TimeDigitCard(
                    digits = hoursPart, label = "Godzin", animationSeed = animationSeed.value
                )

                // Separator
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Minuty
                TimeDigitCard(
                    digits = minutesPart,
                    label = "Minut",
                    animationSeed = animationSeed.value + 123 // Różny seed dla różnej animacji
                )

                // Separator
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Sekundy
                TimeDigitCard(
                    digits = secondsPart,
                    label = "Sekund",
                    animationSeed = animationSeed.value + 456 // Jeszcze inny seed
                )
            }
        }
    }

    // Wiadomość po zakończeniu odliczania
    AnimatedVisibility(
        visible = isTimeUp, enter = fadeIn(
            animationSpec = tween(
                durationMillis = 1000, delayMillis = 500, easing = LinearEasing
            )
        ), modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .testTag("countdown")
        ) {
            // Pozytywna wiadomość po zakończeniu odliczania
            Text(
                text = "Wszystkiego Najlepszego!",
                style = MaterialTheme.typography.headlineMedium,
                color = PurplePrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "18 lat to wyjątkowy moment!",
                style = MaterialTheme.typography.titleMedium,
                color = PurpleLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Komponent z licznikiem dni z ulepszoną animacją. */
@Composable
fun DaysCounter(
    modifier: Modifier = Modifier,
    days: String,
    animationSeed: Int,
) {
    val dayText = days.split(" ")[0]
    val daysLabel = days.replace(dayText, "").trim()

    // Wybierz losowo typ animacji na podstawie seeda
    val animationType = remember(animationSeed) {
        AnimationType.values()[animationSeed % AnimationType.values().size]
    }

    // Różne parametry animacji
    val infiniteTransition = rememberInfiniteTransition(label = "daysAnimation")

    // Pulsuiąca animacja
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f, animationSpec = infiniteRepeatable(
            animation = tween(900 + (animationSeed % 600), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Falująca animacja
    val wave by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * Math.PI.toFloat(), animationSpec = infiniteRepeatable(
            animation = tween(2000 + (animationSeed % 1000)), repeatMode = RepeatMode.Restart
        ), label = "wave"
    )

    // Rotacyjna animacja
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f, targetValue = 2f, animationSpec = infiniteRepeatable(
            animation = tween(1500 + (animationSeed % 500)), repeatMode = RepeatMode.Reverse
        ), label = "rotation"
    )

    Card(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .then(
                when (animationType) {
                    AnimationType.PULSE -> Modifier.scale(pulse)
                    AnimationType.WAVE -> Modifier.scale(1f + sin(wave) * 0.03f)
                    AnimationType.ROTATE -> Modifier.rotate(rotation)
                    AnimationType.BOUNCE -> Modifier.scale(
                        1f + (animationSeed % 5) * 0.01f * sin(
                            wave
                        )
                    )
                }
            ), colors = CardDefaults.cardColors(
            containerColor = PurpleLight.copy(alpha = 0.7f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            // Liczba dni z wycentrowanym tekstem
            Text(
                text = dayText,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = PurplePrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Etykieta dni z wycentrowanym tekstem
            Text(
                text = daysLabel,
                style = MaterialTheme.typography.titleMedium,
                color = PurpleDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Karta z cyframi czasu z wycentrowanym tekstem i animacją. */
@Composable
fun TimeDigitCard(
    modifier: Modifier = Modifier,
    digits: String,
    label: String,
    animationSeed: Int,
) {
    // Wybierz losowo typ animacji na podstawie seeda
    val animationType = remember(animationSeed) {
        AnimationType.values()[(animationSeed / 2) % AnimationType.values().size]
    }

    // Animacje
    val infiniteTransition = rememberInfiniteTransition(label = "digitAnimation")

    // Pulsująca animacja
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f, animationSpec = infiniteRepeatable(
            animation = tween(700 + (animationSeed % 400)), repeatMode = RepeatMode.Reverse
        ), label = "digitPulse"
    )

    // Falująca animacja
    val wave by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * Math.PI.toFloat(), animationSpec = infiniteRepeatable(
            animation = tween(1500 + (animationSeed % 700)), repeatMode = RepeatMode.Restart
        ), label = "digitWave"
    )

    // Rotacyjna animacja
    val rotation by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(1000 + (animationSeed % 300)), repeatMode = RepeatMode.Reverse
        ), label = "digitRotation"
    )

    // Gradient dla karty
    val gradientColors = listOf(
        PurplePrimary, PurpleDark
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        // Karty z cyframi i animacją
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 56.dp, height = 72.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(gradientColors))
                .padding(4.dp)
                .then(
                    when (animationType) {
                        AnimationType.PULSE -> Modifier.scale(pulse)
                        AnimationType.WAVE -> Modifier.scale(1f + sin(wave) * 0.05f)
                        AnimationType.ROTATE -> Modifier.rotate(rotation)
                        AnimationType.BOUNCE -> Modifier.scale(
                            1f + (animationSeed % 3) * 0.02f * sin(
                                wave * 2
                            )
                        )
                    }
                )
        ) {
            // Wyświetlamy cyfry z wycentrowanym tekstem
            Text(
                text = digits,
                style = MaterialTheme.typography.headlineLarge,
                color = White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Etykieta
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PurpleDark,
            textAlign = TextAlign.Center
        )
    }
}

/** Typy animacji dla losowego wyboru */
private enum class AnimationType {
    PULSE, WAVE, ROTATE, BOUNCE
}