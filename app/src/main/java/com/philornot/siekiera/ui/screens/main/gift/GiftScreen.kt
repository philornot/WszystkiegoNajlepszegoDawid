package com.philornot.siekiera.ui.screens.main.gift

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.philornot.siekiera.ui.screens.main.curtain.CurtainSection
import timber.log.Timber

/**
 * Ekran prezentu dostępny z szufladki nawigacyjnej. Pozwala na pobranie
 * prezentu nawet po jego pierwszym odebraniu. Dodano obsługę long press i
 * link do folderu Pobrane.
 *
 * @param onGiftClicked Callback wywoływany po kliknięciu lub przytrzymaniu
 *    prezentu
 * @param giftReceived Czy prezent został już pobrany
 * @param modifier Modyfikator dla całego ekranu
 */
@Composable
fun GiftScreen(
    onGiftClicked: () -> Unit,
    giftReceived: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

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

        // Instrukcja dla użytkownika z linkiem do folderu Pobrane
        val instructionText = buildAnnotatedString {
            if (giftReceived) {
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
        if (giftReceived) {
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
                    // Otwórz folder Pobrane
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                "content://downloads/all_downloads".toUri(), "*/*"
                            )
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(context, intent, null)
                        Timber.d("Otwarto folder Pobrane")
                    } catch (e: Exception) {
                        Timber.e(e, "Nie udało się otworzyć folderu Pobrane")

                        // Fallback - spróbuj otworzyć menedżer plików
                        try {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                type = "resource/folder"
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(context, fallbackIntent, null)
                        } catch (fallbackException: Exception) {
                            Timber.e(fallbackException, "Fallback również nie zadziałał")
                        }
                    }
                })
        }
    }
}