package com.philornot.siekiera

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var bootReceiver: BootReceiver
    private lateinit var appConfig: AppConfig
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        context = ApplicationProvider.getApplicationContext()
        bootReceiver = spyk(BootReceiver())

        // Mock dla AppConfig
        appConfig = mockk(relaxed = true)

        // Ustaw domyślną datę urodzin na 14 maja 2025, 16:34
        val birthdayCalendar = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }

        every { appConfig.getBirthdayTimeMillis() } returns birthdayCalendar.timeInMillis
        every { appConfig.getBirthdayDate() } returns birthdayCalendar
        every { appConfig.isBirthdayNotificationEnabled() } returns true

        // Ustaw singleton AppConfig.INSTANCE
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, appConfig)

        // Mock dla NotificationScheduler
        mockkObject(NotificationScheduler)
        every { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        // Resetowanie singletona
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        unmockkAll()
    }

    @Test
    fun `onReceive schedules notification when ACTION_BOOT_COMPLETED is received`() {
        // Konfiguracja
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Ustawmy czas systemowy na czas przed urodzinami
        val beforeBirthdayTime = System.currentTimeMillis() - 10000 // 10 sekund temu
        every { appConfig.getBirthdayTimeMillis() } returns (beforeBirthdayTime + 1000000) // w przyszłości

        // Wywołanie testowanej metody
        bootReceiver.onReceive(context, intent)

        // Weryfikacja
        verify { NotificationScheduler.scheduleGiftRevealNotification(context, appConfig) }
    }

    @Test
    fun `onReceive does nothing for other intents`() {
        // Konfiguracja
        val intent = Intent("some.other.action")

        // Wywołanie testowanej metody
        bootReceiver.onReceive(context, intent)

        // Weryfikacja - nie powinno być wywołania scheduleGiftRevealNotification
        verify(exactly = 0) { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) }
    }

    @Test
    fun `onReceive does nothing when notifications are disabled`() {
        // Konfiguracja
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Zmień konfigurację, aby wyłączyć powiadomienia
        every { appConfig.isBirthdayNotificationEnabled() } returns false

        // Wywołanie testowanej metody
        bootReceiver.onReceive(context, intent)

        // Weryfikacja - nie powinno być wywołania scheduleGiftRevealNotification
        verify(exactly = 0) { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) }
    }

    @Test
    fun `onReceive does not schedule notification when birthday has passed`() {
        // Konfiguracja
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Ustawmy czas systemowy na czas po urodzinach
        val currentTime = System.currentTimeMillis()
        val pastBirthdayTime = currentTime - 1000000 // w przeszłości
        every { appConfig.getBirthdayTimeMillis() } returns pastBirthdayTime

        // Wywołanie testowanej metody
        bootReceiver.onReceive(context, intent)

        // Weryfikacja - nie powinno być wywołania scheduleGiftRevealNotification
        verify(exactly = 0) { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) }
    }

    @Test
    fun `onReceive handles exceptions gracefully`() {
        // Konfiguracja
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Ustawmy czas systemowy na czas przed urodzinami
        val beforeBirthdayTime = System.currentTimeMillis() - 10000 // 10 sekund temu
        every { appConfig.getBirthdayTimeMillis() } returns (beforeBirthdayTime + 1000000) // w przyszłości

        // Przygotuj NotificationScheduler do rzucania wyjątku
        every {
            NotificationScheduler.scheduleGiftRevealNotification(any(), any())
        } throws RuntimeException("Test exception")

        // Wywołanie testowanej metody nie powinno rzucić wyjątku
        bootReceiver.onReceive(context, intent)

        // Brak asercji - wystarczy, że nie rzuci wyjątku
    }

    @Test
    fun `onReceive gets AppConfig singleton properly`() {
        // Konfiguracja
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        // Resetowanie singletona przed testem
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        // Przygotowanie mocka dla getInstance
        val mockAppConfigClass = mockk<AppConfig>()
        every { mockAppConfigClass.getBirthdayTimeMillis() } returns (System.currentTimeMillis() + 1000000)
        every { mockAppConfigClass.isBirthdayNotificationEnabled() } returns true

        mockkObject(AppConfig.Companion)
        every { AppConfig.getInstance(any()) } returns mockAppConfigClass

        // Wywołanie testowanej metody
        bootReceiver.onReceive(context, intent)

        // Weryfikacja
        verify { AppConfig.getInstance(context) }
        verify { NotificationScheduler.scheduleGiftRevealNotification(context, mockAppConfigClass) }
    }
}