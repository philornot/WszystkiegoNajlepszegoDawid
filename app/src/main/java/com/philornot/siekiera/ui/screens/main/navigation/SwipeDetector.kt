package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Detector for swipe gestures to control the navigation drawer. Detects
 * right-to-left and left-to-right swipes.
 */
object SwipeDetector {
    /**
     * Extension function to detect horizontal swipes.
     *
     * @param onSwipeLeft Callback triggered when user swipes from right to
     *    left
     * @param onSwipeRight Callback triggered when user swipes from left to
     *    right
     * @param thresholdPx Minimum horizontal distance in pixels for a swipe to
     *    be detected
     * @return Modified Modifier with swipe detection
     */
    fun Modifier.detectHorizontalSwipes(
        onSwipeLeft: () -> Unit = {},
        onSwipeRight: () -> Unit = {},
        thresholdPx: Float = 50f,
    ): Modifier = this.pointerInput(Unit) {
        var totalDragDistanceX = 0f

        detectHorizontalDragGestures(onDragStart = { totalDragDistanceX = 0f }, onDragEnd = {
            // Determine swipe direction based on accumulated distance
            if (totalDragDistanceX < -thresholdPx) {
                onSwipeLeft()
            } else if (totalDragDistanceX > thresholdPx) {
                onSwipeRight()
            }
            // Reset accumulator
            totalDragDistanceX = 0f
        }, onDragCancel = { totalDragDistanceX = 0f }, onHorizontalDrag = { _, dragAmount ->
            // Accumulate drag distance
            totalDragDistanceX += dragAmount
        })
    }
}