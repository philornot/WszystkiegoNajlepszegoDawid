package com.philornot.siekiera.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.R

/**
 * Ekran ustawień aplikacji zawierający opcje zmiany nazwy aplikacji,
 * motywu i innych preferencji użytkownika.
 *
 * @param modifier Modifier dla całego ekranu
 * @param currentAppName Aktualna nazwa aplikacji
 * @param isDarkTheme Czy aktywny jest ciemny motyw
 * @param onThemeToggle Callback wywoływany przy zmianie motywu
 * @param onAppNameChange Callback wywoływany przy zmianie nazwy aplikacji
 * @param onAppNameReset Callback wywoływany przy resetowaniu nazwy
 *    aplikacji
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    currentAppName: String,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onAppNameChange: (String) -> Unit,
    onAppNameReset: () -> Unit,
) {
    LocalContext.current
    val scrollState = rememberScrollState()

    // Stany dla dialogu zmiany nazwy
    var showChangeNameDialog by remember { mutableStateOf(false) }
    var newAppName by remember { mutableStateOf("") }

    // Domyślna nazwa aplikacji
    val defaultAppName = stringResource(R.string.app_name)
    val timerPlaceholder = stringResource(R.string.app_name_timer)

    // Sprawdź czy nazwa aplikacji jest domyślna
    val isDefaultName = currentAppName == defaultAppName

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nagłówek
        Text(
            text = "Ustawienia",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        // Sekcja motywu
        SettingsCard(
            title = "Wygląd", description = "Personalizuj wygląd aplikacji"
        ) {
            // Przełącznik motywu
            SettingsItem(
                icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                title = "Ciemny motyw",
                description = if (isDarkTheme) "Włączony" else "Wyłączony",
                trailing = {
                    Switch(
                        checked = isDarkTheme, onCheckedChange = onThemeToggle
                    )
                })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sekcja nazwy aplikacji
        SettingsCard(
            title = "Nazwa aplikacji", description = "Zmień nazwę wyświetlaną w systemie"
        ) {
            Column {
                // Aktualna nazwa
                SettingsItem(
                    icon = Icons.Default.AppRegistration,
                    title = "Aktualna nazwa",
                    description = currentAppName
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Przycisk zmiany nazwy
                SettingsItemButton(
                    icon = Icons.Default.AppRegistration,
                    title = "Zmień nazwę aplikacji",
                    description = "Ustaw własną nazwę (wymaga restartu)",
                    onClick = {
                        newAppName = timerPlaceholder
                        showChangeNameDialog = true
                    })

                // Przycisk resetowania nazwy (tylko jeśli nazwa nie jest domyślna)
                if (!isDefaultName) {
                    SettingsItemButton(
                        icon = Icons.Default.Refresh,
                        title = "Zresetuj nazwę aplikacji",
                        description = "Przywróć domyślną nazwę \"$defaultAppName\"",
                        onClick = onAppNameReset
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sekcja dla przyszłych ustawień
        SettingsCard(
            title = "Dodatkowe opcje",
            description = "Więcej ustawień zostanie dodanych w przyszłości"
        ) {
            Text(
                text = "Tutaj pojawią się dodatkowe opcje konfiguracji aplikacji.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Spacer na dole
        Spacer(modifier = Modifier.height(32.dp))
    }

    // Dialog zmiany nazwy aplikacji
    if (showChangeNameDialog) {
        AlertDialog(onDismissRequest = { showChangeNameDialog = false }, title = {
            Text("Zmień nazwę aplikacji")
        }, text = {
            Column {
                Text(
                    text = "⚠️ Uwaga: Zmiana nazwy aplikacji spowoduje zamknięcie aplikacji. " + "Będziesz musiał ponownie ją otworzyć z ekranu głównego.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = newAppName,
                    onValueChange = { newAppName = it },
                    label = { Text("Nowa nazwa") },
                    placeholder = { Text(timerPlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }, confirmButton = {
            TextButton(
                onClick = {
                    if (newAppName.isNotBlank()) {
                        onAppNameChange(newAppName.trim())
                        showChangeNameDialog = false
                    }
                }, enabled = newAppName.isNotBlank()
            ) {
                Text("Zmień")
            }
        }, dismissButton = {
            TextButton(
                onClick = { showChangeNameDialog = false }) {
                Text("Anuluj")
            }
        })
    }
}