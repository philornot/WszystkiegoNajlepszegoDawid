package com.philornot.siekiera.utils

/**
 * Interfejs dla dostarczyciela czasu. Umożliwia łatwe testowanie
 * komponentów zależnych od czasu poprzez wstrzykiwanie symulowanego czasu
 * w testach.
 */
interface TimeProvider {
    /**
     * Zwraca aktualny czas w milisekundach. W przypadku produkcyjnej
     * implementacji zwraca rzeczywisty czas systemowy. W przypadku
     * testowej implementacji może zwracać symulowany czas.
     */
    fun getCurrentTimeMillis(): Long
}