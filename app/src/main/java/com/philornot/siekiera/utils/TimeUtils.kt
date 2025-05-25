package com.philornot.siekiera.utils

import android.content.Context
import com.philornot.siekiera.config.AppConfig
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Ulepszona wersja TimeUtils z lepszym formatowaniem dla długich timerów.
 * Pomocnicze metody do pracy z czasem w aplikacji.
 */
object TimeUtils {
    // Strefa czasowa Warszawy
    private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

    // Przechowuje kontekst aplikacji - inicjalizowany przy pierwszym użyciu
    private var appContext: Context? = null

    /**
     * Ustawia kontekst aplikacji, który będzie używany przez TimeUtils.
     * Powinno być wywołane jeden raz podczas inicjalizacji aplikacji.
     */
    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            Timber.d("TimeUtils: zainicjalizowano z kontekstem aplikacji")
        }
    }

    /**
     * Tworzy obiekt Calendar z określoną datą i czasem w strefie czasowej
     * Warszawy. Używane zarówno w kodzie produkcyjnym jak i testach.
     */
    fun getCalendarForDateTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
    ): Calendar {
        return Calendar.getInstance(WARSAW_TIMEZONE).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /**
     * Zwraca czas w milisekundach dla daty odsłonięcia prezentu.
     *
     * @param appConfig Instancja konfiguracji aplikacji
     * @return Czas w milisekundach
     */
    fun getRevealDateMillis(appConfig: AppConfig): Long {
        return appConfig.getBirthdayTimeMillis()
    }

    /** Metoda kompatybilności wstecznej bez parametru appConfig. */
    @Deprecated(
        "Użyj wersji z jawnie przekazanym AppConfig", ReplaceWith("getRevealDateMillis(appConfig)")
    )
    fun getRevealDateMillis(): Long {
        val context = appContext ?: throw IllegalStateException(
            "TimeUtils.initialize() nie zostało wywołane. Użyj wersji z jawnie przekazanym AppConfig."
        )

        val appConfig = AppConfig.getInstance(context)
        Timber.d("Używanie przestarzałej metody getRevealDateMillis()")
        return appConfig.getBirthdayTimeMillis()
    }

    /**
     * Zwraca datę następnych urodzin w milisekundach.
     *
     * @param appConfig Instancja konfiguracji aplikacji
     * @return Czas w milisekundach
     */
    fun getNextBirthdayDateMillis(appConfig: AppConfig): Long {
        val currentTime = System.currentTimeMillis()
        val birthdayTime = getBirthdayTimeForCurrentYear(appConfig)

        return if (currentTime > birthdayTime) {
            getBirthdayTimeForYear(appConfig, Calendar.getInstance().get(Calendar.YEAR) + 1)
        } else {
            birthdayTime
        }
    }

    /** Pobiera datę urodzin dla bieżącego roku. */
    private fun getBirthdayTimeForCurrentYear(appConfig: AppConfig): Long {
        return getBirthdayTimeForYear(appConfig, Calendar.getInstance().get(Calendar.YEAR))
    }

    /** Pobiera datę urodzin dla określonego roku. */
    private fun getBirthdayTimeForYear(appConfig: AppConfig, year: Int): Long {
        val birthday = appConfig.getBirthdayDate()
        birthday.set(Calendar.YEAR, year)
        return birthday.timeInMillis
    }

    /**
     * Ulepszone formatowanie pozostałego czasu obsługujące długie timery (do
     * 24h). Automatycznie dostosowuje format w zależności od długości czasu.
     *
     * @param timeRemainingMillis Pozostały czas w milisekundach
     * @param compact Czy użyć kompaktowego formatu (np. "2h 30m" zamiast "2
     *    godziny, 30 minut")
     * @return Sformatowany tekst
     */
    fun formatRemainingTime(timeRemainingMillis: Long, compact: Boolean = false): String {
        if (timeRemainingMillis <= 0) {
            return if (compact) "0s" else "0 sekund"
        }

        val days = TimeUnit.MILLISECONDS.toDays(timeRemainingMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(timeRemainingMillis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemainingMillis) % 60

        return if (compact) {
            formatRemainingTimeCompact(days, hours, minutes, seconds)
        } else {
            formatRemainingTimeVerbose(days, hours, minutes, seconds)
        }
    }

    /** Kompaktowy format czasu dla UI (np. "2h 30m 15s"). */
    private fun formatRemainingTimeCompact(
        days: Long,
        hours: Long,
        minutes: Long,
        seconds: Long,
    ): String {
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (seconds > 0 || isEmpty()) append("${seconds}s")
        }.trim()
    }

    /** Szczegółowy format czasu dla powiadomień i logów. */
    private fun formatRemainingTimeVerbose(
        days: Long,
        hours: Long,
        minutes: Long,
        seconds: Long,
    ): String {
        return when {
            days > 0 -> String.format(
                Locale.getDefault(), "%d dni, %02d:%02d:%02d", days, hours, minutes, seconds
            )

            hours > 0 -> String.format(
                Locale.getDefault(), "%d dni, %02d:%02d:%02d", 0L, hours, minutes, seconds
            )

            else -> String.format(
                Locale.getDefault(), "%d dni, %02d:%02d:%02d", 0L, 0L, minutes, seconds
            )
        }
    }

    /**
     * Formatuje czas w formacie przyjaznym dla użytkownika. Automatycznie
     * wybiera najlepszą reprezentację.
     *
     * @param timeRemainingMillis Pozostały czas w milisekundach
     * @return Czytelny opis czasu (np. "2 godziny i 30 minut", "45 minut", "30
     *    sekund")
     */
    fun formatRemainingTimeHumanReadable(timeRemainingMillis: Long): String {
        if (timeRemainingMillis <= 0) return "Czas upłynął"

        val days = TimeUnit.MILLISECONDS.toDays(timeRemainingMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(timeRemainingMillis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemainingMillis) % 60

        return when {
            days > 0 -> {
                when {
                    hours > 0 -> "$days dni i $hours godzin"
                    else -> "$days dni"
                }
            }

            hours > 0 -> {
                when {
                    minutes > 0 -> "$hours godzin i $minutes minut"
                    else -> "$hours godzin"
                }
            }

            minutes > 0 -> {
                when {
                    seconds > 30 -> "$minutes minut"
                    else -> "$minutes minut"
                }
            }

            else -> "$seconds sekund"
        }
    }

    /**
     * Formatuje minuty na czytelny format dla ustawiania timera.
     *
     * @param minutes Liczba minut
     * @return Czytelny opis (np. "2h 30min", "45min", "1h")
     */
    fun formatTimerDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "${minutes}min"
            minutes == 60 -> "1h"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                "${hours}h ${remainingMinutes}min"
            }
        }
    }

    /**
     * Formatuje minuty na szczegółowy opis dla powiadomień.
     *
     * @param minutes Liczba minut
     * @return Szczegółowy opis (np. "2 godziny i 30 minut")
     */
    fun formatTimerDurationVerbose(minutes: Int): String {
        return when {
            minutes < 60 -> when (minutes) {
                1 -> "1 minutę"
                in 2..4 -> "$minutes minuty"
                else -> "$minutes minut"
            }

            minutes == 60 -> "1 godzinę"
            minutes % 60 == 0 -> {
                val hours = minutes / 60
                when (hours) {
                    in 2..4 -> "$hours godziny"
                    else -> "$hours godzin"
                }
            }

            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                val hoursText = when (hours) {
                    1 -> "1 godzinę"
                    in 2..4 -> "$hours godziny"
                    else -> "$hours godzin"
                }
                val minutesText = when (remainingMinutes) {
                    1 -> "1 minutę"
                    in 2..4 -> "$remainingMinutes minuty"
                    else -> "$remainingMinutes minut"
                }
                "$hoursText i $minutesText"
            }
        }
    }

    /** Sprawdza czy podana liczba minut jest prawidłowa dla timera. */
    fun isValidTimerDuration(minutes: Int): Boolean {
        return minutes in 1..1440 // Do 24 godzin
    }

    /** Konwertuje milisekundy na minuty z zaokrągleniem. */
    fun millisecondsToMinutes(milliseconds: Long): Int {
        return (milliseconds / 60000).toInt()
    }

    /** Konwertuje minuty na milisekundy. */
    fun minutesToMilliseconds(minutes: Int): Long {
        return minutes * 60000L
    }

    /**
     * Formatuje datę do czytelnej postaci dla logów w strefie czasowej
     * Warszawy.
     */
    fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).apply {
            timeZone = WARSAW_TIMEZONE
        }
        return dateFormat.format(date)
    }

    /** Pobiera instancję Calendar w strefie czasowej Warszawy. */
    fun getWarsawCalendar(): Calendar {
        return Calendar.getInstance(WARSAW_TIMEZONE)
    }

    /** Konwertuje zwykłą datę do daty w strefie czasowej Warszawy. */
    fun convertToWarsawTime(date: Date): Date {
        val warsawCalendar = getWarsawCalendar()
        warsawCalendar.time = date
        return warsawCalendar.time
    }

    /**
     * Oblicza procent ukończenia timera.
     *
     * @param initialMinutes Początkowa wartość timera w minutach
     * @param remainingMillis Pozostały czas w milisekundach
     * @return Procent ukończenia (0.0 - 1.0)
     */
    fun calculateTimerProgress(initialMinutes: Int, remainingMillis: Long): Float {
        if (initialMinutes <= 0) return 1.0f

        val initialMillis = minutesToMilliseconds(initialMinutes)
        val elapsed = initialMillis - remainingMillis

        return (elapsed.toFloat() / initialMillis.toFloat()).coerceIn(0.0f, 1.0f)
    }

    /** Formatuje czas w formacie MM:SS dla krótkich okresów. */
    fun formatShortTime(milliseconds: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}