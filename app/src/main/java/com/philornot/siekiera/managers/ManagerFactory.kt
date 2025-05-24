package com.philornot.siekiera.managers

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import com.philornot.siekiera.config.AppConfig

/**
 * Fabryka do tworzenia managerów aplikacji. Enkapsuluje logikę dependency
 * injection i ułatwia testowanie.
 */
class ManagerFactory(
    private val context: Context,
    private val appConfig: AppConfig,
    private val prefs: SharedPreferences,
) {

    /** Tworzy AppStateManager. */
    fun createAppStateManager(): AppStateManager {
        return AppStateManager()
    }

    /** Tworzy PermissionsManager. */
    fun createPermissionsManager(
        activity: ComponentActivity,
        onAlarmPermissionGranted: () -> Unit,
    ): PermissionsManager {
        return PermissionsManager(
            activity = activity,
            appConfig = appConfig,
            onAlarmPermissionGranted = onAlarmPermissionGranted
        )
    }

    /** Tworzy TimerManager. */
    fun createTimerManager(appStateManager: AppStateManager): TimerManager {
        return TimerManager(
            context = context, appStateManager = appStateManager
        )
    }

    /** Tworzy SettingsManager. */
    fun createSettingsManager(): SettingsManager {
        return SettingsManager(context = context)
    }

    /** Tworzy FileManager. */
    fun createFileManager(
        appStateManager: AppStateManager,
        lifecycleOwner: LifecycleOwner,
    ): FileManager {
        return FileManager(
            context = context,
            appConfig = appConfig,
            appStateManager = appStateManager,
            prefs = prefs,
            lifecycleOwner = lifecycleOwner
        )
    }

    /** Tworzy BirthdayNotificationManager. */
    fun createBirthdayNotificationManager(): BirthdayNotificationManager {
        return BirthdayNotificationManager(
            context = context, appConfig = appConfig, prefs = prefs
        )
    }

    /**
     * Tworzy wszystkie managery w odpowiedniej kolejności z właściwymi
     * zależnościami. Zwraca obiekt zawierający wszystkie managery.
     */
    fun createAllManagers(
        activity: ComponentActivity,
        lifecycleOwner: LifecycleOwner,
    ): ManagerContainer {
        // Kolejność ma znaczenie ze względu na zależności
        val appStateManager = createAppStateManager()
        val settingsManager = createSettingsManager()
        val birthdayNotificationManager = createBirthdayNotificationManager()

        val permissionsManager = createPermissionsManager(
            activity = activity, onAlarmPermissionGranted = {
                birthdayNotificationManager.scheduleRevealNotification()
            })

        val timerManager = createTimerManager(appStateManager)
        val fileManager = createFileManager(appStateManager, lifecycleOwner)

        return ManagerContainer(
            appStateManager = appStateManager,
            permissionsManager = permissionsManager,
            timerManager = timerManager,
            settingsManager = settingsManager,
            fileManager = fileManager,
            birthdayNotificationManager = birthdayNotificationManager
        )
    }
}

