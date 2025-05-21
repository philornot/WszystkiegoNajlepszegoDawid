package com.philornot.siekiera.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Prosty, elegancki design kurtyny z lawendowym motywem. */
@Composable
fun Curtain(modifier: Modifier = Modifier) {
    // Utwórz subtelny lawendowy gradient dla kurtyny
    val curtainGradient = Brush.Companion.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(curtainGradient)
    ) {
        // Pionowy separator na środku
        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Companion.Center
        ) {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize(0.005f)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                    .align(Alignment.Companion.Center)
            )
        }
    }
}