# Add project specific ProGuard rules here.

# Keep application class
-keep class com.philornot.siekiera.GiftApp { *; }
-keep class com.philornot.siekiera.MainActivity { *; }

# Keep all classes used by Google Drive API
-keep class com.google.api.** { *; }
-keep class com.google.auth.** { *; }
-keepclassmembers class com.google.api.** { *; }

# Keep HTTP client classes
-keep class com.google.http.** { *; }
-dontwarn com.google.http.**

# Keep JSON factories and related classes
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.http.** { *; }

# Keep all network related classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep WorkManager classes
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Keep our Workers
-keep class com.philornot.siekiera.workers.** { *; }

# Keep notification classes
-keep class com.philornot.siekiera.notification.** { *; }

# Keep config classes
-keep class com.philornot.siekiera.config.** { *; }

# Keep data classes and their members
-keep @kotlin.Metadata class com.philornot.siekiera.** { *; }
-keepclassmembers class com.philornot.siekiera.** {
    <fields>;
    <init>(...);
}

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-keep class kotlin.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging in release builds
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify