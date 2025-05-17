package com.philornot.siekiera.utils

/**
 * Produkcyjna implementacja TimeProvider, która używa rzeczywistego czasu
 * systemowego w strefie czasowej systemu.
 */
class RealTimeProvider : TimeProvider {
    override fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
}