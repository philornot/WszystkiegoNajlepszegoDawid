package com.philornot.siekiera.ui.screens.main.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Maksymalnie uproszczony komponent licznika timera. Tylko podstawowa
 * karta z animowanymi cyframi.
 *
 * @param modifier Modifier dla komponentu
 * @param minutes Liczba minut do wyświetlenia
 * @param isDragging Czy użytkownik przeciąga wartość (nieużywane w
 *    uproszczonej wersji)
 * @param isActive Czy timer jest aktywny (nieużywane w uproszczonej
 *    wersji)
 * @param isPaused Czy timer jest spauzowany (nieużywane w uproszczonej
 *    wersji)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimerDaysCounter(
    modifier: Modifier = Modifier,
    minutes: Int,
    isDragging: Boolean = false,
    isActive: Boolean = false,
    isPaused: Boolean = false,
) {
    // Obliczenie godzin i minut
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    Card(
        modifier = modifier.size(width = 200.dp, height = 140.dp), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (hours > 0) {
                // Pokazuj godziny gdy timer > 60 minut
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Godziny
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(
                            targetState = hours, transitionSpec = {
                                slideInVertically { height -> -height } + fadeIn() togetherWith slideOutVertically { height -> height } + fadeOut()
                            }, label = "hours_animation"
                        ) { animatedHours ->
                            Text(
                                text = animatedHours.toString(),
                                style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "h",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minuty
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(
                            targetState = remainingMinutes, transitionSpec = {
                                slideInVertically { height -> -height } + fadeIn() togetherWith slideOutVertically { height -> height } + fadeOut()
                            }, label = "minutes_animation"
                        ) { animatedMinutes ->
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", animatedMinutes),
                                style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "min",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Tylko minuty gdy timer < 60 minut
                AnimatedContent(
                    targetState = minutes, transitionSpec = {
                        slideInVertically { height -> if (targetState > initialState) -height else height } + fadeIn() togetherWith slideOutVertically { height -> if (targetState > initialState) height else -height } + fadeOut()
                    }, label = "minutes_only_animation"
                ) { animatedMinutes ->
                    Text(
                        text = animatedMinutes.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "minut",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}