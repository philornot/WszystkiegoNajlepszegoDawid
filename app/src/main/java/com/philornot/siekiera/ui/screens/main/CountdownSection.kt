package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.theme.PurpleDark
import com.philornot.siekiera.ui.theme.PurpleLight
import com.philornot.siekiera.ui.theme.PurplePrimary
import com.philornot.siekiera.ui.theme.White

/**
 * Sekcja odliczania do urodzin, uproszczona, z fioletowymi elementami.
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

    // Animacja widoczności licznika
    AnimatedVisibility(
        visible = !isTimeUp, enter = fadeIn(), exit = fadeOut(
            animationSpec = tween(
                durationMillis = 800, easing = LinearEasing
            )
        ), modifier = modifier.fillMaxWidth()
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
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Licznik dni - w fioletowym odcieniu
            DaysCounter(days = days)

            Spacer(modifier = Modifier.height(8.dp))

            // Czas (godziny:minuty:sekundy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Godziny
                TimeDigitCard(
                    digits = hoursPart, label = "Godzin"
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
                    digits = minutesPart, label = "Minut"
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
                    digits = secondsPart, label = "Sekund"
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
            modifier = Modifier.padding(16.dp)
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

/** Komponent z licznikiem dni w fioletowym kolorze. */
@Composable
fun DaysCounter(
    modifier: Modifier = Modifier,
    days: String,
) {
    val dayText = days.split(" ")[0]
    val daysLabel = days.replace(dayText, "").trim()

    Card(
        modifier = modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = PurpleLight.copy(alpha = 0.7f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            // Liczba dni
            Text(
                text = dayText,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = PurplePrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Etykieta dni
            Text(
                text = daysLabel, style = MaterialTheme.typography.titleMedium, color = PurpleDark
            )
        }
    }
}

/** Karta z cyframi czasu (godziny/minuty/sekundy) w fioletowym kolorze. */
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
        // Karty z cyframi
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 56.dp, height = 72.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(PurplePrimary)
                .padding(4.dp)
        ) {
            // Wyświetlamy cyfry
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