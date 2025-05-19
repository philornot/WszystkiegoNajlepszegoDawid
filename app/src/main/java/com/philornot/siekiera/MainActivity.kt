package com.philornot.siekiera

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.notification.NotificationScheduler
import com.philornot.siekiera.ui.screens.main.MainScreen
import com.philornot.siekiera.ui.theme.AppTheme
import com.philornot.siekiera.utils.RealTimeProvider
import com.philornot.siekiera.utils.TimeProvider
import com.philornot.siekiera.utils.TimeUtils
import com.philornot.siekiera.workers.FileCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

            // Sprawdź plik po zakończeniu pobierania
            checkFileAfterDownload()
        }
    }

    // Receiver do monitorowania zakończenia zadania FileCheckWorker
    private val workInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Otrzymano broadcast o zakończeniu zadania FileCheckWorker")

            // Po zakończeniu zadania FileCheckWorker, sprawdź pliki
            CoroutineScope(Dispatchers.Main).launch {
                // Krótkie opóźnienie, żeby pliki miały czas zostać zapisane
                delay(500)
                checkDownloadedFiles()
            }
        }
    }

    // Stany zarządzania uprawnieniami
    private val notificationPermissionRequested = mutableStateOf(false)

    // Preferencje aplikacji do śledzenia pierwszego uruchomienia
    private lateinit var prefs: SharedPreferences

    // Stan pobierania pliku
    private val isDownloadInProgress = mutableStateOf(false)
    private val downloadFileName = mutableStateOf("")

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

        // Dodane: Upewnij się, że TimeUtils jest zainicjalizowane
        TimeUtils.initialize(applicationContext)

        // Inicjalizacja preferencji
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)

        if (appConfig.isVerboseLoggingEnabled()) {
            Timber.d("Konfiguracja załadowana: data urodzin ${TimeUtils.formatDate(appConfig.getBirthdayDate().time)}")
            Timber.d("Pierwsze uruchomienie aplikacji: $isFirstRun")

            // Dodane: Wyświetl informację o trybie testowym
            if (appConfig.isTestMode()) {
                Timber.d("UWAGA: Aplikacja działa w trybie testowym!")
            }
        }

        // Sprawdź uprawnienia do alarmów i powiadomień
        requestAlarmPermission()
        requestNotificationPermission()

        // Zaplanuj powiadomienie na dzień urodzin
        if (appConfig.isBirthdayNotificationEnabled()) {
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

        // Zarejestruj receiver dla pobierania
        registerDownloadReceiver()

        // Zarejestruj receiver dla monitorowania zadań WorkManager
        registerWorkInfoReceiver()

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
                        onGiftClicked = { showDialog.value = true },
                        activity = this // Przekaż referencję do aktywności
                    )

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
                        val intent = Intent("com.philornot.siekiera.WORK_COMPLETED")
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

        // Wykorzystanie statycznej metody do tworzenia żądania
        val oneTimeWorkRequest = FileCheckWorker.createOneTimeCheckRequest()
            .addTag("file_check") // Dodajemy tag, aby można było śledzić stan zadania
            .build()

        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)
    }

    /**
     * Sprawdza, czy w folderze pobranych plików pojawił się nowy plik .daylio
     * po zakończeniu pobierania przez FileCheckWorker
     */
    private fun checkDownloadedFiles() {
        val directory = Environment.DIRECTORY_DOWNLOADS
        val externalDir = getExternalFilesDir(directory)

        Timber.d("Sprawdzanie pobranych plików w katalogu: ${externalDir?.absolutePath}")

        externalDir?.listFiles()?.filter {
            it.name.endsWith(".daylio")
        }?.maxByOrNull {
            it.lastModified()
        }?.let { newestFile ->
            Timber.d("Znaleziono najnowszy plik .daylio: ${newestFile.name} (${newestFile.length()} bajtów)")

            // Aktualizuj nazwę pliku w stanie
            downloadFileName.value = newestFile.name

            // Otwórz plik używając zainstalowanej aplikacji
            openDaylioFile(newestFile)

            // Oznacz pobieranie jako zakończone
            isDownloadInProgress.value = false
        } ?: run {
            Timber.w("Nie znaleziono żadnych plików .daylio w katalogu pobranych")
        }
    }

    /** Sprawdza stan pliku po zakończeniu pobierania */
    private fun checkFileAfterDownload() {
        // Sprawdź wszystkie pliki .daylio w katalogu pobranych
        val directory = Environment.DIRECTORY_DOWNLOADS
        val externalDir = getExternalFilesDir(directory)

        Timber.d("Sprawdzanie plików po zakończeniu pobierania w katalogu: ${externalDir?.absolutePath}")

        // Znajdź najnowszy plik .daylio
        externalDir?.listFiles()?.filter {
            it.name.endsWith(".daylio")
        }?.maxByOrNull {
            it.lastModified()
        }?.let { newestFile ->
            Timber.d("Znaleziono najnowszy plik po pobraniu: ${newestFile.name} (${newestFile.length()} bajtów)")

            Toast.makeText(
                this, "Plik został pobrany pomyślnie!", Toast.LENGTH_SHORT
            ).show()

            // Aktualizuj nazwę pliku w stanie
            downloadFileName.value = newestFile.name

            // Automatyczne otwarcie pliku po pobraniu
            CoroutineScope(Dispatchers.Main).launch {
                // Dodajemy małe opóźnienie, aby upewnić się, że plik został poprawnie zapisany
                delay(500)
                openDaylioFile(newestFile)
            }

            // Oznacz pobieranie jako zakończone
            isDownloadInProgress.value = false
        } ?: run {
            Timber.w("Nie znaleziono żadnych plików .daylio po pobraniu")
            Toast.makeText(
                this, "Wystąpił problem z pobraniem pliku. Spróbuj ponownie.", Toast.LENGTH_LONG
            ).show()
        }
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

        // Wykorzystanie statycznej metody do tworzenia żądania
        val oneTimeWorkRequest =
            FileCheckWorker.createOneTimeCheckRequest().addTag("file_check").build()

        WorkManager.getInstance(this).enqueue(oneTimeWorkRequest)
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
        // FileCheckWorker pobierze najnowszy plik z Google Drive bez względu na nazwę
        // Tutaj zainicjujemy sprawdzenie, które pobierze najnowszy plik

        Timber.d("Rozpoczynam pobieranie najnowszego pliku z Google Drive")

        // Oznacz jako rozpoczęcie pobierania
        isDownloadInProgress.value = true

        if (appConfig.isTestMode()) {
            // W trybie testowym usuwamy istniejące pliki, aby wymusić pobranie
            val directory = Environment.DIRECTORY_DOWNLOADS
            val filePattern = ".daylio"

            // Usuń wszystkie pliki .daylio, aby mieć pewność, że pobierze najnowszy
            getExternalFilesDir(directory)?.listFiles()?.forEach { file ->
                if (file.name.endsWith(filePattern)) {
                    Timber.d("Tryb testowy włączony - usuwam lokalny plik: ${file.name}")
                    file.delete()
                }
            }
        }

        // Uruchom FileCheckWorker do pobrania najnowszego pliku
        checkFileNow()

        // Pokaż informację o trwającym pobieraniu
        Toast.makeText(this, getString(R.string.downloading_file), Toast.LENGTH_SHORT).show()

        // Rejestrujemy observer na WorkManager, aby monitorować zakończenie pobierania
        WorkManager.getInstance(this).getWorkInfosByTagLiveData("file_check")
            .observe(this) { workInfoList ->
                if (workInfoList != null && workInfoList.isNotEmpty()) {
                    val workInfo = workInfoList[0]

                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Timber.d("Zadanie FileCheckWorker zakończone sukcesem - sprawdzam pliki")

                        // Sprawdź pliki po zakończeniu zadania
                        CoroutineScope(Dispatchers.Main).launch {
                            // Daj plikowi chwilę na zapisanie się
                            delay(500)
                            checkDownloadedFiles()
                        }
                    } else if (workInfo.state.isFinished) {
                        Timber.d("Zadanie FileCheckWorker zakończone (stan: ${workInfo.state}) - sprawdzam pliki")

                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500)
                            checkDownloadedFiles()
                        }
                    }
                }
            }
    }

    /** Otwiera plik Daylio za pomocą zainstalowanej aplikacji. */
    private fun openDaylioFile(file: File) {
        Timber.d("Otwieranie pliku Daylio: ${file.absolutePath}")

        try {
            val uri = FileProvider.getUriForFile(
                this, "${applicationContext.packageName}.provider", file
            )

            Timber.d("Utworzono URI dla pliku: $uri")

            // PRÓBA 1: Otwieranie z MIME type "application/octet-stream"
            val intent1 = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/octet-stream")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // WAŻNE: Celowo usuwamy FLAG_ACTIVITY_NEW_TASK, która może blokować dialog
            }

            // PRÓBA 2: Otwieranie z MIME type "*/*" (dla wszystkich aplikacji)
            val intent2 = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Wybierz odpowiedni intent - sprawdź czy są aplikacje dostępne
            val useIntent =
                if (packageManager.queryIntentActivities(intent1, PackageManager.MATCH_DEFAULT_ONLY)
                        .isNotEmpty()
                ) {
                    Timber.d("Używam intencji z MIME type application/octet-stream")
                    intent1
                } else {
                    Timber.d("Używam intencji z MIME type */*")
                    intent2
                }

            // Utworzenie choosera z wymuszoną opcją wyboru aplikacji
            val chooserIntent = Intent.createChooser(
                useIntent, getString(R.string.open_with)
            )

            // Sprawdź czy są aplikacje dostępne dla tego typu pliku
            val activities =
                packageManager.queryIntentActivities(useIntent, PackageManager.MATCH_DEFAULT_ONLY)

            if (activities.isEmpty()) {
                Timber.w("Nie znaleziono aplikacji do otwarcia pliku!")
                Toast.makeText(
                    this,
                    "Nie znaleziono aplikacji do otwarcia pliku ${file.name}!",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            Timber.d("Znaleziono ${activities.size} aplikacji do otwarcia pliku:")
            activities.forEach { resolveInfo ->
                Timber.d(" - ${resolveInfo.activityInfo.packageName} / ${resolveInfo.activityInfo.name}")
            }

            // Uruchom wybór aplikacji
            startActivity(chooserIntent)

        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas otwierania pliku Daylio")
            Toast.makeText(
                this, "Błąd podczas otwierania pliku: ${e.message}", Toast.LENGTH_LONG
            ).show()

            // Spróbuj alternatywną metodę otwarcia
            tryAlternativeFileOpening(file)
        }
    }

    /** Alternatywna metoda otwierania pliku, jeśli standardowa nie zadziała */
    private fun tryAlternativeFileOpening(file: File) {
        try {
            Timber.d("Próbuję alternatywną metodę otwarcia pliku")

            // Stwórz URI
            val uri = FileProvider.getUriForFile(
                this, "${applicationContext.packageName}.provider", file
            )

            // Użyj intencji z setDataAndType, bez ustawiania flag ACTIVITY_NEW_TASK
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Używanie programistycznego podejścia do wyboru aktywności
            val title = getString(R.string.open_with)
            val chooser = Intent.createChooser(intent, title)

            // Dodaj flagi bezpośrednio do choosera
            chooser.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach {
                    Timber.d("Granting permission to ${it.activityInfo.packageName}")
                    grantUriPermission(
                        it.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            startActivity(chooser)

        } catch (e: Exception) {
            Timber.e(e, "Alternatywna metoda otwarcia pliku również zawiodła")
            Toast.makeText(
                this,
                "Błąd podczas próby otwarcia pliku. Spróbuj otworzyć plik manualnie z katalogu: ${file.parentFile?.absolutePath}",
                Toast.LENGTH_LONG
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

    /**
     * Rejestruje BroadcastReceiver dla pobierania w sposób zgodny z różnymi
     * wersjami Androida.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerDownloadReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    public override fun onResume() {
        super.onResume()
        Timber.d("MainActivity onResume")

        // Po wznowieniu aktywności, sprawdź pliki w folderze Download
        // Może użytkownik uruchomił inną aplikację do pobrania pliku
        if (isDownloadInProgress.value) {
            Timber.d("Sprawdzam czy pobieranie zostało zakończone podczas nieaktywności aplikacji")
            checkDownloadedFiles()
        }

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
            unregisterReceiver(workInfoReceiver)
            Timber.d("Wyrejestrowano BroadcastReceiver w onDestroy")

            // Wyczyść instancję AppConfig przy zamykaniu aktywności
            AppConfig.clearInstance()
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas wyrejestrowywania receivera")
        }
    }
}