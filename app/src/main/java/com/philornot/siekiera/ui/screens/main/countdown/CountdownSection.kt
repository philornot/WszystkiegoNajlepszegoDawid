package com.philornot.siekiera.ui.screens.main.countdown

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.philornot.siekiera.ui.screens.main.timer.TimerDaysCounter
import com.philornot.siekiera.utils.TimeUtils
import timber.log.Timber
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Sekcja odliczania z ulepszoną logiką timera. Obsługuje zarówno tryb
 * odliczania do urodzin jak i tryb timera z responsywnymi animacjami.
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
@OptIn(ExperimentalAnimationApi::class)
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

    // UI odliczania
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

            // NOWA LOGIKA: Pokazuj duży kafelek gdy timer nieaktywny, małe kafelki gdy aktywny
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                }
            } else {
                // Licznik dni dla trybu urodzinowego
                DaysCounter(days = days)

                Spacer(modifier = Modifier.height(16.dp))

                // Czas z animowanymi cyframi - dla trybu urodzinowego
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Godziny
                    TimeDigitCard(digits = hoursPart, label = "Godzin")

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Minuty
                    TimeDigitCard(digits = minutesPart, label = "Minut")

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Sekundy
                    TimeDigitCard(digits = secondsPart, label = "Sekund")
                }
            }

            // Kontrolki timera z animacjami przycisków
            if (isTimerMode) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isTimerActive) {
                        // Przycisk START
                        Button(
                            onClick = {
                                Timber.i("Użytkownik kliknął START, ustawianie timera na $currentDragMinutes minut")
                                onTimerSet(currentDragMinutes)
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ), modifier = Modifier.width(120.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start")
                            }
                        }
                    } else {
                        // Przyciski kontroli aktywnego timera
                        if (isTimerPaused) {
                            // Przycisk WZNÓW
                            Button(
                                onClick = {
                                    Timber.i("Użytkownik kliknął WZNÓW timer")
                                    onResumeTimer()
                                }, colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ), modifier = Modifier.width(120.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Wznów",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Wznów")
                                }
                            }
                        } else {
                            // Przycisk PAUZA
                            Button(
                                onClick = {
                                    Timber.i("Użytkownik kliknął PAUZA timer")
                                    onPauseTimer()
                                }, colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ), modifier = Modifier.width(120.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pauza",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pauza")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Przycisk STOP
                        Button(
                            onClick = {
                                Timber.i("Użytkownik kliknął STOP timer")
                                onResetTimer()
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ), modifier = Modifier.width(120.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop")
                            }
                        }
                    }
                }

                // Instrukcja przeciągania - tylko gdy timer nieaktywny
                if (!isTimerActive) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Przeciągnij w górę lub w dół, aby zmienić czas\n(maksymalnie 24 godziny)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Wiadomość po zakończeniu odliczania
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
            Text(
                text = if (isTimerMode) "CZAS MINĄŁ!" else "WOOO HOOO",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = if (isTimerMode) "Twój timer zakończył odliczanie"
                else "Udało ci się przeżyć 72 pory roku\n(i to POD RZĄD!!)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            // Przycisk resetowania timera po zakończeniu
            if (isTimerMode) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        Timber.i("Użytkownik kliknął RESET po zakończeniu timera")
                        onResetTimer()
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset timera",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ustaw nowy timer")
                    }
                }
            }
        }
    }
}