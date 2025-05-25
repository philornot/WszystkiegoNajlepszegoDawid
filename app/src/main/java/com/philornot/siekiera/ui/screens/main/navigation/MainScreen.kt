package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.philornot.siekiera.MainActivity

/**
 * Główny ekran aplikacji - znacznie uproszczony dzięki wydzieleniu logiki
 * do osobnych komponentów. Teraz służy tylko jako publiczne API dla
 * MainActivity.
 *
 * Cała logika stanu została przeniesiona do MainScreenContainer, który
 * deleguje renderowanie do MainScreenLayout i jego podkomponentów.
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
    // Deleguj całą logikę do MainScreenContainer
    MainScreenContainer(
        modifier = modifier,
        targetDate = targetDate,
        currentTime = currentTime,
        onGiftClicked = onGiftClicked,
        activity = activity,
        giftReceived = giftReceived,
        onTimerSet = onTimerSet,
        timerModeEnabled = timerModeEnabled,
        onTimerModeDiscovered = onTimerModeDiscovered,
        activeTimer = activeTimer,
        isTimerPaused = isTimerPaused,
        onCancelTimer = onCancelTimer,
        onResetTimer = onResetTimer,
        onPauseTimer = onPauseTimer,
        onResumeTimer = onResumeTimer,
        isDrawerOpen = isDrawerOpen,
        onDrawerStateChange = onDrawerStateChange,
        currentSection = currentSection,
        onSectionSelected = onSectionSelected,
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        currentAppName = currentAppName,
        onAppNameChange = onAppNameChange,
        onAppNameReset = onAppNameReset
    )
}