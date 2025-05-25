package com.philornot.siekiera.ui.screens.main.birthday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import timber.log.Timber

/**
 * Komponent odpowiedzialny za kontrolki timera i instrukcje dla
 * użytkownika. Zawiera przyciski start/pause/stop i wskazówki dotyczące
 * przeciągania.
 *
 * @param isTimerActive Czy timer jest aktywny
 * @param isTimerPaused Czy timer jest spauzowany
 * @param isTimeUp Czy czas się skończył
 * @param currentDragMinutes Aktualna wartość minut z przeciągania
 * @param onTimerSet Callback do uruchomienia timera
 * @param onPauseTimer Callback do pauzowania timera
 * @param onResumeTimer Callback do wznawiania timera
 * @param onResetTimer Callback do resetowania timera
 * @param modifier Modifier dla komponentu
 */
@Composable
fun TimerControlsSection(
    isTimerActive: Boolean,
    isTimerPaused: Boolean,
    isTimeUp: Boolean,
    currentDragMinutes: Int,
    onTimerSet: (Int) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onResetTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Kontrolki timera z animacjami przycisków
    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isTimerActive) {
            // Przycisk START
            Button(
                onClick = {
                    Timber.i("Użytkownik kliknął START, ustawianie timera na $currentDragMinutes minut")
                    onTimerSet(currentDragMinutes)
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ), modifier = Modifier.width(120.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start")
                }
            }
        } else {
            // Przyciski kontroli aktywnego timera
            if (isTimerPaused) {
                // Przycisk WZNÓW
                Button(
                    onClick = {
                        Timber.i("Użytkownik kliknął WZNÓW timer")
                        onResumeTimer()
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ), modifier = Modifier.width(120.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Wznów",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wznów")
                    }
                }
            } else {
                // Przycisk PAUZA
                Button(
                    onClick = {
                        Timber.i("Użytkownik kliknął PAUZA timer")
                        onPauseTimer()
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ), modifier = Modifier.width(120.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pauza",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pauza")
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Przycisk STOP
            Button(
                onClick = {
                    Timber.i("Użytkownik kliknął STOP timer")
                    onResetTimer()
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ), modifier = Modifier.width(120.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }
        }
    }

    // Instrukcja przeciągania - tylko gdy timer nieaktywny
    if (!isTimerActive) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Przeciągnij w górę lub w dół, aby zmienić czas\n(maksymalnie 24 godziny)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    // Przycisk resetowania timera po zakończeniu (dla trybu timera)
    if (isTimeUp) {
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                Timber.i("Użytkownik kliknął RESET po zakończeniu timera")
                onResetTimer()
            }, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset timera",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ustaw nowy timer")
            }
        }
    }
}