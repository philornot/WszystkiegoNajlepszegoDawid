package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.screens.main.birthday.CountdownSection
import com.philornot.siekiera.ui.screens.main.birthday.CurtainSection
import com.philornot.siekiera.ui.screens.main.birthday.BirthdayMessage
import com.philornot.siekiera.ui.screens.main.shared.HeaderSection
import com.philornot.siekiera.ui.screens.main.shared.GiftScreen
import com.philornot.siekiera.ui.screens.main.timer.TimerFinishedMessage
import com.philornot.siekiera.ui.screens.main.timer.TimerScreen
import com.philornot.siekiera.ui.screens.settings.SettingsScreen
import timber.log.Timber

/**
 * Komponent odpowiedzialny za główną zawartość MainScreen. Renderuje
 * odpowiednią sekcję w zależności od currentSection.
 */
@Composable
fun MainScreenContent(
    modifier: Modifier = Modifier,
    // Stan aplikacji
    isTimeUp: Boolean,
    timeRemaining: Long,
    isTimerMode: Boolean,
    timerRemainingTime: Long,
    timerFinished: Boolean,
    showCelebration: Boolean,
    timerMinutes: Int,
    // Parametry zewnętrzne
    giftReceived: Boolean,
    timerModeEnabled: Boolean,
    isTimerPaused: Boolean,
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
    onTimerSet: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onThemeToggle: (Boolean) -> Unit,
    onAppNameChange: (String) -> Unit,
    onAppNameReset: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Główna zawartość - nie pokazuj gdy timer się zakończył lub gdy celebracja
        AnimatedVisibility(
            visible = !showCelebration && !(isTimerMode && timerFinished),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (currentSection) {
                NavigationSection.BIRTHDAY_COUNTDOWN -> {
                    BirthdayCountdownContent(
                        isTimeUp = isTimeUp,
                        timeRemaining = timeRemaining,
                        timerMinutes = timerMinutes,
                        giftReceived = giftReceived,
                        timerModeEnabled = timerModeEnabled,
                        isTimerPaused = isTimerPaused,
                        onGiftClicked = onGiftClicked,
                        onTimerModeDiscovered = onTimerModeDiscovered,
                        onPauseTimer = onPauseTimer,
                        onResumeTimer = onResumeTimer
                    )
                }

                NavigationSection.TIMER -> {
                    TimerContent(
                        timerRemainingTime = timerRemainingTime,
                        isTimerPaused = isTimerPaused,
                        onTimerSet = onTimerSet,
                        onPauseTimer = onPauseTimer,
                        onResumeTimer = onResumeTimer,
                        onResetTimer = onTimerReset
                    )
                }

                NavigationSection.GIFT -> {
                    GiftContent(
                        giftReceived = giftReceived, onGiftClicked = { onGiftClicked(0.5f, 0.5f) })
                }

                NavigationSection.SETTINGS -> {
                    SettingsContent(
                        currentAppName = currentAppName,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = onThemeToggle,
                        onAppNameChange = onAppNameChange,
                        onAppNameReset = onAppNameReset
                    )
                }
            }
        }

        // Ekran celebracji po kliknięciu prezentu
        AnimatedVisibility(
            visible = showCelebration, enter = fadeIn(), exit = fadeOut()
        ) {
            BirthdayMessage(
                modifier = Modifier.fillMaxSize(), onBackClick = onCelebrationBack
            )
        }

        // Ekran po zakończeniu timera
        AnimatedVisibility(
            visible = isTimerMode && timerFinished, enter = fadeIn(), exit = fadeOut()
        ) {
            TimerFinishedMessage(
                minutes = timerMinutes,
                modifier = Modifier.fillMaxSize(),
                onSetNewTimer = onTimerFinishedReset
            )
        }
    }
}

/** Zawartość sekcji odliczania do urodzin */
@Composable
private fun BirthdayCountdownContent(
    isTimeUp: Boolean,
    timeRemaining: Long,
    timerMinutes: Int,
    giftReceived: Boolean,
    timerModeEnabled: Boolean,
    isTimerPaused: Boolean,
    onGiftClicked: (Float, Float) -> Unit,
    onTimerModeDiscovered: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Przekaż informację o dostępności szufladki
        HeaderSection(hasDrawer = isTimeUp)

        CurtainSection(
            isTimeUp = isTimeUp,
            showGift = true,
            onGiftClicked = onGiftClicked,
            onGiftLongPressed = {
                Timber.d("Prezent długo naciśnięty, aktywuję tryb timera")
                onTimerModeDiscovered()
            },
            giftReceived = giftReceived || timerModeEnabled,
            modifier = Modifier.weight(1f)
        )

        CountdownSection(
            modifier = Modifier.padding(bottom = 24.dp),
            timeRemaining = timeRemaining,
            isTimeUp = isTimeUp,
            isTimerMode = false,
            onTimerMinutesChanged = { /* Nie używane w trybie urodzin */ },
            onTimerSet = { /* Nie używane w trybie urodzin */ },
            timerMinutes = timerMinutes,
            isTimerPaused = isTimerPaused,
            onPauseTimer = onPauseTimer,
            onResumeTimer = onResumeTimer
        )
    }
}

/** Zawartość sekcji timera */
@Composable
private fun TimerContent(
    timerRemainingTime: Long,
    isTimerPaused: Boolean,
    onTimerSet: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onResetTimer: () -> Unit,
) {
    TimerScreen(
        timerRemainingTime = timerRemainingTime,
        timerFinished = false,
        isTimerPaused = isTimerPaused,
        onTimerSet = onTimerSet,
        onPauseTimer = onPauseTimer,
        onResumeTimer = onResumeTimer,
        onResetTimer = onResetTimer
    )
}

/** Zawartość sekcji prezentu */
@Composable
private fun GiftContent(
    giftReceived: Boolean,
    onGiftClicked: () -> Unit,
) {
    GiftScreen(
        onGiftClicked = onGiftClicked, giftReceived = giftReceived
    )
}

/** Zawartość sekcji ustawień */
@Composable
private fun SettingsContent(
    currentAppName: String,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onAppNameChange: (String) -> Unit,
    onAppNameReset: () -> Unit,
) {
    SettingsScreen(
        currentAppName = currentAppName,
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        onAppNameChange = onAppNameChange,
        onAppNameReset = onAppNameReset
    )
}