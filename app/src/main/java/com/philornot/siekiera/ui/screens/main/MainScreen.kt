package com.philornot.siekiera.ui.screens.main

// Importy do efektów trzęsienia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Track if we should show confetti explosion when gift is clicked
    var showConfettiExplosion by remember { mutableStateOf(false) }

    // Remember the click position for confetti explosion
    var confettiCenterX by remember { mutableFloatStateOf(0.5f) }
    var confettiCenterY by remember { mutableFloatStateOf(0.5f) }

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

    // Main container with shake effect when time is almost up
    Box(
        modifier = modifier
            .fillMaxSize()
            .shakeEffect(timeRemaining = timeRemaining)
            .flashEffect(timeRemaining = timeRemaining)
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
                            isTimeUp = isTimeUp, onGiftClicked = { centerX, centerY ->
                                // When gift is clicked, show confetti explosion and then transition to celebration
                                if (isTimeUp) {
                                    // Record click position for confetti
                                    confettiCenterX = centerX
                                    confettiCenterY = centerY

                                    // Show confetti explosion
                                    showConfettiExplosion = true

                                    // Delay showing celebration screen
                                    MainScope().launch {
                                        delay(1500) // Shorter delay to transition after confetti explosion
                                        showCelebration = true
                                        onGiftClicked()
                                    }
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

                // Background fireworks when time is up
                if (isTimeUp && !showCelebration) {
                    // Always show fireworks once time is up
                    FireworksEffect()
                }

                // Confetti explosion when gift is clicked
                AnimatedVisibility(
                    visible = showConfettiExplosion && !showCelebration,
                    enter = fadeIn(tween(100)),
                    exit = fadeOut()
                ) {
                    ConfettiExplosionEffect(
                        centerX = confettiCenterX, centerY = confettiCenterY
                    )
                }

                // Celebration screen after clicking gift
                AnimatedVisibility(
                    visible = showCelebration, enter = fadeIn(), exit = fadeOut()
                ) {
                    BirthdayMessage(
                        modifier = Modifier.fillMaxSize(), onBackClick = {
                            // When returning to the main screen, reset the confetti explosion
                            showConfettiExplosion = false
                            showCelebration = false
                        })
                }
            }
        }
    }
}

/** Simple birthday message screen shown after clicking the gift. */
@Composable
fun BirthdayMessage(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(24.dp)
    ) {
        // Message content
        Box(
            modifier = Modifier.weight(1f), contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big 18 text
                Text(
                    text = "hej Dawid!",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Birthday wishes
                Text(
                    text = "Sorki, że tyle musiałeś czekać, ale uznałem że jednak im więcej wpisów będziesz mieć do przeczytania, tym lepiej.\n" + "A, no i w pewnie powinienem Ci dać prezent urodzinowy w urodziny, bo tradycje i w ogóle.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Na poprzednim ekranie było napisane coś w stylu, że 'udało ci się tyle przeżyć'.\n" + "Chciałbym mocno podkreślić, że to nie znaczy, że to już koniec.\n" + "Wytrzymaj jeszcze chwilkę.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Nie chcę tutaj dużo pisać, bo nie jestem w stanie tego tak łatwo zmienić jak wpis w daylio. Więc po prostu zaimportuj wpisy, tam na pewno ja z przyszłości Ci coś lepszego napiszę. Na pewno. Słyszysz Filip? W Daylio będą życzenia, tak?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Spacer o zmiennej wysokości dla lepszego układu
                Spacer(modifier = Modifier.weight(1f))

                // Nowe elementy - tekst zachęcający do powrotu
                Text(
                    text = "Nie odebrałeś prezentu?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Kliknij strzałkę, aby wrócić do poprzedniego ekranu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Przycisk powrotu ze strzałką
                IconButton(
                    onClick = onBackClick, modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Wróć",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "A może po prostu chcesz pooglądać fajerwerki, nad którymi kompletnie nie spędziłem 3h...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}