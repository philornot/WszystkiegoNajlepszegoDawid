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
 * odpowiedzialności. Usunięto duplikującą sekcję zakończenia dla trybu
 * timera.
 *
 * @param modifier Modifier dla kontenera
 * @param timeRemaining Pozostały czas w milisekundach
 * @param isTimeUp Czy czas upłynął
 * @param isTimerMode Czy jest aktywny tryb timera
 * @param isTimerPaused Czy timer jest spauzowany
 * @param onTimerMinutesChanged Wywołanie gdy zmieniają się minuty timera
 * @param onTimerSet Wywołanie gdy timer powinien zostać uruchomiony
 * @param timerMinutes Aktualnie ustawione minuty w trybie timera
 * @param onPauseTimer Wywołanie gdy użytkownik pauzuje timer
 * @param onResumeTimer Wywołanie gdy użytkownik wznawia timer
 * @param onResetTimer Wywołanie gdy użytkownik resetuje timer
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

    // Formatowanie czasu
    val formattedTime = if (isTimerMode) {
        if (timeRemaining > 0) {
            TimeUtils.formatRemainingTime(timeRemaining)
        } else {
            val hours = timerMinutes / 60
            val minutes = timerMinutes % 60
            String.format(Locale.getDefault(), "%d dni, %02d:%02d:00", timerMinutes, hours, minutes)
        }
    } else {
        TimeUtils.formatRemainingTime(timeRemaining)
    }

    val (days, time) = if ("," in formattedTime) {
        formattedTime.split(", ")
    } else {
        listOf("0 dni", "00:00:00")
    }

    // Rozdziel komponenty czasu
    val hoursPart = if (time.length >= 2) time.substring(0, 2) else "00"
    val minutesPart = if (time.length >= 5) time.substring(3, 5) else "00"
    val secondsPart = if (time.length >= 8) time.substring(6, 8) else "00"

    // Stany dla przeciągania - uproszczona logika
    var isDragging by remember { mutableStateOf(false) }
    var currentDragMinutes by remember { mutableIntStateOf(timerMinutes) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    // Stan aktywności timera
    val isTimerActive = isTimerMode && timeRemaining > 0

    // Synchronizacja lokalnego stanu z przekazanym
    LaunchedEffect(timerMinutes) {
        currentDragMinutes = timerMinutes
        Timber.d("Synchronizacja timerMinutes: $timerMinutes -> currentDragMinutes: $currentDragMinutes")
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
            // Tytuł z animowaną zmianą
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

            // Wyświetlanie czasu
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