package com.philornot.siekiera.workers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.network.DriveApiClient
import com.philornot.siekiera.notification.NotificationHelper
import com.philornot.siekiera.utils.FileUtils
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

/**
 * Worker, który codziennie o 23:59 sprawdza, czy w folderze na Google
 * Drive jest nowsza wersja pliku.
 *
 * Dodano mechanizm pobierania pliku do publicznego folderu Pobrane oraz
 * wysyłanie powiadomień o pobraniu pliku.
 *
 * Zawiera mechanizmy zapobiegające wielokrotnemu pobieraniu tego samego
 * pliku.
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
            // Najpierw sprawdź czy zadanie nie jest już uruchomione
            if (isWorkAlreadyRunning()) {
                Timber.d("Inne zadanie sprawdzania pliku już działa - pomijam")
                return@coroutineScope Result.success()
            }

            // Sprawdź czy już pobraliśmy plik
            if (isFileAlreadyDownloaded()) {
                Timber.d("Plik już został pobrany - pomijam pobieranie")
                return@coroutineScope Result.success()
            }

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

    /** Sprawdza, czy już istnieje uruchomione zadanie sprawdzania pliku */
    private fun isWorkAlreadyRunning(): Boolean {
        val context = getContext() ?: return false

        // Używamy shared preferences do synchronizacji między instancjami
        val prefs = context.getSharedPreferences("file_check_prefs", Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong("last_check_time", 0)
        val currentTime = System.currentTimeMillis()

        // Jeśli od ostatniego sprawdzenia minęło mniej niż 5 sekund, zakładamy że już działa
        if (currentTime - lastCheckTime < 5000) {
            return true
        }

        // Zapisz aktualny czas sprawdzenia
        prefs.edit { putLong("last_check_time", currentTime) }
        return false
    }

    /** Sprawdza, czy plik został już pobrany i jest w folderze Pobrane */
    private fun isFileAlreadyDownloaded(): Boolean {
        val context = getContext() ?: return false
        val appConfig = getAppConfig() ?: return false

        // Sprawdź, czy prezent został już odebrany (ale tylko gdy nie jesteśmy w trybie wymuszania)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val giftReceived = prefs.getBoolean("gift_received", false)
        val forceDownload = inputData.getBoolean("force_download", false)

        if (giftReceived && !forceDownload) {
            // Dodatkowo sprawdź, czy plik istnieje w folderze Pobrane
            val fileName = appConfig.getDaylioFileName()
            if (FileUtils.isFileInPublicDownloads(fileName)) {
                return true
            }
        }

        return false
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

        // Sprawdź czy plik już istnieje w folderze Pobrane
        val forceDownload = inputData.getBoolean("force_download", false)
        if (FileUtils.isFileInPublicDownloads(fileName) && !forceDownload) {
            Timber.d("Plik $fileName już istnieje w folderze Pobrane - pomijam pobieranie")

            // Oznacz prezent jako odebrany, nawet jeśli już wcześniej istniał
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit { putBoolean("gift_received", true) }

            return
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

        // Jeśli prezent został już odebrany i nie wymuszamy pobrania, nie pobieraj pliku ponownie
        if (giftReceived && !forceDownload) {
            Timber.d("Prezent został już odebrany, pomijam pobieranie")
            return
        }

        // Użyj nazwy pliku bezpośrednio z konfiguracji
        val downloadFileName = fileName

        // Pobierz plik bezpośrednio do katalogu Pobrane
        val result = downloadFileDirect(driveClient, newestFile.id, downloadFileName)

        if (result) {
            // Powiadomienie o pobraniu pliku - tylko jeśli to pierwsze powiadomienie
            val firstDownloadNotified = prefs.getBoolean("first_download_notified", false)

            if (!firstDownloadNotified) {
                NotificationHelper.showDownloadCompleteNotification(context, downloadFileName)
                prefs.edit { putBoolean("first_download_notified", true) }
            }

            // Zapisz, że prezent został odebrany i przy okazji zapisz nazwę pliku
            prefs.edit {
                putBoolean("gift_received", true)
                    .putString("downloaded_file_name", downloadFileName)
            }

            Timber.d("Plik pobrany pomyślnie: $downloadFileName")
        } else {
            Timber.e("Nieudane pobranie pliku")
            throw IOException("Nieudane pobranie pliku")
        }
    }

    /**
     * Pobiera plik z Google Drive i zapisuje go bezpośrednio w publicznym
     * folderze Pobrane, używając nowoczesnego podejścia zgodnego z Scoped
     * Storage.
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
                // Najpierw sprawdź, czy plik już istnieje
                val context = getContext() ?: throw IllegalStateException("Brak kontekstu")

                if (FileUtils.isFileInPublicDownloads(fileName)) {
                    Timber.d("Plik $fileName już istnieje w folderze Pobrane - używam istniejącego pliku")
                    return@withContext true
                }

                // Pobierz zawartość pliku z Google Drive
                val inputStream = client.downloadFile(fileId)
                var uri: Uri? = null

                // Różne podejścia do zapisywania pliku w zależności od wersji Androida
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ używa MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            inputStream.use { input ->
                                input.copyTo(outputStream)
                            }
                        }

                        // Zakończ transakcję
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        Timber.d("Plik zapisany przez MediaStore: $uri")
                        return@withContext true
                    } else {
                        Timber.e("Nie można utworzyć URI dla pliku")
                        return@withContext false
                    }
                } else {
                    // Dla starszych wersji Androida, korzystamy z External Storage
                    try {
                        val downloadsDir =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }

                        val destinationFile = File(downloadsDir, fileName)
                        java.io.FileOutputStream(destinationFile).use { outputStream ->
                            inputStream.use { input ->
                                input.copyTo(outputStream)
                            }
                        }

                        Timber.d("Plik zapisany do ${destinationFile.absolutePath}")
                        return@withContext destinationFile.exists() && destinationFile.length() > 0
                    } catch (e: IOException) {
                        // Jeśli metoda z External Storage nie zadziała, spróbuj użyć MediaStore
                        Timber.w(
                            "Nie udało się zapisać pliku bezpośrednio, próbuję przez MediaStore", e
                        )

                        // Alternatywne podejście: zapisz we własnym katalogu aplikacji
                        val internalFile = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName
                        )
                        java.io.FileOutputStream(internalFile).use { outputStream ->
                            inputStream.use { input ->
                                input.copyTo(outputStream)
                            }
                        }

                        Timber.d("Plik zapisany do katalogu aplikacji: ${internalFile.absolutePath}")
                        return@withContext internalFile.exists() && internalFile.length() > 0
                    }
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

        // Stałe dla WorkManager
        private const val FILE_CHECK_WORK_TAG = "file_check"
        private const val ONE_TIME_WORK_NAME = "one_time_file_check"

        /**
         * Tworzy request workera z odpowiednią polityką backoff. Używaj tej metody
         * w MainActivity zamiast bezpośrednio tworzyć request.
         */
        fun createWorkRequest(intervalHours: Long) = PeriodicWorkRequestBuilder<FileCheckWorker>(
            intervalHours, TimeUnit.HOURS
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL, 30, // Minimalne opóźnienie to 30 minut
            TimeUnit.MINUTES
        ).addTag(FILE_CHECK_WORK_TAG)

        /**
         * Tworzy jednorazowy request sprawdzający aktualizacje pliku. Użyteczne
         * przy częstszym sprawdzaniu gdy licznik dobiega końca.
         *
         * @param forceDownload Czy wymusić pobieranie pliku nawet jeśli prezent
         *    został już odebrany
         */
        fun createOneTimeCheckRequest(forceDownload: Boolean = false) =
            OneTimeWorkRequestBuilder<FileCheckWorker>().setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                ).addTag(FILE_CHECK_WORK_TAG).setInputData(
                    androidx.work.Data.Builder().putBoolean("force_download", forceDownload).build()
                )

        /**
         * Sprawdza, czy można uruchomić sprawdzenie pliku, aby uniknąć zbyt
         * częstego wywoływania.
         *
         * @param context Kontekst aplikacji
         * @return true jeśli można wykonać sprawdzenie, false jeśli należy
         *    poczekać
         */
        fun canCheckFile(context: Context): Boolean {
            val prefs = context.getSharedPreferences("file_check_prefs", Context.MODE_PRIVATE)
            val lastCheckTime = prefs.getLong("last_check_time", 0)
            val currentTime = System.currentTimeMillis()

            // Jeśli od ostatniego sprawdzenia minęło mniej niż 2 sekundy, pomijamy
            return currentTime - lastCheckTime > 2000
        }

        /**
         * Planuje jednorazowe sprawdzenie pliku z ochroną przed duplikatami.
         *
         * @param context Kontekst aplikacji
         * @param forceDownload Czy wymusić pobieranie pliku
         */
        fun planOneTimeCheck(context: Context, forceDownload: Boolean = false) {
            // Sprawdź, czy można wykonać sprawdzenie (nie za często)
            if (!canCheckFile(context)) {
                Timber.d("Zbyt częste próby sprawdzenia pliku - pomijam")
                return
            }

            // Zbuduj żądanie jednorazowego sprawdzenia
            val request = createOneTimeCheckRequest(forceDownload).build()

            // Użyj REPLACE aby zastąpić inne oczekujące zadania
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )

            // Aktualizuj czas ostatniego sprawdzenia
            context.getSharedPreferences("file_check_prefs", Context.MODE_PRIVATE).edit {
                putLong("last_check_time", System.currentTimeMillis())
            }

            Timber.d("Zaplanowano jednorazowe sprawdzenie pliku (forceDownload=$forceDownload)")
        }
    }
}