package com.philornot.siekiera.utils

/**
 * Testowa implementacja TimeProvider, która pozwala na ustawienie
 * symulowanego czasu. Używana w testach do symulowania określonych dat i
 * czasów.
 */
open class TestTimeProvider : TimeProvider {
    private var mockedTime: Long? = null

    override fun getCurrentTimeMillis(): Long {
        return mockedTime ?: System.currentTimeMillis()
    }

    /**
     * Ustawia symulowany czas, który będzie zwracany przez
     * getCurrentTimeMillis().
     *
     * @param timeMillis Czas w milisekundach, który ma być symulowany
     */
    fun setCurrentTimeMillis(timeMillis: Long) {
        mockedTime = timeMillis
    }

    /**
     * Resetuje symulowany czas, powracając do używania rzeczywistego czasu
     * systemowego.
     */
    fun resetToSystemTime() {
        mockedTime = null
    }
}