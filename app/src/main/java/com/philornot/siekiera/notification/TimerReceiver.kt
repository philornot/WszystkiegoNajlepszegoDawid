package com.philornot.siekiera.notification

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.edit
import timber.log.Timber

/**
 * BroadcastReceiver do obsługi zdarzeń związanych z timerem. Odbiera
 * powiadomienia o zakończeniu timera i wyświetla powiadomienie. Obsługuje
 * również spauzowane timery.
 */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerScheduler.ACTION_TIMER_COMPLETE -> {
                // Pobierz ilość minut z intencji
                val minutes = intent.getIntExtra(TimerScheduler.EXTRA_TIMER_MINUTES, 0)

                Timber.d("Timer zakończony (ustawiony na $minutes minut)")

                // Sprawdź, czy timer nie został już anulowany, spauzowany lub wcześniej zakończony
                val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
                val timerStillActive = prefs.getBoolean("timer_set", false)
                val timerPaused = prefs.getBoolean("timer_paused", false)
                val changedAppName = prefs.getBoolean("timer_changed_app_name", false)

                // Jeśli timer jest spauzowany, nie wyświetlaj powiadomienia o zakończeniu
                if (timerPaused) {
                    Timber.d("Timer jest spauzowany, ignorowanie powiadomienia o zakończeniu")
                    return
                }

                if (timerStillActive && !timerPaused) {
                    // Zaznacz timer jako zakończony
                    prefs.edit {
                        putBoolean("timer_set", false)
                        putBoolean("timer_paused", false)
                        putLong("timer_remaining_when_paused", 0L)
                        putLong("timer_pause_time", 0L)
                    }

                    // Wyświetl powiadomienie o zakończeniu timera
                    TimerNotificationHelper.showTimerCompletedNotification(context, minutes)

                    // Jeśli zmieniliśmy nazwę aplikacji, przywróć oryginalną
                    if (changedAppName) {
                        restoreOriginalAppName(context)
                    }
                } else {
                    Timber.d("Timer już został anulowany, spauzowany lub zakończony - ignorowanie zdarzenia")
                }
            }
        }
    }

    /**
     * Przywraca oryginalną nazwę aplikacji po zakończeniu timera. Wydzielona
     * jako osobna metoda dla lepszej czytelności.
     */
    private fun restoreOriginalAppName(context: Context) {
        try {
            // Próba nie powiedzie się dla zwykłych aplikacji (wymaga uprawnień systemowych)
            val packageManager = context.packageManager
            val originalComponent = ComponentName(context, "com.philornot.siekiera.MainActivity")
            val timerComponent = ComponentName(context, "com.philornot.siekiera.TimerActivityAlias")

            // Włącz oryginalną aktywność, wyłącz alias timera
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
            Timber.d("Przywrócono oryginalną nazwę aplikacji po zakończeniu timera")

            // Zapisz zmianę stanu
            val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
            prefs.edit { putBoolean("timer_changed_app_name", false) }
        } catch (e: SecurityException) {
            // To oczekiwany błąd - aplikacja nie ma uprawnień do zmiany komponentów
            Timber.w("Brak uprawnień do zmiany nazwy aplikacji: ${e.message}")
        } catch (e: Exception) {
            // Inny nieoczekiwany błąd
            Timber.e(e, "Błąd podczas przywracania nazwy aplikacji: ${e.message}")
        }
    }
}