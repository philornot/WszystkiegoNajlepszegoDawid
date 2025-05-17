package com.philornot.siekiera

import android.Manifest
import android.app.AlarmManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import com.philornot.siekiera.ui.screens.main.MainScreen
import com.philornot.siekiera.ui.theme.AppTheme
import com.philornot.siekiera.utils.RealTimeProvider
import com.philornot.siekiera.utils.TimeProvider
import com.philornot.siekiera.utils.TimeUtils
import com.philornot.siekiera.workers.FileCheckWorker
import timber.log.Timber
import java.io.File
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

    // Receiver do wykrywania zakończenia pobierania
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            Timber.d("Otrzymano broadcast o zakończeniu pobierania: id=$id")
        }
    }

    // Stany zarządzania uprawnieniami
    private val notificationPermissionRequested = mutableStateOf(false)

    // Launcher dla żądania uprawnień do dokładnych alarmów
    private val alarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val hasPermission = alarmManager.canScheduleExactAlarms()

            Timber.d("Powrót z ekranu uprawnień do alarmów, nowy stan uprawnień: $hasPermission")

            if (hasPermission) {
                Timber.d("Uprawnienia do dokładnych alarmów przyznane, planuję powiadomienie")
                scheduleRevealNotification()

                // Pokaż potwierdzenie użytkownikowi
                Toast.makeText(
                    this,
                    "Uprawnienia przyznane! Powiadomienie zostanie wyświetlone w dniu urodzin.",
                    Toast.LENGTH_LONG
                ).show()
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

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        // Inicjalizacja AppConfig
        appConfig = AppConfig.getInstance(applicationContext)

        if (appConfig.isVerboseLoggingEnabled()) {
            Timber.d("Konfiguracja załadowana: data urodzin ${TimeUtils.formatDate(appConfig.getBirthdayDate().time)}")
        }

        // Sprawdź uprawnienia do alarmów i powiadomień
        requestAlarmPermission()
        requestNotificationPermission()

        // Zaplanuj powiadomienie na dzień urodzin
        if (appConfig.isBirthdayNotificationEnabled()) {
            scheduleRevealNotification()
        }

        // Zaplanuj codzienne sprawdzanie aktualizacji pliku
        if (appConfig.isDailyFileCheckEnabled()) {
            scheduleDailyFileCheck()
        }

        // Zarejestruj receiver dla pobierania
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_EXPORTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8-12
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                0 // Flaga nie jest wymagana dla Androida 8-12
            )
        } else { // Android poniżej 8
            registerReceiver(
                onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    // Stan dialogu
                    val showDialog = remember { mutableStateOf(false) }

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
                        targetDate = TimeUtils.getRevealDateMillis(appConfig),
                        currentTime = timeProvider.getCurrentTimeMillis(),
                        onGiftClicked = { showDialog.value = true })

                    // Dialog pobierania
                    if (showDialog.value) {
                        AlertDialog(onDismissRequest = { showDialog.value = false }, title = {
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
                                onClick = { showDialog.value = false }) {
                                Text(getString(R.string.no))
                            }
                        })
                    }
                }
            }
        }
    }

    /** Planuje powiadomienie na dzień urodzin. */
    private fun scheduleRevealNotification() {
        Timber.d("Zlecam zaplanowanie powiadomienia urodzinowego")
        NotificationScheduler.scheduleGiftRevealNotification(this, appConfig)
    }

    /** Planuje codzienne sprawdzanie aktualizacji pliku na Google Drive. */
    private fun scheduleDailyFileCheck() {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val intervalHours = appConfig.getFileCheckIntervalHours().toLong()

        val fileCheckRequest = PeriodicWorkRequestBuilder<FileCheckWorker>(
            intervalHours, TimeUnit.HOURS
        ).setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS).build()

        Timber.d("Planuję codzienne sprawdzanie aktualizacji pliku co $intervalHours godzin")
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            appConfig.getDailyFileCheckWorkName(),
            ExistingPeriodicWorkPolicy.REPLACE,
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
        val directory = Environment.DIRECTORY_DOWNLOADS
        val fileName = appConfig.getDaylioFileName()
        val file = File(getExternalFilesDir(directory), fileName)

        if (file.exists()) {
            Timber.d("Plik już istnieje lokalnie, otwieram: ${file.absolutePath}")
            openDaylioFile(file)
            return
        }

        Timber.d("Plik nie istnieje lokalnie, zlecam pobranie z Google Drive")
        // Rozpocznij pobieranie za pomocą FileCheckWorker zamiast używać DownloadManager
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<FileCheckWorker>().setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        ).build()

        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)

        // Pokaż informację o trwającym pobieraniu
        Toast.makeText(this, getString(R.string.downloading_file), Toast.LENGTH_SHORT).show()
    }

    /** Otwiera plik Daylio za pomocą zainstalowanej aplikacji. */
    private fun openDaylioFile(file: File) {
        Timber.d("Otwieranie pliku Daylio: ${file.absolutePath}")

        try {
            // Otwórz plik za pomocą zewnętrznej aplikacji (Daylio jeśli jest zainstalowana)
            val intent = Intent(Intent.ACTION_VIEW)
            val uri =
                androidx.core.content.FileProvider.getUriForFile(
                    this, "${applicationContext.packageName}.provider", file
                )

            intent.setDataAndType(uri, "application/octet-stream")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(intent, getString(R.string.open_with))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(chooser)
            } else {
                Timber.e("Brak aplikacji do otworzenia pliku .daylio")
                Toast.makeText(
                    this,
                    "Brak aplikacji do otworzenia pliku Daylio. Zainstaluj aplikację Daylio.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas otwierania pliku Daylio")
            Toast.makeText(
                this, "Błąd podczas otwierania pliku: ${e.message}", Toast.LENGTH_LONG
            ).show()
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
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(onDownloadComplete)
            Timber.d("Wyrejestrowano BroadcastReceiver w onDestroy")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas wyrejestrowywania receivera")
        }
    }
}