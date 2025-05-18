package com.philornot.siekiera.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.WorkManagerTestInitHelper
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.network.DriveApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.io.path.createTempDirectory

@RunWith(MockitoJUnitRunner::class)
@ExperimentalCoroutinesApi
class FileCheckWorkerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockDriveApiClient: DriveApiClient

    @Mock
    private lateinit var mockAppConfig: AppConfig

    // Set up to use test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Set main dispatcher for coroutines
        Dispatchers.setMain(testDispatcher)

        // Mock AppConfig
        `when`(mockAppConfig.getDriveFolderId()).thenReturn("test_folder_id")
        `when`(mockAppConfig.getDaylioFileName()).thenReturn("test.daylio")
        `when`(mockAppConfig.isVerboseLoggingEnabled()).thenReturn(true)

        // Make our mock the singleton instance
        AppConfig.INSTANCE = mockAppConfig

        // Mock DriveApiClient initialization
        runBlocking {
            `when`(mockDriveApiClient.initialize()).thenReturn(true)
        }

        // Set our mock as the test client
        DriveApiClient.mockInstance = mockDriveApiClient
        FileCheckWorker.testDriveClient = mockDriveApiClient

        // Initialize WorkManager for testing
        WorkManagerTestInitHelper.initializeTestWorkManager(mockContext)
    }

    @After
    fun tearDown() {
        // Clean up after tests
        Dispatchers.resetMain()
        DriveApiClient.mockInstance = null
        FileCheckWorker.testDriveClient = null
        AppConfig.INSTANCE = null
    }

    @Test
    fun `doWork initializes DriveApiClient`() {
        // Mock getApplicationContext to return our mockContext
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        // Mock listFilesInFolder to return empty list
        runBlocking {
            `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(emptyList())

            // Use a real worker instance instead of using TestListenableWorkerBuilder
            val worker = FileCheckWorker(
                mockContext, TestWorkerParametersMock()
            )

            // Call doWork
            val result = worker.doWork()

            // Verify expected interactions and result
            verify(mockDriveApiClient).initialize()
            assert(result is ListenableWorker.Result.Success)
        }
    }

    @Test
    fun `doWork returns success when files are empty`() {
        // Mock getApplicationContext to return our mockContext
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        runBlocking {
            // Mock listFilesInFolder to return empty list
            `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(emptyList())

            // Use a real worker instance
            val worker = FileCheckWorker(
                mockContext, TestWorkerParametersMock()
            )

            // Call doWork
            val result = worker.doWork()

            // Verify interactions and result
            verify(mockDriveApiClient).listFilesInFolder(eq("test_folder_id"))
            assert(result is ListenableWorker.Result.Success)
        }
    }

    @Test
    fun `doWork downloads file when local file does not exist`() {
        // Mock getApplicationContext to return our mockContext
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        runBlocking {
            // Create mock file list with one file
            val mockFile = DriveApiClient.FileInfo(
                id = "test_file_id",
                name = "test.daylio",
                mimeType = "application/octet-stream",
                size = 1024L,
                modifiedTime = Date()
            )

            // Mock the necessary methods
            `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(listOf(mockFile))
            `when`(mockDriveApiClient.downloadFile(any())).thenReturn(ByteArrayInputStream("test data".toByteArray()))

            // Create a temporary directory for testing
            val tempDir = createTempDirectory().toFile()
            `when`(mockContext.getExternalFilesDir(any())).thenReturn(tempDir)

            // Use a real worker instance
            val worker = FileCheckWorker(
                mockContext, TestWorkerParametersMock()
            )

            // Call doWork
            val result = worker.doWork()

            // Verify interactions and result
            verify(mockDriveApiClient).downloadFile(eq("test_file_id"))
            assert(result is ListenableWorker.Result.Success)

            // Clean up
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `doWork downloads newer file when remote file is newer`() {
        // Mock getApplicationContext to return our mockContext
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        runBlocking {
            // Create mock file list with one newer file
            val currentTime = System.currentTimeMillis()
            val newDate = Date(currentTime)
            val oldDate = Date(currentTime - 86400000) // 1 day older

            val mockFile = DriveApiClient.FileInfo(
                id = "test_file_id",
                name = "test.daylio",
                mimeType = "application/octet-stream",
                size = 1024L,
                modifiedTime = newDate
            )

            // Mock the necessary methods
            `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(listOf(mockFile))
            `when`(mockDriveApiClient.downloadFile(any())).thenReturn(ByteArrayInputStream("test data".toByteArray()))

            // Create a temp file that's older than the remote file
            val tempDir = createTempDirectory().toFile()
            val localFile = File(tempDir, "test.daylio")

            // Create the file and set it to be older
            FileOutputStream(localFile).use { it.write("old data".toByteArray()) }
            localFile.setLastModified(oldDate.time)

            `when`(mockContext.getExternalFilesDir(any())).thenReturn(tempDir)

            // Use a real worker instance
            val worker = FileCheckWorker(
                mockContext, TestWorkerParametersMock()
            )

            // Call doWork
            val result = worker.doWork()

            // Verify interactions and result
            verify(mockDriveApiClient).downloadFile(eq("test_file_id"))
            assert(result is ListenableWorker.Result.Success)

            // Clean up
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `doWork skips download when local file is newest`() {
        // Mock getApplicationContext to return our mockContext
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        runBlocking {
            // Create mock file list with one older file
            val currentTime = System.currentTimeMillis()
            val oldDate = Date(currentTime - 86400000) // 1 day older
            val newDate = Date(currentTime)

            val mockFile = DriveApiClient.FileInfo(
                id = "test_file_id",
                name = "test.daylio",
                mimeType = "application/octet-stream",
                size = 1024L,
                modifiedTime = oldDate
            )

            // Mock the necessary methods
            `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(listOf(mockFile))

            // Create a temp file that's newer than the remote file
            val tempDir = createTempDirectory().toFile()
            val localFile = File(tempDir, "test.daylio")

            // Create the file and set it to be newer
            FileOutputStream(localFile).use { it.write("new data".toByteArray()) }
            localFile.setLastModified(newDate.time)

            `when`(mockContext.getExternalFilesDir(any())).thenReturn(tempDir)

            // Use a real worker instance
            val worker = FileCheckWorker(
                mockContext, TestWorkerParametersMock()
            )

            // Call doWork
            val result = worker.doWork()

            // Verify download was NOT called (file is already newer)
            verify(mockDriveApiClient).listFilesInFolder(eq("test_folder_id"))
            assert(result is ListenableWorker.Result.Success)

            // Clean up
            tempDir.deleteRecursively()
        }
    }

    /**
     * Mock implementation of WorkerParameters for testing. This avoids the
     * need to use TestListenableWorkerBuilder.
     */
    class TestWorkerParametersMock : androidx.work.WorkerParameters(
        java.util.UUID.randomUUID(),
        androidx.work.Data.EMPTY,
        emptyList(),
        RuntimeExtras(),
        1,
        1,
        java.util.concurrent.Executors.newSingleThreadExecutor(),
        androidx.work.impl.utils.taskexecutor.TaskExecutor {
            override fun getMainThreadExecutor(): java.util.concurrent.Executor {
                return java.util.concurrent.Executors.newSingleThreadExecutor()
            }

            override fun getSerialTaskExecutor(): java.util.concurrent.Executor {
                return java.util.concurrent.Executors.newSingleThreadExecutor()
            }

            override fun executeOnMainThread(runnable: Runnable) {
                runnable.run()
            }
        },
        androidx.work.WorkerFactory.getDefaultWorkerFactory(),
        androidx.work.impl.foreground.ForegroundProcessor { _, _, _ -> },
        { _, _ -> },
        { _, _, _ -> })
}