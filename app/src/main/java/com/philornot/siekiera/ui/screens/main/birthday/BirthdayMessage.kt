package com.philornot.siekiera.ui.screens.main.birthday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Prosta wiadomość urodzinowa pokazywana po kliknięciu prezentu. */
@Composable
fun BirthdayMessage(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        modifier = modifier.padding(24.dp)
    ) {
        // Zawartość wiadomości
        Box(
            modifier = Modifier.Companion.weight(1f), contentAlignment = Alignment.Companion.Center
        ) {
            Column(
                horizontalAlignment = Alignment.Companion.CenterHorizontally
            ) {
                // Duży tekst 18
                Text(
                    text = "hej Dawid!",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.Companion.padding(bottom = 32.dp)
                )

                // Życzenia urodzinowe
                Text(
                    text = "Sorki, że tyle musiałeś czekać, ale uznałem że jednak im więcej wpisów będziesz mieć do przeczytania, tym lepiej.\n" + "A, no i w pewnie powinienem Ci dać prezent urodzinowy w urodziny, bo tradycje i w ogóle.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.Companion.padding(bottom = 16.dp)
                )

                Text(
                    text = "Na poprzednim ekranie było napisane coś w stylu, że 'udało ci się tyle przeżyć'.\n" + "Chciałbym mocno podkreślić, że to nie znaczy, że to już koniec.\n" + "Wytrzymaj jeszcze chwilkę.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.Companion.padding(bottom = 16.dp)
                )

                Text(
                    text = "Nie chcę tutaj dużo pisać, bo nie jestem w stanie tego tak łatwo zmienić jak wpis w daylio. Więc po prostu zaimportuj wpisy, tam na pewno ja z przyszłości Ci coś lepszego napiszę. Na pewno. Słyszysz Filip? W Daylio będą życzenia, tak?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Elastyczny odstęp dla lepszego układu
                Spacer(modifier = Modifier.Companion.weight(1f))

                Text(
                    text = "ten konkretny ekran będzie dostępny do północy\n(ale prezent będziesz mógł odebrać jeszcze raz)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Companion.Center,
                    modifier = Modifier.Companion.padding(bottom = 16.dp)
                )

                // Przycisk powrotu ze strzałką
                IconButton(
                    onClick = onBackClick, modifier = Modifier.Companion
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Wróć",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.Companion.size(28.dp)
                    )
                }
                Text(
                    text = "chcesz może pooglądać fajerwerki, nad którymi kompletnie nie spędziłem 3h...?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Companion.Center,
                    modifier = Modifier.Companion.padding(top = 10.dp)
                )

                Spacer(modifier = Modifier.Companion.height(8.dp))
            }
        }
    }
}