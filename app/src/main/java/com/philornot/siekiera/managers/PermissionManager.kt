package com.philornot.siekiera.managers

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.philornot.siekiera.config.AppConfig
import timber.log.Timber

/**
 * Manager zarządzający uprawnieniami aplikacji. Enkapsuluje całą logikę
 * związaną z wymaganiem uprawnień od użytkownika.
 */
class PermissionsManager(
    private val activity: ComponentActivity,
    private val appConfig: AppConfig,
    private val onAlarmPermissionGranted: () -> Unit,
) {

    // Stany zarządzania uprawnieniami
    private val notificationPermissionRequested = mutableStateOf(false)
    private val storagePermissionRequested = mutableStateOf(false)

    // Launcher dla żądania uprawnień do dokładnych alarmów
    private val alarmPermissionLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            handleAlarmPermissionResult()
        }

    // Launcher dla powiadomień
    private val notificationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            handleNotificationPermissionResult(isGranted)
        }

    // Launcher dla uprawnień do przechowywania
    private val storagePermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            handleStoragePermissionResult(isGranted)
        }

    /** Sprawdza i żąda wszystkich wymaganych uprawnień. */
    fun requestAllPermissions() {
        requestAlarmPermission()
        requestNotificationPermission()
        requestStoragePermission()
    }

    /**
     * Sprawdza i żąda uprawnień do dokładnych alarmów (wymagane od Androida
     * 12).
     */
    private fun requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()

            Timber.d("Sprawdzanie uprawnień do dokładnych alarmów: hasPermission=$hasPermission")

            if (!hasPermission) {
                Timber.d("Brak uprawnień do alarmów - otwieram ekran ustawień")
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    alarmPermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Błąd podczas otwierania ekranu uprawnień")
                    Toast.makeText(
                        activity,
                        "Nie można otworzyć ekranu uprawnień: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Timber.d("Uprawnienia do alarmów już przyznane")
            }
        } else {
            Timber.d("Android < 12, uprawnienia do alarmów nie są wymagane")
        }
    }

    /**
     * Sprawdza i żąda uprawnień do wyświetlania powiadomień (wymagane od
     * Androida 13).
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    // Pokaż wyjaśnienie dlaczego potrzebujemy uprawnień
                    Toast.makeText(
                        activity,
                        "Potrzebujemy uprawnień do wyświetlania powiadomień, aby poinformować Cię gdy prezent będzie gotowy.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (!notificationPermissionRequested.value) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                Timber.d("Uprawnienia do powiadomień już przyznane")
            }
        } else {
            Timber.d("Android < 13, uprawnienia do powiadomień nie są wymagane")
        }
    }

    /**
     * Sprawdza i żąda uprawnień do przechowywania (wymagane dla starszych
     * wersji Androida).
     */
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    // Pokaż wyjaśnienie dlaczego potrzebujemy uprawnień
                    Toast.makeText(
                        activity,
                        "Potrzebujemy uprawnień do przechowywania, aby zapisać prezent w folderze Pobrane.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (!storagePermissionRequested.value) {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            } else {
                Timber.d("Uprawnienia do przechowywania już przyznane")
            }
        } else {
            Timber.d("Android > P, uprawnienia do przechowywania nie są wymagane")
        }
    }

    /**
     * Sprawdza ponownie uprawnienia po powrocie do aplikacji. Wywoływane w
     * onResume().
     */
    fun checkPermissionsOnResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()
            Timber.d("Sprawdzanie uprawnień w onResume: hasPermission=$hasPermission")

            // Jeśli uprawnienia zostały przyznane poza normalnym flow (np. z ustawień)
            if (hasPermission && appConfig.isBirthdayNotificationEnabled()) {
                onAlarmPermissionGranted()
            }
        }
    }

    /** Obsługuje wynik żądania uprawnień do alarmów. */
    private fun handleAlarmPermissionResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()

            Timber.d("Powrót z ekranu uprawnień do alarmów, nowy stan uprawnień: $hasPermission")

            if (hasPermission) {
                Timber.d("Uprawnienia do dokładnych alarmów przyznane, planuję powiadomienie")
                onAlarmPermissionGranted()
            } else {
                Timber.w("Uprawnienia do dokładnych alarmów nadal nie przyznane")
                Toast.makeText(
                    activity,
                    "Bez uprawnień do dokładnych alarmów nie mogę zagwarantować punktualnego powiadomienia.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Obsługuje wynik żądania uprawnień do powiadomień. */
    private fun handleNotificationPermissionResult(isGranted: Boolean) {
        notificationPermissionRequested.value = true

        if (isGranted) {
            Timber.d("Uprawnienia do powiadomień przyznane")
            Toast.makeText(
                activity, "Będziesz powiadomiony w dniu urodzin!", Toast.LENGTH_SHORT
            ).show()
        } else {
            Timber.w("Uprawnienia do powiadomień odrzucone")
            Toast.makeText(
                activity,
                "Bez uprawnień do powiadomień nie będziesz mógł zobaczyć powiadomienia urodzinowego",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Obsługuje wynik żądania uprawnień do przechowywania. */
    private fun handleStoragePermissionResult(isGranted: Boolean) {
        storagePermissionRequested.value = true

        if (isGranted) {
            Timber.d("Uprawnienia do przechowywania przyznane")
        } else {
            Timber.w("Uprawnienia do przechowywania odrzucone")
            Toast.makeText(
                activity,
                "Bez uprawnień do przechowywania nie będziesz mógł pobrać prezentu",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}