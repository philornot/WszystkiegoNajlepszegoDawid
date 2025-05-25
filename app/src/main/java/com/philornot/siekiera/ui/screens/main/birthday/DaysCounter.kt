package com.philornot.siekiera.ui.screens.main.birthday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Licznik dni z czystym designem */
@Composable
fun DaysCounter(
    modifier: Modifier = Modifier,
    days: String,
) {
    val dayText = days.split(" ")[0]
    val daysLabel = days.replace(dayText, "").trim()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.Companion.padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            // Liczba dni z wycentrowanym tekstem
            Text(
                text = dayText,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Companion.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion.padding(bottom = 8.dp)
            )

            // Etykieta dni z wycentrowanym tekstem
            Text(
                text = daysLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Companion.Center
            )
        }
    }
}