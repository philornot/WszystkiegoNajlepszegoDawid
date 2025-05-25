package com.philornot.siekiera.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * BroadcastReceiver, który odbiera powiadomienie o odsłonięciu prezentu.
 *
 * Powiadomienie jest zawsze wyświetlane gdy alarm się uruchomi.
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Czas na odsłonięcie prezentu!")

        // Powiadomienie jest wyświetlane zawsze gdy alarm się uruchomi,
        // ponieważ alarm jest planowany tylko gdy data jest w przyszłości

        // Użyj NotificationHelper do wyświetlenia powiadomienia
        NotificationHelper.showGiftRevealNotification(context)
    }
}