package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philornot.siekiera.ui.theme.AccentPink
import com.philornot.siekiera.ui.theme.PurpleDark
import com.philornot.siekiera.ui.theme.PurpleLight
import com.philornot.siekiera.ui.theme.PurplePrimary

/** Sekcja nagłówka z tytułem aplikacji. */
@Composable
fun HeaderSection(
    modifier: Modifier = Modifier,
) {
    // Animacja dla dekoracji
    val infiniteTransition = rememberInfiniteTransition(label = "headerTransition")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f, animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Reverse
        ), label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Tytuł z efektem gradientu
        GradientTitle(
            text = "Wszystkiego najlepszego\nDawid!", modifier = Modifier.padding(vertical = 8.dp)
        )

        // Górna dekoracja
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(3.dp)
                .alpha(shimmerAlpha)
                .shadow(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PurplePrimary,
                            PurpleLight,
                            PurplePrimary,
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        // Dolna dekoracja
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(3.dp)
                .alpha(shimmerAlpha)
                .shadow(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            PurplePrimary,
                            AccentPink,
                            PurplePrimary,
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.BottomCenter)
        )
    }
}

/** Tytuł z gradientowym efektem. */
@OptIn(ExperimentalTextApi::class)
@Composable
fun GradientTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    // Animacja przesunięcia gradientu
    val infiniteTransition = rememberInfiniteTransition(label = "gradientTransition")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Restart
        ), label = "gradientOffset"
    )

    // Kolory gradientu
    val gradientColors = listOf(
        PurplePrimary, PurpleLight, AccentPink, PurpleLight, PurplePrimary
    )

    // Brush dla tekstu
    val brush = Brush.linearGradient(
        colors = gradientColors, start = androidx.compose.ui.geometry.Offset(
            gradientOffset * 1000f, 0f
        ), end = androidx.compose.ui.geometry.Offset(
            gradientOffset * 1000f + 1000f, 500f
        )
    )

    // Tytuł z gradientem i złożonym stylem
    androidx.compose.foundation.text.BasicText(
        text = buildAnnotatedString {
            val parts = text.split("\n")

            // Pierwsza linia
            if (parts.isNotEmpty()) {
                withStyle(
                    style = SpanStyle(
                        brush = brush, fontWeight = FontWeight.Bold, fontSize = 32.sp
                    )
                ) {
                    append(parts[0])
                }
            }

            // Druga linia
            if (parts.size > 1) {
                append("\n")
                withStyle(
                    style = SpanStyle(
                        brush = brush, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp
                    )
                ) {
                    append(parts[1])
                }
            }
        },
        style = androidx.compose.ui.text.TextStyle(
            textAlign = TextAlign.Center,
            lineHeight = 44.sp
        ),
        modifier = modifier
    )

    // Dekoracyjne elementy pod tytułem
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerDot()
        ShimmerLine(offset = gradientOffset)
        ShimmerDot(size = 6.dp)
        ShimmerLine(offset = gradientOffset)
        ShimmerDot()
    }
}

/** Dekoracyjna kropka. */
@Composable
fun ShimmerDot(
    modifier: Modifier = Modifier,
    size: Dp = 4.dp,
) {
    Box(
        modifier = modifier
            .width(size)
            .height(size)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PurpleLight, PurpleDark
                    )
                ), shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

/** Dekoracyjna linia. */
@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    offset: Float,
    width: Int = 40,
) {
    // Animacja przesunięcia gradientu
    val brush = Brush.linearGradient(
        colors = listOf(
            PurpleDark.copy(alpha = 0.5f),
            PurplePrimary,
            PurpleLight,
            PurplePrimary,
            PurpleDark.copy(alpha = 0.5f)
        ), start = androidx.compose.ui.geometry.Offset(
            offset * 100f, 0f
        ), end = androidx.compose.ui.geometry.Offset(
            offset * 100f + 100f, 0f
        )
    )

    Spacer(
        modifier = modifier
            .width(width.dp)
            .height(2.dp)
            .background(brush)
    )
}