package com.philornot.siekiera.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone

/**
 * Manager for admin-level configuration that can override default values
 * from config.xml. Allows changing birthday date and Drive folder without
 * app reinstall.
 */
class AdminConfigManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val warsawTimezone = TimeZone.getTimeZone("Europe/Warsaw")

    /**
     * Check if admin config is enabled (overriding default config.xml values).
     */
    fun isAdminConfigEnabled(): Boolean {
        return prefs.getBoolean(KEY_ADMIN_ENABLED, false)
    }

    /**
     * Enable or disable admin configuration override.
     */
    fun setAdminConfigEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_ADMIN_ENABLED, enabled)
        }
        Timber.d("Admin config ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Get birthday date from admin config, or null if not set.
     * Returns Calendar in Warsaw timezone.
     */
    fun getAdminBirthdayDate(): Calendar? {
        if (!isAdminConfigEnabled()) return null

        val year = prefs.getInt(KEY_BIRTHDAY_YEAR, -1)
        if (year == -1) return null

        val month = prefs.getInt(KEY_BIRTHDAY_MONTH, -1)
        val day = prefs.getInt(KEY_BIRTHDAY_DAY, -1)
        val hour = prefs.getInt(KEY_BIRTHDAY_HOUR, -1)
        val minute = prefs.getInt(KEY_BIRTHDAY_MINUTE, -1)

        if (month == -1 || day == -1 || hour == -1 || minute == -1) return null

        return Calendar.getInstance(warsawTimezone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /**
     * Get birthday date in millis from admin config, or null if not set.
     */
    fun getAdminBirthdayTimeMillis(): Long? {
        return getAdminBirthdayDate()?.timeInMillis
    }

    /**
     * Set birthday date in admin config.
     */
    fun setAdminBirthdayDate(
        year: Int,
        month: Int, // 0-11 (Calendar.MONTH format)
        day: Int,
        hour: Int,
        minute: Int
    ) {
        prefs.edit {
            putBoolean(KEY_ADMIN_ENABLED, true)
            putInt(KEY_BIRTHDAY_YEAR, year)
            putInt(KEY_BIRTHDAY_MONTH, month)
            putInt(KEY_BIRTHDAY_DAY, day)
            putInt(KEY_BIRTHDAY_HOUR, hour)
            putInt(KEY_BIRTHDAY_MINUTE, minute)
        }

        Timber.d("Admin birthday set to: $year-${month+1}-$day $hour:$minute")
    }

    /**
     * Get Google Drive folder ID from admin config, or null if not set.
     */
    fun getAdminDriveFolderId(): String? {
        if (!isAdminConfigEnabled()) return null
        return prefs.getString(KEY_DRIVE_FOLDER_ID, null)
    }

    /**
     * Set Google Drive folder ID in admin config.
     */
    fun setAdminDriveFolderId(folderId: String) {
        prefs.edit {
            putBoolean(KEY_ADMIN_ENABLED, true)
            putString(KEY_DRIVE_FOLDER_ID, folderId)
        }
        Timber.d("Admin Drive folder ID set to: $folderId")
    }

    /**
     * Get Daylio file name from admin config, or null if not set.
     */
    fun getAdminDaylioFileName(): String? {
        if (!isAdminConfigEnabled()) return null
        return prefs.getString(KEY_DAYLIO_FILE_NAME, null)
    }

    /**
     * Set Daylio file name in admin config.
     */
    fun setAdminDaylioFileName(fileName: String) {
        prefs.edit {
            putBoolean(KEY_ADMIN_ENABLED, true)
            putString(KEY_DAYLIO_FILE_NAME, fileName)
        }
        Timber.d("Admin Daylio file name set to: $fileName")
    }

    /**
     * Clear all admin configuration and return to default config.xml values.
     */
    fun clearAdminConfig() {
        prefs.edit {
            clear()
        }
        Timber.d("Admin config cleared - returning to default config.xml values")
    }

    /**
     * Get summary of current admin configuration for display.
     */
    fun getConfigSummary(): String {
        if (!isAdminConfigEnabled()) {
            return "Using default configuration from config.xml"
        }

        val birthday = getAdminBirthdayDate()
        val folderId = getAdminDriveFolderId()
        val fileName = getAdminDaylioFileName()

        return buildString {
            appendLine("Admin Configuration Active:")
            if (birthday != null) {
                appendLine("Birthday: ${birthday.get(Calendar.YEAR)}-${birthday.get(Calendar.MONTH)+1}-${birthday.get(Calendar.DAY_OF_MONTH)} ${birthday.get(Calendar.HOUR_OF_DAY)}:${birthday.get(Calendar.MINUTE)}")
            } else {
                appendLine("Birthday: Using default")
            }
            if (folderId != null) {
                appendLine("Drive Folder: ${folderId.take(20)}...")
            } else {
                appendLine("Drive Folder: Using default")
            }
            if (fileName != null) {
                appendLine("File Name: $fileName")
            } else {
                appendLine("File Name: Using default")
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "admin_config_prefs"

        private const val KEY_ADMIN_ENABLED = "admin_enabled"
        private const val KEY_BIRTHDAY_YEAR = "admin_birthday_year"
        private const val KEY_BIRTHDAY_MONTH = "admin_birthday_month"
        private const val KEY_BIRTHDAY_DAY = "admin_birthday_day"
        private const val KEY_BIRTHDAY_HOUR = "admin_birthday_hour"
        private const val KEY_BIRTHDAY_MINUTE = "admin_birthday_minute"
        private const val KEY_DRIVE_FOLDER_ID = "admin_drive_folder_id"
        private const val KEY_DAYLIO_FILE_NAME = "admin_daylio_file_name"

        @Volatile
        private var instance: AdminConfigManager? = null

        fun getInstance(context: Context): AdminConfigManager {
            return instance ?: synchronized(this) {
                instance ?: AdminConfigManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}