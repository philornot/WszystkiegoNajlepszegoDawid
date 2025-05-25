package com.philornot.siekiera.ui.screens.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Komponent odpowiedzialny za nawigację w MainScreen. Obsługuje szufladkę
 * nawigacyjną dostępną tylko po dacie urodzin.
 */
@Composable
fun MainScreenNavigation(
    modifier: Modifier = Modifier,
    isTimeUp: Boolean,
    isDrawerOpen: Boolean,
    currentSection: NavigationSection,
    onDrawerStateChange: (Boolean) -> Unit,
    onSectionSelected: (NavigationSection) -> Unit,
) {
    // Navigation drawer - dostępna TYLKO po dacie urodzin
    if (isTimeUp) {
        NavigationDrawer(
            modifier = modifier,
            isOpen = isDrawerOpen,
            onOpenStateChange = onDrawerStateChange,
            currentSection = currentSection,
            onSectionSelected = onSectionSelected
        )
    }
}