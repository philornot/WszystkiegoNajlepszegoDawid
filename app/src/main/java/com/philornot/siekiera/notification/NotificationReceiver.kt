package com.philornot.siekiera.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/** BroadcastReceiver, który odbiera powiadomienie o odsłonięciu prezentu. */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Czas na odsłonięcie prezentu!")

        // Sprawdź, czy prezent został już odebrany
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val giftReceived = prefs.getBoolean("gift_received", false)

        if (giftReceived) {
            Timber.d("Prezent został już odebrany, pomijam powiadomienie")
            return
        }

        // Użyj NotificationHelper do wyświetlenia powiadomienia
        NotificationHelper.showGiftRevealNotification(context)
    }
}