package com.philornot.siekiera.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import timber.log.Timber

/**
 * Manager zarządzający powiadomieniami urodzinowymi. Enkapsuluje logikę
 * planowania i anulowania powiadomień związanych z urodzinami.
 *
 * AKTUALIZACJA: Powiadomienia są teraz planowane tylko na podstawie daty z
 * konfiguracji (czy jest w przyszłości), niezależnie od statusu odebrania
 * prezentu. To jest spójne z logiką szufladki nawigacyjnej.
 */
class BirthdayNotificationManager(
    private val context: Context,
    private val appConfig: AppConfig,
    private val prefs: SharedPreferences,
) {

    /**
     * Planuje powiadomienie na dzień urodzin jeśli data jest w przyszłości.
     * Sprawdza warunki i wywołuje NotificationScheduler.
     *
     * Nie sprawdza czy prezent został odebrany - powiadomienia są
     * planowane tylko na podstawie daty z konfiguracji.
     */
    fun scheduleRevealNotification() {
        // Sprawdź czy powiadomienia są włączone w konfiguracji
        if (!appConfig.isBirthdayNotificationEnabled()) {
            Timber.d("Powiadomienia urodzinowe są wyłączone w konfiguracji")
            return
        }

        // Sprawdź czy data urodzin jest w przyszłości
        if (!validateNotificationTiming()) {
            Timber.d("Data urodzin już minęła - nie planuję powiadomienia")
            return
        }

        // Zaplanuj powiadomienie
        Timber.d("Zlecam zaplanowanie powiadomienia urodzinowego")
        NotificationScheduler.scheduleGiftRevealNotification(context, appConfig)
    }

    /**
     * Anuluje zaplanowane powiadomienie urodzinowe. Może być użyte gdy prezent
     * zostanie odebrany wcześniej lub gdy użytkownik wyłączy powiadomienia.
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
     *
     * Nie sprawdza statusu odebrania prezentu.
     */
    fun recheckNotificationScheduling() {
        val notificationCancelled = prefs.getBoolean("notification_cancelled", false)

        if (!notificationCancelled && validateNotificationTiming()) {
            scheduleRevealNotification()
        } else {
            Timber.d("Nie planuję powiadomienia ponownie - cancelled=$notificationCancelled, timing_valid=${validateNotificationTiming()}")
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
        val notificationEnabled = appConfig.isBirthdayNotificationEnabled()
        val isValidTiming = validateNotificationTiming()
        val notificationCancelled = prefs.getBoolean("notification_cancelled", false)

        Timber.d("Status powiadomień urodzinowych:")
        Timber.d("  - Powiadomienia włączone: $notificationEnabled")
        Timber.d("  - Data w przyszłości: $isValidTiming")
        Timber.d("  - Powiadomienie anulowane: $notificationCancelled")
        Timber.d("  - Powinno być zaplanowane: ${notificationEnabled && isValidTiming && !notificationCancelled}")
    }
}