package com.philornot.siekiera.config

import android.content.Context
import com.philornot.siekiera.R
import com.philornot.siekiera.utils.TimeUtils
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone

/**
 * Klasa konfiguracyjna aplikacji, która zapewnia scentralizowany dostęp do
 * wszystkich parametrów konfiguracyjnych.
 *
 * Wszystkie wartości są odczytywane z pliku res/values/config.xml.
 */
class AppConfig(private val context: Context) {

    // Strefa czasowa Warszawy
    private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

    /**
     * Pobiera datę urodzin jako obiekt Calendar w strefie czasowej Warszawy.
     * Wartości konfiguracyjne są pobierane z pliku config.xml.
     *
     * @return Obiekt Calendar z ustawioną datą i czasem urodzin
     */
    fun getBirthdayDate(): Calendar {
        val year = context.resources.getInteger(R.integer.birthday_year)
        // Miesiące w Calendar są indeksowane od 0, ale w config.xml używamy 1-12 dla czytelności
        val month = context.resources.getInteger(R.integer.birthday_month) - 1
        val day = context.resources.getInteger(R.integer.birthday_day)
        val hour = context.resources.getInteger(R.integer.birthday_hour)
        val minute = context.resources.getInteger(R.integer.birthday_minute)

        return Calendar.getInstance(WARSAW_TIMEZONE).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.also {
            if (isVerboseLoggingEnabled()) {
                Timber.d("Data urodzin z konfiguracji: ${TimeUtils.formatDate(it.time)}")
            }
        }
    }

    /**
     * Pobiera datę urodzin w milisekundach.
     *
     * @return Czas w milisekundach
     */
    fun getBirthdayTimeMillis(): Long {
        return getBirthdayDate().timeInMillis
    }

    /**
     * Pobiera ID folderu Google Drive.
     *
     * @return ID folderu Google Drive
     */
    fun getDriveFolderId(): String {
        return context.resources.getString(R.string.drive_folder_id)
    }

    /**
     * Pobiera nazwę pliku z zasobu service_account.
     *
     * @return Nazwa pliku (bez rozszerzenia .json)
     */
    fun getServiceAccountFileName(): String {
        return context.resources.getString(R.string.service_account_file)
    }

    /**
     * Pobiera nazwę pliku Daylio.
     *
     * @return Nazwa pliku Daylio
     */
    fun getDaylioFileName(): String {
        return context.resources.getString(R.string.daylio_file_name)
    }

    /**
     * Sprawdza, czy codzienne sprawdzanie pliku jest włączone.
     *
     * @return true jeśli włączone, false w przeciwnym wypadku
     */
    fun isDailyFileCheckEnabled(): Boolean {
        return context.resources.getBoolean(R.bool.enable_daily_file_check)
    }

    /**
     * Sprawdza, czy powiadomienie urodzinowe jest włączone.
     *
     * @return true jeśli włączone, false w przeciwnym wypadku
     */
    fun isBirthdayNotificationEnabled(): Boolean {
        return context.resources.getBoolean(R.bool.enable_birthday_notification)
    }

    /**
     * Pobiera interwał sprawdzania pliku w godzinach.
     *
     * @return Interwał w godzinach
     */
    fun getFileCheckIntervalHours(): Int {
        return context.resources.getInteger(R.integer.file_check_interval_hours)
    }

    /**
     * Sprawdza, czy szczegółowe logowanie jest włączone.
     *
     * @return true jeśli włączone, false w przeciwnym wypadku
     */
    fun isVerboseLoggingEnabled(): Boolean {
        return context.resources.getBoolean(R.bool.verbose_logging)
    }

    /**
     * Pobiera identyfikator dla codziennego zadania sprawdzania pliku.
     *
     * @return Identyfikator zadania WorkManager
     */
    fun getDailyFileCheckWorkName(): String {
        return context.resources.getString(R.string.work_daily_file_check)
    }

    companion object {
        @Volatile
        internal var INSTANCE: AppConfig? = null

        /**
         * Pobiera instancję konfiguracji aplikacji (singleton).
         *
         * @param context Kontekst aplikacji
         * @return Instancja AppConfig
         */
        fun getInstance(context: Context): AppConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppConfig(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}