package com.philornot.siekiera.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Element ustawienia z funkcją kliknięcia (przycisk).
 *
 * @param icon Ikona elementu
 * @param title Tytuł elementu
 * @param description Opis elementu
 * @param onClick Funkcja wywoływana przy kliknięciu
 * @param modifier Modifier dla elementu
 */
@Composable
fun SettingsItemButton(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
) {
    Row(
        modifier = modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Companion.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.Companion.size(24.dp)
        )

        Spacer(modifier = Modifier.Companion.width(16.dp))

        Column(modifier = Modifier.Companion.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}