package com.philornot.siekiera

import android.app.Application
import androidx.work.Configuration
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import com.philornot.siekiera.utils.TimeUtils
import timber.log.Timber

class GiftApp : Application(), Configuration.Provider {

    // Przechowuje referencję do konfiguracji
    private lateinit var appConfig: AppConfig

    override fun onCreate() {
        super.onCreate()

        // Inicjalizacja AppConfig PRZED jakimkolwiek użyciem
        appConfig = AppConfig.getInstance(applicationContext)

        // Inicjalizacja TimeUtils
        TimeUtils.initialize(applicationContext)

        // Inicjalizacja Timber do logowania
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Zaplanuj powiadomienie jeśli potrzebne
        checkAndScheduleNotification()
    }

    private fun checkAndScheduleNotification() {
        // Sprawdź czy powiadomienia są włączone w konfiguracji
        if (!appConfig.isBirthdayNotificationEnabled()) {
            Timber.d("Powiadomienia urodzinowe są wyłączone w konfiguracji")
            return
        }

        // Pobierz datę urodzin z konfiguracji
        val revealDateMillis = appConfig.getBirthdayTimeMillis()
        val currentTimeMillis = System.currentTimeMillis()

        if (currentTimeMillis < revealDateMillis) {
            Timber.d("Planowanie powiadomienia o odsłonięciu prezentu")
            NotificationScheduler.scheduleGiftRevealNotification(this, appConfig)
        } else {
            Timber.d("Data odsłonięcia już minęła, nie planuję powiadomienia")
        }
    }

    // Konfiguracja WorkManager
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()
}