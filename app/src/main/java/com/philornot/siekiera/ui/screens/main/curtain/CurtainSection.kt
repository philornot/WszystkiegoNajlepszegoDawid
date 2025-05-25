package com.philornot.siekiera.ui.screens.main.curtain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.screens.main.gift.GiftButton

/**
 * Sekcja z kurtyną i prezentem, obsługująca długie naciśnięcie dla
 * aktywacji trybu timera oraz specjalną obsługę dla GiftScreen.
 *
 * @param modifier Modifier dla kontenera
 * @param isTimeUp Czy czas upłynął
 * @param showGift Czy pokazać prezent (domyślnie true, false w trybie
 *    timera)
 * @param onGiftClicked Callback dla kliknięcia prezentu z pozycją
 * @param onGiftLongPressed Callback dla długiego naciśnięcia prezentu
 * @param giftReceived Czy prezent został odebrany, kontroluje działanie
 *    długiego naciśnięcia
 * @param isInGiftScreen Czy komponent jest używany w GiftScreen (wpływa na
 *    obsługę long press)
 */
@Composable
fun CurtainSection(
    modifier: Modifier = Modifier,
    isTimeUp: Boolean,
    showGift: Boolean = true,
    onGiftClicked: (centerX: Float, centerY: Float) -> Unit,
    onGiftLongPressed: () -> Unit = {},
    giftReceived: Boolean = false,
    isInGiftScreen: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("curtain_container"),
        contentAlignment = Alignment.Center
    ) {
        // Animacja kurtyny
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

        // Animacja prezentu - teraz z uwzględnieniem parametru showGift
        AnimatedVisibility(
            visible = isTimeUp && showGift, enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 800, delayMillis = 200, easing = LinearEasing
                )
            ), modifier = Modifier.testTag("gift_container")
        ) {
            GiftButton(
                modifier = Modifier.size(200.dp), onClick = { centerX, centerY ->
                onGiftClicked(centerX, centerY)
            }, onLongPress = {
                if (isInGiftScreen) {
                    // W GiftScreen long press robi to samo co kliknięcie
                    onGiftClicked(0.5f, 0.5f) // Używamy domyślnych wartości centrum
                } else {
                    // W normalnym trybie aktywuj długie naciśnięcie tylko jeśli prezent został odebrany
                    if (giftReceived) {
                        onGiftLongPressed()
                    }
                }
            }, enableLongPress = isInGiftScreen || giftReceived
            )
        }
    }
}
