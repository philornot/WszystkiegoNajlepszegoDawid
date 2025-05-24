package com.philornot.siekiera.managers

import android.content.Context
import android.widget.Toast
import com.philornot.siekiera.R
import com.philornot.siekiera.notification.TimerNotificationHelper
import com.philornot.siekiera.notification.TimerScheduler
import com.philornot.siekiera.ui.screens.main.drawer.NavigationSection
import timber.log.Timber

/**
 * Manager zarządzający funkcjonalnością timera. Enkapsuluje logikę
 * TimerScheduler i zarządzanie stanem timera.
 */
class TimerManager(
    private val context: Context,
    private val appStateManager: AppStateManager,
) {

    /**
     * Ustawia timer na określoną ilość minut. Planuje powiadomienie po upływie
     * czasu.
     *
     * @param minutes Ilość minut do odliczania
     * @return true jeśli timer został ustawiony pomyślnie
     */
    fun setTimer(minutes: Int): Boolean {
        Timber.d("Ustawianie timera na $minutes minut")

        return if (TimerScheduler.scheduleTimer(context, minutes)) {
            // Timer został ustawiony pomyślnie
            Toast.makeText(
                context, context.getString(R.string.timer_set_toast, minutes), Toast.LENGTH_SHORT
            ).show()

            // Ustaw stan timera
            appStateManager.setActiveTimerRemainingTime(minutes * 60 * 1000L)
            appStateManager.setTimerPaused(false)
            appStateManager.setCurrentSection(NavigationSection.TIMER)

            true
        } else {
            // Wystąpił błąd podczas ustawiania timera
            Toast.makeText(
                context,
                "Nie udało się ustawić timera. Sprawdź uprawnienia do alarmów.",
                Toast.LENGTH_LONG
            ).show()

            false
        }
    }

    /**
     * Pauzuje aktywny timer, jeśli istnieje.
     *
     * @return true jeśli timer został spauzowany pomyślnie
     */
    fun pauseTimer(): Boolean {
        return if (TimerScheduler.pauseTimer(context)) {
            Toast.makeText(
                context, context.getString(R.string.timer_paused_toast), Toast.LENGTH_SHORT
            ).show()

            // Zaktualizuj stan timera
            appStateManager.setTimerPaused(true)

            true
        } else {
            Toast.makeText(
                context, context.getString(R.string.timer_cannot_pause), Toast.LENGTH_SHORT
            ).show()

            false
        }
    }

    /**
     * Wznawia spauzowany timer.
     *
     * @return true jeśli timer został wznowiony pomyślnie
     */
    fun resumeTimer(): Boolean {
        return if (TimerScheduler.resumeTimer(context)) {
            Toast.makeText(
                context, context.getString(R.string.timer_resumed_toast), Toast.LENGTH_SHORT
            ).show()

            // Zaktualizuj stan timera
            appStateManager.setTimerPaused(false)

            true
        } else {
            Toast.makeText(
                context, context.getString(R.string.timer_cannot_resume), Toast.LENGTH_SHORT
            ).show()

            false
        }
    }

    /**
     * Anuluje aktywny timer, jeśli istnieje.
     *
     * @return true jeśli timer został anulowany pomyślnie
     */
    fun cancelTimer(): Boolean {
        return if (TimerScheduler.cancelTimer(context)) {
            Toast.makeText(
                context, context.getString(R.string.timer_cancelled_toast), Toast.LENGTH_SHORT
            ).show()

            // Zresetuj stan timera
            appStateManager.resetTimerState()

            true
        } else {
            false
        }
    }

    /** Resetuje aktywny timer i przywraca początkowy stan trybu timera. */
    fun resetTimer() {
        Timber.d("Resetowanie timera")

        // Anuluj aktywny timer
        cancelTimer()

        // Pozostań w trybie timera ale zresetuj stan
        appStateManager.setActiveTimerRemainingTime(0L)
        appStateManager.setTimerPaused(false)
        appStateManager.setCurrentSection(NavigationSection.TIMER)

        // Pokaż toast o zresetowaniu timera
        Toast.makeText(
            context, "Timer został zresetowany", Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Przywraca timer po restarcie aplikacji, jeśli był aktywny. Wywoływane w
     * onCreate() MainActivity.
     */
    fun restoreTimerAfterRestart() {
        if (TimerScheduler.isTimerSet(context)) {
            Timber.d("Przywracanie timera po uruchomieniu aplikacji")

            // Sprawdź, czy timer już się zakończył
            val remainingMillis = TimerScheduler.getRemainingTimeMillis(context)
            val isPaused = TimerScheduler.isTimerPaused(context)

            if (remainingMillis > 0 || isPaused) {
                // Timer wciąż aktywny lub spauzowany, ustaw jego stan
                appStateManager.setActiveTimerRemainingTime(remainingMillis)
                appStateManager.setTimerPaused(isPaused)
                appStateManager.setCurrentSection(NavigationSection.TIMER)
            } else {
                // Timer już się zakończył, pokaż powiadomienie
                val minutes = TimerScheduler.getTimerMinutes(context)
                TimerNotificationHelper.showTimerCompletedNotification(context, minutes)
                TimerScheduler.cancelTimer(context)
            }
        }
    }

    /**
     * Sprawdza stan timera po wznowieniu aplikacji. Wywoływane w onResume()
     * MainActivity.
     */
    fun checkTimerOnResume() {
        // Użyj nowej metody zapobiegającej spamowaniu
        if (TimerScheduler.checkAndCleanupTimer(context)) {
            // Timer wciąż działa, aktualizuj czas pozostały
            val remainingMillis = TimerScheduler.getRemainingTimeMillis(context)
            val isPaused = TimerScheduler.isTimerPaused(context)

            if (remainingMillis > 0 || isPaused) {
                appStateManager.setActiveTimerRemainingTime(remainingMillis)
                appStateManager.setTimerPaused(isPaused)
                appStateManager.setCurrentSection(NavigationSection.TIMER)
            }
        } else {
            // Nie ma aktywnego timera, przywróć normalny tryb
            if (appStateManager.isTimerActive()) {
                appStateManager.resetTimerState()
            }
        }
    }
}