package com.philornot.siekiera.ui.screens.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.config.AdminConfigManager
import com.philornot.siekiera.config.AppConfig
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar

/**
 * Admin settings screen - accessible via secret gesture (5 taps on settings icon).
 * Allows changing birthday date and Google Drive configuration without reinstalling app.
 */
@Composable
fun AdminSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val adminConfig = remember { AdminConfigManager.getInstance(context) }
    val appConfig = remember { AppConfig.getInstance(context) }

    var isAdminEnabled by remember { mutableStateOf(adminConfig.isAdminConfigEnabled()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Current configuration values
    var birthdayCalendar by remember {
        mutableStateOf(
            adminConfig.getAdminBirthdayDate() ?: appConfig.getBirthdayDate()
        )
    }
    var driveFolderId by remember {
        mutableStateOf(
            adminConfig.getAdminDriveFolderId() ?: appConfig.getDriveFolderId()
        )
    }
    var daylioFileName by remember {
        mutableStateOf(
            adminConfig.getAdminDaylioFileName() ?: appConfig.getDaylioFileName()
        )
    }

    var showFolderIdDialog by remember { mutableStateOf(false) }
    var showFileNameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Konfiguracja Administratora",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "⚠️ Tylko dla developera",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Enable/Disable Admin Config
        SettingsCard(
            title = "Status Konfiguracji",
            description = if (isAdminEnabled)
                "Używasz nadpisanej konfiguracji administratora"
            else
                "Używasz domyślnej konfiguracji z config.xml"
        ) {
            SettingsItem(
                icon = if (isAdminEnabled) Icons.Default.AdminPanelSettings else Icons.Default.Settings,
                title = "Tryb Administratora",
                description = if (isAdminEnabled) "Włączony" else "Wyłączony",
                trailing = {
                    Switch(
                        checked = isAdminEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                showConfirmDialog = true
                            } else {
                                isAdminEnabled = true
                                adminConfig.setAdminConfigEnabled(true)
                                Toast.makeText(
                                    context,
                                    "Tryb administratora włączony",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Birthday Configuration
        SettingsCard(
            title = "Data Urodzin",
            description = "Ustaw datę i czas odsłonięcia prezentu"
        ) {
            Column {
                // Current birthday display
                SettingsItem(
                    icon = Icons.Default.Cake,
                    title = "Aktualna Data",
                    description = formatBirthdayDate(birthdayCalendar)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Change date button
                SettingsItemButton(
                    icon = Icons.Default.CalendarToday,
                    title = "Zmień Datę",
                    description = "Wybierz nową datę urodzin",
                    onClick = {
                        if (!isAdminEnabled) {
                            Toast.makeText(
                                context,
                                "Włącz tryb administratora aby zmienić datę",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@SettingsItemButton
                        }

                        val year = birthdayCalendar.get(Calendar.YEAR)
                        val month = birthdayCalendar.get(Calendar.MONTH)
                        val day = birthdayCalendar.get(Calendar.DAY_OF_MONTH)

                        DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                birthdayCalendar.set(Calendar.YEAR, selectedYear)
                                birthdayCalendar.set(Calendar.MONTH, selectedMonth)
                                birthdayCalendar.set(Calendar.DAY_OF_MONTH, selectedDay)

                                adminConfig.setAdminBirthdayDate(
                                    selectedYear,
                                    selectedMonth,
                                    selectedDay,
                                    birthdayCalendar.get(Calendar.HOUR_OF_DAY),
                                    birthdayCalendar.get(Calendar.MINUTE)
                                )

                                Toast.makeText(
                                    context,
                                    "Data zapisana - zrestartuj aplikację",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            year,
                            month,
                            day
                        ).show()
                    }
                )

                // Change time button
                SettingsItemButton(
                    icon = Icons.Default.AccessTime,
                    title = "Zmień Godzinę",
                    description = "Wybierz godzinę odsłonięcia",
                    onClick = {
                        if (!isAdminEnabled) {
                            Toast.makeText(
                                context,
                                "Włącz tryb administratora aby zmienić godzinę",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@SettingsItemButton
                        }

                        val hour = birthdayCalendar.get(Calendar.HOUR_OF_DAY)
                        val minute = birthdayCalendar.get(Calendar.MINUTE)

                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                birthdayCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                                birthdayCalendar.set(Calendar.MINUTE, selectedMinute)

                                adminConfig.setAdminBirthdayDate(
                                    birthdayCalendar.get(Calendar.YEAR),
                                    birthdayCalendar.get(Calendar.MONTH),
                                    birthdayCalendar.get(Calendar.DAY_OF_MONTH),
                                    selectedHour,
                                    selectedMinute
                                )

                                Toast.makeText(
                                    context,
                                    "Godzina zapisana - zrestartuj aplikację",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            hour,
                            minute,
                            true // 24-hour format
                        ).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Drive Configuration
        SettingsCard(
            title = "Google Drive",
            description = "Konfiguracja folderu i pliku Daylio"
        ) {
            Column {
                // Folder ID
                SettingsItemButton(
                    icon = Icons.Default.Folder,
                    title = "ID Folderu Drive",
                    description = driveFolderId.take(30) + if (driveFolderId.length > 30) "..." else "",
                    onClick = {
                        if (isAdminEnabled) {
                            showFolderIdDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Włącz tryb administratora",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // File name
                SettingsItemButton(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    title = "Nazwa Pliku Daylio",
                    description = daylioFileName,
                    onClick = {
                        if (isAdminEnabled) {
                            showFileNameDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Włącz tryb administratora",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Remote Configuration Publishing
        if (isAdminEnabled) {
            var isPublishing by remember { mutableStateOf(false) }
            var showPublishDialog by remember { mutableStateOf(false) }

            SettingsCard(
                title = "Zdalna Konfiguracja",
                description = "Opublikuj konfigurację dla wszystkich użytkowników"
            ) {
                Column {
                    Text(
                        text = "⚠️ UWAGA: Ta funkcja zmieni konfigurację dla WSZYSTKICH użytkowników aplikacji (w tym dla Dawida)!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    SettingsItemButton(
                        icon = Icons.Default.CloudUpload,
                        title = "Opublikuj Konfigurację",
                        description = "Utwórz plik app_config.json na Drive",
                        onClick = { showPublishDialog = true }
                    )
                }
            }

            // Publish dialog
            if (showPublishDialog) {
                AlertDialog(
                    onDismissRequest = { showPublishDialog = false },
                    title = { Text("Opublikować Konfigurację?") },
                    text = {
                        Column {
                            Text(
                                text = "Czy na pewno chcesz opublikować następującą konfigurację dla wszystkich użytkowników?",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text(
                                text = buildString {
                                    appendLine("Data urodzin:")
                                    appendLine(formatBirthdayDate(birthdayCalendar))
                                    appendLine()
                                    appendLine("Nazwa pliku:")
                                    appendLine(daylioFileName)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Po publikacji wszystkie instancje aplikacji pobiorą tę konfigurację przy następnym uruchomieniu.",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isPublishing = true
                                kotlinx.coroutines.MainScope().launch {
                                    try {
                                        // Generate JSON for manual upload
                                        val json = org.json.JSONObject().apply {
                                            put("version", 1)
                                            put("birthday_year", birthdayCalendar.get(Calendar.YEAR))
                                            put("birthday_month", birthdayCalendar.get(Calendar.MONTH) + 1)
                                            put("birthday_day", birthdayCalendar.get(Calendar.DAY_OF_MONTH))
                                            put("birthday_hour", birthdayCalendar.get(Calendar.HOUR_OF_DAY))
                                            put("birthday_minute", birthdayCalendar.get(Calendar.MINUTE))
                                            put("daylio_file_name", daylioFileName)
                                            put("last_updated", System.currentTimeMillis())
                                        }

                                        val jsonString = json.toString(2)

                                        // Copy to clipboard
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("app_config.json", jsonString)
                                        clipboard.setPrimaryClip(clip)

                                        Toast.makeText(
                                            context,
                                            "JSON skopiowany do schowka!\n\nUtwórz plik 'app_config.json' w folderze Drive i wklej zawartość.",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        Timber.d("Generated remote config JSON:\n$jsonString")

                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Błąd: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Timber.e(e, "Error generating config JSON")
                                    } finally {
                                        isPublishing = false
                                        showPublishDialog = false
                                    }
                                }
                            },
                            enabled = !isPublishing
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Generuj JSON", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPublishDialog = false }) {
                            Text("Anuluj")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Configuration Summary
        if (isAdminEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ważne",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Po zmianie konfiguracji zrestartuj aplikację, aby zmiany zostały zastosowane.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Folder ID Dialog
    if (showFolderIdDialog) {
        var newFolderId by remember { mutableStateOf(driveFolderId) }

        AlertDialog(
            onDismissRequest = { showFolderIdDialog = false },
            title = { Text("ID Folderu Google Drive") },
            text = {
                Column {
                    Text(
                        text = "Wprowadź ID folderu z URL Google Drive:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = newFolderId,
                        onValueChange = { newFolderId = it },
                        label = { Text("Folder ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderId.isNotBlank()) {
                            driveFolderId = newFolderId.trim()
                            adminConfig.setAdminDriveFolderId(driveFolderId)
                            showFolderIdDialog = false
                            Toast.makeText(
                                context,
                                "Folder ID zapisany - zrestartuj aplikację",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderIdDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // File Name Dialog
    if (showFileNameDialog) {
        var newFileName by remember { mutableStateOf(daylioFileName) }

        AlertDialog(
            onDismissRequest = { showFileNameDialog = false },
            title = { Text("Nazwa Pliku Daylio") },
            text = {
                Column {
                    Text(
                        text = "Wprowadź nazwę pliku (z rozszerzeniem .daylio):",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Nazwa pliku") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileName.isNotBlank() && newFileName.endsWith(".daylio")) {
                            daylioFileName = newFileName.trim()
                            adminConfig.setAdminDaylioFileName(daylioFileName)
                            showFileNameDialog = false
                            Toast.makeText(
                                context,
                                "Nazwa pliku zapisana - zrestartuj aplikację",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Nazwa musi kończyć się na .daylio",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFileNameDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Confirm disable dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Wyłączyć Tryb Administratora?") },
            text = {
                Text("Wszystkie zmiany zostaną usunięte i aplikacja wróci do domyślnej konfiguracji z config.xml.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        adminConfig.clearAdminConfig()
                        isAdminEnabled = false
                        showConfirmDialog = false

                        // Reset to default values
                        birthdayCalendar = appConfig.getBirthdayDate()
                        driveFolderId = appConfig.getDriveFolderId()
                        daylioFileName = appConfig.getDaylioFileName()

                        Toast.makeText(
                            context,
                            "Przywrócono domyślną konfigurację - zrestartuj aplikację",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                ) {
                    Text("Wyłącz", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

/**
 * Format birthday date for display.
 */
private fun formatBirthdayDate(calendar: Calendar): String {
    return "${calendar.get(Calendar.DAY_OF_MONTH)}.${calendar.get(Calendar.MONTH) + 1}.${calendar.get(Calendar.YEAR)} " +
            "o ${String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))}"
}