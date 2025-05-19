package com.philornot.siekiera.workers

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.network.DriveApiClient
import com.philornot.siekiera.notification.NotificationHelper
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Worker, który codziennie o 23:59 sprawdza, czy w folderze na Google
 * Drive jest nowsza wersja pliku.
 *
 * Dodano mechanizm pobierania pliku do publicznego folderu Pobrane oraz
 * wysyłanie powiadomień o pobraniu pliku.
 */
class FileCheckWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    // Używamy WeakReference, aby uniknąć memory leaks
    private val contextRef = WeakReference(context.applicationContext)

    // Funkcja pomocnicza do uzyskania kontekstu
    private fun getContext(): Context? = contextRef.get()

    // Getter dla AppConfig - nie przechowujemy referencji, tylko pobieramy w razie potrzeby
    private fun getAppConfig(): AppConfig? {
        val context = getContext() ?: return null
        return AppConfig.getInstance(context)
    }

    // Method to get DriveApiClient using a testable approach
    private fun getDriveClient(context: Context): DriveApiClient {
        return testDriveClient ?: DriveApiClient.getInstance(context)
    }

    override suspend fun doWork(): Result = coroutineScope {
        Timber.d("Rozpoczynam sprawdzanie aktualizacji pliku...")

        try {
            withContext(Dispatchers.IO) {
                checkForFileUpdate()
            }

            // Powiadom aktywność o zakończeniu pracy
            sendCompletionBroadcast()

            Result.success()
        } catch (e: IOException) {
            // Błędy sieciowe - próbujemy ponownie z backoff policy
            Timber.e(e, "Błąd sieciowy podczas sprawdzania aktualizacji pliku")
            Result.retry()
        } catch (e: Exception) {
            // Nieoczekiwane błędy - zapisujemy do logów i kończymy błędem
            Timber.e(e, "Nieoczekiwany błąd podczas sprawdzania aktualizacji pliku")

            // Maksymalna liczba prób
            val runAttemptCount = runAttemptCount
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Timber.d("Próba $runAttemptCount z $MAX_RETRY_ATTEMPTS, ponawiam...")
                Result.retry()
            } else {
                Timber.d("Osiągnięto maksymalną liczbę prób ($MAX_RETRY_ATTEMPTS), poddaję się.")
                Result.failure()
            }
        }
    }

    /**
     * Wysyła broadcast o zakończeniu pracy workera. Pozwala na powiadomienie
     * aktywności, że pobranie pliku zostało zakończone.
     */
    private fun sendCompletionBroadcast() {
        val context = getContext() ?: return
        val intent = Intent("com.philornot.siekiera.WORK_COMPLETED")
        context.sendBroadcast(intent)
        Timber.d("Wysłano broadcast o zakończeniu pracy workera")
    }

    private suspend fun checkForFileUpdate() {
        val context = getContext() ?: run {
            Timber.e("Brak kontekstu - nie można kontynuować")
            throw IllegalStateException("Brak kontekstu - nie można kontynuować")
        }

        val appConfig = getAppConfig() ?: run {
            Timber.e("Brak konfiguracji - nie można kontynuować")
            throw IllegalStateException("Brak konfiguracji - nie można kontynuować")
        }

        // Pobierz dane z konfiguracji
        val folderId = appConfig.getDriveFolderId()
        val fileName = appConfig.getDaylioFileName()

        if (appConfig.isVerboseLoggingEnabled()) {
            Timber.d("Sprawdzanie aktualizacji pliku w folderze: $folderId, plik: $fileName")
        }

        // Get the Drive client using the testable method
        val driveClient = getDriveClient(context)

        // Zainicjalizuj klienta, jeśli nie jest już zainicjalizowany
        if (!driveClient.initialize()) {
            Timber.e("Nie udało się zainicjalizować klienta Drive API")
            throw IOException("Nie udało się zainicjalizować klienta Drive API")
        }

        // Wyszukaj wszystkie pliki .daylio w folderze (bez filtrowania po nazwie)
        val files = driveClient.listFilesInFolder(folderId).filter { it.name.endsWith(".daylio") }

        if (files.isEmpty()) {
            Timber.d("Nie znaleziono plików .daylio w folderze")
            return
        }

        // Znajdź najnowszy plik - sortuj według daty modyfikacji w kolejności malejącej
        val newestFile = files.maxByOrNull { it.modifiedTime.time } ?: return

        Timber.d(
            "Znaleziono plik: ${newestFile.name}, data modyfikacji: ${
                TimeUtils.formatDate(
                    newestFile.modifiedTime
                )
            }, rozmiar: ${newestFile.size} bajtów"
        )

        // Sprawdź, czy prezent został już odebrany
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val giftReceived = prefs.getBoolean("gift_received", false)

        // Jeśli prezent został już odebrany, nie pobieraj pliku ponownie
        if (giftReceived && !appConfig.isTestMode()) {
            Timber.d("Prezent został już odebrany, pomijam pobieranie")
            return
        }

        // Użyj nazwy pliku bezpośrednio z konfiguracji
        val downloadFileName = fileName

        // Pobierz plik bezpośrednio do katalogu Pobrane
        val result = downloadFileDirect(driveClient, newestFile.id, downloadFileName)

        if (result) {
            // Powiadomienie o pobraniu pliku
            NotificationHelper.showDownloadCompleteNotification(context, downloadFileName)

            // Zapisz, że prezent został odebrany i przy okazji zapisz nazwę pliku
            prefs.edit().putBoolean("gift_received", true)
                .putString("downloaded_file_name", downloadFileName).apply()

            Timber.d("Plik pobrany pomyślnie: $downloadFileName")
        } else {
            Timber.e("Nieudane pobranie pliku")
            throw IOException("Nieudane pobranie pliku")
        }
    }

    /**
     * Pobiera plik z Google Drive i zapisuje go bezpośrednio w publicznym
     * folderze Pobrane.
     *
     * @param client Klient DriveAPI
     * @param fileId ID pliku Google Drive
     * @param fileName Docelowa nazwa pliku
     * @return true jeśli pobieranie się powiodło, false w przeciwnym razie
     */
    private suspend fun downloadFileDirect(
        client: DriveApiClient,
        fileId: String,
        fileName: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Pobierz zawartość pliku z Google Drive
                val inputStream = client.downloadFile(fileId)

                // Utwórz referencję do katalogu Pobrane
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                // Upewnij się, że katalog istnieje
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                // Utwórz plik docelowy
                val destinationFile = File(downloadsDir, fileName)

                // Kopiuj treść pliku
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }

                // Sprawdź, czy plik został utworzony i ma poprawny rozmiar
                if (destinationFile.exists() && destinationFile.length() > 0) {
                    Timber.d("Plik zapisany do ${destinationFile.absolutePath}")
                    return@withContext true
                } else {
                    Timber.e("Plik nie został poprawnie zapisany")
                    return@withContext false
                }
            } catch (e: Exception) {
                Timber.e(e, "Błąd podczas pobierania pliku bezpośrednio do folderu Pobrane")
                return@withContext false
            }
        }
    }

    companion object {
        // Do testów - pozwala na wstrzyknięcie mocka
        @JvmStatic
        internal var testDriveClient: DriveApiClient? = null

        // Maksymalna liczba ponownych prób w przypadku błędu
        private const val MAX_RETRY_ATTEMPTS = 3

        /**
         * Tworzy request workera z odpowiednią polityką backoff. Używaj tej metody
         * w MainActivity zamiast bezpośrednio tworzyć request.
         */
        fun createWorkRequest(intervalHours: Long) =
            androidx.work.PeriodicWorkRequestBuilder<FileCheckWorker>(
                intervalHours, TimeUnit.HOURS
            ).setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL, 30, // Minimalne opóźnienie to 30 minut
                TimeUnit.MINUTES
            )

        /**
         * Tworzy jednorazowy request sprawdzający aktualizacje pliku. Użyteczne
         * przy częstszym sprawdzaniu gdy licznik dobiega końca.
         */
        fun createOneTimeCheckRequest() =
            OneTimeWorkRequestBuilder<FileCheckWorker>().setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build()
            )
    }
}