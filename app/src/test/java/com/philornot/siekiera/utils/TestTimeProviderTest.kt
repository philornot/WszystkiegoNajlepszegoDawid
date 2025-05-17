package com.philornot.siekiera.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class TestTimeProviderTest {

    @Test
    fun `getCurrentTimeMillis returns system time by default`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()
        val systemTime = System.currentTimeMillis()

        // Wywołanie
        val providedTime = timeProvider.getCurrentTimeMillis()

        // Weryfikacja
        // Tolerujemy małą różnicę ze względu na czas wykonania
        val diff = abs(providedTime - systemTime)
        assertTrue("Różnica czasu powinna być bardzo mała: $diff ms", diff < 100)
    }

    @Test
    fun `getCurrentTimeMillis returns mocked time after setCurrentTimeMillis`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()
        val mockedTime = 1716055200000L // 2024-05-19 00:00:00 UTC

        // Wywołanie
        timeProvider.setCurrentTimeMillis(mockedTime)
        val providedTime = timeProvider.getCurrentTimeMillis()

        // Weryfikacja
        assertEquals(mockedTime, providedTime)
    }

    @Test
    fun `resetToSystemTime clears mocked time`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()
        val mockedTime = 1716055200000L // 2024-05-19 00:00:00 UTC

        // Ustaw czas symulowany
        timeProvider.setCurrentTimeMillis(mockedTime)
        assertEquals(mockedTime, timeProvider.getCurrentTimeMillis())

        // Wywołanie resetToSystemTime
        timeProvider.resetToSystemTime()

        // Weryfikacja - powinien wrócić do czasu systemowego
        val systemTime = System.currentTimeMillis()
        val providedTime = timeProvider.getCurrentTimeMillis()

        // Tolerujemy małą różnicę ze względu na czas wykonania
        val diff = abs(providedTime - systemTime)
        assertTrue("Różnica czasu powinna być bardzo mała: $diff ms", diff < 100)
    }

    @Test
    fun `multiple calls to setCurrentTimeMillis update the time`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()
        val firstMockedTime = 1716055200000L // 2024-05-19 00:00:00 UTC
        val secondMockedTime = 1716141600000L // 2024-05-20 00:00:00 UTC

        // Pierwsze ustawienie
        timeProvider.setCurrentTimeMillis(firstMockedTime)
        assertEquals(firstMockedTime, timeProvider.getCurrentTimeMillis())

        // Drugie ustawienie
        timeProvider.setCurrentTimeMillis(secondMockedTime)
        assertEquals(secondMockedTime, timeProvider.getCurrentTimeMillis())
    }

    @Test
    fun `TimeSimulator setSimulatedTime correctly configures provider`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()

        // Wywołanie TimeSimulator
        TimeSimulator.setSimulatedTime(
            timeProvider,
            2025, Calendar.MAY, 14, 16, 34, 0
        )

        // Oblicz oczekiwany czas w milisekundach
        val expectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Weryfikacja
        assertEquals(expectedCalendar.timeInMillis, timeProvider.getCurrentTimeMillis())
    }

    @Test
    fun `TimeSimulator resetToSystemTime correctly resets provider`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()

        // Ustaw czas symulowany
        TimeSimulator.setSimulatedTime(
            timeProvider,
            2025, Calendar.MAY, 14, 16, 34, 0
        )

        // Wywołanie resetToSystemTime przez TimeSimulator
        TimeSimulator.resetToSystemTime(timeProvider)

        // Weryfikacja - powinien wrócić do czasu systemowego
        val systemTime = System.currentTimeMillis()
        val providedTime = timeProvider.getCurrentTimeMillis()

        // Tolerujemy małą różnicę ze względu na czas wykonania
        val diff = abs(providedTime - systemTime)
        assertTrue("Różnica czasu powinna być bardzo mała: $diff ms", diff < 100)
    }

    @Test
    fun `TimeSimulator simulateBeforeRevealDate sets correct date`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()

        // Wywołanie
        TimeSimulator.simulateBeforeRevealDate(timeProvider)

        // Oblicz oczekiwany czas - 23 sierpnia 2025, 12:00
        val expectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.AUGUST, 23, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Weryfikacja
        assertEquals(expectedCalendar.timeInMillis, timeProvider.getCurrentTimeMillis())
    }

    @Test
    fun `TimeSimulator simulateAfterRevealDate sets correct date`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()

        // Wywołanie
        TimeSimulator.simulateAfterRevealDate(timeProvider)

        // Oblicz oczekiwany czas - 24 sierpnia 2025, 12:00
        val expectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.AUGUST, 24, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Weryfikacja
        assertEquals(expectedCalendar.timeInMillis, timeProvider.getCurrentTimeMillis())
    }

    @Test
    fun `TimeSimulator simulateExactRevealDate sets correct date`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()

        // Wywołanie
        TimeSimulator.simulateExactRevealDate(timeProvider)

        // Oblicz oczekiwany czas - 24 sierpnia 2025, 00:00:01
        val expectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.AUGUST, 24, 0, 0, 1)
            set(Calendar.MILLISECOND, 0)
        }

        // Weryfikacja
        assertEquals(expectedCalendar.timeInMillis, timeProvider.getCurrentTimeMillis())
    }

    @Test
    fun `simulateMilestones covers all key dates`() {
        // Ten test sprawdza, czy możemy zasymulować wszystkie ważne momenty w czasie
        val timeProvider = TestTimeProvider()

        // 1. Czas na długo przed dniem urodzin
        // Ustaw na 1 miesiąc przed
        val oneMonthBefore = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.APRIL, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }
        timeProvider.setCurrentTimeMillis(oneMonthBefore.timeInMillis)
        assertEquals(oneMonthBefore.timeInMillis, timeProvider.getCurrentTimeMillis())

        // 2. Czas tuż przed dniem urodzin (1 sekunda)
        val oneBefore = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.MAY, 14, 16, 33, 59)
            set(Calendar.MILLISECOND, 0)
        }
        timeProvider.setCurrentTimeMillis(oneBefore.timeInMillis)
        assertEquals(oneBefore.timeInMillis, timeProvider.getCurrentTimeMillis())

        // 3. Dokładnie w momencie urodzin
        val exact = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }
        timeProvider.setCurrentTimeMillis(exact.timeInMillis)
        assertEquals(exact.timeInMillis, timeProvider.getCurrentTimeMillis())

        // 4. Czas tuż po urodzinach (1 sekunda)
        val oneAfter = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 1)
            set(Calendar.MILLISECOND, 0)
        }
        timeProvider.setCurrentTimeMillis(oneAfter.timeInMillis)
        assertEquals(oneAfter.timeInMillis, timeProvider.getCurrentTimeMillis())

        // 5. Czas długo po urodzinach
        val oneMonthAfter = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw")).apply {
            set(2025, Calendar.JUNE, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }
        timeProvider.setCurrentTimeMillis(oneMonthAfter.timeInMillis)
        assertEquals(oneMonthAfter.timeInMillis, timeProvider.getCurrentTimeMillis())

        // Reset na koniec
        timeProvider.resetToSystemTime()
    }

    @Test
    fun `handles real-time scenarios correctly`() {
        // Przygotowanie
        val timeProvider = TestTimeProvider()
        val mockedTime = System.currentTimeMillis() + 5000 // 5 sekund w przyszłość

        // Ustawienie czasu w przyszłości
        timeProvider.setCurrentTimeMillis(mockedTime)

        // Sprawdzenie czy czas się nie zmienia samoistnie
        Thread.sleep(100) // Odczekaj chwilę

        // Czas powinien nadal być takim, jaki ustawiliśmy (nie zmienia się jak rzeczywisty)
        assertEquals(mockedTime, timeProvider.getCurrentTimeMillis())
    }

    @Test
    fun `realTimeProvider is actually getting real time`() {
        // Przygotowanie
        val realProvider = RealTimeProvider()

        // Odczytaj czas systemowy
        val systemTime1 = System.currentTimeMillis()

        // Odczytaj czas z providera
        val providerTime = realProvider.getCurrentTimeMillis()

        // Odczytaj ponownie czas systemowy
        val systemTime2 = System.currentTimeMillis()

        // Weryfikacja - providerTime powinien być pomiędzy dwoma odczytami czasu systemowego
        assertTrue(
            "Provider powinien zwracać rzeczywisty czas systemowy",
            providerTime >= systemTime1 && providerTime <= systemTime2
        )
    }
}