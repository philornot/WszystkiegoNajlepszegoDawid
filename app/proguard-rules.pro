# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes LineNumberTable,SourceFile

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# =============================================================================
# PODSTAWOWE REGUŁY ANDROID
# =============================================================================

# Zachowaj wszystkie klasy aplikacji (główny pakiet)
-keep class com.philornot.siekiera.** { *; }

# Zachowaj annotacje
-keepattributes *Annotation*

# Zachowaj informacje o exceptions
-keepattributes Exceptions

# Zachowaj podpisy generics
-keepattributes Signature

# Zachowaj SourceFile i LineNumberTable dla lepszego debugowania
-keepattributes SourceFile,LineNumberTable

# =============================================================================
# KOTLINX COROUTINES
# =============================================================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# =============================================================================
# GSON
# =============================================================================
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# =============================================================================
# GOOGLE API CLIENT
# =============================================================================
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.auth.**

# Zachowaj wszystkie Google Drive API klasy
-keep class com.google.api.services.drive.** { *; }

# Zachowaj Jackson annotations dla Google API
-keepattributes *Jackson*
-keep class org.codehaus.jackson.** { *; }
-keep class com.fasterxml.jackson.** { *; }

# =============================================================================
# HTTP CLIENT
# =============================================================================
-keep class com.google.api.client.http.** { *; }
-keep class com.google.api.client.json.** { *; }
-dontwarn org.apache.http.**
-dontwarn okhttp3.**
-dontwarn okio.**

# =============================================================================
# ANDROIDX
# =============================================================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Work Manager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.WorkDatabase
-keep class androidx.work.impl.WorkDatabase_Impl

# =============================================================================
# COMPOSE
# =============================================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# =============================================================================
# TIMBER
# =============================================================================
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# =============================================================================
# FIREBASE (jeśli używane)
# =============================================================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# =============================================================================
# SENTRY
# =============================================================================
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# =============================================================================
# REFLECTION
# =============================================================================
# Zachowaj klasy używające reflection
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# =============================================================================
# ENUMS
# =============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =============================================================================
# SERIALIZACJA
# =============================================================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =============================================================================
# KOTLIN METADATA
# =============================================================================
-keep class kotlin.Metadata { *; }

# =============================================================================
# USUWANIE LOGÓW W RELEASE
# =============================================================================
# Usuń wywołania Log.d, Log.v, ale zachowaj Log.w, Log.e
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Zachowaj Timber w wersji release (dla crashlyticsów)
-keep class timber.log.Timber { *; }

# =============================================================================
# OPTYMALIZACJE
# =============================================================================
# Optymalizuj kod
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# =============================================================================
# SPECIFICZNE DLA APLIKACJI
# =============================================================================
# Zachowaj wszystkie BroadcastReceiver
-keep class * extends android.content.BroadcastReceiver

# Zachowaj wszystkie Service
-keep class * extends android.app.Service

# Zachowaj wszystkie WorkManager Worker classes
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Zachowaj Application class
-keep class * extends android.app.Application

# Zachowaj Activity classes
-keep class * extends androidx.activity.ComponentActivity

# =============================================================================
# SUPPRESSION OF WARNINGS
# =============================================================================
-dontwarn java.lang.invoke.**
-dontwarn **$$serializer
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn kotlin.coroutines.jvm.internal.**