package com.philornot.siekiera.config

import android.content.Context
import android.content.res.Resources
import com.philornot.siekiera.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import java.util.Calendar
import java.util.TimeZone

@RunWith(MockitoJUnitRunner::class)
class AppConfigTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockResources: Resources

    private lateinit var appConfig: AppConfig

    @Before
    fun setup() {
        // Set up context and resources
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.resources).thenReturn(mockResources)

        // Configure mock resources to return example values
        `when`(mockResources.getInteger(R.integer.birthday_year)).thenReturn(2025)
        `when`(mockResources.getInteger(R.integer.birthday_month)).thenReturn(8) // August
        `when`(mockResources.getInteger(R.integer.birthday_day)).thenReturn(24)
        `when`(mockResources.getInteger(R.integer.birthday_hour)).thenReturn(8)
        `when`(mockResources.getInteger(R.integer.birthday_minute)).thenReturn(24)

        `when`(mockResources.getString(R.string.drive_folder_id)).thenReturn("test_folder_id")
        `when`(mockResources.getString(R.string.service_account_file)).thenReturn("test_service_account")
        `when`(mockResources.getString(R.string.daylio_file_name)).thenReturn("test.daylio")
        `when`(mockResources.getString(R.string.work_daily_file_check)).thenReturn("daily_file_check")

        `when`(mockResources.getBoolean(R.bool.enable_daily_file_check)).thenReturn(true)
        `when`(mockResources.getBoolean(R.bool.enable_birthday_notification)).thenReturn(true)
        `when`(mockResources.getBoolean(R.bool.verbose_logging)).thenReturn(true)

        `when`(mockResources.getInteger(R.integer.file_check_interval_hours)).thenReturn(24)

        // Create AppConfig instance
        appConfig = AppConfig(mockContext)
    }

    @Test
    fun `getBirthdayDate returns correct calendar instance`() {
        // Given (setup already configured in @Before)

        // When
        val birthdayDate = appConfig.getBirthdayDate()

        // Then
        assertEquals(TimeZone.getTimeZone("Europe/Warsaw"), birthdayDate.timeZone)
        assertEquals(2025, birthdayDate.get(Calendar.YEAR))
        assertEquals(7, birthdayDate.get(Calendar.MONTH)) // 0-based (7 = August)
        assertEquals(24, birthdayDate.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, birthdayDate.get(Calendar.HOUR_OF_DAY))
        assertEquals(24, birthdayDate.get(Calendar.MINUTE))
        assertEquals(0, birthdayDate.get(Calendar.SECOND))
        assertEquals(0, birthdayDate.get(Calendar.MILLISECOND))
    }

    @Test
    fun `getBirthdayTimeMillis returns same value as birthdayDate timeInMillis`() {
        // Given (setup already configured in @Before)

        // When
        val birthdayDate = appConfig.getBirthdayDate()
        val birthdayTimeMillis = appConfig.getBirthdayTimeMillis()

        // Then
        assertEquals(birthdayDate.timeInMillis, birthdayTimeMillis)
    }

    @Test
    fun `getDriveFolderId returns correct value`() {
        // Given (setup already configured in @Before)

        // When
        val folderId = appConfig.getDriveFolderId()

        // Then
        assertEquals("test_folder_id", folderId)
        verify(mockResources).getString(R.string.drive_folder_id)
    }

    @Test
    fun `getServiceAccountFileName returns correct value`() {
        // Given (setup already configured in @Before)

        // When
        val fileName = appConfig.getServiceAccountFileName()

        // Then
        assertEquals("test_service_account", fileName)
        verify(mockResources).getString(R.string.service_account_file)
    }

    @Test
    fun `getDaylioFileName returns correct value`() {
        // Given (setup already configured in @Before)

        // When
        val fileName = appConfig.getDaylioFileName()

        // Then
        assertEquals("test.daylio", fileName)
        verify(mockResources).getString(R.string.daylio_file_name)
    }

    @Test
    fun `isDailyFileCheckEnabled returns correct value`() {
        // Given (setup already configured in @Before)

        // When
        val enabled = appConfig.isDailyFileCheckEnabled()

        // Then
        assertTrue(enabled)
        verify(mockResources).getBoolean(R.bool.enable_daily_file_check)

        // Test with false value
        `when`(mockResources.getBoolean(R.bool.enable_daily_file_check)).thenReturn(false)
        assertFalse(appConfig.isDailyFileCheckEnabled())
    }

    @Test
    fun `isBirthdayNotificationEnabled returns correct value`() {
        // Given (setup already configured in @Before)

        // When
        val enabled = appConfig.isBirthdayNotificationEnabled()

        // Then
        assertTrue(enabled)
        verify(mockResources).getBoolean(R.bool.enable_birthday_notification)

        // Test with false value
        `when`(mockResources.getBoolean(R.bool.enable_birthday_notification)).thenReturn(false)
        assertFalse(appConfig.isBirthdayNotificationEnabled())
    }

    @Test
    fun `getFileCheckIntervalHours returns correct value`() {
        // Given (setup already configured in @Before)

        // When
        val interval = appConfig.getFileCheckIntervalHours()

        // Then
        assertEquals(24, interval)
        verify(mockResources).getInteger(R.integer.file_check_interval_hours)
    }

    @Test
    fun `isVerboseLoggingEnabled returns correct value`() {
        // Given (setup already configured in @Before)

        // When
        val enabled = appConfig.isVerboseLoggingEnabled()

        // Then
        assertTrue(enabled)
        verify(mockResources).getBoolean(R.bool.verbose_logging)

        // Test with false value
        `when`(mockResources.getBoolean(R.bool.verbose_logging)).thenReturn(false)
        assertFalse(appConfig.isVerboseLoggingEnabled())
    }

    @Test
    fun `getDailyFileCheckWorkName returns correct value`() {
        // Given (setup already configured in @Before)

        // When
        val workName = appConfig.getDailyFileCheckWorkName()

        // Then
        assertEquals("daily_file_check", workName)
        verify(mockResources).getString(R.string.work_daily_file_check)
    }

    @Test
    fun `getInstance returns singleton instance`() {
        // Given
        AppConfig.INSTANCE = null

        // When
        val instance1 = AppConfig.getInstance(mockContext)
        val instance2 = AppConfig.getInstance(mockContext)

        // Then
        assertEquals(instance1, instance2) // Should return the same instance
        assertEquals(instance1, AppConfig.INSTANCE)
    }
}