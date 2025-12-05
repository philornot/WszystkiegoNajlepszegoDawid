package com.philornot.siekiera.config

import android.content.Context
import com.philornot.siekiera.BuildConfig
import com.philornot.siekiera.R
import com.philornot.siekiera.utils.TimeUtils
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.TimeZone

/**
 * Application configuration class that provides centralized access to all
 * configuration parameters.
 *
 * All values are read from res/values/config.xml and BuildConfig, but
 * can be overridden by AdminConfigManager for easy configuration changes
 * without reinstalling the app.
 *
 * Uses safe approach with WeakReference and lazy initialization.
 */
class AppConfig private constructor(context: Context) {

    // Use WeakReference to context to avoid memory leaks
    private val contextRef: WeakReference<Context> = WeakReference(context.applicationContext)

    // Admin config manager for overriding default values
    private val adminConfig: AdminConfigManager by lazy {
        AdminConfigManager.getInstance(context.applicationContext)
    }

    // Remote config manager for cross-device configuration
    private val remoteConfig: RemoteConfigManager by lazy {
        RemoteConfigManager.getInstance(context.applicationContext)
    }

    // Warsaw timezone
    private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

    // Cache for frequently used values
    private var _birthdayDateCache: Calendar? = null
    private var _driveFolderIdCache: String? = null

    /**
     * Getter for context that checks if reference is still valid.
     *
     * @throws IllegalStateException if context is not available
     */
    private fun getContext(): Context {
        return contextRef.get()
            ?: throw IllegalStateException("Application context is not available - possible memory leak")
    }

    /**
     * Gets birthday date as Calendar object in Warsaw timezone.
     *
     * Priority: Remote Config > Admin Config > config.xml Result is cached for
     * better performance.
     *
     * @return Calendar object with set birthday date and time
     */
    fun getBirthdayDate(): Calendar {
        // Priority 1: Remote config (cross-device)
        remoteConfig.getRemoteBirthdayDate()?.let {
            Timber.d("Using remote-configured birthday date")
            return it
        }

        // Priority 2: Admin config (local override)
        adminConfig.getAdminBirthdayDate()?.let {
            Timber.d("Using admin-configured birthday date")
            return it
        }

        // Priority 3: Default from config.xml
        // Check cache
        _birthdayDateCache?.let { return it }

        val context = getContext()

        val year = context.resources.getInteger(R.integer.birthday_year)
        // Months in Calendar are indexed from 0, but in config.xml we use 1-12 for readability
        val month = context.resources.getInteger(R.integer.birthday_month) - 1
        val day = context.resources.getInteger(R.integer.birthday_day)
        val hour = context.resources.getInteger(R.integer.birthday_hour)
        val minute = context.resources.getInteger(R.integer.birthday_minute)

        val calendar = Calendar.getInstance(WARSAW_TIMEZONE).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Cache result
        _birthdayDateCache = calendar

        if (isVerboseLoggingEnabled()) {
            Timber.d("Birthday date from config: ${TimeUtils.formatDate(calendar.time)}")
        }

        return calendar
    }

    /**
     * Gets birthday date in milliseconds.
     *
     * @return Time in milliseconds
     */
    fun getBirthdayTimeMillis(): Long {
        // Check admin config first
        adminConfig.getAdminBirthdayTimeMillis()?.let {
            return it
        }

        return getBirthdayDate().timeInMillis
    }

    /**
     * Gets Google Drive folder ID. Uses only BuildConfig (from
     * local.properties or CI/CD) but can be overridden by AdminConfigManager.
     *
     * @return Google Drive folder ID
     * @throws IllegalStateException if ID is not configured
     */
    fun getDriveFolderId(): String {
        // Check if admin config overrides default
        adminConfig.getAdminDriveFolderId()?.let {
            Timber.d("Using admin-configured Drive folder ID")
            return it
        }

        // Check cache
        _driveFolderIdCache?.let { return it }

        val folderId = BuildConfig.GDRIVE_FOLDER_ID

        if (folderId.isBlank()) {
            throw IllegalStateException(
                "Google Drive folder ID is not configured. " + "Set gdrive.folder.id in local.properties or define in BuildConfig."
            )
        }

        // Cache result
        _driveFolderIdCache = folderId
        return folderId
    }

    /**
     * Gets service account file name from resources.
     *
     * @return File name (without .json extension)
     */
    fun getServiceAccountFileName(): String {
        val context = getContext()
        return context.resources.getString(R.string.service_account_file)
    }

    /**
     * Gets Daylio file name, can be overridden by Remote or Admin Config.
     *
     * Priority: Remote Config > Admin Config > config.xml
     *
     * @return Daylio file name
     */
    fun getDaylioFileName(): String {
        // Priority 1: Remote config
        remoteConfig.getRemoteDaylioFileName()?.let {
            Timber.d("Using remote-configured Daylio file name")
            return it
        }

        // Priority 2: Admin config
        adminConfig.getAdminDaylioFileName()?.let {
            Timber.d("Using admin-configured Daylio file name")
            return it
        }

        // Priority 3: Default from resources
        val context = getContext()
        return context.resources.getString(R.string.daylio_file_name)
    }

    /**
     * Checks if daily file checking is enabled.
     *
     * @return true if enabled, false otherwise
     */
    fun isDailyFileCheckEnabled(): Boolean {
        val context = getContext()
        return context.resources.getBoolean(R.bool.enable_daily_file_check)
    }

    /**
     * Checks if birthday notification is enabled.
     *
     * @return true if enabled, false otherwise
     */
    fun isBirthdayNotificationEnabled(): Boolean {
        val context = getContext()
        return context.resources.getBoolean(R.bool.enable_birthday_notification)
    }

    /**
     * Gets file check interval in hours.
     *
     * @return Interval in hours
     */
    fun getFileCheckIntervalHours(): Int {
        val context = getContext()
        return context.resources.getInteger(R.integer.file_check_interval_hours)
    }

    /**
     * Checks if verbose logging is enabled. Uses BuildConfig (set in
     * build.gradle.kts).
     *
     * @return true if enabled, false otherwise
     */
    fun isVerboseLoggingEnabled(): Boolean {
        return BuildConfig.DEBUG_LOGGING
    }

    /**
     * Gets identifier for daily file check task.
     *
     * @return WorkManager task identifier
     */
    fun getDailyFileCheckWorkName(): String {
        val context = getContext()
        return context.resources.getString(R.string.work_daily_file_check)
    }

    /**
     * Checks if test mode is enabled. In test mode, app uses test file and
     * ignores time checking. Uses BuildConfig (set in build.gradle.kts).
     *
     * @return true if test mode is enabled, false otherwise
     */
    fun isTestMode(): Boolean {
        return BuildConfig.TEST_MODE
    }

    /**
     * Checks if app is in debug build.
     *
     * @return true if debug build, false otherwise
     */
    fun isDebugBuild(): Boolean = BuildConfig.DEBUG

    /**
     * Gets app version.
     *
     * @return App version as string
     */
    fun getAppVersion(): String = BuildConfig.VERSION_NAME

    /**
     * Gets app version code.
     *
     * @return Version code as integer
     */
    fun getAppVersionCode(): Int = BuildConfig.VERSION_CODE

    /**
     * Validates app configuration and returns list of errors. Useful for
     * debugging configuration issues.
     *
     * @return List of error messages (empty if all OK)
     */
    fun validateConfiguration(): List<String> {
        val errors = mutableListOf<String>()

        try {
            // Check birthday date
            val birthday = getBirthdayDate()
            if (birthday.timeInMillis <= System.currentTimeMillis() && !isTestMode()) {
                errors.add("Birthday date is in the past and test mode is disabled")
            }
        } catch (e: Exception) {
            errors.add("Error in birthday date configuration: ${e.message}")
        }

        try {
            // Check Google Drive folder ID
            getDriveFolderId()
        } catch (e: Exception) {
            errors.add("Error in Google Drive configuration: ${e.message}")
        }

        try {
            // Check service account file name
            val serviceAccountFile = getServiceAccountFileName()
            if (serviceAccountFile.isBlank()) {
                errors.add("Service account file name is not configured")
            }
        } catch (e: Exception) {
            errors.add("Error in service account configuration: ${e.message}")
        }

        return errors
    }

    /** Gets summary of current configuration including admin overrides. */
    fun getConfigSummary(): String {
        val isAdminActive = adminConfig.isAdminConfigEnabled()

        return buildString {
            appendLine("Configuration Status:")
            if (isAdminActive) {
                appendLine("⚠️ Admin overrides ACTIVE")
                appendLine()
                appendLine(adminConfig.getConfigSummary())
            } else {
                appendLine("✓ Using default config.xml values")
                appendLine()
                appendLine("Birthday: ${TimeUtils.formatDate(getBirthdayDate().time)}")
                appendLine("Drive Folder: ${getDriveFolderId().take(20)}...")
                appendLine("File Name: ${getDaylioFileName()}")
            }
        }
    }

    companion object {
        // Use volatile for thread-safe access
        @Volatile
        private var INSTANCE: WeakReference<AppConfig>? = null

        /**
         * Gets app configuration instance (singleton with weak reference).
         * Thread-safe implementation with double-checked locking.
         *
         * @param context Application context
         * @return AppConfig instance
         */
        fun getInstance(context: Context): AppConfig {
            // Fast check with no locking
            val instanceRef = INSTANCE
            if (instanceRef != null) {
                val instance = instanceRef.get()
                if (instance != null) {
                    return instance
                }
            }

            // Slow path with locking
            return synchronized(this) {
                // Recheck after lock acquisition
                val currentInstanceRef = INSTANCE
                val currentInstance = currentInstanceRef?.get()

                if (currentInstance != null) {
                    // Instance still exists, return it
                    currentInstance
                } else {
                    // Create new instance and store weak reference to it
                    val newInstance = AppConfig(context.applicationContext)
                    INSTANCE = WeakReference(newInstance)

                    // Validate configuration in debug builds
                    if (newInstance.isDebugBuild()) {
                        val errors = newInstance.validateConfiguration()
                        if (errors.isNotEmpty()) {
                            Timber.w("Configuration issues: ${errors.joinToString(", ")}")
                        }
                    }

                    newInstance
                }
            }
        }

        /**
         * Clears singleton instance. Used mainly in tests or when app is closing
         * to help garbage collector.
         */
        fun clearInstance() {
            synchronized(this) {
                INSTANCE?.get()?.let { instance ->
                    // Clear cache
                    instance._birthdayDateCache = null
                    instance._driveFolderIdCache = null
                }
                INSTANCE = null
            }
        }

        /**
         * Checks if instance is initialized. Useful in tests.
         *
         * @return true if instance exists, false otherwise
         */
        fun isInitialized(): Boolean {
            return INSTANCE?.get() != null
        }
    }
}