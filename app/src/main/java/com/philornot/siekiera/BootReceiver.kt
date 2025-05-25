package com.philornot.siekiera

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import com.philornot.siekiera.notification.TimerScheduler
import timber.log.Timber

/**
 * BroadcastReceiver wywoływany po uruchomieniu urządzenia. Ponownie
 * planuje powiadomienia i zadania po restarcie urządzenia. Obsługuje
 * również przywracanie spauzowanych timerów.
 *
 * AKTUALIZACJA: Powiadomienia urodzinowe są przywracane tylko na podstawie
 * daty z konfiguracji, niezależnie od statusu odebrania prezentu.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("System uruchomiony, ponowne planowanie zadań")

            // Pobierz instancję konfiguracji
            val appConfig = AppConfig.getInstance(context.applicationContext)

            // Przywróć powiadomienie urodzinowe (tylko jeśli data jest w przyszłości)
            restoreBirthdayNotification(context, appConfig)

            // Przywróć timer, jeśli był aktywny lub spauzowany
            restoreTimer(context)
        }
    }

    /**
     * Przywraca powiadomienie urodzinowe po restarcie urządzenia.
     *
     * Nie sprawdza czy prezent został odebrany - powiadomienie
     * jest przywracane tylko na podstawie daty z konfiguracji.
     */
    private fun restoreBirthdayNotification(context: Context, appConfig: AppConfig) {
        // Sprawdź czy powiadomienia są włączone w konfiguracji
        if (!appConfig.isBirthdayNotificationEnabled()) {
            Timber.d("Powiadomienia urodzinowe są wyłączone w konfiguracji")
            return
        }

        // Sprawdź czy data odsłonięcia jest w przyszłości
        val revealDateMillis = appConfig.getBirthdayTimeMillis()
        val currentTimeMillis = System.currentTimeMillis()

        if (currentTimeMillis < revealDateMillis) {
            // Ponownie zaplanuj powiadomienie
            Timber.d("Ponowne planowanie powiadomienia o odsłonięciu")
            NotificationScheduler.scheduleGiftRevealNotification(context, appConfig)
        } else {
            Timber.d("Data odsłonięcia już minęła, nie planuję powiadomienia")
        }
    }

    /**
     * Przywraca timer po restarcie urządzenia, jeśli był aktywny lub
     * spauzowany
     */
    private fun restoreTimer(context: Context) {
        // Sprawdź, czy timer był aktywny i czy już się nie zakończył
        if (TimerScheduler.checkAndCleanupTimer(context)) {
            val isTimerPaused = TimerScheduler.isTimerPaused(context)

            if (isTimerPaused) {
                Timber.d("Przywracanie spauzowanego timera po restarcie urządzenia")
                // Timer był spauzowany - nie trzeba nic robić z alarmami,
                // stan zostanie przywrócony w MainActivity

                // Sprawdź, czy dla timera była zmieniona nazwa aplikacji
                if (TimerScheduler.wasAppNameChanged(context)) {
                    // Spróbuj przywrócić alias aktywności dla trybu timera
                    tryRestoreAppNameAlias(context)
                }
            } else {
                Timber.d("Przywracanie aktywnego timera po restarcie urządzenia")
                val success = TimerScheduler.restoreTimerAfterReboot(context)

                if (success) {
                    Timber.d("Timer został pomyślnie przywrócony")

                    // Sprawdź, czy dla timera była zmieniona nazwa aplikacji
                    if (TimerScheduler.wasAppNameChanged(context)) {
                        // Spróbuj przywrócić alias aktywności dla trybu timera
                        tryRestoreAppNameAlias(context)
                    }
                } else {
                    Timber.d("Nie udało się przywrócić timera")
                }
            }
        } else {
            Timber.d("Brak aktywnego timera do przywrócenia lub timer już się zakończył")
        }
    }

    /**
     * Próbuje przywrócić alias aktywności dla trybu timera. Oddzielna metoda
     * dla lepszej czytelności kodu.
     */
    private fun tryRestoreAppNameAlias(context: Context) {
        try {
            // Próba nie powiedzie się dla zwykłych aplikacji (wymaga uprawnień systemowych)
            val packageManager = context.packageManager
            val originalComponent = ComponentName(context, "com.philornot.siekiera.MainActivity")
            val timerComponent = ComponentName(context, "com.philornot.siekiera.TimerActivityAlias")

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
            Timber.d("Alias aktywności dla trybu timera został włączony")
        } catch (e: Exception) {
            Timber.w("Nie udało się włączyć aliasu aktywności: ${e.message}")
        }
    }
}