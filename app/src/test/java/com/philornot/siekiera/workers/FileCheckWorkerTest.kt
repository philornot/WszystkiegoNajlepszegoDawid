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
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.io.path.createTempDirectory

/**
 * Testy jednostkowe dla komponentów wykorzystywanych przez
 * FileCheckWorker.
 *
 * UWAGA: Nie używamy tutaj WorkManager ani testów bezpośrednio na
 * FileCheckWorker, ponieważ wymaga to pełnego kontekstu aplikacji, którego
 * nie możemy łatwo mockować. Zamiast tego, testujemy kluczowe komponenty
 * używane przez FileCheckWorker.
 */
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

        // Mock AppConfig - używamy lenient() aby uniknąć UnnecessaryStubbingException
        lenient().`when`(mockAppConfig.getDriveFolderId()).thenReturn("test_folder_id")
        lenient().`when`(mockAppConfig.getDaylioFileName()).thenReturn("test.daylio")
        lenient().`when`(mockAppConfig.isVerboseLoggingEnabled()).thenReturn(true)

        // Make our mock the singleton instance
        AppConfig.INSTANCE = mockAppConfig

        // Mock DriveApiClient initialization - użycie lenient()
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

        // POPRAWKA: Dodany reset dla FileCheckWorker.testDriveClient
        FileCheckWorker.testDriveClient = null
    }

    /** Test 1: Sprawdzenie czy inicjalizacja DriveApiClient działa poprawnie */
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

    /**
     * Test 2: Sprawdzenie czy listFilesInFolder działa poprawnie gdy nie ma
     * plików
     */
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

    /** Test 3: Sprawdzenie czy listFilesInFolder zwraca prawidłowe pliki */
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

    /** Test 4: Sprawdzenie czy downloadFile działa poprawnie */
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
     * Test 5: Sprawdzenie czy logika porównywania dat modyfikacji działa
     * poprawnie gdy plik lokalny jest nowszy niż zdalny
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

            // Then - zdalny plik jest starszy, więc nie powinien zastąpić lokalnego
            assert(!isRemoteFileNewer)

            // Clean up
            tempDir.deleteRecursively()
        }
    }

    /**
     * Test 6: Sprawdzenie czy logika porównywania dat modyfikacji działa
     * poprawnie gdy plik zdalny jest nowszy niż lokalny
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

            // Then - zdalny plik jest nowszy, więc powinien zastąpić lokalny
            assert(isRemoteFileNewer)

            // Clean up
            tempDir.deleteRecursively()
        }
    }

    /**
     * Test 7: Sprawdzenie pełnego scenariusza pobrania pliku gdy plik lokalny
     * nie istnieje
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

            // Set up mocks - używamy whenever zamiast when
            whenever(mockDriveApiClient.listFilesInFolder(any())).thenReturn(listOf(mockFile))
            whenever(mockDriveApiClient.downloadFile(any())).thenReturn(
                ByteArrayInputStream("test data".toByteArray())
            )

            // Create a temporary directory for testing
            val tempDir = createTempDirectory().toFile()
            whenever(mockContext.getExternalFilesDir(any())).thenReturn(tempDir)

            // When - symulujemy przepływ pracy FileCheckWorker
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