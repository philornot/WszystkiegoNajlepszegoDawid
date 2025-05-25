package com.philornot.siekiera.network

import android.annotation.SuppressLint
import android.content.Context
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpIOExceptionHandler
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.philornot.siekiera.R
import com.philornot.siekiera.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Klient API do komunikacji z Google Drive używający konta usługi (Service
 * Account).
 *
 * Nie wymaga interakcji użytkownika ani logowania - używa predefiniowanych
 * poświadczeń konta usługi, które ma dostęp do określonego folderu Google
 * Drive.
 *
 * Mechanizmy auto-refresh tokenu i ponownych prób.
 */
class DriveApiClient(context: Context) {
    // Używamy WeakReference aby uniknąć memory leak
    private val contextRef = WeakReference(context.applicationContext)
    private var driveService: Drive? = null

    // Przechowuje czas ostatniej inicjalizacji
    private var lastInitTime = 0L

    // Limity ponownych prób
    private var retryCount = 0

    // Funkcja pomocnicza do uzyskania kontekstu
    private fun getContext(): Context? = contextRef.get()

    // Getter dla AppConfig - nie przechowujemy referencji, tylko pobieramy w razie potrzeby
    private fun getAppConfig(): AppConfig? {
        val context = getContext() ?: return null
        return AppConfig.getInstance(context)
    }

    /**
     * Inicjalizuje klienta Google Drive API. Musi być wywołane przed innymi
     * metodami.
     *
     * @return true jeśli inicjalizacja się powiodła, false w przeciwnym
     *    wypadku
     */
    @SuppressLint("DiscouragedApi")
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = getContext() ?: return@withContext false
            val appConfig = getAppConfig() ?: return@withContext false

            // Sprawdź, czy serwis jest już zainicjalizowany i czy token nie jest przeterminowany
            if (driveService != null) {
                // Sprawdź, czy nie minęło więcej niż 50 minut od ostatniej inicjalizacji
                // (tokeny Google wygasają po godzinie, więc odświeżamy proaktywnie)
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastInitTime < TimeUnit.MINUTES.toMillis(50)) {
                    return@withContext true
                }

                Timber.d("Token może niedługo wygasnąć - odświeżam połączenie")
            }

            // Konfiguracja transportu HTTP
            val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

            // Pobierz nazwę pliku service account z konfiguracji
            val serviceAccountFileName = appConfig.getServiceAccountFileName()

            // Pobierz poświadczenia z zasobów aplikacji
            // Użyj bezpośredniego dostępu do zasobu, jeśli to możliwe
            val serviceAccountStream = try {
                // Próba użycia bezpośredniego identyfikatora zasobu
                val resourceField = R.raw::class.java.getField(serviceAccountFileName)
                val resourceId = resourceField.getInt(null)
                context.resources.openRawResource(resourceId)
            } catch (_: Exception) {
                // Fallback do metody getIdentifier w przypadku błędu
                Timber.w("Nie znaleziono bezpośredniego identyfikatora zasobu, używam getIdentifier")
                val resourceId = context.resources.getIdentifier(
                    serviceAccountFileName, "raw", context.packageName
                )
                context.resources.openRawResource(resourceId)
            }

            // Skonfiguruj poświadczenia konta usługi
            val credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                .createScoped(listOf(DriveScopes.DRIVE_READONLY))

            // Utwórz HttpRequestInitializer z obsługą ponownych prób
            val requestInitializer = HttpCredentialsAdapter(credentials)

            // Utwórz usługę Drive API
            driveService = Drive.Builder(
                httpTransport, jsonFactory, createRequestInitializerWithRetry(requestInitializer)
            ).setApplicationName(APPLICATION_NAME).build()

            // Zapamiętaj czas inicjalizacji
            lastInitTime = System.currentTimeMillis()

            true
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas inicjalizacji klienta Drive API")
            false
        }
    }

    /**
     * Tworzy HttpRequestInitializer z obsługą ponownych prób. Pozwala na
     * automatyczne ponowienie żądania w przypadku tymczasowych błędów.
     */
    private fun createRequestInitializerWithRetry(delegate: HttpRequestInitializer): HttpRequestInitializer {
        return HttpRequestInitializer { request ->
            delegate.initialize(request)

            // Ustaw limity prób
            request.connectTimeout = 30000 // 30 sekund
            request.readTimeout = 30000 // 30 sekund

            // Ustaw obsługę ponownych prób
            request.numberOfRetries = 3
            request.ioExceptionHandler =
                HttpIOExceptionHandler { _: HttpRequest, supportsRetry: Boolean ->
                    Timber.w("IOException w żądaniu Drive API, supportsRetry=%b", supportsRetry)
                    // Zawsze próbuj ponownie dla błędów sieciowych
                    supportsRetry
                }

            // Dodaj dodatkowe nagłówki, jeśli potrzeba
            request.headers.set("Accept", "application/json")
        }
    }

    /**
     * Sprawdza limity API i opóźnia żądanie jeśli to konieczne. Zapobiega
     * przekroczeniu limitów API Google Drive.
     */
    private suspend fun checkApiLimits() {
        // Używamy statycznych liczników aby zapewnić współdzielenie między instancjami
        val currentTime = System.currentTimeMillis()

        // Reset licznika co 5 minut zamiast co 1 minutę
        if (currentTime - lastMinuteReset.get() > RESET_INTERVAL_MS) {
            Timber.d("Resetuję licznik żądań API (było: ${requestCount.get()})")
            requestCount.set(0)
            lastMinuteReset.set(currentTime)
            return
        }

        // Sprawdź czy nie przekraczamy limitu
        if (requestCount.get() >= MAX_REQUESTS_PER_INTERVAL) {
            // Poczekaj do następnego okresu resetowania
            val waitTime = RESET_INTERVAL_MS - (currentTime - lastMinuteReset.get())
            if (waitTime > 0) {
                Timber.w("Osiągnięto limit API (${requestCount.get()}) - opóźnienie kolejnego żądania o $waitTime ms")
                delay(waitTime)
                requestCount.set(0)
                lastMinuteReset.set(System.currentTimeMillis())
            }
        }

        // Zwiększ licznik zapytań
        val newCount = requestCount.incrementAndGet()
        Timber.d("API request count: $newCount")
    }

    /**
     * Pobiera informacje o pliku z Google Drive.
     *
     * @param fileId ID pliku na Google Drive
     * @return Informacje o pliku
     * @throws Exception jeśli wystąpi błąd
     */
    suspend fun getFileInfo(fileId: String): FileInfo = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            // Sprawdź limity API przed wykonaniem żądania
            checkApiLimits()

            Timber.d("Pobieranie informacji o pliku o ID: $fileId")

            val file =
                driveService.files().get(fileId).setFields("id, name, mimeType, size, modifiedTime")
                    .execute()

            // Bezpieczne konwertowanie rozmiaru pliku
            @Suppress("UNNECESSARY_SAFE_CALL") val fileSize = file.size?.toLong() ?: 0L

            // Resetuj licznik ponownych prób po sukcesie
            retryCount = 0

            FileInfo(
                id = file.id,
                name = file.name,
                mimeType = file.mimeType,
                size = fileSize,
                modifiedTime = parseRfc3339Date(file.modifiedTime.toStringRfc3339())
            )
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas pobierania informacji o pliku: $fileId")

            // Obsługa ponownych prób z mechanizmem backoff
            if (shouldRetry(e)) {
                Timber.d("Ponawiam operację getFileInfo po błędzie...")
                return@withContext getFileInfo(fileId)
            }

            throw e
        }
    }


    /**
     * Pobiera zawartość pliku z Google Drive.
     *
     * @param fileId ID pliku na Google Drive
     * @return Strumień z zawartością pliku
     * @throws Exception jeśli wystąpi błąd
     */
    suspend fun downloadFile(fileId: String): InputStream = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            // Sprawdź limity API przed wykonaniem żądania
            checkApiLimits()

            Timber.d("Pobieranie pliku o ID: $fileId")

            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            // Resetuj licznik ponownych prób po sukcesie
            retryCount = 0

            ByteArrayInputStream(outputStream.toByteArray())
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas pobierania pliku: $fileId")

            // Obsługa ponownych prób z mechanizmem backoff
            if (shouldRetry(e)) {
                Timber.d("Ponawiam operację downloadFile po błędzie...")
                return@withContext downloadFile(fileId)
            }

            throw e
        }
    }

    /**
     * Sprawdza pliki w określonym folderze Google Drive.
     *
     * @param folderId ID folderu na Google Drive
     * @return Lista informacji o plikach w folderze
     * @throws Exception jeśli wystąpi błąd
     */
    suspend fun listFilesInFolder(folderId: String): List<FileInfo> = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            // Sprawdź limity API przed wykonaniem żądania
            checkApiLimits()

            Timber.d("Listowanie plików w folderze: $folderId")

            // Zapytanie o pliki w określonym folderze
            val query = "'$folderId' in parents and trashed = false"
            val result = driveService.files().list().setQ(query)
                .setFields("files(id, name, mimeType, size, modifiedTime)").execute()

            // Resetuj licznik ponownych prób po sukcesie
            retryCount = 0

            result.files.map { file ->
                // Bezpieczne konwertowanie rozmiaru pliku
                @Suppress("UNNECESSARY_SAFE_CALL") val fileSize = file.size?.toLong() ?: 0L

                FileInfo(
                    id = file.id,
                    name = file.name,
                    mimeType = file.mimeType,
                    size = fileSize,
                    modifiedTime = parseRfc3339Date(file.modifiedTime.toStringRfc3339())
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas listowania plików w folderze: $folderId")

            // Obsługa ponownych prób z mechanizmem backoff
            if (shouldRetry(e)) {
                Timber.d("Ponawiam operację listFilesInFolder po błędzie...")
                return@withContext listFilesInFolder(folderId)
            }

            throw e
        }
    }

    /**
     * Określa, czy operacja powinna być ponowiona po wystąpieniu błędu.
     * Implementuje mechanizm exponential backoff.
     *
     * @param exception Wyjątek, który wystąpił
     * @return true jeśli operacja powinna być ponowiona, false w przeciwnym
     *    wypadku
     */
    private suspend fun shouldRetry(exception: Exception): Boolean {
        // Sprawdź limit ponownych prób
        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.d("Osiągnięto maksymalną liczbę ponownych prób: $retryCount")
            return false
        }

        // Sprawdź, czy token nie wygasł - jeśli tak, spróbuj zainicjalizować ponownie
        val exceptionMessage = exception.message?.lowercase() ?: ""
        if (exceptionMessage.contains("token") && exceptionMessage.contains("expire")) {
            Timber.d("Token wygasł, ponowna inicjalizacja...")
            val initialized = initialize()
            if (!initialized) {
                Timber.d("Nie udało się ponownie zainicjalizować klienta po wygaśnięciu tokenu")
                return false
            }

            // Zwiększ licznik ponownych prób
            retryCount++
            return true
        }

        // Sprawdź, czy to błąd sieciowy lub inny tymczasowy błąd
        val isTransientError =
            exceptionMessage.contains("timeout") || exceptionMessage.contains("refused") || exceptionMessage.contains(
                "reset"
            ) || exceptionMessage.contains("unavailable") || exceptionMessage.contains("rate limit") || exceptionMessage.contains(
                "429"
            ) || exceptionMessage.contains("500") || exceptionMessage.contains("503")

        if (isTransientError) {
            // Zwiększ licznik ponownych prób
            retryCount++

            // Oblicz opóźnienie z wykładniczym wzrostem (1s, 2s, 4s, ...)
            val delayMs = (1000L * (1 shl (retryCount - 1))).coerceAtMost(MAX_BACKOFF_DELAY_MS)
            Timber.d("Czekam $delayMs ms przed ponowieniem próby (próba $retryCount z $MAX_RETRY_COUNT)")

            // Zaczekaj przed ponowieniem próby
            delay(delayMs)
            return true
        }

        // Inne błędy - nie ponawiamy
        return false
    }

    /** Parsuje datę w formacie RFC 3339 używanym przez Google API. */
    private fun parseRfc3339Date(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return try {
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Timber.e(e, "Błąd parsowania daty: $dateString")
            Date()
        }
    }

    /** Klasa reprezentująca informacje o pliku. */
    data class FileInfo(
        val id: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val modifiedTime: Date,
    )

    companion object {
        // Nazwa aplikacji, która będzie widoczna w logach Google API
        private const val APPLICATION_NAME = "Wszystkiego Najlepszego Dawid"

        // Ustawienia mechanizmu ponawiania
        private const val MAX_RETRY_COUNT = 3
        private const val MAX_BACKOFF_DELAY_MS = 30_000L // 30 sekund maksymalnego opóźnienia

        // Do testów - pozwala na wstrzyknięcie mocka
        @JvmStatic
        internal var mockInstance: DriveApiClient? = null

        // Statyczne liczniki do monitorowania limitów API - użycie AtomicInteger dla bezpieczeństwa wątków
        @Volatile
        private var requestCount = AtomicInteger(0)

        @Volatile
        private var lastMinuteReset = AtomicLong(System.currentTimeMillis())

        // Zmienione: Interwał resetowania licznika (5 minut zamiast 1 minuty)
        private const val RESET_INTERVAL_MS = 5 * 60 * 1000L // 5 minut

        // Zmienione: Zwiększony limit żądań na interwał
        private const val MAX_REQUESTS_PER_INTERVAL = 40  // Limit na 5-minutowy interwał

        /**
         * Pobiera instancję klienta Drive API. W testach zwraca zaślepkę, w
         * produkcji tworzy nową instancję.
         *
         * @param context Kontekst aplikacji
         * @return Instancja DriveApiClient
         */
        @JvmStatic
        fun getInstance(context: Context): DriveApiClient {
            return mockInstance ?: synchronized(this) {
                // Tworzymy nową instancję jeśli mockInstance jest null
                mockInstance ?: DriveApiClient(context.applicationContext)
            }
        }

        // Dla testów - metoda do czyszczenia mocka
        @JvmStatic
        fun clearMockInstance() {
            mockInstance = null
        }
    }
}