package com.philornot.siekiera.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Ulepszona wersja klienta API Google Drive z lepszą obsługą błędów
 * sieciowych i mechanizmami sprawdzania połączenia internetowego.
 *
 * Dodano:
 * - Sprawdzanie połączenia internetowego przed operacjami
 * - Lepszą obsługę błędów DNS i sieciowych
 * - Szczegółowe komunikaty o błędach dla różnych scenariuszy
 * - Mechanizmy retry z inteligentnym backoff
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

    // Getter dla AppConfig
    private fun getAppConfig(): AppConfig? {
        val context = getContext() ?: return null
        return AppConfig.getInstance(context)
    }

    /**
     * Sprawdza czy urządzenie ma dostęp do internetu. Używa nowoczesnego API
     * NetworkCapabilities dla Android 6.0+
     */
    private fun isNetworkAvailable(): Boolean {
        val context = getContext() ?: return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Timber.d("Połączenie Wi-Fi dostępne")
                    true
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Timber.d("Połączenie komórkowe dostępne")
                    true
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Timber.d("Połączenie Ethernet dostępne")
                    true
                }
                else -> {
                    Timber.d("Nieznany typ połączenia internetowego")
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Błąd podczas sprawdzania połączenia internetowego")
            false
        }
    }

    /** Sprawdza czy błąd jest związany z problemami sieciowymi/DNS. */
    private fun isNetworkError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        val isNetworkError = when {
            exception is UnknownHostException -> true
            message.contains("no address associated with hostname") -> true
            message.contains("unable to resolve host") -> true
            message.contains("network is unreachable") -> true
            message.contains("connection refused") -> true
            message.contains("connection timed out") -> true
            message.contains("eai_nodata") -> true
            message.contains("eai_again") -> true
            message.contains("oauth2.googleapis.com") -> true
            message.contains("www.googleapis.com") -> true
            else -> false
        }

        if (isNetworkError) {
            Timber.w("Wykryto błąd sieciowy: ${exception.javaClass.simpleName}: $message")
        }

        return isNetworkError
    }

    /** Tworzy szczegółowy komunikat o błędzie na podstawie typu wyjątku. */
    private fun createDetailedErrorMessage(exception: Exception): String {
        val message = exception.message?.lowercase() ?: ""

        return when {
            !isNetworkAvailable() -> {
                "Brak połączenia z internetem. Sprawdź połączenie Wi-Fi lub mobilne i spróbuj ponownie."
            }

            message.contains("oauth2.googleapis.com") -> {
                "Nie można połączyć się z serwerami uwierzytelniania Google. Sprawdź czy Google Services są dostępne w Twojej sieci."
            }

            message.contains("no address associated with hostname") || message.contains("eai_nodata") -> {
                "Problemy z rozwiązywaniem nazw serwerów. Sprawdź ustawienia DNS lub spróbuj z innej sieci."
            }

            message.contains("unable to resolve host") -> {
                "Nie można znaleźć serwerów Google Drive. Sprawdź połączenie internetowe."
            }

            message.contains("connection refused") -> {
                "Serwery Google Drive odrzuciły połączenie. Spróbuj ponownie za chwilę."
            }

            message.contains("connection timed out") || message.contains("timeout") -> {
                "Przekroczono limit czasu połączenia. Sprawdź stabilność połączenia internetowego."
            }

            else -> {
                "Wystąpił problem z połączeniem: ${exception.message ?: "Nieznany błąd"}"
            }
        }
    }

    /**
     * Sprawdza połączenie internetowe przed wykonaniem operacji API. Rzuca
     * wyjątek z opisowym komunikatem jeśli brak połączenia.
     */
    private suspend fun checkNetworkConnectivity() {
        if (!isNetworkAvailable()) {
            throw NetworkUnavailableException(
                "Brak dostępu do internetu. Sprawdź połączenie Wi-Fi lub mobilne."
            )
        }

        // Dodatkowe sprawdzenie - próba prostego ping do Google DNS
        try {
            withContext(Dispatchers.IO) {
                val runtime = Runtime.getRuntime()
                val process = runtime.exec("ping -c 1 8.8.8.8")
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    Timber.w("Ping do 8.8.8.8 nieudany (kod: $exitCode)")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Nie można wykonać ping - może być ograniczony")
        }
    }

    /**
     * Inicjalizuje klienta Google Drive API z lepszą obsługą błędów
     * sieciowych.
     */
    @SuppressLint("DiscouragedApi")
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = getContext() ?: return@withContext false
            val appConfig = getAppConfig() ?: return@withContext false

            Timber.d("Rozpoczynam inicjalizację DriveApiClient")

            // Sprawdź połączenie internetowe przed inicjalizacją
            checkNetworkConnectivity()

            // Sprawdź, czy serwis jest już zainicjalizowany
            if (driveService != null) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastInitTime < TimeUnit.MINUTES.toMillis(50)) {
                    Timber.d("DriveService już zainicjalizowany i token jest świeży")
                    return@withContext true
                }
                Timber.d("Token może niedługo wygasnąć - odświeżam połączenie")
            }

            // Konfiguracja transportu HTTP z zwiększonymi timeoutami
            val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

            val serviceAccountFileName = appConfig.getServiceAccountFileName()

            // Pobierz poświadczenia z zasobów aplikacji
            val serviceAccountStream = try {
                val resourceField = R.raw::class.java.getField(serviceAccountFileName)
                val resourceId = resourceField.getInt(null)
                context.resources.openRawResource(resourceId)
            } catch (_: Exception) {
                Timber.w("Nie znaleziono bezpośredniego identyfikatora zasobu, używam getIdentifier")
                val resourceId = context.resources.getIdentifier(
                    serviceAccountFileName, "raw", context.packageName
                )
                if (resourceId == 0) {
                    throw IllegalStateException("Nie znaleziono pliku service account: $serviceAccountFileName")
                }
                context.resources.openRawResource(resourceId)
            }

            // Skonfiguruj poświadczenia konta usługi z retry
            val credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                .createScoped(listOf(DriveScopes.DRIVE_READONLY))

            val requestInitializer = HttpCredentialsAdapter(credentials)

            // Utwórz usługę Drive API z ulepszonymi timeoutami
            driveService = Drive.Builder(
                httpTransport, jsonFactory, createRequestInitializerWithRetry(requestInitializer)
            ).setApplicationName(APPLICATION_NAME).build()

            lastInitTime = System.currentTimeMillis()

            Timber.d("DriveApiClient zainicjalizowany pomyślnie")
            true
        } catch (_: NetworkUnavailableException) {
            Timber.w("Inicjalizacja niemożliwa - brak połączenia internetowego")
            false
        } catch (e: Exception) {
            when {
                isNetworkError(e) -> {
                    val detailedMessage = createDetailedErrorMessage(e)
                    Timber.w(e, "Błąd sieciowy podczas inicjalizacji: $detailedMessage")
                }

                else -> {
                    Timber.e(e, "Nieoczekiwany błąd podczas inicjalizacji klienta Drive API")
                }
            }
            false
        }
    }

    /** Tworzy HttpRequestInitializer z ulepszoną obsługą błędów sieciowych. */
    private fun createRequestInitializerWithRetry(delegate: HttpRequestInitializer): HttpRequestInitializer {
        return HttpRequestInitializer { request ->
            delegate.initialize(request)

            // Zwiększone limity czasu dla słabych połączeń
            request.connectTimeout = 45000 // 45 sekund
            request.readTimeout = 60000    // 60 sekund

            // Zwiększona liczba ponownych prób dla błędów sieciowych
            request.numberOfRetries = 5
            request.ioExceptionHandler =
                HttpIOExceptionHandler { httpRequest: HttpRequest, supportsRetry: Boolean ->
                    Timber.w("IOException w żądaniu Drive API, supportsRetry=$supportsRetry")
                    supportsRetry
                }

            // Dodatkowe nagłówki dla lepszej kompatybilności
            request.headers.set("Accept", "application/json")
            request.headers.set("User-Agent", "$APPLICATION_NAME/1.0")
        }
    }

    /** Sprawdza limity API i opóźnia żądanie jeśli to konieczne. */
    private suspend fun checkApiLimits() {
        val currentTime = System.currentTimeMillis()

        // Reset licznika co 5 minut
        if (currentTime - lastMinuteReset.get() > RESET_INTERVAL_MS) {
            Timber.d("Resetuję licznik żądań API (było: ${requestCount.get()})")
            requestCount.set(0)
            lastMinuteReset.set(currentTime)
            return
        }

        // Sprawdź czy nie przekraczamy limitu
        if (requestCount.get() >= MAX_REQUESTS_PER_INTERVAL) {
            val waitTime = RESET_INTERVAL_MS - (currentTime - lastMinuteReset.get())
            if (waitTime > 0) {
                Timber.w("Osiągnięto limit API (${requestCount.get()}) - opóźnienie kolejnego żądania o $waitTime ms")
                delay(waitTime)
                requestCount.set(0)
                lastMinuteReset.set(System.currentTimeMillis())
            }
        }

        val newCount = requestCount.incrementAndGet()
        Timber.d("API request count: $newCount")
    }

    /** Pobiera informacje o pliku z Google Drive z lepszą obsługą błędów. */
    suspend fun getFileInfo(fileId: String): FileInfo = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            // Sprawdź połączenie przed operacją
            checkNetworkConnectivity()

            // Sprawdź limity API przed wykonaniem żądania
            checkApiLimits()

            Timber.d("Pobieranie informacji o pliku o ID: $fileId")

            val file =
                driveService.files().get(fileId).setFields("id, name, mimeType, size, modifiedTime")
                    .execute()

            val fileSize = file.size.toLong()
            retryCount = 0

            FileInfo(
                id = file.id,
                name = file.name,
                mimeType = file.mimeType,
                size = fileSize,
                modifiedTime = parseRfc3339Date(file.modifiedTime.toStringRfc3339())
            )
        } catch (e: Exception) {
            handleApiException(e, "pobierania informacji o pliku: $fileId") {
                getFileInfo(fileId)
            }
        }
    }

    /** Pobiera zawartość pliku z Google Drive z lepszą obsługą błędów. */
    suspend fun downloadFile(fileId: String): InputStream = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            // Sprawdź połączenie przed operacją
            checkNetworkConnectivity()

            checkApiLimits()
            Timber.d("Pobieranie pliku o ID: $fileId")

            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            retryCount = 0
            ByteArrayInputStream(outputStream.toByteArray())
        } catch (e: Exception) {
            handleApiException(e, "pobierania pliku: $fileId") {
                downloadFile(fileId)
            }
        }
    }

    /**
     * Sprawdza pliki w określonym folderze Google Drive z lepszą obsługą
     * błędów.
     */
    suspend fun listFilesInFolder(folderId: String): List<FileInfo> = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            // Sprawdź połączenie przed operacją
            checkNetworkConnectivity()

            checkApiLimits()
            Timber.d("Listowanie plików w folderze: $folderId")

            val query = "'$folderId' in parents and trashed = false"
            val result = driveService.files().list().setQ(query)
                .setFields("files(id, name, mimeType, size, modifiedTime)").execute()

            retryCount = 0

            result.files.map { file ->
                val fileSize = file.size.toLong()
                FileInfo(
                    id = file.id,
                    name = file.name,
                    mimeType = file.mimeType,
                    size = fileSize,
                    modifiedTime = parseRfc3339Date(file.modifiedTime.toStringRfc3339())
                )
            }
        } catch (e: Exception) {
            handleApiException(e, "listowania plików w folderze: $folderId") {
                listFilesInFolder(folderId)
            }
        }
    }

    /** Ujednolicona obsługa wyjątków API z lepszymi komunikatami błędów. */
    private suspend fun <T> handleApiException(
        exception: Exception,
        operation: String,
        retryOperation: suspend () -> T,
    ): T {
        Timber.e(exception, "Błąd podczas $operation")

        when {
            isNetworkError(exception) -> {
                val detailedMessage = createDetailedErrorMessage(exception)

                if (shouldRetry(exception)) {
                    Timber.d("Ponawiam operację $operation po błędzie sieciowym...")
                    return retryOperation()
                } else {
                    throw NetworkException(detailedMessage, exception)
                }
            }

            shouldRetry(exception) -> {
                Timber.d("Ponawiam operację $operation po błędzie...")
                return retryOperation()
            }

            else -> {
                throw exception
            }
        }
    }

    /** Ulepszona logika ponownych prób z lepszą obsługą błędów sieciowych. */
    private suspend fun shouldRetry(exception: Exception): Boolean {
        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.d("Osiągnięto maksymalną liczbę ponownych prób: $retryCount")
            return false
        }

        // Specjalna obsługa błędów sieciowych
        if (isNetworkError(exception)) {
            // Sprawdź ponownie połączenie internetowe
            if (!isNetworkAvailable()) {
                Timber.d("Brak połączenia internetowego - nie ponawiam próby")
                return false
            }

            retryCount++
            val delayMs = (2000L * retryCount).coerceAtMost(MAX_BACKOFF_DELAY_MS)
            Timber.d("Błąd sieciowy - czekam ${delayMs}ms przed ponowieniem próby ($retryCount z $MAX_RETRY_COUNT)")
            delay(delayMs)
            return true
        }

        // Sprawdź, czy token nie wygasł
        val exceptionMessage = exception.message?.lowercase() ?: ""
        if (exceptionMessage.contains("token") && exceptionMessage.contains("expire")) {
            Timber.d("Token wygasł, ponowna inicjalizacja...")
            val initialized = initialize()
            if (!initialized) {
                Timber.d("Nie udało się ponownie zainicjalizować klienta po wygaśnięciu tokenu")
                return false
            }
            retryCount++
            return true
        }

        // Inne błędy tymczasowe
        val isTransientError =
            exceptionMessage.contains("timeout") || exceptionMessage.contains("refused") || exceptionMessage.contains(
                "reset"
            ) || exceptionMessage.contains("unavailable") || exceptionMessage.contains("rate limit") || exceptionMessage.contains(
                "429"
            ) || exceptionMessage.contains("500") || exceptionMessage.contains("503")

        if (isTransientError) {
            retryCount++
            val delayMs = (1000L * (1 shl (retryCount - 1))).coerceAtMost(MAX_BACKOFF_DELAY_MS)
            Timber.d("Czekam $delayMs ms przed ponowieniem próby (próba $retryCount z $MAX_RETRY_COUNT)")
            delay(delayMs)
            return true
        }

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

    /** Wyjątek rzucany gdy brak połączenia internetowego. */
    class NetworkUnavailableException(message: String) : Exception(message)

    /** Wyjątek rzucany przy problemach sieciowych z opisowym komunikatem. */
    class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        private const val APPLICATION_NAME = "Wszystkiego Najlepszego Dawid"
        private const val MAX_RETRY_COUNT = 5 // Zwiększono z 3 do 5
        private const val MAX_BACKOFF_DELAY_MS = 45_000L // Zwiększono do 45 sekund

        // Do testów
        @JvmStatic
        internal var mockInstance: DriveApiClient? = null

        // Statyczne liczniki do monitorowania limitów API
        @Volatile
        private var requestCount = AtomicInteger(0)

        @Volatile
        private var lastMinuteReset = AtomicLong(System.currentTimeMillis())

        private const val RESET_INTERVAL_MS = 5 * 60 * 1000L // 5 minut
        private const val MAX_REQUESTS_PER_INTERVAL = 40

        @JvmStatic
        fun getInstance(context: Context): DriveApiClient {
            return mockInstance ?: synchronized(this) {
                mockInstance ?: DriveApiClient(context.applicationContext)
            }
        }

        @JvmStatic
        fun clearMockInstance() {
            mockInstance = null
        }
    }
}