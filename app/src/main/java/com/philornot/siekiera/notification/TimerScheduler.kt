package com.philornot.siekiera.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import timber.log.Timber
import java.util.Calendar

/** Klasa do planowania i zarządzania powiadomieniami timera. */
object TimerScheduler {
    // Identyfikator akcji dla broadcast receivera
    const val ACTION_TIMER_COMPLETE = "com.philornot.siekiera.ACTION_TIMER_COMPLETE"

    // Klucz dla minut w intent extras
    const val EXTRA_TIMER_MINUTES = "extra_timer_minutes"

    // Klucz preferencji timera
    private const val PREFS_NAME = "timer_prefs"
    private const val KEY_TIMER_SET = "timer_set"
    private const val KEY_TIMER_END_TIME = "timer_end_time"
    private const val KEY_TIMER_MINUTES = "timer_minutes"
    private const val KEY_TIMER_CHANGED_APP_NAME = "timer_changed_app_name"

    /**
     * Planuje timer na określoną liczbę minut od teraz.
     *
     * @param context Kontekst aplikacji
     * @param minutes Liczba minut do odliczenia
     * @param changeAppName Czy zmienić nazwę aplikacji
     * @return true jeśli timer został zaplanowany pomyślnie, false w przypadku
     *    błędu
     */
    fun scheduleTimer(context: Context, minutes: Int, changeAppName: Boolean = false): Boolean {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Oblicz czas zakończenia timera
            val calendar = Calendar.getInstance()
            val currentTimeMillis = calendar.timeInMillis
            val endTimeMillis = currentTimeMillis + (minutes * 60 * 1000L)

            // Zapisz informacje o timerze
            saveTimerInfo(context, true, endTimeMillis, minutes, changeAppName)

            // Utwórz intent z informacją o czasie timera
            val intent = Intent(context, TimerReceiver::class.java).apply {
                action = ACTION_TIMER_COMPLETE
                putExtra(EXTRA_TIMER_MINUTES, minutes)
            }

            // Utwórz PendingIntent
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Zaplanuj alarm na czas zakończenia timera
            Timber.d("Planuję timer na $minutes minut, zakończenie o $endTimeMillis")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent
                    )
                } else {
                    // Fallback dla urządzeń bez uprawnień do dokładnych alarmów
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent
                    )
                }
            } else {
                // Dla starszych wersji Androida
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent
                )
            }

            return true
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas planowania timera")
            return false
        }
    }

    /**
     * Anuluje bieżący timer, jeśli jest aktywny.
     *
     * @param context Kontekst aplikacji
     * @return true jeśli timer został anulowany, false jeśli nie było
     *    aktywnego timera lub wystąpił błąd
     */
    fun cancelTimer(context: Context): Boolean {
        try {
            // Sprawdź, czy timer jest aktywny
            if (!isTimerSet(context)) {
                Timber.d("Brak aktywnego timera do anulowania")
                return false
            }

            // Pobierz AlarmManager
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Przygotuj PendingIntent do anulowania (musi być identyczny jak przy ustawianiu)
            val intent = Intent(context, TimerReceiver::class.java).apply {
                action = ACTION_TIMER_COMPLETE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Anuluj alarm
            alarmManager.cancel(pendingIntent)

            // Wyczyść informacje o timerze
            saveTimerInfo(context, false, 0, 0, false)

            // Anuluj ewentualne aktywne powiadomienie
            TimerNotificationHelper.cancelTimerNotification(context)

            Timber.d("Timer został anulowany")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas anulowania timera")
            return false
        }
    }

    /**
     * Sprawdza, czy dla timera zmieniono nazwę aplikacji.
     *
     * @param context Kontekst aplikacji
     * @return true jeśli zmieniono nazwę aplikacji, false w przeciwnym razie
     */
    fun wasAppNameChanged(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TIMER_CHANGED_APP_NAME, false)
    }

    /**
     * Sprawdza, czy jest aktywny timer.
     *
     * @param context Kontekst aplikacji
     * @return true jeśli jest aktywny timer, false w przeciwnym razie
     */
    fun isTimerSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TIMER_SET, false)
    }

    /**
     * Pobiera pozostały czas w milisekundach do zakończenia timera.
     *
     * @param context Kontekst aplikacji
     * @return Pozostały czas w milisekundach lub 0, jeśli timer nie jest
     *    aktywny lub już się zakończył
     */
    fun getRemainingTimeMillis(context: Context): Long {
        if (!isTimerSet(context)) return 0

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endTimeMillis = prefs.getLong(KEY_TIMER_END_TIME, 0)
        val currentTimeMillis = System.currentTimeMillis()

        return (endTimeMillis - currentTimeMillis).coerceAtLeast(0)
    }

    /**
     * Pobiera liczbę minut ustawioną dla aktywnego timera.
     *
     * @param context Kontekst aplikacji
     * @return Liczba minut lub 0, jeśli timer nie jest aktywny
     */
    fun getTimerMinutes(context: Context): Int {
        if (!isTimerSet(context)) return 0

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TIMER_MINUTES, 0)
    }

    /**
     * Zapisuje informacje o timerze w preferencjach.
     *
     * @param context Kontekst aplikacji
     * @param isSet Czy timer jest aktywny
     * @param endTimeMillis Czas zakończenia timera w milisekundach
     * @param minutes Liczba minut ustawiona dla timera
     * @param changedAppName Czy zmieniono nazwę aplikacji dla tego timera
     */
    private fun saveTimerInfo(
        context: Context,
        isSet: Boolean,
        endTimeMillis: Long,
        minutes: Int,
        changedAppName: Boolean,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_TIMER_SET, isSet)
            putLong(KEY_TIMER_END_TIME, endTimeMillis)
            putInt(KEY_TIMER_MINUTES, minutes)
            putBoolean(KEY_TIMER_CHANGED_APP_NAME, changedAppName)
        }
    }

    /**
     * Przywraca timer po restarcie urządzenia, jeśli był aktywny.
     *
     * @param context Kontekst aplikacji
     * @return true jeśli timer został przywrócony, false w przeciwnym razie
     */
    fun restoreTimerAfterReboot(context: Context): Boolean {
        if (!isTimerSet(context)) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val endTimeMillis = prefs.getLong(KEY_TIMER_END_TIME, 0)
        val minutes = prefs.getInt(KEY_TIMER_MINUTES, 0)
        prefs.getBoolean(KEY_TIMER_CHANGED_APP_NAME, false)

        // Sprawdź, czy timer już się nie zakończył
        val currentTimeMillis = System.currentTimeMillis()
        if (endTimeMillis <= currentTimeMillis) {
            // Timer już się zakończył, wyczyść dane i pokaż powiadomienie
            saveTimerInfo(context, false, 0, 0, false)
            TimerNotificationHelper.showTimerCompletedNotification(context, minutes)
            return false
        }

        // Timer jeszcze trwa, przywróć go
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Utwórz intent z informacją o czasie timera
            val intent = Intent(context, TimerReceiver::class.java).apply {
                action = ACTION_TIMER_COMPLETE
                putExtra(EXTRA_TIMER_MINUTES, minutes)
            }

            // Utwórz PendingIntent
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Zaplanuj alarm na pozostały czas
            Timber.d("Przywracanie timera po restarcie, pozostało ${(endTimeMillis - currentTimeMillis) / 1000 / 60} minut")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent
                )
            }

            return true
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas przywracania timera po restarcie")
            return false
        }
    }

    /**
     * Sprawdza, czy timer jest aktywny, i jeśli się już zakończył, anuluje go.
     * Zapobiega wyświetlaniu powiadomień o zakończonym już timerze.
     *
     * @param context Kontekst aplikacji
     * @return true jeśli timer jest nadal aktywny, false w przeciwnym razie
     */
    fun checkAndCleanupTimer(context: Context): Boolean {
        if (!isTimerSet(context)) return false

        val remainingTimeMillis = getRemainingTimeMillis(context)
        if (remainingTimeMillis <= 0) {
            // Timer już się zakończył - anuluj go bez pokazywania powiadomienia
            val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
            prefs.edit {
                putBoolean("timer_set", false)
                putBoolean(KEY_TIMER_CHANGED_APP_NAME, false)
            }

            // Anuluj potencjalne alarmy (to nie wyświetli powiadomienia)
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, TimerReceiver::class.java).apply {
                    action = ACTION_TIMER_COMPLETE
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            } catch (e: Exception) {
                Timber.e(e, "Błąd podczas anulowania timera")
            }

            return false
        }

        return true
    }
}