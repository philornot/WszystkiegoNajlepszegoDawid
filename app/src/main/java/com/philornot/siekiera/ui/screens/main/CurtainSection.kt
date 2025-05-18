package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Redesigned curtain and gift section with a clean, lavender theme.
 *
 * @param modifier Modifier for the container
 * @param isTimeUp Whether the time is up
 * @param onGiftClicked Callback for when the gift is clicked
 */
@Composable
fun CurtainSection(
    modifier: Modifier = Modifier,
    isTimeUp: Boolean,
    onGiftClicked: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("curtain_container"),
        contentAlignment = Alignment.Center
    ) {
        // Clean curtain animation
        AnimatedVisibility(
            visible = !isTimeUp, enter = fadeIn(), exit = fadeOut(
            animationSpec = tween(
                durationMillis = 1000, easing = LinearEasing
            )
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 1000, easing = LinearEasing
            ), targetOffsetY = { it }), modifier = Modifier.testTag("curtain")) {
            Curtain(modifier = Modifier.fillMaxSize())
        }

        // Clean gift animation
        AnimatedVisibility(
            visible = isTimeUp, enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 800, delayMillis = 200, easing = LinearEasing
                )
            ), modifier = Modifier.testTag("gift_container")
        ) {
            Gift(
                modifier = Modifier.size(200.dp), onClick = onGiftClicked
            )
        }
    }
}

/** Simple, elegant curtain design with lavender theme. */
@Composable
fun Curtain(modifier: Modifier = Modifier) {
    // Create a subtle lavender gradient for the curtain
    val curtainGradient = Brush.verticalGradient(
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
        // Vertical divider in the center
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.005f)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                    .align(Alignment.Center)
            )
        }
    }
}

/** Simple, elegant gift design with lavender theme. */
@Composable
fun Gift(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = CircleShape,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("gift")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Gift icon
            Icon(
                imageVector = Icons.Outlined.CardGiftcard,
                contentDescription = "Gift",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            )

            // Text
            Text(
                text = "Odbierz swój prezent!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}