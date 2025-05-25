package com.philornot.siekiera.ui.screens.main.navigation

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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.screens.main.effects.ExplosiveFireworksDisplay
import com.philornot.siekiera.ui.screens.main.effects.ConfettiExplosionEffect

/**
 * Komponent odpowiedzialny za efekty wizualne i dialogi w MainScreen.
 * Zawiera fajerwerki, konfetti i dialogi overlay.
 */
@Composable
fun MainScreenOverlays(
    modifier: Modifier = Modifier,
    isTimeUp: Boolean,
    isTimerMode: Boolean,
    showCelebration: Boolean,
    showConfettiExplosion: Boolean,
    confettiCenterX: Float,
    confettiCenterY: Float,
    currentSection: NavigationSection,
    showProgressDialog: Boolean,
    progressValue: Float,
) {
    Box(modifier = modifier.fillMaxSize()) {
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

        // Dialog postępu zmiany nazwy aplikacji
        if (showProgressDialog) {
            ProgressDialog(progressValue = progressValue)
        }
    }
}

/** Dialog postępu dla zmiany nazwy aplikacji */
@Composable
private fun ProgressDialog(progressValue: Float) {
    AlertDialog(
        onDismissRequest = { /* Nie można zamknąć podczas postępu */ },
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
        confirmButton = { /* Brak przycisku podczas postępu */ })
}