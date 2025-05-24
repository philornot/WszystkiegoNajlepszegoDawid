package com.philornot.siekiera.ui.screens.main.gift

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.philornot.siekiera.ui.screens.main.curtain.CurtainSection

/**
 * Ekran prezentu dostępny z szufladki nawigacyjnej. Pozwala na pobranie
 * prezentu nawet po jego pierwszym odebraniu.
 *
 * @param onGiftClicked Callback wywoływany po kliknięciu prezentu
 * @param giftReceived Czy prezent został już pobrany
 * @param modifier Modyfikator dla całego ekranu
 */
@Composable
fun GiftScreen(
    onGiftClicked: () -> Unit,
    giftReceived: Boolean,
    modifier: Modifier = Modifier,
) {
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
            }, giftReceived = true, modifier = Modifier.weight(1f)
        )

        // Instrukcja dla użytkownika
        Text(
            text = if (giftReceived) "Kliknij prezent, aby ponownie pobrać plik z eksportem Daylio"
            else "Kliknij prezent, aby pobrać plik z eksportem Daylio",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Informacja o wcześniejszym pobraniu
        if (giftReceived) {
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