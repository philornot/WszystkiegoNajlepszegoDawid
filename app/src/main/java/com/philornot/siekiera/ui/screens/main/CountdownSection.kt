package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Sekcja odliczania, która obsługuje zarówno tryb odliczania do urodzin
 * jak i tryb timera.
 *
 * @param modifier Modifier dla kontenera
 * @param timeRemaining Pozostały czas w milisekundach
 * @param isTimeUp Czy czas upłynął
 * @param isTimerMode Czy jest aktywny tryb timera
 * @param onTimerMinutesChanged Wywołanie gdy zmieniają się minuty timera
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
    timerMinutes: Int = 5,
    changeAppName: Boolean = false,
    onChangeAppNameChanged: (Boolean) -> Unit = {},
    onResetTimer: () -> Unit = {},
) {
    // Format the time string
    val formattedTime = if (isTimerMode) {
        // W trybie timera formatujemy czas na podstawie ustawionych minut
        val hours = timerMinutes / 60
        val minutes = timerMinutes % 60
        "$timerMinutes dni, ${hours.toString().padStart(2, '0')}:${
            minutes.toString().padStart(2, '0')
        }:00"
    } else {
        // W trybie odliczania urodzin używamy standardowego formatowania
        com.philornot.siekiera.utils.TimeUtils.formatRemainingTime(timeRemaining)
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
                    if (isTimerMode) {
                        detectVerticalDragGestures(onDragStart = { offset ->
                            isDragging = true
                            dragStartY = offset.y
                        }, onDragEnd = {
                            isDragging = false
                            // Zatwierdź zmianę minut po zakończeniu przeciągania
                            onTimerMinutesChanged(currentDragMinutes)
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
                text = if (isTimerMode) "Timer: ustaw czas" else "Czas do urodzin:",
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
                    minutes = currentDragMinutes, isDragging = isDragging
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
                    Checkbox(
                        checked = changeAppName, onCheckedChange = { onChangeAppNameChanged(it) })

                    Text(
                        text = "Zmień nazwę aplikacji na 'Lawendowy Timer'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Przycisk resetowania timera
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
                        Text("Resetuj timer")
                    }
                }

                // Instrukcja przeciągania
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

/** Timer licznik dni/minut. Pokazuje minuty w trybie timera. */
@Composable
fun TimerDaysCounter(
    modifier: Modifier = Modifier,
    minutes: Int,
    isDragging: Boolean = false,
) {
    // Animacja pulsowania podczas przeciągania
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1.0f, label = "drag_scale"
    )

    Card(
        modifier = modifier.size(width = 160.dp, height = 160.dp * scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            // Liczba minut z wycentrowanym tekstem
            Text(
                text = minutes.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Etykieta minut z wycentrowanym tekstem
            Text(
                text = "minut",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Licznik dni z czystym designem */
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
            // Liczba dni z wycentrowanym tekstem
            Text(
                text = dayText,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Etykieta dni z wycentrowanym tekstem
            Text(
                text = daysLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Karta z cyframi czasu z animowanymi przejściami cyfr */
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
        // Karta z animowanymi cyframi
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
                    // Animuj każdą cyfrę osobno
                    for (i in digits.indices) {
                        AnimatedContent(
                            targetState = digits[i], transitionSpec = {
                                // Wybierz losowy typ animacji dla każdej zmiany cyfry
                                val animationType = Random.nextInt(6)
                                when (animationType) {
                                    0 -> {
                                        // Przesunięcie pionowe
                                        val direction = if (Random.nextBoolean()) 1 else -1
                                        slideInVertically { height -> direction * height } + fadeIn() togetherWith slideOutVertically { height -> -direction * height } + fadeOut()
                                    }

                                    1 -> {
                                        // Przesunięcie poziome
                                        val direction = if (Random.nextBoolean()) 1 else -1
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
                                            if (Random.nextBoolean()) it else -it
                                        } + fadeIn() togetherWith slideOutVertically {
                                            if (Random.nextBoolean()) -it else it
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
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Etykieta
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}