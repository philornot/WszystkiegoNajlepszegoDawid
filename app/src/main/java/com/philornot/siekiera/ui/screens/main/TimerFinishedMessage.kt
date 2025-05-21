package com.philornot.siekiera.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Ekran wyświetlany po zakończeniu timera. */
@Composable
fun TimerFinishedMessage(
    minutes: Int,
    modifier: Modifier = Modifier.Companion,
    onBackClick: () -> Unit = {},
    onResetTimer: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        modifier = modifier.padding(24.dp)
    ) {
        // Zawartość wiadomości
        Box(
            modifier = Modifier.Companion.weight(1f), contentAlignment = Alignment.Companion.Center
        ) {
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {
                // Duży tytuł
                Text(
                    text = "Czas minął!",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.Companion.padding(bottom = 32.dp)
                )

                // Opis timera
                Text(
                    text = "Twój timer na $minutes minut zakończył odliczanie.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.Companion.padding(bottom = 16.dp),
                    textAlign = TextAlign.Companion.Center
                )

                // Przycisk resetowania timera
                Button(
                    onClick = onResetTimer,
                    modifier = Modifier.Companion.padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.Companion.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Ustaw nowy timer",
                            modifier = Modifier.Companion.size(18.dp)
                        )
                        Spacer(modifier = Modifier.Companion.width(8.dp))
                        Text("Ustaw nowy timer")
                    }
                }

                // Elastyczny odstęp dla lepszego układu
                Spacer(modifier = Modifier.Companion.weight(1f))

                // Przycisk powrotu ze strzałką
                IconButton(
                    onClick = onBackClick, modifier = Modifier.Companion
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Wróć",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.Companion.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.Companion.height(8.dp))

                Text(
                    text = "Wróć do trybu odliczania",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Companion.Center
                )
            }
        }
    }
}