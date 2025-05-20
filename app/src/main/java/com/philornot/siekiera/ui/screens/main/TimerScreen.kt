package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Ekran timera, który pojawia się po długim naciśnięciu prezentu.
 * Pozwala na ustawienie czasu poprzez przeciąganie w górę i w dół.
 *
 * @param visible Czy ekran jest widoczny
 * @param onBackPressed Callback wywoływany po naciśnięciu przycisku powrotu
 * @param onTimerSet Callback wywoływany po ustawieniu timera, przekazuje czas w minutach
 */
@Composable
fun TimerScreen(
    visible: Boolean,
    onBackPressed: () -> Unit,
    onTimerSet: (Int) -> Unit
) {
    // Configuracja ekranu
    val screenHeight = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    // Stan timera
    var minutes by remember { mutableIntStateOf(5) } // Domyślnie 5 minut
    var sliderValue by remember { mutableFloatStateOf(minutes.toFloat() / 60f) }
    var isDragging by remember { mutableStateOf(false) }
    var initialDragY by remember { mutableStateOf(0f) }

    // Animacja pulsowania zegara
    val animatedScale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "pulseAnimation"
    )

    // Resetuj timer przy pokazaniu ekranu
    LaunchedEffect(visible) {
        if (visible) {
            minutes = 5
            sliderValue = minutes.toFloat() / 60f
            isDragging = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                    )
            ) {
                // Timer content
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Powrót",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Title
                        Text(
                            text = "Lawendowy Timer",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Placeholder for symmetry
                        Box(modifier = Modifier.size(48.dp))
                    }

                    // Timer clock face
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .size(280.dp * animatedScale)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        initialDragY = offset.y
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        // Convert drag to minutes (negative drag = increase)
                                        val dragSensitivity = 0.5f // Adjust sensitivity
                                        val minuteChange = -dragAmount * dragSensitivity

                                        // Update minutes with bounds
                                        minutes = (minutes + minuteChange.toInt())
                                            .coerceIn(1, 60) // 1 min to 60 min

                                        // Update slider for visual feedback
                                        sliderValue = minutes.toFloat() / 60f

                                        change.consume()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Clock face background
                        CircularProgressIndicator(
                            progress = sliderValue,
                            modifier = Modifier.size(280.dp * animatedScale),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 16.dp
                        )

                        // Time display
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$minutes",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 72.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "minut",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Drag instructions
                    Text(
                        text = "Przeciągnij w górę lub w dół aby ustawić czas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    // Slider for fine adjustment
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            minutes = (it * 60).toInt().coerceIn(1, 60)
                        },
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    // Set timer button
                    FloatingActionButton(
                        onClick = { onTimerSet(minutes) },
                        modifier = Modifier
                            .padding(32.dp)
                            .size(72.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Ustaw timer",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Bottom spacer for better layout
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Okrągły wskaźnik postępu, wizualizujący wybrany czas.
 */
@Composable
fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 8.dp
) {
    val stroke = with(LocalDensity.current) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    }

    Canvas(modifier = modifier) {
        val diameter = min(size.width, size.height)
        val radius = diameter / 2f
        val innerRadius = radius - stroke.width / 2

        // Draw track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(size.width / 2 - innerRadius, size.height / 2 - innerRadius),
            size = Size(innerRadius * 2, innerRadius * 2),
            style = stroke
        )

        // Draw progress
        drawArc(
            color = indicatorColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(size.width / 2 - innerRadius, size.height / 2 - innerRadius),
            size = Size(innerRadius * 2, innerRadius * 2),
            style = stroke
        )
    }
}