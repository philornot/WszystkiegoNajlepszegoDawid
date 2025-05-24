package com.philornot.siekiera.ui.screens.main.gift

import android.widget.Toast
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.philornot.siekiera.ui.screens.main.effects.SparkleAnimation
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Przycisk prezentu z animacją iskier przy najechaniu, efektem pulsowania,
 * ulepszoną detekcją długiego naciśnięcia i ukrytym easter egg -
 * nieskończone powiększanie przy bardzo długim przytrzymaniu.
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
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

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
    var longPressStartTime by remember { mutableLongStateOf(0L) }

    // Easter egg state
    var isEasterEggActive by remember { mutableStateOf(false) }
    var easterEggScale by remember { mutableFloatStateOf(1f) }
    var easterEggToastShown by remember { mutableStateOf(false) }

    // Feedback haptyczny
    val haptic = LocalHapticFeedback.current

    // Animacja skali dla długiego naciśnięcia
    val longPressScale by animateFloatAsState(
        targetValue = if (isLongPressing && !isEasterEggActive) 1.15f else 1.0f,
        animationSpec = tween(500, easing = LinearEasing),
        label = "longPressScale"
    )

    // Animacja easter egg - nieskończone powiększanie
    val easterEggAnimatedScale by animateFloatAsState(
        targetValue = easterEggScale, animationSpec = if (isEasterEggActive) {
            tween(2000, easing = LinearEasing)
        } else {
            tween(1000, easing = LinearEasing) // Szybsza animacja powrotu (deflacja)
        }, label = "easterEggScale"
    )

    // Efekt drgania przy długim naciśnięciu
    val vibrationOffset by animateFloatAsState(
        targetValue = if (isLongPressing && !isEasterEggActive) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 300
                0f at 0
                -1f at 75
                1f at 150
                -1f at 225
                0f at 300
            }, repeatMode = RepeatMode.Restart
        ),
        label = "vibrationAnimation"
    )

    // Flaga do śledzenia czy długie naciśnięcie jest obsługiwane
    var isLongPressTriggered by remember { mutableStateOf(false) }

    // Efekt dla animacji długiego naciśnięcia i easter egg
    LaunchedEffect(isLongPressing) {
        if (isLongPressing && enableLongPress) {
            longPressProgress = 0f
            longPressStartTime = System.currentTimeMillis()

            // Standardowa animacja długiego naciśnięcia
            while (longPressProgress < 1f && isLongPressing && !isEasterEggActive) {
                delay(16) // ~60fps
                longPressProgress += 0.03f // Pełen postęp po około 0.5 sekundy

                // Wibracja po osiągnięciu połowy postępu
                if (longPressProgress >= 0.5f && longPressProgress < 0.53f) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                // Wywołaj callback po osiągnięciu pełnego postępu
                if (longPressProgress >= 1f) {
                    isLongPressTriggered = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                    break
                }
            }

            // Easter egg - jeśli długie naciśnięcie trwa dłużej niż 3 sekundy
            if (isLongPressing && !isLongPressTriggered) {
                val elapsed = System.currentTimeMillis() - longPressStartTime
                if (elapsed >= 3000 && !isEasterEggActive) {
                    Timber.d("Aktywuję easter egg - nieskończone powiększanie prezentu!")
                    isEasterEggActive = true
                    easterEggToastShown = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            // Resetuj flagę po krótkim opóźnieniu
            if (isLongPressTriggered) {
                delay(1000)
                isLongPressTriggered = false
            }
        }
    }

    // Easter egg logic - nieskończone powiększanie
    LaunchedEffect(isEasterEggActive) {
        if (isEasterEggActive && isLongPressing) {
            // Oblicz maksymalną skalę żeby wypełnić ekran
            val buttonSize = 220f // rozmiar kontenera w dp
            val maxScale = maxOf(screenWidth, screenHeight) / buttonSize * 1.2f // 120% ekranu

            // Stopniowo powiększaj przycisk
            while (isEasterEggActive && isLongPressing) {
                easterEggScale += 0.02f // Stopniowe powiększanie

                // Jeśli osiągnęliśmy maksymalną skalę i nie pokazano jeszcze toastu
                if (easterEggScale >= maxScale && !easterEggToastShown) {
                    easterEggToastShown = true
                    Toast.makeText(context, "już możesz puścić :)", Toast.LENGTH_SHORT).show()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Timber.d("Easter egg toast pokazany, skala: $easterEggScale")
                }

                delay(16) // ~60fps
            }
        }
    }

    // Efekt deflacji po puszczeniu w easter egg
    LaunchedEffect(isEasterEggActive, isLongPressing) {
        if (isEasterEggActive && !isLongPressing) {
            // Animacja deflacji - powrót do normalnego rozmiaru
            Timber.d("Deflacja balonu - powrót do normalnego rozmiaru")
            easterEggScale = 1f
            isEasterEggActive = false
            easterEggToastShown = false
        }
    }

    // Śledź pozycję tego komponentu w layoutcie
    val positionState = remember { mutableStateOf(Offset.Zero) }
    val sizeState = remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier.size(220.dp),  // Nieco większy kontener dla efektu
        contentAlignment = Alignment.Center
    ) {
        // Subtelny efekt iskrzenia wokół prezentu
        SparkleAnimation(
            modifier = Modifier.fillMaxSize()
        )

        // Przycisk prezentu z animacjami
        Card(
            colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ), shape = CircleShape, modifier = modifier
                .scale(
                    if (isEasterEggActive) {
                        easterEggAnimatedScale
                    } else {
                        pulseScale.value * longPressScale
                    }
                )
                .graphicsLayer(
                    transformOrigin = TransformOrigin.Center,
                    translationX = if (isLongPressing && !isEasterEggActive) vibrationOffset * 3f else 0f, // Efekt drgania
                )
                .clip(CircleShape)
                .pointerInput(enableLongPress) {
                    detectTapGestures(onPress = { offset ->
                        if (enableLongPress) {
                            isLongPressing = true
                            longPressStartTime = System.currentTimeMillis()
                            awaitRelease()
                            isLongPressing = false
                            longPressProgress = 0f
                        }
                    }, onTap = {
                        // Obsługuj kliknięcie tylko jeśli długie naciśnięcie nie zostało właśnie wywołane
                        // i jeśli nie jest aktywny easter egg
                        if (!isLongPressTriggered && !isEasterEggActive) {
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