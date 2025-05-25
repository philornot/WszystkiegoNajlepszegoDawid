package com.philornot.siekiera.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.philornot.siekiera.MainActivity
import com.philornot.siekiera.R
import com.philornot.siekiera.utils.TimeUtils
import timber.log.Timber

/**
 * Ulepszona wersja TimerNotificationHelper z lepszą obsługą długich
 * timerów i bardziej informatywnymi powiadomieniami.
 *
 * AKTUALIZACJA: Wszystkie powiadomienia teraz poprawnie otwierają
 * aplikację po kliknięciu dzięki odpowiednim flagom Intent.
 */
object TimerNotificationHelper {

    // Identyfikator kanału powiadomień dla timera
    private const val CHANNEL_ID_TIMER = "timer_notification_channel"

    // Identyfikatory powiadomień
    private const val NOTIFICATION_ID_TIMER = 1003
    private const val NOTIFICATION_ID_TIMER_PROGRESS = 1004

    /** Inicjalizuje kanał powiadomień dla timera z ulepszeniami. */
    fun initTimerNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Kanał dla powiadomień timera - z wysokim priorytetem
            val timerChannel = NotificationChannel(
                CHANNEL_ID_TIMER,
                context.getString(R.string.timer_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.timer_notification_channel_description)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setSound(null, null) // Wyłącz dźwięk domyślny - dodamy własny w powiadomieniu
            }

            notificationManager.createNotificationChannel(timerChannel)
            Timber.d("Utworzono kanał powiadomień dla timera")
        }
    }

    /**
     * Wyświetla ulepszone powiadomienie o zakończeniu odliczania timera.
     * Obsługuje różne długości timerów z odpowiednimi komunikatami.
     *
     * @param context Kontekst aplikacji
     * @param minutes Liczba minut, która była ustawiona na timerze
     */
    fun showTimerCompletedNotification(context: Context, minutes: Int) {
        Timber.d("Wyświetlanie powiadomienia o zakończeniu timera na $minutes minut")

        // Przygotowanie intencji do otwarcia aplikacji z informacją o zakończonym timerze
        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_TIMER_FINISHED", true)
            putExtra("TIMER_MINUTES", minutes)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Inteligentne formatowanie na podstawie długości timera
        val timerDuration = TimeUtils.formatTimerDurationVerbose(minutes)
        val shortDuration = TimeUtils.formatTimerDuration(minutes)

        // Personalizowane komunikaty w zależności od długości timera
        val (title, text, bigText) = when {
            minutes == 1 -> Triple(
                "Minutnik zakończony! ⏰",
                "Upłynęła 1 minuta",
                "Twój minutnik zakończył odliczanie po 1 minucie."
            )

            minutes < 60 -> Triple(
                "Timer zakończony! ⏰",
                "Upłynęło $timerDuration",
                "Twój timer zakończył odliczanie po $timerDuration.\n\nKliknij aby otworzyć aplikację."
            )

            minutes == 60 -> Triple(
                "Godzinny timer zakończony! ⏰",
                "Upłynęła 1 godzina",
                "Twój godzinny timer zakończył odliczanie.\n\nKliknij aby otworzyć aplikację."
            )

            minutes < 120 -> Triple(
                "Timer zakończony! ⏰",
                "Upłynęło $timerDuration",
                "Twój timer zakończył odliczanie po $timerDuration.\n\nKliknij aby otworzyć aplikację."
            )

            minutes % 60 == 0 -> Triple(
                "Długi timer zakończony! ⏰",
                "Upłynęło ${minutes / 60} godzin",
                "Twój ${minutes / 60}-godzinny timer zakończył odliczanie.\n\nKliknij aby otworzyć aplikację."
            )

            else -> Triple(
                "Timer zakończony! ⏰",
                "Upłynęło $shortDuration",
                "Twój timer ($timerDuration) zakończył odliczanie.\n\nKliknij aby otworzyć aplikację."
            )
        }

        // Buduj powiadomienie z ulepszoną personalizacją
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
            .setSmallIcon(R.drawable.notification_timer_icon)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
            .setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setAutoCancel(true) // Powiadomienie zniknie po kliknięciu
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setVibrate(
                longArrayOf(
                    0,
                    500,
                    250,
                    500,
                    250,
                    500
                )
            ) // Dłuższa sekwencja dla ważnego powiadomienia
            .setContentIntent(pendingIntent) // Główny intent otwierający aplikację
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Zamknij",
                createDismissIntent(context)
            ).build()

        // Wyświetl powiadomienie
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER, notification)
            Timber.d("Wyświetlono powiadomienie o zakończeniu timera ($timerDuration)")
        } catch (e: SecurityException) {
            Timber.e(e, "Brak uprawnień do wyświetlania powiadomień")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas wyświetlania powiadomienia o timerze")
        }
    }

    /**
     * Wyświetla powiadomienie o postępie timera (opcjonalne). Może być używane
     * dla długich timerów jako przypomnienie.
     *
     * @param context Kontekst aplikacji
     * @param initialMinutes Początkowa wartość timera
     * @param remainingMinutes Pozostała liczba minut
     */
    fun showTimerProgressNotification(
        context: Context,
        initialMinutes: Int,
        remainingMinutes: Int,
    ) {
        if (remainingMinutes <= 0 || initialMinutes <= 0) return

        val progress =
            ((initialMinutes - remainingMinutes).toFloat() / initialMinutes.toFloat() * 100).toInt()
        val remainingFormatted = TimeUtils.formatTimerDuration(remainingMinutes)

        // Wyświetlaj powiadomienia o postępie tylko dla długich timerów
        if (initialMinutes < 30) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_TIMER_PROGRESS", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TIMER_PROGRESS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
            .setSmallIcon(R.drawable.notification_timer_icon)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setContentTitle("Timer w trakcie")
            .setContentText("Pozostało $remainingFormatted ($progress% ukończone)")
            .setProgress(100, progress, false).setOngoing(true) // Nie można usunąć przez swipe
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent) // Intent otwierający aplikację
            .addAction(
                android.R.drawable.ic_media_pause, "Pauzuj", createPauseIntent(context)
            ).addAction(
                android.R.drawable.ic_menu_close_clear_cancel, "Anuluj", createCancelIntent(context)
            ).build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_TIMER_PROGRESS, notification)
            Timber.d("Wyświetlono powiadomienie o postępie timera: $progress%")
        } catch (e: SecurityException) {
            Timber.e(e, "Brak uprawnień do wyświetlania powiadomień o postępie")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas wyświetlania powiadomienia o postępie timera")
        }
    }

    /** Anuluje wszystkie powiadomienia timera. */
    fun cancelTimerNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(NOTIFICATION_ID_TIMER)
            notificationManager.cancel(NOTIFICATION_ID_TIMER_PROGRESS)
            Timber.d("Anulowano wszystkie powiadomienia timera")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas anulowania powiadomień timera")
        }
    }

    /** Anuluje tylko powiadomienie o postępie timera. */
    fun cancelTimerProgressNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_TIMER_PROGRESS)
            Timber.d("Anulowano powiadomienie o postępie timera")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas anulowania powiadomienia o postępie")
        }
    }

    /** Wyświetla powiadomienie o spauzowaniu timera. */
    fun showTimerPausedNotification(context: Context, remainingMinutes: Int) {
        val remainingFormatted = TimeUtils.formatTimerDuration(remainingMinutes)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_TIMER_PAUSED", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_TIMER_PROGRESS + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
            .setSmallIcon(R.drawable.notification_timer_icon)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setContentTitle("Timer spauzowany ⏸️")
            .setContentText("Pozostało $remainingFormatted do odliczenia").setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Timer został spauzowany.\n\n" + "Pozostało $remainingFormatted do odliczenia.\n" + "Kliknij aby otworzyć aplikację i wznowić timer."
                )
            ).setOngoing(true) // Timer spauzowany - powinien pozostać w powiadomieniach
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent) // Intent otwierający aplikację
            .addAction(
                android.R.drawable.ic_media_play, "Wznów", createResumeIntent(context)
            ).addAction(
                android.R.drawable.ic_menu_close_clear_cancel, "Anuluj", createCancelIntent(context)
            ).build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_TIMER_PROGRESS, notification)
            Timber.d("Wyświetlono powiadomienie o spauzowanym timerze")
        } catch (e: SecurityException) {
            Timber.e(e, "Brak uprawnień do wyświetlania powiadomienia o pauzie")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas wyświetlania powiadomienia o spauzowanym timerze")
        }
    }

    /** Tworzy PendingIntent dla akcji zamknięcia powiadomienia. */
    private fun createDismissIntent(context: Context): PendingIntent {
        val intent = Intent().apply {
            action = "com.philornot.siekiera.DISMISS_TIMER_NOTIFICATION"
        }
        return PendingIntent.getBroadcast(
            context, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Tworzy PendingIntent dla akcji pauzowania timera. */
    private fun createPauseIntent(context: Context): PendingIntent {
        val intent = Intent().apply {
            action = "com.philornot.siekiera.PAUSE_TIMER"
        }
        return PendingIntent.getBroadcast(
            context, 11, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Tworzy PendingIntent dla akcji wznowienia timera. */
    private fun createResumeIntent(context: Context): PendingIntent {
        val intent = Intent().apply {
            action = "com.philornot.siekiera.RESUME_TIMER"
        }
        return PendingIntent.getBroadcast(
            context, 12, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Tworzy PendingIntent dla akcji anulowania timera. */
    private fun createCancelIntent(context: Context): PendingIntent {
        val intent = Intent().apply {
            action = "com.philornot.siekiera.CANCEL_TIMER"
        }
        return PendingIntent.getBroadcast(
            context, 13, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Sprawdza czy powiadomienie timera jest aktualnie wyświetlane. */
    fun isTimerNotificationActive(context: Context): Boolean {
        return try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.activeNotifications.any {
                    it.id == NOTIFICATION_ID_TIMER || it.id == NOTIFICATION_ID_TIMER_PROGRESS
                }
            } else {
                // Na starszych wersjach Androida nie można sprawdzić aktywnych powiadomień
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas sprawdzania aktywnych powiadomień")
            false
        }
    }
}