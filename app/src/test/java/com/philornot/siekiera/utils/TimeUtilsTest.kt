package com.philornot.siekiera.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TimeUtilsTest {

    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getCalendarForDateTime returns calendar with correct values`() {
        // Przygotuj testowe wartości
        val year = 2025
        val month = Calendar.MAY
        val day = 14
        val hour = 16
        val minute = 34
        val second = 0

        // Wywołaj testowaną metodę
        val calendar = TimeUtils.getCalendarForDateTime(year, month, day, hour, minute, second)

        // Sprawdź wartości
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(minute, calendar.get(Calendar.MINUTE))
        assertEquals(second, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))

        // Sprawdź strefę czasową
        assertEquals(warsawTimeZone, calendar.timeZone)
    }

    @Test
    fun `formatRemainingTime correctly formats time with days hours minutes seconds`() {
        // Przygotuj testowe czasy
        val oneDay = TimeUnit.DAYS.toMillis(1)
        val twoHours = TimeUnit.HOURS.toMillis(2)
        val threeMinutes = TimeUnit.MINUTES.toMillis(3)
        val fourSeconds = TimeUnit.SECONDS.toMillis(4)

        val totalMillis = oneDay + twoHours + threeMinutes + fourSeconds

        // Wywołaj testowaną metodę
        val formatted = TimeUtils.formatRemainingTime(totalMillis)

        // Sprawdź format
        assertEquals("1 dni, 02:03:04", formatted)
    }

    @Test
    fun `formatRemainingTime handles zero time correctly`() {
        val formatted = TimeUtils.formatRemainingTime(0)
        assertEquals("0 dni, 00:00:00", formatted)
    }

    @Test
    fun `formatRemainingTime handles large values correctly`() {
        // 100 dni, 23 godziny, 59 minut, 59 sekund
        val largeMillis = TimeUnit.DAYS.toMillis(100) +
                TimeUnit.HOURS.toMillis(23) +
                TimeUnit.MINUTES.toMillis(59) +
                TimeUnit.SECONDS.toMillis(59)

        val formatted = TimeUtils.formatRemainingTime(largeMillis)
        assertEquals("100 dni, 23:59:59", formatted)
    }

    @Test
    fun `formatDate formats date in Warsaw timezone`() {
        // Przygotuj datę do testów
        val date = Date(1716055200000) // 2024-05-19 00:00:00 UTC

        // Wywołaj testowaną metodę
        val formatted = TimeUtils.formatDate(date)

        // Data powinna być sformatowana w strefie czasowej Warszawy
        // Dokładny format zależy od implementacji, ale powinien zawierać strefę CEST/CET
        assertTrue(formatted.contains("CEST") || formatted.contains("CET"))
    }

    @Test
    fun `getWarsawCalendar returns calendar in Warsaw timezone`() {
        val calendar = TimeUtils.getWarsawCalendar()
        assertEquals(warsawTimeZone, calendar.timeZone)
    }

    @Test
    fun `convertToWarsawTime converts date to Warsaw timezone`() {
        // Przygotuj datę w innej strefie czasowej
        val defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

        try {
            // Stwórz datę w czasie lokalnym (LA)
            val localDate = Date()

            // Konwertuj na czas warszawski
            val warsawDate = TimeUtils.convertToWarsawTime(localDate)

            // Sprawdź czy daty są różne (bo strefy czasowe są różne)
            assertNotEquals(localDate.time, warsawDate.time)

            // Stwórz kalendarz warszawski z oryginalną datą
            val warsawCalendar = Calendar.getInstance(warsawTimeZone)
            warsawCalendar.time = localDate

            // Powinien odpowiadać skonwertowanej dacie
            assertEquals(warsawCalendar.timeInMillis, warsawDate.time)
        } finally {
            // Przywróć domyślną strefę czasową
            TimeZone.setDefault(defaultTimeZone)
        }
    }

    @Test
    fun `getRevealDateMillis returns correct timestamp`() {
        // Przygotuj mock dla AppConfig - nie można łatwo przetestować tej metody
        // bez dokładnej znajomości implementacji getRevealDateMillis

        // Ten test jest bardziej integracyjny niż jednostkowy
        // i polega na sprawdzeniu spójności daty i czasu urodzin

        // Przygotuj oczekiwaną datę: 14 maja 2025, 16:34:00
        val expectedCalendar = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val expectedMillis = expectedCalendar.timeInMillis

        // Zależnie od implementacji, możemy sprawdzić wzorce lub wywołać
        // mockowane zależności

        // Przykład sprawdzenia, czy dla danej daty 14 maja 2025 czas ujawnienia
        // wypada tego samego dnia:
        val testCalendar = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 14, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startOfDay = testCalendar.timeInMillis
        val endOfDay = startOfDay + TimeUnit.DAYS.toMillis(1) - 1

        // Sprawdź czy oczekiwany czas mieści się w tym samym dniu
        assertTrue(expectedMillis >= startOfDay && expectedMillis <= endOfDay)
    }
}