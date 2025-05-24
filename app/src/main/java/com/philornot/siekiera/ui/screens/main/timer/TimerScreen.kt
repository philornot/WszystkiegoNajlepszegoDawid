package com.philornot.siekiera.ui.screens.main.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.screens.main.countdown.CountdownSection
import kotlinx.coroutines.delay

/**
 * Ulepszona wersja ekranu timera z lepszą responsywnością i wizualną
 * obsługą stanu. Pozwala na ustawienie własnego timera niezależnie od
 * odliczania urodzin.
 *
 * @param timerRemainingTime Pozostały czas timera w milisekundach
 * @param timerFinished Czy timer zakończył odliczanie
 * @param isTimerPaused Czy timer jest spauzowany
 * @param onTimerSet Callback wywoływany po ustawieniu timera
 * @param onPauseTimer Callback wywoływany po spauzowaniu timera
 * @param onResumeTimer Callback wywoływany po wznowieniu timera
 * @param onResetTimer Callback wywoływany po resetowaniu timera
 * @param modifier Modyfikator dla całego ekranu
 */
@Composable
fun TimerScreen(
    timerRemainingTime: Long,
    timerFinished: Boolean,
    isTimerPaused: Boolean = false,
    onTimerSet: (Int) -> Unit,
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onResetTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lokalne stany dla ekranu timera
    var timerMinutes by remember { mutableIntStateOf(5) }

    // Stany animacji
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var showSetupHint by remember { mutableStateOf(true) }

    // Stan czy timer jest aktywny
    val isTimerActive = timerRemainingTime > 0

    // Efekty reaktywne na zmiany stanu
    LaunchedEffect(isTimerActive) {
        if (isTimerActive) {
            showSetupHint = false
        } else {
            // Krótkie opóźnienie przed pokazaniem hintu przy resetowaniu
            delay(300)
            showSetupHint = true
        }
    }

    // Animacja sukcesu po ustawieniu timera
    LaunchedEffect(isTimerActive, isTimerPaused) {
        if (isTimerActive && !isTimerPaused) {
            showSuccessAnimation = true
            delay(2000)
            showSuccessAnimation = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamiczny nagłówek z ikoną stanu
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    timerFinished -> MaterialTheme.colorScheme.primaryContainer
                    isTimerActive && isTimerPaused -> MaterialTheme.colorScheme.secondaryContainer
                    isTimerActive -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Ikona stanu
                Icon(
                    imageVector = when {
                        timerFinished -> Icons.Default.CheckCircle
                        isTimerActive && isTimerPaused -> Icons.Default.Pause
                        isTimerActive -> Icons.Default.PlayArrow
                        else -> Icons.Default.AccessTime
                    }, contentDescription = null, tint = when {
                        timerFinished -> MaterialTheme.colorScheme.primary
                        isTimerActive && isTimerPaused -> MaterialTheme.colorScheme.secondary
                        isTimerActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }, modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Tekst nagłówka
                Text(
                    text = when {
                        timerFinished -> "Timer zakończony"
                        isTimerActive && isTimerPaused -> "Timer spauzowany"
                        isTimerActive -> "Timer aktywny"
                        else -> "Tryb Timera"
                    }, style = MaterialTheme.typography.headlineSmall, color = when {
                        timerFinished -> MaterialTheme.colorScheme.primary
                        isTimerActive && isTimerPaused -> MaterialTheme.colorScheme.secondary
                        isTimerActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }, fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Wskaźnik postępu/stanu timera
        AnimatedVisibility(
            visible = isTimerActive, enter = slideInVertically(
                animationSpec = tween(300, easing = LinearEasing)
            ) + fadeIn(), exit = slideOutVertically(
                animationSpec = tween(300, easing = LinearEasing)
            ) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTimerPaused) MaterialTheme.colorScheme.secondaryContainer.copy(
                        alpha = 0.7f
                    )
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isTimerPaused) "Timer jest wstrzymany" else "Timer odlicza...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Pasek postępu wizualny
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                brush = if (isTimerPaused) {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    )
                                }, shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        // Animowana wskazówka dla nowych użytkowników
        AnimatedVisibility(
            visible = showSetupHint && !isTimerActive,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "💡 Przeciągnij palcem góra-dół aby ustawić czas\n(maksymalnie 24 godziny)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .alpha(0.8f)
                )
            }
        }

        // Animacja sukcesu przy uruchomieniu timera
        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 }) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Timer został uruchomiony!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Główna sekcja odliczania z elastycznym spacer
        Spacer(modifier = Modifier.weight(1f))

        // Sekcja odliczania timera z animacjami
        CountdownSection(
            timeRemaining = timerRemainingTime,
            isTimeUp = timerFinished,
            isTimerMode = true,
            isTimerPaused = isTimerPaused,
            onTimerMinutesChanged = { minutes ->
                timerMinutes = minutes
            },
            onTimerSet = { minutes ->
                onTimerSet(minutes)
            },
            timerMinutes = timerMinutes,
            onPauseTimer = onPauseTimer,
            onResumeTimer = onResumeTimer,
            onResetTimer = onResetTimer,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.weight(0.5f))
    }
}