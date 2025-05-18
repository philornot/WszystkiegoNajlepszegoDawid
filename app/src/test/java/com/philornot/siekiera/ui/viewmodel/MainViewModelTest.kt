// W pliku MainViewModelTest.kt

package com.philornot.siekiera.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.utils.TestTimeProvider
import com.philornot.siekiera.utils.TimeUtils
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@ExperimentalCoroutinesApi
class MainViewModelTest {

    // This rule ensures LiveData updates happen immediately in tests
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel
    private lateinit var timeProvider: TestTimeProvider
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Mock
    private lateinit var mockAppConfig: AppConfig

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        timeProvider = TestTimeProvider()

        // Inicjalizuj mockAppConfig
        val revealDate = Calendar.getInstance(warsawTimeZone).apply {
            set(Calendar.YEAR, 2025)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 24)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        `when`(mockAppConfig.getBirthdayTimeMillis()).thenReturn(revealDate)

        // Ustaw singleton
        AppConfig.INSTANCE = mockAppConfig

        viewModel = MainViewModel(timeProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // POPRAWKA: Czyścimy singleton po teście
        AppConfig.INSTANCE = null
    }

    @Test
    fun `getTimeRemaining returns correct time difference`() = runTest {
        // Given - Set current time to exactly 24 hours before the reveal date
        val revealDate = TimeUtils.getRevealDateMillis(mockAppConfig)
        val oneDayInMillis = TimeUnit.DAYS.toMillis(1)
        val currentTime = revealDate - oneDayInMillis
        timeProvider.setCurrentTimeMillis(currentTime)

        // When
        val timeRemaining = viewModel.getTimeRemaining(currentTime)

        // Then
        assertEquals(oneDayInMillis, timeRemaining)
    }

    @Test
    fun `getTimeRemaining returns zero when time is up`() = runTest {
        // Given - Set current time to after the reveal date
        val revealDate = TimeUtils.getRevealDateMillis(mockAppConfig)
        val currentTime = revealDate + 1000 // 1 second after
        timeProvider.setCurrentTimeMillis(currentTime)

        // When
        val timeRemaining = viewModel.getTimeRemaining(currentTime)

        // Then
        assertEquals(0L, timeRemaining)
    }

    @Test
    fun `isTimeUp returns false before reveal date`() = runTest {
        // Given - Set current time to before the reveal date
        val revealDate = TimeUtils.getRevealDateMillis(mockAppConfig)
        val currentTime = revealDate - 1000 // 1 second before
        timeProvider.setCurrentTimeMillis(currentTime)

        // When
        val result = viewModel.isTimeUp(currentTime)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isTimeUp returns true on reveal date`() = runTest {
        // Given - Set current time to exactly the reveal date
        val revealDate = TimeUtils.getRevealDateMillis(mockAppConfig)
        val currentTime = revealDate
        timeProvider.setCurrentTimeMillis(currentTime)

        // When
        val result = viewModel.isTimeUp(currentTime)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isTimeUp returns true after reveal date`() = runTest {
        // Given - Set current time to after the reveal date
        val revealDate = TimeUtils.getRevealDateMillis(mockAppConfig)
        val currentTime = revealDate + 1000 // 1 second after
        timeProvider.setCurrentTimeMillis(currentTime)

        // When
        val result = viewModel.isTimeUp(currentTime)

        // Then
        assertTrue(result)
    }

    @Test
    fun `formatCountdown formats time correctly`() = runTest {
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
        val formatted = viewModel.formatCountdown(milliseconds)

        // Then
        assertEquals("5 dni, 06:30:15", formatted)
    }

    @Test
    fun `updateState sets correct UI state before reveal date`() = runTest {
        // Given - Set current time to before the reveal date
        val revealDate = TimeUtils.getRevealDateMillis(mockAppConfig)
        val currentTime = revealDate - TimeUnit.DAYS.toMillis(1) // 1 day before
        timeProvider.setCurrentTimeMillis(currentTime)

        // When
        viewModel.updateState(currentTime)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isRevealTime)
        assertTrue(state.showCurtain)
        assertFalse(state.showGift)
        assertEquals("1 dni, 00:00:00", state.countdownText)
    }

    @Test
    fun `updateState sets correct UI state after reveal date`() = runTest {
        // Given - Set current time to after the reveal date
        val revealDate = TimeUtils.getRevealDateMillis(mockAppConfig)
        val currentTime = revealDate + 1000 // 1 second after
        timeProvider.setCurrentTimeMillis(currentTime)

        // When
        viewModel.updateState(currentTime)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isRevealTime)
        assertFalse(state.showCurtain)
        assertTrue(state.showGift)
        assertEquals("0 dni, 00:00:00", state.countdownText)
    }

    @Test
    fun `updateState with no parameter uses timeProvider`() = runTest {
        // Given - Set current time in the timeProvider
        val revealDate = TimeUtils.getRevealDateMillis(mockAppConfig)
        val currentTime = revealDate - TimeUnit.HOURS.toMillis(2) // 2 hours before
        timeProvider.setCurrentTimeMillis(currentTime)

        // When
        viewModel.updateState() // No parameter, should use timeProvider

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isRevealTime)
        assertTrue(state.showCurtain)
        assertFalse(state.showGift)
        // The formatted string should show approximately 2 hours remaining
        assertTrue(state.countdownText.contains("0 dni"))
    }
}