package com.philornot.siekiera.ui.screens.main.birthday

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.screens.main.timer.TimerDaysCounter

/**
 * Komponent odpowiedzialny za wyświetlanie czasu w różnych trybach.
 * Obsługuje zarówno tryb urodzinowy jak i tryb timera.
 *
 * @param isTimerMode Czy aplikacja jest w trybie timera
 * @param isTimerActive Czy timer jest aktywny
 * @param currentDragMinutes Aktualna wartość minut z przeciągania
 * @param isDragging Czy użytkownik przeciąga wartość
 * @param isTimerPaused Czy timer jest spauzowany
 * @param days Sformatowane dni dla trybu urodzinowego
 * @param hoursPart Część godzin do wyświetlenia
 * @param minutesPart Część minut do wyświetlenia
 * @param secondsPart Część sekund do wyświetlenia
 * @param modifier Modifier dla komponentu
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimeDisplaySection(
    isTimerMode: Boolean,
    isTimerActive: Boolean,
    currentDragMinutes: Int,
    isDragging: Boolean,
    isTimerPaused: Boolean,
    days: String,
    hoursPart: String,
    minutesPart: String,
    secondsPart: String,
    modifier: Modifier = Modifier,
) {
    if (isTimerMode) {
        if (!isTimerActive) {
            // Duży kafelek do ustawiania timera
            AnimatedVisibility(
                visible = true, enter = scaleIn(animationSpec = tween(400)) + fadeIn(
                    animationSpec = tween(300)
                ), exit = scaleOut(animationSpec = tween(300)) + fadeOut(
                    animationSpec = tween(200)
                )
            ) {
                TimerDaysCounter(
                    minutes = currentDragMinutes,
                    isDragging = isDragging,
                    isActive = false,
                    isPaused = false
                )
            }
        } else {
            // Małe kafelki z czasem gdy timer aktywny
            AnimatedVisibility(
                visible = true, enter = slideInVertically(
                    initialOffsetY = { it }, animationSpec = tween(500)
                ) + fadeIn(animationSpec = tween(400)), exit = slideOutVertically(
                    targetOffsetY = { it }, animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                TimeDigitsRow(
                    hoursPart = hoursPart,
                    minutesPart = minutesPart,
                    secondsPart = secondsPart,
                    isTimerActive = isTimerActive,
                    isTimerPaused = isTimerPaused
                )
            }
        }
    } else {
        // Tryb urodzinowy
        // Licznik dni dla trybu urodzinowego
        DaysCounter(days = days)

        Spacer(modifier = Modifier.height(16.dp))

        // Czas z animowanymi cyframi - dla trybu urodzinowego
        TimeDigitsRow(
            hoursPart = hoursPart,
            minutesPart = minutesPart,
            secondsPart = secondsPart,
            isTimerActive = false,
            isTimerPaused = false
        )
    }
}

/**
 * Komponent wyświetlający cyfry czasu w rzędzie. Wydzielony dla
 * czytelności i ponownego użycia.
 */
@Composable
private fun TimeDigitsRow(
    hoursPart: String,
    minutesPart: String,
    secondsPart: String,
    isTimerActive: Boolean,
    isTimerPaused: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Godziny
        TimeDigitCard(digits = hoursPart, label = "Godzin")

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (isTimerActive && !isTimerPaused) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Minuty
        TimeDigitCard(digits = minutesPart, label = "Minut")

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = if (isTimerActive && !isTimerPaused) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Sekundy
        TimeDigitCard(digits = secondsPart, label = "Sekund")
    }
}