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
 * Klasa konfiguracyjna aplikacji, która zapewnia scentralizowany dostęp do
 * wszystkich parametrów konfiguracyjnych.
 *
 * Wszystkie wartości są odczytywane z pliku res/values/config.xml oraz
 * BuildConfig. Używa bezpiecznego podejścia z WeakReference i lazy
 * initialization.
 */
class AppConfig private constructor(context: Context) {

    // Używamy WeakReference do kontekstu, aby uniknąć wycieków pamięci
    private val contextRef: WeakReference<Context> = WeakReference(context.applicationContext)

    // Strefa czasowa Warszawy
    private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

    // Cache dla często używanych wartości
    private var _birthdayDateCache: Calendar? = null
    private var _driveFolderIdCache: String? = null

    /**
     * Getter dla kontekstu, który sprawdza czy referencja jest wciąż ważna
     *
     * @throws IllegalStateException jeśli kontekst nie jest dostępny
     */
    private fun getContext(): Context {
        return contextRef.get()
            ?: throw IllegalStateException("Kontekst aplikacji nie jest dostępny - może wystąpić memory leak")
    }

    /**
     * Pobiera datę urodzin jako obiekt Calendar w strefie czasowej Warszawy.
     * Wartości konfiguracyjne są pobierane z pliku config.xml. Wynik jest
     * cache'owany dla lepszej wydajności.
     *
     * @return Obiekt Calendar z ustawioną datą i czasem urodzin
     */
    fun getBirthdayDate(): Calendar {
        // Sprawdź cache
        _birthdayDateCache?.let { return it }

        val context = getContext()

        val year = context.resources.getInteger(R.integer.birthday_year)
        // Miesiące w Calendar są indeksowane od 0, ale w config.xml używamy 1-12 dla czytelności
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

        // Cache wynik
        _birthdayDateCache = calendar

        if (isVerboseLoggingEnabled()) {
            Timber.d("Data urodzin z konfiguracji: ${TimeUtils.formatDate(calendar.time)}")
        }

        return calendar
    }

    /**
     * Pobiera datę urodzin w milisekundach.
     *
     * @return Czas w milisekundach
     */
    fun getBirthdayTimeMillis(): Long = getBirthdayDate().timeInMillis

    /**
     * Pobiera ID folderu Google Drive. Używa wyłącznie BuildConfig (z
     * local.properties lub CI/CD)
     *
     * @return ID folderu Google Drive
     * @throws IllegalStateException jeśli ID nie jest skonfigurowane
     */
    fun getDriveFolderId(): String {
        // Sprawdź cache
        _driveFolderIdCache?.let { return it }

        val folderId = BuildConfig.GDRIVE_FOLDER_ID

        if (folderId.isBlank()) {
            throw IllegalStateException(
                "ID folderu Google Drive nie jest skonfigurowane. " + "Ustaw gdrive.folder.id w local.properties lub zdefiniuj w BuildConfig."
            )
        }

        // Cache wynik
        _driveFolderIdCache = folderId
        return folderId
    }

    /**
     * Pobiera nazwę pliku z zasobu service_account.
     *
     * @return Nazwa pliku (bez rozszerzenia .json)
     */
    fun getServiceAccountFileName(): String {
        val context = getContext()
        return context.resources.getString(R.string.service_account_file)
    }

    /**
     * Pobiera nazwę pliku Daylio.
     *
     * @return Nazwa pliku Daylio
     */
    fun getDaylioFileName(): String {
        val context = getContext()
        return context.resources.getString(R.string.daylio_file_name)
    }

    /**
     * Sprawdza, czy codzienne sprawdzanie pliku jest włączone.
     *
     * @return true jeśli włączone, false w przeciwnym wypadku
     */
    fun isDailyFileCheckEnabled(): Boolean {
        val context = getContext()
        return context.resources.getBoolean(R.bool.enable_daily_file_check)
    }

    /**
     * Sprawdza, czy powiadomienie urodzinowe jest włączone.
     *
     * @return true jeśli włączone, false w przeciwnym wypadku
     */
    fun isBirthdayNotificationEnabled(): Boolean {
        val context = getContext()
        return context.resources.getBoolean(R.bool.enable_birthday_notification)
    }

    /**
     * Pobiera interwał sprawdzania pliku w godzinach.
     *
     * @return Interwał w godzinach
     */
    fun getFileCheckIntervalHours(): Int {
        val context = getContext()
        return context.resources.getInteger(R.integer.file_check_interval_hours)
    }

    /**
     * Sprawdza, czy szczegółowe logowanie jest włączone. Używa BuildConfig
     * (ustawianego w build.gradle.kts).
     *
     * @return true jeśli włączone, false w przeciwnym wypadku
     */
    fun isVerboseLoggingEnabled(): Boolean {
        return BuildConfig.DEBUG_LOGGING
    }

    /**
     * Pobiera identyfikator dla codziennego zadania sprawdzania pliku.
     *
     * @return Identyfikator zadania WorkManager
     */
    fun getDailyFileCheckWorkName(): String {
        val context = getContext()
        return context.resources.getString(R.string.work_daily_file_check)
    }

    /**
     * Sprawdza, czy jest włączony tryb testowy. W trybie testowym aplikacja
     * używa pliku testowego i ignoruje sprawdzanie czasu. Używa BuildConfig
     * (ustawianego w build.gradle.kts).
     *
     * @return true jeśli tryb testowy jest włączony, false w przeciwnym
     *    wypadku
     */
    fun isTestMode(): Boolean {
        return BuildConfig.TEST_MODE
    }

    /**
     * Sprawdza, czy aplikacja jest w trybie debug.
     *
     * @return true jeśli debug build, false w przeciwnym wypadku
     */
    fun isDebugBuild(): Boolean = BuildConfig.DEBUG

    /**
     * Pobiera wersję aplikacji.
     *
     * @return Wersja aplikacji jako string
     */
    fun getAppVersion(): String = BuildConfig.VERSION_NAME

    /**
     * Pobiera kod wersji aplikacji.
     *
     * @return Kod wersji jako integer
     */
    fun getAppVersionCode(): Int = BuildConfig.VERSION_CODE

    /**
     * Waliduje konfigurację aplikacji i zwraca listę błędów. Przydatne do
     * debugowania problemów z konfiguracją.
     *
     * @return Lista komunikatów o błędach (pusta jeśli wszystko OK)
     */
    fun validateConfiguration(): List<String> {
        val errors = mutableListOf<String>()

        try {
            // Sprawdź datę urodzin
            val birthday = getBirthdayDate()
            if (birthday.timeInMillis <= System.currentTimeMillis() && !isTestMode()) {
                errors.add("Data urodzin jest w przeszłości i tryb testowy jest wyłączony")
            }
        } catch (e: Exception) {
            errors.add("Błąd konfiguracji daty urodzin: ${e.message}")
        }

        try {
            // Sprawdź ID folderu Google Drive
            getDriveFolderId()
        } catch (e: Exception) {
            errors.add("Błąd konfiguracji Google Drive: ${e.message}")
        }

        try {
            // Sprawdź nazwę pliku service account
            val serviceAccountFile = getServiceAccountFileName()
            if (serviceAccountFile.isBlank()) {
                errors.add("Nazwa pliku service account nie jest skonfigurowana")
            }
        } catch (e: Exception) {
            errors.add("Błąd konfiguracji service account: ${e.message}")
        }

        return errors
    }

    companion object {
        // Używamy volatile dla bezpieczeństwa wielowątkowego
        @Volatile
        private var INSTANCE: WeakReference<AppConfig>? = null

        /**
         * Pobiera instancję konfiguracji aplikacji (singleton z weak reference).
         * Thread-safe implementation z double-checked locking.
         *
         * @param context Kontekst aplikacji
         * @return Instancja AppConfig
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

                    // Waliduj konfigurację w debug builds
                    if (newInstance.isDebugBuild()) {
                        val errors = newInstance.validateConfiguration()
                        if (errors.isNotEmpty()) {
                            Timber.w("Problemy z konfiguracją: ${errors.joinToString(", ")}")
                        }
                    }

                    newInstance
                }
            }
        }

        /**
         * Czyści instancję singleton. Używane głównie w testach lub gdy aplikacja
         * jest zamykana, aby pomóc garbage collectorowi.
         */
        fun clearInstance() {
            synchronized(this) {
                INSTANCE?.get()?.let { instance ->
                    // Wyczyść cache
                    instance._birthdayDateCache = null
                    instance._driveFolderIdCache = null
                }
                INSTANCE = null
            }
        }

        /**
         * Sprawdza czy instancja jest zainicjalizowana. Przydatne w testach.
         *
         * @return true jeśli instancja istnieje, false w przeciwnym wypadku
         */
        fun isInitialized(): Boolean {
            return INSTANCE?.get() != null
        }
    }
}