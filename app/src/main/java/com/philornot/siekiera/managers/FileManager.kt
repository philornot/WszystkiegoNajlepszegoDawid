package com.philornot.siekiera.managers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.philornot.siekiera.R
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.utils.FileUtils
import com.philornot.siekiera.utils.TimeUtils
import com.philornot.siekiera.workers.FileCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Manager zarządzający pobieraniem plików i WorkManager. Enkapsuluje
 * logikę związaną z synchronizacją plików z Google Drive.
 */
class FileManager(
    private val context: Context,
    private val appConfig: AppConfig,
    private val appStateManager: AppStateManager,
    private val prefs: SharedPreferences,
    private val lifecycleOwner: LifecycleOwner,
) {

    // Strefa czasowa Warszawy
    @Suppress("PrivatePropertyName")
    private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

    // Receiver do monitorowania zakończenia zadania FileCheckWorker
    private val workInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Otrzymano broadcast o zakończeniu zadania FileCheckWorker")
            handleWorkCompletion()
        }
    }

    /** Inicjalizuje FileManager - rejestruje receivery i planuje zadania. */
    fun initialize() {
        registerWorkInfoReceiver()

        val isFirstRun = prefs.getBoolean("is_first_run", true)

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
    }

    /**
     * Pobiera plik eksportu Daylio z Google Drive lub otwiera istniejący
     * lokalny plik.
     */
    fun downloadFile() {
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
                context, "Plik $fileName został już pobrany wcześniej", Toast.LENGTH_SHORT
            ).show()

            // Zakończ pobieranie
            appStateManager.setDownloadInProgress(false)
            return
        }

        // Oznacz jako rozpoczęcie pobierania
        appStateManager.setDownloadInProgress(true)
        Timber.d("Nie znaleziono lokalnego pliku, oznaczono pobieranie jako w trakcie, uruchomienie workera")

        // Uruchom FileCheckWorker do pobrania najnowszego pliku z wymuszeniem
        Timber.d("Uruchamiam FileCheckWorker z forceDownload=true dla pobrania najnowszego pliku")
        FileCheckWorker.planOneTimeCheck(context, forceDownload = true)

        // Pokaż informację o trwającym pobieraniu
        Toast.makeText(context, context.getString(R.string.downloading_file), Toast.LENGTH_SHORT)
            .show()
        Timber.d("Wyświetlono toast o pobieraniu, rejestruję obserwatora zakończenia pracy")

        // Rejestrujemy observer na WorkManager, aby monitorować zakończenie pobierania
        WorkManager.getInstance(context).getWorkInfosByTagLiveData("file_check")
            .observe(lifecycleOwner) { workInfoList ->
                if (workInfoList != null && workInfoList.isNotEmpty()) {
                    val workInfo = workInfoList[0]
                    Timber.d("Aktualizacja WorkManager: stan pracy to ${workInfo.state}")

                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Timber.d("Zadanie FileCheckWorker zakończone sukcesem")
                        appStateManager.setDownloadInProgress(false)
                        Timber.d("Zresetowano flagę pobierania w trakcie")
                    } else if (workInfo.state.isFinished) {
                        Timber.d("Zadanie FileCheckWorker zakończone (stan: ${workInfo.state})")
                        appStateManager.setDownloadInProgress(false)
                        Timber.d("Zresetowano flagę pobierania w trakcie z powodu zakończenia workera (niekoniecznie sukcesu)")
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
        FileCheckWorker.planOneTimeCheck(context, forceDownload = false)
    }

    /**
     * Sprawdza pliki okresowo gdy zbliża się koniec odliczania. Wywoływane w
     * pętli głównej MainActivity.
     */
    fun checkFilesPeriodically(timeRemaining: Long) {
        val remainingMinutes = timeRemaining / 60000

        // Określ minimalny interwał między sprawdzeniami
        val checkInterval = when {
            remainingMinutes <= 1 -> 30_000      // 30 sekund w ostatniej minucie
            remainingMinutes <= 5 -> 60_000      // 1 minuta
            remainingMinutes <= 15 -> 120_000    // 2 minuty
            remainingMinutes <= 60 -> 300_000    // 5 minut
            else -> 900_000                       // 15 minut
        }

        val currentTime = System.currentTimeMillis()
        val lastCheckTime = prefs.getLong("last_periodic_check", 0)

        // Sprawdź tylko jeśli:
        // 1. Prezent nie został jeszcze odebrany
        // 2. Minął odpowiedni czas od ostatniego sprawdzenia
        // 3. Sprawdzenie nie jest zablokowane przez inny worker
        val giftReceived = prefs.getBoolean("gift_received", false)

        if (!giftReceived && currentTime - lastCheckTime >= checkInterval && FileCheckWorker.canCheckFile(
                context
            )
        ) {

            Timber.d("Uruchamiam dodatkowe sprawdzenie pliku, pozostało $remainingMinutes minut (interwał: ${checkInterval / 1000}s)")
            checkFileNow()
            prefs.edit { putLong("last_periodic_check", currentTime) }
        }
    }

    /** Wykonuje ostatnie sprawdzenie pliku po upływie czasu odliczania. */
    fun performFinalCheck() {
        Timber.d("Uruchamiam ostatnie sprawdzenie pliku po upływie czasu")
        checkFileNow()
        prefs.edit { putLong("last_periodic_check", System.currentTimeMillis()) }
    }

    /** Rejestruje odbiornik do monitorowania stanu zadań WorkManager. */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerWorkInfoReceiver() {
        val filter = IntentFilter("com.philornot.siekiera.WORK_COMPLETED")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                workInfoReceiver, filter, Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(workInfoReceiver, filter)
        }

        // Dodaj listener WorkManager, który wyśle broadcast po zakończeniu zadania
        WorkManager.getInstance(context).getWorkInfosByTagLiveData("file_check")
            .observe(lifecycleOwner) { workInfoList ->
                if (workInfoList != null && workInfoList.isNotEmpty()) {
                    val workInfo = workInfoList[0]
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        // Wyślij broadcast informujący o zakończeniu zadania
                        val intent = Intent("com.philornot.siekiera.WORK_COMPLETED").apply {
                            // Ustawienie pakietu, aby adresować konkretnie do naszej aplikacji
                            setPackage(context.packageName)
                        }
                        context.sendBroadcast(intent)
                    }
                }
            }
    }

    /** Obsługuje zakończenie pracy FileCheckWorker. */
    private fun handleWorkCompletion() {
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
                appStateManager.setDownloadInProgress(false)

                // Pokaż informację o pobraniu pliku tylko raz
                if (!firstDownloadNotified) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.download_complete_toast, fileName),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** Natychmiastowe sprawdzenie pliku przy pierwszym uruchomieniu. */
    private fun checkFileImmediately() {
        Timber.d("Wykonuję natychmiastowe sprawdzenie pliku przy pierwszym uruchomieniu")
        FileCheckWorker.planOneTimeCheck(context, forceDownload = false)
    }

    /** Planuje codzienne sprawdzanie aktualizacji pliku na Google Drive. */
    private fun scheduleDailyFileCheck() {
        val intervalHours = appConfig.getFileCheckIntervalHours().toLong()

        // Wykorzystanie statycznej metody do tworzenia żądania
        val fileCheckRequest = FileCheckWorker.createWorkRequest(intervalHours).addTag("file_check")
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS).build()

        Timber.d("Planuję codzienne sprawdzanie aktualizacji pliku co $intervalHours godzin")
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
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

    /** Wyrejestruj receiver przy zniszczeniu. */
    fun cleanup() {
        try {
            context.unregisterReceiver(workInfoReceiver)
            Timber.d("Wyrejestrowano FileManager BroadcastReceiver")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas wyrejestrowywania FileManager receivera")
        }
    }
}