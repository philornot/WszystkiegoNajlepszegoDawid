package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

/**
 * Redesigned curtain and gift section with a clean, lavender theme.
 *
 * @param modifier Modifier for the container
 * @param isTimeUp Whether the time is up
 * @param onGiftClicked Callback for when the gift is clicked with position
 */
@Composable
fun CurtainSection(
    modifier: Modifier = Modifier,
    isTimeUp: Boolean,
    onGiftClicked: (centerX: Float, centerY: Float) -> Unit,
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
                ), targetOffsetY = { it }), modifier = Modifier.testTag("curtain")
        ) {
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
            GiftButton(
                modifier = Modifier.size(200.dp), onClick = { centerX, centerY ->
                    onGiftClicked(centerX, centerY)
                })
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

/** Enhanced gift button with sparkle animation on hover and pulse effect */
@Composable
fun GiftButton(
    modifier: Modifier = Modifier,
    onClick: (centerX: Float, centerY: Float) -> Unit,
) {
    // Animated scale for a pulse effect
    val pulseScale = animateFloatAsState(
        targetValue = 1.05f,  // Subtle pulse
        animationSpec = infiniteRepeatable(
            animation = tween(800), repeatMode = RepeatMode.Reverse
        ), label = "pulseAnimation"
    )

    // Keep track of this component's position in the layout
    val positionState = remember { mutableStateOf(Offset.Zero) }
    val sizeState = remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier.size(220.dp),  // Slightly larger container for the effect
        contentAlignment = Alignment.Center
    ) {
        // Subtle sparkle effect around the gift
        SparkleAnimation(
            modifier = Modifier.fillMaxSize()
        )

        // Gift button with pulse animation
        Card(
            colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
            shape = CircleShape,
            modifier = modifier
                .scale(pulseScale.value)  // Apply pulse animation
                .clip(CircleShape)
                .clickable {
                    // Calculate center position as normalized coordinates (0.0 to 1.0)
                    val centerX =
                        (positionState.value.x + sizeState.value.width / 2) / (positionState.value.x + sizeState.value.width)
                    val centerY =
                        (positionState.value.y + sizeState.value.height / 2) / (positionState.value.y + sizeState.value.height + 1000f) // Adding padding to account for screen height

                    // Call the onClick handler with the center position of this component
                    onClick(centerX, centerY)
                }
                .onGloballyPositioned { coordinates ->
                    // Save the component position and size whenever layout changes
                    positionState.value = coordinates.positionInRoot()
                    sizeState.value = coordinates.size.toSize()
                }
                .testTag("gift")) {
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
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}