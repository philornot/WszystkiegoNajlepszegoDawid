package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Komponent odpowiedzialny za nawigację w MainScreen. Obsługuje szufladkę
 * nawigacyjną dostępną po dacie urodzin lub gdy dzisiaj są urodziny.
 * Ukrywa zakładkę "Prezent" gdy dzisiaj są urodziny.
 */
@Composable
fun MainScreenNavigation(
    modifier: Modifier = Modifier,
    isTimeUp: Boolean,
    isTodayBirthday: Boolean,
    isDrawerOpen: Boolean,
    currentSection: NavigationSection,
    onDrawerStateChange: (Boolean) -> Unit,
    onSectionSelected: (NavigationSection) -> Unit,
) {
    // Navigation drawer - dostępna po dacie urodzin lub gdy dzisiaj są urodziny
    if (isTimeUp || isTodayBirthday) {
        NavigationDrawer(
            modifier = modifier,
            isOpen = isDrawerOpen,
            onOpenStateChange = onDrawerStateChange,
            currentSection = currentSection,
            onSectionSelected = onSectionSelected,
            hideGiftSection = isTodayBirthday // Ukryj zakładkę Prezent gdy dzisiaj urodziny
        )
    }
}