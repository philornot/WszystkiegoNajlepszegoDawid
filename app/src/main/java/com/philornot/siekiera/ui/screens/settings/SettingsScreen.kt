package com.philornot.siekiera.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.R
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.network.DriveApiClient
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

/**
 * Ekran ustawień aplikacji zawierający opcje zmiany nazwy aplikacji,
 * motywu i informację o najnowszym pliku Daylio z 5-minutowym cache'em.
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
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Stany dla dialogu zmiany nazwy
    var showChangeNameDialog by remember { mutableStateOf(false) }
    var newAppName by remember { mutableStateOf("") }

    // Stany dla informacji o pliku
    var fileInfo by remember { mutableStateOf("Sprawdzanie...") }
    var isRefreshing by remember { mutableStateOf(false) }

    // Domyślna nazwa aplikacji
    val defaultAppName = stringResource(R.string.app_name)
    val timerPlaceholder = stringResource(R.string.app_name_timer)

    // Sprawdź czy nazwa aplikacji jest domyślna
    val isDefaultName = currentAppName == defaultAppName

    // Klucze dla SharedPreferences
    val PREF_LAST_CHECK_TIME = "file_sync_last_check_time"
    val PREF_LAST_FILE_INFO = "file_sync_last_file_info"
    val PREF_LAST_SYNC_TIME_DISPLAY = "file_sync_last_sync_time_display"

    // Limit sprawdzania - 5 minut
    val CHECK_LIMIT_MS = 5 * 60 * 1000L

    /** Pobiera zapisane dane z cache'u */
    fun getCachedData(): Triple<String, Long, String> {
        val prefs =
            context.getSharedPreferences("settings_cache", android.content.Context.MODE_PRIVATE)
        val lastFileInfo = prefs.getString(PREF_LAST_FILE_INFO, "") ?: ""
        val lastCheckTime = prefs.getLong(PREF_LAST_CHECK_TIME, 0L)
        val lastSyncTimeDisplay = prefs.getString(PREF_LAST_SYNC_TIME_DISPLAY, "") ?: ""
        return Triple(lastFileInfo, lastCheckTime, lastSyncTimeDisplay)
    }

    /** Zapisuje dane do cache'u */
    fun saveCachedData(fileInfo: String, checkTime: Long, syncTimeDisplay: String) {
        val prefs =
            context.getSharedPreferences("settings_cache", android.content.Context.MODE_PRIVATE)
        prefs.edit {
            putString(PREF_LAST_FILE_INFO, fileInfo)
                .putLong(PREF_LAST_CHECK_TIME, checkTime)
                .putString(PREF_LAST_SYNC_TIME_DISPLAY, syncTimeDisplay)
        }
    }

    /** Formatuje czas sprawdzenia do czytelnej postaci */
    fun formatCheckTime(timeMs: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(timeMs))
    }

    /**
     * Funkcja do sprawdzania informacji o najnowszym pliku Daylio. Sprawdza
     * limity API i cache, żeby nie nadużywać API Google Drive.
     */
    suspend fun checkLatestFileInfo(forceRefresh: Boolean = false): String {
        return withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val (cachedFileInfo, lastCheckTime, lastSyncTimeDisplay) = getCachedData()

                // Sprawdź czy można wykonać sprawdzenie (nie częściej niż raz na 5 minut)
                if (!forceRefresh && currentTime - lastCheckTime < CHECK_LIMIT_MS) {
                    val remainingMinutes =
                        ((CHECK_LIMIT_MS) - (currentTime - lastCheckTime)) / (60 * 1000L)

                    // Jeśli mamy cache'owane dane, pokaż je z informacją o ostatniej synchronizacji
                    return@withContext if (cachedFileInfo.isNotEmpty() && lastSyncTimeDisplay.isNotEmpty()) {
                        "Z ostatniej synchronizacji ($lastSyncTimeDisplay):\n$cachedFileInfo\n\nMożna odświeżyć za $remainingMinutes minut"
                    } else {
                        "Można odświeżyć za $remainingMinutes minut"
                    }
                }

                val appConfig = AppConfig.getInstance(context)
                val driveClient = DriveApiClient.getInstance(context)

                // Zainicjalizuj klienta
                if (!driveClient.initialize()) {
                    return@withContext "Błąd połączenia z Google Drive"
                }

                val folderId = appConfig.getDriveFolderId()

                // Pobierz listę plików .daylio
                val files =
                    driveClient.listFilesInFolder(folderId).filter { it.name.endsWith(".daylio") }

                if (files.isEmpty()) {
                    return@withContext "Nie znaleziono plików .daylio"
                }

                // Znajdź najnowszy plik
                val newestFile = files.maxByOrNull { it.modifiedTime.time }
                    ?: return@withContext "Błąd podczas wyszukiwania najnowszego pliku"

                val formattedDate = TimeUtils.formatDate(newestFile.modifiedTime)
                val syncTimeDisplay = formatCheckTime(currentTime)
                val newFileInfo =
                    "Najnowszy plik: ${newestFile.name}\nData modyfikacji: $formattedDate"

                // Zapisz nowe dane do cache'u
                saveCachedData(newFileInfo, currentTime, syncTimeDisplay)

                return@withContext newFileInfo

            } catch (e: Exception) {
                Timber.e(e, "Błąd podczas sprawdzania pliku")
                "Błąd: ${e.message ?: "Nieznany błąd"}"
            }
        }
    }

    /** Funkcja do odświeżania informacji o pliku */
    fun refreshFileInfo() {
        if (isRefreshing) return

        isRefreshing = true
        kotlinx.coroutines.MainScope().launch {
            try {
                fileInfo = checkLatestFileInfo(forceRefresh = true)
            } catch (_: Exception) {
                fileInfo = "Błąd podczas odświeżania"
                Toast.makeText(context, "Nie udało się odświeżyć informacji", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                isRefreshing = false
            }
        }
    }

    // Automatyczne sprawdzenie przy pierwszym załadowaniu
    LaunchedEffect(Unit) {
        fileInfo = try {
            checkLatestFileInfo()
        } catch (_: Exception) {
            "Błąd podczas ładowania"
        }
    }

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

        // Sekcja informacji o pliku Daylio
        SettingsCard(
            title = "Plik z wpisami",
            description = "Informacja o najnowszym pliku Daylio na Google Drive"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                // Ikona informacji
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Informacja o pliku
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Status synchronizacji",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = fileInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = if (fileInfo.startsWith("Błąd") || fileInfo.contains("Można odświeżyć")) {
                            FontStyle.Italic
                        } else {
                            FontStyle.Normal
                        }
                    )
                }

                // Przycisk odświeżania
                IconButton(
                    onClick = { refreshFileInfo() }, enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Odśwież",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Dodatkowa informacja o limitach
            if (fileInfo.contains("Można odświeżyć za")) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Ograniczenie sprawdzania chroni przed przekroczeniem limitów API",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
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