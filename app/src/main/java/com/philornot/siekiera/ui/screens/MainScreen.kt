package com.philornot.siekiera.ui.screens

// Jawny import standardowej wersji AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philornot.siekiera.R
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    targetDate: Long,
    currentTime: Long = System.currentTimeMillis(),
    onGiftClicked: () -> Unit,
) {
    // Oblicz czy czas już minął
    var isTimeUp by remember { mutableStateOf(currentTime >= targetDate) }

    // Stan dla aktualnego czasu (będzie aktualizowany co sekundę)
    var currentTimeState by remember { mutableStateOf(currentTime) }
    var timeRemaining by remember { mutableStateOf((targetDate - currentTimeState).coerceAtLeast(0)) }

    // Aktualizuj czas co sekundę
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeState = System.currentTimeMillis()
            timeRemaining = (targetDate - currentTimeState).coerceAtLeast(0)

            // Sprawdź czy czas minął
            if (currentTimeState >= targetDate && !isTimeUp) {
                isTimeUp = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Tytuł na górze
        Text(
            text = stringResource(id = R.string.app_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 24.dp),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Centralny element - kurtyna lub prezent
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            // Prezent (z animacją pojawienia się)
            androidx.compose.animation.AnimatedVisibility(
                visible = isTimeUp, enter = fadeIn(
                    animationSpec = tween(durationMillis = 1500, delayMillis = 500)
                ), modifier = Modifier.testTag("gift_container")
            ) {
                Icon(
                    imageVector = Icons.Filled.CardGiftcard,
                    contentDescription = stringResource(id = R.string.gift_description),
                    modifier = Modifier
                        .size(250.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable { onGiftClicked() }
                        .testTag("gift"),
                    tint = MaterialTheme.colorScheme.primary)
            }

            // Kurtyna (z animacją znikania)
            androidx.compose.animation.AnimatedVisibility(
                visible = !isTimeUp, exit = fadeOut(
                    animationSpec = tween(durationMillis = 2000)
                ), modifier = Modifier.testTag("curtain_container")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.tertiary)
                        .testTag("curtain")
                ) {
                    // Rysuj kurtynę za pomocą Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val width = size.width
                        val height = size.height

                        // Linia podziału kurtyny
                        drawLine(
                            color = Color.White.copy(alpha = 0.3f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, height),
                            strokeWidth = 5f
                        )

                        // Fałdy kurtyny - lewa strona
                        for (i in 1..5) {
                            val x = (centerX / 5) * i
                            val path = Path().apply {
                                moveTo(x, 0f)
                                cubicTo(
                                    x - 20f, height / 3, x + 20f, 2 * height / 3, x, height
                                )
                            }
                            drawPath(
                                path, Color.White.copy(alpha = 0.1f), style = Stroke(width = 2f)
                            )
                        }

                        // Fałdy kurtyny - prawa strona
                        for (i in 1..5) {
                            val x = centerX + ((width - centerX) / 5) * i
                            val path = Path().apply {
                                moveTo(x, 0f)
                                cubicTo(
                                    x - 20f, height / 3, x + 20f, 2 * height / 3, x, height
                                )
                            }
                            drawPath(
                                path, Color.White.copy(alpha = 0.1f), style = Stroke(width = 2f)
                            )
                        }
                    }
                }
            }
        }

        // Timer na dole (widoczny tylko przed upływem czasu)
        androidx.compose.animation.AnimatedVisibility(
            visible = !isTimeUp, exit = fadeOut(
                animationSpec = tween(durationMillis = 500)
            ), modifier = Modifier.testTag("countdown_container")
        ) {
            CountdownTimer(
                timeRemaining = timeRemaining,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .fillMaxWidth()
                    .testTag("countdown")
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CountdownTimer(
    timeRemaining: Long,
    modifier: Modifier = Modifier,
) {
    val formattedTime = TimeUtils.formatRemainingTime(timeRemaining)
    val (days, time) = formattedTime.split(", ")

    // Rozbij czas na komponenty dla animacji
    val hoursPart = time.substring(0, 2)
    val minutesPart = time.substring(3, 5)
    val secondsPart = time.substring(6, 8)

    Column(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.countdown_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Dni z animacją
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
        ) {
            AnimatedCounter(text = days)
        }

        // Czas (godziny:minuty:sekundy) z animacją
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
        ) {
            AnimatedCounter(text = hoursPart)
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            AnimatedCounter(text = minutesPart)
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            AnimatedCounter(text = secondsPart)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedCounter(text: String) {
    Row {
        text.forEach { char ->
            AnimatedContent(
                targetState = char, transitionSpec = {
                    // Losowa animacja przy zmianie - raz wsuwa z góry, raz z dołu
                    if (Random.nextBoolean()) {
                        slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
                    } else {
                        slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                    }
                }) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}