package com.philornot.siekiera.ui.screens.main.timer

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.screens.main.birthday.CountdownSection
import kotlin.math.roundToInt

/**
 * Maksymalnie uproszczona wersja ekranu timera. Tylko nagłówek, odliczanie
 * i podstawowe przyciski. Dodano przeciągany informacyjny tekst o
 * aktywności timera.
 *
 * @param modifier Modyfikator dla całego ekranu
 * @param timerRemainingTime Pozostały czas timera w milisekundach
 * @param timerFinished Czy timer zakończył odliczanie
 * @param isTimerPaused Czy timer jest spauzowany
 * @param onTimerSet Callback wywoływany po ustawieniu timera
 * @param onPauseTimer Callback wywoływany po spauzowaniu timera
 * @param onResumeTimer Callback wywoływany po wznowieniu timera
 * @param onResetTimer Callback wywoływany po resetowaniu timera
 */
@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    timerRemainingTime: Long,
    timerFinished: Boolean,
    isTimerPaused: Boolean = false,
    onTimerSet: (Int) -> Unit,
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onResetTimer: () -> Unit,
) {
    // Lokalne stany dla ekranu timera
    var timerMinutes by remember { mutableIntStateOf(5) }

    // Sprawdź czy timer jest aktywny (ma pozostały czas)
    val isTimerActive = timerRemainingTime > 0

    // Stan do przechowywania pozycji przeciąganego tekstu
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Główna zawartość w kolumnie
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Stały nagłówek
            Text(
                text = "Tryb timera",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // Sekcja odliczania timera
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
                modifier = Modifier.weight(1f)
            )
        }

        // Informacyjny tekst gdy timer jest aktywny
        if (isTimerActive) {
            Text(
                text = "nie udało mi się ostatecznie uruchomić animacji cyferek wraz z momentem rozpoczęcia timera\n\n" + "jeśli jest napisane \"timer aktywny\", to timer jest aktywny, trust me",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset {
                        IntOffset(offsetX.roundToInt(), offsetY.roundToInt())
                    }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    })
        }
    }
}