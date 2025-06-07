package com.philornot.siekiera.workers

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * Enhanced FileCheckWorker with improved network error handling and user
 * feedback.
 *
 * Handles network issues gracefully and provides better diagnostics for
 * debugging connection problems with Google Drive API.
 */
class FileCheckWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    // Używamy WeakReference, aby uniknąć memory leaks
    private val contextRef = WeakReference(context.applicationContext)

    // Enhanced error tracking
    private var networkErrorType: NetworkErrorType? = null
    private var lastErrorMessage: String? = null

    enum class NetworkErrorType {
        NO_INTERNET, DNS_FAILURE, TIMEOUT, SSL_ERROR, AUTH_ERROR, RATE_LIMITED, SERVER_ERROR, UNKNOWN
    }

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

    /**
     * Classifies network exceptions for better error handling and user
     * feedback
     */
    private fun classifyNetworkException(exception: Exception): NetworkErrorType {
        return when {
            exception is UnknownHostException -> {
                val message = exception.message?.lowercase() ?: ""
                if (message.contains("oauth2.googleapis.com") || message.contains("www.googleapis.com")) {
                    NetworkErrorType.DNS_FAILURE
                } else {
                    NetworkErrorType.NO_INTERNET
                }
            }

            exception is ConnectException -> NetworkErrorType.NO_INTERNET
            exception is SocketTimeoutException -> NetworkErrorType.TIMEOUT
            exception is SSLException -> NetworkErrorType.SSL_ERROR
            exception.message?.contains("401") == true || exception.message?.contains("unauthorized") == true -> NetworkErrorType.AUTH_ERROR
            exception.message?.contains("429") == true -> NetworkErrorType.RATE_LIMITED
            exception.message?.contains("50") == true -> NetworkErrorType.SERVER_ERROR
            else -> NetworkErrorType.UNKNOWN
        }
    }

    /** Provides user-friendly error message based on network error type */
    private fun getUserFriendlyErrorMessage(errorType: NetworkErrorType): String {
        return when (errorType) {
            NetworkErrorType.NO_INTERNET -> "Brak połączenia z internetem. Sprawdź WiFi lub dane mobilne."
            NetworkErrorType.DNS_FAILURE -> "Problem z rozwiązywaniem nazw serwerów Google. Sprawdź ustawienia DNS."
            NetworkErrorType.TIMEOUT -> "Przekroczono limit czasu połączenia. Spróbuj ponownie później."
            NetworkErrorType.SSL_ERROR -> "Problem z certyfikatami SSL. Sprawdź datę i czas na urządzeniu."
            NetworkErrorType.AUTH_ERROR -> "Problem z uwierzytelnianiem Google Drive. Skontaktuj się z deweloperem."
            NetworkErrorType.RATE_LIMITED -> "Przekroczono limit zapytań API. Spróbuj ponownie za kilka minut."
            NetworkErrorType.SERVER_ERROR -> "Serwery Google Drive są tymczasowo niedostępne."
            NetworkErrorType.UNKNOWN -> "Wystąpił nieznany błąd sieciowy."
        }
    }

    /** Logs detailed network error information for debugging */
    private fun logNetworkError(operation: String, exception: Exception) {
        val errorType = classifyNetworkException(exception)
        networkErrorType = errorType
        lastErrorMessage = exception.message

        Timber.e("Network error in FileCheckWorker.$operation:")
        Timber.e("  Error type: $errorType")
        Timber.e("  Exception: ${exception.javaClass.simpleName}")
        Timber.e("  Message: ${exception.message}")

        // Add specific diagnostics
        when (errorType) {
            NetworkErrorType.DNS_FAILURE -> {
                Timber.e("  Issue: Cannot resolve Google API hostnames")
                Timber.e("  Possible solutions: Check network, try different DNS, restart device")
            }

            NetworkErrorType.NO_INTERNET -> {
                Timber.e("  Issue: No internet connectivity")
                Timber.e("  Possible solutions: Enable WiFi/mobile data, check airplane mode")
            }

            NetworkErrorType.TIMEOUT -> {
                Timber.e("  Issue: Network requests timing out")
                Timber.e("  Possible solutions: Check connection speed, try again later")
            }

            NetworkErrorType.SSL_ERROR -> {
                Timber.e("  Issue: SSL certificate problems")
                Timber.e("  Possible solutions: Check device date/time, update system")
            }

            else -> {
                Timber.e("  User message: ${getUserFriendlyErrorMessage(errorType)}")
            }
        }
    }

    /** Saves network error information to SharedPreferences for later retrieval */
    private fun saveNetworkErrorInfo(context: Context) {
        if (networkErrorType != null) {
            val prefs = context.getSharedPreferences("network_error_prefs", Context.MODE_PRIVATE)
            prefs.edit {
                putString("last_error_type", networkErrorType!!.name)
                putString("last_error_message", lastErrorMessage ?: "Unknown error")
                putLong("last_error_time", System.currentTimeMillis())
            }
        }
    }

    override suspend fun doWork(): Result = kotlinx.coroutines.runBlocking {
        Timber.d("FileCheckWorker: Starting enhanced file check workflow")

        try {
            // Reset error tracking
            networkErrorType = null
            lastErrorMessage = null

            // Sprawdź czy wymuszamy pobieranie
            val forceDownload = inputData.getBoolean("force_download", false)

            // Check if another task is already running (unless forcing download)
            if (isWorkAlreadyRunning() && !forceDownload) {
                Timber.d("FileCheckWorker: Another file check task is running - skipping")
                return@runBlocking Result.success()
            }

            // Check if file is already downloaded (unless forcing download)
            if (isFileAlreadyDownloaded() && !forceDownload) {
                Timber.d("FileCheckWorker: File already downloaded - skipping")
                return@runBlocking Result.success()
            }

            // Perform the main file check operation
            withContext(Dispatchers.IO) {
                Timber.d("FileCheckWorker: Starting file update check on IO dispatcher")
                checkForFileUpdate()
                Timber.d("FileCheckWorker: File update check completed successfully")
            }

            // Send completion broadcast
            sendCompletionBroadcast()

            Timber.d("FileCheckWorker: Work completed successfully")
            Result.success()

        } catch (e: IOException) {
            // Enhanced network error handling
            logNetworkError("doWork", e)

            val context = getContext()
            if (context != null) {
                saveNetworkErrorInfo(context)
            }

            // Determine retry strategy based on error type
            val shouldRetry = when (networkErrorType) {
                NetworkErrorType.NO_INTERNET,
                NetworkErrorType.DNS_FAILURE,
                NetworkErrorType.TIMEOUT,
                    -> runAttemptCount < 3

                NetworkErrorType.SERVER_ERROR,
                NetworkErrorType.RATE_LIMITED,
                    -> runAttemptCount < 5

                NetworkErrorType.SSL_ERROR,
                NetworkErrorType.AUTH_ERROR,
                    -> runAttemptCount < 1 // Limited retry
                else -> runAttemptCount < MAX_RETRY_ATTEMPTS
            }

            if (shouldRetry) {
                Timber.d("FileCheckWorker: Network error - will retry (attempt $runAttemptCount)")
                Result.retry()
            } else {
                Timber.e("FileCheckWorker: Max retries reached for network error")
                Result.failure()
            }

        } catch (e: Exception) {
            // Handle non-network exceptions
            Timber.e(e, "FileCheckWorker: Unexpected error: ${e.message}")

            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Timber.d("FileCheckWorker: Unexpected error - will retry (attempt $runAttemptCount)")
                Result.retry()
            } else {
                Timber.e("FileCheckWorker: Max retries reached for unexpected error")
                Result.failure()
            }
        }
    }

    /** Sprawdza, czy już istnieje uruchomione zadanie sprawdzania pliku */
    private fun isWorkAlreadyRunning(): Boolean {
        val context = getContext() ?: return false

        val forceDownload = inputData.getBoolean("force_download", false)
        if (forceDownload) {
            Timber.d("FileCheckWorker: Force download enabled - ignoring running check")
            return false
        }

        val prefs = context.getSharedPreferences("file_check_prefs", Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong("last_check_time", 0)
        val currentTime = System.currentTimeMillis()

        // If less than 5 seconds since last check, assume another instance is running
        if (currentTime - lastCheckTime < 5000) {
            Timber.d("FileCheckWorker: Recent check detected (${currentTime - lastCheckTime}ms ago)")
            return true
        }

        // Update last check time
        prefs.edit { putLong("last_check_time", currentTime) }
        return false
    }

    /** Sprawdza, czy plik został już pobrany i jest w folderze Pobrane */
    private fun isFileAlreadyDownloaded(): Boolean {
        val context = getContext() ?: return false
        val appConfig = getAppConfig() ?: return false

        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val giftReceived = prefs.getBoolean("gift_received", false)
        val forceDownload = inputData.getBoolean("force_download", false)

        if (giftReceived && !forceDownload) {
            val fileName = appConfig.getDaylioFileName()
            val fileExists = FileUtils.isFileInPublicDownloads(fileName)
            Timber.d("FileCheckWorker: Gift received, file exists: $fileExists")
            return fileExists
        }

        return false
    }

    /** Enhanced file update check with better error handling */
    private suspend fun checkForFileUpdate() {
        val context = getContext() ?: throw IllegalStateException("Context unavailable")
        val appConfig = getAppConfig() ?: throw IllegalStateException("AppConfig unavailable")

        val folderId = appConfig.getDriveFolderId()
        val fileName = appConfig.getDaylioFileName()

        Timber.d("FileCheckWorker: Checking for file updates - folder: $folderId, target: $fileName")

        // Check if file already exists locally
        val forceDownload = inputData.getBoolean("force_download", false)
        if (FileUtils.isFileInPublicDownloads(fileName) && !forceDownload) {
            Timber.d("FileCheckWorker: File $fileName already exists locally")
            markGiftAsReceived(context, fileName)
            return
        }

        try {
            // Get Drive client with health check
            val driveClient = getDriveClient(context)

            // Check client health before proceeding
            if (!driveClient.isHealthy()) {
                Timber.w("FileCheckWorker: DriveClient is not healthy, attempting reinitialization")
            }

            // Initialize client with enhanced error handling
            if (!driveClient.initialize()) {
                throw IOException("Failed to initialize Drive API client")
            }

            Timber.d("FileCheckWorker: Drive API client initialized successfully")

            // Search for .daylio files in the folder
            val files =
                driveClient.listFilesInFolder(folderId).filter { it.name.endsWith(".daylio") }

            Timber.d("FileCheckWorker: Found ${files.size} .daylio files in folder")

            if (files.isEmpty()) {
                Timber.d("FileCheckWorker: No .daylio files found - nothing to download")
                return
            }

            // Find the newest file
            val newestFile = files.maxByOrNull { it.modifiedTime.time } ?: return

            Timber.d("FileCheckWorker: Newest file: ${newestFile.name}")
            Timber.d("  Modified: ${TimeUtils.formatDate(newestFile.modifiedTime)}")
            Timber.d("  Size: ${newestFile.size} bytes")

            // Check if we should download the file
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val giftReceived = prefs.getBoolean("gift_received", false)

            if (giftReceived && !forceDownload) {
                Timber.d("FileCheckWorker: Gift already received and not forcing download")
                return
            }

            // Download the file
            Timber.d("FileCheckWorker: Starting direct download of file")
            val success = downloadFileDirect(driveClient, newestFile.id, fileName)

            if (success) {
                handleSuccessfulDownload(context, fileName, prefs)
            } else {
                throw IOException("File download failed")
            }

        } catch (e: Exception) {
            // Log network error details and rethrow
            logNetworkError("checkForFileUpdate", e)
            throw e
        }
    }

    /** Handles successful download with notifications and preferences update */
    private fun handleSuccessfulDownload(
        context: Context,
        fileName: String,
        prefs: SharedPreferences,
    ) {
        val firstDownloadNotified = prefs.getBoolean("first_download_notified", false)

        // Show notification only for first download
        if (!firstDownloadNotified) {
            Timber.d("FileCheckWorker: Showing first download notification")
            NotificationHelper.showDownloadCompleteNotification(context, fileName)
            prefs.edit { putBoolean("first_download_notified", true) }
        } else {
            Timber.d("FileCheckWorker: Download notification already shown previously")
        }

        // Mark gift as received
        markGiftAsReceived(context, fileName)

        Timber.d("FileCheckWorker: File downloaded successfully: $fileName")
    }

    /** Marks the gift as received in preferences */
    private fun markGiftAsReceived(context: Context, fileName: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean("gift_received", true)
            putString("downloaded_file_name", fileName)
        }
        Timber.d("FileCheckWorker: Marked gift as received: $fileName")
    }

    /** Enhanced direct file download with better error handling */
    private suspend fun downloadFileDirect(
        client: DriveApiClient,
        fileId: String,
        fileName: String,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("FileCheckWorker: Starting direct download - fileId: $fileId, fileName: $fileName")

                val context = getContext() ?: throw IllegalStateException("Context unavailable")

                // Check if file exists and we're not forcing download
                val forceDownload = inputData.getBoolean("force_download", false)
                if (FileUtils.isFileInPublicDownloads(fileName) && !forceDownload) {
                    Timber.d("FileCheckWorker: File already exists, using existing")
                    return@withContext true
                }

                // Download file content from Google Drive
                Timber.d("FileCheckWorker: Downloading content from Google Drive")
                val inputStream = client.downloadFile(fileId)
                Timber.d("FileCheckWorker: Successfully obtained input stream from Drive API")

                var uri: Uri? = null

                // Save file using appropriate method based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ uses MediaStore
                    Timber.d("FileCheckWorker: Using MediaStore approach for Android 10+")
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
                                val bytesCopied = input.copyTo(outputStream)
                                Timber.d("FileCheckWorker: Copied $bytesCopied bytes to output stream")
                            }
                        }

                        // Complete the transaction
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)

                        Timber.d("FileCheckWorker: File saved via MediaStore: $uri")
                        return@withContext true
                    } else {
                        throw IOException("Failed to create MediaStore URI")
                    }
                } else {
                    // Older Android versions use direct file access
                    Timber.d("FileCheckWorker: Using External Storage approach for older Android")
                    try {
                        val downloadsDir =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs()
                        }

                        val destinationFile = File(downloadsDir, fileName)
                        Timber.d("FileCheckWorker: Saving to ${destinationFile.absolutePath}")

                        java.io.FileOutputStream(destinationFile).use { outputStream ->
                            inputStream.use { input ->
                                val bytesCopied = input.copyTo(outputStream)
                                Timber.d("FileCheckWorker: Copied $bytesCopied bytes to file")
                            }
                        }

                        val fileExists = destinationFile.exists()
                        val fileSize = destinationFile.length()
                        Timber.d("FileCheckWorker: File saved - exists: $fileExists, size: $fileSize bytes")
                        return@withContext fileExists && fileSize > 0

                    } catch (e: IOException) {
                        // Fallback to app-specific directory
                        Timber.w("FileCheckWorker: External storage failed, using app directory fallback")

                        val internalFile = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName
                        )

                        java.io.FileOutputStream(internalFile).use { outputStream ->
                            inputStream.use { input ->
                                val bytesCopied = input.copyTo(outputStream)
                                Timber.d("FileCheckWorker: Copied $bytesCopied bytes to app directory")
                            }
                        }

                        val fileExists = internalFile.exists()
                        val fileSize = internalFile.length()
                        Timber.d("FileCheckWorker: Fallback save - exists: $fileExists, size: $fileSize bytes")
                        return@withContext fileExists && fileSize > 0
                    }
                }
            } catch (e: Exception) {
                logNetworkError("downloadFileDirect", e)
                Timber.e(e, "FileCheckWorker: Error during direct file download")
                return@withContext false
            }
        }
    }

    /** Sends completion broadcast with error information if applicable */
    private fun sendCompletionBroadcast() {
        val context = getContext() ?: return

        val intent = Intent("com.philornot.siekiera.WORK_COMPLETED").apply {
            setPackage(context.packageName)

            // Add error information if available
            networkErrorType?.let { errorType ->
                putExtra("network_error_type", errorType.name)
                putExtra("error_message", getUserFriendlyErrorMessage(errorType))
            }
        }

        context.sendBroadcast(intent)
        Timber.d("FileCheckWorker: Completion broadcast sent")
    }

    companion object {
        // Do testów - pozwala na wstrzyknięcie mocka
        @JvmStatic
        internal var testDriveClient: DriveApiClient? = null

        // Enhanced retry settings
        private const val MAX_RETRY_ATTEMPTS = 4

        // WorkManager constants
        private const val FILE_CHECK_WORK_TAG = "file_check"
        private const val ONE_TIME_WORK_NAME = "one_time_file_check"

        /** Creates work request with network-aware retry policy */
        fun createWorkRequest(intervalHours: Long) = PeriodicWorkRequestBuilder<FileCheckWorker>(
            intervalHours, TimeUnit.HOURS
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL, 30, // Minimum backoff of 30 minutes
            TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true) // Don't drain battery on poor network
                .build()
        ).addTag(FILE_CHECK_WORK_TAG)

        /** Creates one-time work request with enhanced constraints */
        fun createOneTimeCheckRequest(forceDownload: Boolean = false) =
            OneTimeWorkRequestBuilder<FileCheckWorker>().setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(false) // Allow on low battery for user-initiated actions
                        .build()
                ).addTag(FILE_CHECK_WORK_TAG).setInputData(
                    androidx.work.Data.Builder().putBoolean("force_download", forceDownload).build()
                )

        /** Enhanced check for whether file check can be performed */
        fun canCheckFile(context: Context): Boolean {
            val prefs = context.getSharedPreferences("file_check_prefs", Context.MODE_PRIVATE)
            val lastCheckTime = prefs.getLong("last_check_time", 0)
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastCheckTime

            // Check for recent network errors
            val errorPrefs =
                context.getSharedPreferences("network_error_prefs", Context.MODE_PRIVATE)
            val lastErrorTime = errorPrefs.getLong("last_error_time", 0)
            val recentNetworkError = currentTime - lastErrorTime < TimeUnit.MINUTES.toMillis(10)

            // Allow check if enough time has passed and no recent critical network errors
            val canCheck = timeDiff > 2000 && !recentNetworkError

            if (!canCheck) {
                val reason = when {
                    timeDiff <= 2000 -> "recent check (${timeDiff}ms ago)"
                    recentNetworkError -> "recent network error"
                    else -> "unknown"
                }
                Timber.d("FileCheckWorker: Cannot check file - reason: $reason")
            }

            return canCheck
        }

        /** Enhanced one-time check scheduling with network awareness */
        fun planOneTimeCheck(context: Context, forceDownload: Boolean = false) {
            Timber.d("FileCheckWorker: Planning one-time check (forceDownload=$forceDownload)")

            // Check if we can perform the operation
            if (!canCheckFile(context) && !forceDownload) {
                Timber.d("FileCheckWorker: Cannot perform check - conditions not met")
                return
            }

            // Build the work request
            val request = createOneTimeCheckRequest(forceDownload).build()

            // Use REPLACE to replace any pending tasks
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )

            // Update last check time
            val prefs = context.getSharedPreferences("file_check_prefs", Context.MODE_PRIVATE)
            prefs.edit { putLong("last_check_time", System.currentTimeMillis()) }

            Timber.d("FileCheckWorker: One-time check scheduled successfully")
        }

        /** Gets last network error information for diagnostics */
        fun getLastNetworkError(context: Context): Triple<String?, String?, Long>? {
            val prefs = context.getSharedPreferences("network_error_prefs", Context.MODE_PRIVATE)
            val errorType = prefs.getString("last_error_type", null)
            val errorMessage = prefs.getString("last_error_message", null)
            val errorTime = prefs.getLong("last_error_time", 0)

            return if (errorType != null && errorTime > 0) {
                Triple(errorType, errorMessage, errorTime)
            } else {
                null
            }
        }

        /** Clears stored network error information */
        fun clearNetworkErrorInfo(context: Context) {
            val prefs = context.getSharedPreferences("network_error_prefs", Context.MODE_PRIVATE)
            prefs.edit { clear() }
        }
    }
}