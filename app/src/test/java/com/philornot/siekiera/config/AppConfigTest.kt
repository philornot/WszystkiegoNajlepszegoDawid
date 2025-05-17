package com.philornot.siekiera.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class AppConfigTest {

    private lateinit var context: Context
    private lateinit var appConfig: AppConfig
    private val warsawTimeZone = TimeZone.getTimeZone("Europe/Warsaw")

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        appConfig = AppConfig.getInstance(context)
    }

    @After
    fun tearDown() {
        // Resetowanie singletona
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
    }

    @Test
    fun `singleton getInstance returns same instance`() {
        val instance1 = AppConfig.getInstance(context)
        val instance2 = AppConfig.getInstance(context)

        // Powinny to być te same instancje (singleton)
        assertTrue("getInstance powinien zwracać tę samą instancję", instance1 === instance2)
    }

    @Test
    fun `getBirthdayDate returns correct date in Warsaw timezone`() {
        val birthdayDate = appConfig.getBirthdayDate()

        // Sprawdź strefę czasową
        assertEquals(warsawTimeZone, birthdayDate.timeZone)

        // Sprawdź wartości daty (z config.xml)
        assertEquals(2025, birthdayDate.get(Calendar.YEAR))
        assertEquals(
            Calendar.MAY,
            birthdayDate.get(Calendar.MONTH)
        ) // Miesiące są 0-based w Calendar
        assertEquals(14, birthdayDate.get(Calendar.DAY_OF_MONTH))
        assertEquals(16, birthdayDate.get(Calendar.HOUR_OF_DAY))
        assertEquals(34, birthdayDate.get(Calendar.MINUTE))
        assertEquals(0, birthdayDate.get(Calendar.SECOND))
        assertEquals(0, birthdayDate.get(Calendar.MILLISECOND))
    }

    @Test
    fun `getBirthdayTimeMillis returns same time as calendar`() {
        val birthdayCalendar = appConfig.getBirthdayDate()
        val birthdayMillis = appConfig.getBirthdayTimeMillis()

        assertEquals(birthdayCalendar.timeInMillis, birthdayMillis)
    }

    @Test
    fun `getDriveFolderId returns correct value from resources`() {
        val folderId = appConfig.getDriveFolderId()

        // Wartość z config.xml
        assertEquals("1oiohasnIeKrtbmX2NY7g9p9HJF82zmn7", folderId)
    }

    @Test
    fun `getServiceAccountFileName returns correct value from resources`() {
        val fileName = appConfig.getServiceAccountFileName()

        // Wartość z config.xml
        assertEquals("service_account", fileName)
    }

    @Test
    fun `getDaylioFileName returns correct value from resources`() {
        val fileName = appConfig.getDaylioFileName()

        // Wartość z config.xml
        assertEquals("dawid_pamiętnik.daylio", fileName)
    }

    @Test
    fun `isDailyFileCheckEnabled returns boolean from resources`() {
        val isEnabled = appConfig.isDailyFileCheckEnabled()

        // Wartość z config.xml
        assertTrue(isEnabled)
    }

    @Test
    fun `isBirthdayNotificationEnabled returns boolean from resources`() {
        val isEnabled = appConfig.isBirthdayNotificationEnabled()

        // Wartość z config.xml
        assertTrue(isEnabled)
    }

    @Test
    fun `getFileCheckIntervalHours returns integer from resources`() {
        val hours = appConfig.getFileCheckIntervalHours()

        // Wartość z config.xml
        assertEquals(24, hours)
    }

    @Test
    fun `isVerboseLoggingEnabled returns boolean from resources`() {
        val isEnabled = appConfig.isVerboseLoggingEnabled()

        // Wartość z config.xml
        assertTrue(isEnabled)
    }

    @Test
    fun `getDailyFileCheckWorkName returns string from resources`() {
        val workName = appConfig.getDailyFileCheckWorkName()

        // Wartość z config.xml
        assertEquals("daily_file_check", workName)
    }

    @Test
    @Config(qualifiers = "en")
    fun `config works correctly with different locale`() {
        // Ten test sprawdza, czy konfiguracja działa poprawnie
        // niezależnie od ustawień lokalizacji urządzenia

        val birthdayDate = appConfig.getBirthdayDate()

        // Data powinna być zawsze spójna, niezależnie od locale
        assertEquals(2025, birthdayDate.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, birthdayDate.get(Calendar.MONTH))
        assertEquals(14, birthdayDate.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `handle configuration changes correctly`() {
        // Ten test sprawdza czy konfiguracja jest zachowywana poprawnie
        // po zmianach konfiguracji urządzenia (np. obrót ekranu)

        // Pobierz początkową konfigurację
        val initialConfig = AppConfig.getInstance(context)
        val initialDate = initialConfig.getBirthdayDate()

        // Symuluj zmianę konfiguracji
        val field = AppConfig::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)

        // Pobierz konfigurację po "zmianie"
        val newConfig = AppConfig.getInstance(context)
        val newDate = newConfig.getBirthdayDate()

        // Daty powinny być takie same
        assertEquals(initialDate.timeInMillis, newDate.timeInMillis)
    }
}