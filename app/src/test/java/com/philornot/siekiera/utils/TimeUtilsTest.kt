package com.philornot.siekiera.utils

import com.philornot.siekiera.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class TimeUtilsTest {

    @Mock
    private lateinit var mockAppConfig: AppConfig

    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Before
    fun setup() {
        // Setup the mock configuration
        val calendar = Calendar.getInstance(warsawTimeZone)
        calendar.set(2025, Calendar.AUGUST, 24, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Usuwamy nieużywany stubbing
        // `when`(mockAppConfig.getBirthdayDate()).thenReturn(calendar)

        `when`(mockAppConfig.getBirthdayTimeMillis()).thenReturn(calendar.timeInMillis)

        // Set our mock as the singleton instance
        AppConfig.INSTANCE = mockAppConfig
    }

    @Test
    fun `getCalendarForDateTime creates correct calendar instance`() {
        // Given
        val year = 2025
        val month = Calendar.AUGUST
        val day = 24
        val hour = 12
        val minute = 30
        val second = 45

        // When
        val calendar = TimeUtils.getCalendarForDateTime(year, month, day, hour, minute, second)

        // Then
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(minute, calendar.get(Calendar.MINUTE))
        assertEquals(second, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
        assertEquals("Europe/Warsaw", calendar.timeZone.id)
    }

    @Test
    fun `getRevealDateMillis with appConfig returns correct timestamp`() {
        // Given
        val expected = mockAppConfig.getBirthdayTimeMillis()

        // When
        val result = TimeUtils.getRevealDateMillis(mockAppConfig)

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun `getRevealDateMillis deprecated version returns correct timestamp`() {
        // Given
        val expected = mockAppConfig.getBirthdayTimeMillis()

        // When
        @Suppress("DEPRECATION") val result = TimeUtils.getRevealDateMillis()

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun `formatRemainingTime formats time correctly for days hours minutes seconds`() {
        // Given
        val days = 5L
        val hours = 6L
        val minutes = 30L
        val seconds = 15L

        val milliseconds =
            TimeUnit.DAYS.toMillis(days) + TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(
                minutes
            ) + TimeUnit.SECONDS.toMillis(seconds)

        // When
        val formatted = TimeUtils.formatRemainingTime(milliseconds)

        // Then
        assertEquals("5 dni, 06:30:15", formatted)
    }

    @Test
    fun `formatRemainingTime formats time correctly for zero time`() {
        // Given
        val milliseconds = 0L

        // When
        val formatted = TimeUtils.formatRemainingTime(milliseconds)

        // Then
        assertEquals("0 dni, 00:00:00", formatted)
    }

    @Test
    fun `formatDate formats date in Warsaw timezone`() {
        // Given
        val calendar = Calendar.getInstance(warsawTimeZone)
        calendar.set(2025, Calendar.JANUARY, 15, 14, 30, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val date = calendar.time

        // When
        val formatted = TimeUtils.formatDate(date)

        // Then
        val expected = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).apply {
            timeZone = warsawTimeZone
        }.format(date)
        assertEquals(expected, formatted)
    }

    @Test
    fun `getWarsawCalendar returns calendar with correct timezone`() {
        // When
        val calendar = TimeUtils.getWarsawCalendar()

        // Then
        assertEquals("Europe/Warsaw", calendar.timeZone.id)
    }

    @Test
    fun `convertToWarsawTime converts date to Warsaw timezone`() {
        // Given
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.set(2025, Calendar.JANUARY, 15, 12, 0, 0)
        utcCalendar.set(Calendar.MILLISECOND, 0)
        val utcDate = utcCalendar.time

        // When
        val warsawDate = TimeUtils.convertToWarsawTime(utcDate)

        // Then
        val warsawCalendar = Calendar.getInstance(warsawTimeZone)
        warsawCalendar.time = warsawDate

        // The time should be the same, just in different timezone
        // UTC 12:00 should be Warsaw 13:00 in winter (UTC+1)
        assertEquals(13, warsawCalendar.get(Calendar.HOUR_OF_DAY))
    }
}