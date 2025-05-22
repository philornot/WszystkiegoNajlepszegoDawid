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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.philornot.siekiera.ui.screens.main.timer.TimerDaysCounter
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.delay

/**
 * Sekcja odliczania, która obsługuje zarówno tryb odliczania do urodzin
 * jak i tryb timera.
 *
 * @param modifier Modifier dla kontenera
 * @param timeRemaining Pozostały czas w milisekundach
 * @param isTimeUp Czy czas upłynął
 * @param isTimerMode Czy jest aktywny tryb timera
 * @param onTimerMinutesChanged Wywołanie gdy zmieniają się minuty timera
 * @param onTimerSet Wywołanie gdy timer powinien zostać uruchomiony
 * @param timerMinutes Aktualnie ustawione minuty w trybie timera
 * @param changeAppName Czy zmienić nazwę aplikacji w trybie timera
 * @param onChangeAppNameChanged Wywołanie gdy zmienia się opcja zmiany
 *    nazwy
 * @param onResetTimer Wywołanie gdy użytkownik resetuje timer
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CountdownSection(
    modifier: Modifier = Modifier,
    timeRemaining: Long,
    isTimeUp: Boolean,
    isTimerMode: Boolean = false,
    onTimerMinutesChanged: (Int) -> Unit = {},
    onTimerSet: (Int) -> Unit = {},  // Added parameter for starting the timer
    timerMinutes: Int = 5,
    changeAppName: Boolean = false,
    onChangeAppNameChanged: (Boolean) -> Unit = {},
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

    // Drag state dla trybu timera
    var isDragging by remember { mutableStateOf(false) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    var currentDragMinutes by remember { mutableIntStateOf(timerMinutes) }

    // Track if timer needs to be auto-started after drag
    var shouldStartTimer by remember { mutableStateOf(false) }

    // Odświeżanie UI co sekundę dla trybu timera
    LaunchedEffect(isTimerMode) {
        if (isTimerMode) {
            // Zainicjuj wartość minut
            currentDragMinutes = timerMinutes
        }
    }

    // Auto-start timer after drag is completed (with delay)
    LaunchedEffect(shouldStartTimer) {
        if (shouldStartTimer && isTimerMode && timeRemaining <= 0) {
            // Add a 1.5 second delay before starting the timer
            delay(1500)
            // Check if user hasn't cancelled the auto-start in the meantime
            if (shouldStartTimer) {
                onTimerSet(currentDragMinutes)
                shouldStartTimer = false
            }
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
                    if (isTimerMode) {
                        detectVerticalDragGestures(onDragStart = { offset ->
                            isDragging = true
                            dragStartY = offset.y
                        }, onDragEnd = {
                            isDragging = false
                            // Zatwierdź zmianę minut po zakończeniu przeciągania
                            onTimerMinutesChanged(currentDragMinutes)

                            // Auto-start timer if not already running
                            if (timeRemaining <= 0) {
                                shouldStartTimer = true
                            }
                        }, onDragCancel = {
                            isDragging = false
                        }, onVerticalDrag = { change, dragAmount ->
                            // Konwersja przeciągnięcia na minuty (ujemne przeciągnięcie = zwiększenie)
                            if (isTimerMode) {
                                val dragSensitivity = 0.05f
                                val minuteChange = (-dragAmount * dragSensitivity).toInt()

                                if (minuteChange != 0) {
                                    // Aktualizuj minuty z ograniczeniami
                                    currentDragMinutes =
                                        (currentDragMinutes + minuteChange).coerceIn(
                                            1, 60
                                        ) // 1 min do 60 min

                                    // Powiadom o zmianie
                                    onTimerMinutesChanged(currentDragMinutes)
                                }
                            }
                            change.consume()
                        })
                    }
                }) {
            // Tytuł
            Text(
                text = if (isTimerMode) if (timeRemaining > 0) "Timer aktywny:" else "Timer: ustaw czas"
                else "Czas do urodzin:",
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
                    minutes = if (timeRemaining > 0) (timeRemaining / 60000).toInt() else currentDragMinutes,
                    isDragging = isDragging
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

            // Dodatkowe opcje dla trybu timera
            if (isTimerMode) {
                Spacer(modifier = Modifier.height(24.dp))

                // Opcja zmiany nazwy aplikacji
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    // Pobierz kontekst w kontekście funkcji @Composable
                    val context = LocalContext.current

                    Checkbox(
                        checked = changeAppName, onCheckedChange = {
                            onChangeAppNameChanged(it)

                            // Używamy pobranego wcześniej kontekstu
                            context.getSharedPreferences(
                                "timer_prefs", android.content.Context.MODE_PRIVATE
                            ).edit { putBoolean("change_app_name", it) }
                        })

                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "Zmień nazwę aplikacji na 'Lawendowy Timer'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Uwaga: może wymagać zamknięcia aplikacji",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }

                // Start timer button (show only if timer not running)
                if (timeRemaining <= 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onTimerSet(currentDragMinutes) },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Rozpocznij odliczanie")
                    }
                }

                // Przycisk resetowania timera (show only if timer is running)
                if (timeRemaining > 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onResetTimer,
                        modifier = Modifier.fillMaxWidth(0.7f),
                        colors = ButtonDefaults.buttonColors(
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
                            Text("Anuluj timer")
                        }
                    }
                }

                // Instrukcja przeciągania (show only if timer not running)
                if (timeRemaining <= 0) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                    onClick = onResetTimer,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
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