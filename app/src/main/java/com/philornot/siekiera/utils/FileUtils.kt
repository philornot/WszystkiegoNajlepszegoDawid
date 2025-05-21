package com.philornot.siekiera.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

/** Klasa pomocnicza do operacji na plikach. */
object FileUtils {
    /**
     * Sprawdza, czy plik istnieje w publicznym folderze Pobrane.
     *
     * @param fileName Nazwa pliku
     * @return true jeśli plik istnieje i ma rozmiar większy niż 0, false w
     *    przeciwnym wypadku
     */
    fun isFileInPublicDownloads(fileName: String): Boolean {
        val downloadDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val file = File(downloadDir, fileName)
        val exists = file.exists() && file.length() > 0

        if (exists) {
            Timber.d("Znaleziono istniejący plik w Downloads: $fileName (rozmiar: ${file.length()} bajtów)")
        }

        return exists
    }

    /**
     * Zwraca listę plików w folderze Pobrane odpowiadających podanemu wzorcowi
     * nazwy.
     *
     * @param pattern Wzorzec dla nazwy pliku (używa String.matches())
     * @return Lista znalezionych plików
     */
    fun getFilesInDownloads(pattern: String): List<File> {
        val downloadDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )

        if (!downloadDir.exists() || !downloadDir.isDirectory) {
            return emptyList()
        }

        return downloadDir.listFiles { file ->
            file.isFile && file.name.matches(Regex(pattern))
        }?.toList() ?: emptyList()
    }

    /**
     * Usuwa wszystkie pliki z folderu Pobrane pasujące do wzorca, z wyjątkiem
     * najnowszego. Przydatne do czyszczenia duplikatów.
     *
     * @param pattern Wzorzec dla nazwy pliku (używa String.matches())
     * @return Liczba usuniętych plików
     */
    fun cleanupDuplicates(pattern: String): Int {
        val files = getFilesInDownloads(pattern)

        if (files.size <= 1) {
            return 0
        }

        // Sortuj po dacie modyfikacji malejąco
        val sortedFiles = files.sortedByDescending { it.lastModified() }

        // Zachowaj najnowszy, usuń resztę
        var deletedCount = 0
        for (i in 1 until sortedFiles.size) {
            if (sortedFiles[i].delete()) {
                Timber.d("Usunięto duplikat: ${sortedFiles[i].name}")
                deletedCount++
            } else {
                Timber.e("Nie udało się usunąć pliku: ${sortedFiles[i].name}")
            }
        }

        return deletedCount
    }

    /**
     * Oblicza sumę kontrolną MD5 dla pliku. Może być użyta do porównania, czy
     * dwa pliki są identyczne.
     *
     * @param file Plik, dla którego należy obliczyć sumę kontrolną
     * @return Suma kontrolna MD5 jako string heksadecymalny lub null w
     *    przypadku błędu
     */
    fun calculateMD5(file: File): String? {
        try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }

            // Konwersja tablicy bajtów na string heksadecymalny
            val md5Bytes = md.digest()
            return md5Bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas obliczania sumy kontrolnej MD5 dla pliku: ${file.name}")
            return null
        }
    }

    /**
     * Znajduje konkretny plik w MediaStore na podstawie nazwy. Przydatne do
     * uzyskania URI dla istniejących plików.
     *
     * @param context Kontekst aplikacji
     * @param fileName Nazwa pliku do znalezienia
     * @return URI pliku lub null jeśli nie znaleziono
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun findFileInMediaStore(context: Context, fileName: String): Uri? {
        val contentResolver = context.contentResolver

        // Dla Androida 10+ używamy odpytania MediaStore
        val queryUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME
        )
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        contentResolver.query(
            queryUri, projection, selection, selectionArgs, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val id = cursor.getLong(idColumn)
                return Uri.withAppendedPath(queryUri, id.toString())
            }
        }

        return null
    }
}