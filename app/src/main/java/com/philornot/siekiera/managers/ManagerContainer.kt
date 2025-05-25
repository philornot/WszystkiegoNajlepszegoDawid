package com.philornot.siekiera.managers

import com.philornot.siekiera.ui.screens.main.drawer.NavigationSection
import timber.log.Timber

/**
 * Kontener przechowujący wszystkie managery aplikacji. Ułatwia
 * przekazywanie managerów między komponentami.
 *
 * AKTUALIZACJA: Dodano inteligentną logikę nawigacji, która zapewnia że po
 * odebraniu prezentu domyślną sekcją jest "Odliczanie do urodzin", chyba
 * że timer jest rzeczywiście aktywny.
 */
data class ManagerContainer(
    val appStateManager: AppStateManager,
    val permissionsManager: PermissionsManager,
    val timerManager: TimerManager,
    val settingsManager: SettingsManager,
    val fileManager: FileManager,
    val birthdayNotificationManager: BirthdayNotificationManager,
) {

    /** Inicjalizuje wszystkie managery, które wymagają inicjalizacji. */
    fun initialize() {
        settingsManager.loadAppSettings()
        fileManager.initialize()
    }

    /** Czyści zasoby wszystkich managerów. */
    fun cleanup() {
        fileManager.cleanup()
    }

    /**
     * Przywraca stan aplikacji przy starcie z inteligentną logiką nawigacji.
     * Zapewnia, że po odebraniu prezentu domyślną sekcją jest "Odliczanie do
     * urodzin", chyba że timer jest aktywny.
     *
     * @param giftReceived Czy prezent został już odebrany
     * @param timerModeEnabled Czy tryb timera został odkryty przez użytkownika
     */
    fun restoreApplicationState(
        giftReceived: Boolean,
        timerModeEnabled: Boolean,
    ) {
        Timber.d("Przywracanie stanu aplikacji: giftReceived=$giftReceived, timerModeEnabled=$timerModeEnabled")

        // Ustaw stan odkrycia trybu timera
        appStateManager.setTimerModeDiscovered(timerModeEnabled)

        // Przywróć timer jeśli był aktywny - metoda zwraca true jeśli timer jest rzeczywiście aktywny
        val timerIsActive = timerManager.restoreTimerAfterRestart()

        // Inteligentnie ustaw sekcję nawigacji na podstawie stanu aplikacji
        val targetSection = when {
            timerIsActive -> {
                // Timer jest aktywny - przełącz na sekcję timera
                Timber.d("Timer jest aktywny po przywróceniu - ustawiam sekcję TIMER")
                NavigationSection.TIMER
            }

            giftReceived -> {
                // Prezent został odebrany ale timer nie jest aktywny - ustaw na urodziny
                Timber.d("Prezent odebrany, timer nieaktywny - ustawiam sekcję BIRTHDAY_COUNTDOWN")
                NavigationSection.BIRTHDAY_COUNTDOWN
            }

            else -> {
                // Prezent nie został odebrany - pozostaw domyślną sekcję urodzin
                Timber.d("Prezent nie odebrany - pozostawiam domyślną sekcję BIRTHDAY_COUNTDOWN")
                NavigationSection.BIRTHDAY_COUNTDOWN
            }
        }

        // Ustaw docelową sekcję
        appStateManager.setCurrentSection(targetSection)

        Timber.d("Stan aplikacji przywrócony: sekcja=${targetSection.name}, timer aktywny=$timerIsActive")
    }

    /** Ustawia powiadomienia i zadania w tle. */
    fun setupNotificationsAndTasks() {
        birthdayNotificationManager.scheduleRevealNotification()
    }

    /** Sprawdza stan przy wznowieniu aplikacji. */
    fun checkStateOnResume() {
        permissionsManager.checkPermissionsOnResume()
        timerManager.checkTimerOnResume()
    }
}