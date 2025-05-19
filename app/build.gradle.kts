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

        // Włączenie desugaring, konieczne dla Google Play Services
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
            excludes += "/*.txt"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE-notice.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "META-INF/MANIFEST.MF"
            excludes += "META-INF/maven/**"
            excludes += "META-INF/versions/**"
        }
    }
}

dependencies {
    // Biblioteka do desugaring - potrzebna dla Google Play Services
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Podstawowe zależności Androida
    implementation(libs.androidx.core.ktx.v1160)
    implementation(libs.androidx.lifecycle.runtime.ktx.v290)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom.v20250500))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)

    // Ikony z biblioteki Material
    implementation(libs.androidx.material.icons.extended)

    // WorkManager - do planowania zadań
    implementation(libs.androidx.work.runtime.ktx)

    // OkHttp - klient HTTP
    implementation(libs.okhttp)

    // Timber - do logowania
    implementation(libs.timber)

    // Google Drive API i zależności
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.oauth.client.jetty)
    implementation(libs.kotlinx.coroutines.play.services)

    // Do pracy z JSON
    implementation(libs.gson)
    implementation(libs.androidx.junit.ktx)

    // Dla testów jednostkowych (folder 'test')
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.mockito.kotlin) // Możesz rozważyć usunięcie, jeśli używasz tylko MockK
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.work.testing)

    // Dodatkowe zależności MockK dla testów
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent) // Potrzebne do mockowania statycznych metod
    testImplementation(libs.mockk.android) // dla wsparcia Android
    testImplementation(libs.kotlin.test.junit)

    // Dla testów instrumentalnych (folder 'androidTest')
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v361)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.work.testing)

    // MockK dla testów instrumentalnych
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)

    // Dla debugowania Compose
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Google Tasks API dla operacji na Future
    implementation(libs.play.services.tasks)

    // Dodatkowe narzędzia do testów z Compose
    testImplementation(libs.ui.test.junit4)
}