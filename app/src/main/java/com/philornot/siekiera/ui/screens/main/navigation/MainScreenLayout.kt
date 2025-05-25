package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.philornot.siekiera.ui.screens.main.shared.AppBackground
import com.philornot.siekiera.ui.screens.main.navigation.SwipeDetector.detectHorizontalSwipes
import com.philornot.siekiera.ui.screens.main.effects.flashEffect
import com.philornot.siekiera.ui.screens.main.effects.shakeEffect

/**
 * Layout komponent odpowiedzialny za układ UI MainScreen. Zawiera tło,
 * nawigację i deleguje zawartość do innych komponentów.
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
    timerModeEnabled: Boolean,
    isTimerPaused: Boolean,
    isDrawerOpen: Boolean,
    currentSection: NavigationSection,
    isDarkTheme: Boolean,
    currentAppName: String,
    // Callbacki
    onGiftClicked: (Float, Float) -> Unit,
    onTimerModeDiscovered: () -> Unit,
    onTimerMinutesChanged: (Int) -> Unit,
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
    // Główny kontener z efektami
    Box(
        modifier = modifier
            .fillMaxSize()
            .shakeEffect(timeRemaining = if (isTimerMode) timerRemainingTime else timeRemaining)
            .flashEffect(timeRemaining = if (isTimerMode) timerRemainingTime else timeRemaining)
            .then(
                // Dodaj obsługę swipe tylko gdy szufladka jest dostępna
                if (isTimeUp) {
                    Modifier.Companion.detectHorizontalSwipes(
                        onSwipeLeft = { onDrawerStateChange(false) },
                        onSwipeRight = { onDrawerStateChange(true) })
                } else {
                    Modifier
                }
            )) {
        // Tło aplikacji
        AppBackground(isTimeUp = isTimeUp || (isTimerMode && timerFinished))

        // Warstwa nawigacji
        MainScreenNavigation(
            isTimeUp = isTimeUp,
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
            timerModeEnabled = timerModeEnabled,
            isTimerPaused = isTimerPaused,
            currentSection = currentSection,
            isDarkTheme = isDarkTheme,
            currentAppName = currentAppName,
            onGiftClicked = onGiftClicked,
            onTimerModeDiscovered = onTimerModeDiscovered,
            onTimerMinutesChanged = onTimerMinutesChanged,
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