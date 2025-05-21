package com.philornot.siekiera.workers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.edit
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
        Timber.d("FileCheckWorker: Rozpoczynam workflow sprawdzania pliku")

        try {
            // Najpierw sprawdź czy zadanie nie jest już uruchomione
            if (isWorkAlreadyRunning()) {
                Timber.d("FileCheckWorker: Inne zadanie sprawdzania pliku już działa - pomijam")
                return@coroutineScope Result.success()
            }
            Timber.d("FileCheckWorker: Brak innych uruchomionych zadań, kontynuuję sprawdzanie")

            // Sprawdź czy już pobraliśmy plik
            if (isFileAlreadyDownloaded()) {
                Timber.d("FileCheckWorker: Plik już został pobrany - pomijam pobieranie")
                return@coroutineScope Result.success()
            }
            Timber.d("FileCheckWorker: Plik jeszcze nie pobrany lub wymuszono pobieranie, kontynuuję")

            withContext(Dispatchers.IO) {
                Timber.d("FileCheckWorker: Rozpoczynam sprawdzanie aktualizacji pliku na dispatcherze IO")
                checkForFileUpdate()
                Timber.d("FileCheckWorker: Sprawdzanie aktualizacji pliku zakończone pomyślnie")
            }

            // Powiadom aktywność o zakończeniu pracy
            Timber.d("FileCheckWorker: Wysyłam broadcast o zakończeniu pracy")
            sendCompletionBroadcast()

            Timber.d("FileCheckWorker: Praca zakończona sukcesem")
            Result.success()
        } catch (e: IOException) {
            // Błędy sieciowe - próbujemy ponownie z backoff policy
            Timber.e(
                e, "FileCheckWorker: Błąd sieciowy podczas sprawdzania pliku - spróbuję ponownie"
            )
            Result.retry()
        } catch (e: Exception) {
            // Nieoczekiwane błędy - zapisujemy do logów i kończymy błędem
            Timber.e(
                e, "FileCheckWorker: Nieoczekiwany błąd podczas sprawdzania pliku: ${e.message}"
            )

            // Maksymalna liczba prób
            val runAttemptCount = runAttemptCount
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Timber.d("FileCheckWorker: Próba $runAttemptCount z $MAX_RETRY_ATTEMPTS, ponawiam...")
                Result.retry()
            } else {
                Timber.d("FileCheckWorker: Osiągnięto maksymalną liczbę prób ($MAX_RETRY_ATTEMPTS), poddaję się.")
                Result.failure()
            }
        }
    }

    /** Sprawdza, czy już istnieje uruchomione zadanie sprawdzania pliku */
    private fun isWorkAlreadyRunning(): Boolean {
        val context = getContext() ?: return false
        Timber.d("FileCheckWorker: Sprawdzam czy inne zadanie sprawdzania jest już uruchomione")

        // Używamy shared preferences do synchronizacji między instancjami
        val prefs = context.getSharedPreferences("file_check_prefs", Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong("last_check_time", 0)
        val currentTime = System.currentTimeMillis()

        // Jeśli od ostatniego sprawdzenia minęło mniej niż 5 sekund, zakładamy że już działa
        if (currentTime - lastCheckTime < 5000) {
            Timber.d("FileCheckWorker: Inne sprawdzenie rozpoczęto mniej niż 5 sekund temu (${currentTime - lastCheckTime}ms) - uznano za już uruchomione")
            return true
        }

        // Zapisz aktualny czas sprawdzenia
        Timber.d("FileCheckWorker: Brak wykrytych niedawnych sprawdzeń, aktualizuję last_check_time")
        prefs.edit { putLong("last_check_time", currentTime) }
        return false
    }

    /** Sprawdza, czy plik został już pobrany i jest w folderze Pobrane */
    private fun isFileAlreadyDownloaded(): Boolean {
        val context = getContext() ?: return false
        val appConfig = getAppConfig() ?: return false
        Timber.d("FileCheckWorker: Sprawdzam czy plik został już pobrany")

        // Sprawdź, czy prezent został już odebrany (ale tylko gdy nie jesteśmy w trybie wymuszania)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val giftReceived = prefs.getBoolean("gift_received", false)
        val forceDownload = inputData.getBoolean("force_download", false)
        Timber.d("FileCheckWorker: Aktualny stan - gift_received=$giftReceived, force_download=$forceDownload")

        if (giftReceived && !forceDownload) {
            // Dodatkowo sprawdź, czy plik istnieje w folderze Pobrane
            val fileName = appConfig.getDaylioFileName()
            val fileExists = FileUtils.isFileInPublicDownloads(fileName)
            Timber.d("FileCheckWorker: Prezent już odebrany i plik lokalny istnieje=$fileExists")
            return fileExists
        }

        Timber.d("FileCheckWorker: Plik musi zostać pobrany (prezent nie odebrany lub wymuszono pobieranie)")
        return false
    }

    /**
     * Wysyła broadcast o zakończeniu pracy workera. Pozwala na powiadomienie
     * aktywności, że pobranie pliku zostało zakończone.
     */
    private fun sendCompletionBroadcast() {
        val context = getContext() ?: return
        Timber.d("FileCheckWorker: Wysyłam broadcast że praca została zakończona")
        val intent = Intent("com.philornot.siekiera.WORK_COMPLETED")
        context.sendBroadcast(intent)
        Timber.d("FileCheckWorker: Broadcast wysłany: com.philornot.siekiera.WORK_COMPLETED")
    }

    private suspend fun checkForFileUpdate() {
        val context = getContext() ?: run {
            Timber.e("FileCheckWorker: Brak kontekstu - nie można kontynuować")
            throw IllegalStateException("Brak kontekstu - nie można kontynuować")
        }

        val appConfig = getAppConfig() ?: run {
            Timber.e("FileCheckWorker: Brak konfiguracji - nie można kontynuować")
            throw IllegalStateException("Brak konfiguracji - nie można kontynuować")
        }

        // Pobierz dane z konfiguracji
        val folderId = appConfig.getDriveFolderId()
        val fileName = appConfig.getDaylioFileName()

        Timber.d("FileCheckWorker: Sprawdzanie aktualizacji pliku w folderze: $folderId, docelowy plik: $fileName")

        // Sprawdź czy plik już istnieje w folderze Pobrane
        val forceDownload = inputData.getBoolean("force_download", false)
        if (FileUtils.isFileInPublicDownloads(fileName) && !forceDownload) {
            Timber.d("FileCheckWorker: Plik $fileName już istnieje w folderze Pobrane - pomijam pobieranie")

            // Oznacz prezent jako odebrany, nawet jeśli już wcześniej istniał
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit { putBoolean("gift_received", true) }
            Timber.d("FileCheckWorker: Zaktualizowano preferencje: gift_received=true")

            return
        }

        Timber.d("FileCheckWorker: Plik wymaga pobrania${if (forceDownload) " (wymuszono pobranie)" else ""}")

        // Get the Drive client using the testable method
        val driveClient = getDriveClient(context)
        Timber.d("FileCheckWorker: Uzyskano instancję klienta Drive API")

        // Zainicjalizuj klienta, jeśli nie jest już zainicjalizowany
        if (!driveClient.initialize()) {
            Timber.e("FileCheckWorker: Nie udało się zainicjalizować klienta Drive API")
            throw IOException("Nie udało się zainicjalizować klienta Drive API")
        }
        Timber.d("FileCheckWorker: Klient Drive API zainicjalizowany pomyślnie")

        // Wyszukaj wszystkie pliki .daylio w folderze (bez filtrowania po nazwie)
        Timber.d("FileCheckWorker: Wyszukiwanie plików .daylio w folderze")
        val files = driveClient.listFilesInFolder(folderId).filter { it.name.endsWith(".daylio") }
        Timber.d("FileCheckWorker: Znaleziono ${files.size} plików .daylio w folderze")

        if (files.isEmpty()) {
            Timber.d("FileCheckWorker: Nie znaleziono plików .daylio w folderze - nic do pobrania")
            return
        }

        // Znajdź najnowszy plik - sortuj według daty modyfikacji w kolejności malejącej
        val newestFile = files.maxByOrNull { it.modifiedTime.time } ?: return
        Timber.d(
            "FileCheckWorker: Najnowszy plik: ${newestFile.name}, zmodyfikowany: ${
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
            Timber.d("FileCheckWorker: Prezent został już odebrany i nie wymuszono pobrania - pomijam pobieranie")
            return
        }

        // Użyj nazwy pliku bezpośrednio z konfiguracji
        val downloadFileName = fileName
        Timber.d("FileCheckWorker: Plik zostanie pobrany jako: $downloadFileName")

        // Pobierz plik bezpośrednio do katalogu Pobrane
        Timber.d("FileCheckWorker: Rozpoczynam bezpośrednie pobieranie pliku")
        val result = downloadFileDirect(driveClient, newestFile.id, downloadFileName)

        if (result) {
            // Powiadomienie o pobraniu pliku - tylko jeśli to pierwsze powiadomienie
            val firstDownloadNotified = prefs.getBoolean("first_download_notified", false)

            if (!firstDownloadNotified) {
                Timber.d("FileCheckWorker: Wyświetlam pierwsze powiadomienie o pobraniu")
                NotificationHelper.showDownloadCompleteNotification(context, downloadFileName)
                prefs.edit { putBoolean("first_download_notified", true) }
            } else {
                Timber.d("FileCheckWorker: Powiadomienie o pobraniu było już wcześniej wyświetlone - pomijam")
            }

            // Zapisz, że prezent został odebrany i przy okazji zapisz nazwę pliku
            prefs.edit {
                putBoolean("gift_received", true).putString(
                        "downloaded_file_name",
                        downloadFileName
                    )
            }
            Timber.d("FileCheckWorker: Zaktualizowano preferencje: gift_received=true, downloaded_file_name=$downloadFileName")

            Timber.d("FileCheckWorker: Plik pobrany pomyślnie: $downloadFileName")
        } else {
            Timber.e("FileCheckWorker: Nieudane pobranie pliku")
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
                Timber.d("FileCheckWorker: Rozpoczynam proces bezpośredniego pobierania pliku dla fileId: $fileId jako: $fileName")

                // Najpierw sprawdź, czy plik już istnieje
                val context = getContext() ?: throw IllegalStateException("Brak kontekstu")

                if (FileUtils.isFileInPublicDownloads(fileName)) {
                    Timber.d("FileCheckWorker: Plik $fileName już istnieje w folderze Pobrane - używam istniejącego pliku")
                    return@withContext true
                }
                Timber.d("FileCheckWorker: Plik nie istnieje lokalnie, będzie pobrany z Drive")

                // Pobierz zawartość pliku z Google Drive
                Timber.d("FileCheckWorker: Pobieranie zawartości pliku z Google Drive")
                val inputStream = client.downloadFile(fileId)
                Timber.d("FileCheckWorker: Pomyślnie uzyskano strumień wejściowy z API Drive")

                var uri: Uri? = null

                // Różne podejścia do zapisywania pliku w zależności od wersji Androida
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Timber.d("FileCheckWorker: Używam podejścia MediaStore dla Android 10+")
                    // Android 10+ używa MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    Timber.d("FileCheckWorker: Przygotowano ContentValues dla MediaStore")

                    val resolver = context.contentResolver
                    uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    Timber.d("FileCheckWorker: Uzyskano URI z MediaStore: $uri")

                    if (uri != null) {
                        Timber.d("FileCheckWorker: Otwieram strumień wyjściowy do zapisu danych pliku")
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            inputStream.use { input ->
                                val bytesCopied = input.copyTo(outputStream)
                                Timber.d("FileCheckWorker: Skopiowano $bytesCopied bajtów do strumienia wyjściowego")
                            }
                        }

                        // Zakończ transakcję
                        Timber.d("FileCheckWorker: Kończę transakcję MediaStore")
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        Timber.d("FileCheckWorker: Plik zapisany przez MediaStore: $uri")
                        return@withContext true
                    } else {
                        Timber.e("FileCheckWorker: Nie można utworzyć URI dla pliku")
                        return@withContext false
                    }
                } else {
                    // Dla starszych wersji Androida, korzystamy z External Storage
                    Timber.d("FileCheckWorker: Używam podejścia External Storage dla starszych wersji Androida")
                    try {
                        val downloadsDir =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (!downloadsDir.exists()) {
                            Timber.d("FileCheckWorker: Katalog Pobrane nie istnieje, tworzę go")
                            downloadsDir.mkdirs()
                        }

                        val destinationFile = File(downloadsDir, fileName)
                        Timber.d("FileCheckWorker: Zapisuję plik do ${destinationFile.absolutePath}")

                        java.io.FileOutputStream(destinationFile).use { outputStream ->
                            inputStream.use { input ->
                                val bytesCopied = input.copyTo(outputStream)
                                Timber.d("FileCheckWorker: Skopiowano $bytesCopied bajtów do pliku wyjściowego")
                            }
                        }

                        val fileExists = destinationFile.exists()
                        val fileSize = destinationFile.length()
                        Timber.d("FileCheckWorker: Plik zapisany do ${destinationFile.absolutePath}, istnieje=$fileExists, rozmiar=$fileSize bajtów")
                        return@withContext fileExists && fileSize > 0
                    } catch (e: IOException) {
                        // Jeśli metoda z External Storage nie zadziała, spróbuj użyć MediaStore
                        Timber.w(
                            "FileCheckWorker: Nie udało się zapisać pliku bezpośrednio, próbuję przez MediaStore\n" +
                                    "wyjątek: $e"
                        )

                        // Alternatywne podejście: zapisz we własnym katalogu aplikacji
                        val internalFile = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName
                        )
                        Timber.d("FileCheckWorker: Użycie fallbacku do własnego katalogu aplikacji: ${internalFile.absolutePath}")

                        java.io.FileOutputStream(internalFile).use { outputStream ->
                            inputStream.use { input ->
                                val bytesCopied = input.copyTo(outputStream)
                                Timber.d("FileCheckWorker: Skopiowano $bytesCopied bajtów do wewnętrznego pliku aplikacji")
                            }
                        }

                        val fileExists = internalFile.exists()
                        val fileSize = internalFile.length()
                        Timber.d("FileCheckWorker: Plik zapisany do katalogu aplikacji: ${internalFile.absolutePath}, istnieje=$fileExists, rozmiar=$fileSize bajtów")
                        return@withContext fileExists && fileSize > 0
                    }
                }
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "FileCheckWorker: Błąd podczas bezpośredniego pobierania pliku do folderu Pobrane: ${e.message}"
                )
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
            val timeDiff = currentTime - lastCheckTime

            // Jeśli od ostatniego sprawdzenia minęło mniej niż 2 sekundy, pomijamy
            val canCheck = timeDiff > 2000
            Timber.d("FileCheckWorker: Sprawdzam czy można uruchomić sprawdzenie pliku - ostatnie sprawdzenie ${timeDiff}ms temu, canCheck=$canCheck")
            return canCheck
        }

        /**
         * Planuje jednorazowe sprawdzenie pliku z ochroną przed duplikatami.
         *
         * @param context Kontekst aplikacji
         * @param forceDownload Czy wymusić pobieranie pliku
         */
        fun planOneTimeCheck(context: Context, forceDownload: Boolean = false) {
            Timber.d("FileCheckWorker: Planowanie jednorazowego sprawdzenia pliku (forceDownload=$forceDownload)")

            // Sprawdź, czy można wykonać sprawdzenie (nie za często)
            if (!canCheckFile(context)) {
                Timber.d("FileCheckWorker: Zbyt częste próby sprawdzenia pliku - pomijam")
                return
            }

            // Zbuduj żądanie jednorazowego sprawdzenia
            val request = createOneTimeCheckRequest(forceDownload).build()
            Timber.d("FileCheckWorker: Utworzono żądanie jednorazowego sprawdzenia")

            // Użyj REPLACE aby zastąpić inne oczekujące zadania
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
            Timber.d("FileCheckWorker: Dodano zadanie jednorazowego sprawdzenia do kolejki WorkManager z polityką REPLACE")

            // Aktualizuj czas ostatniego sprawdzenia
            context.getSharedPreferences("file_check_prefs", Context.MODE_PRIVATE).edit {
                putLong("last_check_time", System.currentTimeMillis())
            }
            Timber.d("FileCheckWorker: Zaktualizowano czas ostatniego sprawdzenia w preferencjach")

            Timber.d("FileCheckWorker: Zaplanowano jednorazowe sprawdzenie pliku (forceDownload=$forceDownload)")
        }
    }
}