package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Redesigned main screen with a clean, lavender theme and minimal
 * animations.
 *
 * @param modifier Modifier for the container
 * @param targetDate Birthday date in milliseconds
 * @param currentTime Current time (default is system time)
 * @param onGiftClicked Callback for when the gift is clicked
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    targetDate: Long,
    currentTime: Long = System.currentTimeMillis(),
    onGiftClicked: () -> Unit,
) {
    // Calculate if time is up
    var isTimeUp by remember { mutableStateOf(currentTime >= targetDate) }

    // Track current time (updated every second)
    var currentTimeState by remember { mutableLongStateOf(currentTime) }
    var timeRemaining by remember {
        mutableLongStateOf(
            (targetDate - currentTimeState).coerceAtLeast(
                0
            )
        )
    }

    // Track if we should show celebration screen after clicking gift
    var showCelebration by remember { mutableStateOf(false) }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeState = System.currentTimeMillis()
            timeRemaining = (targetDate - currentTimeState).coerceAtLeast(0)

            // Check if time is up
            if (currentTimeState >= targetDate && !isTimeUp) {
                isTimeUp = true
            }
        }
    }

    // Main container
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // App background
        AppBackground(isTimeUp = isTimeUp)

        // Main content
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
        ) {
            // Main content
            Box(modifier = Modifier.fillMaxSize()) {
                // Regular screen with waiting/gift
                AnimatedVisibility(
                    visible = !showCelebration, enter = fadeIn(), exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with title
                        HeaderSection()

                        // Curtain or gift section
                        CurtainSection(
                            isTimeUp = isTimeUp, onGiftClicked = {
                                // Show celebration screen after clicking gift
                                if (isTimeUp) {
                                    showCelebration = true
                                    onGiftClicked()
                                }
                            }, modifier = Modifier.weight(1f)
                        )

                        // Countdown section
                        CountdownSection(
                            timeRemaining = timeRemaining,
                            isTimeUp = isTimeUp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                }

                // Simple confetti effect when time is up
                if (isTimeUp && !showCelebration) {
                    ConfettiEffect()
                }

                // Celebration screen after clicking gift
                AnimatedVisibility(
                    visible = showCelebration, enter = fadeIn(), exit = fadeOut()
                ) {
                    BirthdayMessage(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/** Simple birthday message screen shown after clicking the gift. */
@Composable
fun BirthdayMessage(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(24.dp)
    ) {
        // Header
        HeaderSection()

        // Message content
        Box(
            modifier = Modifier.weight(1f), contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big 18 text
                androidx.compose.material3.Text(
                    text = "18 Lat!",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Birthday wishes
                androidx.compose.material3.Text(
                    text = "Osiemnastka to wyjątkowy moment w życiu. Z okazji Twoich urodzin życzę Ci wszystkiego najlepszego!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                androidx.compose.material3.Text(
                    text = "Niech Twoje marzenia się spełniają, a plany realizują. Ciesz się w pełni dorosłością!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}