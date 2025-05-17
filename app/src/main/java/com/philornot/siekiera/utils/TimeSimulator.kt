package com.philornot.siekiera.utils

import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Narzędzie do symulowania określonej daty w aplikacji dla celów
 * testowych.
 *
 * Używa TestTimeProvider do dostarczania symulowanego czasu do komponentów
 * aplikacji. Przeznaczone do użycia w testach.
 */
class TimeSimulator {

    companion object {
        // Strefa czasowa Warszawy
        private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

        /**
         * Ustawia symulowany czas w podanym dostawcy czasu w strefie czasowej
         * Warszawy.
         *
         * @param timeProvider Dostawca czasu do zmodyfikowania
         * @param year Rok do symulacji
         * @param month Miesiąc do symulacji (używaj stałych Calendar.JANUARY,
         *    Calendar.FEBRUARY, itd.)
         * @param day Dzień miesiąca do symulacji
         * @param hour Godzina dnia do symulacji (opcjonalnie, domyślnie 0)
         * @param minute Minuta do symulacji (opcjonalnie, domyślnie 0)
         * @param second Sekunda do symulacji (opcjonalnie, domyślnie 0)
         */
        fun setSimulatedTime(
            timeProvider: TestTimeProvider,
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
        ) {
            val calendar = Calendar.getInstance(WARSAW_TIMEZONE).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, second)
                set(Calendar.MILLISECOND, 0)
            }

            timeProvider.setCurrentTimeMillis(calendar.timeInMillis)
            Timber.d("Ustawiono symulowany czas: ${TimeUtils.formatDate(calendar.time)}")
        }

        /**
         * Resetuje symulowany czas do rzeczywistego czasu systemowego.
         *
         * @param timeProvider Dostawca czasu do zresetowania
         */
        fun resetToSystemTime(timeProvider: TestTimeProvider) {
            timeProvider.resetToSystemTime()
            Timber.d("Zresetowano do czasu systemowego: ${TimeUtils.formatDate(Date())}")
        }

        /**
         * Symuluje datę przed datą ujawnienia prezentu (23 sierpnia 2025, 12:00
         * czasu warszawskiego)
         *
         * @param timeProvider Dostawca czasu do zmodyfikowania
         */
        fun simulateBeforeRevealDate(timeProvider: TestTimeProvider) {
            setSimulatedTime(
                timeProvider, 2025, Calendar.AUGUST, 23, 12, 0, 0
            )
        }

        /**
         * Symuluje datę po dacie ujawnienia prezentu (24 sierpnia 2025, 12:00
         * czasu warszawskiego)
         *
         * @param timeProvider Dostawca czasu do zmodyfikowania
         */
        fun simulateAfterRevealDate(timeProvider: TestTimeProvider) {
            setSimulatedTime(
                timeProvider, 2025, Calendar.AUGUST, 24, 12, 0, 0
            )
        }

        /**
         * Symuluje datę dokładnie w momencie ujawnienia prezentu (24 sierpnia
         * 2025, 00:00:01 czasu warszawskiego)
         *
         * @param timeProvider Dostawca czasu do zmodyfikowania
         */
        fun simulateExactRevealDate(timeProvider: TestTimeProvider) {
            setSimulatedTime(
                timeProvider, 2025, Calendar.AUGUST, 24, 0, 0, 1
            )
        }
    }
}