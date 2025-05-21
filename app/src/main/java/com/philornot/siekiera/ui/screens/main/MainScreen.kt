package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Główny ekran z czystym, lawendowym motywem i minimalnymi animacjami.
 * Teraz obsługuje zarówno tryb odliczania urodzin jak i tryb timera.
 *
 * @param modifier Modifier dla kontenera
 * @param targetDate Data urodzin w milisekundach
 * @param currentTime Aktualny czas (domyślnie czas systemowy)
 * @param onGiftClicked Callback dla kliknięcia prezentu
 * @param activity Referencja do MainActivity dla sprawdzania plików
 * @param giftReceived Czy prezent został już odebrany
 * @param onTimerSet Callback kiedy timer jest ustawiony
 * @param timerModeEnabled Czy tryb timera został odkryty przez użytkownika
 * @param onTimerModeDiscovered Callback informujący że tryb timera został
 *    odkryty
 * @param activeTimer Aktualnie aktywny timer w milisekundach (0 jeśli brak
 *    aktywnego timera)
 * @param onCancelTimer Callback do anulowania timera
 * @param onResetTimer Callback do resetowania timera
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    targetDate: Long,
    currentTime: Long = System.currentTimeMillis(),
    onGiftClicked: () -> Unit,
    activity: MainActivity? = null,
    giftReceived: Boolean = false,
    onTimerSet: (Int) -> Unit = {},
    timerModeEnabled: Boolean = false,
    onTimerModeDiscovered: () -> Unit = {},
    activeTimer: Long = 0,
    onCancelTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
) {
    // Oblicz czy czas upłynął
    var isTimeUp by remember { mutableStateOf(currentTime >= targetDate) }

    // Śledź aktualny czas (aktualizowany co sekundę)
    var currentTimeState by remember { mutableLongStateOf(currentTime) }

    // Śledź pozostały czas do urodzin
    var timeRemaining by remember {
        mutableLongStateOf(
            (targetDate - currentTimeState).coerceAtLeast(0)
        )
    }

    // Śledź czas od ostatniego sprawdzenia pliku
    var lastCheckTime by remember { mutableLongStateOf(0L) }

    // Śledź czy pokazać ekran celebracji po kliknięciu prezentu
    var showCelebration by remember { mutableStateOf(false) }

    // Śledź czy pokazać wybuch konfetti gdy prezent jest kliknięty lub gdy czas upłynie
    var showConfettiExplosion by remember { mutableStateOf(false) }

    // Pamiętaj pozycję kliknięcia dla wybuchu konfetti
    var confettiCenterX by remember { mutableFloatStateOf(0.5f) }
    var confettiCenterY by remember { mutableFloatStateOf(0.5f) }

    // Stany dla trybu timera
    var isTimerMode by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableIntStateOf(5) }
    var changeAppName by remember { mutableStateOf(false) }

    // Stan dla dialogu zmiany nazwy aplikacji
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }

    // Czy pokazujemy fajerwerki po zakończeniu timera
    var showTimerFinishedCelebration by remember { mutableStateOf(false) }

    // Pozostały czas timera, jeśli aktywny
    var timerRemainingTime by remember { mutableLongStateOf(activeTimer) }
    var timerFinished by remember { mutableStateOf(false) }

    // Inicjalizacja - sprawdź czy timer jest aktywny
    LaunchedEffect(activeTimer) {
        if (activeTimer > 0) {
            timerRemainingTime = activeTimer
            isTimerMode = true
            timerFinished = false
        }
    }

    // Reagowanie na zmianę stanu isTimeUp - automatyczne fajerwerki po zakończeniu odliczania
    // (tylko w trybie urodzinowym)
    LaunchedEffect(isTimeUp) {
        if (isTimeUp && !isTimerMode) {
            Timber.d("Czas upłynął! Uruchamiam automatyczne fajerwerki!")
            // Automatycznie uruchamiamy fajerwerki gdy czas się kończy
            showConfettiExplosion = true

            // Po 5 sekundach automatycznie ukrywamy efekt konfetti
            // aby nie kolidował z innymi efektami
            kotlinx.coroutines.MainScope().launch {
                delay(5000)
                showConfettiExplosion = false
            }
        }
    }

    // Aktualizuj czas co sekundę i sprawdzaj aktualizacje pliku gdy czas zbliża się do końca
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeState = System.currentTimeMillis()

            if (isTimerMode && timerRemainingTime > 0) {
                // W trybie timera aktualizuj pozostały czas
                timerRemainingTime -= 1000

                if (timerRemainingTime <= 0) {
                    // Timer zakończony
                    timerRemainingTime = 0
                    timerFinished = true
                    showTimerFinishedCelebration = true

                    // Nie pokazujemy fajerwerków w trybie timera
                }
            } else if (!isTimerMode) {
                // W trybie odliczania urodzin aktualizuj pozostały czas
                timeRemaining = (targetDate - currentTimeState).coerceAtLeast(0)

                // Sprawdzaj częściej gdy pozostało mało czasu
                if (activity != null && timeRemaining > 0) {
                    val remainingMinutes = timeRemaining / 60000
                    val currentTime = System.currentTimeMillis()

                    // Określ minimalny interwał między sprawdzeniami
                    val checkInterval = when {
                        remainingMinutes <= 1 -> 30_000      // 30 sekund w ostatniej minucie
                        remainingMinutes <= 5 -> 60_000      // 1 minuta
                        remainingMinutes <= 15 -> 120_000    // 2 minuty
                        remainingMinutes <= 60 -> 300_000    // 5 minut
                        else -> 900_000                      // 15 minut
                    }

                    // Sprawdź tylko jeśli minął wymagany czas od ostatniego sprawdzenia
                    if (currentTime - lastCheckTime >= checkInterval) {
                        Timber.d("Uruchamiam dodatkowe sprawdzenie pliku, pozostało $remainingMinutes minut (interwał: ${checkInterval / 1000}s)")
                        activity.checkFileNow()
                        lastCheckTime = currentTime
                    }
                }

                // Sprawdź czy czas się skończył
                if (currentTimeState >= targetDate && !isTimeUp) {
                    isTimeUp = true

                    // Opcjonalnie: ostatnie sprawdzenie tuż po upływie czasu
                    if (activity != null) {
                        Timber.d("Uruchamiam ostatnie sprawdzenie pliku po upływie czasu")
                        activity.checkFileNow()
                        lastCheckTime = currentTimeState
                    }
                }
            }
        }
    }

    // Obsługa dialogu postępu zmiany nazwy aplikacji
    LaunchedEffect(showProgressDialog) {
        if (showProgressDialog) {
            progressValue = 0f

            // Animacja przez 2 sekundy
            val updateInterval = 50L // 50ms
            val steps = 2000L / updateInterval
            val increment = 1f / steps

            while (progressValue < 1f) {
                delay(updateInterval)
                progressValue += increment
            }

            // Po zakończeniu animacji ustaw timer i zamknij dialog
            onTimerSet(timerMinutes)
            showProgressDialog = false
        }
    }

    // Główny kontener z efektem drgania gdy czas prawie upłynął
    Box(
        modifier = modifier
            .fillMaxSize()
            .shakeEffect(timeRemaining = if (isTimerMode) timerRemainingTime else timeRemaining)
            .flashEffect(timeRemaining = if (isTimerMode) timerRemainingTime else timeRemaining)
    ) {
        // Tło aplikacji
        AppBackground(isTimeUp = isTimeUp || (isTimerMode && timerFinished))

        // Główna zawartość
        Box(modifier = Modifier.fillMaxSize()) {
            // Zwykły ekran z czekaniem/prezentem
            AnimatedVisibility(
                visible = !showCelebration && !showTimerFinishedCelebration,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Górny rząd z przełącznikiem trybów
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Przełącznik trybów - widoczny tylko po odkryciu trybu timera
                        AnimatedVisibility(
                            visible = timerModeEnabled || isTimerMode,
                            enter = fadeIn(tween(500)),
                            exit = fadeOut(tween(300))
                        ) {
                            ModeToggle(
                                isTimerMode = isTimerMode, onModeChanged = { newTimerMode ->
                                    isTimerMode = newTimerMode

                                    // Jeśli wychodzimy z trybu timera i był aktywny timer, anuluj go
                                    if (!newTimerMode && timerRemainingTime > 0) {
                                        onCancelTimer()
                                        timerRemainingTime = 0
                                    }

                                    if (newTimerMode) {
                                        // Przy wejściu w tryb timera inicjalizuj wartości
                                        timerMinutes = 5
                                        changeAppName = false
                                        timerFinished = false
                                    }
                                })
                        }
                    }

                    // Nagłówek z tytułem
                    HeaderSection()

                    // Sekcja kurtyny lub prezentu
                    CurtainSection(
                        isTimeUp = if (isTimerMode) true else isTimeUp, // Zawsze ukryj kurtynę w trybie timera
                        showGift = !isTimerMode, // Nowy parametr - pokaż prezent tylko w trybie urodzinowym
                        onGiftClicked = { centerX, centerY ->
                            // Jeśli jesteśmy w trybie timera, to kliknięcie prezentu ustawia timer
                            // (teraz niewidoczne w trybie timera, ale kod pozostanie dla kompatybilności)
                            if (isTimerMode && !timerFinished) {
                                if (changeAppName) {
                                    // Pokaż dialog postępu przed ustawieniem timera
                                    showProgressDialog = true
                                } else {
                                    // Od razu ustaw timer bez dialogu
                                    onTimerSet(timerMinutes)
                                }
                            } else {
                                // W trybie urodzinowym, zapisz pozycję kliknięcia i pokaż konfetti
                                confettiCenterX = centerX
                                confettiCenterY = centerY

                                // Pokaż wybuch konfetti
                                showConfettiExplosion = true

                                // Opóźnij pokazanie ekranu celebracji
                                kotlinx.coroutines.MainScope().launch {
                                    delay(1500) // Krótsze opóźnienie aby przejść po wybuchu konfetti
                                    showCelebration = true
                                    onGiftClicked()
                                }
                            }
                        },
                        onGiftLongPressed = {
                            // Aktywuj tryb timera i powiadom, że został odkryty
                            Timber.d("Prezent długo naciśnięty, aktywuję tryb timera")
                            if (!isTimerMode) {
                                isTimerMode = true
                                timerMinutes = 5
                                changeAppName = false
                                timerFinished = false
                                onTimerModeDiscovered()
                            }
                        },
                        giftReceived = giftReceived || timerModeEnabled,
                        modifier = Modifier.weight(1f)
                    )

                    // Sekcja odliczania
                    CountdownSection(
                        timeRemaining = if (isTimerMode) timerRemainingTime else timeRemaining,
                        isTimeUp = if (isTimerMode) timerFinished else isTimeUp,
                        isTimerMode = isTimerMode,
                        onTimerMinutesChanged = { minutes ->
                            timerMinutes = minutes
                        },
                        timerMinutes = timerMinutes,
                        changeAppName = changeAppName,
                        onChangeAppNameChanged = { checked ->
                            changeAppName = checked
                        },
                        onResetTimer = onResetTimer,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }

            // Wyświetlaj fajerwerki natychmiast, gdy czas się skończy, ale tylko w trybie urodzinowym
            AnimatedVisibility(
                visible = (isTimeUp && !showCelebration && !isTimerMode),
                enter = fadeIn(tween(300)),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ExplosiveFireworksDisplay()
                }
            }

            // Wybuch konfetti gdy prezent jest kliknięty lub gdy czas upłynie (tylko w trybie urodzinowym)
            AnimatedVisibility(
                visible = showConfettiExplosion && !showCelebration && !isTimerMode,
                enter = fadeIn(tween(100)),
                exit = fadeOut()
            ) {
                ConfettiExplosionEffect(
                    centerX = confettiCenterX, centerY = confettiCenterY
                )
            }

            // Ekran celebracji po kliknięciu prezentu
            AnimatedVisibility(
                visible = showCelebration, enter = fadeIn(), exit = fadeOut()
            ) {
                BirthdayMessage(
                    modifier = Modifier.fillMaxSize(), onBackClick = {
                        // Przy powrocie do głównego ekranu resetuj wybuch konfetti
                        showConfettiExplosion = false
                        showCelebration = false
                    })
            }

            // Ekran po zakończeniu timera - bez fajerwerków
            AnimatedVisibility(
                visible = showTimerFinishedCelebration, enter = fadeIn(), exit = fadeOut()
            ) {
                TimerFinishedMessage(
                    minutes = timerMinutes,
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = {
                        // Przy powrocie do głównego ekranu
                        showTimerFinishedCelebration = false
                        isTimerMode = false // Wróć do trybu urodzinowego
                    },
                    onResetTimer = {
                        // Resetuj timer
                        showTimerFinishedCelebration = false
                        onResetTimer()
                    })
            }

            // Dialog postępu zmiany nazwy aplikacji
            if (showProgressDialog) {
                AlertDialog(
                    onDismissRequest = { showProgressDialog = false },
                    title = { Text("Zmieniam nazwę aplikacji") },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                progress = { progressValue },
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Zmieniam nazwę aplikacji na 'Lawendowy Timer'...",
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    confirmButton = { })
            }
        }
    }
}

