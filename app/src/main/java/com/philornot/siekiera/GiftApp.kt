package com.philornot.siekiera

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Configuration
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.config.RemoteConfigManager
import com.philornot.siekiera.notification.NotificationHelper
import com.philornot.siekiera.notification.NotificationScheduler
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow

/**
 * Main application class responsible for initializing global components.
 *
 * Notifications are scheduled based on the date from configuration,
 * regardless of gift receipt status.
 */
class GiftApp : Application(), Configuration.Provider {

    // Stores reference to configuration
    private lateinit var appConfig: AppConfig

    // Retry configuration for remote config fetching
    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
        const val INITIAL_RETRY_DELAY_MS = 5000L // 5 seconds
        const val MAX_RETRY_DELAY_MS = 60000L // 1 minute
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize AppConfig BEFORE any usage
        appConfig = AppConfig.getInstance(applicationContext)

        // Initialize TimeUtils
        TimeUtils.initialize(applicationContext)

        // Initialize notification channels
        NotificationHelper.initNotificationChannels(applicationContext)

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 🆕 Fetch remote configuration asynchronously with smart retry
        fetchRemoteConfigWithRetry()

        // Schedule notification if needed
        checkAndScheduleNotification()
    }

    /**
     * Checks if device has active internet connection.
     *
     * @return true if connected to internet, false otherwise
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false

        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
        } catch (e: Exception) {
            Timber.d("Network check failed: ${e.message}")
            false
        }
    }

    /**
     * Fetches remote configuration from Google Drive with intelligent retry
     * mechanism.
     * - Checks internet connectivity before attempting fetch
     * - Uses exponential backoff for retries
     * - Gracefully handles network unavailability
     * - Does not log errors to Sentry for expected failures (no internet, no
     *   file)
     */
    private fun fetchRemoteConfigWithRetry() {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var attempt = 0
            var success = false

            while (attempt < MAX_RETRY_ATTEMPTS && !success) {
                attempt++

                // Check network availability before attempt
                if (!isNetworkAvailable()) {
                    Timber.d("⏸️ Remote config fetch skipped - no internet connection (attempt $attempt/$MAX_RETRY_ATTEMPTS)")

                    // Wait before next attempt with exponential backoff
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val delayMs = min(
                            INITIAL_RETRY_DELAY_MS * 2.0.pow(attempt - 1).toLong(),
                            MAX_RETRY_DELAY_MS
                        )
                        Timber.d("⏰ Retrying remote config fetch in ${delayMs / 1000}s...")
                        delay(delayMs)
                    }
                    continue
                }

                try {
                    val remoteConfig = RemoteConfigManager.getInstance(applicationContext)
                    val folderId = appConfig.getDriveFolderId()

                    Timber.d("🔄 Attempting to fetch remote config (attempt $attempt/$MAX_RETRY_ATTEMPTS)")
                    success = remoteConfig.fetchRemoteConfig(folderId)

                    if (success) {
                        Timber.d("✅ Remote config fetched successfully")

                        // Clear cached birthday date to force reload
                        AppConfig.clearInstance()
                        appConfig = AppConfig.getInstance(applicationContext)

                        // Reschedule notifications with new config
                        kotlinx.coroutines.MainScope().launch {
                            checkAndScheduleNotification()
                        }
                    } else {
                        Timber.d("ℹ️ No remote config available or not updated (attempt $attempt/$MAX_RETRY_ATTEMPTS)")

                        // If no config file exists, don't retry - this is expected
                        break
                    }
                } catch (e: Exception) {
                    // Distinguish between network errors and other errors
                    val isNetworkError = e is java.net.UnknownHostException || e.message?.contains(
                        "network", ignoreCase = true
                    ) == true || e.message?.contains(
                        "connection", ignoreCase = true
                    ) == true || e.message?.contains("timeout", ignoreCase = true) == true

                    if (isNetworkError) {
                        Timber.d("🌐 Network error during remote config fetch (attempt $attempt/$MAX_RETRY_ATTEMPTS): ${e.message}")

                        // Retry network errors
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            val delayMs = min(
                                INITIAL_RETRY_DELAY_MS * 2.0.pow(attempt - 1).toLong(),
                                MAX_RETRY_DELAY_MS
                            )
                            Timber.d("⏰ Retrying in ${delayMs / 1000}s...")
                            delay(delayMs)
                        }
                    } else {
                        // Non-network errors (e.g., file not found, config error) - don't retry
                        Timber.d("ℹ️ Remote config not available: ${e.message}")
                        break
                    }
                }
            }

            if (!success && attempt >= MAX_RETRY_ATTEMPTS) {
                Timber.d("⚠️ Remote config fetch failed after $MAX_RETRY_ATTEMPTS attempts - using local config")
            }
        }
    }

    /**
     * Checks and schedules birthday notification based on date from
     * configuration.
     *
     * Does not check if gift was received - notification is scheduled only if
     * birthday date is in the future.
     */
    private fun checkAndScheduleNotification() {
        // Check if birthday notifications are enabled in configuration
        if (!appConfig.isBirthdayNotificationEnabled()) {
            Timber.d("Birthday notifications disabled in configuration")
            return
        }

        // Get birthday date from configuration
        val revealDateMillis = appConfig.getBirthdayTimeMillis()
        val currentTimeMillis = System.currentTimeMillis()

        if (currentTimeMillis < revealDateMillis) {
            Timber.d("Scheduling gift reveal notification")
            NotificationScheduler.scheduleGiftRevealNotification(this, appConfig)
        } else {
            Timber.d("Birthday date already passed, not scheduling notification")
        }
    }

    // WorkManager configuration
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()
}