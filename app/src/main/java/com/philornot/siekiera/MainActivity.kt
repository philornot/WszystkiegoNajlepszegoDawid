package com.philornot.siekiera

import android.Manifest
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
import androidx.core.content.edit
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.managers.ManagerContainer
import com.philornot.siekiera.managers.ManagerFactory
import com.philornot.siekiera.notification.NotificationHelper
import com.philornot.siekiera.notification.TimerNotificationHelper
import com.philornot.siekiera.ui.screens.main.core.MainScreen
import com.philornot.siekiera.ui.screens.main.drawer.NavigationSection
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

        // Inicjalizacja UI
        setupUI()
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
        val timerModeEnabled = prefs.getBoolean("timer_mode_discovered", false)

        managers.restoreApplicationState(giftReceived, timerModeEnabled)
    }

    /** Ustawia interfejs użytkownika. */
    private fun setupUI() {
        setContent {
            // Collect state flows
            val darkTheme by managers.settingsManager.isDarkTheme.collectAsState()
            val appName by managers.settingsManager.currentAppName.collectAsState()
            val drawerOpen by managers.appStateManager.isDrawerOpen.collectAsState()
            val navigationSection by managers.appStateManager.currentSection.collectAsState()
            val timerModeDiscovered by managers.appStateManager.timerModeDiscovered.collectAsState()
            val activeTimerTime by managers.appStateManager.activeTimerRemainingTime.collectAsState()
            val isTimerPaused by managers.appStateManager.isTimerPaused.collectAsState()

            AppTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    // Stan dialogu
                    val showDialog = remember { mutableStateOf(false) }
                    val giftReceived = prefs.getBoolean("gift_received", false)

                    // Prośba o uprawnienia do powiadomień w Compose UI
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val requestPermissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            if (isGranted) {
                                Timber.d("Uprawnienia do powiadomień przyznane (z Compose)")
                            } else {
                                Timber.w("Uprawnienia do powiadomień odrzucone (z Compose)")
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
                        targetDate = calculateNextBirthday(),
                        currentTime = timeProvider.getCurrentTimeMillis(),
                        onGiftClicked = { showDialog.value = true },
                        activity = this@MainActivity,
                        giftReceived = giftReceived,
                        onTimerSet = { minutes -> managers.timerManager.setTimer(minutes) },
                        timerModeEnabled = timerModeDiscovered,
                        onTimerModeDiscovered = {
                            managers.appStateManager.setTimerModeDiscovered(true)
                            prefs.edit { putBoolean("timer_mode_discovered", true) }
                            Toast.makeText(
                                this@MainActivity, "Odkryto tryb timera!", Toast.LENGTH_SHORT
                            ).show()
                        },
                        activeTimer = activeTimerTime,
                        isTimerPaused = isTimerPaused,
                        onCancelTimer = { managers.timerManager.cancelTimer() },
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

    /** Oblicza datę następnych urodzin w oparciu o aktualny czas. */
    private fun calculateNextBirthday(): Long {
        val currentTime = System.currentTimeMillis()

        // Pobierz datę urodzin skonfigurowaną w aplikacji
        val birthdayCalendar = Calendar.getInstance(WARSAW_TIMEZONE)
        val configCalendar = appConfig.getBirthdayDate()

        // Ustaw datę urodzin w bieżącym roku
        birthdayCalendar.set(Calendar.YEAR, birthdayCalendar.get(Calendar.YEAR))
        birthdayCalendar.set(Calendar.MONTH, configCalendar.get(Calendar.MONTH))
        birthdayCalendar.set(Calendar.DAY_OF_MONTH, configCalendar.get(Calendar.DAY_OF_MONTH))
        birthdayCalendar.set(Calendar.HOUR_OF_DAY, configCalendar.get(Calendar.HOUR_OF_DAY))
        birthdayCalendar.set(Calendar.MINUTE, configCalendar.get(Calendar.MINUTE))
        birthdayCalendar.set(Calendar.SECOND, 0)
        birthdayCalendar.set(Calendar.MILLISECOND, 0)

        // Jeśli bieżący czas jest po dacie urodzin w tym roku, dodaj rok
        if (currentTime > birthdayCalendar.timeInMillis) {
            birthdayCalendar.add(Calendar.YEAR, 1)
        }

        return birthdayCalendar.timeInMillis
    }

    /** Loguje stan aplikacji dla debugowania. */
    private fun logApplicationState() {
        val isFirstRun = prefs.getBoolean("is_first_run", true)
        val giftReceived = prefs.getBoolean("gift_received", false)
        val timerModeEnabled = prefs.getBoolean("timer_mode_discovered", false)

        Timber.d("Konfiguracja załadowana: data urodzin ${TimeUtils.formatDate(appConfig.getBirthdayDate().time)}")
        Timber.d("Pierwsze uruchomienie aplikacji: $isFirstRun")
        Timber.d("Prezent odebrany: $giftReceived")
        Timber.d("Tryb timera odkryty: $timerModeEnabled")

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

    /**
     * Sprawdza pliki okresowo gdy zbliża się koniec odliczania. Wywoływane z
     * MainScreen.
     */
    fun checkFilesPeriodically(timeRemaining: Long) {
        managers.fileManager.checkFilesPeriodically(timeRemaining)
    }

    /**
     * Wykonuje ostatnie sprawdzenie pliku po upływie czasu odliczania.
     * Wywoływane z MainScreen.
     */
    fun performFinalFileCheck() {
        managers.fileManager.performFinalCheck()
    }

    public override fun onResume() {
        super.onResume()
        Timber.d("MainActivity onResume")

        // Sprawdź stan aplikacji przy wznowieniu
        managers.checkStateOnResume()
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