package com.philornot.siekiera.ui.screens.main.timer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.screens.main.countdown.CountdownSection

/**
 * Ekran timera dostępny z szufladki nawigacyjnej. Pozwala na ustawienie
 * własnego timera niezależnie od odliczania urodzin.
 *
 * @param timerRemainingTime Pozostały czas timera w milisekundach
 * @param timerFinished Czy timer zakończył odliczanie
 * @param onTimerSet Callback wywoływany po ustawieniu timera
 * @param onResetTimer Callback wywoływany po resetowaniu timera
 * @param modifier Modyfikator dla całego ekranu
 */
@Composable
fun TimerScreen(
    timerRemainingTime: Long,
    timerFinished: Boolean,
    onTimerSet: (Int) -> Unit,
    onResetTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lokalne stany dla ekranu timera
    var timerMinutes by remember { mutableStateOf(5) }
    var changeAppName by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nagłówek dla trybu timera
        Text(
            text = if (timerRemainingTime > 0) "Timer aktywny" else "Tryb Timera",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        // Spacer zamiast kurtyny (lub można dodać jakiś element wizualny)
        Spacer(modifier = Modifier.weight(1f))

        // Sekcja odliczania timera
        CountdownSection(
            timeRemaining = timerRemainingTime,
            isTimeUp = timerFinished,
            isTimerMode = true,
            onTimerMinutesChanged = { minutes ->
                timerMinutes = minutes
            },
            onTimerSet = { minutes ->
                onTimerSet(minutes)
            },
            timerMinutes = timerMinutes,
            changeAppName = changeAppName,
            onChangeAppNameChanged = { checked ->
                changeAppName = checked
            },
            onResetTimer = onResetTimer,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}