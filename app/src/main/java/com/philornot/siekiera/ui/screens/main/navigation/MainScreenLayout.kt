package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.philornot.siekiera.ui.screens.main.effects.flashEffect
import com.philornot.siekiera.ui.screens.main.effects.shakeEffect
import com.philornot.siekiera.ui.screens.main.navigation.SwipeDetector.detectHorizontalSwipes
import com.philornot.siekiera.ui.screens.main.shared.AppBackground

/**
 * Layout komponent odpowiedzialny za układ UI MainScreen. Zawiera tło,
 * nawigację i deleguje zawartość do innych komponentów.
 *
 * Zaktualizowany o logikę dostępności drawer - dostępny gdy:
 * - Czas upłynął (isTimeUp)
 * - Dzisiaj są urodziny (isTodayBirthday)
 * - Urodziny już były w tym roku ale nie dzisiaj (isBirthdayPastThisYear)
 */
@Composable
fun MainScreenLayout(
    modifier: Modifier = Modifier,
    // Stan aplikacji
    isTimeUp: Boolean,
    timeRemaining: Long,
    isTimerMode: Boolean,
    timerRemainingTime: Long,
    timerFinished: Boolean,
    showCelebration: Boolean,
    showConfettiExplosion: Boolean,
    confettiCenterX: Float,
    confettiCenterY: Float,
    timerMinutes: Int,
    showProgressDialog: Boolean,
    progressValue: Float,
    // Parametry zewnętrzne
    giftReceived: Boolean,
    isTimerPaused: Boolean,
    isDrawerOpen: Boolean,
    currentSection: NavigationSection,
    isDarkTheme: Boolean,
    currentAppName: String,
    isTodayBirthday: Boolean,
    isBirthdayPastThisYear: Boolean,
    // Callbacki
    onGiftClicked: (Float, Float) -> Unit,
    onTimerReset: () -> Unit,
    onCelebrationBack: () -> Unit,
    onTimerFinishedReset: () -> Unit,
    onDrawerStateChange: (Boolean) -> Unit,
    onSectionSelected: (NavigationSection) -> Unit,
    onTimerSet: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onThemeToggle: (Boolean) -> Unit,
    onAppNameChange: (String) -> Unit,
    onAppNameReset: () -> Unit,
) {
    // Określ czy drawer powinien być dostępny
    val isDrawerAvailable = isTimeUp || isTodayBirthday || isBirthdayPastThisYear

    // Główny kontener z efektami
    Box(
        modifier = modifier
            .fillMaxSize()
            .shakeEffect(timeRemaining = if (isTimerMode) timerRemainingTime else timeRemaining)
            .flashEffect(timeRemaining = if (isTimerMode) timerRemainingTime else timeRemaining)
            .then(
                // Dodaj obsługę swipe tylko gdy szufladka jest dostępna
                if (isDrawerAvailable) {
                    Modifier.Companion.detectHorizontalSwipes(onSwipeLeft = {
                        onDrawerStateChange(
                            false
                        )
                    }, onSwipeRight = { onDrawerStateChange(true) })
                } else {
                    Modifier
                }
            )
    ) {
        // Tło aplikacji - pokazuj celebracyjne tło gdy dostępny drawer lub timer zakończony
        AppBackground(isTimeUp = isDrawerAvailable || (isTimerMode && timerFinished))

        // Warstwa nawigacji
        MainScreenNavigation(
            isTimeUp = isTimeUp,
            isTodayBirthday = isTodayBirthday,
            isBirthdayPastThisYear = isBirthdayPastThisYear,
            isDrawerOpen = isDrawerOpen,
            currentSection = currentSection,
            onDrawerStateChange = onDrawerStateChange,
            onSectionSelected = onSectionSelected
        )

        // Główna zawartość
        MainScreenContent(
            isTimeUp = isTimeUp,
            timeRemaining = timeRemaining,
            isTimerMode = isTimerMode,
            timerRemainingTime = timerRemainingTime,
            timerFinished = timerFinished,
            showCelebration = showCelebration,
            timerMinutes = timerMinutes,
            giftReceived = giftReceived,
            isTimerPaused = isTimerPaused,
            currentSection = currentSection,
            isDarkTheme = isDarkTheme,
            currentAppName = currentAppName,
            isTodayBirthday = isTodayBirthday,
            isDrawerAvailable = isDrawerAvailable,
            onGiftClicked = onGiftClicked,
            onTimerReset = onTimerReset,
            onCelebrationBack = onCelebrationBack,
            onTimerFinishedReset = onTimerFinishedReset,
            onTimerSet = onTimerSet,
            onPauseTimer = onPauseTimer,
            onResumeTimer = onResumeTimer,
            onThemeToggle = onThemeToggle,
            onAppNameChange = onAppNameChange,
            onAppNameReset = onAppNameReset
        )

        // Warstwa efektów i overlay
        MainScreenOverlays(
            isTimeUp = isTimeUp,
            isTimerMode = isTimerMode,
            showCelebration = showCelebration,
            showConfettiExplosion = showConfettiExplosion,
            confettiCenterX = confettiCenterX,
            confettiCenterY = confettiCenterY,
            currentSection = currentSection,
            showProgressDialog = showProgressDialog,
            progressValue = progressValue
        )
    }
}