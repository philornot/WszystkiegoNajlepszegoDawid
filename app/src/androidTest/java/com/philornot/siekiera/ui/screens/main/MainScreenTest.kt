package com.philornot.siekiera.ui.screens.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.philornot.siekiera.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Warsaw timezone
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Test
    fun curtainIsVisibleBeforeRevealDate() {
        // Setup - Time before birthday
        val currentTime = Calendar.getInstance(warsawTimeZone).apply {
            set(2024, Calendar.DECEMBER, 1, 12, 0, 0)
        }.timeInMillis

        val targetDate = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.AUGUST, 24, 0, 0, 0)
        }.timeInMillis

        // Render UI
        composeTestRule.setContent {
            AppTheme {
                MainScreen(
                    targetDate = targetDate, currentTime = currentTime, onGiftClicked = {})
            }
        }

        // Wait for animations to complete
        composeTestRule.waitForIdle()

        // Verify curtain is visible and gift is not
        composeTestRule.onNodeWithTag("curtain").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gift").assertDoesNotExist()

        // Verify countdown is visible with title
        composeTestRule.onNodeWithTag("countdown_container").assertIsDisplayed()
        composeTestRule.onNodeWithText("Czas do urodzin:").assertIsDisplayed()
    }

    @Test
    fun giftIsVisibleAfterRevealDate() {
        // Setup - Time after birthday
        val currentTime = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.AUGUST, 25, 12, 0, 0)
        }.timeInMillis

        val targetDate = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.AUGUST, 24, 0, 0, 0)
        }.timeInMillis

        // Render UI
        composeTestRule.setContent {
            AppTheme {
                MainScreen(
                    targetDate = targetDate, currentTime = currentTime, onGiftClicked = {})
            }
        }

        // Wait for animations to complete
        composeTestRule.waitForIdle()

        // Verify curtain is not visible and gift is visible
        composeTestRule.onNodeWithTag("curtain").assertDoesNotExist()
        composeTestRule.onNodeWithTag("gift").assertIsDisplayed()
    }

    @Test
    fun clickingGiftShowsCelebrationScreen() {
        // Setup - Time after birthday
        val currentTime = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.AUGUST, 25, 12, 0, 0)
        }.timeInMillis

        val targetDate = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.AUGUST, 24, 0, 0, 0)
        }.timeInMillis

        var giftClicked = false

        // Render UI
        composeTestRule.setContent {
            AppTheme {
                MainScreen(
                    targetDate = targetDate,
                    currentTime = currentTime,
                    onGiftClicked = { giftClicked = true })
            }
        }

        // Wait for animations to complete
        composeTestRule.waitForIdle()

        // Make sure the gift is visible before clicking
        composeTestRule.onNodeWithTag("gift").assertIsDisplayed()

        // Click on gift
        composeTestRule.onNodeWithTag("gift").performClick()

        // Wait for celebrations animations to complete
        composeTestRule.waitForIdle()

        // Verify callback was called
        assert(giftClicked) { "Gift click callback was not triggered" }

        // Verify celebration screen appears (look for "18 Lat!" text)
        composeTestRule.onNodeWithText("18 Lat!").assertIsDisplayed()
    }

    @Test
    fun countdownShowsCorrectTimeFormat() {
        // Setup - Exactly 97 days before birthday
        val targetDate = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.AUGUST, 24, 0, 0, 0)
        }.timeInMillis

        val currentTime = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.MAY, 19, 0, 0, 0) // 97 days before
            // Calculate exact time for 97 days before target
            val daysInMillis = TimeUnit.DAYS.toMillis(97)
            timeInMillis = targetDate - daysInMillis
        }.timeInMillis

        // Render UI
        composeTestRule.setContent {
            AppTheme {
                MainScreen(
                    targetDate = targetDate, currentTime = currentTime, onGiftClicked = {})
            }
        }

        // Wait for UI to be ready
        composeTestRule.waitForIdle()

        // Verify days count is displayed correctly
        composeTestRule.onNodeWithText("97").assertIsDisplayed()
        composeTestRule.onNodeWithText("dni").assertIsDisplayed()

        // Verify time format with hours, minutes, seconds is displayed
        composeTestRule.onNodeWithText("Godzin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Minut").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sekund").assertIsDisplayed()

        // Verify separation colons are displayed
        composeTestRule.onAllNodesWithText(":").fetchSemanticsNodes().size.let { count ->
            assert(count == 2) { "Expected 2 colon separators, found $count" }
        }
    }

    @Test
    fun headerShowsCorrectTitle() {
        // Render UI with any time
        composeTestRule.setContent {
            AppTheme {
                MainScreen(
                    targetDate = System.currentTimeMillis() + 1000000,
                    currentTime = System.currentTimeMillis(),
                    onGiftClicked = {})
            }
        }

        // Wait for UI to render
        composeTestRule.waitForIdle()

        // Verify correct header text is displayed
        composeTestRule.onNodeWithText("Wszystkiego najlepszego").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dawid!").assertIsDisplayed()
    }

    @Test
    fun digitAnimationsDoNotCrashUI() {
        // Test that our various digit animations don't crash the UI
        // Setup with time that has a mix of digit values
        val targetDate = Calendar.getInstance(warsawTimeZone).apply {
            set(2025, Calendar.AUGUST, 24, 0, 0, 0)
        }.timeInMillis

        val currentTime =
            targetDate - TimeUnit.DAYS.toMillis(123) - TimeUnit.HOURS.toMillis(12) - TimeUnit.MINUTES.toMillis(
                34
            ) - TimeUnit.SECONDS.toMillis(56)

        // Render UI
        composeTestRule.setContent {
            AppTheme {
                MainScreen(
                    targetDate = targetDate, currentTime = currentTime, onGiftClicked = {})
            }
        }

        // Force a second render with slightly different time to trigger animations
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(1000) // Advance one second

        // Wait for UI and animations to complete
        composeTestRule.waitForIdle()

        // If we get here without crashes, digit animations are working
        // Verify at least digits are visible
        composeTestRule.onNodeWithTag("countdown_container").assertIsDisplayed()
    }
}