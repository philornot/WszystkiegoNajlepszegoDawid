package com.philornot.siekiera.workers

import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.network.DriveApiClient
import com.philornot.siekiera.utils.TimeUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.lang.reflect.Field
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Testy dla FileCheckWorker, który pobiera plik Daylio z Google Drive
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FileCheckWorkerTest {

    private lateinit var context: Context
    private lateinit var driveApiClient: DriveApiClient
    private lateinit var testFile: File
    private lateinit var appConfig: AppConfig
    private val downloadDir = Environment.DIRECTORY_DOWNLOADS
    private val fileName = "dawid_pamiętnik.daylio"
    private val testFolderId = "test_folder_id"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Initialize WorkManager for tests
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        // Mock dla AppConfig
        appConfig = mock(AppConfig::class.java)
        whenever(appConfig.getDaylioFileName()).thenReturn(fileName)
        whenever(appConfig.getDriveFolderId()).thenReturn(testFolderId)
        whenever(appConfig.isVerboseLoggingEnabled()).thenReturn(true)

        // Ustaw singleton AppConfig.INSTANCE
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, appConfig)

        // Mock for DriveApiClient
        driveApiClient = mock(DriveApiClient::class.java)
        runBlocking {
            whenever(driveApiClient.initialize()).thenReturn(true)
        }

        // Prepare test file - use the same path as the worker!
        testFile = File(context.getExternalFilesDir(downloadDir), fileName)

        // Ensure directory exists
        testFile.parentFile?.mkdirs()

        // Delete file if exists
        if (testFile.exists()) {
            testFile.delete()
        }

        // Set static mock instance
        setMockInstance(driveApiClient)
    }

    @After
    fun tearDown() {
        // Clear mockInstance
        setMockInstance(null)

        // Clear INSTANCE in AppConfig
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        // Clean up test file
        if (testFile.exists()) {
            testFile.delete()
        }
    }

    /**
     * Helper method to set static mockInstance field using reflection
     */
    private fun setMockInstance(instance: DriveApiClient?) {
        try {
            val field = DriveApiClient::class.java.getDeclaredField("mockInstance")
            field.isAccessible = true

            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true

            field.set(null, instance)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun `worker returns success when file is downloaded successfully`() = runBlocking {
        // Prepare mocked responses
        val fileInfo = DriveApiClient.FileInfo(
            id = "testId",
            name = fileName,
            mimeType = "application/octet-stream",
            size = 1024L,
            modifiedTime = Date()
        )

        val testData = "test data"
        val testInputStream = ByteArrayInputStream(testData.toByteArray())

        // Mock folder listing to return one file
        whenever(driveApiClient.listFilesInFolder(testFolderId)).thenReturn(listOf(fileInfo))
        whenever(driveApiClient.downloadFile("testId")).thenReturn(testInputStream)

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Diagnostic information
        println("File path: ${testFile.absolutePath}")
        println("File exists: ${testFile.exists()}")

        // Check result
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue("File should exist after worker execution", testFile.exists())
        assertEquals("File content should match", testData, testFile.readText())
    }

    @Test
    fun `worker returns retry when exception occurs during initialization`() = runBlocking {
        // Mock initialization to fail
        whenever(driveApiClient.initialize()).thenReturn(false)

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Check result - should be retry
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `worker returns retry when exception occurs during listing files`() = runBlocking {
        // Mock folder listing to throw exception
        whenever(driveApiClient.listFilesInFolder(testFolderId)).thenThrow(RuntimeException("Test error"))

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Check result - should be retry
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `worker returns retry when exception occurs during downloading file`() = runBlocking {
        // Prepare mocked responses
        val fileInfo = DriveApiClient.FileInfo(
            id = "testId",
            name = fileName,
            mimeType = "application/octet-stream",
            size = 1024L,
            modifiedTime = Date()
        )

        // Mock folder listing to return one file
        whenever(driveApiClient.listFilesInFolder(testFolderId)).thenReturn(listOf(fileInfo))

        // Mock download to throw exception
        whenever(driveApiClient.downloadFile("testId")).thenThrow(RuntimeException("Download error"))

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Check result - should be retry
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `worker updates file when remote file is newer`() = runBlocking {
        // Create local file with modification date in the past
        testFile.createNewFile()
        testFile.writeText("old data")
        val oldModifiedTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        testFile.setLastModified(oldModifiedTime)

        println("Test file created: ${testFile.absolutePath}")
        println("Old modification date: ${Date(testFile.lastModified())}")

        // Prepare mocked responses
        val newModifiedTime = System.currentTimeMillis()
        val fileInfo = DriveApiClient.FileInfo(
            id = "testId",
            name = fileName,
            mimeType = "application/octet-stream",
            size = 1024L,
            modifiedTime = Date(newModifiedTime) // Current date - newer than local file
        )

        val updatedData = "updated data"
        val testInputStream = ByteArrayInputStream(updatedData.toByteArray())

        // Mock folder listing to return one file with newer date
        whenever(driveApiClient.listFilesInFolder(testFolderId)).thenReturn(listOf(fileInfo))
        whenever(driveApiClient.downloadFile("testId")).thenReturn(testInputStream)

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Diagnostic information
        println("Worker executed with result: $result")
        println("File exists after execution: ${testFile.exists()}")
        if (testFile.exists()) {
            println("File content: ${testFile.readText()}")
            println("File size: ${testFile.length()}")
            println("New modification date: ${Date(testFile.lastModified())}")
        }

        // Check result
        assertEquals(ListenableWorker.Result.success(), result)

        // Verify that file was updated
        assertTrue("File should exist", testFile.exists())
        assertEquals("File content should be updated", updatedData, testFile.readText())
    }

    @Test
    fun `worker does not update file when local file is newer`() = runBlocking {
        // Create local file with current modification date
        testFile.createNewFile()
        val currentData = "current data"
        testFile.writeText(currentData)
        val newModifiedTime = System.currentTimeMillis()
        testFile.setLastModified(newModifiedTime)

        // Prepare mocked responses with date in the past
        val oldDate = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
        val fileInfo = DriveApiClient.FileInfo(
            id = "testId",
            name = fileName,
            mimeType = "application/octet-stream",
            size = 1024L,
            modifiedTime = oldDate // Older date than local file
        )

        // Mock folder listing to return one file with older date
        whenever(driveApiClient.listFilesInFolder(testFolderId)).thenReturn(listOf(fileInfo))

        // We should never try to download an older file
        whenever(driveApiClient.downloadFile("testId")).thenAnswer {
            throw AssertionError("Should not attempt to download older file")
        }

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Diagnostic information
        println("Worker executed with result: $result")
        println("File content after execution: ${testFile.readText()}")

        // Check result
        assertEquals(ListenableWorker.Result.success(), result)

        // Verify that file was NOT updated
        assertEquals("File content should not change", currentData, testFile.readText())
    }

    @Test
    fun `worker handles empty folder correctly`() = runBlocking {
        // Mock folder listing to return empty list
        whenever(driveApiClient.listFilesInFolder(testFolderId)).thenReturn(emptyList())

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Check result
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker filters files by daylio extension`() = runBlocking {
        // Prepare mocked responses with multiple files of different types
        val daylioFile = DriveApiClient.FileInfo(
            id = "daylio1",
            name = "dawid_pamiętnik.daylio",
            mimeType = "application/octet-stream",
            size = 1024L,
            modifiedTime = Date()
        )

        val textFile = DriveApiClient.FileInfo(
            id = "text1",
            name = "notes.txt",
            mimeType = "text/plain",
            size = 512L,
            modifiedTime = Date()
        )

        val olderDaylioFile = DriveApiClient.FileInfo(
            id = "daylio2",
            name = "old_backup.daylio",
            mimeType = "application/octet-stream",
            size = 2048L,
            modifiedTime = Date(System.currentTimeMillis() - 86400000) // 1 day older
        )

        val testData = "test daylio data"
        val testInputStream = ByteArrayInputStream(testData.toByteArray())

        // Mock folder listing to return multiple files
        whenever(driveApiClient.listFilesInFolder(testFolderId)).thenReturn(
            listOf(daylioFile, textFile, olderDaylioFile)
        )

        // Mock download to return test data
        whenever(driveApiClient.downloadFile("daylio1")).thenReturn(testInputStream)

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Check result
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue("File should exist after worker execution", testFile.exists())
        assertEquals("File content should match", testData, testFile.readText())
    }

    @Test
    fun `worker selects newest file when multiple daylio files exist`() = runBlocking {
        // Prepare mocked responses with multiple daylio files of different ages
        val newestDaylioFile = DriveApiClient.FileInfo(
            id = "daylio1",
            name = "dawid_pamiętnik.daylio",
            mimeType = "application/octet-stream",
            size = 1024L,
            modifiedTime = Date() // Current time
        )

        val olderDaylioFile = DriveApiClient.FileInfo(
            id = "daylio2",
            name = "old_backup.daylio",
            mimeType = "application/octet-stream",
            size = 2048L,
            modifiedTime = Date(System.currentTimeMillis() - 86400000) // 1 day older
        )

        val oldestDaylioFile = DriveApiClient.FileInfo(
            id = "daylio3",
            name = "very_old_backup.daylio",
            mimeType = "application/octet-stream",
            size = 512L,
            modifiedTime = Date(System.currentTimeMillis() - 172800000) // 2 days older
        )

        val newestData = "newest daylio data"
        val testInputStream = ByteArrayInputStream(newestData.toByteArray())

        // Mock folder listing to return multiple daylio files
        whenever(driveApiClient.listFilesInFolder(testFolderId)).thenReturn(
            listOf(olderDaylioFile, oldestDaylioFile, newestDaylioFile)
        )

        // Mock download to return newest data
        whenever(driveApiClient.downloadFile("daylio1")).thenReturn(testInputStream)

        // Prepare worker for test
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(context).build()

        // Execute worker
        val result = worker.doWork()

        // Check result
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue("File should exist after worker execution", testFile.exists())
        assertEquals("File content should match newest file", newestData, testFile.readText())
    }
}