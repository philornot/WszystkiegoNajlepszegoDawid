package com.philornot.siekiera.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Karta grupująca powiązane ustawienia - bez widocznych ramek.
 *
 * @param title Tytuł sekcji ustawień
 * @param description Opis sekcji
 * @param modifier Modifier dla karty
 * @param content Zawartość karty
 */
@Composable
fun SettingsCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        ), elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // Usunięcie cienia
        ), border = null // Usunięcie obramowania
    ) {
        Column(
            modifier = Modifier.padding(20.dp) // Zwiększony padding dla lepszego wyglądu
        ) {
            // Nagłówek sekcji
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp) // Zwiększony padding
            )

            // Zawartość sekcji
            content()
        }
    }
}