package com.philornot.siekiera.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber
import androidx.core.content.edit

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

                if (timerStillActive) {
                    // Zaznacz timer jako zakończony
                    prefs.edit { putBoolean("timer_set", false) }

                    // Wyświetl powiadomienie o zakończeniu timera
                    TimerNotificationHelper.showTimerCompletedNotification(context, minutes)
                } else {
                    Timber.d("Timer już został anulowany lub zakończony - ignorowanie zdarzenia")
                }
            }
        }
    }
}