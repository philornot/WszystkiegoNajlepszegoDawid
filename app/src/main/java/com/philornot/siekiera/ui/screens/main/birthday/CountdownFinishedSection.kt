package com.philornot.siekiera.ui.screens.main.birthday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Komponent wyświetlany po zakończeniu odliczania. Zawiera odpowiednie
 * komunikaty dla trybu urodzinowego i trybu timera.
 *
 * @param isTimerMode Czy aplikacja jest w trybie timera
 * @param modifier Modifier dla komponentu
 */
@Composable
fun CountdownFinishedSection(
    isTimerMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(16.dp)
            .testTag("countdown")
    ) {
        Text(
            text = if (isTimerMode) "CZAS MINĄŁ!" else "WOOO HOOO",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (isTimerMode) "Twój timer zakończył odliczanie"
            else "Udało ci się przeżyć 72 pory roku\n(i to POD RZĄD!!)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
    }
}