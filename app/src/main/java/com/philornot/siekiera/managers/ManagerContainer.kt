package com.philornot.siekiera.managers

/**
 * Kontener przechowujący wszystkie managery aplikacji. Ułatwia
 * przekazywanie managerów między komponentami.
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

    /** Przywraca stan aplikacji przy starcie. */
    fun restoreApplicationState(
        giftReceived: Boolean,
        timerModeEnabled: Boolean,
    ) {
        appStateManager.setTimerModeDiscovered(timerModeEnabled)
        timerManager.restoreTimerAfterRestart()
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