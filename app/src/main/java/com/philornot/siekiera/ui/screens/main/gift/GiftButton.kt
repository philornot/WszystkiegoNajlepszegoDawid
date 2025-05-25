package com.philornot.siekiera.ui.screens.main.gift

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.philornot.siekiera.ui.screens.main.effects.SparkleAnimation
import timber.log.Timber

/**
 * Przycisk prezentu z delikatną animacją pulsowania i prostą animacją
 * kliknięcia. Przytrzymanie wykonuje tę samą akcję co kliknięcie.
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
    // Animowany scale dla subtelnego efektu pulsowania
    val pulseScale = animateFloatAsState(
        targetValue = 1.03f,  // Bardzo subtelne pulsowanie
        animationSpec = infiniteRepeatable(
            animation = tween(1200), repeatMode = RepeatMode.Reverse
        ), label = "pulseAnimation"
    )

    // Stan animacji kliknięcia
    val clickAnimation = remember { Animatable(1f) }

    // Feedback haptyczny
    val haptic = LocalHapticFeedback.current

    // Śledź pozycję tego komponentu w layoutcie
    val positionState = remember { mutableStateOf(Offset.Zero) }
    val sizeState = remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center
    ) {
        // Subtelny efekt iskrzenia wokół prezentu
        SparkleAnimation(
            modifier = Modifier.fillMaxSize()
        )

        // Przycisk prezentu z animacjami
        Card(
            colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
            shape = CircleShape,
            modifier = modifier
                .scale(pulseScale.value * clickAnimation.value)
                .clip(CircleShape)
                .pointerInput(enableLongPress) {
                    detectTapGestures(onPress = {
                        // Animacja wciśnięcia
                        clickAnimation.snapTo(0.95f)

                        // Czekaj na zwolnienie
                        awaitRelease()

                        // Animacja powrotu
                        clickAnimation.animateTo(
                            targetValue = 1f, animationSpec = tween(150, easing = LinearEasing)
                        )
                    }, onTap = {
                        // Delikatna wibracja przy kliknięciu
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        // Oblicz pozycję środka jako znormalizowane współrzędne (0.0 do 1.0)
                        val centerX =
                            (positionState.value.x + sizeState.value.width / 2) / (positionState.value.x + sizeState.value.width)
                        val centerY =
                            (positionState.value.y + sizeState.value.height / 2) / (positionState.value.y + sizeState.value.height + 1000f)

                        onClick(centerX, centerY)
                        Timber.d("Prezent kliknięty")
                    }, onLongPress = {
                        // Długie naciśnięcie robi to samo co kliknięcie
                        if (enableLongPress) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            // Oblicz pozycję środka
                            val centerX =
                                (positionState.value.x + sizeState.value.width / 2) / (positionState.value.x + sizeState.value.width)
                            val centerY =
                                (positionState.value.y + sizeState.value.height / 2) / (positionState.value.y + sizeState.value.height + 1000f)

                            onClick(centerX, centerY)
                            Timber.d("Prezent długo naciśnięty - wykonuję tę samą akcję co kliknięcie")
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Ikona prezentu
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