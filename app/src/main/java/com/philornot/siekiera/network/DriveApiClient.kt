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
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLException

/**
 * Enhanced DriveApiClient with improved network error handling and diagnostics.
 *
 * Handles various network scenarios:
 * - No internet connection
 * - DNS resolution failures
 * - SSL/TLS errors
 * - Connection timeouts
 * - Google API server errors
 *
 * Provides intelligent retry mechanisms with exponential backoff.
 */
class DriveApiClient(context: Context) {
    // Używamy WeakReference aby uniknąć memory leak
    private val contextRef = WeakReference(context.applicationContext)
    private var driveService: Drive? = null

    // Przechowuje czas ostatniej inicjalizacji
    private var lastInitTime = 0L

    // Limity ponownych prób
    private var retryCount = 0

    // Network diagnostics
    private var lastNetworkError: NetworkErrorInfo? = null

    /**
     * Enhanced network error classification for better diagnostics
     */
    data class NetworkErrorInfo(
        val errorType: NetworkErrorType,
        val originalException: Exception,
        val timestamp: Long = System.currentTimeMillis(),
        val retryAttempt: Int = 0
    )

    enum class NetworkErrorType {
        NO_INTERNET,           // UnknownHostException, ConnectException
        DNS_RESOLUTION_FAILED, // Specific DNS issues
        SSL_ERROR,             // SSL/TLS certificate issues
        TIMEOUT,               // Connection/read timeouts
        AUTH_ERROR,            // Google Auth specific errors
        RATE_LIMITED,          // API rate limiting (429)
        SERVER_ERROR,          // Google server errors (5xx)
        UNKNOWN_NETWORK_ERROR  // Other network issues
    }

    // Funkcja pomocnicza do uzyskania kontekstu
    private fun getContext(): Context? = contextRef.get()

    // Getter dla AppConfig - nie przechowujemy referencji, tylko pobieramy w razie potrzeby
    private fun getAppConfig(): AppConfig? {
        val context = getContext() ?: return null
        return AppConfig.getInstance(context)
    }

    /**
     * Classifies network exceptions for better error handling
     */
    private fun classifyNetworkError(exception: Exception): NetworkErrorType {
        return when {
            exception is UnknownHostException -> {
                val message = exception.message?.lowercase() ?: ""
                when {
                    message.contains("oauth2.googleapis.com") ||
                            message.contains("www.googleapis.com") -> NetworkErrorType.DNS_RESOLUTION_FAILED
                    else -> NetworkErrorType.NO_INTERNET
                }
            }
            exception is ConnectException -> NetworkErrorType.NO_INTERNET
            exception is SocketTimeoutException -> NetworkErrorType.TIMEOUT
            exception is SSLException -> NetworkErrorType.SSL_ERROR
            exception.message?.contains("401") == true ||
                    exception.message?.contains("unauthorized") == true -> NetworkErrorType.AUTH_ERROR
            exception.message?.contains("429") == true ||
                    exception.message?.contains("rate limit") == true -> NetworkErrorType.RATE_LIMITED
            exception.message?.contains("50") == true -> NetworkErrorType.SERVER_ERROR
            else -> NetworkErrorType.UNKNOWN_NETWORK_ERROR
        }
    }

    /**
     * Enhanced error logging with network diagnostics
     */
    private fun logNetworkError(operation: String, exception: Exception, attempt: Int = 0) {
        val errorType = classifyNetworkError(exception)
        val errorInfo = NetworkErrorInfo(errorType, exception, retryAttempt = attempt)
        lastNetworkError = errorInfo

        Timber.w("Network error in $operation (attempt $attempt):")
        Timber.w("  Error type: $errorType")
        Timber.w("  Exception: ${exception.javaClass.simpleName}")
        Timber.w("  Message: ${exception.message}")

        // Add specific diagnostics based on error type
        when (errorType) {
            NetworkErrorType.DNS_RESOLUTION_FAILED -> {
                Timber.w("  Diagnosis: DNS cannot resolve Google API hostnames")
                Timber.w("  Possible causes: Network connectivity issues, DNS server problems, firewall blocking")
            }
            NetworkErrorType.NO_INTERNET -> {
                Timber.w("  Diagnosis: No internet connectivity")
                Timber.w("  Possible causes: WiFi/mobile data disabled, airplane mode, network outage")
            }
            NetworkErrorType.SSL_ERROR -> {
                Timber.w("  Diagnosis: SSL/TLS certificate issues")
                Timber.w("  Possible causes: Outdated system certificates, man-in-the-middle blocking")
            }
            NetworkErrorType.AUTH_ERROR -> {
                Timber.w("  Diagnosis: Google authentication failed")
                Timber.w("  Possible causes: Invalid service account, expired token, permissions")
            }
            NetworkErrorType.RATE_LIMITED -> {
                Timber.w("  Diagnosis: API rate limit exceeded")
                Timber.w("  Recommended action: Implement longer delays between requests")
            }
            NetworkErrorType.SERVER_ERROR -> {
                Timber.w("  Diagnosis: Google server error")
                Timber.w("  Recommended action: Retry with exponential backoff")
            }
            else -> {
                Timber.w("  Diagnosis: Unknown network issue")
            }
        }
    }

    /**
     * Checks if we should retry based on error type and attempt count
     */
    private fun shouldRetryForNetworkError(errorType: NetworkErrorType, attempt: Int): Boolean {
        if (attempt >= MAX_RETRY_COUNT) return false

        return when (errorType) {
            NetworkErrorType.NO_INTERNET,
            NetworkErrorType.DNS_RESOLUTION_FAILED -> attempt < 2 // Limited retries for connectivity issues
            NetworkErrorType.TIMEOUT,
            NetworkErrorType.SERVER_ERROR -> true // Always retry these
            NetworkErrorType.RATE_LIMITED -> true // Retry with longer delays
            NetworkErrorType.SSL_ERROR,
            NetworkErrorType.AUTH_ERROR -> attempt < 1 // One retry only
            NetworkErrorType.UNKNOWN_NETWORK_ERROR -> attempt < 2
        }
    }

    /**
     * Calculates retry delay based on error type
     */
    private fun getRetryDelay(errorType: NetworkErrorType, attempt: Int): Long {
        val baseDelay = when (errorType) {
            NetworkErrorType.RATE_LIMITED -> 60_000L // 1 minute for rate limiting
            NetworkErrorType.SERVER_ERROR -> 5_000L   // 5 seconds for server errors
            NetworkErrorType.TIMEOUT -> 3_000L        // 3 seconds for timeouts
            NetworkErrorType.DNS_RESOLUTION_FAILED,
            NetworkErrorType.NO_INTERNET -> 10_000L   // 10 seconds for connectivity
            else -> 2_000L                            // 2 seconds default
        }

        // Exponential backoff with jitter
        val exponentialDelay = baseDelay * (1 shl attempt)
        val jitter = (Math.random() * 1000).toLong()

        return (exponentialDelay + jitter).coerceAtMost(MAX_BACKOFF_DELAY_MS)
    }

    /**
     * Inicjalizuje klienta Google Drive API z ulepszną obsługą błędów sieciowych.
     */
    @SuppressLint("DiscouragedApi")
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = getContext() ?: return@withContext false
            val appConfig = getAppConfig() ?: return@withContext false

            // Sprawdź, czy serwis jest już zainicjalizowany i czy token nie jest przeterminowany
            if (driveService != null) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastInitTime < TimeUnit.MINUTES.toMillis(50)) {
                    return@withContext true
                }
                Timber.d("Token może niedługo wygasnąć - odświeżam połączenie")
            }

            // Reset retry count for new initialization
            retryCount = 0

            // Konfiguracja transportu HTTP z ulepszeniami
            val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

            // Pobierz nazwę pliku service account z konfiguracji
            val serviceAccountFileName = appConfig.getServiceAccountFileName()

            // Pobierz poświadczenia z zasobów aplikacji
            val serviceAccountStream = try {
                val resourceField = R.raw::class.java.getField(serviceAccountFileName)
                val resourceId = resourceField.getInt(null)
                context.resources.openRawResource(resourceId)
            } catch (e: Exception) {
                Timber.w("Nie znaleziono bezpośredniego identyfikatora zasobu, używam getIdentifier")
                val resourceId = context.resources.getIdentifier(
                    serviceAccountFileName, "raw", context.packageName
                )
                if (resourceId == 0) {
                    throw IllegalStateException("Service account file not found: $serviceAccountFileName")
                }
                context.resources.openRawResource(resourceId)
            }

            // Skonfiguruj poświadczenia konta usługi
            val credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                .createScoped(listOf(DriveScopes.DRIVE_READONLY))

            // Utwórz HttpRequestInitializer z ulepszoną obsługą błędów
            val requestInitializer = HttpCredentialsAdapter(credentials)

            // Utwórz usługę Drive API
            driveService = Drive.Builder(
                httpTransport, jsonFactory, createEnhancedRequestInitializer(requestInitializer)
            ).setApplicationName(APPLICATION_NAME).build()

            // Zapamiętaj czas inicjalizacji
            lastInitTime = System.currentTimeMillis()
            lastNetworkError = null // Clear previous errors on successful init

            Timber.d("DriveApiClient successfully initialized")
            true
        } catch (e: Exception) {
            logNetworkError("initialize", e)
            false
        }
    }

    /**
     * Creates enhanced HttpRequestInitializer with better error handling
     */
    private fun createEnhancedRequestInitializer(delegate: HttpRequestInitializer): HttpRequestInitializer {
        return HttpRequestInitializer { request ->
            delegate.initialize(request)

            // Enhanced timeouts
            request.connectTimeout = 30000 // 30 seconds
            request.readTimeout = 45000    // 45 seconds (longer for large files)

            // Enhanced retry logic
            request.numberOfRetries = 3
            request.ioExceptionHandler = HttpIOExceptionHandler { httpRequest: HttpRequest, supportsRetry: Boolean ->
                val currentTime = System.currentTimeMillis()
                Timber.w("HTTP IOException in request to ${httpRequest.url}")
                Timber.w("  SupportsRetry: $supportsRetry")
                Timber.w("  Timestamp: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(currentTime))}")

                // Always attempt retry for network errors if supported
                supportsRetry
            }

            // Enhanced headers
            request.headers.set("Accept", "application/json")
            request.headers.set("User-Agent", "$APPLICATION_NAME (Android)")
        }
    }

    /**
     * Sprawdza limity API z ulepszonym zarządzaniem częstotliwością.
     */
    private suspend fun checkApiLimits() {
        val currentTime = System.currentTimeMillis()

        // Reset licznika co 5 minut
        if (currentTime - lastMinuteReset.get() > RESET_INTERVAL_MS) {
            val previousCount = requestCount.getAndSet(0)
            lastMinuteReset.set(currentTime)
            if (previousCount > 0) {
                Timber.d("API request counter reset (previous: $previousCount requests)")
            }
            return
        }

        // Sprawdź czy nie przekraczamy limitu
        val currentCount = requestCount.get()
        if (currentCount >= MAX_REQUESTS_PER_INTERVAL) {
            val waitTime = RESET_INTERVAL_MS - (currentTime - lastMinuteReset.get())
            if (waitTime > 0) {
                Timber.w("API rate limit reached ($currentCount requests) - waiting ${waitTime}ms")
                delay(waitTime)
                requestCount.set(0)
                lastMinuteReset.set(System.currentTimeMillis())
            }
        }

        val newCount = requestCount.incrementAndGet()
        if (newCount % 10 == 0) { // Log every 10th request to avoid spam
            Timber.d("API request count: $newCount")
        }
    }

    /**
     * Enhanced retry logic with network-aware error handling
     */
    private suspend fun <T> executeWithRetry(
        operation: String,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT + 1) { attempt ->
            try {
                checkApiLimits()
                val result = block()

                if (attempt > 0) {
                    Timber.d("Operation '$operation' succeeded after $attempt retries")
                }

                // Reset retry count on success
                retryCount = 0
                return result

            } catch (e: Exception) {
                lastException = e
                logNetworkError(operation, e, attempt)

                val errorType = classifyNetworkError(e)

                if (attempt < MAX_RETRY_COUNT && shouldRetryForNetworkError(errorType, attempt)) {
                    val delay = getRetryDelay(errorType, attempt)
                    Timber.d("Retrying '$operation' in ${delay}ms (attempt ${attempt + 1}/$MAX_RETRY_COUNT)")
                    delay(delay)
                    retryCount = attempt + 1
                } else {
                    Timber.e("Operation '$operation' failed after $attempt attempts")
                    throw lastException
                }
            }
        }

        throw lastException ?: RuntimeException("Operation failed with unknown error")
    }

    /**
     * Pobiera informacje o pliku z Google Drive z ulepszną obsługą błędów.
     */
    suspend fun getFileInfo(fileId: String): FileInfo = executeWithRetry("getFileInfo") {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Drive API client not initialized")

        Timber.d("Fetching file info for: $fileId")

        val file = driveService.files().get(fileId)
            .setFields("id, name, mimeType, size, modifiedTime")
            .execute()

        val fileSize = file.size.toLong()

        FileInfo(
            id = file.id,
            name = file.name,
            mimeType = file.mimeType,
            size = fileSize,
            modifiedTime = parseRfc3339Date(file.modifiedTime.toStringRfc3339())
        )
    }

    /**
     * Pobiera zawartość pliku z Google Drive z ulepszną obsługą błędów.
     */
    suspend fun downloadFile(fileId: String): InputStream = executeWithRetry("downloadFile") {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Drive API client not initialized")

        Timber.d("Downloading file: $fileId")

        val outputStream = java.io.ByteArrayOutputStream()
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

        ByteArrayInputStream(outputStream.toByteArray())
    }

    /**
     * Sprawdza pliki w folderze z ulepszną obsługą błędów.
     */
    suspend fun listFilesInFolder(folderId: String): List<FileInfo> = executeWithRetry("listFilesInFolder") {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Drive API client not initialized")

        Timber.d("Listing files in folder: $folderId")

        val query = "'$folderId' in parents and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setFields("files(id, name, mimeType, size, modifiedTime)")
            .execute()

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
    }

    /**
     * Provides diagnostic information about the last network error
     */
    fun getLastNetworkErrorInfo(): NetworkErrorInfo? = lastNetworkError

    /**
     * Checks if the client is in a good state for API calls
     */
    fun isHealthy(): Boolean {
        val hasValidService = driveService != null
        val tokenNotExpired = System.currentTimeMillis() - lastInitTime < TimeUnit.MINUTES.toMillis(50)
        val noRecentCriticalErrors = lastNetworkError?.let { error ->
            val isRecent = System.currentTimeMillis() - error.timestamp < TimeUnit.MINUTES.toMillis(5)
            val isCritical = error.errorType in setOf(
                NetworkErrorType.AUTH_ERROR,
                NetworkErrorType.SSL_ERROR
            )
            !(isRecent && isCritical)
        } != false

        return hasValidService && tokenNotExpired && noRecentCriticalErrors
    }

    /** Parsuje datę w formacie RFC 3339 używanym przez Google API. */
    private fun parseRfc3339Date(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return try {
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Timber.e(e, "Error parsing date: $dateString")
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

        // Enhanced retry settings
        private const val MAX_RETRY_COUNT = 4 // Increased from 3
        private const val MAX_BACKOFF_DELAY_MS = 60_000L // Increased to 60 seconds

        // Do testów - pozwala na wstrzyknięcie mocka
        @JvmStatic
        internal var mockInstance: DriveApiClient? = null

        // Statyczne liczniki do monitorowania limitów API
        @Volatile
        private var requestCount = AtomicInteger(0)

        @Volatile
        private var lastMinuteReset = AtomicLong(System.currentTimeMillis())

        // API rate limiting settings
        private const val RESET_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val MAX_REQUESTS_PER_INTERVAL = 35 // Reduced to be more conservative

        /**
         * Pobiera instancję klienta Drive API z diagnostyką zdrowia.
         */
        @JvmStatic
        fun getInstance(context: Context): DriveApiClient {
            return mockInstance ?: synchronized(this) {
                mockInstance ?: DriveApiClient(context.applicationContext).also {
                    mockInstance = it
                }
            }
        }

        // Dla testów - metoda do czyszczenia mocka
        @JvmStatic
        fun clearMockInstance() {
            mockInstance = null
        }
    }
}