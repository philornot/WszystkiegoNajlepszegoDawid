package com.philornot.siekiera.ui.screens.main.core

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.philornot.siekiera.ui.screens.main.countdown.CountdownSection
import com.philornot.siekiera.ui.screens.main.countdown.ExplosiveFireworksDisplay
import com.philornot.siekiera.ui.screens.main.curtain.CurtainSection
import com.philornot.siekiera.ui.screens.main.drawer.NavigationDrawer
import com.philornot.siekiera.ui.screens.main.drawer.NavigationSection
import com.philornot.siekiera.ui.screens.main.drawer.SwipeDetector.detectHorizontalSwipes
import com.philornot.siekiera.ui.screens.main.effects.ConfettiExplosionEffect
import com.philornot.siekiera.ui.screens.main.effects.flashEffect
import com.philornot.siekiera.ui.screens.main.effects.shakeEffect
import com.philornot.siekiera.ui.screens.main.gift.BirthdayMessage
import com.philornot.siekiera.ui.screens.main.gift.GiftScreen
import com.philornot.siekiera.ui.screens.main.timer.TimerFinishedMessage
import com.philornot.siekiera.ui.screens.main.timer.TimerScreen
import com.philornot.siekiera.ui.screens.settings.SettingsScreen
import com.philornot.siekiera.workers.FileCheckWorker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Główny ekran z czystym, lawendowym motywem i minimalnymi animacjami.
 * Ulepszona wersja z lepszą responsywnością timera i animacjami.
 * Zawiera również szufladkę nawigacyjną po odebraniu prezentu.
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
 * @param isTimerPaused Czy timer jest spauzowany
 * @param onCancelTimer Callback do anulowania timera
 * @param onResetTimer Callback do resetowania timera
 * @param onPauseTimer Callback do pauzowania timera
 * @param onResumeTimer Callback do wznawiania timera
 * @param isDrawerOpen Czy szufladka nawigacyjna jest otwarta
 * @param onDrawerStateChange Callback wywoływany przy zmianie stanu
 *    szufladki
 * @param currentSection Obecnie wybrana sekcja nawigacyjna
 * @param onSectionSelected Callback wywoływany przy wyborze sekcji
 * @param isDarkTheme Czy aktywny jest ciemny motyw
 * @param onThemeToggle Callback wywoływany przy zmianie motywu
 * @param currentAppName Aktualna nazwa aplikacji
 * @param onAppNameChange Callback wywoływany przy zmianie nazwy aplikacji
 * @param onAppNameReset Callback wywoływany przy resetowaniu nazwy
 *    aplikacji
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
    isTimerPaused: Boolean = false,
    onCancelTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    isDrawerOpen: Boolean = false,
    onDrawerStateChange: (Boolean) -> Unit = {},
    currentSection: NavigationSection = NavigationSection.BIRTHDAY_COUNTDOWN,
    onSectionSelected: (NavigationSection) -> Unit = {},
    isDarkTheme: Boolean = false,
    onThemeToggle: (Boolean) -> Unit = {},
    currentAppName: String = "",
    onAppNameChange: (String) -> Unit = {},
    onAppNameReset: () -> Unit = {},
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

    // Śledź czy prezent został kliknięty (lokalny stan dla pokazywania szufladki)
    var giftWasClicked by remember { mutableStateOf(giftReceived) }

    // Śledź czy pokazać wybuch konfetti gdy prezent jest kliknięty lub gdy czas upłynie
    var showConfettiExplosion by remember { mutableStateOf(false) }

    // Pamiętaj pozycję kliknięcia dla wybuchu konfetti
    var confettiCenterX by remember { mutableFloatStateOf(0.5f) }
    var confettiCenterY by remember { mutableFloatStateOf(0.5f) }

    // Stany dla trybu timera
    var timerMinutes by remember { mutableIntStateOf(5) }

    // Stan dla dialogu zmiany nazwy aplikacji
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }

    // Czy pokazujemy fajerwerki po zakończeniu timera
    var showTimerFinishedCelebration by remember { mutableStateOf(false) }

    // Pozostały czas timera, jeśli aktywny - ulepszony tracking
    var timerRemainingTime by remember { mutableLongStateOf(activeTimer) }
    var timerFinished by remember { mutableStateOf(false) }
    var previousTimerActive by remember { mutableStateOf(false) }

    // Czy aplikacja jest w trybie timera (kontrolowane teraz przez currentSection)
    val isTimerMode = currentSection == NavigationSection.TIMER

    // Ulepszona inicjalizacja - lepsze śledzenie zmian stanu timera
    LaunchedEffect(activeTimer, isTimerPaused, isTimerMode) {
        val isCurrentlyActive = activeTimer > 0

        // Sprawdź czy timer właśnie się rozpoczął
        if (isCurrentlyActive && !previousTimerActive) {
            timerRemainingTime = activeTimer
            timerFinished = false
        }
        // Sprawdź czy timer właśnie się zakończył
        else if (!isCurrentlyActive && previousTimerActive && timerRemainingTime > 0) {
            Timber.d("Timer się zakończył - pokazuję celebrację")
            timerFinished = true
            if (isTimerMode) {
                showTimerFinishedCelebration = true
            }
        }
        // Normalne śledzenie aktywnego timera
        else if (isCurrentlyActive) {
            timerRemainingTime = activeTimer
        }

        previousTimerActive = isCurrentlyActive
    }

    // Reagowanie na zmianę stanu isTimeUp - automatyczne fajerwerki po zakończeniu odliczania
    // (tylko w trybie urodzinowym)
    LaunchedEffect(isTimeUp) {
        if (isTimeUp && !isTimerMode) {
            Timber.d("Czas upłynął! Uruchamiam automatyczne fajerwerki!")
            showConfettiExplosion = true

            MainScope().launch {
                delay(5000)
                showConfettiExplosion = false
            }
        }
    }

    // Ulepszony system aktualizacji czasu - bardziej responsywny dla timera
    LaunchedEffect(Unit) {
        while (true) {
            if (isTimerMode && timerRemainingTime > 0 && !isTimerPaused) {
                // Częstsze aktualizacje dla aktywnego timera (co 100ms dla płynności)
                delay(100)
                currentTimeState = System.currentTimeMillis()

                // Aktualizuj timer co 1000ms (sekundę)
                if (System.currentTimeMillis() % 1000 < 100) {
                    timerRemainingTime -= 1000

                    if (timerRemainingTime <= 0) {
                        timerRemainingTime = 0
                        timerFinished = true
                        showTimerFinishedCelebration = true
                        Timber.d("Timer zakończył odliczanie")
                    }
                }
                100L
            } else {
                // Standardowe aktualizacje co sekundę dla pozostałych trybów
                delay(1000)
                currentTimeState = System.currentTimeMillis()

                if (!isTimerMode && currentSection == NavigationSection.BIRTHDAY_COUNTDOWN) {
                    timeRemaining = (targetDate - currentTimeState).coerceAtLeast(0)

                    // Sprawdzaj pliki gdy zbliża się koniec
                    if (activity != null && timeRemaining > 0) {
                        val remainingMinutes = timeRemaining / 60000
                        val checkInterval = when {
                            remainingMinutes <= 1 -> 30_000
                            remainingMinutes <= 5 -> 60_000
                            remainingMinutes <= 15 -> 120_000
                            remainingMinutes <= 60 -> 300_000
                            else -> 900_000
                        }

                        val currentTime = System.currentTimeMillis()

                        if (!giftReceived && currentTime - lastCheckTime >= checkInterval && FileCheckWorker.canCheckFile(
                                activity
                            )
                        ) {

                            Timber.d("Uruchamiam dodatkowe sprawdzenie pliku, pozostało $remainingMinutes minut")
                            activity.checkFileNow()
                            lastCheckTime = currentTime
                        }
                    }

                    // Sprawdź czy czas się skończył
                    if (currentTimeState >= targetDate && !isTimeUp) {
                        isTimeUp = true

                        if (activity != null) {
                            Timber.d("Uruchamiam ostatnie sprawdzenie pliku po upływie czasu")
                            activity.checkFileNow()
                            lastCheckTime = currentTimeState
                        }
                    }
                }
                1000L
            }
        }
    }

    // Obsługa dialogu postępu zmiany nazwy aplikacji
    LaunchedEffect(showProgressDialog) {
        if (showProgressDialog) {
            progressValue = 0f

            val updateInterval = 50L
            val steps = 2000L / updateInterval
            val increment = 1f / steps

            while (progressValue < 1f) {
                delay(updateInterval)
                progressValue += increment
            }

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
            .then(
                if (giftWasClicked) {
                    Modifier.detectHorizontalSwipes(
                        onSwipeLeft = { onDrawerStateChange(false) },
                        onSwipeRight = { onDrawerStateChange(true) })
                } else {
                    Modifier
                }
            )
    ) {
        // Tło aplikacji
        AppBackground(isTimeUp = isTimeUp || (isTimerMode && timerFinished))

        // Navigation drawer (pokazuje się po kliknięciu prezentu)
        if (giftWasClicked) {
            NavigationDrawer(
                isOpen = isDrawerOpen,
                onOpenStateChange = onDrawerStateChange,
                currentSection = currentSection,
                onSectionSelected = onSectionSelected
            )
        }

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
                    when (currentSection) {
                        NavigationSection.BIRTHDAY_COUNTDOWN -> {
                            HeaderSection(hasDrawer = giftWasClicked)

                            CurtainSection(
                                isTimeUp = isTimeUp,
                                showGift = true,
                                onGiftClicked = { centerX, centerY ->
                                    confettiCenterX = centerX
                                    confettiCenterY = centerY
                                    showConfettiExplosion = true

                                    MainScope().launch {
                                        giftWasClicked = true
                                        delay(1500)
                                        showCelebration = true
                                        onGiftClicked()
                                    }
                                },
                                onGiftLongPressed = {
                                    Timber.d("Prezent długo naciśnięty, aktywuję tryb timera")
                                    if (!timerModeEnabled) {
                                        timerMinutes = 5
                                        timerFinished = false
                                        onTimerModeDiscovered()
                                    }
                                },
                                giftReceived = giftReceived || timerModeEnabled,
                                modifier = Modifier.weight(1f)
                            )

                            CountdownSection(
                                modifier = Modifier.padding(bottom = 24.dp),
                                timeRemaining = timeRemaining,
                                isTimeUp = isTimeUp,
                                onTimerMinutesChanged = { /* Nie używane w trybie urodzin */ },
                                onTimerSet = { /* Nie używane w trybie urodzin */ },
                                timerMinutes = timerMinutes,
                                isTimerPaused = isTimerPaused,
                                onPauseTimer = onPauseTimer,
                                onResumeTimer = onResumeTimer
                            )
                        }

                        NavigationSection.TIMER -> {
                            // Ulepszona wersja TimerScreen z lepszą responsywnością
                            TimerScreen(
                                timerRemainingTime = timerRemainingTime,
                                timerFinished = timerFinished,
                                isTimerPaused = isTimerPaused,
                                onTimerSet = onTimerSet,
                                onPauseTimer = onPauseTimer,
                                onResumeTimer = onResumeTimer,
                                onResetTimer = onResetTimer
                            )
                        }

                        NavigationSection.GIFT -> {
                            GiftScreen(
                                onGiftClicked = onGiftClicked, giftReceived = giftReceived
                            )
                        }

                        NavigationSection.SETTINGS -> {
                            SettingsScreen(
                                currentAppName = currentAppName,
                                isDarkTheme = isDarkTheme,
                                onThemeToggle = onThemeToggle,
                                onAppNameChange = onAppNameChange,
                                onAppNameReset = onAppNameReset
                            )
                        }
                    }
                }
            }

            // Fajerwerki dla zakończenia odliczania urodzin
            AnimatedVisibility(
                visible = (isTimeUp && !showCelebration && currentSection == NavigationSection.BIRTHDAY_COUNTDOWN),
                enter = fadeIn(tween(300)),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ExplosiveFireworksDisplay()
                }
            }

            // Wybuch konfetti
            AnimatedVisibility(
                visible = showConfettiExplosion && !showCelebration && currentSection == NavigationSection.BIRTHDAY_COUNTDOWN,
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
                        showConfettiExplosion = false
                        showCelebration = false
                    })
            }

            // Ekran po zakończeniu timera
            AnimatedVisibility(
                visible = showTimerFinishedCelebration, enter = fadeIn(), exit = fadeOut()
            ) {
                TimerFinishedMessage(
                    minutes = timerMinutes,
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = {
                        showTimerFinishedCelebration = false
                        onSectionSelected(NavigationSection.BIRTHDAY_COUNTDOWN)
                    },
                    onResetTimer = {
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