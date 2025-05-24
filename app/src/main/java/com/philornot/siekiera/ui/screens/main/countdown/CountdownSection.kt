package com.philornot.siekiera.ui.screens.main.countdown

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.mutableLongStateOf
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

/**
 * Sekcja odliczania, która obsługuje zarówno tryb odliczania do urodzin
 * jak i tryb timera z przyciskami kontroli.
 *
 * Usunięto checkbox zmiany nazwy aplikacji - ta opcja została przeniesiona
 * do sekcji Ustawienia.
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
    // Format the time string
    val formattedTime = if (isTimerMode) {
        // W trybie timera formatujemy czas na podstawie ustawionych minut lub pozostałego czasu
        if (timeRemaining > 0) {
            TimeUtils.formatRemainingTime(timeRemaining)
        } else {
            val hours = timerMinutes / 60
            val minutes = timerMinutes % 60
            "$timerMinutes dni, ${hours.toString().padStart(2, '0')}:${
                minutes.toString().padStart(2, '0')
            }:00"
        }
    } else {
        // W trybie odliczania urodzin używamy standardowego formatowania
        TimeUtils.formatRemainingTime(timeRemaining)
    }

    val (days, time) = if ("," in formattedTime) {
        formattedTime.split(", ")
    } else {
        listOf("0 dni", "00:00:00")
    }

    // Split time components
    val hoursPart = if (time.length >= 2) time.substring(0, 2) else "00"
    val minutesPart = if (time.length >= 5) time.substring(3, 5) else "00"
    val secondsPart = if (time.length >= 8) time.substring(6, 8) else "00"

    // Drag state dla trybu timera - tylko do zmiany wartości
    var isDragging by remember { mutableStateOf(false) }
    var currentDragMinutes by remember { mutableIntStateOf(timerMinutes) }

    // Dodane: Stan dla inteligentnego przeciągania
    var lastDragTime by remember { mutableLongStateOf(0L) }
    var lastDragPosition by remember { mutableFloatStateOf(0f) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    // Stan czy timer jest aktywny (ma pozostały czas)
    val isTimerActive = isTimerMode && timeRemaining > 0

    // Odświeżanie UI co sekundę dla trybu timera
    LaunchedEffect(isTimerMode) {
        if (isTimerMode) {
            // Zainicjuj wartość minut
            currentDragMinutes = timerMinutes
        }
    }

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
            modifier = Modifier
                .padding(16.dp)
                .pointerInput(isTimerMode) {
                    if (isTimerMode && !isTimerActive) {
                        // Przeciąganie działa tylko gdy timer nie jest aktywny
                        detectVerticalDragGestures(onDragStart = { offset ->
                            isDragging = true
                            lastDragTime = System.currentTimeMillis()
                            lastDragPosition = offset.y
                            dragVelocity = 0f
                            accumulatedDrag = 0f
                        }, onDragEnd = {
                            isDragging = false
                            // Zatwierdź zmianę minut po zakończeniu przeciągania
                            onTimerMinutesChanged(currentDragMinutes)

                            // Reset wartości
                            accumulatedDrag = 0f
                            dragVelocity = 0f
                        }, onDragCancel = {
                            isDragging = false
                            accumulatedDrag = 0f
                            dragVelocity = 0f
                        }, onVerticalDrag = { change, dragAmount ->
                            val currentTime = System.currentTimeMillis()
                            val timeDelta = (currentTime - lastDragTime).coerceAtLeast(1L)

                            // Oblicz prędkość przeciągania (piksele na milisekundę)
                            val instantVelocity = kotlin.math.abs(dragAmount) / timeDelta.toFloat()

                            // Wygładź prędkość używając prostego filtra
                            dragVelocity = dragVelocity * 0.7f + instantVelocity * 0.3f

                            // Akumuluj przeciągnięcie
                            accumulatedDrag += dragAmount

                            // Oblicz czułość na podstawie prędkości i akumulacji
                            val baseMultiplier = 0.15f // Podstawowa czułość (większa niż wcześniej)

                            // Mnożnik prędkości - im szybciej przeciągamy, tym większe zmiany
                            val velocityMultiplier = when {
                                dragVelocity > 2.0f -> 4.0f      // Bardzo szybkie przeciąganie
                                dragVelocity > 1.0f -> 2.5f      // Szybkie przeciąganie
                                dragVelocity > 0.5f -> 1.5f      // Średnie przeciąganie
                                else -> 1.0f                     // Wolne, precyzyjne przeciąganie
                            }

                            // Mnożnik akceleracji - im dłużej przeciągamy w jedną stronę, tym szybciej
                            val accumulationAmount = kotlin.math.abs(accumulatedDrag)
                            val accelerationMultiplier = when {
                                accumulationAmount > 200f -> 2.0f   // Długie przeciąganie
                                accumulationAmount > 100f -> 1.5f   // Średnie przeciąganie
                                else -> 1.0f                        // Krótkie przeciąganie
                            }

                            // Finalna czułość to kombinacja wszystkich czynników
                            val finalSensitivity =
                                baseMultiplier * velocityMultiplier * accelerationMultiplier

                            // Konwersja przeciągnięcia na minuty (ujemne przeciągnięcie = zwiększenie)
                            val minuteChange = (-dragAmount * finalSensitivity).toInt()

                            if (minuteChange != 0) {
                                // Aktualizuj minuty z ograniczeniami
                                val newMinutes = (currentDragMinutes + minuteChange).coerceIn(1, 60)

                                if (newMinutes != currentDragMinutes) {
                                    currentDragMinutes = newMinutes
                                    // Powiadom o zmianie dla natychmiastowej aktualizacji UI
                                    onTimerMinutesChanged(currentDragMinutes)
                                }
                            }

                            // Aktualizuj stan dla następnej iteracji
                            lastDragTime = currentTime
                            lastDragPosition = change.position.y

                            change.consume()
                        })
                    }
                }) {
            // Tytuł
            Text(
                text = when {
                    isTimerMode && isTimerActive && isTimerPaused -> "Timer spauzowany:"
                    isTimerMode && isTimerActive -> "Timer aktywny:"
                    isTimerMode -> "Timer: ustaw czas"
                    else -> "Czas do urodzin:"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Licznik dni
            if (isTimerMode) {
                // W trybie timera pokazujemy minuty zamiast dni
                TimerDaysCounter(
                    minutes = if (isTimerActive) {
                        // Jeśli timer jest aktywny, pokazuj pozostały czas w minutach
                        (timeRemaining / 60000).toInt()
                    } else {
                        // Jeśli timer nie jest aktywny, pokazuj ustawioną wartość
                        currentDragMinutes
                    }, isDragging = isDragging && !isTimerActive
                )
            } else {
                // W trybie odliczania do urodzin pokazujemy dni
                DaysCounter(days = days)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Czas (godziny:minuty:sekundy) z animowanymi cyframi
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
                    color = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Sekundy
                TimeDigitCard(
                    digits = secondsPart, label = "Sekund"
                )
            }

            // Dodatkowe opcje dla trybu timera - usunięto checkbox zmiany nazwy
            if (isTimerMode) {
                Spacer(modifier = Modifier.height(24.dp))

                // Przyciski kontroli timera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isTimerActive) {
                        // Przycisk START (tylko gdy timer nie jest aktywny)
                        Button(
                            onClick = { onTimerSet(currentDragMinutes) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.width(120.dp)
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
                        // Przyciski gdy timer jest aktywny
                        if (isTimerPaused) {
                            // Przycisk WZNÓW
                            Button(
                                onClick = onResumeTimer, colors = ButtonDefaults.buttonColors(
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
                                onClick = onPauseTimer, colors = ButtonDefaults.buttonColors(
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

                        // Przycisk STOP/RESET
                        Button(
                            onClick = onResetTimer, colors = ButtonDefaults.buttonColors(
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

                // Instrukcja przeciągania (tylko gdy timer nie jest aktywny)
                if (!isTimerActive) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Przeciągnij w górę lub w dół, aby zmienić czas",
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
            // Pozytywna wiadomość po zakończeniu odliczania
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

            // Dodaj przycisk resetowania timera również po zakończeniu
            if (isTimerMode) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onResetTimer, colors = ButtonDefaults.buttonColors(
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