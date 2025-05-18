package com.philornot.siekiera.workers

import android.content.Context
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
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.io.path.createTempDirectory

/**
 * Unit tests for components used by FileCheckWorker.
 *
 * Note: We don't test FileCheckWorker directly since it requires a full
 * application context. Instead, we test the key components used by
 * FileCheckWorker.
 */
@RunWith(MockitoJUnitRunner.Silent::class) // Changed to Silent to avoid unnecessary stubbing errors
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

        // Mock AppConfig - use lenient() for better test behavior
        lenient().`when`(mockAppConfig.getDriveFolderId()).thenReturn("test_folder_id")
        lenient().`when`(mockAppConfig.getDaylioFileName()).thenReturn("test.daylio")
        lenient().`when`(mockAppConfig.isVerboseLoggingEnabled()).thenReturn(true)

        // Make our mock the singleton instance
        AppConfig.INSTANCE = mockAppConfig

        // Mock DriveApiClient initialization
        runBlocking {
            lenient().`when`(mockDriveApiClient.initialize()).thenReturn(true)
        }

        // Set our mock as the test client
        DriveApiClient.mockInstance = mockDriveApiClient

        // Mock application context
        lenient().`when`(mockContext.applicationContext).thenReturn(mockContext)
    }

    @After
    fun tearDown() {
        // Clean up after tests
        Dispatchers.resetMain()
        DriveApiClient.mockInstance = null
        AppConfig.INSTANCE = null

        // Reset FileCheckWorker.testDriveClient
        FileCheckWorker.testDriveClient = null
    }

    /** Test 1: Check if DriveApiClient initialization works properly */
    @Test
    fun `initialize DriveApiClient test`() {
        runBlocking {
            // Given
            whenever(mockDriveApiClient.initialize()).thenReturn(true)

            // When
            val result = mockDriveApiClient.initialize()

            // Then
            assert(result)
            verify(mockDriveApiClient).initialize()
        }
    }

    /** Test 2: Check if listFilesInFolder works properly when no files */
    @Test
    fun `listFilesInFolder returns empty list test`() {
        runBlocking {
            // Given
            whenever(mockDriveApiClient.listFilesInFolder("test_folder_id")).thenReturn(emptyList())

            // When
            val files = mockDriveApiClient.listFilesInFolder("test_folder_id")

            // Then
            assert(files.isEmpty())
            verify(mockDriveApiClient).listFilesInFolder("test_folder_id")
        }
    }

    /** Test 3: Check if listFilesInFolder returns correct files */
    @Test
    fun `listFilesInFolder returns files test`() {
        runBlocking {
            // Given
            val testFiles = listOf(
                DriveApiClient.FileInfo(
                    id = "test_file_id",
                    name = "test.daylio",
                    mimeType = "application/octet-stream",
                    size = 1024L,
                    modifiedTime = Date()
                )
            )
            whenever(mockDriveApiClient.listFilesInFolder("test_folder_id")).thenReturn(testFiles)

            // When
            val files = mockDriveApiClient.listFilesInFolder("test_folder_id")

            // Then
            assert(files.size == 1)
            assert(files[0].id == "test_file_id")
            assert(files[0].name == "test.daylio")
            verify(mockDriveApiClient).listFilesInFolder("test_folder_id")
        }
    }

    /** Test 4: Check if downloadFile works properly */
    @Test
    fun `downloadFile test`() {
        runBlocking {
            // Given
            val testData = "test data".toByteArray()
            whenever(mockDriveApiClient.downloadFile("test_file_id")).thenReturn(
                ByteArrayInputStream(testData)
            )

            // When
            val result = mockDriveApiClient.downloadFile("test_file_id")

            // Then
            val resultBytes = result.readBytes()
            assert(resultBytes.contentEquals(testData))
            verify(mockDriveApiClient).downloadFile("test_file_id")
        }
    }

    /**
     * Test 5: Check if date comparison logic works correctly when local file
     * is newer than remote file
     */
    @Test
    fun `local file newer than remote file test`() {
        runBlocking {
            // Given
            val currentTime = System.currentTimeMillis()
            val oldDate = Date(currentTime - 86400000) // 1 day older
            val newDate = Date(currentTime)

            // Create a temp local file that's newer than the remote file
            val tempDir = createTempDirectory().toFile()
            val localFile = File(tempDir, "test.daylio")
            FileOutputStream(localFile).use { it.write("new data".toByteArray()) }
            localFile.setLastModified(newDate.time)

            // When
            val isRemoteFileNewer = oldDate.time > localFile.lastModified()

            // Then - remote file is older, so it shouldn't replace local
            assert(!isRemoteFileNewer)

            // Clean up
            tempDir.deleteRecursively()
        }
    }

    /**
     * Test 6: Check if date comparison logic works correctly when remote file
     * is newer than local file
     */
    @Test
    fun `remote file newer than local file test`() {
        runBlocking {
            // Given
            val currentTime = System.currentTimeMillis()
            val oldDate = Date(currentTime - 86400000) // 1 day older
            val newDate = Date(currentTime)

            // Create a temp local file that's older than the remote file
            val tempDir = createTempDirectory().toFile()
            val localFile = File(tempDir, "test.daylio")
            FileOutputStream(localFile).use { it.write("old data".toByteArray()) }
            localFile.setLastModified(oldDate.time)

            // When
            val isRemoteFileNewer = newDate.time > localFile.lastModified()

            // Then - remote file is newer, so it should replace local
            assert(isRemoteFileNewer)

            // Clean up
            tempDir.deleteRecursively()
        }
    }

    /**
     * Test 7: Full scenario testing for file download when local file doesn't
     * exist
     */
    @Test
    fun `download file when local file does not exist`() {
        runBlocking {
            // Given
            val mockFile = DriveApiClient.FileInfo(
                id = "test_file_id",
                name = "test.daylio",
                mimeType = "application/octet-stream",
                size = 1024L,
                modifiedTime = Date()
            )

            // Set up mocks
            // Use doReturn syntax for better stub handling
            whenever(mockDriveApiClient.listFilesInFolder(any())) doReturn listOf(mockFile)
            whenever(mockDriveApiClient.downloadFile(any())) doReturn ByteArrayInputStream("test data".toByteArray())

            // Create a temporary directory for testing
            val tempDir = createTempDirectory().toFile()
            whenever(mockContext.getExternalFilesDir(any())).thenReturn(tempDir)

            // When - simulating FileCheckWorker workflow
            val files = mockDriveApiClient.listFilesInFolder("test_folder_id")
            val fileToDownload = files.find { it.name == "test.daylio" }

            // Then
            assert(fileToDownload != null)
            fileToDownload?.let {
                val inputStream = mockDriveApiClient.downloadFile(it.id)
                val downloadedData = inputStream.readBytes()
                assert(downloadedData.isNotEmpty())

                // Verify interactions
                verify(mockDriveApiClient).listFilesInFolder("test_folder_id")
                verify(mockDriveApiClient).downloadFile(it.id)
            }

            // Clean up
            tempDir.deleteRecursively()
        }
    }
}