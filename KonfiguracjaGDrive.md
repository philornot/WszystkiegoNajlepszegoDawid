# Integracja z Google Drive - Przewodnik

Ten przewodnik wyjaśnia, jak skonfigurować folder Google Drive dla aplikacji "Wszystkiego Najlepszego
Dawid" bez wymagania logowania od użytkownika.

## Konfiguracja konta usługi Google

1. **Utwórz projekt w Google Cloud Console:**
    - Przejdź do [Google Cloud Console](https://console.cloud.google.com/)
    - Utwórz nowy projekt
    - Zanotuj ID projektu

2. **Włącz Google Drive API:**
    - W menu bocznym wybierz "API i usługi" > "Biblioteka"
    - Wyszukaj "Google Drive API" i włącz je

3. **Utwórz konto usługi (Service Account):**
    - W menu bocznym wybierz "API i usługi" > "Poświadczenia"
    - Kliknij "Utwórz poświadczenia" > "Konto usługi"
    - Wprowadź nazwę, ID i opis konta usługi
    - Nadaj rolę "Przeglądający" (Viewer)
    - Kliknij "Gotowe"

4. **Utwórz klucz dla konta usługi:**
    - Na liście kont usługi kliknij utworzone konto
    - Przejdź do zakładki "Klucze"
    - Kliknij "Dodaj klucz" > "Utwórz nowy klucz"
    - Wybierz format JSON
    - Kliknij "Utwórz" - plik z kluczem zostanie pobrany

## Konfiguracja folderu Google Drive

1. **Utwórz folder na swoim Google Drive:**
    - Otwórz [Google Drive](https://drive.google.com/)
    - Utwórz nowy folder (np. "Daylio dla Dawida")

2. **Udostępnij folder dla konta usługi:**
    - Kliknij prawym przyciskiem myszy na folder i wybierz "Udostępnij"
    - W polu "Dodaj osoby i grupy" wklej adres email konta usługi (znajduje się w pobranym pliku
      JSON, pole "client_email")
    - Ustaw uprawnienia na "Przeglądający" (Viewer)
    - Wyłącz opcję powiadamiania osób
    - Kliknij "Udostępnij"

3. **Pobierz ID folderu:**
    - Otwórz folder
    - Z adresu URL w przeglądarce skopiuj ID folderu (część po "folders/")
    - URL ma format: `https://drive.google.com/drive/folders/FOLDER_ID_HERE`

## Konfiguracja aplikacji

1. **Dodaj klucz do projektu:**
    - Zmień nazwę pobranego pliku JSON na `service_account.json`
    - Utwórz folder `app/src/main/res/raw/` w swoim projekcie
    - Skopiuj plik `service_account.json` do tego folderu

2. **Aktualizuj kod aplikacji:**
    - Otwórz plik `FileCheckWorker.kt`
    - Zastąp wartość `FOLDER_ID` ID folderu, który uzyskałeś wcześniej:
   ```kotlin
   private const val FOLDER_ID = "twoje_id_folderu"
   ```

3. **Dodaj wymagane zależności w build.gradle (moduł app):**
   ```kotlin
   dependencies {
       // Google Drive API
       implementation 'com.google.api-client:google-api-client-android:1.33.0'
       implementation 'com.google.apis:google-api-services-drive:v3-rev20220815-1.32.1'
       implementation 'com.google.auth:google-auth-library-oauth2-http:1.6.0'
   }
   ```

## Dodawanie pliku Daylio do folderu

1. **Wyeksportuj plik z aplikacji Daylio:**
    - Otwórz aplikację Daylio
    - Przejdź do Ustawienia > Kopia zapasowa
    - Wybierz "Utwórz kopię zapasową"
    - Zostanie utworzony plik z rozszerzeniem .daylio

2. **Prześlij plik do utworzonego folderu:**
    - Otwórz udostępniony folder na Google Drive
    - Kliknij "Nowy" > "Prześlij plik"
    - Wybierz plik Daylio ze swojego urządzenia
    - Poczekaj na zakończenie przesyłania

3. **Testowanie:**
    - Aplikacja sprawdzi raz dziennie, czy istnieje nowszy plik w folderze
    - Aby zaktualizować plik przed urodzinami, po prostu zastąp go nowszą wersją w folderze Google
      Drive

## Bezpieczeństwo i uwagi

- Konto usługi ma dostęp tylko do odczytu i tylko do określonego folderu
- Klucz konta usługi jest bezpiecznie przechowywany w aplikacji
- Nie są przechowywane żadne dane logowania użytkownika
- Aplikacja nie wymaga od użytkownika (Dawida) żadnych uprawnień czy logowania

## Rozwiązywanie problemów

Jeśli masz problemy z konfiguracją:

1. **Upewnij się, że folder jest prawidłowo udostępniony** - konto usługi musi mieć uprawnienia do
   przeglądania
2. **Sprawdź ID folderu** - upewnij się, że ID folderu zostało poprawnie skopiowane
3. **Zweryfikuj plik klucza** - upewnij się, że plik `service_account.json` jest poprawny i znajduje
   się w folderze `raw`

Możesz monitorować logi aplikacji, aby zobaczyć, czy występują problemy:

```
adb logcat -s "DriveApiClient" "FileCheckWorker"
```

## Aktualizowanie pliku Daylio

Aby zaktualizować plik Daylio przed urodzinami Dawida:

1. Utwórz nową kopię zapasową w aplikacji Daylio
2. Prześlij ten plik do folderu Google Drive
3. Worker aplikacji automatycznie pobierze najnowszą wersję podczas następnego zaplanowanego
   sprawdzenia