package com.philornot.siekiera.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.network.DriveApiClient
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Worker, który codziennie o 23:59 sprawdza, czy w folderze na Google
 * Drive jest nowsza wersja pliku.
 *
 * Dodano mechanizm exponential backoff oraz lepszą obsługę błędów.
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

        // Wyszukaj pliki .daylio w folderze
        val files = driveClient.listFilesInFolder(folderId).filter { it.name.endsWith(".daylio") }

        if (files.isEmpty()) {
            Timber.d("Nie znaleziono plików .daylio w folderze")
            return
        }

        // Znajdź najnowszy plik
        val newestFile = files.maxByOrNull { it.modifiedTime.time } ?: return

        Timber.d(
            "Znaleziono plik: ${newestFile.name}, data modyfikacji: ${
                TimeUtils.formatDate(
                    newestFile.modifiedTime
                )
            }"
        )

        // Sprawdź lokalny plik
        val localFile = getLocalFile(context, fileName)

        if (!localFile.exists()) {
            // Plik lokalny nie istnieje, pobierz go
            Timber.d("Plik lokalny nie istnieje, pobieram plik...")
            downloadAndSaveFile(driveClient, newestFile.id, localFile)
            return
        }

        // Sprawdź, czy zdalny plik jest nowszy
        if (isRemoteFileNewer(localFile, newestFile.modifiedTime)) {
            // Utwórz kopię zapasową lokalnego pliku przed usunięciem
            createBackup(localFile)

            // Usuń stary plik
            Timber.d(
                "Zdalny plik jest nowszy (lokalny: ${
                    TimeUtils.formatDate(
                        java.util.Date(
                            localFile.lastModified()
                        )
                    )
                }, zdalny: ${TimeUtils.formatDate(newestFile.modifiedTime)})"
            )
            localFile.delete()

            // Pobierz nowy plik
            downloadAndSaveFile(driveClient, newestFile.id, localFile)

            Timber.d("Plik zaktualizowany pomyślnie")
        } else {
            Timber.d("Aktualny plik jest najnowszy, nie ma potrzeby aktualizacji")
        }
    }

    /**
     * Tworzy kopię zapasową pliku przed jego nadpisaniem. To zapewnia, że w
     * przypadku uszkodzenia pliku podczas pobierania będziemy mogli przywrócić
     * poprzednią wersję.
     */
    private fun createBackup(file: File) {
        try {
            if (file.exists()) {
                val backupFile = File("${file.absolutePath}.bak")

                // Usuń starą kopię zapasową, jeśli istnieje
                if (backupFile.exists()) {
                    backupFile.delete()
                }

                // Skopiuj plik do pliku kopii zapasowej
                file.copyTo(backupFile, overwrite = true)
                Timber.d("Utworzono kopię zapasową pliku: ${backupFile.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas tworzenia kopii zapasowej pliku")
            // Nie przerywamy operacji, jeśli utworzenie kopii nie powiodło się
        }
    }

    private fun getLocalFile(context: Context, fileName: String): File {
        val directory = android.os.Environment.DIRECTORY_DOWNLOADS
        val file = File(context.getExternalFilesDir(directory), fileName)

        // Upewnij się, że katalog istnieje
        file.parentFile?.mkdirs()

        return file
    }

    private fun isRemoteFileNewer(localFile: File, remoteModifiedTime: java.util.Date): Boolean {
        val localModified = localFile.lastModified()
        return remoteModifiedTime.time > localModified
    }

    private suspend fun downloadAndSaveFile(
        client: DriveApiClient,
        fileId: String,
        localFile: File,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val fileContent = client.downloadFile(fileId)

                // Zapisz do pliku tymczasowego, aby uniknąć uszkodzenia w przypadku błędu
                val tempFile = File("${localFile.absolutePath}.tmp")

                tempFile.outputStream().use { output ->
                    fileContent.use { input ->
                        input.copyTo(output)
                    }
                }

                // Kiedy pobieranie się powiedzie, zmień nazwę pliku tymczasowego na docelową
                if (tempFile.exists() && tempFile.length() > 0) {
                    if (tempFile.renameTo(localFile)) {
                        Timber.d("Plik pobrany i zapisany pomyślnie: ${localFile.absolutePath}")
                    } else {
                        throw IOException("Nie udało się zmienić nazwy pliku tymczasowego na docelową")
                    }
                } else {
                    throw IOException("Pobrany plik jest pusty lub nie istnieje")
                }
            } catch (e: IOException) {
                Timber.e(e, "Błąd podczas pobierania pliku")

                // Sprawdź, czy istnieje kopia zapasowa i przywróć ją w przypadku błędu
                val backupFile = File("${localFile.absolutePath}.bak")
                if (backupFile.exists() && !localFile.exists()) {
                    Timber.d("Przywracanie kopii zapasowej pliku...")
                    try {
                        backupFile.copyTo(localFile, overwrite = true)
                        Timber.d("Pomyślnie przywrócono kopię zapasową")
                    } catch (backupException: Exception) {
                        Timber.e(backupException, "Nie udało się przywrócić kopii zapasowej")
                    }
                }

                throw e
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
         * Tworzy jednorazowy request sprawdzający aktualizacje pliku.
         * Użyteczne przy częstszym sprawdzaniu gdy licznik dobiega końca.
         */
        fun createOneTimeCheckRequest() =
            OneTimeWorkRequestBuilder<FileCheckWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
    }
}