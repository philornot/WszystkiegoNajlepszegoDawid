package com.philornot.siekiera

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import com.philornot.siekiera.notification.TimerScheduler
import timber.log.Timber

/**
 * BroadcastReceiver wywoływany po uruchomieniu urządzenia. Ponownie
 * planuje powiadomienia i zadania po restarcie urządzenia.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("System uruchomiony, ponowne planowanie zadań")

            // Sprawdź, czy prezent został już odebrany
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val giftReceived = prefs.getBoolean("gift_received", false)

            // Pobierz instancję konfiguracji
            val appConfig = AppConfig.getInstance(context.applicationContext)

            // Przywróć powiadomienie urodzinowe (tylko jeśli prezent nie został odebrany)
            if (!giftReceived) {
                restoreBirthdayNotification(context, appConfig)
            }

            // Przywróć timer, jeśli był aktywny przed restartem
            restoreTimer(context)
        }
    }

    /** Przywraca powiadomienie urodzinowe po restarcie urządzenia */
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

    /** Przywraca timer po restarcie urządzenia, jeśli był aktywny */
    private fun restoreTimer(context: Context) {
        // Sprawdź, czy timer był aktywny i czy już się nie zakończył
        if (TimerScheduler.checkAndCleanupTimer(context)) {
            Timber.d("Przywracanie aktywnego timera po restarcie urządzenia")
            val success = TimerScheduler.restoreTimerAfterReboot(context)

            if (success) {
                Timber.d("Timer został pomyślnie przywrócony")

                // Spróbuj przywrócić alias aktywności dla trybu timera
                try {
                    val componentName =
                        ComponentName(context, "com.philornot.siekiera.TimerActivityAlias")
                    context.packageManager.setComponentEnabledSetting(
                        componentName,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    Timber.d("Alias aktywności dla trybu timera został włączony")
                } catch (e: Exception) {
                    Timber.w("Nie udało się włączyć aliasu aktywności: ${e.message}")
                }
            } else {
                Timber.d("Nie udało się przywrócić timera")
            }
        } else {
            Timber.d("Brak aktywnego timera do przywrócenia lub timer już się zakończył")
        }
    }
}