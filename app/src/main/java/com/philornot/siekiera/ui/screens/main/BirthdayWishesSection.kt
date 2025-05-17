package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philornot.siekiera.ui.theme.AccentPink
import com.philornot.siekiera.ui.theme.PurpleDark
import com.philornot.siekiera.ui.theme.PurpleLight
import com.philornot.siekiera.ui.theme.PurplePrimary
import kotlinx.coroutines.delay

/**
 * Ekran wyświetlany po ujawnieniu prezentu, zawierający życzenia urodzinowe
 * i specjalne wiadomości.
 *
 * @param isVisible czy ekran powinien być widoczny
 * @param modifier modyfikator dla kontenera
 */
@Composable
fun BirthdayWishesSection(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
) {
    // Stan animacji poszczególnych elementów
    var showTitle by remember { mutableStateOf(false) }
    var showWishes by remember { mutableStateOf(false) }
    var showCards by remember { mutableStateOf(false) }

    // Sekwencja animacji
    LaunchedEffect(isVisible) {
        if (isVisible) {
            showTitle = true
            delay(500)
            showWishes = true
            delay(500)
            showCards = true
        } else {
            showCards = false
            showWishes = false
            showTitle = false
        }
    }

    // Główny kontener
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 800,
                easing = androidx.compose.animation.core.LinearEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 500,
                easing = androidx.compose.animation.core.LinearEasing
            )
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Tło z dekoracją cyfry 18
            Number18Decoration(isVisible = true)

            // Zawartość życzeń
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                // Tytuł
                AnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn(
                        animationSpec = tween(700)
                    ) + slideInVertically(
                        animationSpec = tween(700),
                        initialOffsetY = { -100 }
                    ),
                    exit = fadeOut() + slideOutVertically(
                        targetOffsetY = { -100 }
                    )
                ) {
                    GradientTitle(
                        text = "18 Lat!",
                        fontSize = 48,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                // Życzenia
                AnimatedVisibility(
                    visible = showWishes,
                    enter = fadeIn(
                        animationSpec = tween(1000)
                    ) + slideInVertically(
                        animationSpec = tween(800),
                        initialOffsetY = { 100 }
                    ),
                    exit = fadeOut() + slideOutVertically(
                        targetOffsetY = { 100 }
                    )
                ) {
                    BirthdayMessage(
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }

                // Karty z ikonami
                AnimatedVisibility(
                    visible = showCards,
                    enter = fadeIn(
                        animationSpec = tween(1000)
                    ),
                    exit = fadeOut()
                ) {
                    BirthdayIconsRow()
                }
            }
        }
    }
}

/**
 * Komponent wyświetlający główne życzenia urodzinowe.
 */
@Composable
fun BirthdayMessage(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Osiemnastka to wyjątkowy moment w życiu każdego człowieka. ")

                    withStyle(
                        style = SpanStyle(
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Dawid")
                    }

                    append(", z okazji Twoich ")

                    withStyle(
                        style = SpanStyle(
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    ) {
                        append("18")
                    }

                    append(". urodzin, życzę Ci wszystkiego najlepszego! Niech każdy kolejny rok przynosi Ci coraz więcej szczęścia i spełnienia.")
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Twoje marzenia niech się spełniają, a plany realizują. Ciesz się w pełni dorosłością i wszystkimi możliwościami, które przed Tobą stoją!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

/**
 * Komponent z ikonami urodzinowymi.
 */
@Composable
fun BirthdayIconsRow(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        BirthdayIconCard(
            icon = Icons.Default.Cake,
            title = "Tort",
            description = "Na słodko i radośnie"
        )

        BirthdayIconCard(
            icon = Icons.Default.Celebration,
            title = "Impreza",
            description = "Zabawa do rana"
        )

        BirthdayIconCard(
            icon = Icons.Default.EmojiEmotions,
            title = "Szczęście",
            description = "W każdym dniu"
        )

        BirthdayIconCard(
            icon = Icons.Default.Redeem,
            title = "Prezenty",
            description = "Wyjątkowe jak Ty"
        )
    }
}

/**
 * Pojedyncza karta z ikoną urodzinową.
 */
@Composable
fun BirthdayIconCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(8.dp)
    ) {
        // Ikona
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PurpleLight.copy(alpha = 0.9f),
                            PurplePrimary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tytuł
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PurplePrimary
        )

        // Opis
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = PurpleDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(100.dp)
        )
    }
}

/**
 * Tytuł z efektem gradientu.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun GradientTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 32,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            PurplePrimary,
            PurpleLight,
            AccentPink,
            PurpleLight,
            PurplePrimary
        )
    )

    androidx.compose.foundation.text.BasicText(
        text = text,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            brush = gradient
        ),
        modifier = modifier
    )
}