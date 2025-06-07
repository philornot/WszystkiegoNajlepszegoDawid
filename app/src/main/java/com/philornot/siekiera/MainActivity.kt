package com.philornot.siekiera

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.managers.ManagerContainer
import com.philornot.siekiera.managers.ManagerFactory
import com.philornot.siekiera.notification.NotificationHelper
import com.philornot.siekiera.notification.TimerNotificationHelper
import com.philornot.siekiera.ui.screens.main.navigation.MainScreen
import com.philornot.siekiera.ui.screens.main.navigation.NavigationSection
import com.philornot.siekiera.ui.theme.AppTheme
import com.philornot.siekiera.utils.RealTimeProvider
import com.philornot.siekiera.utils.TimeProvider
import com.philornot.siekiera.utils.TimeUtils
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone

/**
 * Główna aktywność aplikacji - znacznie zmniejszona dzięki użyciu
 * managerów. Odpowiada tylko za podstawowy lifecycle, UI i koordynację
 * między managerami.
 *
 * AKTUALIZACJA: Dodana obsługa intencji z powiadomień - aplikacja
 * poprawnie otwiera się po kliknięciu w powiadomienie i reaguje na różne
 * typy powiadomień.
 *
 * NAPRAWKA: Dodano obsługę onRequestPermissionsResult dla
 * PermissionManager.
 */
class MainActivity : ComponentActivity() {

    // Strefa czasowa Warszawy
    @Suppress("PrivatePropertyName")
    private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

    // Domyślnie używaj prawdziwego czasu, ale umożliw wstrzyknięcie testowego w testach
    var timeProvider: TimeProvider = RealTimeProvider()

    // Podstawowe komponenty
    private lateinit var appConfig: AppConfig
    private lateinit var prefs: SharedPreferences

    // Kontener wszystkich managerów
    private lateinit var managers: ManagerContainer

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        // Inicjalizacja podstawowych komponentów
        initializeComponents()

        // Inicjalizacja managerów
        initializeManagers()

        // Sprawdź uprawnienia
        managers.permissionsManager.requestAllPermissions()

        // Zaplanuj powiadomienia i zadania
        setupNotificationsAndTasks()

        // Przywróć stan aplikacji
        restoreApplicationState()

        // Obsłuż intent z powiadomienia jeśli aplikacja została otwarta przez powiadomienie
        handleNotificationIntent(intent)

        // Inicjalizacja UI
        setupUI()
    }

    /**
     * Obsługuje wynik żądania uprawnień.
     *
     * Ta metoda jest wywoływana przez system Android, gdy użytkownik odpowie
     * na prośbę o uprawnienia (np. zezwoli lub odmówi). Wynik jest następnie
     * przekazywany do PermissionManager w celu dalszego przetworzenia.
     *
     * @param requestCode Kod żądania przekazany do `requestPermissions()`.
     * @param permissions Tablica żądanych uprawnień.
     * @param grantResults Wyniki dla odpowiednich uprawnień:
     *    `PERMISSION_GRANTED` lub `PERMISSION_DENIED`.
     */
    @Deprecated(
        "This method is deprecated. Use the Activity Result API for better type safety and testability. " + "Specifically, use registerForActivityResult with ActivityResultContracts.RequestMultiplePermissions " + "and handle the result in the ActivityResultCallback.",
        ReplaceWith(
            "registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* handle result */ }",
            "androidx.activity.result.contract.ActivityResultContracts"
        )
    )
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Przekaż wynik do PermissionManager
        if (::managers.isInitialized) {
            managers.permissionsManager.handlePermissionResult(
                requestCode, permissions, grantResults
            )
        }
    }

    /**
     * Wywoływane gdy nowy intent dociera do już działającej aktywności.
     * Obsługuje przypadki gdy aplikacja jest już uruchomiona i użytkownik
     * kliknie w powiadomienie.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("MainActivity onNewIntent: ${intent.action}")

        // Ustaw nowy intent jako obecny
        setIntent(intent)

        // Obsłuż intent z powiadomienia
        handleNotificationIntent(intent)
    }

    /**
     * Obsługuje intencje pochodzące z powiadomień i wykonuje odpowiednie
     * akcje.
     *
     * @param intent Intent do przetworzenia
     */
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return

        Timber.d("Sprawdzanie intencji z powiadomienia: action=${intent.action}")

        // Loguj wszystkie extras dla debugowania
        intent.extras?.let { extras ->
            for (key in extras.keySet()) {
                val value = when {
                    extras.getString(key) != null -> extras.getString(key)
                    extras.getInt(key, Int.MIN_VALUE) != Int.MIN_VALUE -> extras.getInt(key)
                    extras.getBoolean(key, false) != extras.getBoolean(key, true) -> extras.getBoolean(key)
                    extras.getLong(key, Long.MIN_VALUE) != Long.MIN_VALUE -> extras.getLong(key)
                    else -> "unknown type"
                }
                Timber.d("Intent extra: $key = $value")
            }
        }

        when {
            // Powiadomienie o pobraniu pliku
            intent.hasExtra("SHOW_DOWNLOADED_FILE") -> {
                val fileName = intent.getStringExtra("FILE_NAME") ?: "nieznany plik"
                Timber.d("Otwarto aplikację z powiadomienia o pobraniu pliku: $fileName")

                // Przełącz na sekcję prezentu jeśli plik został pobrany
                if (::managers.isInitialized) {
                    managers.appStateManager.setCurrentSection(NavigationSection.GIFT)
                }

                Toast.makeText(
                    this, "Plik $fileName został pobrany", Toast.LENGTH_LONG
                ).show()
            }

            // Powiadomienie o zakończeniu timera
            intent.hasExtra("SHOW_TIMER_FINISHED") -> {
                val minutes = intent.getIntExtra("TIMER_MINUTES", 0)
                Timber.d("Otwarto aplikację z powiadomienia o zakończeniu timera: $minutes minut")

                // Przełącz na sekcję timera
                if (::managers.isInitialized) {
                    managers.appStateManager.setCurrentSection(NavigationSection.TIMER)
                }

                Toast.makeText(
                    this, "Timer na $minutes minut został zakończony", Toast.LENGTH_SHORT
                ).show()
            }

            // Powiadomienie o postępie timera
            intent.hasExtra("SHOW_TIMER_PROGRESS") -> {
                Timber.d("Otwarto aplikację z powiadomienia o postępie timera")

                // Przełącz na sekcję timera
                if (::managers.isInitialized) {
                    managers.appStateManager.setCurrentSection(NavigationSection.TIMER)
                }
            }

            // Powiadomienie o spauzowanym timerze
            intent.hasExtra("SHOW_TIMER_PAUSED") -> {
                Timber.d("Otwarto aplikację z powiadomienia o spauzowanym timerze")

                // Przełącz na sekcję timera
                if (::managers.isInitialized) {
                    managers.appStateManager.setCurrentSection(NavigationSection.TIMER)
                }

                Toast.makeText(
                    this, "Timer jest spauzowany", Toast.LENGTH_SHORT
                ).show()
            }

            // Powiadomienie urodzinowe (gift reveal)
            intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER) -> {
                // To standardowe uruchomienie aplikacji, nie z powiadomienia
                Timber.d("Standardowe uruchomienie aplikacji")
            }

            else -> {
                // Sprawdź czy to może być powiadomienie urodzinowe (nie ma specjalnych extras)
                Timber.d("Potencjalne powiadomienie urodzinowe lub standardowe uruchomienie")

                // Jeśli aplikacja została uruchomiona i dzisiaj są urodziny, pokaż odpowiedni komunikat
                if (isTodayBirthday()) {
                    Toast.makeText(
                        this, "Wszystkiego najlepszego! 🎂", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** Inicjalizuje podstawowe komponenty aplikacji. */
    private fun initializeComponents() {
        // Inicjalizacja AppConfig
        appConfig = AppConfig.getInstance(applicationContext)

        // Upewnij się, że TimeUtils jest zainicjalizowane
        TimeUtils.initialize(applicationContext)

        // Inicjalizacja preferencji
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Inicjalizacja kanałów powiadomień
        NotificationHelper.initNotificationChannels(this)
        TimerNotificationHelper.initTimerNotificationChannel(this)

        if (appConfig.isVerboseLoggingEnabled()) {
            logApplicationState()
        }
    }

    /** Inicjalizuje wszystkie managery za pomocą ManagerFactory. */
    private fun initializeManagers() {
        val factory = ManagerFactory(
            context = this, appConfig = appConfig, prefs = prefs
        )

        managers = factory.createAllManagers(
            activity = this, lifecycleOwner = this
        )

        // Inicjalizuj managery
        managers.initialize()
    }

    /** Ustawia powiadomienia i zadania w tle. */
    private fun setupNotificationsAndTasks() {
        managers.setupNotificationsAndTasks()
    }

    /** Przywraca stan aplikacji po restarcie. */
    private fun restoreApplicationState() {
        val giftReceived = prefs.getBoolean("gift_received", false)
        managers.restoreApplicationState(giftReceived)
    }

    /** Ustawia interfejs użytkownika. */
    private fun setupUI() {
        setContent {
            // Collect state flows
            val darkTheme by managers.settingsManager.isDarkTheme.collectAsState()
            val appName by managers.settingsManager.currentAppName.collectAsState()
            val drawerOpen by managers.appStateManager.isDrawerOpen.collectAsState()
            val navigationSection by managers.appStateManager.currentSection.collectAsState()
            val activeTimerTime by managers.appStateManager.activeTimerRemainingTime.collectAsState()
            val isTimerPaused by managers.appStateManager.isTimerPaused.collectAsState()

            AppTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    // Stan dialogu
                    val showDialog = remember { mutableStateOf(false) }
                    val giftReceived = prefs.getBoolean("gift_received", false)

                    // Oblicz stany związane z urodzinami
                    val isTodayBirthday = isTodayBirthday()
                    val isBirthdayPastThisYear = isBirthdayPastThisYear()
                    val targetDate = calculateTargetDate()

                    // Prośba o uprawnienia do powiadomień w Compose UI
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val requestPermissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            if (isGranted) {
                                Timber.d("Uprawnienia do powiadomień przyznane (z Compose)")
                                Toast.makeText(
                                    this@MainActivity,
                                    "Będziesz powiadomiony w dniu urodzin!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Timber.w("Uprawnienia do powiadomień odrzucone (z Compose)")
                                Toast.makeText(
                                    this@MainActivity,
                                    "Bez uprawnień do powiadomień nie będziesz mógł zobaczyć powiadomienia urodzinowego",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        LaunchedEffect(Unit) {
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    // Główny ekran
                    MainScreen(
                        targetDate = targetDate,
                        currentTime = timeProvider.getCurrentTimeMillis(),
                        onGiftClicked = { showDialog.value = true },
                        activity = this@MainActivity,
                        giftReceived = giftReceived,
                        isTodayBirthday = isTodayBirthday,
                        isBirthdayPastThisYear = isBirthdayPastThisYear,
                        onTimerSet = { minutes -> managers.timerManager.setTimer(minutes) },
                        activeTimer = activeTimerTime,
                        isTimerPaused = isTimerPaused,
                        onResetTimer = { managers.timerManager.resetTimer() },
                        onPauseTimer = { managers.timerManager.pauseTimer() },
                        onResumeTimer = { managers.timerManager.resumeTimer() },
                        isDrawerOpen = drawerOpen,
                        onDrawerStateChange = { isOpen ->
                            managers.appStateManager.setDrawerOpen(isOpen)
                        },
                        currentSection = navigationSection,
                        onSectionSelected = { section ->
                            managers.appStateManager.setCurrentSection(section)

                            // Specjalne działanie dla sekcji prezentu
                            if (section == NavigationSection.GIFT && !giftReceived) {
                                showDialog.value = true
                            }
                        },
                        isDarkTheme = darkTheme,
                        onThemeToggle = { enableDarkTheme ->
                            managers.settingsManager.toggleTheme(enableDarkTheme)
                        },
                        currentAppName = appName,
                        onAppNameChange = { newName ->
                            managers.settingsManager.changeAppName(newName)
                        },
                        onAppNameReset = {
                            managers.settingsManager.resetAppName()
                        })

                    // Dialog pobierania
                    if (showDialog.value) {
                        AlertDialog(
                            onDismissRequest = {
                            Toast.makeText(
                                this@MainActivity,
                                "Anulowano pobieranie prezentu",
                                Toast.LENGTH_SHORT
                            ).show()
                            showDialog.value = false
                        },
                            title = { Text(text = getString(R.string.download_dialog_title)) },
                            text = { Text(text = getString(R.string.download_dialog_message)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        managers.fileManager.downloadFile()
                                        showDialog.value = false
                                    }) {
                                    Text(getString(R.string.yes))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        // "Drugie Tak" - również pobiera plik
                                        managers.fileManager.downloadFile()
                                        showDialog.value = false
                                    }) {
                                    Text(getString(R.string.yes))
                                }
                            })
                    }
                }
            }
        }
    }

    /**
     * Sprawdza czy dzisiaj są urodziny (dzień i miesiąc się zgadzają z
     * konfiguracją).
     *
     * @return true jeśli dzisiaj jest dzień i miesiąc urodzin z konfiguracji
     */
    private fun isTodayBirthday(): Boolean {
        val currentTime = timeProvider.getCurrentTimeMillis()
        val currentCalendar = Calendar.getInstance(WARSAW_TIMEZONE)
        currentCalendar.timeInMillis = currentTime

        val birthdayCalendar = appConfig.getBirthdayDate()

        val isSameDayAndMonth =
            currentCalendar.get(Calendar.DAY_OF_MONTH) == birthdayCalendar.get(Calendar.DAY_OF_MONTH) && currentCalendar.get(
                Calendar.MONTH
            ) == birthdayCalendar.get(Calendar.MONTH)

        Timber.d("Sprawdzanie czy dzisiaj urodziny:")
        Timber.d("  Aktualna data: ${TimeUtils.formatDate(currentCalendar.time)}")
        Timber.d("  Dzień urodzin: ${birthdayCalendar.get(Calendar.DAY_OF_MONTH)}")
        Timber.d("  Miesiąc urodzin: ${birthdayCalendar.get(Calendar.MONTH) + 1}")
        Timber.d("  Aktualny dzień: ${currentCalendar.get(Calendar.DAY_OF_MONTH)}")
        Timber.d("  Aktualny miesiąc: ${currentCalendar.get(Calendar.MONTH) + 1}")
        Timber.d("  isTodayBirthday: $isSameDayAndMonth")

        return isSameDayAndMonth
    }

    /**
     * Sprawdza czy urodziny już były w tym roku (ale nie dzisiaj). Używane do
     * określenia czy pokazać zakładkę "Prezent" w drawer.
     *
     * @return true jeśli urodziny już były w tym roku ale nie dzisiaj
     */
    private fun isBirthdayPastThisYear(): Boolean {
        val currentTime = timeProvider.getCurrentTimeMillis()
        val birthdayThisYear =
            calculateBirthdayForYear(Calendar.getInstance(WARSAW_TIMEZONE).get(Calendar.YEAR))
        val result = currentTime >= birthdayThisYear && !isTodayBirthday()

        Timber.d("Sprawdzanie czy urodziny były w tym roku:")
        Timber.d("  Aktualna data: ${TimeUtils.formatDate(java.util.Date(currentTime))}")
        Timber.d("  Urodziny w tym roku: ${TimeUtils.formatDate(java.util.Date(birthdayThisYear))}")
        Timber.d("  Dzisiaj urodziny: ${isTodayBirthday()}")
        Timber.d("  isBirthdayPastThisYear: $result")

        return result
    }

    /**
     * Oblicza datę docelową w zależności od tego czy dzisiaj są urodziny, czy
     * urodziny już były.
     *
     * @return Datę docelową w milisekundach
     */
    private fun calculateTargetDate(): Long {
        val currentTime = timeProvider.getCurrentTimeMillis()
        val birthdayThisYear =
            calculateBirthdayForYear(Calendar.getInstance(WARSAW_TIMEZONE).get(Calendar.YEAR))

        return when {
            isTodayBirthday() -> {
                // Dzisiaj są urodziny - zwróć datę z tego roku (będzie pokazywał że czas upłynął)
                Timber.d("Dzisiaj są urodziny - używam daty z tego roku")
                birthdayThisYear
            }

            currentTime < birthdayThisYear -> {
                // Urodziny jeszcze nie były w tym roku - licz do tegorocznych
                Timber.d("Urodziny jeszcze nie były - liczę do tegorocznych")
                birthdayThisYear
            }

            else -> {
                // Urodziny już były w tym roku ale nie dzisiaj - licz do przyszłorocznych
                val nextYear = Calendar.getInstance(WARSAW_TIMEZONE).get(Calendar.YEAR) + 1
                val birthdayNextYear = calculateBirthdayForYear(nextYear)
                Timber.d("Urodziny już były - liczę do przyszłorocznych")
                birthdayNextYear
            }
        }
    }

    /** Oblicza datę urodzin dla określonego roku na podstawie konfiguracji. */
    private fun calculateBirthdayForYear(year: Int): Long {
        val birthdayCalendar = Calendar.getInstance(WARSAW_TIMEZONE)
        val configCalendar = appConfig.getBirthdayDate()

        birthdayCalendar.set(Calendar.YEAR, year)
        birthdayCalendar.set(Calendar.MONTH, configCalendar.get(Calendar.MONTH))
        birthdayCalendar.set(Calendar.DAY_OF_MONTH, configCalendar.get(Calendar.DAY_OF_MONTH))
        birthdayCalendar.set(Calendar.HOUR_OF_DAY, configCalendar.get(Calendar.HOUR_OF_DAY))
        birthdayCalendar.set(Calendar.MINUTE, configCalendar.get(Calendar.MINUTE))
        birthdayCalendar.set(Calendar.SECOND, 0)
        birthdayCalendar.set(Calendar.MILLISECOND, 0)

        return birthdayCalendar.timeInMillis
    }

    /**
     * Oblicza datę następnych urodzin w oparciu o aktualny czas.
     *
     * @deprecated Używaj calculateTargetDate() która uwzględnia logikę czy
     *    dzisiaj są urodziny
     */
    @Deprecated("Używaj calculateTargetDate() która uwzględnia logikę czy dzisiaj są urodziny")
    private fun calculateNextBirthday(): Long {
        return calculateTargetDate()
    }

    /** Loguje stan aplikacji dla debugowania. */
    private fun logApplicationState() {
        val isFirstRun = prefs.getBoolean("is_first_run", true)
        val giftReceived = prefs.getBoolean("gift_received", false)

        Timber.d("Konfiguracja załadowana: data urodzin ${TimeUtils.formatDate(appConfig.getBirthdayDate().time)}")
        Timber.d("Pierwsze uruchomienie aplikacji: $isFirstRun")
        Timber.d("Prezent odebrany: $giftReceived")
        Timber.d("Dzisiaj urodziny: ${isTodayBirthday()}")
        Timber.d("Urodziny były w tym roku: ${isBirthdayPastThisYear()}")

        if (appConfig.isTestMode()) {
            Timber.d("UWAGA: Aplikacja działa w trybie testowym!")
        }
    }

    /**
     * Wymusza natychmiastowe sprawdzenie aktualizacji pliku. Metoda publiczna
     * dla kompatybilności z istniejącym kodem.
     */
    fun checkFileNow() {
        managers.fileManager.checkFileNow()
    }

    public override fun onResume() {
        super.onResume()
        Timber.d("MainActivity onResume")

        // Sprawdź stan aplikacji przy wznowieniu
        managers.checkStateOnResume()

        // Sprawdź czy aplikacja została wznowiona z powiadomienia
        handleNotificationIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Wyczyść managery
        managers.cleanup()

        // Wyczyść instancję AppConfig przy zamykaniu aktywności
        AppConfig.clearInstance()

        Timber.d("MainActivity onDestroy - wyczyszczono managery")
    }
}