package com.philornot.siekiera.ui.screens.main.gift

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.ui.screens.main.curtain.CurtainSection
import com.philornot.siekiera.utils.FileUtils
import timber.log.Timber

/**
 * Ekran prezentu dostępny z szufladki nawigacyjnej. Pozwala na pobranie
 * prezentu nawet po jego pierwszym odebraniu. Dodano obsługę long press i
 * ulepszone otwieranie folderu Pobrane używając Storage Access Framework.
 * Sprawdza rzeczywiste istnienie pliku przed pokazaniem komunikatu.
 *
 * @param onGiftClicked Callback wywoływany po kliknięciu lub przytrzymaniu
 *    prezentu
 * @param giftReceived Czy prezent został już pobrany (flaga w
 *    SharedPreferences)
 * @param modifier Modyfikator dla całego ekranu
 */
@Composable
fun GiftScreen(
    onGiftClicked: () -> Unit,
    giftReceived: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Stan sprawdzający czy plik rzeczywiście istnieje
    var fileActuallyExists by remember { mutableStateOf(false) }
    var isCheckingFile by remember { mutableStateOf(true) }

    // Sprawdź przy załadowaniu czy plik rzeczywiście istnieje w folderze Pobrane
    LaunchedEffect(Unit) {
        try {
            val appConfig = AppConfig.getInstance(context)
            val fileName = appConfig.getDaylioFileName()
            fileActuallyExists = FileUtils.isFileInPublicDownloads(fileName)
            Timber.d("Sprawdzenie istnienia pliku '$fileName': $fileActuallyExists")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas sprawdzania istnienia pliku")
            fileActuallyExists = false
        } finally {
            isCheckingFile = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nagłówek dla ekranu prezentu
        Text(
            text = "Twój Prezent",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        // Sekcja prezentu - zawsze pokazujemy prezent (isTimeUp=true)
        CurtainSection(
            isTimeUp = true, showGift = true, onGiftClicked = { _, _ ->
            // Bezpośrednio wywołaj dialog pobierania
            onGiftClicked()
        }, onGiftLongPressed = {
            // Long press robi to samo co kliknięcie
            Timber.d("Długie naciśnięcie prezentu w GiftScreen")
            onGiftClicked()
        }, giftReceived = true, isInGiftScreen = true, // Informujemy, że to GiftScreen
            modifier = Modifier.weight(1f)
        )

        // Instrukcja dla użytkownika
        if (!isCheckingFile) {
            val instructionText = buildAnnotatedString {
                if (giftReceived && fileActuallyExists) {
                    append("Kliknij prezent, aby ponownie pobrać plik z eksportem Daylio")
                } else {
                    append("Kliknij prezent, aby pobrać plik z eksportem Daylio")
                }
            }

            Text(
                text = instructionText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Informacja o wcześniejszym pobraniu z linkiem do folderu Pobrane
            // TYLKO gdy plik rzeczywiście istnieje
            if (giftReceived && fileActuallyExists) {
                Spacer(modifier = Modifier.height(8.dp))

                val downloadText = buildAnnotatedString {
                    append("Prezent został już wcześniej pobrany. Sprawdź folder ")

                    // Wyróżniony link do folderu Pobrane
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Pobrane")
                    }

                    append(" w swoim urządzeniu.")
                }

                Text(
                    text = downloadText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        openDownloadsFolder(context)
                    })
            }
        }
    }
}

/**
 * Otwiera folder Pobrane używając zalecanego podejścia Storage Access
 * Framework z fallbackami dla różnych wersji Androida.
 */
private fun openDownloadsFolder(context: Context) {
    try {
        Timber.d("Próbuję otworzyć folder Pobrane")

        // Metoda 1: Storage Access Framework (zalecana dla nowszych wersji)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val storageManager =
                    context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()

                // Ustawienie początkowego katalogu na Downloads
                val initialUri =
                    intent.getParcelableExtra("android.provider.extra.INITIAL_URI") as? android.net.Uri
                if (initialUri != null) {
                    var scheme = initialUri.toString()
                    scheme = scheme.replace("/root/", "/document/")
                    scheme += "%3ADownload" // Kodowanie URL dla "Download"
                    val uri = scheme.toUri()
                    intent.putExtra("android.provider.extra.INITIAL_URI", uri)
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(context, intent, null)
                Timber.d("Otworzono folder Pobrane przez Storage Access Framework")
                return
            } catch (e: Exception) {
                Timber.w(e, "Storage Access Framework nie zadziałał, próbuję fallback")
            }
        }

        // Metoda 2: Intent dla DownloadManager (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(context, intent, null)
                Timber.d("Otworzono Downloads przez DownloadManager.ACTION_VIEW_DOWNLOADS")
                return
            } catch (e: Exception) {
                Timber.w(e, "DownloadManager nie zadziałał, próbuję następny fallback")
            }
        }

        // Metoda 3: Ogólny menedżer plików z kategorią BROWSABLE
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            startActivity(context, intent, null)
            Timber.d("Otworzono menedżer plików przez ACTION_GET_CONTENT")
            return
        } catch (e: Exception) {
            Timber.w(e, "ACTION_GET_CONTENT nie zadziałał, próbuję ostatni fallback")
        }

        // Metoda 4: Próba otwarcia ogólnego menedżera plików
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                type = "resource/folder"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(context, intent, null)
            Timber.d("Otworzono menedżer plików przez ACTION_VIEW z typem folder")
            return
        } catch (e: Exception) {
            Timber.w(e, "Ostatni fallback nie zadziałał")
        }

        // Metoda 5: Próba otwarcia konkretnej aplikacji menedżera plików
        try {
            // Próbuj otworzyć Google Files (com.google.android.apps.nbu.files)
            val packageManager = context.packageManager
            val intent =
                packageManager.getLaunchIntentForPackage("com.google.android.apps.nbu.files")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(context, intent, null)
                Timber.d("Otworzono Google Files")
                return
            }
        } catch (e: Exception) {
            Timber.w(e, "Google Files nie dostępne")
        }

        // Metoda 6: Ostatni fallback - otwórz ustawienia przechowywania
        try {
            val intent = Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(context, intent, null)
            Timber.d("Otworzono ustawienia przechowywania jako ostatni fallback")
        } catch (e: Exception) {
            Timber.e(e, "Wszystkie metody otwierania folderu zawiodły")

            // Jeśli nic nie zadziałało, spróbuj podstawowego ACTION_VIEW z dokumentami
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType("content://downloads/all_downloads".toUri(), "text/plain")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(context, intent, null)
                Timber.d("Ostatnia próba przez content://downloads/all_downloads")
            } catch (finalException: Exception) {
                Timber.e(finalException, "Definitywnie nie udało się otworzyć folderu Pobrane")
            }
        }

    } catch (e: Exception) {
        Timber.e(e, "Ogólny błąd podczas otwierania folderu Pobrane")
    }
}