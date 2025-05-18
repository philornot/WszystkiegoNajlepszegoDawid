package com.philornot.siekiera

import android.content.Context
import android.content.Intent
import com.philornot.siekiera.config.AppConfig
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.Calendar
import java.util.TimeZone

@RunWith(MockitoJUnitRunner::class)
class BootReceiverTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAppConfig: AppConfig

    private lateinit var bootReceiver: BootReceiver

    @Before
    fun setup() {
        // Set up context mock
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        // Set up AppConfig mock with default values
        // IMPORTANT: Make sure the mocks are set up properly for each test
        `when`(mockAppConfig.isBirthdayNotificationEnabled()).thenReturn(false)

        // Set the mock as the singleton instance
        AppConfig.INSTANCE = mockAppConfig

        // Create instance of the component to test
        bootReceiver = BootReceiver()
    }

    @Test
    fun `onReceive ignores non-boot intents`() {
        // Given
        val intent = Intent("SOME_OTHER_ACTION")

        // When
        bootReceiver.onReceive(mockContext, intent)

        // Then - Notification scheduling should not happen
        verify(mockAppConfig, never()).isBirthdayNotificationEnabled()
    }

    @Test
    fun `onReceive processes boot intent with notifications enabled and future date`() {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Configure AppConfig to return that notifications are enabled
        `when`(mockAppConfig.isBirthdayNotificationEnabled()).thenReturn(true)

        // Set the birthday date to be in the future
        val futureDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        futureDate.add(Calendar.DAY_OF_YEAR, 1) // Tomorrow
        `when`(mockAppConfig.getBirthdayTimeMillis()).thenReturn(futureDate.timeInMillis)

        // When
        bootReceiver.onReceive(mockContext, intent)

        // Then
        verify(mockAppConfig).isBirthdayNotificationEnabled()
        verify(mockAppConfig).getBirthdayTimeMillis()
    }

    @Test
    fun `onReceive processes boot intent with notifications enabled but past date`() {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Configure AppConfig to return that notifications are enabled
        `when`(mockAppConfig.isBirthdayNotificationEnabled()).thenReturn(true)

        // Set the birthday date to be in the past
        val pastDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        pastDate.add(Calendar.DAY_OF_YEAR, -1) // Yesterday
        `when`(mockAppConfig.getBirthdayTimeMillis()).thenReturn(pastDate.timeInMillis)

        // When
        bootReceiver.onReceive(mockContext, intent)

        // Then
        verify(mockAppConfig).isBirthdayNotificationEnabled()
        verify(mockAppConfig).getBirthdayTimeMillis()
    }

    @Test
    fun `onReceive processes boot intent but notifications disabled`() {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Configure AppConfig to return that notifications are disabled
        `when`(mockAppConfig.isBirthdayNotificationEnabled()).thenReturn(false)

        // When
        bootReceiver.onReceive(mockContext, intent)

        // Then
        verify(mockAppConfig).isBirthdayNotificationEnabled()
        // No further processing should happen
        verify(mockAppConfig, never()).getBirthdayTimeMillis()
    }
}