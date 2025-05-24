package com.philornot.siekiera.managers

import android.content.Context
import android.content.SharedPreferences
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import timber.log.Timber
import androidx.core.content.edit

/**
 * Manager zarządzający powiadomieniami urodzinowymi. Enkapsuluje logikę
 * planowania i anulowania powiadomień związanych z urodzinami.
 */
class BirthdayNotificationManager(
    private val context: Context,
    private val appConfig: AppConfig,
    private val prefs: SharedPreferences,
) {

    /**
     * Planuje powiadomienie na dzień urodzin jeśli prezent nie został jeszcze
     * odebrany. Sprawdza warunki i wywołuje NotificationScheduler.
     */
    fun scheduleRevealNotification() {
        val giftReceived = prefs.getBoolean("gift_received", false)

        // Zaplanuj powiadomienie tylko jeśli prezent nie został odebrany
        if (appConfig.isBirthdayNotificationEnabled() && !giftReceived) {
            Timber.d("Zlecam zaplanowanie powiadomienia urodzinowego")
            NotificationScheduler.scheduleGiftRevealNotification(context, appConfig)
        } else {
            if (giftReceived) {
                Timber.d("Prezent został już odebrany - nie planuję powiadomienia")
            } else {
                Timber.d("Powiadomienia urodzinowe są wyłączone w konfiguracji")
            }
        }
    }

    /**
     * Anuluje zaplanowane powiadomienie urodzinowe. Może być użyte gdy prezent
     * zostanie odebrany wcześniej.
     */
    fun cancelRevealNotification() {
        // NotificationScheduler nie ma metody cancel, ale można by ją dodać
        // Na razie logujemy informację
        Timber.d("Żądanie anulowania powiadomienia urodzinowego")

        // Opcjonalnie: oznacz w preferencjach, że powiadomienie zostało anulowane
        prefs.edit { putBoolean("notification_cancelled", true) }
    }

    /**
     * Sprawdza czy powiadomienie powinno być zaplanowane ponownie. Używane po
     * przyznaniu uprawnień do alarmów.
     */
    fun recheckNotificationScheduling() {
        val giftReceived = prefs.getBoolean("gift_received", false)
        val notificationCancelled = prefs.getBoolean("notification_cancelled", false)

        if (!giftReceived && !notificationCancelled) {
            scheduleRevealNotification()
        } else {
            Timber.d("Nie planuję powiadomienia ponownie - gift_received=$giftReceived, cancelled=$notificationCancelled")
        }
    }

    /**
     * Sprawdza czy data urodzin jest w przyszłości i czy warto planować
     * powiadomienie.
     */
    fun validateNotificationTiming(): Boolean {
        val revealDateMillis = appConfig.getBirthdayTimeMillis()
        val currentTimeMillis = System.currentTimeMillis()

        val isInFuture = currentTimeMillis < revealDateMillis

        if (!isInFuture) {
            Timber.d("Data urodzin już minęła - nie planuję powiadomienia")
        }

        return isInFuture
    }

    /** Loguje szczegółowe informacje o stanie powiadomień dla debugowania. */
    fun logNotificationStatus() {
        val giftReceived = prefs.getBoolean("gift_received", false)
        val notificationEnabled = appConfig.isBirthdayNotificationEnabled()
        val isValidTiming = validateNotificationTiming()
        val notificationCancelled = prefs.getBoolean("notification_cancelled", false)

        Timber.d("Status powiadomień urodzinowych:")
        Timber.d("  - Prezent odebrany: $giftReceived")
        Timber.d("  - Powiadomienia włączone: $notificationEnabled")
        Timber.d("  - Data w przyszłości: $isValidTiming")
        Timber.d("  - Powiadomienie anulowane: $notificationCancelled")
        Timber.d("  - Powinno być zaplanowane: ${notificationEnabled && !giftReceived && isValidTiming && !notificationCancelled}")
    }
}
