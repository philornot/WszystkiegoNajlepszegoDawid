package com.philornot.siekiera

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationHelper
import com.philornot.siekiera.notification.NotificationScheduler
import com.philornot.siekiera.notification.TimerNotificationHelper
import com.philornot.siekiera.notification.TimerScheduler
import com.philornot.siekiera.ui.screens.main.core.MainScreen
import com.philornot.siekiera.ui.screens.main.drawer.NavigationSection
import com.philornot.siekiera.ui.theme.AppTheme
import com.philornot.siekiera.utils.FileUtils
import com.philornot.siekiera.utils.RealTimeProvider
import com.philornot.siekiera.utils.TimeProvider
import com.philornot.siekiera.utils.TimeUtils
import com.philornot.siekiera.workers.FileCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    // Strefa czasowa Warszawy
    private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

    // Domyślnie używaj prawdziwego czasu, ale umożliw wstrzyknięcie testowego w testach
    var timeProvider: TimeProvider = RealTimeProvider()

    // Instancja konfiguracji aplikacji
    private lateinit var appConfig: AppConfig

    // Receiver do monitorowania zakończenia zadania FileCheckWorker
    private val workInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Otrzymano broadcast o zakończeniu zadania FileCheckWorker")

            // Po zakończeniu zadania FileCheckWorker, sprawdź pliki
            CoroutineScope(Dispatchers.Main).launch {
                // Krótkie opóźnienie, żeby pliki miały czas zostać zapisane
                delay(500)

                // Sprawdź, czy plik został pobrany poprawnie
                val fileName = appConfig.getDaylioFileName()

                // Sprawdź, czy plik istnieje w folderze Pobrane
                if (FileUtils.isFileInPublicDownloads(fileName)) {
                    Timber.d("Plik $fileName został poprawnie pobrany")

                    // Sprawdź czy już pokazaliśmy powiadomienie o pobraniu
                    val firstDownloadNotified = prefs.getBoolean("first_download_notified", false)

                    // Oznacz prezent jako odebrany
                    prefs.edit {
                        putBoolean("gift_received", true)

                        // Jeśli to pierwsze pobranie, pokaż powiadomienie
                        if (!firstDownloadNotified) {
                            putBoolean("first_download_notified", true)
                        }
                    }

                    // Zaktualizuj stan pobierania
                    isDownloadInProgress.value = false

                    // Pokaż informację o pobraniu pliku tylko raz
                    if (!firstDownloadNotified) {
                        Toast.makeText(
                            context,
                            getString(R.string.download_complete_toast, fileName),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // Stany zarządzania uprawnieniami
    private val notificationPermissionRequested = mutableStateOf(false)
    private val storagePermissionRequested = mutableStateOf(false)

    // Preferencje aplikacji do śledzenia pierwszego uruchomienia
    private lateinit var prefs: SharedPreferences

    // Stan pobierania pliku
    private val isDownloadInProgress = mutableStateOf(false)

    // Stan trybu timera
    private val isTimerMode = mutableStateOf(false)

    // Stan odkrycia trybu timera
    private val timerModeDiscovered = mutableStateOf(false)

    // Stan szufladki nawigacyjnej
    private val _isDrawerOpen = MutableStateFlow(false)
    private val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen

    // Aktualna sekcja nawigacji
    private val _currentSection = MutableStateFlow(NavigationSection.BIRTHDAY_COUNTDOWN)
    private val currentSection: StateFlow<NavigationSection> = _currentSection

    // Aktywny czas timera (w milisekundach)
    private val activeTimerRemainingTime = mutableStateOf(0L)

    // Launcher dla żądania uprawnień do dokładnych alarmów
    private val alarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()

            Timber.d("Powrót z ekranu uprawnień do alarmów, nowy stan uprawnień: $hasPermission")

            if (hasPermission) {
                Timber.d("Uprawnienia do dokładnych alarmów przyznane, planuję powiadomienie")
                scheduleRevealNotification()
            } else {
                Timber.w("Uprawnienia do dokładnych alarmów nadal nie przyznane")
                Toast.makeText(
                    this,
                    "Bez uprawnień do dokładnych alarmów nie mogę zagwarantować punktualnego powiadomienia.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Launcher dla powiadomień
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionRequested.value = true

        if (isGranted) {
            Timber.d("Uprawnienia do powiadomień przyznane")
            Toast.makeText(
                this, "Będziesz powiadomiony w dniu urodzin!", Toast.LENGTH_SHORT
            ).show()
        } else {
            Timber.w("Uprawnienia do powiadomień odrzucone")
            Toast.makeText(
                this,
                "Bez uprawnień do powiadomień nie będziesz mógł zobaczyć powiadomienia urodzinowego",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Launcher dla uprawnień do przechowywania
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        storagePermissionRequested.value = true

        if (isGranted) {
            Timber.d("Uprawnienia do przechowywania przyznane")
        } else {
            Timber.w("Uprawnienia do przechowywania odrzucone")
            Toast.makeText(
                this,
                "Bez uprawnień do przechowywania nie będziesz mógł pobrać prezentu",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        // Inicjalizacja AppConfig
        appConfig = AppConfig.getInstance(applicationContext)

        // Dodane: Upewnij się, że TimeUtils jest zainicjalizowane
        TimeUtils.initialize(applicationContext)

        // Inicjalizacja preferencji
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)

        // Sprawdź, czy prezent został odebrany
        val giftReceived = prefs.getBoolean("gift_received", false)

        // Sprawdź, czy tryb timera został odkryty
        val timerModeEnabled = prefs.getBoolean("timer_mode_discovered", false)
        timerModeDiscovered.value = timerModeEnabled

        // Inicjalizacja kanałów powiadomień
        NotificationHelper.initNotificationChannels(this)

        // Inicjalizacja kanału powiadomień dla timera
        TimerNotificationHelper.initTimerNotificationChannel(this)

        if (appConfig.isVerboseLoggingEnabled()) {
            Timber.d("Konfiguracja załadowana: data urodzin ${TimeUtils.formatDate(appConfig.getBirthdayDate().time)}")
            Timber.d("Pierwsze uruchomienie aplikacji: $isFirstRun")
            Timber.d("Prezent odebrany: $giftReceived")
            Timber.d("Tryb timera odkryty: $timerModeEnabled")

            // Dodane: Wyświetl informację o trybie testowym
            if (appConfig.isTestMode()) {
                Timber.d("UWAGA: Aplikacja działa w trybie testowym!")
            }
        }

        // Sprawdź uprawnienia do alarmów i powiadomień
        requestAlarmPermission()
        requestNotificationPermission()

        // Sprawdź uprawnienia do przechowywania (dla starszych Androidów)
        requestStoragePermission()

        // Zaplanuj powiadomienie na dzień urodzin
        if (appConfig.isBirthdayNotificationEnabled() && !giftReceived) {
            scheduleRevealNotification()
        }

        // Zaplanuj codzienne sprawdzanie aktualizacji pliku na Google Drive
        if (appConfig.isDailyFileCheckEnabled()) {
            scheduleDailyFileCheck()

            // Natychmiast sprawdź plik przy pierwszym uruchomieniu
            if (isFirstRun) {
                checkFileImmediately()

                // Zapisz, że to już nie jest pierwsze uruchomienie
                prefs.edit { putBoolean("is_first_run", false) }
            }
        }

        // Przywróć timer po restarcie, jeśli był aktywny
        if (TimerScheduler.isTimerSet(this)) {
            Timber.d("Przywracanie timera po uruchomieniu aplikacji")

            // Sprawdź, czy timer już się zakończył
            val remainingMillis = TimerScheduler.getRemainingTimeMillis(this)
            if (remainingMillis > 0) {
                // Timer wciąż aktywny, ustaw jego stan
                activeTimerRemainingTime.value = remainingMillis
                isTimerMode.value = true
                // Ustaw odpowiednią sekcję w nawigacji
                _currentSection.value = NavigationSection.TIMER
            } else {
                // Timer już się zakończył, pokaż powiadomienie
                val minutes = TimerScheduler.getTimerMinutes(this)
                TimerNotificationHelper.showTimerCompletedNotification(this, minutes)
                TimerScheduler.cancelTimer(this)
            }
        }

        // Zarejestruj receiver dla monitorowania zadań WorkManager
        registerWorkInfoReceiver()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    // Stan dialogu
                    val showDialog = remember { mutableStateOf(false) }

                    // Collect state flows
                    val drawerOpen by isDrawerOpen.collectAsState()
                    val navigationSection by currentSection.collectAsState()

                    // Prośba o uprawnienia do powiadomień w Compose UI
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val requestPermissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            notificationPermissionRequested.value = true
                            if (isGranted) {
                                Timber.d("Uprawnienia do powiadomień przyznane (z Compose)")
                            } else {
                                Timber.w("Uprawnienia do powiadomień odrzucone (z Compose)")
                            }
                        }

                        LaunchedEffect(Unit) {
                            if (!notificationPermissionRequested.value && ContextCompat.checkSelfPermission(
                                    this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    // Główny ekran z kurtyną, licznikiem i prezentem
                    MainScreen(
                        targetDate = calculateNextBirthday(),
                        currentTime = timeProvider.getCurrentTimeMillis(),
                        onGiftClicked = { showDialog.value = true },
                        activity = this, // Przekaż referencję do aktywności
                        giftReceived = giftReceived,
                        onTimerSet = { minutes ->
                            // Ustaw timer na podaną ilość minut
                            setTimer(minutes)
                        },
                        timerModeEnabled = timerModeDiscovered.value,
                        onTimerModeDiscovered = {
                            // Zapisz, że tryb timera został odkryty
                            timerModeDiscovered.value = true
                            prefs.edit { putBoolean("timer_mode_discovered", true) }

                            // Pokaż krótki komunikat o odkryciu nowej funkcji
                            Toast.makeText(
                                this@MainActivity, "Odkryto tryb timera!", Toast.LENGTH_SHORT
                            ).show()
                        },
                        activeTimer = activeTimerRemainingTime.value,
                        onCancelTimer = {
                            // Anuluj aktywny timer
                            cancelTimer()
                        },
                        onResetTimer = {
                            // Resetuj timer
                            resetTimer()
                        },
                        // Drawer parameters
                        isDrawerOpen = drawerOpen,
                        onDrawerStateChange = { isOpen ->
                            _isDrawerOpen.value = isOpen
                        },
                        currentSection = navigationSection,
                        onSectionSelected = { section ->
                            _currentSection.value = section

                            // Update app state based on section
                            when (section) {
                                NavigationSection.TIMER -> {
                                    isTimerMode.value = true
                                }

                                NavigationSection.BIRTHDAY_COUNTDOWN -> {
                                    isTimerMode.value = false
                                }

                                NavigationSection.GIFT -> {
                                    isTimerMode.value = false
                                    // If needed, automatically show download dialog
                                    if (!giftReceived) {
                                        showDialog.value = true
                                    }
                                }
                            }
                        })

                    // Dialog pobierania
                    if (showDialog.value) {
                        AlertDialog(onDismissRequest = {
                            // Gdy użytkownik kliknie poza dialogiem, anulujemy pobieranie i pokazujemy toast
                            Toast.makeText(
                                this, "Anulowano pobieranie prezentu", Toast.LENGTH_SHORT
                            ).show()
                            showDialog.value = false
                        }, title = {
                            Text(text = getString(R.string.download_dialog_title))
                        }, text = {
                            Text(text = getString(R.string.download_dialog_message))
                        }, confirmButton = {
                            TextButton(
                                onClick = {
                                    downloadFile()
                                    showDialog.value = false
                                }) {
                                Text(getString(R.string.yes))
                            }
                        }, dismissButton = {
                            TextButton(
                                onClick = {
                                    // "Drugie Tak" - również pobiera plik
                                    downloadFile()
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
     * Ustawia timer na określoną ilość minut. Planuje powiadomienie po upływie
     * czasu i zmienia nazwę aplikacji, jeśli jest to wymagane.
     *
     * @param minutes Ilość minut do odliczania
     */
    private fun setTimer(minutes: Int) {
        Timber.d("Ustawianie timera na $minutes minut")

        if (TimerScheduler.scheduleTimer(this, minutes)) {
            // Timer został ustawiony pomyślnie
            Toast.makeText(
                this, getString(R.string.timer_set_toast, minutes), Toast.LENGTH_SHORT
            ).show()

            // Ustaw stan timera
            activeTimerRemainingTime.value = minutes * 60 * 1000L
            isTimerMode.value = true
            _currentSection.value = NavigationSection.TIMER

            // Sprawdź, czy użytkownik chce zmienić nazwę aplikacji poprzez checkbox
            val changeAppName = getSharedPreferences("timer_prefs", MODE_PRIVATE).getBoolean(
                "change_app_name", false
            )

            // Próba zmiany nazwy aplikacji na "Lawendowy Timer" tylko jeśli checkbox jest zaznaczony
            if (changeAppName) {
                tryChangeAppName(true)
            }
        } else {
            // Wystąpił błąd podczas ustawiania timera
            Toast.makeText(
                this,
                "Nie udało się ustawić timera. Sprawdź uprawnienia do alarmów.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Anuluje aktywny timer, jeśli istnieje. */
    fun cancelTimer() {
        if (TimerScheduler.cancelTimer(this)) {
            Toast.makeText(
                this, getString(R.string.timer_cancelled_toast), Toast.LENGTH_SHORT
            ).show()

            // Zresetuj stan timera
            activeTimerRemainingTime.value = 0L
            isTimerMode.value = false

            // Przywróć oryginalną nazwę aplikacji
            tryChangeAppName(false)
        }
    }

    /** Resetuje aktywny timer i przywraca początkowy stan trybu timera. */
    private fun resetTimer() {
        Timber.d("Resetowanie timera")

        // Anuluj aktywny timer
        cancelTimer()

        // Zresetuj stan trybu timera
        activeTimerRemainingTime.value = 0L

        // Zaktualizuj UI
        isTimerMode.value = true // Pozostań w trybie timera
        _currentSection.value = NavigationSection.TIMER

        // Pokaż toast o zresetowaniu timera
        Toast.makeText(
            this, "Timer został zresetowany", Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Oblicza datę następnych urodzin w oparciu o aktualny czas. Jeśli bieżący
     * czas jest po dacie urodzin w tym roku, zwraca datę urodzin w następnym
     * roku.
     */
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

    /**
     * Próbuje zmienić nazwę aplikacji poprzez włączenie/wyłączenie
     * activity-alias. UWAGA: Wymaga uprawnień CHANGE_COMPONENT_ENABLED_STATE,
     * które zwykle są dostępne tylko dla aplikacji systemowych.
     *
     * @param enableTimerMode true, jeśli włączamy tryb timera, false dla
     *    normalnego trybu
     */
    private fun tryChangeAppName(enableTimerMode: Boolean) {
        try {
            // Próba nie powiedzie się dla zwykłych aplikacji (wymaga uprawnień systemowych)
            val packageManager = packageManager
            val originalComponent = ComponentName(this, MainActivity::class.java)
            val timerComponent = ComponentName(this, "com.philornot.siekiera.TimerActivityAlias")

            if (enableTimerMode) {
                // Włącz alias timera, wyłącz oryginalną aktywność
                packageManager.setComponentEnabledSetting(
                    originalComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    timerComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                Timber.d("Zmieniono nazwę aplikacji na 'Lawendowy Timer'")

                // Próba automatycznego ponownego uruchomienia aplikacji
                try {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    // Uruchom nową instancję aplikacji po krótkim opóźnieniu
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(intent)
                        finish() // Zakończ obecną aktywność
                    }, 500)
                } catch (e: Exception) {
                    Timber.e(e, "Błąd podczas próby ponownego uruchomienia aplikacji")
                }
            } else {
                // Włącz oryginalną aktywność, wyłącz alias timera
                packageManager.setComponentEnabledSetting(
                    originalComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    timerComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                Timber.d("Przywrócono oryginalną nazwę aplikacji")

                // Próba automatycznego ponownego uruchomienia aplikacji
                try {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    // Uruchom nową instancję aplikacji po krótkim opóźnieniu
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(intent)
                        finish() // Zakończ obecną aktywność
                    }, 500)
                } catch (e: Exception) {
                    Timber.e(e, "Błąd podczas próby ponownego uruchomienia aplikacji")
                }
            }

            // Zapisz aktualny tryb
            prefs.edit { putBoolean("timer_mode", enableTimerMode) }
            isTimerMode.value = enableTimerMode

        } catch (e: SecurityException) {
            // To oczekiwany błąd - aplikacja nie ma uprawnień do zmiany komponentów
            Timber.w("Brak uprawnień do zmiany nazwy aplikacji: ${e.message}")
        } catch (e: Exception) {
            // Inny nieoczekiwany błąd
            Timber.e(e, "Błąd podczas próby zmiany nazwy aplikacji")
        }
    }

    /** Rejestruje odbiornik do monitorowania stanu zadań WorkManager */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerWorkInfoReceiver() {
        val filter = IntentFilter("com.philornot.siekiera.WORK_COMPLETED")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                workInfoReceiver, filter, RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(workInfoReceiver, filter)
        }

        // Dodaj listener WorkManager, który wyśle broadcast po zakończeniu zadania
        WorkManager.getInstance(this).getWorkInfosByTagLiveData("file_check")
            .observe(this) { workInfoList ->
                if (workInfoList != null && workInfoList.isNotEmpty()) {
                    val workInfo = workInfoList[0]
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        // Wyślij broadcast informujący o zakończeniu zadania
                        val intent = Intent("com.philornot.siekiera.WORK_COMPLETED").apply {
                            // Ustawienie pakietu, aby adresować konkretnie do naszej aplikacji
                            setPackage(packageName)
                        }
                        sendBroadcast(intent)
                    }
                }
            }
    }

    /**
     * Wymusza natychmiastowe sprawdzenie aktualizacji pliku. Można wywołać z
     * innych komponentów, gdy zbliża się moment zakończenia odliczania.
     */
    fun checkFileNow() {
        Timber.d("Wymuszam natychmiastowe sprawdzenie pliku")

        // Używamy zmodyfikowanej metody do planowania jednorazowego sprawdzenia
        FileCheckWorker.planOneTimeCheck(this, forceDownload = false)
    }

    /** Planuje powiadomienie na dzień urodzin. */
    private fun scheduleRevealNotification() {
        Timber.d("Zlecam zaplanowanie powiadomienia urodzinowego")
        NotificationScheduler.scheduleGiftRevealNotification(this, appConfig)
    }

    /**
     * Natychmiastowe sprawdzenie pliku przy pierwszym uruchomieniu. Funkcja
     * dodana dla poprawy doświadczenia użytkownika.
     */
    private fun checkFileImmediately() {
        Timber.d("Wykonuję natychmiastowe sprawdzenie pliku przy pierwszym uruchomieniu")

        // Używamy zmodyfikowanej metody do planowania sprawdzenia
        FileCheckWorker.planOneTimeCheck(this, forceDownload = false)
    }

    /** Planuje codzienne sprawdzanie aktualizacji pliku na Google Drive. */
    private fun scheduleDailyFileCheck() {
        val intervalHours = appConfig.getFileCheckIntervalHours().toLong()

        // Wykorzystanie statycznej metody do tworzenia żądania
        val fileCheckRequest = FileCheckWorker.createWorkRequest(intervalHours).addTag("file_check")
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS).build()

        Timber.d("Planuję codzienne sprawdzanie aktualizacji pliku co $intervalHours godzin")
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            appConfig.getDailyFileCheckWorkName(),
            ExistingPeriodicWorkPolicy.UPDATE, // Zmienione z REPLACE na UPDATE
            fileCheckRequest
        )
    }

    /**
     * Oblicza opóźnienie do pierwszego uruchomienia zadania sprawdzania pliku.
     * Używa strefy czasowej Warszawy.
     */
    private fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance(WARSAW_TIMEZONE)
        val now = calendar.timeInMillis

        // Ustaw czas na 23:59 dzisiaj
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Jeśli już po 23:59, ustaw na jutro
        if (now > calendar.timeInMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = calendar.timeInMillis - now
        Timber.d("Obliczone opóźnienie początkowe: ${delay / (60 * 1000)} minut")
        Timber.d("Zaplanowane na czas warszawski: ${TimeUtils.formatDate(calendar.time)}")
        return delay
    }

    /**
     * Pobiera plik eksportu Daylio z Google Drive lub otwiera istniejący
     * lokalny plik.
     */
    private fun downloadFile() {
        // Log rozpoczęcia procesu
        Timber.d("Użytkownik rozpoczął proces pobierania pliku - inicjuję workflow pobierania")

        // Sprawdź, czy jest włączony tryb testowy - tylko wtedy resetuj flagę gift_received
        if (appConfig.isTestMode()) {
            // W trybie testowym można resetować flagę gift_received
            prefs.edit { putBoolean("gift_received", false) }
            Timber.d("Tryb testowy: zresetowano flagę gift_received")
        }

        // Szczegółowe logi przed sprawdzeniem pliku
        Timber.d("Sprawdzam czy plik już istnieje lokalnie przed próbą pobrania")

        // Sprawdź, czy plik już istnieje w folderze Pobrane
        val fileName = appConfig.getDaylioFileName()
        if (FileUtils.isFileInPublicDownloads(fileName)) {
            // Plik już istnieje - oznacz jako pobrany
            Timber.d("Plik $fileName już istnieje w folderze Pobrane - nie trzeba pobierać ponownie")

            // Oznacz prezent jako odebrany
            prefs.edit {
                putBoolean("gift_received", true)
                putString("downloaded_file_name", fileName)
            }
            Timber.d("Zaktualizowano preferencje: gift_received=true, downloaded_file_name=$fileName")

            // Wyświetl informację o istniejącym pliku
            Toast.makeText(
                this, "Plik $fileName został już pobrany wcześniej", Toast.LENGTH_SHORT
            ).show()

            // Zakończ pobieranie
            isDownloadInProgress.value = false
            return
        }

        // Oznacz jako rozpoczęcie pobierania
        isDownloadInProgress.value = true
        Timber.d("Nie znaleziono lokalnego pliku, oznaczono pobieranie jako w trakcie, uruchomienie workera")

        // Uruchom FileCheckWorker do pobrania najnowszego pliku z wymuszeniem
        Timber.d("Uruchamiam FileCheckWorker z forceDownload=true dla pobrania najnowszego pliku")
        FileCheckWorker.planOneTimeCheck(this, forceDownload = true)

        // Pokaż informację o trwającym pobieraniu
        Toast.makeText(this, getString(R.string.downloading_file), Toast.LENGTH_SHORT).show()
        Timber.d("Wyświetlono toast o pobieraniu, rejestruję obserwatora zakończenia pracy")

        // Rejestrujemy observer na WorkManager, aby monitorować zakończenie pobierania
        WorkManager.getInstance(this).getWorkInfosByTagLiveData("file_check")
            .observe(this) { workInfoList ->
                if (workInfoList != null && workInfoList.isNotEmpty()) {
                    val workInfo = workInfoList[0]
                    Timber.d("Aktualizacja WorkManager: stan pracy to ${workInfo.state}")

                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Timber.d("Zadanie FileCheckWorker zakończone sukcesem")
                        isDownloadInProgress.value = false
                        Timber.d("Zresetowano flagę pobierania w trakcie")
                    } else if (workInfo.state.isFinished) {
                        Timber.d("Zadanie FileCheckWorker zakończone (stan: ${workInfo.state})")
                        isDownloadInProgress.value = false
                        Timber.d("Zresetowano flagę pobierania w trakcie z powodu zakończenia workera (niekoniecznie sukcesu)")
                    }
                }
            }
    }

    /**
     * Sprawdza i żąda uprawnień do dokładnych alarmów (wymagane od Androida
     * 12).
     */
    private fun requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()

            Timber.d("Sprawdzanie uprawnień do dokładnych alarmów: hasPermission=$hasPermission")

            if (!hasPermission) {
                Timber.d("Brak uprawnień do alarmów - otwieram ekran ustawień")
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        data = Uri.fromParts("package", packageName, null)
                    }
                    // Użyj nowego ActivityResultLauncher zamiast startActivityForResult
                    alarmPermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Błąd podczas otwierania ekranu uprawnień")
                    Toast.makeText(
                        this, "Nie można otworzyć ekranu uprawnień: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Timber.d("Uprawnienia do alarmów już przyznane")
            }
        } else {
            Timber.d("Android < 12, uprawnienia do alarmów nie są wymagane")
        }
    }

    /**
     * Sprawdza i żąda uprawnień do wyświetlania powiadomień (wymagane od
     * Androida 13).
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    // Pokaż wyjaśnienie dlaczego potrzebujemy uprawnień
                    Toast.makeText(
                        this,
                        "Potrzebujemy uprawnień do wyświetlania powiadomień, aby poinformować Cię gdy prezent będzie gotowy.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Nie używamy launcher z Compose, bo ten kod wykonuje się przed utworzeniem UI
                if (!notificationPermissionRequested.value) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                Timber.d("Uprawnienia do powiadomień już przyznane")
            }
        } else {
            Timber.d("Android < 13, uprawnienia do powiadomień nie są wymagane")
        }
    }

    /**
     * Sprawdza i żąda uprawnień do przechowywania (wymagane dla starszych
     * wersji Androida).
     */
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    // Pokaż wyjaśnienie dlaczego potrzebujemy uprawnień
                    Toast.makeText(
                        this,
                        "Potrzebujemy uprawnień do przechowywania, aby zapisać prezent w folderze Pobrane.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (!storagePermissionRequested.value) {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            } else {
                Timber.d("Uprawnienia do przechowywania już przyznane")
            }
        } else {
            Timber.d("Android > P, uprawnienia do przechowywania nie są wymagane")
        }
    }


    @Override
    public override fun onResume() {
        super.onResume()
        Timber.d("MainActivity onResume")

        // Sprawdź ponownie uprawnienia po powrocie do aplikacji
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()
            Timber.d("Sprawdzanie uprawnień w onResume: hasPermission=$hasPermission")

            // Jeśli uprawnienia zostały przyznane poza normalnym flow (np. z ustawień)
            if (hasPermission && appConfig.isBirthdayNotificationEnabled()) {
                scheduleRevealNotification()
            }
        }

        // Sprawdź stan timera po wznowieniu aplikacji - użyj nowej metody zapobiegającej spamowaniu
        if (TimerScheduler.checkAndCleanupTimer(this)) {
            // Timer wciąż działa, aktualizuj czas pozostały
            val remainingMillis = TimerScheduler.getRemainingTimeMillis(this)
            if (remainingMillis > 0) {
                activeTimerRemainingTime.value = remainingMillis
                isTimerMode.value = true
                _currentSection.value = NavigationSection.TIMER
            }

            // Sprawdź czy tryb timera jest włączony dla nazwy aplikacji
            if (!isTimerMode.value) {
                tryChangeAppName(true)
            }
        } else {
            // Nie ma aktywnego timera, przywróć normalny tryb
            if (isTimerMode.value) {
                isTimerMode.value = false
                activeTimerRemainingTime.value = 0L
                tryChangeAppName(false)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(workInfoReceiver)
            Timber.d("Wyrejestrowano BroadcastReceiver w onDestroy")

            // Wyczyść instancję AppConfig przy zamykaniu aktywności
            AppConfig.clearInstance()
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas wyrejestrowywania receivera")
        }
    }
}