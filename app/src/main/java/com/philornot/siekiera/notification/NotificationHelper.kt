package com.philornot.siekiera.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.philornot.siekiera.MainActivity
import com.philornot.siekiera.R
import timber.log.Timber
import java.io.File

/**
 * Klasa pomocnicza do zarządzania powiadomieniami w aplikacji.
 *
 * AKTUALIZACJA: Powiadomienia urodzinowe są wyświetlane zawsze gdy zostają
 * wywołane, niezależnie od statusu odebrania prezentu.
 */
object NotificationHelper {

    // Kanały powiadomień
    private const val CHANNEL_ID_BIRTHDAY = "gift_reveal_channel"
    private const val CHANNEL_ID_DOWNLOAD = "download_complete_channel"

    // Identyfikatory powiadomień
    private const val NOTIFICATION_ID_BIRTHDAY = 1001
    private const val NOTIFICATION_ID_DOWNLOAD = 1002

    /**
     * Inicjalizuje kanały powiadomień. Powinno być wywołane podczas startu
     * aplikacji.
     *
     * @param context Kontekst aplikacji
     */
    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Kanał dla powiadomień urodzinowych
            val birthdayChannel = NotificationChannel(
                CHANNEL_ID_BIRTHDAY,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableLights(true)
                enableVibration(true)
            }

            // Kanał dla powiadomień o pobraniu
            val downloadChannel = NotificationChannel(
                CHANNEL_ID_DOWNLOAD,
                context.getString(R.string.download_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.download_notification_channel_description)
            }

            // Rejestracja kanałów
            notificationManager.createNotificationChannel(birthdayChannel)
            notificationManager.createNotificationChannel(downloadChannel)

            Timber.d("Utworzono kanały powiadomień")
        }
    }

    /**
     * Wyświetla powiadomienie o pobraniu prezentu.
     *
     * @param context Kontekst aplikacji
     * @param fileName Nazwa pobranego pliku
     */
    fun showDownloadCompleteNotification(context: Context, fileName: String) {
        // Sprawdź, czy prezent został już odebrany
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.getBoolean("gift_received", false)

        // Oznacz prezent jako odebrany
        prefs.edit { putBoolean("gift_received", true) }

        // Utwórz intent, który otworzy aplikację
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_DOWNLOADED_FILE", true)
            putExtra("FILE_NAME", fileName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Utwórz intent otwierający folder Pobrane
        val viewDownloadsIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType("content://downloads/all_downloads".toUri(), "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val viewPendingIntent = PendingIntent.getActivity(
            context,
            1,
            viewDownloadsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Uproszczona lokalizacja pliku
        val simplifiedPath = "Pobrane"

        // Pełna ścieżka do pliku (do logowania)
        val fullPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName
        ).absolutePath
        Timber.d("Wyświetlam powiadomienie o pobranym pliku: $fileName, ścieżka: $fullPath")

        // Zbuduj powiadomienie
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DOWNLOAD)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setContentTitle(context.getString(R.string.download_notification_title))
            .setContentText(
                context.getString(
                    R.string.download_notification_text, fileName, simplifiedPath
                )
            ).setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(
                        R.string.download_notification_big_text, fileName, simplifiedPath
                    )
                )
            ).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent).addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.view_downloads),
                viewPendingIntent
            ).build()

        // Wyświetl powiadomienie
        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_DOWNLOAD, notification)
                Timber.d("Wyświetlono powiadomienie o pobraniu pliku: $fileName")
            } catch (e: SecurityException) {
                Timber.e(e, "Brak uprawnień do wyświetlania powiadomień")
            }
        }
    }

    /**
     * Wyświetla powiadomienie o odsłonięciu prezentu.
     *
     * Nie sprawdza czy prezent został odebrany - powiadomienie
     * jest wyświetlane zawsze gdy zostanie wywołane.
     *
     * @param context Kontekst aplikacji
     */
    fun showGiftRevealNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Utwórz kanał powiadomień dla Androida 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_BIRTHDAY,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Utwórz intent do otwarcia aplikacji
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Zbuduj powiadomienie
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BIRTHDAY)
            .setSmallIcon(R.drawable.notification_gift_icon)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setContentTitle("Wszystkiego najlepszego") // Tytuł zgodny z wymaganiem
            .setContentText("Twój prezent jest gotowy do otwarcia") // Opis zgodny z wymaganiem
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(pendingIntent)
            .build()

        // Pokaż powiadomienie
        try {
            notificationManager.notify(NOTIFICATION_ID_BIRTHDAY, notification)
            Timber.d("Wyświetlono powiadomienie o odsłonięciu prezentu")
        } catch (e: SecurityException) {
            Timber.e(e, "Brak uprawnień do wyświetlania powiadomień")
        }
    }

}