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
 * Pomocnicze metody do pracy z czasem w aplikacji. Używa strefy czasowej
 * Warszawy dla wszystkich operacji czasowych.
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
     * Zwraca czas w milisekundach dla daty odsłonięcia prezentu. Teraz pobiera
     * datę z konfiguracji.
     *
     * @param appConfig Instancja konfiguracji aplikacji
     * @return Czas w milisekundach
     */
    fun getRevealDateMillis(appConfig: AppConfig): Long {
        return appConfig.getBirthdayTimeMillis()
    }

    /**
     * Metoda kompatybilności wstecznej bez parametru appConfig. Wymaga
     * kontekstu aplikacji przez AppConfig. Ta metoda istnieje tylko dla
     * kompatybilności z istniejącym kodem.
     *
     * @throws IllegalStateException jeśli nie można uzyskać instancji
     *    AppConfig
     */
    @Deprecated(
        "Użyj wersji z jawnie przekazanym AppConfig", ReplaceWith("getRevealDateMillis(appConfig)")
    )
    fun getRevealDateMillis(): Long {
        // Wykorzystanie zapisanego kontekstu aplikacji
        val context = appContext ?: throw IllegalStateException(
            "TimeUtils.initialize() nie zostało wywołane. Użyj wersji z jawnie przekazanym AppConfig."
        )

        val appConfig = AppConfig.getInstance(context)
        Timber.d("Używanie przestarzałej metody getRevealDateMillis()")
        return appConfig.getBirthdayTimeMillis()
    }

    /**
     * Formatuje pozostały czas do wskazanej daty w postaci czytelnej dla
     * użytkownika.
     *
     * @param timeRemainingMillis Pozostały czas w milisekundach
     * @return Sformatowany tekst w formacie "X dni, HH:MM:SS"
     */
    fun formatRemainingTime(timeRemainingMillis: Long): String {
        // Oblicz dni, godziny, minuty i sekundy
        val days = TimeUnit.MILLISECONDS.toDays(timeRemainingMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(timeRemainingMillis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeRemainingMillis) % 60

        // Formatuj jako "X dni, HH:MM:SS"
        return String.format(
            Locale.getDefault(), "%d dni, %02d:%02d:%02d", days, hours, minutes, seconds
        )
    }

    /**
     * Formatuje datę do czytelnej postaci dla logów w strefie czasowej
     * Warszawy.
     *
     * @param date Data do sformatowania
     * @return Sformatowany tekst daty
     */
    fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).apply {
            timeZone = WARSAW_TIMEZONE
        }
        return dateFormat.format(date)
    }

    /**
     * Pobiera instancję Calendar w strefie czasowej Warszawy.
     *
     * @return Obiekt Calendar w strefie czasowej Warszawy
     */
    fun getWarsawCalendar(): Calendar {
        return Calendar.getInstance(WARSAW_TIMEZONE)
    }

    /**
     * Konwertuje zwykłą datę do daty w strefie czasowej Warszawy.
     *
     * @param date Data wejściowa
     * @return Data w strefie czasowej Warszawy
     */
    fun convertToWarsawTime(date: Date): Date {
        val warsawCalendar = getWarsawCalendar()
        warsawCalendar.time = date
        return warsawCalendar.time
    }
}