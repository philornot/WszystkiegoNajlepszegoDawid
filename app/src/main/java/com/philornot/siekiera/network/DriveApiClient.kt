package com.philornot.siekiera.network

import android.content.Context
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials
import com.philornot.siekiera.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Klient API do komunikacji z Google Drive używający konta usługi (Service
 * Account).
 *
 * Nie wymaga interakcji użytkownika ani logowania - używa predefiniowanych
 * poświadczeń konta usługi, które ma dostęp do określonego folderu Google
 * Drive.
 */
class DriveApiClient(context: Context) {
    // Używamy WeakReference aby uniknąć memory leak
    private val contextRef = WeakReference(context.applicationContext)
    private var driveService: Drive? = null

    // Funkcja pomocnicza do uzyskania kontekstu
    private fun getContext(): Context? = contextRef.get()

    // Getter dla AppConfig - nie przechowujemy referencji, tylko pobieramy w razie potrzeby
    private fun getAppConfig(): AppConfig? {
        val context = getContext() ?: return null
        return AppConfig.getInstance(context)
    }

    /**
     * Inicjalizuje klienta Google Drive API. Musi być wywołane przed innymi
     * metodami.
     *
     * @return true jeśli inicjalizacja się powiodła, false w przeciwnym
     *    wypadku
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = getContext() ?: return@withContext false
            val appConfig = getAppConfig() ?: return@withContext false

            // Konfiguracja transportu HTTP
            val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

            // Pobierz nazwę pliku service account z konfiguracji
            val serviceAccountFileName = appConfig.getServiceAccountFileName()

            // Pobierz poświadczenia z zasobów aplikacji
            val serviceAccountStream = context.resources.openRawResource(
                context.resources.getIdentifier(serviceAccountFileName, "raw", context.packageName)
            )

            // Skonfiguruj poświadczenia konta usługi
            val credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                .createScoped(listOf(DriveScopes.DRIVE_READONLY))

            // Utwórz usługę Drive API
            driveService =
                Drive.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME).build()

            true
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas inicjalizacji klienta Drive API")
            false
        }
    }

    /**
     * Pobiera informacje o pliku z Google Drive.
     *
     * @param fileId ID pliku na Google Drive
     * @return Informacje o pliku
     * @throws Exception jeśli wystąpi błąd
     */
    suspend fun getFileInfo(fileId: String): FileInfo = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            Timber.d("Pobieranie informacji o pliku o ID: $fileId")

            val file =
                driveService.files().get(fileId).setFields("id, name, mimeType, size, modifiedTime")
                    .execute()

            // POPRAWKA: Jawnie rzutujemy wartość size na Long lub używamy 0L jako domyślnej wartości
            val fileSize: Long = if (file.size != null) file.size.toLong() else 0L

            FileInfo(
                id = file.id,
                name = file.name,
                mimeType = file.mimeType,
                size = fileSize,
                modifiedTime = parseRfc3339Date(file.modifiedTime.toStringRfc3339())
            )
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas pobierania informacji o pliku: $fileId")
            throw e
        }
    }

    /**
     * Pobiera zawartość pliku z Google Drive.
     *
     * @param fileId ID pliku na Google Drive
     * @return Strumień z zawartością pliku
     * @throws Exception jeśli wystąpi błąd
     */
    suspend fun downloadFile(fileId: String): InputStream = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            Timber.d("Pobieranie pliku o ID: $fileId")

            val outputStream = java.io.ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            ByteArrayInputStream(outputStream.toByteArray())
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas pobierania pliku: $fileId")
            throw e
        }
    }

    /**
     * Sprawdza pliki w określonym folderze Google Drive.
     *
     * @param folderId ID folderu na Google Drive
     * @return Lista informacji o plikach w folderze
     * @throws Exception jeśli wystąpi błąd
     */
    suspend fun listFilesInFolder(folderId: String): List<FileInfo> = withContext(Dispatchers.IO) {
        val driveService = this@DriveApiClient.driveService
            ?: throw IllegalStateException("Klient Drive API nie został zainicjalizowany")

        try {
            Timber.d("Listowanie plików w folderze: $folderId")

            // Zapytanie o pliki w określonym folderze
            val query = "'$folderId' in parents and trashed = false"
            val result = driveService.files().list().setQ(query)
                .setFields("files(id, name, mimeType, size, modifiedTime)").execute()

            result.files.map { file ->
                // POPRAWKA: Jawnie rzutujemy wartość size na Long lub używamy 0L jako domyślnej wartości
                val fileSize: Long = if (file.size != null) file.size.toLong() else 0L

                FileInfo(
                    id = file.id,
                    name = file.name,
                    mimeType = file.mimeType,
                    size = fileSize,
                    modifiedTime = parseRfc3339Date(file.modifiedTime.toStringRfc3339())
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas listowania plików w folderze: $folderId")
            throw e
        }
    }

    /** Parsuje datę w formacie RFC 3339 używanym przez Google API. */
    private fun parseRfc3339Date(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return try {
            format.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Timber.e(e, "Błąd parsowania daty: $dateString")
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

        // Do testów - pozwala na wstrzyknięcie mocka
        @JvmStatic
        internal var mockInstance: DriveApiClient? = null

        /**
         * Pobiera instancję klienta Drive API. W testach zwraca zaślepkę, w
         * produkcji tworzy nową instancję.
         *
         * @param context Kontekst aplikacji
         * @return Instancja DriveApiClient
         */
        @JvmStatic
        fun getInstance(context: Context): DriveApiClient {
            return mockInstance ?: synchronized(this) {
                mockInstance ?: DriveApiClient(context.applicationContext).also {
                    // NIE przypisujemy mockInstance = it, ponieważ to spowodowałoby memory leak
                    // Używamy mockInstance tylko do testów, nie w kodzie produkcyjnym
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