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
import java.lang.ref.WeakReference
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced DriveApiClient with improved network error handling and
 * initialization.
 *
 * - Checks internet connectivity before initialization
 * - Properly handles network unavailability as expected state
 * - Uses debug logging for expected errors
 * - Implements intelligent retry with exponential backoff
 */
class DriveApiClient(context: Context) {
    // Use WeakReference to avoid memory leaks
    private val contextRef = WeakReference(context.applicationContext)
    private var driveService: Drive? = null

    // Stores last initialization time
    private var lastInitTime = 0L

    // Retry limits
    private var retryCount = 0

    // Helper function to get context
    private fun getContext(): Context? = contextRef.get()

    // Getter for AppConfig
    private fun getAppConfig(): AppConfig? {
        val context = getContext() ?: return null
        return AppConfig.getInstance(context)
    }

    /**
     * Checks if device has active internet connection using modern
     * NetworkCapabilities API.
     *
     * @return true if connected to internet with validated connection, false
     *    otherwise
     */
    private fun isNetworkAvailable(): Boolean {
        val context = getContext() ?: return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            // Check both internet capability and validated connection
            val hasInternet =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Timber.d("Wi-Fi connection available (validated: $isValidated)")
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Timber.d("Cellular connection available (validated: $isValidated)")
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Timber.d("Ethernet connection available (validated: $isValidated)")
                }

                else -> {
                    Timber.d("Unknown connection type (has internet: $hasInternet, validated: $isValidated)")
                }
            }

            hasInternet && isValidated
        } catch (e: Exception) {
            Timber.d("Network check failed: ${e.message}")
            false
        }
    }

    /**
     * Checks network connectivity and throws exception if unavailable.
     * Separated from isNetworkAvailable() for clearer error handling.
     */
    private suspend fun checkNetworkConnectivity() {
        if (!isNetworkAvailable()) {
            throw NetworkUnavailableException(
                "No internet connection available. Please check Wi-Fi or cellular data."
            )
        }
    }

    /** Checks if error is network-related and should be retried. */
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
            Timber.d("Detected network error: ${exception.javaClass.simpleName}: $message")
        }

        return isNetworkError
    }

    /** Creates detailed error message based on exception type. */
    private fun createDetailedErrorMessage(exception: Exception): String {
        val message = exception.message?.lowercase() ?: ""

        return when {
            !isNetworkAvailable() -> {
                "No internet connection. Check Wi-Fi or cellular data."
            }

            message.contains("oauth2.googleapis.com") -> {
                "Cannot connect to Google authentication servers. Check if Google Services are accessible."
            }

            message.contains("no address associated with hostname") || message.contains("eai_nodata") -> {
                "DNS resolution problems. Check DNS settings."
            }

            message.contains("unable to resolve host") -> {
                "Cannot reach Google Drive servers. Check internet connection."
            }

            message.contains("connection refused") -> {
                "Google Drive servers rejected connection. Try again later."
            }

            message.contains("connection timed out") || message.contains("timeout") -> {
                "Connection timeout. Check internet stability."
            }

            else -> {
                "Connection problem: ${exception.message ?: "Unknown error"}"
            }
        }
    }

    /**
     * Initializes Google Drive API client with network checking.
     *
     * Checks network availability before attempting
     * initialization. This prevents Sentry noise from expected network
     * unavailability.
     *
     * @return true if initialized successfully, false otherwise
     */
    @SuppressLint("DiscouragedApi")
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = getContext() ?: return@withContext false
            val appConfig = getAppConfig() ?: return@withContext false

            Timber.d("Starting DriveApiClient initialization")

            // Check network availability BEFORE attempting initialization
            // This prevents Sentry errors when there's no internet
            if (!isNetworkAvailable()) {
                Timber.d("Cannot initialize Drive client - no internet connection (this is expected)")
                return@withContext false
            }

            // Check if service is already initialized
            if (driveService != null) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastInitTime < TimeUnit.MINUTES.toMillis(50)) {
                    Timber.d("DriveService already initialized and token is fresh")
                    return@withContext true
                }
                Timber.d("Token may expire soon - refreshing connection")
            }

            // Configure HTTP transport with increased timeouts
            val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

            val serviceAccountFileName = appConfig.getServiceAccountFileName()

            // Get credentials from app resources
            val serviceAccountStream = try {
                val resourceField = R.raw::class.java.getField(serviceAccountFileName)
                val resourceId = resourceField.getInt(null)
                context.resources.openRawResource(resourceId)
            } catch (_: Exception) {
                Timber.d("Direct resource ID not found, using getIdentifier")
                val resourceId = context.resources.getIdentifier(
                    serviceAccountFileName, "raw", context.packageName
                )
                if (resourceId == 0) {
                    Timber.w("Service account file not found: $serviceAccountFileName")
                    throw IllegalStateException("Service account file not found: $serviceAccountFileName")
                }
                context.resources.openRawResource(resourceId)
            }

            // Configure service account credentials with retry
            val credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                .createScoped(listOf(DriveScopes.DRIVE_READONLY))

            val requestInitializer = HttpCredentialsAdapter(credentials)

            // Create Drive API service with improved timeouts
            driveService = Drive.Builder(
                httpTransport, jsonFactory, createRequestInitializerWithRetry(requestInitializer)
            ).setApplicationName(APPLICATION_NAME).build()

            lastInitTime = System.currentTimeMillis()

            Timber.d("DriveApiClient initialized successfully")
            true
        } catch (e: NetworkUnavailableException) {
            // This is an expected case when there's no internet - log as debug, not warning
            Timber.d("Initialization skipped - no internet connection (expected)")
            false
        } catch (e: Exception) {
            when {
                isNetworkError(e) -> {
                    val detailedMessage = createDetailedErrorMessage(e)
                    // Log network errors as debug since they're often expected
                    Timber.d("Network error during initialization: $detailedMessage")
                }

                else -> {
                    // Only log unexpected errors as warnings
                    Timber.w("Unexpected error during Drive API initialization: ${e.message}")
                }
            }
            false
        }
    }

    /** Creates HttpRequestInitializer with improved network error handling. */
    private fun createRequestInitializerWithRetry(delegate: HttpRequestInitializer): HttpRequestInitializer {
        return HttpRequestInitializer { request ->
            delegate.initialize(request)

            // Increased timeouts for weak connections
            request.connectTimeout = 45000 // 45 seconds
            request.readTimeout = 60000    // 60 seconds

            // Increased retry count for network errors
            request.numberOfRetries = 5
            request.ioExceptionHandler =
                HttpIOExceptionHandler { httpRequest: HttpRequest, supportsRetry: Boolean ->
                    Timber.d("IOException in Drive API request, supportsRetry=$supportsRetry")
                    supportsRetry
                }

            // Additional headers for better compatibility
            request.headers.set("Accept", "application/json")
            request.headers.set("User-Agent", "$APPLICATION_NAME/1.0")
        }
    }

    /** Checks API limits and delays request if necessary. */
    private suspend fun checkApiLimits() {
        val currentTime = System.currentTimeMillis()

        // Reset counter every 5 minutes
        if (currentTime - lastMinuteReset.get() > RESET_INTERVAL_MS) {
            Timber.d("Resetting API request counter (was: ${requestCount.get()})")
            requestCount.set(0)
            lastMinuteReset.set(currentTime)
            return
        }

        // Check if not exceeding limit
        if (requestCount.get() >= MAX_REQUESTS_PER_INTERVAL) {
            val waitTime = RESET_INTERVAL_MS - (currentTime - lastMinuteReset.get())
            if (waitTime > 0) {
                Timber.d("API limit reached (${requestCount.get()}) - delaying next request by $waitTime ms")
                delay(waitTime)
                requestCount.set(0)
                lastMinuteReset.set(System.currentTimeMillis())
            }
        }

        val newCount = requestCount.incrementAndGet()
        Timber.d("API request count: $newCount")
    }

    /** Gets file info from Google Drive with improved error handling. */
    suspend fun getFileInfo(fileId: String): FileInfo = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Drive API client not initialized")

        try {
            // Check network connectivity before operation
            checkNetworkConnectivity()

            // Check API limits before executing request
            checkApiLimits()

            Timber.d("Fetching file info for ID: $fileId")

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
            handleApiException(e, "fetching file info for: $fileId") {
                getFileInfo(fileId)
            }
        }
    }

    /** Downloads file from Google Drive with improved error handling. */
    suspend fun downloadFile(fileId: String): java.io.InputStream = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Drive API client not initialized")

        try {
            // Check network connectivity before operation
            checkNetworkConnectivity()

            checkApiLimits()
            Timber.d("Downloading file with ID: $fileId")

            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            retryCount = 0
            java.io.ByteArrayInputStream(outputStream.toByteArray())
        } catch (e: Exception) {
            handleApiException(e, "downloading file: $fileId") {
                downloadFile(fileId)
            }
        }
    }

    /**
     * Lists files in specified Google Drive folder with improved error
     * handling.
     */
    suspend fun listFilesInFolder(folderId: String): List<FileInfo> = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Drive API client not initialized")

        try {
            // Check network connectivity before operation
            checkNetworkConnectivity()

            checkApiLimits()
            Timber.d("Listing files in folder: $folderId")

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
            handleApiException(e, "listing files in folder: $folderId") {
                listFilesInFolder(folderId)
            }
        }
    }

    /** Unified API exception handling with improved error messages. */
    private suspend fun <T> handleApiException(
        exception: Exception,
        operation: String,
        retryOperation: suspend () -> T,
    ): T {
        // Log with appropriate level based on error type
        when {
            exception is NetworkUnavailableException -> {
                Timber.d("Network unavailable during $operation (expected)")
            }

            isNetworkError(exception) -> {
                val detailedMessage = createDetailedErrorMessage(exception)
                Timber.d("Network error during $operation: $detailedMessage")
            }

            else -> {
                Timber.w("Error during $operation: ${exception.message}")
            }
        }

        when {
            isNetworkError(exception) -> {
                val detailedMessage = createDetailedErrorMessage(exception)

                if (shouldRetry(exception)) {
                    Timber.d("Retrying operation $operation after network error...")
                    return retryOperation()
                } else {
                    throw NetworkException(detailedMessage, exception)
                }
            }

            shouldRetry(exception) -> {
                Timber.d("Retrying operation $operation after error...")
                return retryOperation()
            }

            else -> {
                throw exception
            }
        }
    }

    /** Improved retry logic with better network error handling. */
    private suspend fun shouldRetry(exception: Exception): Boolean {
        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.d("Max retry count reached: $retryCount")
            return false
        }

        // Special handling for network errors
        if (isNetworkError(exception)) {
            // Recheck network connectivity
            if (!isNetworkAvailable()) {
                Timber.d("No internet connection - not retrying")
                return false
            }

            retryCount++
            val delayMs = (2000L * retryCount).coerceAtMost(MAX_BACKOFF_DELAY_MS)
            Timber.d("Network error - waiting ${delayMs}ms before retry attempt $retryCount of $MAX_RETRY_COUNT")
            delay(delayMs)
            return true
        }

        // Check if token expired
        val exceptionMessage = exception.message?.lowercase() ?: ""
        if (exceptionMessage.contains("token") && exceptionMessage.contains("expire")) {
            Timber.d("Token expired, reinitializing...")
            val initialized = initialize()
            if (!initialized) {
                Timber.d("Failed to reinitialize after token expiry")
                return false
            }
            retryCount++
            return true
        }

        // Other transient errors
        val isTransientError =
            exceptionMessage.contains("timeout") || exceptionMessage.contains("refused") || exceptionMessage.contains(
                "reset"
            ) || exceptionMessage.contains("unavailable") || exceptionMessage.contains("rate limit") || exceptionMessage.contains(
                "429"
            ) || exceptionMessage.contains("500") || exceptionMessage.contains("503")

        if (isTransientError) {
            retryCount++
            val delayMs = (1000L * (1 shl (retryCount - 1))).coerceAtMost(MAX_BACKOFF_DELAY_MS)
            Timber.d("Waiting $delayMs ms before retry attempt $retryCount of $MAX_RETRY_COUNT")
            delay(delayMs)
            return true
        }

        return false
    }

    /** Parses date in RFC 3339 format used by Google API. */
    private fun parseRfc3339Date(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return try {
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Timber.w("Error parsing date: $dateString - ${e.message}")
            Date()
        }
    }

    /** Class representing file information. */
    data class FileInfo(
        val id: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val modifiedTime: Date,
    )

    /** Exception thrown when no internet connection is available. */
    class NetworkUnavailableException(message: String) : Exception(message)

    /** Exception thrown for network problems with descriptive message. */
    class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        private const val APPLICATION_NAME = "Wszystkiego Najlepszego Dawid"
        private const val MAX_RETRY_COUNT = 5
        private const val MAX_BACKOFF_DELAY_MS = 45_000L

        // For tests
        @JvmStatic
        internal var mockInstance: DriveApiClient? = null

        // Static counters for monitoring API limits
        @Volatile
        private var requestCount = AtomicInteger(0)

        @Volatile
        private var lastMinuteReset = AtomicLong(System.currentTimeMillis())

        private const val RESET_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
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