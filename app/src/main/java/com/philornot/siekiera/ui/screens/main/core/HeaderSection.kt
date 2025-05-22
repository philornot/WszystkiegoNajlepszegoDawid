package com.philornot.siekiera.ui.screens.main.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Clean, lavender-themed header section with proper spacing to avoid collision
 * with navigation drawer hamburger menu icon.
 */
@Composable
fun HeaderSection(
    modifier: Modifier = Modifier,
    hasDrawer: Boolean = false // Parametr informujący czy drawer jest dostępny
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (hasDrawer) 24.dp else 16.dp, // Subtelnie większy top padding gdy drawer jest aktywny
                bottom = 8.dp,
                start = if (hasDrawer) 32.dp else 16.dp, // Trochę więcej miejsca dla ikony menu
                end = 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Title text
            Text(
                text = "Wszystkiego najlepszego",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            // Name with emphasis
            Text(
                text = "Dawid!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}