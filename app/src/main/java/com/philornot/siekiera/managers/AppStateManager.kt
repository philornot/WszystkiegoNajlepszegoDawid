package com.philornot.siekiera.managers

import com.philornot.siekiera.ui.screens.main.navigation.NavigationSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager zarządzający stanem aplikacji - drawer, nawigacja, tryb timera
 * itp. Enkapsuluje wszystkie StateFlow używane w MainActivity.
 */
class AppStateManager {

    // Stan szufladki nawigacyjnej
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    // Aktualna sekcja nawigacji
    private val _currentSection = MutableStateFlow(NavigationSection.BIRTHDAY_COUNTDOWN)
    val currentSection: StateFlow<NavigationSection> = _currentSection.asStateFlow()

    // Stan trybu timera
    private val _isTimerMode = MutableStateFlow(false)
    val isTimerMode: StateFlow<Boolean> = _isTimerMode.asStateFlow()

    // Aktywny czas timera (w milisekundach)
    private val _activeTimerRemainingTime = MutableStateFlow(0L)
    val activeTimerRemainingTime: StateFlow<Long> = _activeTimerRemainingTime.asStateFlow()

    // Stan pauzy timera
    private val _isTimerPaused = MutableStateFlow(false)
    val isTimerPaused: StateFlow<Boolean> = _isTimerPaused.asStateFlow()

    // Stan pobierania pliku
    private val _isDownloadInProgress = MutableStateFlow(false)

    /** Zmienia stan szufladki nawigacyjnej. */
    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    /** Zmienia aktualną sekcję nawigacji i aktualizuje powiązane stany. */
    fun setCurrentSection(section: NavigationSection) {
        _currentSection.value = section

        // Automatycznie aktualizuj tryb timera na podstawie sekcji
        when (section) {
            NavigationSection.TIMER -> {
                _isTimerMode.value = true
            }

            NavigationSection.BIRTHDAY_COUNTDOWN,
            NavigationSection.GIFT,
            NavigationSection.SETTINGS,
                -> {
                // W innych sekcjach wyłącz tryb timera tylko jeśli nie ma aktywnego timera
                if (_activeTimerRemainingTime.value <= 0 && !_isTimerPaused.value) {
                    _isTimerMode.value = false
                }
            }
        }
    }

    /** Aktualizuje pozostały czas timera. */
    fun setActiveTimerRemainingTime(time: Long) {
        _activeTimerRemainingTime.value = time
    }

    /** Ustawia stan pauzy timera. */
    fun setTimerPaused(paused: Boolean) {
        _isTimerPaused.value = paused
    }

    /** Resetuje stan timera do wartości początkowych. */
    fun resetTimerState() {
        _activeTimerRemainingTime.value = 0L
        _isTimerPaused.value = false
        _isTimerMode.value = false
    }

    /** Ustawia stan pobierania pliku. */
    fun setDownloadInProgress(inProgress: Boolean) {
        _isDownloadInProgress.value = inProgress
    }

    /** Sprawdza czy timer jest aktywny (ma pozostały czas lub jest spauzowany). */
    fun isTimerActive(): Boolean {
        return _activeTimerRemainingTime.value > 0 || _isTimerPaused.value
    }
}