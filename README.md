# Wszystkiego Najlepszego Dawid

A birthday Android app that reveals a gift on August 24, 2025, at exactly 8:24 AM Warsaw time.

## Overview

This app displays an animated curtain that automatically opens on the specified date and time, revealing a downloadable gift - a Daylio diary export file automatically synced from Google Drive. The app includes a hidden timer mode activated by long-pressing the gift.

## Features

- Precise countdown to birthday with animated digits
- Animated curtain that reveals automatically at the target time
- Automatic file download from Google Drive using Service Account
- Background sync with WorkManager for file updates
- Notification system for birthday alerts
- Hidden timer functionality
- Navigation drawer with multiple sections
- Material Design 3 with custom lavender theme

## Technical Stack

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **Background Processing**: WorkManager
- **Cloud Integration**: Google Drive API v3
- **Authentication**: Service Account (no user login required)
- **Notifications**: AlarmManager + NotificationManager
- **Testing**: JUnit, Espresso, MockK, Robolectric

## Architecture

```
UI Layer (Compose)
├── MainActivity
├── MainScreen with ViewModels
└── Theme & Resources

Domain Layer
├── AppConfig for centralized configuration
├── TimeProvider for testable time management
└── Use cases for business logic

Data Layer
├── DriveApiClient for Google Drive integration
├── FileCheckWorker for background sync
├── NotificationScheduler for alerts
└── Local storage with SharedPreferences
```

## Setup

### Prerequisites

- Android Studio Arctic Fox or newer
- Android SDK 24+ (Android 7.0)
- Google Cloud Project with Drive API enabled
- Service Account with JSON key file

### Configuration

1. Create `local.properties` file:
```properties
gdrive.folder.id=YOUR_GOOGLE_DRIVE_FOLDER_ID
```

2. Add Service Account key:
```
app/src/main/res/raw/service_account.json
```

3. Configure Google Drive folder:
  - Create folder on Google Drive
  - Share with Service Account email
  - Copy folder ID from URL

### Build

```bash
# Debug build with test mode enabled
./gradlew assembleDebug

# Release build with optimizations
./gradlew assembleRelease
```

## Testing

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

The project includes comprehensive tests for:
- Time calculations and state management
- Google Drive API integration
- UI components and animations
- Background workers and notifications

## Configuration

Main configuration in `app/src/main/res/values/config.xml`:
- Birthday date and time
- File check intervals
- Notification settings
- Debug/test mode flags

Sensitive data handled via BuildConfig and local.properties to avoid hardcoding secrets.

## Key Components

- **MainViewModel**: State management and time calculations
- **DriveApiClient**: Google Drive integration with retry mechanisms
- **FileCheckWorker**: Background file synchronization
- **AppConfig**: Centralized configuration management
- **TimeProvider**: Abstraction for testable time operations

## Build Variants

- **Debug**: Test mode enabled, verbose logging, development settings
- **Release**: Optimized with ProGuard, production configuration required

---

# Wszystkiego Najlepszego Dawid (Polski)

Aplikacja urodzinowa na Androida, która odsłania prezent 24 sierpnia 2025 roku o dokładnie 8:24 czasu warszawskiego.

## Opis

Aplikacja wyświetla animowaną kurtynę, która automatycznie otwiera się w określonym dniu i czasie, odsłaniając prezent do pobrania - eksport pamiętnika z Daylio automatycznie synchronizowany z Google Drive. Aplikacja zawiera ukryty tryb timera aktywowany przez długie naciśnięcie prezentu.

## Funkcje

- Precyzyjne odliczanie do urodzin z animowanymi cyframi
- Animowana kurtyna odsłaniająca się automatycznie w docelowym czasie
- Automatyczne pobieranie pliku z Google Drive za pomocą Service Account
- Synchronizacja w tle przez WorkManager dla aktualizacji pliku
- System powiadomień o urodzinach
- Ukryta funkcjonalność timera
- Szufladka nawigacyjna z wieloma sekcjami
- Material Design 3 z niestandardowym motywem lawendowym

## Stack Technologiczny

- **Język**: Kotlin 100%
- **Framework UI**: Jetpack Compose
- **Architektura**: MVVM + Clean Architecture
- **Przetwarzanie w tle**: WorkManager
- **Integracja z chmurą**: Google Drive API v3
- **Uwierzytelnianie**: Service Account (bez logowania użytkownika)
- **Powiadomienia**: AlarmManager + NotificationManager
- **Testowanie**: JUnit, Espresso, MockK, Robolectric

## Konfiguracja

### Wymagania

- Android Studio Arctic Fox lub nowsze
- Android SDK 24+ (Android 7.0)
- Projekt Google Cloud z włączonym Drive API
- Service Account z plikiem klucza JSON

### Ustawienia

1. Utwórz plik `local.properties`:
```properties
gdrive.folder.id=TWOJE_ID_FOLDERU_GOOGLE_DRIVE
```

2. Dodaj klucz Service Account:
```
app/src/main/res/raw/service_account.json
```

3. Skonfiguruj folder Google Drive:
  - Utwórz folder na Google Drive
  - Udostępnij dla email Service Account
  - Skopiuj ID folderu z URL

### Budowanie

```bash
# Build debug z włączonym trybem testowym
./gradlew assembleDebug

# Build release z optymalizacjami
./gradlew assembleRelease
```

## Testowanie

```bash
# Testy jednostkowe
./gradlew test

# Testy integracyjne
./gradlew connectedAndroidTest

# Wszystkie testy
./gradlew check
```

Projekt zawiera kompleksowe testy dla:
- Obliczeń czasowych i zarządzania stanem
- Integracji z Google Drive API
- Komponentów UI i animacji
- Workerów w tle i powiadomień

## Główne Komponenty

- **MainViewModel**: Zarządzanie stanem i obliczenia czasowe
- **DriveApiClient**: Integracja z Google Drive z mechanizmami ponownych prób
- **FileCheckWorker**: Synchronizacja plików w tle
- **AppConfig**: Scentralizowane zarządzanie konfiguracją
- **TimeProvider**: Abstrakcja dla testowalnych operacji czasowych

## Warianty Budowania

- **Debug**: Tryb testowy włączony, szczegółowe logowanie, ustawienia rozwojowe
- **Release**: Zoptymalizowany z ProGuard, wymagana konfiguracja produkcyjna