package com.philornot.siekiera.managers

import android.content.Context
import android.widget.Toast
import com.philornot.siekiera.R
import com.philornot.siekiera.notification.TimerNotificationHelper
import com.philornot.siekiera.notification.TimerScheduler
import com.philornot.siekiera.ui.screens.main.drawer.NavigationSection
import timber.log.Timber

/**
 * Ulepszona wersja TimerManager z lepszą responsywnością i obsługą błędów.
 * Enkapsuluje logikę TimerScheduler i zarządzanie stanem timera. Dodano
 * natychmiastową aktualizację stanu dla lepszych animacji.
 */
class TimerManager(
    private val context: Context,
    private val appStateManager: AppStateManager,
) {

    /**
     * Ustawia timer na określoną ilość minut z ulepszoną obsługą błędów.
     * Planuje powiadomienie po upływie czasu i natychmiastowo aktualizuje stan
     * UI.
     *
     * @param minutes Ilość minut do odliczania (1-1440)
     * @return true jeśli timer został ustawiony pomyślnie
     */
    fun setTimer(minutes: Int): Boolean {
        // Walidacja wejścia
        if (minutes < 1 || minutes > 1440) {
            Toast.makeText(
                context,
                "Timer może być ustawiony na 1-1440 minut (maksymalnie 24 godziny)",
                Toast.LENGTH_LONG
            ).show()
            Timber.w("Próba ustawienia timera na nieprawidłową wartość: $minutes minut")
            return false
        }

        Timber.d("Ustawianie timera na $minutes minut")

        // Anuluj poprzedni timer jeśli aktywny
        if (TimerScheduler.isTimerSet(context)) {
            Timber.d("Anulowanie poprzedniego timera przed ustawieniem nowego")
            TimerScheduler.cancelTimer(context)
        }

        // Natychmiast aktualizuj stan UI przed planowaniem timera
        val initialTimeMillis = minutes * 60 * 1000L
        appStateManager.setActiveTimerRemainingTime(initialTimeMillis)
        appStateManager.setTimerPaused(false)
        appStateManager.setCurrentSection(NavigationSection.TIMER)

        return if (TimerScheduler.scheduleTimer(context, minutes)) {
            // Timer został ustawiony pomyślnie
            val message = when {
                minutes < 60 -> context.getString(R.string.timer_set_toast, minutes)
                minutes == 60 -> "Timer ustawiony na 1 godzinę"
                minutes < 120 -> "Timer ustawiony na 1 godzinę i ${minutes - 60} minut"
                else -> {
                    val hours = minutes / 60
                    val remainingMinutes = minutes % 60
                    if (remainingMinutes == 0) {
                        "Timer ustawiony na $hours godzin"
                    } else {
                        "Timer ustawiony na $hours godzin i $remainingMinutes minut"
                    }
                }
            }

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

            Timber.d("Timer został pomyślnie ustawiony na $minutes minut")
            true
        } else {
            // Wystąpił błąd podczas ustawiania timera - cofnij zmiany stanu
            appStateManager.resetTimerState()

            Toast.makeText(
                context,
                "Nie udało się ustawić timera. Sprawdź uprawnienia do alarmów w ustawieniach systemowych.",
                Toast.LENGTH_LONG
            ).show()

            Timber.e("Nie udało się ustawić timera na $minutes minut")
            false
        }
    }

    /**
     * Pauzuje aktywny timer z natychmiastową reakcją UI.
     *
     * @return true jeśli timer został spauzowany pomyślnie
     */
    fun pauseTimer(): Boolean {
        Timber.d("Próba spauzowania timera")

        if (!TimerScheduler.isTimerSet(context)) {
            Toast.makeText(
                context, "Brak aktywnego timera do spauzowania", Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Natychmiastowa aktualizacja stanu UI przed operacją
        val remainingTime = TimerScheduler.getRemainingTimeMillis(context)
        appStateManager.setTimerPaused(true)
        appStateManager.setActiveTimerRemainingTime(remainingTime)

        return if (TimerScheduler.pauseTimer(context)) {
            Toast.makeText(
                context, context.getString(R.string.timer_paused_toast), Toast.LENGTH_SHORT
            ).show()

            Timber.d("Timer został spauzowany, pozostały czas: ${remainingTime / 1000} sekund")
            true
        } else {
            // Cofnij zmiany stanu jeśli operacja się nie powiodła
            appStateManager.setTimerPaused(false)

            Toast.makeText(
                context, context.getString(R.string.timer_cannot_pause), Toast.LENGTH_SHORT
            ).show()

            Timber.w("Nie udało się spauzować timera")
            false
        }
    }

    /**
     * Wznawia spauzowany timer z natychmiastową reakcją UI.
     *
     * @return true jeśli timer został wznowiony pomyślnie
     */
    fun resumeTimer(): Boolean {
        Timber.d("Próba wznowienia timera")

        if (!TimerScheduler.isTimerPaused(context)) {
            Toast.makeText(
                context, "Brak spauzowanego timera do wznowienia", Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Natychmiastowa aktualizacja stanu UI przed operacją
        val remainingTime = TimerScheduler.getRemainingTimeMillis(context)
        appStateManager.setTimerPaused(false)
        appStateManager.setActiveTimerRemainingTime(remainingTime)

        return if (TimerScheduler.resumeTimer(context)) {
            Toast.makeText(
                context, context.getString(R.string.timer_resumed_toast), Toast.LENGTH_SHORT
            ).show()

            Timber.d("Timer został wznowiony, pozostały czas: ${remainingTime / 1000} sekund")
            true
        } else {
            // Cofnij zmiany stanu jeśli operacja się nie powiodła
            appStateManager.setTimerPaused(true)

            Toast.makeText(
                context, context.getString(R.string.timer_cannot_resume), Toast.LENGTH_SHORT
            ).show()

            Timber.w("Nie udało się wznowić timera")
            false
        }
    }

    /**
     * Anuluje aktywny timer z natychmiastową reakcją UI.
     *
     * @return true jeśli timer został anulowany pomyślnie
     */
    fun cancelTimer(): Boolean {
        Timber.d("Próba anulowania timera")

        // Natychmiastowa aktualizacja stanu UI
        appStateManager.resetTimerState()

        return if (TimerScheduler.cancelTimer(context)) {
            Toast.makeText(
                context, context.getString(R.string.timer_cancelled_toast), Toast.LENGTH_SHORT
            ).show()

            Timber.d("Timer został anulowany")
            true
        } else {
            Timber.w("Nie udało się anulować timera (prawdopodobnie nie był aktywny)")
            false
        }
    }

    /**
     * Resetuje aktywny timer i przywraca początkowy stan trybu timera.
     * Ulepszona wersja z lepszą obsługą przejść stanów.
     */
    fun resetTimer() {
        Timber.d("Resetowanie timera")

        // Sprawdź czy timer był aktywny przed anulowaniem
        val wasActive = TimerScheduler.isTimerSet(context)
        val wasPaused = TimerScheduler.isTimerPaused(context)

        // Natychmiastowa aktualizacja stanu UI
        appStateManager.setActiveTimerRemainingTime(0L)
        appStateManager.setTimerPaused(false)
        appStateManager.setCurrentSection(NavigationSection.TIMER)

        // Anuluj aktywny timer
        val cancelled = TimerScheduler.cancelTimer(context)

        // Personalizowana wiadomość na podstawie poprzedniego stanu
        val message = when {
            !wasActive -> "Timer jest gotowy do ustawienia"
            wasPaused -> "Spauzowany timer został zresetowany"
            cancelled -> "Aktywny timer został zresetowany"
            else -> "Timer został zresetowany"
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

        Timber.d("Timer został zresetowany (poprzedni stan: aktywny=$wasActive, spauzowany=$wasPaused)")
    }

    /**
     * Przywraca timer po restarcie aplikacji z ulepszoną obsługą. Wywoływane w
     * onCreate() MainActivity.
     */
    fun restoreTimerAfterRestart() {
        if (!TimerScheduler.isTimerSet(context)) {
            Timber.d("Brak timera do przywrócenia po restarcie")
            return
        }

        Timber.d("Przywracanie timera po uruchomieniu aplikacji")

        val remainingMillis = TimerScheduler.getRemainingTimeMillis(context)
        val isPaused = TimerScheduler.isTimerPaused(context)
        val minutes = TimerScheduler.getTimerMinutes(context)

        Timber.d("Stan timera: pozostało ${remainingMillis / 1000}s, spauzowany=$isPaused, ustawiony na $minutes minut")

        if (remainingMillis > 0 || isPaused) {
            // Timer wciąż aktywny lub spauzowany, przywróć jego stan
            appStateManager.setActiveTimerRemainingTime(remainingMillis)
            appStateManager.setTimerPaused(isPaused)
            appStateManager.setCurrentSection(NavigationSection.TIMER)

            val statusMessage = when {
                isPaused -> "Przywrócono spauzowany timer (${remainingMillis / 60000} min pozostało)"
                remainingMillis > 0 -> "Przywrócono aktywny timer (${remainingMillis / 60000} min pozostało)"
                else -> "Przywrócono timer"
            }

            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            Timber.d("Timer przywrócony pomyślnie")
        } else {
            // Timer już się zakończył, pokaż powiadomienie i wyczyść
            Timber.d("Timer zakończył się podczas nieobecności aplikacji")
            TimerNotificationHelper.showTimerCompletedNotification(context, minutes)
            TimerScheduler.cancelTimer(context)
            appStateManager.resetTimerState()
        }
    }

    /**
     * Sprawdza stan timera po wznowieniu aplikacji z lepszą diagnostyką.
     * Wywoływane w onResume() MainActivity.
     */
    fun checkTimerOnResume() {
        Timber.d("Sprawdzanie stanu timera przy wznowieniu aplikacji")

        // Ulepszona diagnostyka stanu timera
        val isSet = TimerScheduler.isTimerSet(context)
        val isPaused = TimerScheduler.isTimerPaused(context)
        val remaining = if (isSet) TimerScheduler.getRemainingTimeMillis(context) else 0L

        Timber.d("Stan timera: ustawiony=$isSet, spauzowany=$isPaused, pozostało=${remaining / 1000}s")

        if (TimerScheduler.checkAndCleanupTimer(context)) {
            // Timer wciąż działa, aktualizuj czas pozostały
            val remainingMillis = TimerScheduler.getRemainingTimeMillis(context)
            val isTimerPaused = TimerScheduler.isTimerPaused(context)

            if (remainingMillis > 0 || isTimerPaused) {
                // Synchronizuj stan z rzeczywistym stanem timera
                appStateManager.setActiveTimerRemainingTime(remainingMillis)
                appStateManager.setTimerPaused(isTimerPaused)

                // Upewnij się że jesteśmy w trybie timera jeśli timer jest aktywny
                if (appStateManager.currentSection.value != NavigationSection.TIMER) {
                    appStateManager.setCurrentSection(NavigationSection.TIMER)
                }

                Timber.d("Zsynchronizowano stan timera: ${remainingMillis / 1000}s pozostało, spauzowany=$isTimerPaused")
            }
        } else {
            // Nie ma aktywnego timera lub timer się zakończył
            if (appStateManager.isTimerActive()) {
                Timber.d("Resetowanie nieaktywnego timera w AppStateManager")
                appStateManager.resetTimerState()
            }
        }
    }

    /**
     * Pobiera czytelny opis aktualnego stanu timera. Przydatne do debugowania
     * i logowania.
     */
    fun getTimerStatusDescription(): String {
        return when {
            !TimerScheduler.isTimerSet(context) -> "Timer nieaktywny"
            TimerScheduler.isTimerPaused(context) -> {
                val remaining = TimerScheduler.getRemainingTimeMillis(context)
                "Timer spauzowany (${remaining / 60000} min pozostało)"
            }

            else -> {
                val remaining = TimerScheduler.getRemainingTimeMillis(context)
                when {
                    remaining <= 0 -> "Timer zakończony"
                    remaining < 60000 -> "Timer aktywny (${remaining / 1000} sek)"
                    else -> "Timer aktywny (${remaining / 60000} min)"
                }
            }
        }
    }

    /** Sprawdza czy podana liczba minut jest prawidłowa dla timera. */
    fun isValidTimerDuration(minutes: Int): Boolean {
        return minutes in 1..1440
    }

    /** Konwertuje minuty na czytelny format (np. "1h 30min"). */
    fun formatTimerDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes min"
            minutes == 60 -> "1h"
            minutes < 120 -> "1h ${minutes - 60}min"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                if (remainingMinutes == 0) {
                    "${hours}h"
                } else {
                    "${hours}h ${remainingMinutes}min"
                }
            }
        }
    }
}