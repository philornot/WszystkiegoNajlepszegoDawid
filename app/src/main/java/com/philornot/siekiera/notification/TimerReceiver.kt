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
 * powiadomienia o zakończeniu timera i wyświetla powiadomienie.
 */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerScheduler.ACTION_TIMER_COMPLETE -> {
                // Pobierz ilość minut z intencji
                val minutes = intent.getIntExtra(TimerScheduler.EXTRA_TIMER_MINUTES, 0)

                Timber.d("Timer zakończony (ustawiony na $minutes minut)")

                // Sprawdź, czy timer nie został już anulowany lub wcześniej zakończony
                val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
                val timerStillActive = prefs.getBoolean("timer_set", false)
                val changedAppName = prefs.getBoolean("timer_changed_app_name", false)

                if (timerStillActive) {
                    // Zaznacz timer jako zakończony
                    prefs.edit { putBoolean("timer_set", false) }

                    // Wyświetl powiadomienie o zakończeniu timera
                    TimerNotificationHelper.showTimerCompletedNotification(context, minutes)

                    // Jeśli zmieniliśmy nazwę aplikacji, przywróć oryginalną
                    if (changedAppName) {
                        try {
                            // Próba nie powiedzie się dla zwykłych aplikacji (wymaga uprawnień systemowych)
                            val packageManager = context.packageManager
                            val originalComponent =
                                ComponentName(context, "com.philornot.siekiera.MainActivity")
                            val timerComponent =
                                ComponentName(context, "com.philornot.siekiera.TimerActivityAlias")

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
                            prefs.edit { putBoolean("timer_changed_app_name", false) }
                        } catch (e: SecurityException) {
                            // To oczekiwany błąd - aplikacja nie ma uprawnień do zmiany komponentów
                            Timber.w("Brak uprawnień do zmiany nazwy aplikacji: ${e.message}")
                        } catch (e: Exception) {
                            // Inny nieoczekiwany błąd
                            Timber.e(e, "Błąd podczas przywracania nazwy aplikacji: ${e.message}")
                        }
                    }
                } else {
                    Timber.d("Timer już został anulowany lub zakończony - ignorowanie zdarzenia")
                }
            }
        }
    }
}