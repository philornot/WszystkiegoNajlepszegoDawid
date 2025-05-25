package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Komponent odpowiedzialny za nawigację w MainScreen. Obsługuje szufladkę
 * nawigacyjną dostępną po dacie urodzin, gdy dzisiaj są urodziny, lub gdy
 * urodziny już były w tym roku.
 *
 * Logika ukrywania zakładki "Prezent":
 * - Ukryj gdy dzisiaj są urodziny (isTodayBirthday = true)
 * - Pokaż w pozostałych przypadkach gdy drawer jest dostępny
 */
@Composable
fun MainScreenNavigation(
    modifier: Modifier = Modifier,
    isTimeUp: Boolean,
    isTodayBirthday: Boolean,
    isBirthdayPastThisYear: Boolean,
    isDrawerOpen: Boolean,
    currentSection: NavigationSection,
    onDrawerStateChange: (Boolean) -> Unit,
    onSectionSelected: (NavigationSection) -> Unit,
) {
    // Drawer jest dostępny gdy:
    // - Czas upłynął (normalne zakończenie odliczania lub dzisiaj urodziny)
    // - LUB urodziny już były w tym roku (ale nie dzisiaj) - wtedy można odebrać prezent ponownie
    val isDrawerAvailable = isTimeUp || isTodayBirthday || isBirthdayPastThisYear

    if (isDrawerAvailable) {
        NavigationDrawer(
            modifier = modifier,
            isOpen = isDrawerOpen,
            onOpenStateChange = onDrawerStateChange,
            currentSection = currentSection,
            onSectionSelected = onSectionSelected,
            hideGiftSection = isTodayBirthday // Ukryj zakładkę Prezent tylko gdy dzisiaj urodziny
        )
    }
}