plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.philornot.siekiera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.philornot.siekiera"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            // Włącz obfuskację kodu
            isPseudoLocalesEnabled = true
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        // DODANE: Włączenie desugaring, konieczne dla Google Play Services
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        // Dodaj opcję, która pozwoli na tymczasowe pominięcie błędów niezgodności wersji
        freeCompilerArgs = listOf("-Xskip-metadata-version-check")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
            // Może potrzebne będą także te
            excludes += "/*.txt"
            excludes += "META-INF/MANIFEST.MF"
            excludes += "META-INF/maven/**"
            excludes += "META-INF/versions/**"
        }
    }
}

dependencies {
    // DODANE: Biblioteka do desugaring - potrzebna dla Google Play Services
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Podstawowe zależności Androida
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Ikony z biblioteki Material
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // WorkManager - do planowania zadań
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // OkHttp - klient HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Timber - do logowania
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Google Drive API i zależności
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.http-client:google-http-client-gson:1.43.1")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Do pracy z JSON
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.androidx.junit.ktx)

    // Dla testów jednostkowych (folder 'test')
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1") // Możesz rozważyć usunięcie, jeśli używasz tylko MockK
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.work:work-testing:2.10.1")

    // POPRAWIONE: Dodatkowe zależności MockK dla testów
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("io.mockk:mockk-agent:1.13.9") // Potrzebne do mockowania statycznych metod
    testImplementation("io.mockk:mockk-android:1.13.9") // Dodane dla wsparcia Android
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.10")

    // Dla testów instrumentalnych (folder 'androidTest')
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.8.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.work:work-testing:2.10.1")

    // DODANE: MockK dla testów instrumentalnych
    androidTestImplementation("io.mockk:mockk-android:1.13.9")
    androidTestImplementation("io.mockk:mockk-agent:1.13.9")

    // Dla debugowania Compose
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Google Tasks API dla operacji na Future
    implementation("com.google.android.gms:play-services-tasks:18.3.0")

    // Dodatkowe narzędzia do testów z Compose
    testImplementation("androidx.compose.ui:ui-test-junit4:1.8.1")
}