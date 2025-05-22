package com.philornot.siekiera.ui.screens.main.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Przełącznik do wyboru trybu aplikacji - odliczanie do urodzin lub timer.
 *
 * @param isTimerMode Czy tryb timera jest aktywny
 * @param onModeChanged Callback wywoływany przy zmianie trybu
 * @param modifier Modifier dla komponentu
 */
@Composable
fun ModeToggle(
    isTimerMode: Boolean,
    onModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Animacja pozycji kursora

    // Kolory tła dla poszczególnych trybów
    val birthdayBgColor by animateColorAsState(
        targetValue = if (!isTimerMode) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), label = "birthdayBgColor"
    )

    val timerBgColor by animateColorAsState(
        targetValue = if (isTimerMode) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), label = "timerBgColor"
    )

    // Powierzchnia zawierająca przełącznik
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            ), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Przyciski
            Row(
                modifier = Modifier.width(160.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Przycisk trybu urodzinowego
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(birthdayBgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Brak efektu ripple - mamy własną animację
                        ) { onModeChanged(false) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = "Tryb urodzinowy",
                        tint = if (!isTimerMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Przycisk trybu timera
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(timerBgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // Brak efektu ripple - mamy własną animację
                        ) { onModeChanged(true) }
                        .padding(8.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Tryb timera",
                        tint = if (isTimerMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}