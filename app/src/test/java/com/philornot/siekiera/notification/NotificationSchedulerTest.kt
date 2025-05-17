// Fixed version of NotificationSchedulerTest.kt
package com.philornot.siekiera.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.philornot.siekiera.config.AppConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Calendar
import java.util.TimeZone

/**
 * Testy dla NotificationScheduler - sprawdzają czy powiadomienia są
 * planowane poprawnie
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S]) // Android 12
class NotificationSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var mockPendingIntent: PendingIntent
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var appConfig: AppConfig

    // Domyślna implementacja interfejsu PendingIntentFactory do testów
    private class TestPendingIntentFactory : NotificationScheduler.PendingIntentFactory {
        override fun createBroadcastPendingIntent(
            context: Context,
            requestCode: Int,
            intent: Intent,
            flags: Int,
        ): PendingIntent {
            return mockk(relaxed = true)
        }
    }

    // Strefa czasowa Warszawy
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)
        mockPendingIntent = mockk(relaxed = true)
        appConfig = mockk(relaxed = true)

        // Ustaw domyślną datę urodzin - 14 maja 2025, 16:34
        val birthdayCalendar = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }

        every { appConfig.getBirthdayTimeMillis() } returns birthdayCalendar.timeInMillis
        every { appConfig.getBirthdayDate() } returns birthdayCalendar
        every { appConfig.isBirthdayNotificationEnabled() } returns true
        every { appConfig.isVerboseLoggingEnabled() } returns false

        // Utworzenie i podmiana fabryki PendingIntent na wersję testową
        val testFactory = mockk<NotificationScheduler.PendingIntentFactory>()
        every {
            testFactory.createBroadcastPendingIntent(
                any(), any(), any(), any()
            )
        } returns mockPendingIntent

        // Przypisanie testowej fabryki do klasy NotificationScheduler
        val field = NotificationScheduler::class.java.getDeclaredField("pendingIntentFactory")
        field.isAccessible = true

        // Dla pól statycznych musimy usunąć modyfikator final
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

        field.set(null, testFactory)

        // Dla Android 12+ ustawienie możliwości dokładnych alarmów w ShadowAlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Używając refleksji jako obejście dla braku metody setCanScheduleExactAlarms w nowszych wersjach Robolectric
            try {
                val canScheduleMethod = ShadowAlarmManager::class.java.getDeclaredMethod(
                    "setCanScheduleExactAlarms",
                    Boolean::class.java
                )
                canScheduleMethod.isAccessible = true
                canScheduleMethod.invoke(shadowAlarmManager, true)
            } catch (e: Exception) {
                // Jeśli metoda nie istnieje, musimy opracować inne podejście
                // Mockujemy AlarmManager dla testu S
                val mockAlarmManager = mockk<AlarmManager>(relaxed = true)
                every { mockAlarmManager.canScheduleExactAlarms() } returns true
                every { context.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager
                alarmManager = mockAlarmManager
            }
        }
    }

    @After
    fun tearDown() {
        // Przywróć domyślną fabrykę PendingIntent
        try {
            val field = NotificationScheduler::class.java.getDeclaredField("pendingIntentFactory")
            field.isAccessible = true

            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

            field.set(null, NotificationScheduler.DefaultPendingIntentFactory())
        } catch (e: Exception) {
            // Ignoruj błędy podczas czyszczenia
            e.printStackTrace()
        }
    }

    @Test
    fun `scheduleGiftRevealNotification sets alarm for birthday date and time`() {
        // Wywołaj testowaną metodę
        NotificationScheduler.scheduleGiftRevealNotification(context, appConfig)

        // Zamiast używania zdeprecowanych pól, weryfikujemy wywołanie alarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Dla Android 12+ weryfikujemy przez verify (używając mockk)
            verify {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, appConfig.getBirthdayTimeMillis(), mockPendingIntent
                )
            }
        } else {
            // Dla starszych wersji, możemy używać shadowAlarmManager
            val scheduledAlarms = shadowAlarmManager.scheduledAlarms
            assertTrue("Brak zaplanowanych alarmów", scheduledAlarms.isNotEmpty())

            // Używamy metod dostępowych zamiast bezpośredniego dostępu do pól
            val scheduledAlarm = scheduledAlarms[0]
            assertEquals(AlarmManager.RTC_WAKEUP, scheduledAlarm.type)
            assertEquals(appConfig.getBirthdayTimeMillis(), scheduledAlarm.triggerAtTime)
            assertEquals(mockPendingIntent, scheduledAlarm.operation)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S]) // Android 12
    fun `does not schedule exact alarm when permission is not granted on Android 12+`() {
        // Używamy refleksji lub mocka zamiast bezpośredniego wywołania metody
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mockAlarmManager = mockk<AlarmManager>(relaxed = true)
            every { mockAlarmManager.canScheduleExactAlarms() } returns false
            every { context.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager

            // Wywołaj testowaną metodę
            NotificationScheduler.scheduleGiftRevealNotification(context, appConfig)

            // Weryfikuj, że używana jest metoda set zamiast setExactAndAllowWhileIdle
            verify(exactly = 0) {
                mockAlarmManager.setExactAndAllowWhileIdle(any(), any(), any())
            }

            verify {
                mockAlarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    appConfig.getBirthdayTimeMillis(),
                    mockPendingIntent
                )
            }
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R]) // Android 11
    fun `schedules alarm without permission check on Android 11 and below`() {
        // Dla starszych wersji Androida używamy spyk zamiast mockk
        val contextR = spyk(context)
        val alarmManagerR = mockk<AlarmManager>(relaxed = true)

        every { contextR.getSystemService(Context.ALARM_SERVICE) } returns alarmManagerR

        // Wywołaj testowaną metodę dla Android 11
        NotificationScheduler.scheduleGiftRevealNotification(contextR, appConfig)

        // Weryfikujemy wywołanie setExactAndAllowWhileIdle bez sprawdzania uprawnień
        verify {
            alarmManagerR.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, appConfig.getBirthdayTimeMillis(), mockPendingIntent
            )
        }
    }

    @Test
    fun `uses correct intent for notification`() {
        // Przygotuj slot do przechwycenia intentu
        val intentSlot = slot<Intent>()
        val testFactory = mockk<NotificationScheduler.PendingIntentFactory>()

        every {
            testFactory.createBroadcastPendingIntent(
                any(), any(), capture(intentSlot), any()
            )
        } returns mockPendingIntent

        // Przypisz tymczasową testową fabrykę
        val field = NotificationScheduler::class.java.getDeclaredField("pendingIntentFactory")
        field.isAccessible = true

        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

        val originalFactory = field.get(null)
        field.set(null, testFactory)

        try {
            // Wywołaj testowaną metodę
            NotificationScheduler.scheduleGiftRevealNotification(context, appConfig)

            // Sprawdź czy przechwycony Intent ma właściwą klasę odbiorcy
            // Nie możemy bezpośrednio sprawdzić klasy, więc sprawdzamy czy ma component
            assertNotNull("Intent powinien mieć określony komponent", intentSlot.captured.component)
            // Możemy dodatkowo sprawdzić czy klasa odbiorcy to NotificationReceiver
            assertEquals(
                "Niepoprawna klasa odbiorcy",
                "com.philornot.siekiera.notification.NotificationReceiver",
                intentSlot.captured.component?.className
            )
        } finally {
            // Przywróć oryginalną fabrykę
            field.set(null, originalFactory)
        }
    }

    @Test
    fun `handles security exception gracefully`() {
        // Przygotuj kontekst i AlarmManager, który będzie rzucać wyjątek
        val mockContext = mockk<Context>()
        val mockAlarmMgr = mockk<AlarmManager>()

        every { mockContext.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmMgr

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            every { mockAlarmMgr.canScheduleExactAlarms() } returns true
        }

        every {
            mockAlarmMgr.setExactAndAllowWhileIdle(any(), any(), any())
        } throws SecurityException("Test exception")

        // Brak wyjątku oznacza, że metoda obsługuje wyjątek poprawnie
        NotificationScheduler.scheduleGiftRevealNotification(mockContext, appConfig)

        // Weryfikacja, że próbowano ustawić alarm
        verify {
            mockAlarmMgr.setExactAndAllowWhileIdle(eq(AlarmManager.RTC_WAKEUP), any(), any())
        }
    }

    @Test
    fun `uses correct pending intent flags`() {
        // Przygotuj fabrykę do testów, która będzie weryfikować flagi
        val testFactory = mockk<NotificationScheduler.PendingIntentFactory>()
        val flagsSlot = slot<Int>()

        every {
            testFactory.createBroadcastPendingIntent(
                any(), any(), any(), capture(flagsSlot)
            )
        } returns mockPendingIntent

        // Podmień tymczasowo fabrykę
        val field = NotificationScheduler::class.java.getDeclaredField("pendingIntentFactory")
        field.isAccessible = true

        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

        val originalFactory = field.get(null)
        field.set(null, testFactory)

        try {
            // Wywołaj testowaną metodę
            NotificationScheduler.scheduleGiftRevealNotification(context, appConfig)

            // Sprawdź czy flagi zawierają FLAG_IMMUTABLE (wymagane dla bezpieczeństwa)
            assertTrue(
                "Flagi PendingIntent powinny zawierać FLAG_IMMUTABLE",
                (flagsSlot.captured and PendingIntent.FLAG_IMMUTABLE) != 0
            )

            // Sprawdź czy flagi zawierają FLAG_UPDATE_CURRENT
            assertTrue(
                "Flagi PendingIntent powinny zawierać FLAG_UPDATE_CURRENT",
                (flagsSlot.captured and PendingIntent.FLAG_UPDATE_CURRENT) != 0
            )
        } finally {
            // Przywróć oryginalną fabrykę
            field.set(null, originalFactory)
        }
    }

    @Test
    fun `does not schedule notification when birthday notifications are disabled`() {
        // Skonfiguruj appConfig, aby zwracał false dla isBirthdayNotificationEnabled
        val disabledConfig = mockk<AppConfig>()
        every { disabledConfig.isBirthdayNotificationEnabled() } returns false
        every { disabledConfig.getBirthdayDate() } returns Calendar.getInstance(warsawTimeZone)

        val spyAlarmManager = spyk(alarmManager)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns spyAlarmManager

        // Wywołaj testowaną metodę
        NotificationScheduler.scheduleGiftRevealNotification(context, disabledConfig)

        // Weryfikuj, że żadna metoda ustawiania alarmu nie została wywołana
        verify(exactly = 0) {
            spyAlarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
        verify(exactly = 0) {
            spyAlarmManager.set(any(), any(), any())
        }
    }

    @Test
    fun `uses deprecated method correctly`() {
        // Przygotuj mock dla singleton AppConfig.INSTANCE
        val oldAppConfig = mockk<AppConfig>()

        try {
            val field = AppConfig::class.java.getDeclaredField("INSTANCE")
            field.isAccessible = true

            // Zapisz oryginalną wartość, aby przywrócić później
            val originalInstance = field.get(null)

            try {
                // Ustaw mock jako singleton
                field.set(null, oldAppConfig)

                // Skonfiguruj appConfig
                every { oldAppConfig.getBirthdayTimeMillis() } returns 123456789L
                every { oldAppConfig.getBirthdayDate() } returns Calendar.getInstance()
                every { oldAppConfig.isBirthdayNotificationEnabled() } returns true

                val spyAlarmManager = mockk<AlarmManager>(relaxed = true)
                every { context.getSystemService(Context.ALARM_SERVICE) } returns spyAlarmManager

                // Wywołaj przestarzałą metodę
                @Suppress("DEPRECATION") NotificationScheduler.scheduleGiftRevealNotification(
                    context
                )

                // Weryfikuj wywołanie metody ustawiania alarmu
                verify {
                    spyAlarmManager.setExactAndAllowWhileIdle(any(), eq(123456789L), any())
                }

            } finally {
                // Przywróć oryginalną wartość
                field.set(null, originalInstance)
            }
        } catch (e: NoSuchFieldException) {
            // Obsługa braku pola INSTANCE (może być getInstance() zamiast singletonu)
            every { AppConfig.getInstance(context) } returns oldAppConfig
            every { oldAppConfig.getBirthdayTimeMillis() } returns 123456789L
            every { oldAppConfig.getBirthdayDate() } returns Calendar.getInstance()
            every { oldAppConfig.isBirthdayNotificationEnabled() } returns true

            val spyAlarmManager = mockk<AlarmManager>(relaxed = true)
            every { context.getSystemService(Context.ALARM_SERVICE) } returns spyAlarmManager

            // Wywołaj przestarzałą metodę
            @Suppress("DEPRECATION") NotificationScheduler.scheduleGiftRevealNotification(context)

            // Weryfikuj wywołanie metody ustawiania alarmu
            verify {
                spyAlarmManager.setExactAndAllowWhileIdle(any(), eq(123456789L), any())
            }
        }
    }
}