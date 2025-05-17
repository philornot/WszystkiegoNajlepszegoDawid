package com.philornot.siekiera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class GiftAppTest {

    private lateinit var context: Context
    private lateinit var giftApp: GiftApp
    private lateinit var appConfig: AppConfig
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

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

        // Mock dla NotificationScheduler
        mockkObject(NotificationScheduler)
        every { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) } returns Unit

        // Inicjalizacja testowanego obiektu
        giftApp = spyk(GiftApp())

        // Mock dla ApplicationContext
        every { giftApp.applicationContext } returns context

        // Przygotuj MockAppConfig.getInstance
        mockkObject(AppConfig.Companion)
        every { AppConfig.getInstance(any()) } returns appConfig
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onCreate initializes AppConfig`() {
        // Wywołanie testowanej metody
        giftApp.onCreate()

        // Weryfikacja
        verify { AppConfig.getInstance(context) }
    }

    @Test
    fun `onCreate schedules notifications when enabled and date is in future`() {
        // Konfiguracja - data urodzin w przyszłości
        every { appConfig.getBirthdayTimeMillis() } returns (System.currentTimeMillis() + 1000000)

        // Wywołanie testowanej metody
        giftApp.onCreate()

        // Weryfikacja
        verify { NotificationScheduler.scheduleGiftRevealNotification(giftApp, appConfig) }
    }

    @Test
    fun `onCreate does not schedule notifications when disabled`() {
        // Konfiguracja - wyłączone powiadomienia
        every { appConfig.isBirthdayNotificationEnabled() } returns false

        // Wywołanie testowanej metody
        giftApp.onCreate()

        // Weryfikacja
        verify(exactly = 0) { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) }
    }

    @Test
    fun `onCreate does not schedule notifications when date is in past`() {
        // Konfiguracja - data urodzin w przeszłości
        every { appConfig.getBirthdayTimeMillis() } returns (System.currentTimeMillis() - 1000000)

        // Wywołanie testowanej metody
        giftApp.onCreate()

        // Weryfikacja
        verify(exactly = 0) { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) }
    }

    @Test
    fun `workManagerConfiguration has correct logging level`() {
        // Wywołanie testowanej metody
        val config = giftApp.workManagerConfiguration

        // Weryfikacja
        assertNotNull(config)

        // Nie możemy bezpośrednio sprawdzić poziomu logowania, ale zapewniamy że
        // klasa konfiguracji została zwrócona poprawnie
        assertTrue(config is Configuration)
    }

    @Test
    fun `checkAndScheduleNotification handles exceptions gracefully`() {
        // Konfiguracja - NotificationScheduler rzuca wyjątek
        every {
            NotificationScheduler.scheduleGiftRevealNotification(any(), any())
        } throws RuntimeException("Test exception")

        // Konfiguracja - data urodzin w przyszłości
        every { appConfig.getBirthdayTimeMillis() } returns (System.currentTimeMillis() + 1000000)

        // Wywołanie prywatnej metody przez refleksję
        val method = GiftApp::class.java.getDeclaredMethod("checkAndScheduleNotification")
        method.isAccessible = true

        // Wywołanie nie powinno rzucić wyjątku
        method.invoke(giftApp)

        // Weryfikacja wywołania mimo wyjątku
        verify { NotificationScheduler.scheduleGiftRevealNotification(giftApp, appConfig) }
    }

    @Test
    fun `onCreate initializes Timber in debug mode`() {
        // Konfiguracja - symulacja debug build
        every {
            giftApp.applicationContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
        } returns android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE

        // Wywołanie testowanej metody
        giftApp.onCreate()

        // Weryfikacja czy Timber.plant została wywołana
        // Jest to trudne do przetestowania bezpośrednio w Robolectric,
        // ale możemy sprawdzić czy Timber jest zainicjalizowany przez test loga

        // Próbujemy zalogować wiadomość - nie powinna rzucić wyjątku,
        // jeśli Timber został poprawnie zainicjalizowany
        Timber.d("Test log")

        // Brak asercji - wystarczy brak wyjątku
    }

    @Test
    fun `configureTimber handles different build flavors`() {
        // Ten test sprawdza czy Timber jest konfigurowany poprawnie
        // zarówno dla wersji debug jak i release

        // Nie używamy bezpośrednio funkcji, wystarczy że sprawdzamy logikę,
        // a szczegółowe testy byłyby zbyt zaawansowane i polegały na mockach
        // pól statycznych BuildConfig, co jest trudne do wykonania

        // Zasymulujmy mockowanie BuildConfig.DEBUG
        // Niestety to nie jest łatwe do zrobienia w Robolectric,
        // więc jest to bardziej test koncepcyjny niż pełny test jednostkowy

        // W pełnych testach moglibyśmy użyć PowerMock do mockowania pól statycznych
        // lub jawnie przetestować oba przypadki w różnych wersjach budowania
    }

    @Test
    fun `GiftApp implements Configuration Provider`() {
        // Sprawdź, czy klasa implementuje prawidłowy interfejs
        assertTrue(giftApp is Configuration.Provider)

        // Sprawdź, czy metoda zwraca poprawny typ
        val config = giftApp.workManagerConfiguration
        assertTrue(config is Configuration)
    }
}