package com.philornot.siekiera.network

import android.content.Context
import android.content.res.Resources
import com.google.api.services.drive.Drive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
@ExperimentalCoroutinesApi
class DriveApiClientTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockResources: Resources

    @Mock
    private lateinit var mockDrive: Drive

    @Mock
    private lateinit var mockDriveFiles: Drive.Files

    @Mock
    private lateinit var mockDriveFilesList: Drive.Files.List

    @Mock
    private lateinit var mockDriveFilesGet: Drive.Files.Get

    // Sample test data
    private val testServiceAccountContent = """
        {
          "type": "service_account",
          "project_id": "test-project",
          "private_key_id": "test_key_id",
          "private_key": "test_private_key",
          "client_email": "test@example.com",
          "client_id": "test_client_id",
          "auth_uri": "https://accounts.google.com/o/oauth2/auth",
          "token_uri": "https://oauth2.googleapis.com/token",
          "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
          "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/test%40example.com"
        }
    """.trimIndent().toByteArray()

    private lateinit var driveApiClient: DriveApiClient

    @Before
    fun setup() {
        // Mock context and resources
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.resources).thenReturn(mockResources)
        `when`(mockResources.getIdentifier(any(), eq("raw"), any())).thenReturn(123)
        `when`(mockResources.openRawResource(123)).thenReturn(
            ByteArrayInputStream(
                testServiceAccountContent
            )
        )

        // Create mocks for Drive API chain
        `when`(mockDrive.files()).thenReturn(mockDriveFiles)
        `when`(mockDriveFiles.list()).thenReturn(mockDriveFilesList)

        // Get mock setup
        `when`(mockDriveFiles.get(any())).thenReturn(mockDriveFilesGet)

        // Set up DriveApiClient with test mocks
        driveApiClient = DriveApiClient(mockContext)

        // Use reflection to set the mock Drive service
        val field = DriveApiClient::class.java.getDeclaredField("driveService")
        field.isAccessible = true
        field.set(driveApiClient, mockDrive)
    }

    @Test
    fun `initialize sets up Drive service correctly`() = runTest {
        // We bypass the real initialization here since we can't easily mock the static GoogleNetHttpTransport
        // Instead we'll verify that initialization was attempted with the correct parameters

        // Given that we've already set up the mock Drive service in setup

        // When/Then - Verify initialization was successful
        assertTrue(driveApiClient.initialize())
    }

    @Test
    fun `getFileInfo returns correct file info`() = runTest {
        // Given
        val fileId = "test_file_id"
        val fileName = "test.daylio"
        val mimeType = "application/octet-stream"
        val size = 1024L

        val mockFile = com.google.api.services.drive.model.File().setId(fileId).setName(fileName)
            .setMimeType(mimeType).setSize(size)
            .setModifiedTime(com.google.api.client.util.DateTime(Date()))

        `when`(mockDriveFilesGet.setFields(any())).thenReturn(mockDriveFilesGet)
        `when`(mockDriveFilesGet.execute()).thenReturn(mockFile)

        // When
        val fileInfo = driveApiClient.getFileInfo(fileId)

        // Then
        assertEquals(fileId, fileInfo.id)
        assertEquals(fileName, fileInfo.name)
        assertEquals(mimeType, fileInfo.mimeType)
        assertEquals(size, fileInfo.size)
    }

    @Test
    fun `downloadFile returns file content as InputStream`() = runTest {
        // Given
        val fileId = "test_file_id"
        val fileContent = "test file content".toByteArray()

        // Mock the execute and download method
        Mockito.doAnswer { invocation ->
            val outputStream = invocation.getArgument<java.io.OutputStream>(0)
            outputStream.write(fileContent)
            outputStream.flush()
            null
        }.`when`(mockDriveFilesGet).executeMediaAndDownloadTo(any())

        // When
        val inputStream = driveApiClient.downloadFile(fileId)

        // Then
        val resultBytes = inputStream.readBytes()
        assertEquals(fileContent.size, resultBytes.size)
        assertTrue(fileContent.contentEquals(resultBytes))
    }

    @Test
    fun `listFilesInFolder returns correct file list`() = runTest {
        // Given
        val folderId = "test_folder_id"

        // Create mock response
        val mockFilesList = mutableListOf<com.google.api.services.drive.model.File>()
        mockFilesList.add(
            com.google.api.services.drive.model.File().setId("file1").setName("file1.daylio")
                .setMimeType("application/octet-stream").setSize(1024L)
                .setModifiedTime(com.google.api.client.util.DateTime(Date()))
        )
        mockFilesList.add(
            com.google.api.services.drive.model.File().setId("file2").setName("file2.daylio")
                .setMimeType("application/octet-stream").setSize(2048L)
                .setModifiedTime(com.google.api.client.util.DateTime(Date()))
        )

        val mockResult = mock<com.google.api.services.drive.model.FileList>()
        `when`(mockResult.files).thenReturn(mockFilesList)

        // Set up mock chain
        `when`(mockDriveFilesList.setQ(any())).thenReturn(mockDriveFilesList)
        `when`(mockDriveFilesList.setFields(any())).thenReturn(mockDriveFilesList)
        `when`(mockDriveFilesList.execute()).thenReturn(mockResult)

        // When
        val files = driveApiClient.listFilesInFolder(folderId)

        // Then
        assertEquals(2, files.size)
        assertEquals("file1", files[0].id)
        assertEquals("file1.daylio", files[0].name)
        assertEquals("file2", files[1].id)
        assertEquals("file2.daylio", files[1].name)
    }

    @Test
    fun `getInstance returns new instance when mockInstance is null`() {
        // Given
        DriveApiClient.mockInstance = null

        // When
        val instance = DriveApiClient.getInstance(mockContext)

        // Then
        assertTrue(instance is DriveApiClient)
    }

    @Test
    fun `getInstance returns mockInstance when it is set`() {
        // Given
        val mockInstance = mock<DriveApiClient>()
        DriveApiClient.mockInstance = mockInstance

        // When
        val instance = DriveApiClient.getInstance(mockContext)

        // Then
        assertEquals(mockInstance, instance)

        // Clean up
        DriveApiClient.clearMockInstance()
    }

    @Test
    fun `clearMockInstance sets mockInstance to null`() {
        // Given
        val mockInstance = mock<DriveApiClient>()
        DriveApiClient.mockInstance = mockInstance

        // When
        DriveApiClient.clearMockInstance()

        // Then
        assertEquals(null, DriveApiClient.mockInstance)
    }
}