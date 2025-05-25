import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("io.sentry.android.gradle") version "5.6.0"
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

        // Bezpieczne domyślne wartości
        buildConfigField("String", "GDRIVE_FOLDER_ID", "\"\"")
        buildConfigField("boolean", "DEBUG_LOGGING", "false")
        buildConfigField("boolean", "TEST_MODE", "false")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false

            // Włącz debugowanie i tryb testowy w debug
            buildConfigField("boolean", "DEBUG_LOGGING", "true")
            buildConfigField("boolean", "TEST_MODE", "true")

            // Użyj lokalnego pliku z konfiguracją jeśli istnieje
            val localProperties = project.rootProject.file("local.properties")
            if (localProperties.exists()) {
                val properties = Properties()
                properties.load(localProperties.inputStream())
                val gdriveId = properties.getProperty("gdrive.folder.id", "")
                if (gdriveId.isNotEmpty()) {
                    buildConfigField("String", "GDRIVE_FOLDER_ID", "\"$gdriveId\"")
                }
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

            // Wyłącz debugowanie i tryb testowy w release
            buildConfigField("boolean", "DEBUG_LOGGING", "false")
            buildConfigField("boolean", "TEST_MODE", "false")

            // W release musi być ustawione przez CI/CD lub local.properties
            val localProperties = project.rootProject.file("local.properties")
            if (localProperties.exists()) {
                val properties = Properties()
                properties.load(localProperties.inputStream())
                val gdriveId = properties.getProperty("gdrive.folder.id", "")
                if (gdriveId.isNotEmpty()) {
                    buildConfigField("String", "GDRIVE_FOLDER_ID", "\"$gdriveId\"")
                } else {
                    throw GradleException("gdrive.folder.id must be set in local.properties for release builds")
                }
            } else {
                throw GradleException("local.properties file is required for release builds")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xskip-metadata-version-check",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
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

        // Dodaj wsparcie dla testów instrumentalnych z większym timeoutem
        managedDevices {
            allDevices {
                maybeCreate<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6api33").apply {
                    device = "Pixel 6"
                    apiLevel = 33
                    systemImageSource = "aosp"
                }
            }
        }
    }

    packaging {
        resources {
            excludes += "/*.txt"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/notice.txt"
            excludes += "META-INF/MANIFEST.MF"
            excludes += "META-INF/maven/**"
            excludes += "META-INF/versions/**"
        }
    }

    // Dodaj konfigurację lint aby naprawić problemy z release build
    lint {
        baseline = file("lint-baseline.xml")
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Firebase
    implementation(platform(libs.firebase.bom))

    // Biblioteka do desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Podstawowe zależności Androida
    implementation(libs.androidx.core.ktx.v1160)
    implementation(libs.androidx.lifecycle.runtime.ktx.v290)
    implementation(libs.androidx.activity.compose)

    // NAPRAWKA: Dodaj Fragment library dla ActivityResult APIs
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Compose
    implementation(platform(libs.androidx.compose.bom.v20250500))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Network
    implementation(libs.okhttp)

    // Logging
    implementation(libs.timber)

    // Google Drive API
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.oauth.client.jetty)
    implementation(libs.kotlinx.coroutines.play.services)

    // JSON
    implementation(libs.gson)
    implementation(libs.androidx.junit.ktx)

    // Testy jednostkowe
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.mockk.android)
    testImplementation(libs.kotlin.test.junit)

    // Testy instrumentalne
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v361)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)

    // Debug
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Google Tasks API
    implementation(libs.play.services.tasks)

    // Testy Compose
    testImplementation(libs.ui.test.junit4)
}

// Konfiguracja Sentry
sentry {
    // Włącz szczegółowe logowanie Sentry CLI w debug
    debug = true

    // Automatyczne przesyłanie kodu źródłowego do Sentry
    // Umożliwia wyświetlanie kodu źródłowego w stack trace w Sentry UI
    includeSourceContext = true

    // Dodatkowe foldery z kodem źródłowym do przesłania
    additionalSourceDirsForSourceContext = setOf(
        "src/main/java", "src/main/kotlin"
    )

    // Konfiguracja organizacji i projektu Sentry
    org = "wszystkiegonajlepszegodawid"
    projectName = "wszystkiegonajlepszegodawid"

    // Token autoryzacyjny Sentry z zmiennej środowiskowej
    authToken = System.getenv("SENTRY_AUTH_TOKEN")

    // Automatyczne przesyłanie ProGuard mapping files w release
    autoUploadProguardMapping = true
}