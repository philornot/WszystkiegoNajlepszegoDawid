package com.philornot.siekiera.ui.screens.main.shared

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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.ui.screens.main.birthday.CurtainSection
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

            // Informacja o wcześniejszym pobraniu
            // TYLKO gdy plik rzeczywiście istnieje
            if (giftReceived && fileActuallyExists) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Prezent został już wcześniej pobrany. Sprawdź folder Pobrane w swoim urządzeniu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
