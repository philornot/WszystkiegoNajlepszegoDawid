# Kompletny Przewodnik Testowania Aplikacji "Niespodzianka dla Dawida"

Ten przewodnik zawiera wszystko, co musisz wiedzieć, aby kompleksowo przetestować aplikację i mieć pewność, że zadziała bezbłędnie 24 sierpnia 2025 roku.

## Spis treści
1. [Organizacja projektu testowego](#1-organizacja-projektu-testowego)
2. [Konfiguracja środowiska](#2-konfiguracja-środowiska)
3. [Testy jednostkowe](#3-testy-jednostkowe)
4. [Testy interfejsu użytkownika](#4-testy-interfejsu-użytkownika)
5. [Symulacja czasu](#5-symulacja-czasu)
6. [Testowanie powiadomień](#6-testowanie-powiadomień)
7. [Testowanie pobierania plików](#7-testowanie-pobierania-plików)
8. [Testowanie po restarcie urządzenia](#8-testowanie-po-restarcie-urządzenia)
9. [Debugowanie testów](#9-debugowanie-testów)
10. [Testy przed wdrożeniem](#10-testy-przed-wdrożeniem)

## 1. Organizacja projektu testowego

### Struktura katalogów

Projekt jest zorganizowany zgodnie z najlepszymi praktykami dla aplikacji Android:

```
com.philornot.siekiera/
├── src/
│   ├── main/                               # Główny kod aplikacji
│   ├── test/                               # Testy jednostkowe (JVM)
│   │   └── kotlin/
│   │       └── com/philornot/siekiera/
│   │           ├── utils/                  # Narzędzia pomocnicze do testów
│   │           │   └── TimeTestUtils.kt    # Symulacja czasu w testach
│   │           ├── viewmodel/              # Testy ViewModeli
│   │           │   └── MainViewModelTest.kt
│   │           ├── notification/           # Testy powiadomień
│   │           │   └── NotificationSchedulerTest.kt
│   │           └── workers/                # Testy WorkManagera
│   │               └── FileCheckWorkerTest.kt
│   └── androidTest/                        # Testy instrumentalne (na urządzeniu)
│       └── kotlin/
│           └── com/philornot/siekiera/
│               ├── utils/                  # Narzędzia do testów UI
│               │   └── ComposeTestUtils.kt
│               └── ui/                     # Testy interfejsu
│                   └── MainScreenTest.kt
```

### Rodzaje testów

1. **Testy jednostkowe** (w katalogu `test/`) - uruchamiane na JVM, szybkie, bez emulatorów
   - Testy logiki biznesowej, ViewModeli, kalkulacji czasu, formatowania daty itp.

2. **Testy instrumentalne** (w katalogu `androidTest/`) - uruchamiane na urządzeniu/emulatorze
   - Testy UI (Compose)
   - Testy integracji z systemem Android (powiadomienia, praca w tle)
   - Testy pełnej funkcjonalności (end-to-end)

## 2. Konfiguracja środowiska

### Dodanie zależności testowych

W pliku `build.gradle.kts` (moduł) muszą być zdefiniowane następujące zależności:

```kotlin
// Compose Compiler Plugin - dodaj do sekcji plugins
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

// Zależności testowe
dependencies {
    // Dla testów jednostkowych (folder 'test')
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("app.cash.turbine:turbine:1.0.0") // Do testowania Flow
    
    // Dla testów instrumentalnych (folder 'androidTest')
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.compose.ui:ui-test:1.5.4")
    androidTestImplementation("androidx.work:work-testing:2.9.0")
    
    // Dla debugowania testów Compose
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}
```

### Przygotowanie emulatorów

Upewnij się, że masz skonfigurowane odpowiednie emulatory w Android Studio:

1. Otwórz Android Studio i przejdź do Device Manager
2. Kliknij "Create Device" i skonfiguruj emulator (np. Pixel 6 z Android 13)
3. Dla testów powiadomień i systemu użyj emulatora z Google Play Services

## 3. Testy jednostkowe

### Uruchamianie testów jednostkowych

```bash
# W terminalu:
./gradlew test

# Lub w Android Studio:
1. Przejdź do katalogu src/test
2. Kliknij prawym przyciskiem myszy
3. Wybierz "Run Tests in 'test'"
```

### Przykład testu ViewModel

Plik: `src/test/kotlin/com/philornot/siekiera/viewmodel/MainViewModelTest.kt`

```kotlin
@RunWith(JUnit4::class)
class MainViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel
    private lateinit var timeProvider: TestTimeProvider

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        timeProvider = TestTimeProvider()
        viewModel = MainViewModel(timeProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `time is calculated correctly before reveal date`() = runTest {
        // Symulacja daty 23 sierpnia 2025, 12:00
        val testDate = getCalendarForDateTime(2025, Calendar.AUGUST, 23, 12, 0, 0)
        timeProvider.setCurrentTimeMillis(testDate.timeInMillis)
        
        // Sprawdź czy zostało jeszcze 12 godzin do ujawnienia
        val expected = 12 * 60 * 60 * 1000L // 12 godzin w milisekundach
        val result = viewModel.getTimeRemaining()
        
        // Dozwolona mała różnica, bo testy mogą trwać kilka milisekund
        assertTrue(Math.abs(expected - result) < 1000)
    }
}
```

## 4. Testy interfejsu użytkownika

### Uruchamianie testów UI

```bash
# W terminalu:
./gradlew connectedAndroidTest

# Lub w Android Studio:
1. Upewnij się, że emulator jest uruchomiony
2. Przejdź do katalogu src/androidTest
3. Kliknij prawym przyciskiem myszy
4. Wybierz "Run Tests in 'androidTest'"
```

### Przykład testu Compose

Plik: `src/androidTest/kotlin/com/philornot/siekiera/ui/MainScreenTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val timeProvider = TestTimeProvider()

    @Test
    fun curtainIsVisibleBeforeRevealDate() {
        // GIVEN - Ustawienie daty przed ujawnieniem (23 sierpnia 2025)
        timeProvider.setCurrentTimeMillis(
            getCalendarForDateTime(2025, Calendar.AUGUST, 23, 12, 0, 0).timeInMillis
        )
        
        // WHEN - Renderowanie UI
        composeTestRule.setContent {
            MainScreen(
                targetDate = getRevealDateMillis(),
                onGiftClicked = {},
                timeProvider = timeProvider
            )
        }
        
        // THEN - Sprawdzenie widoczności kurtyny i licznika
        composeTestRule
            .onNodeWithTag("curtain")
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithTag("countdown")
            .assertIsDisplayed()
    }
}
```

## 5. Symulacja czasu

Testowanie aplikacji zależnej od konkretnej daty wymaga możliwości symulacji czasu.

### Klasa TimeProvider

Plik: `src/main/kotlin/com/philornot/siekiera/utils/TimeProvider.kt`

```kotlin
// Interfejs dla dostarczyciela czasu
interface TimeProvider {
    fun getCurrentTimeMillis(): Long
}

// Implementacja produkcyjna - używa rzeczywistego czasu systemowego
class RealTimeProvider : TimeProvider {
    override fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
}

// Implementacja testowa - umożliwia ustawienie symulowanego czasu
class TestTimeProvider : TimeProvider {
    private var mockedTime: Long? = null
    
    override fun getCurrentTimeMillis(): Long {
        return mockedTime ?: System.currentTimeMillis()
    }
    
    fun setCurrentTimeMillis(time: Long) {
        mockedTime = time
    }
    
    fun resetToSystemTime() {
        mockedTime = null
    }
}
```

### Symulacja różnych dat

```kotlin
// Utworzenie instancji do testów
val timeProvider = TestTimeProvider()

// Symulacja dnia przed urodzinami
timeProvider.setCurrentTimeMillis(
    getCalendarForDateTime(2025, Calendar.AUGUST, 23, 12, 0, 0).timeInMillis
)

// Symulacja dnia urodzin
timeProvider.setCurrentTimeMillis(
    getCalendarForDateTime(2025, Calendar.AUGUST, 24, 0, 0, 1).timeInMillis
)
```

### Testowanie przejścia między datami

```kotlin
@Test
fun curtainDisappearsAfterRevealDate() {
    // GIVEN - ustawienie daty przed północą
    val beforeMidnightDate = getCalendarForDateTime(2025, Calendar.AUGUST, 23, 23, 59, 59).timeInMillis
    timeProvider.setCurrentTimeMillis(beforeMidnightDate)
    
    // Wyświetlenie ekranu
    composeTestRule.setContent {
        MainScreen(
            targetDate = getRevealDateMillis(),
            onGiftClicked = {},
            timeProvider = timeProvider
        )
    }
    
    // Weryfikacja - kurtyna widoczna
    composeTestRule
        .onNodeWithTag("curtain")
        .assertIsDisplayed()
    
    // WHEN - ustawienie czasu po północy
    timeProvider.setCurrentTimeMillis(
        getCalendarForDateTime(2025, Calendar.AUGUST, 24, 0, 0, 1).timeInMillis
    )
    
    // Odświeżenie UI
    composeTestRule.mainClock.advanceTimeBy(1000) // Przesunięcie czasu w testach o 1 sekundę
    
    // THEN - kurtyna powinna zniknąć, a prezent powinien być widoczny
    composeTestRule
        .onNodeWithTag("curtain")
        .assertDoesNotExist()
        
    composeTestRule
        .onNodeWithTag("gift")
        .assertIsDisplayed()
}
```

## 6. Testowanie powiadomień

### Testowanie planowania powiadomień

Plik: `src/test/kotlin/com/philornot/siekiera/notification/NotificationSchedulerTest.kt`

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S]) // API 31 (Android 12)
class NotificationSchedulerTest {
    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = mock()
        whenever(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
    }

    @Test
    fun scheduleNotificationSetsAlarmForCorrectDate() {
        // Wywołanie metody, którą testujemy
        NotificationScheduler.scheduleGiftRevealNotification(context)
        
        // Sprawdzenie argumentów przekazanych do AlarmManager
        argumentCaptor<Long> {
            verify(alarmManager).setExactAndAllowWhileIdle(
                eq(AlarmManager.RTC_WAKEUP),
                capture(),
                any()
            )
            
            // Konwersja czasu do obiektu Calendar
            val calendar = Calendar.getInstance().apply {
                timeInMillis = firstValue
            }
            
            // Weryfikacja ustawionej daty
            assertEquals(2025, calendar.get(Calendar.YEAR))
            assertEquals(Calendar.AUGUST, calendar.get(Calendar.MONTH))
            assertEquals(24, calendar.get(Calendar.DAY_OF_MONTH))
            assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
            assertEquals(0, calendar.get(Calendar.MINUTE))
        }
    }
}
```

### Testowanie wyświetlania powiadomień na urządzeniu

Aby przetestować rzeczywiste powiadomienia:

1. Tymczasowo zmodyfikuj kod w `NotificationScheduler.kt`, aby zaplanować powiadomienie za 30 sekund zamiast czekać do 24 sierpnia 2025:

```kotlin
// Tymczasowa zmiana do testów
val calendar = Calendar.getInstance().apply {
    add(Calendar.SECOND, 30) // Powiadomienie za 30 sekund
}
```

2. Uruchom aplikację na emulatorze lub fizycznym urządzeniu
3. Zablokuj ekran lub zminimalizuj aplikację
4. Poczekaj 30 sekund i sprawdź czy powiadomienie się pojawia
5. Po testach przywróć oryginalny kod

## 7. Testowanie pobierania plików

### Konfiguracja testowego pliku

1. Utwórz plik testowy `.daylio`
2. Umieść go na swoim Google Drive i pobierz publiczny link
3. Umieść ten link w kodzie aplikacji

```kotlin
// W MainActivity.kt
private val fileUrl = "TWÓJ_LINK_DO_PLIKU"
```

### Test pobierania pliku

Plik: `src/androidTest/kotlin/com/philornot/siekiera/DownloadTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class DownloadTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun fileDownloadsCorrectly() {
        // GIVEN - symulacja daty po ujawnieniu
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            // Użyj timeProvider w MainActivity
            val testProvider = TestTimeProvider()
            testProvider.setCurrentTimeMillis(
                getCalendarForDateTime(2025, Calendar.AUGUST, 24, 12, 0, 0).timeInMillis
            )
            activity.timeProvider = testProvider
            
            // Wymuszenie odświeżenia UI
            activity.recreate()
        }
        
        // WHEN - kliknięcie przycisku pobrania
        onView(withId(R.id.downloadButton))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // Potwierdzenie w dialogu
        onView(withText(R.string.yes))
            .check(matches(isDisplayed()))
            .perform(click())
        
        // THEN - oczekiwanie na zakończenie pobierania i sprawdzenie pliku
        // Użyj IdlingResource lub CountDownLatch, aby zaczekać na zakończenie pobierania
        Thread.sleep(5000) // Uproszczone podejście - czekaj 5 sekund
        
        // Sprawdź czy plik istnieje w odpowiednim katalogu
        scenario.onActivity { activity ->
            val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "dawid_pamiętnik.daylio")
            assertTrue(file.exists())
            assertTrue(file.length() > 0)
        }
    }
}
```

## 8. Testowanie po restarcie urządzenia

Aby przetestować, czy aplikacja poprawnie przywraca swój stan po restarcie urządzenia:

1. Zainstaluj aplikację na emulatorze
2. Uruchom aplikację, aby zaplanowała powiadomienia i zadania
3. Zrestartuj emulator (wyłącz i włącz)
4. Sprawdź w logach, czy `BootReceiver` został wywołany

```kotlin
@RunWith(AndroidJUnit4::class)
class BootReceiverTest {
    @Test
    fun bootReceiverReschedulesNotification() {
        // GIVEN - symulacja restartu urządzenia
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        
        // WHEN - otrzymanie broadcastu
        val receiver = BootReceiver()
        receiver.onReceive(context, intent)
        
        // THEN - weryfikacja, że powiadomienie zostało zaplanowane ponownie
        // Używamy mockowanych komponentów lub sprawdzamy pośrednie efekty
    }
}
```

## 9. Debugowanie testów

### Rozwiązywanie problemów z testami jednostkowymi

1. Użyj breakpointów w Android Studio
2. Dodaj instrukcje `println()` lub użyj loggera
3. Sprawdź raporty testowe w katalogu `build/reports/tests`

### Debugowanie testów UI

1. Użyj `composeTestRule.onRoot().printToLog("TEST")` aby wypisać całe drzewo semantyczne
2. Sprawdź screenshoty w katalogu `build/outputs/androidTest-results`
3. Dodaj tagi do komponentów Compose dla łatwiejszego znajdowania:

```kotlin
Box(
    modifier = Modifier
        .testTag("curtain") // Dodaj tag dla testów
)
```

## 10. Testy przed wdrożeniem

Przed ostatecznym wysłaniem aplikacji do Dawida, przeprowadź następujące testy:

1. **Testy na różnych urządzeniach**
   - Przetestuj na co najmniej 2-3 różnych emulatorach (różne wielkości ekranu)
   - Jeśli możliwe, przetestuj na fizycznych urządzeniach

2. **Testy wydajności**
   - Sprawdź zużycie baterii (nie powinno być wysokie w czasie oczekiwania)
   - Użyj Profiler w Android Studio, aby sprawdzić zużycie pamięci i CPU

3. **Test z Firebase App Distribution**
   - Opublikuj aplikację w Firebase App Distribution
   - Zainstaluj na testowym urządzeniu i sprawdź czy wszystko działa

### Przygotowanie do dystrybucji przez Firebase App Distribution

1. Utwórz projekt w Firebase Console
2. Podłącz swoją aplikację Android do Firebase
3. Wygeneruj podpisany plik APK
   ```bash
   ./gradlew assembleRelease
   ```
4. Prześlij plik APK do Firebase App Distribution
5. Dodaj Dawida jako testera (wystarczy jego adres email)

## Podsumowanie

Kompleksowe testowanie aplikacji "Niespodzianka dla Dawida" obejmuje:
- Sprawdzenie poprawności obliczeń czasowych
- Weryfikację planowania i wyświetlania powiadomień
- Testowanie widoku kurtyny i jej odsłaniania o odpowiedniej porze
- Walidację pobierania i aktualizacji pliku z chmury
- Sprawdzenie odporności na restart urządzenia

Stosując się do powyższych wytycznych, masz pewność, że aplikacja zadziała perfekcyjnie w dniu 24 sierpnia 2025 roku, dostarczając Dawidowi wyjątkową niespodziankę urodzinową!