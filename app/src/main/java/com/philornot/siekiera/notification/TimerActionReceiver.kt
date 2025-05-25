package com.philornot.siekiera.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import timber.log.Timber

/**
 * BroadcastReceiver obsługujący akcje wykonywane z powiadomień timera.
 * Umożliwia pauzowanie, wznawianie i anulowanie timera bezpośrednio
 * z powiadomienia bez konieczności otwierania aplikacji.
 */
class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Timber.d("TimerActionReceiver otrzymał akcję: $action")

        when (action) {
            "com.philornot.siekiera.DISMISS_TIMER_NOTIFICATION" -> {
                handleDismissNotification(context)
            }

            "com.philornot.siekiera.PAUSE_TIMER" -> {
                handlePauseTimer(context)
            }

            "com.philornot.siekiera.RESUME_TIMER" -> {
                handleResumeTimer(context)
            }

            "com.philornot.siekiera.CANCEL_TIMER" -> {
                handleCancelTimer(context)
            }

            else -> {
                Timber.w("TimerActionReceiver: Nieznana akcja: $action")
            }
        }
    }

    /**
     * Obsługuje zamknięcie powiadomienia timera.
     */
    private fun handleDismissNotification(context: Context) {
        Timber.d("Zamykanie powiadomienia timera")

        try {
            TimerNotificationHelper.cancelTimerNotification(context)
            Timber.d("Powiadomienie timera zostało zamknięte")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas zamykania powiadomienia timera")
        }
    }

    /**
     * Obsługuje pauzowanie timera z powiadomienia.
     */
    private fun handlePauseTimer(context: Context) {
        Timber.d("Pauzowanie timera z powiadomienia")

        try {
            if (TimerScheduler.isTimerSet(context) && !TimerScheduler.isTimerPaused(context)) {
                val success = TimerScheduler.pauseTimer(context)

                if (success) {
                    val remainingMinutes = TimerScheduler.getRemainingTimeMillis(context) / (60 * 1000)

                    // Anuluj powiadomienie o postępie i pokaż powiadomienie o pauzie
                    TimerNotificationHelper.cancelTimerProgressNotification(context)
                    TimerNotificationHelper.showTimerPausedNotification(context, remainingMinutes.toInt())

                    Toast.makeText(context, "Timer został spauzowany", Toast.LENGTH_SHORT).show()
                    Timber.d("Timer został spauzowany z powiadomienia")
                } else {
                    Toast.makeText(context, "Nie można spauzować timera", Toast.LENGTH_SHORT).show()
                    Timber.w("Nie udało się spauzować timera z powiadomienia")
                }
            } else {
                Toast.makeText(context, "Brak aktywnego timera do spauzowania", Toast.LENGTH_SHORT).show()
                Timber.w("Próba pauzowania nieaktywnego timera")
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas pauzowania timera z powiadomienia")
            Toast.makeText(context, "Błąd podczas pauzowania timera", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Obsługuje wznawianie timera z powiadomienia.
     */
    private fun handleResumeTimer(context: Context) {
        Timber.d("Wznawianie timera z powiadomienia")

        try {
            if (TimerScheduler.isTimerPaused(context)) {
                val success = TimerScheduler.resumeTimer(context)

                if (success) {
                    // Anuluj powiadomienie o pauzie - timer będzie działał w tle
                    TimerNotificationHelper.cancelTimerNotification(context)

                    Toast.makeText(context, "Timer został wznowiony", Toast.LENGTH_SHORT).show()
                    Timber.d("Timer został wznowiony z powiadomienia")
                } else {
                    Toast.makeText(context, "Nie można wznowić timera", Toast.LENGTH_SHORT).show()
                    Timber.w("Nie udało się wznowić timera z powiadomienia")
                }
            } else {
                Toast.makeText(context, "Brak spauzowanego timera do wznowienia", Toast.LENGTH_SHORT).show()
                Timber.w("Próba wznowienia nie-spauzowanego timera")
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas wznawiania timera z powiadomienia")
            Toast.makeText(context, "Błąd podczas wznawiania timera", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Obsługuje anulowanie timera z powiadomienia.
     */
    private fun handleCancelTimer(context: Context) {
        Timber.d("Anulowanie timera z powiadomienia")

        try {
            if (TimerScheduler.isTimerSet(context)) {
                val minutes = TimerScheduler.getTimerMinutes(context)
                val success = TimerScheduler.cancelTimer(context)

                if (success) {
                    // Anuluj wszystkie powiadomienia timera
                    TimerNotificationHelper.cancelTimerNotification(context)

                    Toast.makeText(context, "Timer został anulowany", Toast.LENGTH_SHORT).show()
                    Timber.d("Timer na $minutes minut został anulowany z powiadomienia")
                } else {
                    Toast.makeText(context, "Nie można anulować timera", Toast.LENGTH_SHORT).show()
                    Timber.w("Nie udało się anulować timera z powiadomienia")
                }
            } else {
                Toast.makeText(context, "Brak aktywnego timera do anulowania", Toast.LENGTH_SHORT).show()
                Timber.w("Próba anulowania nieaktywnego timera")
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas anulowania timera z powiadomienia")
            Toast.makeText(context, "Błąd podczas anulowania timera", Toast.LENGTH_SHORT).show()
        }
    }
}