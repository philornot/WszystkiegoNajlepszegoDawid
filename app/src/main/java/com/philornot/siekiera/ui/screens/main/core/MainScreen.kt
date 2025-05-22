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
import com.philornot.siekiera.workers.FileCheckWorker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Główny ekran z czystym, lawendowym motywem i minimalnymi animacjami.
 * Teraz obsługuje zarówno tryb odliczania urodzin jak i tryb timera.
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
    var changeAppName by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableIntStateOf(5) }

    // Stan dla dialogu zmiany nazwy aplikacji
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }

    // Czy pokazujemy fajerwerki po zakończeniu timera
    var showTimerFinishedCelebration by remember { mutableStateOf(false) }

    // Pozostały czas timera, jeśli aktywny
    var timerRemainingTime by remember { mutableLongStateOf(activeTimer) }
    var timerFinished by remember { mutableStateOf(false) }

    // Czy aplikacja jest w trybie timera (kontrolowane teraz przez currentSection)
    val isTimerMode = currentSection == NavigationSection.TIMER

    // Inicjalizacja - sprawdź czy timer jest aktywny
    LaunchedEffect(activeTimer, isTimerPaused) {
        if (activeTimer > 0) {
            timerRemainingTime = activeTimer
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
            MainScope().launch {
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

            if (isTimerMode && timerRemainingTime > 0 && !isTimerPaused) {
                // W trybie timera aktualizuj pozostały czas tylko jeśli nie jest spauzowany
                timerRemainingTime -= 1000

                if (timerRemainingTime <= 0) {
                    // Timer zakończony
                    timerRemainingTime = 0
                    timerFinished = true
                    showTimerFinishedCelebration = true

                    // Nie pokazujemy fajerwerków w trybie timera
                }
            } else if (!isTimerMode && currentSection == NavigationSection.BIRTHDAY_COUNTDOWN) {
                // W trybie odliczania urodzin aktualizuj pozostały czas
                timeRemaining = (targetDate - currentTimeState).coerceAtLeast(0)

                // Sprawdzaj częściej gdy pozostało mało czasu
                if (activity != null && timeRemaining > 0) {
                    val remainingMinutes = timeRemaining / 60000

                    // Określ minimalny interwał między sprawdzeniami
                    val checkInterval = when {
                        remainingMinutes <= 1 -> 30_000      // 30 sekund w ostatniej minucie
                        remainingMinutes <= 5 -> 60_000      // 1 minuta
                        remainingMinutes <= 15 -> 120_000    // 2 minuty
                        remainingMinutes <= 60 -> 300_000    // 5 minut
                        else -> 900_000                       // 15 minut
                    }

                    val currentTime = System.currentTimeMillis()

                    // Sprawdź tylko jeśli:
                    // 1. Prezent nie został jeszcze odebrany (używamy giftReceived, które już jest parametrem)
                    // 2. Minął odpowiedni czas od ostatniego sprawdzenia
                    // 3. Sprawdzenie nie jest zablokowane przez inny worker
                    if (!giftReceived && currentTime - lastCheckTime >= checkInterval && FileCheckWorker.canCheckFile(
                            activity
                        )
                    ) {

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
            // Add swipe detection if gift has been received
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
                    // Sprawdź aktualną sekcję i wyświetl odpowiednią zawartość
                    when (currentSection) {
                        NavigationSection.BIRTHDAY_COUNTDOWN -> {
                            // Standardowy ekran odliczania urodzin

                            // Nagłówek z tytułem
                            HeaderSection()

                            // Sekcja kurtyny lub prezentu
                            CurtainSection(
                                isTimeUp = isTimeUp,
                                showGift = true,
                                onGiftClicked = { centerX, centerY ->
                                    // Zapisz pozycję kliknięcia i pokaż konfetti
                                    confettiCenterX = centerX
                                    confettiCenterY = centerY

                                    // Pokaż wybuch konfetti
                                    showConfettiExplosion = true

                                    // Opóźnij pokazanie ekranu celebracji
                                    MainScope().launch {
                                        // Oznacz, że prezent został kliknięty, aby pokazać drawer
                                        giftWasClicked = true

                                        delay(1500) // Krótsze opóźnienie aby przejść po wybuchu konfetti
                                        showCelebration = true
                                        onGiftClicked()
                                    }
                                },
                                onGiftLongPressed = {
                                    // Aktywuj tryb timera i powiadom, że został odkryty
                                    Timber.d("Prezent długo naciśnięty, aktywuję tryb timera")
                                    if (!timerModeEnabled) {
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
                            // Użyj wydzielonego ekranu timera
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
                            // Użyj wydzielonego ekranu prezentu
                            GiftScreen(
                                onGiftClicked = onGiftClicked, giftReceived = giftReceived
                            )
                        }
                    }
                }
            }

            // Wyświetlaj fajerwerki natychmiast, gdy czas się skończy, ale tylko w trybie urodzinowym
            AnimatedVisibility(
                visible = (isTimeUp && !showCelebration && currentSection == NavigationSection.BIRTHDAY_COUNTDOWN),
                enter = fadeIn(tween(300)),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ExplosiveFireworksDisplay()
                }
            }

            // Wybuch konfetti gdy prezent jest kliknięty lub gdy czas upłynie (tylko w trybie urodzinowym)
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
                        onSectionSelected(NavigationSection.BIRTHDAY_COUNTDOWN)
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