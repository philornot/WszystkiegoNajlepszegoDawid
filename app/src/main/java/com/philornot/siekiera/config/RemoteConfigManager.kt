package com.philornot.siekiera.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.philornot.siekiera.network.DriveApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import java.util.TimeZone

/**
 * Manager for remote configuration stored on Google Drive.
 * Allows one admin to change configuration for all app instances.
 *
 * Configuration file format (app_config.json):
 * {
 *   "version": 1,
 *   "birthday_year": 2025,
 *   "birthday_month": 5,
 *   "birthday_day": 15,
 *   "birthday_hour": 12,
 *   "birthday_minute": 0,
 *   "daylio_file_name": "backup.daylio",
 *   "last_updated": 1234567890
 * }
 */
class RemoteConfigManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val warsawTimezone = TimeZone.getTimeZone("Europe/Warsaw")
    private val appContext = context.applicationContext

    /**
     * Data class representing remote configuration.
     */
    data class RemoteConfig(
        val birthdayYear: Int,
        val birthdayMonth: Int, // 0-11 (Calendar format)
        val birthdayDay: Int,
        val birthdayHour: Int,
        val birthdayMinute: Int,
        val daylioFileName: String,
        val lastUpdated: Long
    )

    /**
     * Check if remote config is enabled and downloaded.
     */
    fun hasRemoteConfig(): Boolean {
        return prefs.contains(KEY_BIRTHDAY_YEAR)
    }

    /**
     * Get remote configuration from local cache.
     */
    fun getRemoteConfig(): RemoteConfig? {
        if (!hasRemoteConfig()) return null

        return try {
            RemoteConfig(
                birthdayYear = prefs.getInt(KEY_BIRTHDAY_YEAR, -1),
                birthdayMonth = prefs.getInt(KEY_BIRTHDAY_MONTH, -1),
                birthdayDay = prefs.getInt(KEY_BIRTHDAY_DAY, -1),
                birthdayHour = prefs.getInt(KEY_BIRTHDAY_HOUR, -1),
                birthdayMinute = prefs.getInt(KEY_BIRTHDAY_MINUTE, -1),
                daylioFileName = prefs.getString(KEY_DAYLIO_FILE_NAME, "") ?: "",
                lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0L)
            )
        } catch (e: Exception) {
            Timber.e(e, "Error reading remote config")
            null
        }
    }

    /**
     * Get birthday date from remote config as Calendar.
     */
    fun getRemoteBirthdayDate(): Calendar? {
        val config = getRemoteConfig() ?: return null

        if (config.birthdayYear == -1) return null

        return Calendar.getInstance(warsawTimezone).apply {
            set(Calendar.YEAR, config.birthdayYear)
            set(Calendar.MONTH, config.birthdayMonth)
            set(Calendar.DAY_OF_MONTH, config.birthdayDay)
            set(Calendar.HOUR_OF_DAY, config.birthdayHour)
            set(Calendar.MINUTE, config.birthdayMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /**
     * Get Daylio file name from remote config.
     */
    fun getRemoteDaylioFileName(): String? {
        return getRemoteConfig()?.daylioFileName?.takeIf { it.isNotEmpty() }
    }

    /**
     * Fetch remote configuration from Google Drive.
     *
     * @param folderId Google Drive folder ID containing app_config.json
     * @return true if config was successfully downloaded, false otherwise
     */
    suspend fun fetchRemoteConfig(folderId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("Fetching remote config from Drive folder: $folderId")

            val driveClient = DriveApiClient.getInstance(appContext)

            // Initialize client
            if (!driveClient.initialize()) {
                Timber.e("Failed to initialize Drive client for remote config")
                return@withContext false
            }

            // List files in folder and find app_config.json
            val files = driveClient.listFilesInFolder(folderId)
            val configFile = files.find { it.name == CONFIG_FILE_NAME }

            if (configFile == null) {
                Timber.d("Remote config file not found in Drive folder")
                return@withContext false
            }

            Timber.d("Found remote config file: ${configFile.name}, modified: ${configFile.modifiedTime}")

            // Check if we already have this version
            val lastKnownUpdate = prefs.getLong(KEY_LAST_UPDATED, 0L)
            if (configFile.modifiedTime.time <= lastKnownUpdate) {
                Timber.d("Remote config is not newer than cached version, skipping download")
                return@withContext true
            }

            // Download and parse config file
            val inputStream = driveClient.downloadFile(configFile.id)
            val content = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

            Timber.d("Downloaded remote config content: $content")

            // Parse JSON
            val json = JSONObject(content)

            // Validate version
            val version = json.optInt("version", 0)
            if (version < 1) {
                Timber.e("Invalid config version: $version")
                return@withContext false
            }

            // Extract configuration
            val birthdayYear = json.getInt("birthday_year")
            val birthdayMonth = json.getInt("birthday_month") - 1 // Convert to Calendar format (0-11)
            val birthdayDay = json.getInt("birthday_day")
            val birthdayHour = json.getInt("birthday_hour")
            val birthdayMinute = json.getInt("birthday_minute")
            val daylioFileName = json.getString("daylio_file_name")
            val lastUpdated = json.optLong("last_updated", configFile.modifiedTime.time)

            // Save to local cache
            prefs.edit {
                putInt(KEY_BIRTHDAY_YEAR, birthdayYear)
                putInt(KEY_BIRTHDAY_MONTH, birthdayMonth)
                putInt(KEY_BIRTHDAY_DAY, birthdayDay)
                putInt(KEY_BIRTHDAY_HOUR, birthdayHour)
                putInt(KEY_BIRTHDAY_MINUTE, birthdayMinute)
                putString(KEY_DAYLIO_FILE_NAME, daylioFileName)
                putLong(KEY_LAST_UPDATED, lastUpdated)
            }

            Timber.d("Successfully cached remote config: birthday=${birthdayYear}-${birthdayMonth+1}-${birthdayDay} ${birthdayHour}:${birthdayMinute}, file=$daylioFileName")

            true

        } catch (e: Exception) {
            Timber.e(e, "Error fetching remote config")
            false
        }
    }

    /**
     * Upload new configuration to Google Drive (admin only).
     *
     * @param folderId Google Drive folder ID
     * @param config Configuration to upload
     * @return true if upload was successful, false otherwise
     */
    suspend fun uploadRemoteConfig(folderId: String, config: RemoteConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("Uploading remote config to Drive folder: $folderId")

            // Create JSON content
            val json = JSONObject().apply {
                put("version", 1)
                put("birthday_year", config.birthdayYear)
                put("birthday_month", config.birthdayMonth + 1) // Convert from Calendar format
                put("birthday_day", config.birthdayDay)
                put("birthday_hour", config.birthdayHour)
                put("birthday_minute", config.birthdayMinute)
                put("daylio_file_name", config.daylioFileName)
                put("last_updated", System.currentTimeMillis())
            }

            val jsonString = json.toString(2) // Pretty print with indent

            Timber.d("Generated config JSON: $jsonString")

            // TODO: Implement upload to Google Drive
            // This requires Drive API write permissions
            // For now, admin needs to manually create/update the file

            Timber.w("Remote config upload not implemented - admin must manually create app_config.json in Drive folder")
            Timber.d("JSON to upload:\n$jsonString")

            false

        } catch (e: Exception) {
            Timber.e(e, "Error uploading remote config")
            false
        }
    }

    /**
     * Clear cached remote configuration.
     */
    fun clearRemoteConfig() {
        prefs.edit { clear() }
        Timber.d("Cleared remote config cache")
    }

    /**
     * Get time of last config update.
     */
    fun getLastUpdateTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATED, 0L)
    }

    companion object {
        private const val PREFS_NAME = "remote_config_prefs"
        private const val CONFIG_FILE_NAME = "app_config.json"

        private const val KEY_BIRTHDAY_YEAR = "remote_birthday_year"
        private const val KEY_BIRTHDAY_MONTH = "remote_birthday_month"
        private const val KEY_BIRTHDAY_DAY = "remote_birthday_day"
        private const val KEY_BIRTHDAY_HOUR = "remote_birthday_hour"
        private const val KEY_BIRTHDAY_MINUTE = "remote_birthday_minute"
        private const val KEY_DAYLIO_FILE_NAME = "remote_daylio_file_name"
        private const val KEY_LAST_UPDATED = "remote_last_updated"

        @Volatile
        private var instance: RemoteConfigManager? = null

        fun getInstance(context: Context): RemoteConfigManager {
            return instance ?: synchronized(this) {
                instance ?: RemoteConfigManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}