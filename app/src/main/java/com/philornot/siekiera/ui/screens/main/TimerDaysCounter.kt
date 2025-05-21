package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Timer licznik dni/minut. Pokazuje minuty w trybie timera. */
@Composable
fun TimerDaysCounter(
    modifier: Modifier = Modifier.Companion,
    minutes: Int,
    isDragging: Boolean = false,
) {
    // Animacja pulsowania podczas przeciągania
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1.0f, label = "drag_scale"
    )

    Card(
        modifier = modifier.size(width = 160.dp, height = 160.dp * scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            // Liczba minut z wycentrowanym tekstem
            Text(
                text = minutes.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Companion.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion.padding(bottom = 8.dp)
            )

            // Etykieta minut z wycentrowanym tekstem
            Text(
                text = "minut",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Companion.Center
            )
        }
    }
}