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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.philornot.siekiera.config.AppConfig
import timber.log.Timber

/**
 * Manager zarządzający uprawnieniami aplikacji. Enkapsuluje całą logikę
 * związaną z wymaganiem uprawnień od użytkownika.
 *
 * NAPRAWKA: Usunięto ActivityResultLauncher który powodował problemy z
 * Fragment version. Logika uprawnień została przeniesiona do MainActivity
 * gdzie używa Compose.
 */
class PermissionsManager(
    private val activity: ComponentActivity,
    private val appConfig: AppConfig,
    private val onAlarmPermissionGranted: () -> Unit,
) {

    /** Sprawdza i żąda wszystkich wymaganych uprawnień. */
    fun requestAllPermissions() {
        checkAndRequestAlarmPermission()
        checkNotificationPermission()
        checkStoragePermission()
    }

    /**
     * Sprawdza uprawnienia do dokładnych alarmów i otwiera ustawienia jeśli
     * potrzeba. NAPRAWKA: Uproszczone - bez ActivityResultLauncher.
     */
    private fun checkAndRequestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()

            Timber.d("Sprawdzanie uprawnień do dokładnych alarmów: hasPermission=$hasPermission")

            if (!hasPermission) {
                Timber.d("Brak uprawnień do alarmów - pokazuję instrukcje")
                showAlarmPermissionInstructions()
            } else {
                Timber.d("Uprawnienia do alarmów już przyznane")
            }
        } else {
            Timber.d("Android < 12, uprawnienia do alarmów nie są wymagane")
        }
    }

    /** Pokazuje instrukcje jak włączyć uprawnienia do alarmów. */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun showAlarmPermissionInstructions() {
        Toast.makeText(
            activity,
            "Aby aplikacja działała poprawnie, włącz uprawnienia do alarmów w ustawieniach. " + "Automatycznie otworzę ustawienia.",
            Toast.LENGTH_LONG
        ).show()

        // Spróbuj otworzyć ustawienia alarmów
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Nie można otworzyć ustawień alarmów")
            // Fallback - otwórz główne ustawienia aplikacji
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
            } catch (e2: Exception) {
                Timber.e(e2, "Nie można otworzyć żadnych ustawień")
                Toast.makeText(
                    activity,
                    "Nie można otworzyć ustawień automatycznie. " + "Przejdź ręcznie do Ustawienia > Aplikacje > ${activity.packageName} > Uprawnienia",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Sprawdza uprawnienia do wyświetlania powiadomień. NAPRAWKA: Tylko
     * sprawdza, nie żąda - żądanie jest w MainActivity z Compose.
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                Timber.d("Uprawnienia do powiadomień już przyznane")
            } else {
                Timber.d("Brak uprawnień do powiadomień - będą wymagane w MainActivity")
                Toast.makeText(
                    activity, "Będziesz poproszony o uprawnienia do powiadomień", Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Timber.d("Android < 13, uprawnienia do powiadomień nie są wymagane")
        }
    }

    /** Sprawdza uprawnienia do przechowywania. */
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Timber.d("Brak uprawnień do przechowywania - prośba o przyznanie")
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    Toast.makeText(
                        activity,
                        "Potrzebujemy uprawnień do przechowywania, aby zapisać prezent w folderze Pobrane.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Użyj standardowego requestPermissions
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
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

    /** Sprawdza czy wszystkie wymagane uprawnienia są przyznane. */
    fun hasAllRequiredPermissions(): Boolean {
        val hasAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasStoragePermission = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return hasAlarmPermission && hasNotificationPermission && hasStoragePermission
    }

    /** Obsługuje wynik żądania uprawnień z onRequestPermissionsResult. */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Uprawnienia do przechowywania przyznane")
                    Toast.makeText(
                        activity, "Uprawnienia do przechowywania przyznane", Toast.LENGTH_SHORT
                    ).show()
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
    }

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001
    }
}