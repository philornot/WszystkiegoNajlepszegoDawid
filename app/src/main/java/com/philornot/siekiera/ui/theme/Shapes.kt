package com.philornot.siekiera.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Definicja kształtów dla aplikacji
val Shapes = Shapes(
    // Używane dla małych elementów jak przyciski, pola tekstowe
    small = RoundedCornerShape(8.dp),

    // Używane dla średnich elementów jak karty
    medium = RoundedCornerShape(12.dp),

    // Używane dla dużych elementów jak arkusze dolne
    large = RoundedCornerShape(16.dp),

    // Używane dla bardzo dużych elementów
    extraLarge = RoundedCornerShape(24.dp)
)