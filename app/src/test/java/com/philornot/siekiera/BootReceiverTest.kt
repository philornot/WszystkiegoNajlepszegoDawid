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
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.spy
import java.util.Calendar
import java.util.TimeZone

@RunWith(MockitoJUnitRunner.Silent::class) // Change to Silent runner to avoid unnecessary stubbing errors
class BootReceiverTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAppConfig: AppConfig

    private lateinit var bootReceiver: BootReceiver

    // Use a spy of BootReceiver to avoid direct static method calls to NotificationScheduler
    private lateinit var spyBootReceiver: BootReceiver

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Setup mock context
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        // Create receiver instance
        bootReceiver = BootReceiver()

        // Create a spy to avoid static method calls
        spyBootReceiver = spy(bootReceiver)

        // Set up singleton manually
        AppConfig.INSTANCE = mockAppConfig
    }

    @Test
    fun `onReceive ignores non-boot intents`() {
        // Given
        val intent = Intent("SOME_OTHER_ACTION")

        // When
        spyBootReceiver.onReceive(mockContext, intent)

        // Then - No interaction should happen with AppConfig
        verify(mockAppConfig, never()).isBirthdayNotificationEnabled()
    }

    @Test
    fun `onReceive processes boot intent but notifications disabled`() {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        `when`(mockAppConfig.isBirthdayNotificationEnabled()).thenReturn(false)

        // When
        spyBootReceiver.onReceive(mockContext, intent)

        // Then
        verify(mockAppConfig).isBirthdayNotificationEnabled()
        // No further processing should happen
        verify(mockAppConfig, never()).getBirthdayTimeMillis()
    }

    @Test
    fun `onReceive processes boot intent with notifications enabled and future date`() {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        `when`(mockAppConfig.isBirthdayNotificationEnabled()).thenReturn(true)

        // Set the birthday date to be in the future
        val futureDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        futureDate.add(Calendar.DAY_OF_YEAR, 1) // Tomorrow
        `when`(mockAppConfig.getBirthdayTimeMillis()).thenReturn(futureDate.timeInMillis)
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        // When
        spyBootReceiver.onReceive(mockContext, intent)

        // Then
        verify(mockAppConfig).isBirthdayNotificationEnabled()
        verify(mockAppConfig).getBirthdayTimeMillis()
        // We can't verify static NotificationScheduler call directly with standard Mockito
    }

    @Test
    fun `onReceive processes boot intent with notifications enabled but past date`() {
        // Given
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        `when`(mockAppConfig.isBirthdayNotificationEnabled()).thenReturn(true)

        // Set the birthday date to be in the past
        val pastDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        pastDate.add(Calendar.DAY_OF_YEAR, -1) // Yesterday
        `when`(mockAppConfig.getBirthdayTimeMillis()).thenReturn(pastDate.timeInMillis)
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        // When
        spyBootReceiver.onReceive(mockContext, intent)

        // Then
        verify(mockAppConfig).isBirthdayNotificationEnabled()
        verify(mockAppConfig).getBirthdayTimeMillis()
        // Notification should not be scheduled, date is in the past
    }
}