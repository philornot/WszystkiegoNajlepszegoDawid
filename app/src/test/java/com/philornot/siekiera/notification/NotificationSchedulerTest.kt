package com.philornot.siekiera.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.philornot.siekiera.config.AppConfig
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.Calendar
import java.util.TimeZone

@RunWith(MockitoJUnitRunner.Silent::class)
class NotificationSchedulerTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAlarmManager: AlarmManager

    @Mock
    private lateinit var mockPendingIntent: PendingIntent

    @Mock
    private lateinit var mockAppConfig: AppConfig

    private lateinit var pendingIntentFactory: NotificationScheduler.PendingIntentFactory

    // Save the original factory to restore it after tests
    private lateinit var originalPendingIntentFactory: NotificationScheduler.PendingIntentFactory

    @Before
    fun setup() {
        // Save original factory
        originalPendingIntentFactory = NotificationScheduler.pendingIntentFactory

        // Setup mock context to return our mock AlarmManager
        `when`(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager)

        // Setup mock AppConfig to return a predetermined birthday date
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"))
        calendar.set(2025, Calendar.AUGUST, 24, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // This stubbing is actually used
        lenient().`when`(mockAppConfig.getBirthdayDate()).thenReturn(calendar)
        lenient().`when`(mockAppConfig.getBirthdayTimeMillis()).thenReturn(calendar.timeInMillis)
        lenient().`when`(mockAppConfig.isBirthdayNotificationEnabled()).thenReturn(true)

        // Create a mock PendingIntentFactory that returns our mockPendingIntent
        pendingIntentFactory = object : NotificationScheduler.PendingIntentFactory {
            override fun createBroadcastPendingIntent(
                context: Context,
                requestCode: Int,
                intent: Intent,
                flags: Int,
            ): PendingIntent {
                return mockPendingIntent
            }
        }

        // Assign our mock factory
        NotificationScheduler.pendingIntentFactory = pendingIntentFactory

        // Setup singleton AppConfig
        AppConfig.INSTANCE = mockAppConfig
    }

    @After
    fun tearDown() {
        // Restore original factory
        NotificationScheduler.pendingIntentFactory = originalPendingIntentFactory

        // Reset singleton
        AppConfig.INSTANCE = null
    }

    @Test
    fun `scheduleGiftRevealNotification sets exact alarm for correct date`() {
        // For Android 12 and above, we need to check canScheduleExactAlarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            `when`(mockAlarmManager.canScheduleExactAlarms()).thenReturn(true)
        }

        // Call the method under test
        NotificationScheduler.scheduleGiftRevealNotification(mockContext, mockAppConfig)

        // Verify that setExactAndAllowWhileIdle was called with correct parameters
        verify(mockAlarmManager).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, mockAppConfig.getBirthdayTimeMillis(), mockPendingIntent
        )
    }

    @Test
    fun `scheduleGiftRevealNotification uses deprecated version correctly`() {
        // Setup singleton instance
        AppConfig.INSTANCE = mockAppConfig

        // For Android 12 and above, we need to check canScheduleExactAlarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            `when`(mockAlarmManager.canScheduleExactAlarms()).thenReturn(true)
        }

        // Call the deprecated method
        @Suppress("DEPRECATION") NotificationScheduler.scheduleGiftRevealNotification(mockContext)

        // Verify that setExactAndAllowWhileIdle was called with correct parameters
        verify(mockAlarmManager).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, mockAppConfig.getBirthdayTimeMillis(), mockPendingIntent
        )
    }

    @Test
    fun `scheduleGiftRevealNotification handles Android 12 and above permissions`() {
        // Skip test if running on older Android
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        // Given that we don't have permission
        `when`(mockAlarmManager.canScheduleExactAlarms()).thenReturn(false)

        // Call the method under test
        NotificationScheduler.scheduleGiftRevealNotification(mockContext, mockAppConfig)

        // Should fall back to non-exact alarm
        verify(mockAlarmManager).set(
            AlarmManager.RTC_WAKEUP, mockAppConfig.getBirthdayTimeMillis(), mockPendingIntent
        )
    }
}