package com.philornot.siekiera.managers

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.edit
import com.philornot.siekiera.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.system.exitProcess

/**
 * Manager zarządzający ustawieniami aplikacji - motyw, nazwa aplikacji, itp.
 * Enkapsuluje logikę zapisywania i ładowania ustawień z SharedPreferences.
 */
class SettingsManager(private val context: Context) {

    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    // Stany dla ustawień
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _currentAppName = MutableStateFlow("")
    val currentAppName: StateFlow<String> = _currentAppName.asStateFlow()

    /**
     * Ładuje ustawienia aplikacji z SharedPreferences.
     * Wywoływane przy starcie aplikacji.
     */
    fun loadAppSettings() {
        // Załaduj ustawienie motywu (domyślnie system)
        val savedTheme = settingsPrefs.getString("theme_preference", "system")
        val systemDarkTheme = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val isDark = when (savedTheme) {
            "dark" -> true
            "light" -> false
            else -> systemDarkTheme // "system" lub brak ustawienia
        }
        _isDarkTheme.value = isDark

        // Załaduj aktualną nazwę aplikacji
        val savedAppName = settingsPrefs.getString("app_name", context.getString(R.string.app_name))
        _currentAppName.value = savedAppName ?: context.getString(R.string.app_name)

        Timber.d("Załadowano ustawienia - motyw: $savedTheme (isDark: $isDark), nazwa: ${_currentAppName.value}")
    }

    /**
     * Przełącza motyw aplikacji między jasnym a ciemnym.
     */
    fun toggleTheme(enableDarkTheme: Boolean) {
        _isDarkTheme.value = enableDarkTheme

        // Zapisz ustawienie motywu
        settingsPrefs.edit {
            putString("theme_preference", if (enableDarkTheme) "dark" else "light")
        }

        Timber.d("Zmieniono motyw na: ${if (enableDarkTheme) "ciemny" else "jasny"}")
        Toast.makeText(
            context,
            "Motyw został zmieniony na ${if (enableDarkTheme) "ciemny" else "jasny"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Zmienia nazwę aplikacji na nową wartość i zamyka aplikację.
     * Aplikacja musi zostać ponownie otwarta przez użytkownika.
     */
    fun changeAppName(newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) {
            Toast.makeText(context, "Nazwa aplikacji nie może być pusta", Toast.LENGTH_SHORT).show()
            return
        }

        _currentAppName.value = trimmedName

        // Zapisz nową nazwę
        settingsPrefs.edit {
            putString("app_name", trimmedName)
        }

        Timber.d("Zmieniono nazwę aplikacji na: $trimmedName")

        // Próba zmiany nazwy w systemie przez włączenie odpowiedniego aliasu
        tryChangeAppNameInSystem(trimmedName)

        // Pokaż toast i zamknij aplikację
        Toast.makeText(
            context,
            context.getString(R.string.app_name_changed_toast),
            Toast.LENGTH_SHORT
        ).show()

        // Zamknij aplikację po krótkim opóźnieniu, żeby toast się pokazał
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500) // 1.5 sekundy na pokazanie toast

            // Jeśli context to Activity, zamknij ją
            if (context is android.app.Activity) {
                context.finishAffinity() // Zamknij wszystkie aktywności
            }
            exitProcess(0) // Wymusi zamknięcie aplikacji
        }
    }

    /**
     * Resetuje nazwę aplikacji do domyślnej wartości i zamyka aplikację.
     */
    fun resetAppName() {
        val defaultName = context.getString(R.string.app_name)
        _currentAppName.value = defaultName

        // Usuń zapisaną nazwę (powróć do domyślnej)
        settingsPrefs.edit {
            remove("app_name")
        }

        Timber.d("Zresetowano nazwę aplikacji do: $defaultName")

        // Próba przywrócenia oryginalnej nazwy w systemie
        tryChangeAppNameInSystem(defaultName)

        // Pokaż toast i zamknij aplikację
        Toast.makeText(
            context,
            context.getString(R.string.app_name_reset_toast),
            Toast.LENGTH_SHORT
        ).show()

        // Zamknij aplikację po krótkim opóźnieniu
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)

            if (context is android.app.Activity) {
                context.finishAffinity()
            }
            exitProcess(0)
        }
    }

    /**
     * Próbuje zmienić nazwę aplikacji w systemie Android poprzez manipulację
     * activity-alias. Ta funkcja próbuje włączyć/wyłączyć odpowiednie aliasy aktywności.
     */
    private fun tryChangeAppNameInSystem(newName: String) {
        try {
            val packageManager = context.packageManager
            val originalComponent = ComponentName(context, "com.philornot.siekiera.MainActivity")
            val timerComponent = ComponentName(context, "com.philornot.siekiera.TimerActivityAlias")

            // Sprawdź czy nazwa odpowiada nazwie timera
            val timerName = context.getString(R.string.app_name_timer)
            val defaultName = context.getString(R.string.app_name)

            when (newName) {
                timerName -> {
                    // Włącz alias timera, wyłącz oryginalną aktywność
                    packageManager.setComponentEnabledSetting(
                        originalComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    packageManager.setComponentEnabledSetting(
                        timerComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Timber.d("Włączono alias timera dla nazwy: $newName")
                }
                defaultName -> {
                    // Przywróć oryginalną aktywność, wyłącz alias
                    packageManager.setComponentEnabledSetting(
                        originalComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    packageManager.setComponentEnabledSetting(
                        timerComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Timber.d("Przywrócono oryginalną aktywność dla nazwy: $newName")
                }
                else -> {
                    // Dla innych nazw używamy domyślnej aktywności
                    packageManager.setComponentEnabledSetting(
                        originalComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    packageManager.setComponentEnabledSetting(
                        timerComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Timber.d("Ustawiono domyślną aktywność dla niestandardowej nazwy: $newName")
                }
            }

        } catch (e: SecurityException) {
            Timber.w("Brak uprawnień do zmiany komponentów aplikacji: ${e.message}")
            // Nie pokazujemy błędu użytkownikowi, ponieważ to oczekiwane ograniczenie
        } catch (e: Exception) {
            Timber.e(e, "Nieznany błąd podczas zmiany nazwy aplikacji w systemie: ${e.message}")
        }
    }
}