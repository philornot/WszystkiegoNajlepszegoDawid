package com.philornot.siekiera.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Zaktualizowany główny ekran aplikacji, połączony z nowymi komponentami.
 * Zawiera animowane tło, sekcję nagłówka, kurtyną/prezentem,
 * odliczaniem oraz dodatkową sekcją życzeń po odblokowaniu.
 *
 * @param targetDate Data urodzin w milisekundach
 * @param currentTime Aktualny czas (domyślnie pobierany z systemu)
 * @param onGiftClicked Funkcja wywoływana po kliknięciu prezentu
 * @param timeProvider Dostawca czasu (używany głównie w testach)
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    targetDate: Long,
    currentTime: Long = System.currentTimeMillis(),
    onGiftClicked: () -> Unit,
) {
    // Oblicz czy czas już minął
    var isTimeUp by remember { mutableStateOf(currentTime >= targetDate) }

    // Stan dla aktualnego czasu (będzie aktualizowany co sekundę)
    var currentTimeState by remember { mutableLongStateOf(currentTime) }
    var timeRemaining by remember { mutableLongStateOf((targetDate - currentTimeState).coerceAtLeast(0)) }

    // Stan pokazywania ekranu z życzeniami (po kliknięciu prezentu)
    var showWishes by remember { mutableStateOf(false) }

    // Aktualizuj czas co sekundę
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTimeState = System.currentTimeMillis()
            timeRemaining = (targetDate - currentTimeState).coerceAtLeast(0)

            // Sprawdź czy czas minął
            if (currentTimeState >= targetDate && !isTimeUp) {
                isTimeUp = true
            }
        }
    }

    // Główny kontener
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Efekt animowanego tła
        AnimatedBackground(isTimeUp = isTimeUp)

        // Efekt iskier (tylko po upływie czasu)
        SparkleEffect(isVisible = isTimeUp)

        // Powierzchnia z główną zawartością
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
        ) {
            // Główna zawartość
            Box(modifier = Modifier.fillMaxSize()) {
                // Sekcja z ekranem oczekiwania (kurtyna i licznik)
                AnimatedVisibility(
                    visible = !showWishes, enter = fadeIn(), exit = fadeOut(
                        animationSpec = tween(durationMillis = 500)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Nagłówek z tytułem
                        HeaderSection()

                        // Sekcja kurtyny lub prezentu
                        CurtainSection(
                            isTimeUp = isTimeUp, onGiftClicked = {
                                // Pokaż ekran życzeń po kliknięciu prezentu
                                if (isTimeUp) {
                                    showWishes = true
                                    onGiftClicked()
                                }
                            }, modifier = Modifier.weight(1f)
                        )

                        // Sekcja licznika czasu
                        CountdownSection(
                            timeRemaining = timeRemaining,
                            isTimeUp = isTimeUp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Efekt konfetti gdy czas się skończy
                        if (isTimeUp) {
                            ConfettiEffect()
                        }
                    }
                }

                // Sekcja z życzeniami (po kliknięciu prezentu)
                BirthdayWishesSection(
                    isVisible = showWishes, modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}