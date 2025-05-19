package com.philornot.siekiera.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

/**
 * A simple, subtle background component with lavender theme.
 *
 * @param isTimeUp Whether the time is up, affecting the background style
 * @param modifier Modifier for the container
 */
@Composable
fun AppBackground(
    isTimeUp: Boolean,
    modifier: Modifier = Modifier,
) {
    // Choose different gradients based on whether time is up
    val backgroundGradient = if (isTimeUp) {
        // Celebration background
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        // Waiting background
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        )
    }

    // Simple gradient background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    )
}