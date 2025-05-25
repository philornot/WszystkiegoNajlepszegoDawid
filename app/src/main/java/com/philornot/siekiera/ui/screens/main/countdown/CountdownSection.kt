package com.philornot.siekiera.ui.screens.main.countdown

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.utils.TimeUtils
import timber.log.Timber
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Główny komponent sekcji odliczania. Orkiestruje wyświetlanie czasu,
 * kontrolek timera i obsługę przeciągania. Zrefaktorowana wersja
 * z wydzielonymi komponentami dla lepszej czytelności i podziału
 * odpowiedzialności. Formatowanie czasu dla trybu
 * timera z lepszą synchronizacją animacji.
 */
@Composable
fun CountdownSection(
    modifier: Modifier = Modifier,
    timeRemaining: Long,
    isTimeUp: Boolean,
    isTimerMode: Boolean = false,
    isTimerPaused: Boolean = false,
    onTimerMinutesChanged: (Int) -> Unit = {},
    onTimerSet: (Int) -> Unit = {},
    timerMinutes: Int = 5,
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
) {
    Timber.v("CountdownSection: timeRemaining=$timeRemaining, isTimerMode=$isTimerMode, isTimerPaused=$isTimerPaused, timerMinutes=$timerMinutes")

    // Ulepszone formatowanie czasu z lepszą obsługą trybu timera
    val formattedTime = if (isTimerMode && timeRemaining > 0) {
        // W trybie timera, gdy timer jest aktywny, używaj rzeczywistego timeRemaining
        TimeUtils.formatRemainingTime(timeRemaining)
    } else if (isTimerMode && timeRemaining <= 0) {
        // W trybie timera, gdy timer nie jest aktywny, pokaż ustawienie
        val hours = timerMinutes / 60
        val minutes = timerMinutes % 60
        String.format(Locale.getDefault(), "0 dni, %02d:%02d:00", hours, minutes)
    } else {
        // Tryb urodzinowy - standardowe formatowanie
        TimeUtils.formatRemainingTime(timeRemaining)
    }

    // Lepsze rozdzielanie komponentów czasu
    val (days, time) = if ("," in formattedTime) {
        val parts = formattedTime.split(", ", limit = 2)
        Pair(parts.getOrElse(0) { "0 dni" }, parts.getOrElse(1) { "00:00:00" })
    } else {
        Pair("0 dni", formattedTime.ifEmpty { "00:00:00" })
    }

    // Bezpieczniejsze rozdzielanie komponenentów czasu z walidacją
    val timeParts = time.split(":")
    val hoursPart = timeParts.getOrElse(0) { "00" }.padStart(2, '0')
    val minutesPart = timeParts.getOrElse(1) { "00" }.padStart(2, '0')
    val secondsPart = timeParts.getOrElse(2) { "00" }.padStart(2, '0')

    // Stany dla przeciągania - uproszczona logika
    var isDragging by remember { mutableStateOf(false) }
    var currentDragMinutes by remember { mutableIntStateOf(timerMinutes) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    // Lepsze określenie stanu aktywności timera
    val isTimerActive = isTimerMode && timeRemaining > 0

    // Synchronizacja lokalnego stanu z przekazanym
    LaunchedEffect(timerMinutes) {
        if (!isTimerActive) { // Tylko gdy timer nie jest aktywny
            currentDragMinutes = timerMinutes
            Timber.d("Synchronizacja timerMinutes: $timerMinutes -> currentDragMinutes: $currentDragMinutes")
        }
    }

    // Dodatkowa synchronizacja dla aktywnego timera
    LaunchedEffect(isTimerActive, timeRemaining) {
        if (isTimerActive) {
            // Gdy timer jest aktywny, oblicz minuty na podstawie pozostałego czasu
            val remainingMinutes = (timeRemaining / (60 * 1000)).toInt()
            if (remainingMinutes != currentDragMinutes) {
                Timber.v("Aktualizacja currentDragMinutes na podstawie aktywnego timera: $remainingMinutes")
            }
        }
    }

    // UI odliczania - dla trybu timera nie pokazuj gdy zakończony
    AnimatedVisibility(
        visible = if (isTimerMode) !isTimeUp else !isTimeUp,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .testTag("countdown_container")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(16.dp)
                .pointerInput(isTimerMode, isTimerActive) {
                    if (isTimerMode && !isTimerActive) {
                        Timber.d("Włączam detekcję przeciągania dla timera")
                        // Uproszczona logika przeciągania - bardziej intuicyjna
                        detectVerticalDragGestures(onDragStart = {
                            isDragging = true
                            accumulatedDrag = 0f
                            Timber.d("Rozpoczęto przeciąganie timera")
                        }, onDragEnd = {
                            isDragging = false
                            onTimerMinutesChanged(currentDragMinutes)
                            accumulatedDrag = 0f
                            Timber.d("Zakończono przeciąganie timera na wartości: $currentDragMinutes minut")
                        }, onDragCancel = {
                            isDragging = false
                            accumulatedDrag = 0f
                            Timber.d("Anulowano przeciąganie timera")
                        }) { _, dragAmount ->
                            // Bardzo uproszczona logika - jeden piksel = jedna zmiana
                            accumulatedDrag += dragAmount

                            // Każde 15 pikseli = 1 minuta zmiany
                            val sensitivity = 15f
                            val minuteChange = -(accumulatedDrag / sensitivity).roundToInt()

                            if (minuteChange != 0) {
                                // Zwiększony limit timera - do 24 godzin (1440 minut)
                                val newMinutes =
                                    (currentDragMinutes + minuteChange).coerceIn(1, 1440)

                                if (newMinutes != currentDragMinutes) {
                                    currentDragMinutes = newMinutes
                                    onTimerMinutesChanged(currentDragMinutes)
                                    accumulatedDrag = 0f // Reset akumulacji po zmianie
                                    Timber.v("Zmiana wartości timera przez przeciąganie: $newMinutes minut")
                                }
                            }
                        }
                    }
                }) {
            // Lepszy tytuł z animowaną zmianą
            Text(
                text = when {
                    isTimerMode && isTimerActive && isTimerPaused -> "Timer spauzowany:"
                    isTimerMode && isTimerActive -> "Timer aktywny:"
                    isTimerMode -> "Timer: ustaw czas"
                    else -> "Czas do urodzin:"
                },
                style = MaterialTheme.typography.titleLarge,
                color = when {
                    isTimerMode && isTimerActive && !isTimerPaused -> MaterialTheme.colorScheme.primary
                    isTimerMode && isTimerPaused -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                },
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Wyświetlanie czasu z poprawionym formatowaniem
            TimeDisplaySection(
                isTimerMode = isTimerMode,
                isTimerActive = isTimerActive,
                currentDragMinutes = currentDragMinutes,
                isDragging = isDragging,
                isTimerPaused = isTimerPaused,
                days = days,
                hoursPart = hoursPart,
                minutesPart = minutesPart,
                secondsPart = secondsPart
            )

            // Kontrolki timera
            if (isTimerMode) {
                TimerControlsSection(
                    isTimerActive = isTimerActive,
                    isTimerPaused = isTimerPaused,
                    isTimeUp = isTimeUp,
                    currentDragMinutes = currentDragMinutes,
                    onTimerSet = onTimerSet,
                    onPauseTimer = onPauseTimer,
                    onResumeTimer = onResumeTimer,
                    onResetTimer = onResetTimer
                )
            }
        }
    }

    // Wiadomość po zakończeniu odliczania - tylko dla trybu urodzinowego
    AnimatedVisibility(
        visible = isTimeUp && !isTimerMode, enter = fadeIn(), modifier = modifier.fillMaxWidth()
    ) {
        CountdownFinishedSection(
            isTimerMode = isTimerMode
        )
    }
}