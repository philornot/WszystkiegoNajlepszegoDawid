package com.philornot.siekiera.utils

import android.os.Environment
import java.io.File

/** Klasa pomocnicza do operacji na plikach. */
object FileUtils {
    /**
     * Sprawdza, czy plik istnieje w publicznym folderze Pobrane.
     *
     * @param fileName Nazwa pliku
     * @return true jeśli plik istnieje, false w przeciwnym wypadku
     */
    fun isFileInPublicDownloads(fileName: String): Boolean {
        val downloadDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val file = File(downloadDir, fileName)
        return file.exists() && file.length() > 0
    }

}