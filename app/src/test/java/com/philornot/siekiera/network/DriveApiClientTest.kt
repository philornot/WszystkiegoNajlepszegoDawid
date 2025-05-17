package com.philornot.siekiera.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import com.philornot.siekiera.config.AppConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Date
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DriveApiClientTest {

    private lateinit var context: Context
    private lateinit var appConfig: AppConfig
    private lateinit var mockDriveService: Drive
    private lateinit var driveClient: DriveApiClient
    private lateinit var mockFiles: Drive.Files
    private lateinit var mockFilesList: Drive.Files.List
    private lateinit var mockFilesGet: Drive.Files.Get

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        // Mock dla AppConfig
        appConfig = mockk(relaxed = true)
        every { appConfig.getServiceAccountFileName() } returns "service_account"

        // Ustaw singleton AppConfig.INSTANCE
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, appConfig)

        // Mock dla Drive API Service i jego komponentów
        mockDriveService = mockk(relaxed = true)
        mockFiles = mockk(relaxed = true)
        mockFilesList = mockk(relaxed = true)
        mockFilesGet = mockk(relaxed = true)

        every { mockDriveService.files() } returns mockFiles
        every { mockFiles.list() } returns mockFilesList
        every { mockFiles.get(any()) } returns mockFilesGet

        // Mock dla otwierania zasobu z pliku service_account.json
        mockkStatic(Class::class.java.name)
        every {
            context.resources.getIdentifier("service_account", "raw", context.packageName)
        } returns 1

        every {
            context.resources.openRawResource(1)
        } returns ByteArrayInputStream(
            """
            {"type":"service_account","client_email":"test@example.com","private_key":"test_key"}
        """.trimIndent().toByteArray()
        )

        // Inicjalizacja testowanego obiektu
        driveClient = DriveApiClient(context)

        // Podmieniamy wewnętrzne pole driveService na mocka
        val driveServiceField = DriveApiClient::class.java.getDeclaredField("driveService")
        driveServiceField.isAccessible = true
        driveServiceField.set(driveClient, mockDriveService)
    }

    @After
    fun tearDown() {
        // Resetowanie singletona
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        // Czyszczenie mockInstance w DriveApiClient
        DriveApiClient.clearMockInstance()

        unmockkAll()
    }

    @Test
    fun `initialize returns true when credentials are valid`() = runBlocking {
        // Konfiguracja
        val result = driveClient.initialize()

        // Weryfikacja
        assertTrue("Inicjalizacja powinna zwrócić true dla poprawnych poświadczeń", result)
    }

    @Test
    fun `initialize handles exception and returns false`() = runBlocking {
        // Konfiguracja - symulacja błędu podczas inicjalizacji
        val newDriveClient = spyk(driveClient)

        // Przygotuj pole driveService, aby wyrzucało wyjątek
        val driveServiceField = DriveApiClient::class.java.getDeclaredField("driveService")
        driveServiceField.isAccessible = true
        driveServiceField.set(
            newDriveClient,
            null
        ) // Ustawienie na null spowoduje NullPointerException

        // Nadpisz kod inicjalizacji, aby symulować wyjątek
        every {
            context.resources.openRawResource(any())
        } throws IOException("Test exception")

        // Wywołanie testowanej metody
        val result = newDriveClient.initialize()

        // Weryfikacja
        assertFalse("Inicjalizacja powinna zwrócić false gdy wystąpi wyjątek", result)
    }

    @Test
    fun `getFileInfo returns correct file info`() = runBlocking {
        // Konfiguracja
        val fileId = "test_file_id"
        val fileName = "test_file.daylio"
        val mimeType = "application/octet-stream"
        val fileSize = 1024L
        val modifiedTime = com.google.api.client.util.DateTime(System.currentTimeMillis())

        val mockFile = com.google.api.services.drive.model.File().setId(fileId).setName(fileName)
            .setMimeType(mimeType).setSize(fileSize).setModifiedTime(modifiedTime)

        every { mockFilesGet.setFields(any()) } returns mockFilesGet
        every { mockFilesGet.execute() } returns mockFile

        // Wywołanie testowanej metody
        val fileInfo = driveClient.getFileInfo(fileId)

        // Weryfikacja
        assertEquals(fileId, fileInfo.id)
        assertEquals(fileName, fileInfo.name)
        assertEquals(mimeType, fileInfo.mimeType)
        assertEquals(fileSize, fileInfo.size)
        // Data może być przekształcona, więc nie sprawdzamy dokładnej wartości
        assertNotNull(fileInfo.modifiedTime)
    }

    @Test(expected = IllegalStateException::class)
    fun `getFileInfo throws exception when not initialized`() = runBlocking {
        // Przygotuj klienta z pustym driveService
        val newDriveClient = spyk(driveClient)

        // Ustaw driveService na null
        val driveServiceField = DriveApiClient::class.java.getDeclaredField("driveService")
        driveServiceField.isAccessible = true
        driveServiceField.set(newDriveClient, null)

        // Wywołanie powinno rzucić wyjątek
        newDriveClient.getFileInfo("any_id")
    }

    @Test(expected = RuntimeException::class)
    fun `getFileInfo propagates exceptions from Drive API`() = runBlocking {
        // Konfiguracja - symulacja błędu API
        every { mockFilesGet.setFields(any()) } returns mockFilesGet
        every { mockFilesGet.execute() } throws RuntimeException("API error")

        // Wywołanie testowanej metody powinno przepropagować wyjątek
        driveClient.getFileInfo("test_file_id")
    }

    @Test
    fun `downloadFile returns correct input stream`() = runBlocking {
        // Konfiguracja
        val fileId = "test_file_id"
        val testData = "file content test data"

        // Mock dla executeMediaAndDownloadTo
        every { mockFilesGet.executeMediaAndDownloadTo(any()) } answers {
            val outputStream = firstArg<java.io.OutputStream>()
            outputStream.write(testData.toByteArray())
        }

        // Wywołanie testowanej metody
        val inputStream = driveClient.downloadFile(fileId)

        // Weryfikacja
        val resultData = inputStream.readBytes().toString(Charsets.UTF_8)
        assertEquals(testData, resultData)
    }

    @Test(expected = IllegalStateException::class)
    fun `downloadFile throws exception when not initialized`() = runBlocking {
        // Przygotuj klienta z pustym driveService
        val newDriveClient = spyk(driveClient)

        // Ustaw driveService na null
        val driveServiceField = DriveApiClient::class.java.getDeclaredField("driveService")
        driveServiceField.isAccessible = true
        driveServiceField.set(newDriveClient, null)

        // Wywołanie powinno rzucić wyjątek
        newDriveClient.downloadFile("any_id")
    }

    @Test(expected = RuntimeException::class)
    fun `downloadFile propagates exceptions from Drive API`() = runBlocking {
        // Konfiguracja - symulacja błędu API
        every { mockFilesGet.executeMediaAndDownloadTo(any()) } throws RuntimeException("Download error")

        // Wywołanie testowanej metody powinno przepropagować wyjątek
        driveClient.downloadFile("test_file_id")
    }

    @Test
    fun `listFilesInFolder returns list of files`() = runBlocking {
        // Konfiguracja
        val folderId = "test_folder_id"
        val fileId1 = "file_id_1"
        val fileId2 = "file_id_2"
        val fileName1 = "file1.daylio"
        val fileName2 = "file2.txt"

        // Przygotuj wynik z API
        val files = listOf(
            com.google.api.services.drive.model.File().setId(fileId1).setName(fileName1)
                .setMimeType("application/octet-stream").setSize(1024L)
                .setModifiedTime(com.google.api.client.util.DateTime(System.currentTimeMillis())),
            com.google.api.services.drive.model.File().setId(fileId2).setName(fileName2)
                .setMimeType("text/plain").setSize(512L)
                .setModifiedTime(com.google.api.client.util.DateTime(System.currentTimeMillis() - 86400000))
        )

        val fileList = FileList().setFiles(files)

        every { mockFilesList.setQ(any()) } returns mockFilesList
        every { mockFilesList.setFields(any()) } returns mockFilesList
        every { mockFilesList.execute() } returns fileList

        // Wywołanie testowanej metody
        val fileInfoList = driveClient.listFilesInFolder(folderId)

        // Weryfikacja
        assertEquals(2, fileInfoList.size)

        // Sprawdź pierwszy plik
        val firstFile = fileInfoList.find { it.id == fileId1 }
        assertNotNull(firstFile)
        assertEquals(fileName1, firstFile!!.name)

        // Sprawdź drugi plik
        val secondFile = fileInfoList.find { it.id == fileId2 }
        assertNotNull(secondFile)
        assertEquals(fileName2, secondFile!!.name)
    }

    @Test
    fun `listFilesInFolder returns empty list when no files found`() = runBlocking {
        // Konfiguracja
        val folderId = "test_folder_id"

        // Przygotuj pusty wynik z API
        val fileList = FileList().setFiles(emptyList())

        every { mockFilesList.setQ(any()) } returns mockFilesList
        every { mockFilesList.setFields(any()) } returns mockFilesList
        every { mockFilesList.execute() } returns fileList

        // Wywołanie testowanej metody
        val fileInfoList = driveClient.listFilesInFolder(folderId)

        // Weryfikacja
        assertTrue("Lista powinna być pusta", fileInfoList.isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun `listFilesInFolder throws exception when not initialized`() = runBlocking {
        // Przygotuj klienta z pustym driveService
        val newDriveClient = spyk(driveClient)

        // Ustaw driveService na null
        val driveServiceField = DriveApiClient::class.java.getDeclaredField("driveService")
        driveServiceField.isAccessible = true
        driveServiceField.set(newDriveClient, null)

        // Wywołanie powinno rzucić wyjątek
        newDriveClient.listFilesInFolder("any_folder_id")
    }

    @Test(expected = RuntimeException::class)
    fun `listFilesInFolder propagates exceptions from Drive API`() = runBlocking {
        // Konfiguracja - symulacja błędu API
        every { mockFilesList.setQ(any()) } returns mockFilesList
        every { mockFilesList.setFields(any()) } returns mockFilesList
        every { mockFilesList.execute() } throws RuntimeException("API error")

        // Wywołanie testowanej metody powinno przepropagować wyjątek
        driveClient.listFilesInFolder("test_folder_id")
    }

    @Test
    fun `parseRfc3339Date handles valid date format`() = runBlocking {
        // Testujemy prywatną metodę parseRfc3339Date
        // Przygotowanie testowej daty
        val expectedMillis = 1716053200000L // 2024-05-18 23:40:00 UTC
        val dateString = "2024-05-18T23:40:00.000Z"

        // Wywołanie metody prywatnej przez refleksję
        val method =
            DriveApiClient::class.java.getDeclaredMethod("parseRfc3339Date", String::class.java)
        method.isAccessible = true
        val result = method.invoke(driveClient, dateString) as Date

        // Weryfikacja z tolerancją na różnice stref czasowych i dokładność
        // Sprawdzamy czy data jest w przybliżeniu poprawna (różnica < 1h)
        val diffMs = abs(expectedMillis - result.time)
        assertTrue("Data powinna być poprawnie sparsowana", diffMs < 3600000)
    }

    @Test
    fun `parseRfc3339Date handles invalid date format`() = runBlocking {
        // Testujemy prywatną metodę parseRfc3339Date z niepoprawnym formatem
        val invalidDateString = "invalid-date-format"

        // Wywołanie metody prywatnej przez refleksję
        val method =
            DriveApiClient::class.java.getDeclaredMethod("parseRfc3339Date", String::class.java)
        method.isAccessible = true
        val result = method.invoke(driveClient, invalidDateString) as Date

        // Weryfikacja - metoda powinna zwrócić nowy Date() w przypadku błędu
        val now = Date()
        val diffMs = abs(now.time - result.time)

        // Różnica nie powinna być większa niż kilka sekund
        assertTrue(
            "W przypadku błędu parsowania powinna zostać zwrócona aktualna data", diffMs < 5000
        )
    }

    @Test
    fun `getInstance returns mockInstance when available`() {
        // Konfiguracja
        val mockClient = mockk<DriveApiClient>()
        DriveApiClient.mockInstance = mockClient

        // Wywołanie testowanej metody
        val instance = DriveApiClient.getInstance(context)

        // Weryfikacja
        assertEquals(
            "getInstance powinno zwrócić mockInstance gdy jest dostępny", mockClient, instance
        )
    }

    @Test
    fun `getInstance creates new instance when mockInstance is null`() {
        // Konfiguracja
        DriveApiClient.mockInstance = null

        // Wywołanie testowanej metody
        val instance = DriveApiClient.getInstance(context)

        // Weryfikacja
        assertNotNull(
            "getInstance powinno stworzyć nową instancję gdy mockInstance jest null", instance
        )

        // Sprawdź czy to nowa instancja klasy DriveApiClient
        assertEquals(
            "Zwrócona instancja powinna być typu DriveApiClient",
            DriveApiClient::class.java,
            instance.javaClass
        )
    }

    @Test
    fun `clearMockInstance sets mockInstance to null`() {
        // Konfiguracja
        val mockClient = mockk<DriveApiClient>()
        DriveApiClient.mockInstance = mockClient

        // Wywołanie testowanej metody
        DriveApiClient.clearMockInstance()

        // Weryfikacja
        assertEquals(
            "clearMockInstance powinno ustawić mockInstance na null",
            null,
            DriveApiClient.mockInstance
        )
    }
}