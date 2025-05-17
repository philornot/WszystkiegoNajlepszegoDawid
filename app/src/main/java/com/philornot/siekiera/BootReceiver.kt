package com.philornot.siekiera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import timber.log.Timber

/**
 * BroadcastReceiver wywoływany po uruchomieniu urządzenia. Ponownie
 * planuje powiadomienia i zadania po restarcie urządzenia.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("System uruchomiony, ponowne planowanie zadań")

            // Pobierz instancję konfiguracji
            val appConfig = AppConfig.getInstance(context.applicationContext)

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
    }
}