package com.philornot.siekiera.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.network.DriveApiClient
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Date
import kotlin.io.path.createTempDirectory

@RunWith(MockitoJUnitRunner::class)
class FileCheckWorkerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockDriveApiClient: DriveApiClient

    @Mock
    private lateinit var mockAppConfig: AppConfig

    @Before
    fun setup() {
        // Mock AppConfig
        `when`(mockAppConfig.getDriveFolderId()).thenReturn("test_folder_id")
        `when`(mockAppConfig.getDaylioFileName()).thenReturn("test.daylio")
        `when`(mockAppConfig.isVerboseLoggingEnabled()).thenReturn(true)

        AppConfig.INSTANCE = mockAppConfig

        // Mock DriveApiClient
        runBlocking {
            `when`(mockDriveApiClient.initialize()).thenReturn(true)
        }

        // Set the test mock instance
        DriveApiClient.mockInstance = mockDriveApiClient
        FileCheckWorker.testDriveClient = mockDriveApiClient
    }

    @Test
    fun `doWork initializes DriveApiClient`() = runBlocking {
        // Given
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(mockContext).build()

        `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(emptyList())

        // When
        worker.doWork()

        // Then
        verify(mockDriveApiClient).initialize()
    }

    @Test
    fun `doWork returns success when files are empty`() = runBlocking {
        // Given
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(mockContext).build()

        `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(emptyList())

        // When
        val result = worker.doWork()

        // Then
        assert(result is ListenableWorker.Result.Success)
    }

    @Test
    fun `doWork downloads file when local file does not exist`() = runBlocking {
        // Given
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(mockContext).build()

        // Create mock file list with one file
        val mockFile = DriveApiClient.FileInfo(
            id = "test_file_id",
            name = "test.daylio",
            mimeType = "application/octet-stream",
            size = 1024L,
            modifiedTime = Date()
        )

        `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(listOf(mockFile))
        `when`(mockDriveApiClient.downloadFile(any())).thenReturn(ByteArrayInputStream("test data".toByteArray()))

        // Mock the File behavior using a temp file that doesn't exist
        val tempFile = File.createTempFile("nonexistent", ".tmp")
        tempFile.delete() // Make sure it doesn't exist

        // Mock context to return our directory structure
        val mockDir = File.createTempFile("mockDir", "")
        mockDir.delete()
        mockDir.mkdir()

        `when`(mockContext.getExternalFilesDir(any())).thenReturn(mockDir)

        // When
        val result = worker.doWork()

        // Then
        assert(result is ListenableWorker.Result.Success)
        verify(mockDriveApiClient).downloadFile(eq("test_file_id"))

        // Clean up
        mockDir.deleteRecursively()
    }

    @Test
    fun `doWork downloads newer file when remote file is newer`() = runBlocking {
        // Given
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(mockContext).build()

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

        `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(listOf(mockFile))
        `when`(mockDriveApiClient.downloadFile(any())).thenReturn(ByteArrayInputStream("test data".toByteArray()))

        // Create a temp file that's older than the remote file
        val tempDir = createTempDirectory().toFile()
        val localFile = File(tempDir, "test.daylio")
        localFile.createNewFile()
        localFile.setLastModified(oldDate.time)

        // Mock context to return our directory structure
        `when`(mockContext.getExternalFilesDir(any())).thenReturn(tempDir)

        // When
        val result = worker.doWork()

        // Then
        assert(result is ListenableWorker.Result.Success)
        verify(mockDriveApiClient).downloadFile(eq("test_file_id"))

        // Clean up
        tempDir.deleteRecursively()
    }

    @Test
    fun `doWork skips download when local file is newest`() = runBlocking {
        // Given
        val worker = TestListenableWorkerBuilder<FileCheckWorker>(mockContext).build()

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

        `when`(mockDriveApiClient.listFilesInFolder(any())).thenReturn(listOf(mockFile))

        // Create a temp file that's newer than the remote file
        val tempDir = createTempDirectory().toFile()
        val localFile = File(tempDir, "test.daylio")
        localFile.createNewFile()
        localFile.setLastModified(newDate.time)

        // Mock context to return our directory structure
        `when`(mockContext.getExternalFilesDir(any())).thenReturn(tempDir)

        // When
        val result = worker.doWork()

        // Then
        assert(result is ListenableWorker.Result.Success)
        // Shouldn't call download since local file is newer

        // Clean up
        tempDir.deleteRecursively()
    }
}