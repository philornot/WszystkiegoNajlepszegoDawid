package com.philornot.siekiera.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.philornot.siekiera.ui.viewmodel.MainScreenState
import com.philornot.siekiera.ui.viewmodel.MainViewModel
import com.philornot.siekiera.utils.TestTimeProvider
import com.philornot.siekiera.utils.TimeUtils
import com.philornot.siekiera.config.AppConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import app.cash.turbine.test
import kotlin.math.abs

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var appConfig: AppConfig

    // Strefa czasowa Warszawy
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        timeProvider = TestTimeProvider()
        appConfig = mockk(relaxed = true)

        // Ustaw domyślną datę urodzin - 14 maja 2025, 16:34
        val birthdayCalendar = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }

        every { appConfig.getBirthdayTimeMillis() } returns birthdayCalendar.timeInMillis
        every { appConfig.getBirthdayDate() } returns birthdayCalendar

        viewModel = MainViewModel(timeProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getTimeRemaining returns correct value before reveal date`() = runTest {
        // Ustaw czas testowy na 13 maja 2025, 16:34 (dokładnie 24h przed)
        val testDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 13, 16, 34, 0
        )
        timeProvider.setCurrentTimeMillis(testDate.timeInMillis)

        // Oczekiwany czas pozostały - 24 godziny (w milisekundach)
        val expectedTime = 24 * 60 * 60 * 1000L

        // Wywołaj funkcję i sprawdź wynik
        val result = viewModel.getTimeRemaining()

        // Oczekiwany wynik: 24 godziny (z możliwością małej różnicy)
        assertTrue("Oczekiwane ~24h (${expectedTime}ms), otrzymano: ${result}ms",
            abs(expectedTime - result) < 1000) // Tolerancja 1 sekunda

        // Sprawdź, czy funkcja isTimeUp zwraca false (czas jeszcze nie nadszedł)
        assertFalse(viewModel.isTimeUp())
    }

    @Test
    fun `getTimeRemaining returns zero on reveal date`() = runTest {
        // Ustaw czas testowy na 14 maja 2025, 16:34:00 (dokładnie czas ujawnienia)
        val testDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 14, 16, 34, 0
        )
        timeProvider.setCurrentTimeMillis(testDate.timeInMillis)

        // Wywołaj funkcję i sprawdź wynik
        val result = viewModel.getTimeRemaining()

        // Oczekiwany wynik: 0 (czas dokładnie nadszedł)
        assertEquals(0L, result)

        // Sprawdź, czy funkcja isTimeUp zwraca true (czas nadszedł)
        assertTrue(viewModel.isTimeUp())
    }

    @Test
    fun `getTimeRemaining returns zero after reveal date`() = runTest {
        // Ustaw czas testowy na 15 maja 2025, 16:34 (24h po)
        val testDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 15, 16, 34, 0
        )
        timeProvider.setCurrentTimeMillis(testDate.timeInMillis)

        // Wywołaj funkcję i sprawdź wynik
        val result = viewModel.getTimeRemaining()

        // Oczekiwany wynik: 0 (czas już minął)
        assertEquals(0L, result)

        // Sprawdź, czy funkcja isTimeUp zwraca true (czas już minął)
        assertTrue(viewModel.isTimeUp())
    }

    @Test
    fun `getTimeRemaining handles edge case at millisecond precision`() = runTest {
        // Ustaw czas testowy na 1 milisekundę przed
        val testDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 14, 16, 33, 59
        )
        testDate.set(Calendar.MILLISECOND, 999)
        timeProvider.setCurrentTimeMillis(testDate.timeInMillis)

        // Wywołaj funkcję i sprawdź wynik
        val result = viewModel.getTimeRemaining()

        // Oczekiwany wynik: 1 milisekunda
        assertEquals(1L, result)
        assertFalse(viewModel.isTimeUp())

        // Teraz ustaw czas na dokładnie moment ujawnienia
        val exactDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 14, 16, 34, 0
        )
        exactDate.set(Calendar.MILLISECOND, 0)
        timeProvider.setCurrentTimeMillis(exactDate.timeInMillis)

        // Sprawdź ponownie
        assertEquals(0L, viewModel.getTimeRemaining())
        assertTrue(viewModel.isTimeUp())
    }

    @Test
    fun `formatCountdown formats time correctly`() = runTest {
        // Test dla 10 dni, 11 godzin, 12 minut, 13 sekund
        val timeInMillis =
            TimeUnit.DAYS.toMillis(10) +
                    TimeUnit.HOURS.toMillis(11) +
                    TimeUnit.MINUTES.toMillis(12) +
                    TimeUnit.SECONDS.toMillis(13)

        val formatted = viewModel.formatCountdown(timeInMillis)

        // Oczekiwany wynik w formacie "10 dni, 11:12:13"
        assertEquals("10 dni, 11:12:13", formatted)
    }

    @Test
    fun `formatCountdown handles single day correctly`() = runTest {
        // Test dla 1 dnia
        val timeInMillis = TimeUnit.DAYS.toMillis(1)

        val formatted = viewModel.formatCountdown(timeInMillis)

        // Oczekiwany wynik w formacie "1 dni, 00:00:00" (polska gramatyka wymaga formy mnogiej "dni")
        assertEquals("1 dni, 00:00:00", formatted)
    }

    @Test
    fun `formatCountdown handles zero time correctly`() = runTest {
        val formatted = viewModel.formatCountdown(0L)

        // Oczekiwany wynik w formacie "0 dni, 00:00:00"
        assertEquals("0 dni, 00:00:00", formatted)
    }

    @Test
    fun `formatCountdown handles large values correctly`() = runTest {
        // Test dla 100 dni
        val timeInMillis = TimeUnit.DAYS.toMillis(100)

        val formatted = viewModel.formatCountdown(timeInMillis)

        // Oczekiwany wynik w formacie "100 dni, 00:00:00"
        assertEquals("100 dni, 00:00:00", formatted)
    }

    @Test
    fun `state is updated properly before reveal date`() = runTest {
        // Ustaw czas przed ujawnieniem prezentu
        val beforeRevealDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 13, 12, 0, 0
        )
        timeProvider.setCurrentTimeMillis(beforeRevealDate.timeInMillis)

        // Aktualizacja stanu
        viewModel.updateState()

        // Sprawdź stan - kurtyna powinna być widoczna, a prezent nie
        val state = viewModel.uiState.first()
        assertFalse(state.isRevealTime)
        assertTrue(state.showCurtain)
        assertFalse(state.showGift)
    }

    @Test
    fun `state is updated properly after reveal date`() = runTest {
        // Ustaw czas po ujawnieniu prezentu
        val afterRevealDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 15, 12, 0, 0
        )
        timeProvider.setCurrentTimeMillis(afterRevealDate.timeInMillis)

        // Aktualizacja stanu
        viewModel.updateState()

        // Sprawdź stan - kurtyna nie powinna być widoczna, a prezent tak
        val state = viewModel.uiState.first()
        assertTrue(state.isRevealTime)
        assertFalse(state.showCurtain)
        assertTrue(state.showGift)
    }

    @Test
    fun `state transition when time changes`() = runTest {
        // Ustaw czas przed ujawnieniem prezentu
        val beforeRevealDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 14, 16, 33, 59
        )
        timeProvider.setCurrentTimeMillis(beforeRevealDate.timeInMillis)

        // Aktualizacja stanu
        viewModel.updateState()

        // Sprawdź początkowy stan
        var state = viewModel.uiState.first()
        assertFalse(state.isRevealTime)
        assertTrue(state.showCurtain)
        assertFalse(state.showGift)

        // Zmień czas na 2 sekundy później (po czasie ujawnienia)
        val afterRevealDate = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 14, 16, 34, 1
        )
        timeProvider.setCurrentTimeMillis(afterRevealDate.timeInMillis)

        // Aktualizacja stanu
        viewModel.updateState()

        // Sprawdź końcowy stan
        state = viewModel.uiState.first()
        assertTrue(state.isRevealTime)
        assertFalse(state.showCurtain)
        assertTrue(state.showGift)
    }

    @Test
    fun `uiState flow emits correct values on updates`() = runTest {
        viewModel.uiState.test {
            // Ustaw czas przed ujawnieniem
            timeProvider.setCurrentTimeMillis(
                TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 13, 16, 34, 0).timeInMillis
            )

            // Początkowy stan (domyślny)
            val initialState = awaitItem()
            assertEquals(MainScreenState(), initialState)

            // Aktualizuj stan
            viewModel.updateState()

            // Nowy stan po aktualizacji
            val updatedState = awaitItem()
            assertFalse(updatedState.isRevealTime)
            assertTrue(updatedState.showCurtain)
            assertFalse(updatedState.showGift)

            // Zmień czas na po ujawnieniu
            timeProvider.setCurrentTimeMillis(
                TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 15, 16, 34, 0).timeInMillis
            )

            // Aktualizuj stan ponownie
            viewModel.updateState()

            // Kolejny stan po aktualizacji
            val finalState = awaitItem()
            assertTrue(finalState.isRevealTime)
            assertFalse(finalState.showCurtain)
            assertTrue(finalState.showGift)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateState uses provided currentTimeMillis parameter correctly`() = runTest {
        // Ustaw czas w TimeProvider na czas przed ujawnieniem
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 13, 16, 34, 0).timeInMillis
        )

        // Ale wywołaj updateState z jawnie podanym czasem po ujawnieniu
        val afterRevealTime = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 15, 16, 34, 0
        ).timeInMillis

        viewModel.updateState(afterRevealTime)

        // Sprawdź stan - mimo że TimeProvider ma czas przed, to stan powinien być po
        val state = viewModel.uiState.first()
        assertTrue(state.isRevealTime)
        assertFalse(state.showCurtain)
        assertTrue(state.showGift)
    }

    @Test
    fun `countdown text is formatted correctly in state`() = runTest {
        // Ustaw czas dokładnie 1 dzień przed ujawnieniem
        val oneDayBefore = TimeUtils.getCalendarForDateTime(
            2025, Calendar.MAY, 13, 16, 34, 0
        )
        timeProvider.setCurrentTimeMillis(oneDayBefore.timeInMillis)

        // Aktualizuj stan
        viewModel.updateState()

        // Sprawdź formatowanie odliczania w stanie
        val state = viewModel.uiState.first()
        assertEquals("1 dni, 00:00:00", state.countdownText)
    }
}