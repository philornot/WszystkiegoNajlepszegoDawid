package com.philornot.siekiera.workers

import android.content.Context
import androidx.work.CoroutineWorker
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

/**
 * Worker, który codziennie o 23:59 sprawdza, czy w folderze na Google
 * Drive jest nowsza wersja pliku.
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
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas sprawdzania aktualizacji pliku")
            Result.retry()
        }
    }

    private suspend fun checkForFileUpdate() {
        val context = getContext() ?: run {
            Timber.e("Brak kontekstu - nie można kontynuować")
            return
        }

        val appConfig = getAppConfig() ?: run {
            Timber.e("Brak konfiguracji - nie można kontynuować")
            return
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

                // Zapisz do pliku lokalnego
                localFile.outputStream().use { output ->
                    fileContent.use { input ->
                        input.copyTo(output)
                    }
                }

                Timber.d("Plik pobrany i zapisany pomyślnie: ${localFile.absolutePath}")
            } catch (e: IOException) {
                Timber.e(e, "Błąd podczas pobierania pliku")
                throw e
            }
        }
    }

    companion object {
        // Do testów - pozwala na wstrzyknięcie mocka
        @JvmStatic
        internal var testDriveClient: DriveApiClient? = null
    }
}