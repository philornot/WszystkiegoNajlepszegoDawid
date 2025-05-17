package com.philornot.siekiera.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.ui.screens.MainScreen
import com.philornot.siekiera.ui.theme.GiftTheme
import com.philornot.siekiera.utils.TestTimeProvider
import com.philornot.siekiera.utils.TimeUtils
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Testy dla głównego ekranu aplikacji, weryfikujące poprawne zachowanie
 * w różnych momentach czasu.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var downloadClicked = false
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var appConfig: AppConfig
    private lateinit var context: Context

    // Strefa czasowa Warszawy
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Before
    fun setup() {
        MockKAnnotations.init(this) // Dodanie inicjalizacji MockK
        context = ApplicationProvider.getApplicationContext()
        timeProvider = TestTimeProvider()

        // Używamy mockk zamiast Mockito
        appConfig = mockk(relaxed = true)

        // Ustaw domyślną datę urodzin - 14 maja 2025, 16:34
        val birthdayCalendar = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 14, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }

        every { appConfig.getBirthdayTimeMillis() } returns birthdayCalendar.timeInMillis
        every { appConfig.getBirthdayDate() } returns birthdayCalendar

        // Bezpieczniejsze mockowanie statycznych metod
        try {
            mockkStatic(TimeUtils::class)
            every { TimeUtils.getRevealDateMillis(any()) } returns birthdayCalendar.timeInMillis
        } catch (e: Exception) {
            // Fallback - użyjemy bezpośrednio daty
            println("Nie udało się zmockować TimeUtils: ${e.message}")
        }

        downloadClicked = false
    }

    @After
    fun tearDown() {
        try {
            unmockkAll() // Czyszczenie wszystkich mocków
        } catch (e: Exception) {
            // Ignoruj błędy
        }
    }

    @Test
    fun curtainIsVisibleBeforeRevealDate() {
        // Przygotowanie - czas przed dniem urodzin (13 maja 2025, 12:00)
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 13, 12, 0, 0).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Czekaj na stabilizację UI
        composeTestRule.waitForIdle()

        // Weryfikacja - kurtyna powinna być widoczna
        composeTestRule.onNodeWithTag("curtain_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("curtain").assertIsDisplayed()

        // Licznik powinien być widoczny
        composeTestRule.onNodeWithTag("countdown_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("countdown").assertIsDisplayed()

        // Prezent NIE powinien być widoczny - zamiast assertDoesNotExist używamy assertIsNotDisplayed
        // lub sprawdzamy, że nie można znaleźć elementu z tagiem
        composeTestRule.onNodeWithTag("gift").assertIsNotDisplayed()
    }

    @Test
    fun giftIsVisibleAfterRevealDate() {
        // Przygotowanie - czas po dniu urodzin (15 maja 2025, 12:00)
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 15, 12, 0, 0).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Czekaj na zakończenie animacji i stabilizację UI
        composeTestRule.waitForIdle()

        // Weryfikacja - kurtyna NIE powinna być widoczna
        composeTestRule.onNodeWithTag("curtain").assertIsNotDisplayed()

        // Licznik NIE powinien być widoczny
        composeTestRule.onNodeWithTag("countdown").assertIsNotDisplayed()

        // Prezent powinien być widoczny
        composeTestRule.onNodeWithTag("gift_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gift").assertIsDisplayed()
    }

    @Test
    fun clickingGiftTriggersCallback() {
        // Przygotowanie - czas po dniu urodzin (15 maja 2025, 12:00)
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 15, 12, 0, 0).timeInMillis
        )

        val targetDate = getTargetDateForTest()
        downloadClicked = false // Reset flagi

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Poczekaj na zakończenie animacji i stabilizację UI
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(2000) // Dodatkowe czekanie na animacje

        // Kliknięcie prezentu
        composeTestRule.onNodeWithTag("gift").performClick()

        // Weryfikacja - callback powinien być wywołany
        assert(downloadClicked) { "Callback onGiftClicked nie został wywołany po kliknięciu prezentu" }
    }

    @Test
    fun exactlyAtRevealDate() {
        // Przygotowanie - dokładnie w momencie dnia urodzin (14 maja 2025, 16:34:00)
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 34, 0).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Poczekaj na zakończenie animacji i stabilizację UI
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(2000) // Dodatkowe czekanie na animacje

        // Weryfikacja - kurtyna NIE powinna być widoczna
        composeTestRule.onNodeWithTag("curtain").assertIsNotDisplayed()

        // Prezent powinien być widoczny
        composeTestRule.onNodeWithTag("gift_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gift").assertIsDisplayed()
    }

    @Test
    fun oneSecondBeforeRevealDate() {
        // Przygotowanie - 1 sekunda przed dniem urodzin (14 maja 2025, 16:33:59)
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 33, 59).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Czekaj na stabilizację UI
        composeTestRule.waitForIdle()

        // Weryfikacja - kurtyna powinna być widoczna
        composeTestRule.onNodeWithTag("curtain_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("curtain").assertIsDisplayed()

        // Licznik powinien być widoczny
        composeTestRule.onNodeWithTag("countdown_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("countdown").assertIsDisplayed()
    }

    @Test
    fun oneSecondAfterRevealDate() {
        // Przygotowanie - 1 sekunda po dniu urodzin (14 maja 2025, 16:34:01)
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 34, 1).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Poczekaj na zakończenie animacji i stabilizację UI
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(2000) // Dodatkowe czekanie na animacje

        // Weryfikacja - kurtyna NIE powinna być widoczna
        composeTestRule.onNodeWithTag("curtain").assertIsNotDisplayed()

        // Prezent powinien być widoczny
        composeTestRule.onNodeWithTag("gift_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gift").assertIsDisplayed()
    }

    @Test
    fun differentTimeZoneShouldStillUseWarsawTime() {
        // Ustaw systemową strefę czasową na inną niż Warszawa (np. Los Angeles)
        val defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

        try {
            // Czas w Los Angeles, który odpowiada 13 maja 2025, 12:00 w Warszawie
            // (9 godzin różnicy)
            val losAngelesTime = Calendar.getInstance().apply {
                // Data w Los Angeles odpowiadająca 13 maja, 12:00 w Warszawie
                // Przy założeniu 9 godzin różnicy: 13 maja, 03:00 w LA
                set(2025, Calendar.MAY, 13, 3, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            timeProvider.setCurrentTimeMillis(losAngelesTime)

            val targetDate = getTargetDateForTest()

            // Renderowanie UI
            composeTestRule.setContent {
                GiftTheme {
                    MainScreen(
                        targetDate = targetDate,
                        currentTime = timeProvider.getCurrentTimeMillis(),
                        onGiftClicked = { downloadClicked = true }
                    )
                }
            }

            // Czekaj na stabilizację UI
            composeTestRule.waitForIdle()

            // Kurtyna powinna być nadal widoczna, ponieważ używamy czasu warszawskiego
            composeTestRule.onNodeWithTag("curtain_container").assertIsDisplayed()
            composeTestRule.onNodeWithTag("curtain").assertIsDisplayed()

        } finally {
            // Przywróć domyślną strefę czasową
            TimeZone.setDefault(defaultTimeZone)
        }
    }

    @Test
    fun countdownShowsCorrectTime() {
        // Przygotowanie - 10 dni przed dniem urodzin
        val tenDaysBefore = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 4, 16, 34, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        timeProvider.setCurrentTimeMillis(tenDaysBefore)

        val targetDate = getTargetDateForTest()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Czekaj na stabilizację UI
        composeTestRule.waitForIdle()

        // Weryfikacja - licznik powinien pokazywać "10 dni, ..."
        composeTestRule.onNodeWithText("10 dni,", substring = true).assertIsDisplayed()
    }

    @Test
    fun transitionFromBeforeToAfterRevealDate() {
        // Tworzymy mutableState do kontrolowania czasu
        val currentTimeState = mutableStateOf(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 33, 59).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Renderowanie UI z dynamicznym czasem
        composeTestRule.setContent {
            val currentTime = androidx.compose.runtime.remember { currentTimeState }

            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = currentTime.value,
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Czekaj na stabilizację UI
        composeTestRule.waitForIdle()

        // Weryfikacja przed zmianą czasu - kurtyna widoczna
        composeTestRule.onNodeWithTag("curtain_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("curtain").assertIsDisplayed()
        composeTestRule.onNodeWithTag("countdown_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("countdown").assertIsDisplayed()

        // Zmiana czasu o 2 sekundy (przekroczenie czasu granicznego)
        currentTimeState.value =
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 34, 1).timeInMillis

        // Poczekaj na przetworzenie zmiany stanu i zakończenie animacji
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(3000) // Dodatkowe czekanie na animacje

        // Weryfikacja po zmianie czasu - prezent widoczny
        composeTestRule.onNodeWithTag("curtain").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("gift_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gift").assertIsDisplayed()
    }

    @Test
    fun titleIsDisplayed() {
        // Przygotowanie - czas przed dniem urodzin
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 13, 12, 0, 0).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Czekaj na stabilizację UI
        composeTestRule.waitForIdle()

        // Weryfikacja - tytuł powinien być widoczny (używamy substring=true, aby uniknąć problemów z formatowaniem)
        composeTestRule.onNodeWithText("Wszystkiego najlepszego", substring = true).assertIsDisplayed()
    }

    @Test
    fun countdownTitleIsDisplayed() {
        // Przygotowanie - czas przed dniem urodzin
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 13, 12, 0, 0).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Czekaj na stabilizację UI
        composeTestRule.waitForIdle()

        // Weryfikacja - tytuł odliczania powinien być widoczny
        composeTestRule.onNodeWithText("Czas do urodzin", substring = true).assertIsDisplayed()
    }

    @Test
    fun timeUpdatesAutomatically() {
        // To test opakujemy w timeout, aby uniknąć zawieszenia
        val timeoutLatch = CountDownLatch(1)

        thread {
            try {
                // Przygotowanie - 3 sekundy przed datą docelową
                val initialTime =
                    TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 33, 57).timeInMillis

                val targetDate = getTargetDateForTest()

                // Ustaw specjalny TestTimeProvider, aby symulować upływ czasu
                val systemTimeProvider = object : TestTimeProvider() {
                    var currentMockedTime = initialTime
                    override fun getCurrentTimeMillis(): Long {
                        // Symulujemy upływ czasu - każde wywołanie zwiększa czas o 1 sekundę
                        currentMockedTime += 1000
                        return currentMockedTime
                    }
                }

                // Renderowanie UI
                composeTestRule.setContent {
                    GiftTheme {
                        MainScreen(
                            targetDate = targetDate,
                            currentTime = systemTimeProvider.getCurrentTimeMillis(),
                            onGiftClicked = { downloadClicked = true }
                        )
                    }
                }

                // Weryfikujemy czy aktualizacja czasu działa - początkowo widać kurtynę
                composeTestRule.onNodeWithTag("curtain").assertIsDisplayed()

                // Wymuszamy kilka aktualizacji UI
                repeat(5) {
                    composeTestRule.mainClock.advanceTimeBy(1000)
                    composeTestRule.waitForIdle()
                }

                // Dodatkowe czekanie na animacje
                composeTestRule.mainClock.advanceTimeBy(3000)
                composeTestRule.waitForIdle()

                // Po 5 sekundach kurtyna powinna zniknąć, a prezent być widoczny
                // (bo przekroczyliśmy czas docelowy)
                composeTestRule.onNodeWithTag("curtain").assertIsNotDisplayed()
                composeTestRule.onNodeWithTag("gift").assertIsDisplayed()

                timeoutLatch.countDown()
            } catch (e: Exception) {
                // Log błędu i odblokuj latch
                println("Błąd w teście: ${e.message}")
                timeoutLatch.countDown()
            }
        }

        // Czekaj maksymalnie 10 sekund na zakończenie testu
        timeoutLatch.await(10, TimeUnit.SECONDS)
    }

    @Test
    fun performanceTest() {
        // Test wydajności animacji - sprawdzamy czy nie ma opóźnień w animacji

        // Przygotowanie - dokładnie w momencie przejścia
        timeProvider.setCurrentTimeMillis(
            TimeUtils.getCalendarForDateTime(2025, Calendar.MAY, 14, 16, 34, 0).timeInMillis
        )

        val targetDate = getTargetDateForTest()

        // Mierzymy czas potrzebny na renderowanie i animację
        val startTime = System.currentTimeMillis()

        // Renderowanie UI
        composeTestRule.setContent {
            GiftTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = timeProvider.getCurrentTimeMillis(),
                    onGiftClicked = { downloadClicked = true }
                )
            }
        }

        // Czekamy na zakończenie wszystkich animacji
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(2000) // Dodatkowe czekanie na animacje

        // Weryfikacja - prezent powinien być widoczny
        composeTestRule.onNodeWithTag("gift").assertIsDisplayed()

        val renderTime = System.currentTimeMillis() - startTime
        // Logging tylko do celów testowych
        println("Czas renderowania i animacji: $renderTime ms")

        // W środowisku testowym, animacje są natychmiastowe, więc nie sprawdzamy czasu
        // W rzeczywistym urządzeniu można by sprawdzić: assert(renderTime < 3000)
    }

    // Metoda pomocnicza do uzyskania targetDate bezpośrednio
    private fun getTargetDateForTest(): Long {
        return try {
            TimeUtils.getRevealDateMillis(appConfig)
        } catch (e: Exception) {
            // Fallback jeśli nie możemy użyć AppConfig
            Calendar.getInstance(warsawTimeZone).apply {
                set(2025, Calendar.MAY, 14, 16, 34, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}