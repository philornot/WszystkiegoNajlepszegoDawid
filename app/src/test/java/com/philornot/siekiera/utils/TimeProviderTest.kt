package com.philornot.siekiera.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

class TimeProviderTest {

    @Test
    fun `RealTimeProvider returns system time`() {
        // Given
        val realTimeProvider = RealTimeProvider()
        val beforeTime = System.currentTimeMillis()

        // When
        val providedTime = realTimeProvider.getCurrentTimeMillis()
        val afterTime = System.currentTimeMillis()

        // Then
        // The provided time should be between before and after
        assertTrue(providedTime >= beforeTime)
        assertTrue(providedTime <= afterTime)
    }

    @Test
    fun `TestTimeProvider returns mocked time when set`() {
        // Given
        val testTimeProvider = TestTimeProvider()
        val mockedTime = 1234567890L

        // When
        testTimeProvider.setCurrentTimeMillis(mockedTime)
        val providedTime = testTimeProvider.getCurrentTimeMillis()

        // Then
        assertEquals(mockedTime, providedTime)
    }

    @Test
    fun `TestTimeProvider returns system time when not set`() {
        // Given
        val testTimeProvider = TestTimeProvider()
        val beforeTime = System.currentTimeMillis()

        // When
        val providedTime = testTimeProvider.getCurrentTimeMillis()
        val afterTime = System.currentTimeMillis()

        // Then
        // The provided time should be between before and after
        assertTrue(providedTime >= beforeTime)
        assertTrue(providedTime <= afterTime)
    }

    @Test
    fun `TestTimeProvider resets to system time`() {
        // Given
        val testTimeProvider = TestTimeProvider()
        val mockedTime = 1234567890L

        // When
        testTimeProvider.setCurrentTimeMillis(mockedTime)
        assertEquals(mockedTime, testTimeProvider.getCurrentTimeMillis())

        testTimeProvider.resetToSystemTime()
        val providedTime = testTimeProvider.getCurrentTimeMillis()
        val systemTime = System.currentTimeMillis()

        // Then
        // The provided time should be very close to system time
        assertTrue(abs(providedTime - systemTime) < 100) // Within 100ms
    }
}

class TimeSimulatorTest {

    @Test
    fun `setSimulatedTime sets correct time in provider`() {
        // Given
        val testTimeProvider = TestTimeProvider()
        val year = 2025
        val month = Calendar.AUGUST
        val day = 24
        val hour = 12
        val minute = 34
        val second = 56

        // When
        TimeSimulator.setSimulatedTime(
            testTimeProvider, year, month, day, hour, minute, second
        )

        // Then
        val time = testTimeProvider.getCurrentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        calendar.timeInMillis = time

        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(minute, calendar.get(Calendar.MINUTE))
        assertEquals(second, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `resetToSystemTime resets provider time`() {
        // Given
        val testTimeProvider = TestTimeProvider()
        TimeSimulator.setSimulatedTime(
            testTimeProvider, 2025, Calendar.AUGUST, 24, 12, 34, 56
        )

        // When
        TimeSimulator.resetToSystemTime(testTimeProvider)

        // Then
        val providedTime = testTimeProvider.getCurrentTimeMillis()
        val systemTime = System.currentTimeMillis()

        // The provided time should be very close to system time
        assertTrue(abs(providedTime - systemTime) < 100) // Within 100ms
    }

    @Test
    fun `simulateBeforeRevealDate sets time before birthday`() {
        // Given
        val testTimeProvider = TestTimeProvider()

        // When
        TimeSimulator.simulateBeforeRevealDate(testTimeProvider)

        // Then
        val time = testTimeProvider.getCurrentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        calendar.timeInMillis = time

        assertEquals(2025, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, calendar.get(Calendar.MONTH))
        assertEquals(23, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
    }

    @Test
    fun `simulateAfterRevealDate sets time after birthday`() {
        // Given
        val testTimeProvider = TestTimeProvider()

        // When
        TimeSimulator.simulateAfterRevealDate(testTimeProvider)

        // Then
        val time = testTimeProvider.getCurrentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        calendar.timeInMillis = time

        assertEquals(2025, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, calendar.get(Calendar.MONTH))
        assertEquals(24, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
    }

    @Test
    fun `simulateExactRevealDate sets time at birthday`() {
        // Given
        val testTimeProvider = TestTimeProvider()

        // When
        TimeSimulator.simulateExactRevealDate(testTimeProvider)

        // Then
        val time = testTimeProvider.getCurrentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        calendar.timeInMillis = time

        assertEquals(2025, calendar.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, calendar.get(Calendar.MONTH))
        assertEquals(24, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(1, calendar.get(Calendar.SECOND))
    }
}