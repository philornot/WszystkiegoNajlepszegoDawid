package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.philornot.siekiera.MainActivity
import com.philornot.siekiera.workers.FileCheckWorker
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Główny kontener MainScreen odpowiedzialny za zarządzanie stanem
 * aplikacji. Zawiera całą logikę stanu bez UI - deleguje renderowanie do
 * MainScreenLayout.
 */
@Composable
fun MainScreenContainer(
    modifier: Modifier = Modifier,
    targetDate: Long,
    currentTime: Long = System.currentTimeMillis(),
    onGiftClicked: () -> Unit,
    activity: MainActivity? = null,
    giftReceived: Boolean = false,
    isTodayBirthday: Boolean = false,
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
    // === STAN GŁÓWNY ===
    // Jeśli dzisiaj są urodziny, pokaż że czas upłynął, jeśli nie - sprawdź normalnie
    var isTimeUp by remember(isTodayBirthday) {
        mutableStateOf(isTodayBirthday || currentTime >= targetDate)
    }
    var currentTimeState by remember { mutableLongStateOf(currentTime) }
    var timeRemaining by remember {
        mutableLongStateOf(
            (targetDate - currentTimeState).coerceAtLeast(0)
        )
    }
    var lastCheckTime by remember { mutableLongStateOf(0L) }

    // === STAN CELEBRACJI I EFEKTÓW ===
    var showCelebration by remember { mutableStateOf(false) }
    var showConfettiExplosion by remember { mutableStateOf(false) }
    var confettiCenterX by remember { mutableFloatStateOf(0.5f) }
    var confettiCenterY by remember { mutableFloatStateOf(0.5f) }

    // === STAN TIMERA ===
    var timerMinutes by remember { mutableIntStateOf(5) }
    var timerRemainingTime by remember { mutableLongStateOf(activeTimer) }
    var timerFinished by remember { mutableStateOf(false) }
    var previousTimerActive by remember { mutableStateOf(false) }
    val isTimerMode = currentSection == NavigationSection.TIMER

    // === STAN DIALOGÓW ===
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }

    // === EFEKTY ZARZĄDZANIA TIMEREM ===
    LaunchedEffect(activeTimer, isTimerPaused, isTimerMode) {
        val isCurrentlyActive = activeTimer > 0

        when {
            isCurrentlyActive && !previousTimerActive -> {
                timerRemainingTime = activeTimer
                timerFinished = false
                Timber.d("Timer rozpoczęty: ${timerRemainingTime}ms")
            }

            !isCurrentlyActive && previousTimerActive && timerRemainingTime > 0 -> {
                Timber.d("Timer się zakończył")
                timerFinished = true
            }

            isCurrentlyActive -> {
                timerRemainingTime = activeTimer
            }
        }
        previousTimerActive = isCurrentlyActive
    }

    // === EFEKT AKTUALIZACJI CZASU ===
    LaunchedEffect(isTimerMode, activeTimer, isTimerPaused, isTodayBirthday) {
        while (true) {
            currentTimeState = System.currentTimeMillis()

            if (isTimerMode) {
                handleTimerModeUpdate(
                    activeTimer = activeTimer,
                    isTimerPaused = isTimerPaused,
                    onTimerRemainingTimeUpdate = { timerRemainingTime = it },
                    onTimerFinished = { timerFinished = true })
            } else {
                handleBirthdayModeUpdate(
                    currentTimeState = currentTimeState,
                    targetDate = targetDate,
                    currentSection = currentSection,
                    giftReceived = giftReceived,
                    isTodayBirthday = isTodayBirthday,
                    lastCheckTime = lastCheckTime,
                    activity = activity,
                    onTimeRemainingUpdate = { timeRemaining = it },
                    onIsTimeUpUpdate = { isTimeUp = it },
                    onLastCheckTimeUpdate = { lastCheckTime = it })
            }
        }
    }

    // === EFEKT FAJERWERKÓW ===
    LaunchedEffect(isTimeUp, isTodayBirthday) {
        // Pokaż fajerwerki jeśli czas upłynął (normalnie) lub jeśli dzisiaj są urodziny
        if ((isTimeUp && !isTimerMode) || isTodayBirthday) {
            Timber.d("Czas upłynął lub dzisiaj urodziny! Uruchamiam automatyczne fajerwerki!")
            showConfettiExplosion = true
            delay(5000)
            showConfettiExplosion = false
        }
    }

    // === EFEKT DIALOGU POSTĘPU ===
    LaunchedEffect(showProgressDialog) {
        if (showProgressDialog) {
            animateProgressDialog(onProgressUpdate = { progressValue = it }, onComplete = {
                onTimerSet(timerMinutes)
                showProgressDialog = false
            })
        }
    }

    // === RENDEROWANIE UI ===
    MainScreenLayout(
        modifier = modifier,
        isTimeUp = isTimeUp,
        timeRemaining = timeRemaining,
        isTimerMode = isTimerMode,
        timerRemainingTime = timerRemainingTime,
        timerFinished = timerFinished,
        showCelebration = showCelebration,
        showConfettiExplosion = showConfettiExplosion,
        confettiCenterX = confettiCenterX,
        confettiCenterY = confettiCenterY,
        timerMinutes = timerMinutes,
        showProgressDialog = showProgressDialog,
        progressValue = progressValue,
        // Parametry przekazane dalej
        giftReceived = giftReceived,
        timerModeEnabled = timerModeEnabled,
        isTimerPaused = isTimerPaused,
        isDrawerOpen = isDrawerOpen,
        currentSection = currentSection,
        isDarkTheme = isDarkTheme,
        currentAppName = currentAppName,
        isTodayBirthday = isTodayBirthday,
        // Callbacki
        onGiftClicked = { centerX, centerY ->
            confettiCenterX = centerX
            confettiCenterY = centerY
            showConfettiExplosion = true
            MainScope().launch {
                delay(1500)
                showCelebration = true
                onGiftClicked()
            }
        },
        onTimerModeDiscovered = {
            if (!timerModeEnabled) {
                timerMinutes = 5
                timerFinished = false
                onTimerModeDiscovered()
            }
        },
        onTimerMinutesChanged = { timerMinutes = it },
        onTimerReset = {
            timerFinished = false
            timerRemainingTime = 0L
            onResetTimer()
        },
        onCelebrationBack = {
            showConfettiExplosion = false
            showCelebration = false
        },
        onTimerFinishedReset = {
            timerFinished = false
            timerRemainingTime = 0L
            onResetTimer()
        },
        onDrawerStateChange = onDrawerStateChange,
        onSectionSelected = onSectionSelected,
        onTimerSet = onTimerSet,
        onPauseTimer = onPauseTimer,
        onResumeTimer = onResumeTimer,
        onThemeToggle = onThemeToggle,
        onAppNameChange = onAppNameChange,
        onAppNameReset = onAppNameReset
    )
}

/** Obsługuje aktualizację w trybie timera */
private suspend fun handleTimerModeUpdate(
    activeTimer: Long,
    isTimerPaused: Boolean,
    onTimerRemainingTimeUpdate: (Long) -> Unit,
    onTimerFinished: () -> Unit,
) {
    when {
        activeTimer > 0 && !isTimerPaused -> {
            onTimerRemainingTimeUpdate(activeTimer)
            // Naprawiona kondycja - sprawdzamy po aktualizacji
            delay(250)
            // Jeśli po delay timer się skończył, oznacz jako zakończony
            if (activeTimer <= 250) { // 250ms to nasza częstotliwość aktualizacji
                onTimerFinished()
                Timber.d("Timer zakończył odliczanie")
            }
        }

        isTimerPaused -> {
            onTimerRemainingTimeUpdate(activeTimer)
            delay(1000)
        }

        else -> {
            onTimerRemainingTimeUpdate(0L)
            delay(1000)
        }
    }
}

/** Obsługuje aktualizację w trybie urodzinowym */
private suspend fun handleBirthdayModeUpdate(
    currentTimeState: Long,
    targetDate: Long,
    currentSection: NavigationSection,
    giftReceived: Boolean,
    isTodayBirthday: Boolean,
    lastCheckTime: Long,
    activity: MainActivity?,
    onTimeRemainingUpdate: (Long) -> Unit,
    onIsTimeUpUpdate: (Boolean) -> Unit,
    onLastCheckTimeUpdate: (Long) -> Unit,
) {
    if (currentSection == NavigationSection.BIRTHDAY_COUNTDOWN) {
        val timeRemaining = (targetDate - currentTimeState).coerceAtLeast(0)
        onTimeRemainingUpdate(timeRemaining)

        // Sprawdzanie plików - tylko jeśli nie dzisiaj urodziny (bo wtedy nie ma sensu sprawdzać plików)
        if (activity != null && timeRemaining > 0 && !isTodayBirthday) {
            val remainingMinutes = timeRemaining / 60000
            val checkInterval = when {
                remainingMinutes <= 1 -> 30_000
                remainingMinutes <= 5 -> 60_000
                remainingMinutes <= 15 -> 120_000
                remainingMinutes <= 60 -> 300_000
                else -> 900_000
            }

            if (!giftReceived && currentTimeState - lastCheckTime >= checkInterval && FileCheckWorker.canCheckFile(
                    activity
                )
            ) {
                Timber.d("Uruchamiam dodatkowe sprawdzenie pliku")
                activity.checkFileNow()
                onLastCheckTimeUpdate(currentTimeState)
            }
        }

        // Sprawdź czy czas się skończył lub czy dzisiaj są urodziny
        if (currentTimeState >= targetDate || isTodayBirthday) {
            onIsTimeUpUpdate(true)
            if (activity != null && !isTodayBirthday) {
                // Sprawdź pliki tylko jeśli to nie dzisiaj urodziny
                Timber.d("Uruchamiam ostatnie sprawdzenie pliku po upływie czasu")
                activity.checkFileNow()
                onLastCheckTimeUpdate(currentTimeState)
            }
        }
    }
    delay(1000)
}

/** Animuje dialog postępu */
private suspend fun animateProgressDialog(
    onProgressUpdate: (Float) -> Unit,
    onComplete: () -> Unit,
) {
    onProgressUpdate(0f)
    val updateInterval = 50L
    val steps = 2000L / updateInterval
    val increment = 1f / steps
    var progress = 0f

    while (progress < 1f) {
        delay(updateInterval)
        progress += increment
        onProgressUpdate(progress)
    }
    onComplete()
}