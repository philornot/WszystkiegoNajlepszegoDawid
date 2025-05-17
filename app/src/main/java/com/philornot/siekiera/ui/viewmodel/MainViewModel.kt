package com.philornot.siekiera.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.philornot.siekiera.utils.RealTimeProvider
import com.philornot.siekiera.utils.TimeProvider
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel do zarządzania stanem głównego ekranu aplikacji. Zawiera
 * logikę związaną z odliczaniem i odsłanianiem prezentu.
 */
class MainViewModel(
    private val timeProvider: TimeProvider = RealTimeProvider(),
) : ViewModel() {

    // Wewnętrzny stan UI
    private val _uiState = MutableStateFlow(MainScreenState())

    // Publiczny stan UI dostępny do obserwacji
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    /** Aktualizuje stan UI na podstawie bieżącego czasu. */
    fun updateState(currentTimeMillis: Long = timeProvider.getCurrentTimeMillis()) {
        val timeRemaining = getTimeRemaining(currentTimeMillis)
        val isTimeUp = isTimeUp(currentTimeMillis)

        _uiState.value = MainScreenState(
            isRevealTime = isTimeUp,
            countdownText = formatCountdown(timeRemaining),
            showCurtain = !isTimeUp,
            showGift = isTimeUp
        )
    }

    /** Oblicza pozostały czas do ujawnienia prezentu. */
    fun getTimeRemaining(currentTimeMillis: Long = timeProvider.getCurrentTimeMillis()): Long {
        val revealTime = TimeUtils.getRevealDateMillis()
        return (revealTime - currentTimeMillis).coerceAtLeast(0)
    }

    /** Sprawdza, czy nadszedł czas ujawnienia prezentu. */
    fun isTimeUp(currentTimeMillis: Long = timeProvider.getCurrentTimeMillis()): Boolean {
        return currentTimeMillis >= TimeUtils.getRevealDateMillis()
    }

    /** Formatuje pozostały czas jako tekst do wyświetlenia. */
    fun formatCountdown(timeRemainingMillis: Long): String {
        return TimeUtils.formatRemainingTime(timeRemainingMillis)
    }
}

/** Klasa reprezentująca stan UI głównego ekranu. */
data class MainScreenState(
    val isRevealTime: Boolean = false,
    val countdownText: String = "",
    val showCurtain: Boolean = true,
    val showGift: Boolean = false,
)