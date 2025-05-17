# Wszystkiego Najlepszego Dawid

Aplikacja na 18. urodziny Dawida, zaprojektowana jako wyjątkowy prezent urodzinowy.

## 📱 O Aplikacji

Wszystkiego Najlepszego Dawid to prosta aplikacja na Androida stworzona specjalnie na 18. urodziny
Dawida.
Głównym elementem aplikacji jest kurtyna, która automatycznie odsłoni się dokładnie w dniu 24
sierpnia 2025 roku,
ujawniając prezent - eksport pamiętnika z aplikacji Daylio.

### Główne funkcje

- **Animowana kurtyna** zakrywająca prezent aż do dnia urodzin
- **Odmierzanie czasu** pokazujące, ile dni pozostało do urodzin
- **Automatyczne powiadomienie** w dniu urodzin
- **Ukryty prezent** - plik z eksportem Daylio pobierany z Google Drive
- **Automatyczna aktualizacja** prezentu, jeśli zostanie zaktualizowany na Google Drive

## 🛠️ Technologie

- **Kotlin** - główny język programowania
- **Jetpack Compose** - nowoczesny framework UI
- **WorkManager** - do planowania zadań w tle
- **Google Drive API** - do pobierania i aktualizacji pliku prezentu
- **Service Account** - do bezobsługowego dostępu do Google Drive bez logowania użytkownika

### Konfiguracja Google Drive

Szczegółowa instrukcja konfiguracji Google Drive znajduje się w
pliku [KonfiguracjaGDrive.md](KonfiguracjaGDrive.md).

### Budowanie Projektu

1. Sklonuj repozytorium
2. Stwórz konto usługi Google i pobierz klucz JSON
3. Umieść plik klucza jako `app/src/main/res/raw/service_account.json`
4. Zaktualizuj `FOLDER_ID` w klasie `FileCheckWorker` na ID twojego folderu Google Drive
5. Zbuduj i uruchom aplikację

## 📅 Logika Timera

Aplikacja używa klasy `TimeProvider` do zarządzania czasem, co umożliwia:

- W normalnym trybie: odliczanie do 24 sierpnia 2025
- W trybie testowym: symulowanie różnych dat do testowania

## 📱 Główne Komponenty

- **MainActivity** - główna aktywność aplikacji
- **MainScreen** - ekran z kurtyną i odliczaniem
- **MainViewModel** - zarządzanie stanem UI
- **FileCheckWorker** - worker sprawdzający aktualizacje pliku na Google Drive
- **DriveApiClient** - klient API Google Drive używający konta usługi
- **NotificationScheduler** - planowanie powiadomień urodzinowych

## 🧪 Testowanie

Projekt zawiera kompleksowe testy:

- **Testy jednostkowe** dla logiki biznesowej
- **Testy UI** dla ekranu głównego z kurtyną
- **Testy integracyjne** dla komunikacji z Google Drive

Do testowania różnych scenariuszy czasowych użyj klasy `TimeSimulator`.

## 📝 Uwagi

Ta aplikacja jest prywatnym projektem, stworzonym specjalnie dla Dawida na jego 18. urodziny.
Nie jest przeznaczona do użytku komercyjnego ani dystrybucji.

## 📄 Licencja

Ten projekt jest własnością prywatną i nie jest objęty licencją open source.
