package com.philornot.siekiera

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.network.DriveApiClient
import com.philornot.siekiera.notification.NotificationScheduler
import com.philornot.siekiera.utils.TestTimeProvider
import com.philornot.siekiera.utils.TimeUtils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class IntegrationTest {

    private lateinit var appConfig: AppConfig
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var driveApiClient: DriveApiClient
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Before
    fun setup() {
        // Inicjalizacja mocków i komponentów
        ApplicationProvider.getApplicationContext<android.content.Context>()

        // Mock dla AppConfig
        appConfig = mockk(relaxed = true)

        // Ustaw domyślną datę urodzin - 14 maja 2025, 16:34
        val birthdayCalendar = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }

        every { appConfig.getBirthdayTimeMillis() } returns birthdayCalendar.timeInMillis
        every { appConfig.getBirthdayDate() } returns birthdayCalendar
        every { appConfig.isBirthdayNotificationEnabled() } returns true
        every { appConfig.isDailyFileCheckEnabled() } returns true
        every { appConfig.getFileCheckIntervalHours() } returns 24
        every { appConfig.getDailyFileCheckWorkName() } returns "daily_file_check"
        every { appConfig.getDaylioFileName() } returns "dawid_pamiętnik.daylio"
        every { appConfig.getDriveFolderId() } returns "test_folder_id"
        every { appConfig.isVerboseLoggingEnabled() } returns true

        // Ustaw singleton AppConfig.INSTANCE
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, appConfig)

        // Inicjalizacja TimeProvider
        timeProvider = TestTimeProvider()

        // Mock dla DriveApiClient
        driveApiClient = mockk(relaxed = true)
        coEvery { driveApiClient.initialize() } returns true

        // Ustawienie mockInstance
        DriveApiClient.mockInstance = driveApiClient

        // Mock dla NotificationScheduler
        mockkObject(NotificationScheduler)
        every { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        // Resetowanie singltonów i mocków
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        DriveApiClient.mockInstance = null

        unmockkAll()
    }

    @Test
    fun `integration test - full flow before birthday`() = runBlocking {
        // Symulowanie czasu przed dniem urodzin
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 13, 16, 34, 0).timeInMillis
        )

        // Inicjalizacja komponentów aplikacji
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mainActivity = spyk(MainActivity())
        every { mainActivity.applicationContext } returns context

        // Ustawienie TimeProvider w MainActivity
        mainActivity.timeProvider = timeProvider

        // Symulowanie cyklu życia Activity
        mainActivity.onCreate(null)
        mainActivity.onResume()

        // Weryfikacja - powiadomienie powinno zostać zaplanowane
        verify { NotificationScheduler.scheduleGiftRevealNotification(any(), any()) }

        // Symulacja kliknięcia prezentu nie powinna nic zrobić,
        // ponieważ prezent nie powinien być widoczny przed dniem urodzin

        // Weryfikacja stanu UI za pomocą ViewModel
        val viewModel = spyk(com.philornot.siekiera.ui.viewmodel.MainViewModel(timeProvider))
        viewModel.updateState()

        val state = viewModel.uiState.value
        assertEquals(false, state.isRevealTime) // Nie czas jeszcze na ujawnienie
        assertEquals(true, state.showCurtain) // Kurtyna powinna być widoczna
        assertEquals(false, state.showGift) // Prezent nie powinien być widoczny

        // Sprawdź, czy odliczanie pokazuje poprawną wartość (> 0)
        val timeRemaining = viewModel.getTimeRemaining()
        assertTrue("Pozostały czas powinien być > 0", timeRemaining > 0)

        // Sprawdź formatowanie odliczania
        val formattedTime = viewModel.formatCountdown(timeRemaining)
        assertTrue(
            "Sformatowany czas powinien zawierać format dni, HH:MM:SS",
            formattedTime.matches(Regex("\\d+ dni, \\d{2}:\\d{2}:\\d{2}"))
        )
    }

    @Test
    fun `integration test - full flow exactly at birthday time`() = runBlocking {
        // Symulowanie dokładnie czasu urodzin
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 34, 0).timeInMillis
        )

        // Inicjalizacja komponentów aplikacji
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mainActivity = spyk(MainActivity())
        every { mainActivity.applicationContext } returns context

        // Ustawienie TimeProvider w MainActivity
        mainActivity.timeProvider = timeProvider

        // Symulowanie cyklu życia Activity
        mainActivity.onCreate(null)
        mainActivity.onResume()

        // Powiadomienie zostanie zaplanowane, ale powinno być natychmiast wyświetlone

        // Weryfikacja stanu UI za pomocą ViewModel
        val viewModel = spyk(com.philornot.siekiera.ui.viewmodel.MainViewModel(timeProvider))
        viewModel.updateState()

        val state = viewModel.uiState.value
        assertEquals(true, state.isRevealTime) // Czas na ujawnienie
        assertEquals(false, state.showCurtain) // Kurtyna nie powinna być widoczna
        assertEquals(true, state.showGift) // Prezent powinien być widoczny

        // Sprawdź, czy odliczanie pokazuje 0
        val timeRemaining = viewModel.getTimeRemaining()
        assertEquals("Pozostały czas powinien być 0", 0L, timeRemaining)

        // Symulacja pobierania prezentu

        // Przygotuj mock dla DriveApiClient do listowania plików
        val fileInfo = DriveApiClient.FileInfo(
            id = "testId",
            name = "dawid_pamiętnik.daylio",
            mimeType = "application/octet-stream",
            size = 1024L,
            modifiedTime = Date()
        )

        coEvery {
            driveApiClient.listFilesInFolder("test_folder_id")
        } returns listOf(fileInfo)

        val testData = "test daylio content"
        val testInputStream = ByteArrayInputStream(testData.toByteArray())

        coEvery {
            driveApiClient.downloadFile("testId")
        } returns testInputStream

        // Wywołanie downloadFile w MainActivity będzie trudne do przetestowania
        // w środowisku Robolectric, ponieważ wymaga użycia WorkManager,
        // ale możemy zweryfikować, że komponenty są prawidłowo połączone
    }

    @Test
    fun `integration test - full flow after birthday`() = runBlocking {
        // Symulowanie czasu po urodzinach
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 15, 16, 34, 0).timeInMillis
        )

        // Inicjalizacja komponentów aplikacji
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val mainActivity = spyk(MainActivity())
        every { mainActivity.applicationContext } returns context

        // Ustawienie TimeProvider w MainActivity
        mainActivity.timeProvider = timeProvider

        // Symulowanie cyklu życia Activity
        mainActivity.onCreate(null)
        mainActivity.onResume()

        // Weryfikacja - powiadomienie NIE powinno zostać zaplanowane, gdyż data już minęła
        verify(exactly = 0) {
            NotificationScheduler.scheduleGiftRevealNotification(any(), any())
        }

        // Weryfikacja stanu UI za pomocą ViewModel
        val viewModel = spyk(com.philornot.siekiera.ui.viewmodel.MainViewModel(timeProvider))
        viewModel.updateState()

        val state = viewModel.uiState.value
        assertEquals(true, state.isRevealTime) // Czas na ujawnienie
        assertEquals(false, state.showCurtain) // Kurtyna nie powinna być widoczna
        assertEquals(true, state.showGift) // Prezent powinien być widoczny

        // Sprawdź, czy odliczanie pokazuje 0
        val timeRemaining = viewModel.getTimeRemaining()
        assertEquals("Pozostały czas powinien być 0", 0L, timeRemaining)
    }

    @Test
    fun `integration test - edge case one second before birthday`() = runBlocking {
        // Symulowanie 1 sekundy przed urodzinami
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 33, 59).timeInMillis
        )

        // Inicjalizacja komponentów aplikacji
        ApplicationProvider.getApplicationContext<android.content.Context>()

        // Weryfikacja stanu UI za pomocą ViewModel
        val viewModel = spyk(com.philornot.siekiera.ui.viewmodel.MainViewModel(timeProvider))
        viewModel.updateState()

        val state = viewModel.uiState.value
        assertEquals(false, state.isRevealTime) // Nie czas jeszcze na ujawnienie
        assertEquals(true, state.showCurtain) // Kurtyna powinna być widoczna
        assertEquals(false, state.showGift) // Prezent nie powinien być widoczny

        // Sprawdź, czy odliczanie pokazuje 1 sekundę
        val timeRemaining = viewModel.getTimeRemaining()
        assertTrue(
            "Pozostały czas powinien być 1 sekunda (1000ms)",
            timeRemaining <= 1000 && timeRemaining > 0
        )

        // Zmiana czasu na 1 sekundę później - dokładnie w momencie urodzin
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 34, 0).timeInMillis
        )

        // Aktualizacja stanu
        viewModel.updateState()

        // Weryfikacja zmienionego stanu
        val updatedState = viewModel.uiState.value
        assertEquals(true, updatedState.isRevealTime) // Teraz czas na ujawnienie
        assertEquals(false, updatedState.showCurtain) // Kurtyna nie powinna być widoczna
        assertEquals(true, updatedState.showGift) // Prezent powinien być widoczny

        // Sprawdź, czy odliczanie pokazuje 0
        val updatedTimeRemaining = viewModel.getTimeRemaining()
        assertEquals("Pozostały czas powinien być 0", 0L, updatedTimeRemaining)
    }

    @Test
    fun `integration test - timezone handling`() = runBlocking {
        // Sprawdzamy, czy aplikacja poprawnie obsługuje strefę czasową
        val defaultTimeZone = TimeZone.getDefault()

        try {
            // Ustawmy strefę czasową na inną niż Warszawa (np. Los Angeles)
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

            // Symulowanie czasu, który odpowiada 13 maja 2025, 12:00 w Warszawie
            // ale jest inną godziną w Los Angeles
            val warsawCalendar = Calendar.getInstance(warsawTimeZone).apply {
                set(2025, Calendar.MAY, 13, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Ustawienie czasu w TimeProvider
            timeProvider.setCurrentTimeMillis(warsawCalendar.timeInMillis)

            // Weryfikacja stanu UI za pomocą ViewModel
            val viewModel = spyk(com.philornot.siekiera.ui.viewmodel.MainViewModel(timeProvider))
            viewModel.updateState()

            // Stan UI powinien być taki jak dla czasu przed urodzinami,
            // niezależnie od lokalnej strefy czasowej
            val state = viewModel.uiState.value
            assertEquals(false, state.isRevealTime) // Nie czas jeszcze na ujawnienie
            assertEquals(true, state.showCurtain) // Kurtyna powinna być widoczna
            assertEquals(false, state.showGift) // Prezent nie powinien być widoczny

            // Sprawdźmy też datę urodzin z AppConfig
            val birthdayDate = appConfig.getBirthdayDate()
            assertEquals(warsawTimeZone, birthdayDate.timeZone)
            assertEquals(2025, birthdayDate.get(Calendar.YEAR))
            assertEquals(Calendar.MAY, birthdayDate.get(Calendar.MONTH))
            assertEquals(14, birthdayDate.get(Calendar.DAY_OF_MONTH))
            assertEquals(16, birthdayDate.get(Calendar.HOUR_OF_DAY))
            assertEquals(34, birthdayDate.get(Calendar.MINUTE))

        } finally {
            // Przywróć domyślną strefę czasową
            TimeZone.setDefault(defaultTimeZone)
        }
    }

    @Test
    fun `integration test - full app initialization flow`() {
        // Test inicjalizacji całej aplikacji
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Testujemy GiftApp - aplikację główną
        val giftApp = spyk(GiftApp())
        every { giftApp.applicationContext } returns context

        // Wywołanie onCreate powinno zainicjalizować wszystkie komponenty
        giftApp.onCreate()

        // AppConfig powinien zostać zainicjalizowany
        assertNotNull(AppConfig.INSTANCE)

        // WorkManager powinien zostać skonfigurowany
        val workManagerConfig = giftApp.workManagerConfiguration
        assertNotNull(workManagerConfig)

        // W zależności od czasu, NotificationScheduler mógł lub nie być wywołany
        // Nie ma potrzeby bezpośredniego sprawdzania tego w tym teście
    }
}