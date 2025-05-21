package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay

/**
 * Przycisk prezentu z animacją iskier przy najechaniu, efektem pulsowania
 * i ulepszoną detekcją długiego naciśnięcia.
 *
 * @param enableLongPress Kontroluje czy funkcjonalność długiego
 *    naciśnięcia jest włączona
 */
@Composable
fun GiftButton(
    modifier: Modifier = Modifier,
    onClick: (centerX: Float, centerY: Float) -> Unit,
    onLongPress: () -> Unit = {},
    enableLongPress: Boolean = false,
) {
    // Animowany scale dla efektu pulsowania
    val pulseScale = animateFloatAsState(
        targetValue = 1.05f,  // Subtelne pulsowanie
        animationSpec = infiniteRepeatable(
            animation = tween(800), repeatMode = RepeatMode.Reverse
        ), label = "pulseAnimation"
    )

    // Stan długiego naciśnięcia
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressProgress by remember { mutableFloatStateOf(0f) }

    // Feedback haptyczny
    val haptic = LocalHapticFeedback.current

    // Animacja skali dla długiego naciśnięcia
    val longPressScale by animateFloatAsState(
        targetValue = if (isLongPressing) 1.15f else 1.0f,
        animationSpec = tween(500, easing = LinearEasing),
        label = "longPressScale"
    )

    // Efekt drgania przy długim naciśnięciu
    val vibrationOffset by animateFloatAsState(
        targetValue = if (isLongPressing) 1f else 0f, animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 300
                0f at 0
                -1f at 75
                1f at 150
                -1f at 225
                0f at 300
            }, repeatMode = RepeatMode.Restart
        ), label = "vibrationAnimation"
    )

    // Flaga do śledzenia czy długie naciśnięcie jest obsługiwane
    var isLongPressTriggered by remember { mutableStateOf(false) }

    // Efekt dla animacji długiego naciśnięcia
    LaunchedEffect(isLongPressing) {
        if (isLongPressing && enableLongPress) {
            longPressProgress = 0f
            // Animuj postęp długiego naciśnięcia
            while (longPressProgress < 1f && isLongPressing) {
                delay(16) // ~60fps
                longPressProgress += 0.03f // Pełen postęp po około 0.5 sekundy

                // Wibracja po osiągnięciu połowy postępu
                if (longPressProgress >= 0.5f && longPressProgress < 0.53f) {
                    haptic.performHapticFeedback(HapticFeedbackType.Companion.LongPress)
                }

                // Wywołaj callback po osiągnięciu pełnego postępu
                if (longPressProgress >= 1f) {
                    isLongPressTriggered = true
                    haptic.performHapticFeedback(HapticFeedbackType.Companion.LongPress)
                    onLongPress()
                    break
                }
            }

            // Resetuj flagę po krótkim opóźnieniu
            if (isLongPressTriggered) {
                delay(1000)
                isLongPressTriggered = false
            }
        }
    }

    // Śledź pozycję tego komponentu w layoutcie
    val positionState = remember { mutableStateOf(Offset.Companion.Zero) }
    val sizeState = remember { mutableStateOf(Size.Companion.Zero) }

    Box(
        modifier = Modifier.Companion.size(220.dp),  // Nieco większy kontener dla efektu
        contentAlignment = Alignment.Companion.Center
    ) {
        // Subtelny efekt iskrzenia wokół prezentu
        SparkleAnimation(
            modifier = Modifier.Companion.fillMaxSize()
        )

        // Przycisk prezentu z animacjami
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = CircleShape,
            modifier = modifier
                .scale(pulseScale.value * (if (isLongPressing) longPressScale else 1f))  // Zastosuj obie animacje skali
                .graphicsLayer(
                    transformOrigin = TransformOrigin.Companion.Center,
                    translationX = if (isLongPressing) vibrationOffset * 3f else 0f, // Efekt drgania
                )
                .clip(CircleShape)
                .pointerInput(enableLongPress) {
                    detectTapGestures(onPress = { offset ->
                        if (enableLongPress) {
                            isLongPressing = true
                            awaitRelease()
                            isLongPressing = false
                            longPressProgress = 0f
                        }
                    }, onTap = {
                        // Obsługuj kliknięcie tylko jeśli długie naciśnięcie nie zostało właśnie wywołane
                        if (!isLongPressTriggered) {
                            // Oblicz pozycję środka jako znormalizowane współrzędne (0.0 do 1.0)
                            val centerX =
                                (positionState.value.x + sizeState.value.width / 2) / (positionState.value.x + sizeState.value.width)
                            val centerY =
                                (positionState.value.y + sizeState.value.height / 2) / (positionState.value.y + sizeState.value.height + 1000f)

                            onClick(centerX, centerY)
                        }
                    })
                }
                .onGloballyPositioned { coordinates ->
                    // Zapisz pozycję i rozmiar komponentu przy każdej zmianie layoutu
                    positionState.value = coordinates.positionInRoot()
                    sizeState.value = coordinates.size.toSize()
                }
                .testTag("gift")) {
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Ikona prezentu
                Icon(
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = "Gift",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.Companion.size(80.dp)
                )
            }
        }
    }
}