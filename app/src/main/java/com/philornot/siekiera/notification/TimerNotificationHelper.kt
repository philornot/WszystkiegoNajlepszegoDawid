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
import timber.log.Timber

/**
 * Rozszerzenie NotificationHelper obsługujące powiadomienia dla timera.
 * Zawiera metody do tworzenia i wyświetlania powiadomień timera.
 */
object TimerNotificationHelper {

    // Identyfikator kanału powiadomień dla timera
    private const val CHANNEL_ID_TIMER = "timer_notification_channel"

    // Identyfikator powiadomienia timera
    private const val NOTIFICATION_ID_TIMER = 1003

    /**
     * Inicjalizuje kanał powiadomień dla timera. Powinno być wywołane podczas
     * tworzenia aplikacji.
     *
     * @param context Kontekst aplikacji
     */
    fun initTimerNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Kanał dla powiadomień timera - z wysokim priorytetem
            val timerChannel = NotificationChannel(
                CHANNEL_ID_TIMER,
                context.getString(R.string.timer_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH // Zmienione na HIGH
            ).apply {
                description = context.getString(R.string.timer_notification_channel_description)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500) // Dodanie wibracji
            }

            notificationManager.createNotificationChannel(timerChannel)
            Timber.d("Utworzono kanał powiadomień dla timera")
        }
    }

    /**
     * Wyświetla powiadomienie o zakończeniu odliczania timera.
     *
     * @param context Kontekst aplikacji
     * @param minutes Liczba minut, która była ustawiona na timerze
     */
    fun showTimerCompletedNotification(context: Context, minutes: Int) {
        // Przygotowanie intencji do otwarcia aplikacji po kliknięciu powiadomienia
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_TIMER_FINISHED", true)
            putExtra("TIMER_MINUTES", minutes)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Buduj powiadomienie z wyższym priorytetem i wibracją
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
            .setSmallIcon(R.drawable.notification_timer_icon)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
            .setContentTitle(context.getString(R.string.timer_notification_title))
            .setContentText(context.getString(R.string.timer_notification_text, minutes)).setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.timer_notification_text, minutes))
            ).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Zmienione z DEFAULT na HIGH
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Dodane
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 500, 250, 500)) // Dodano wibracje
            .setContentIntent(pendingIntent).build()

        // Wyświetl powiadomienie
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TIMER, notification)
            Timber.d("Wyświetlono powiadomienie o zakończeniu timera ($minutes minut)")
        } catch (e: SecurityException) {
            Timber.e(e, "Brak uprawnień do wyświetlania powiadomień")
        }
    }

    /**
     * Anuluje aktywne powiadomienie timera.
     *
     * @param context Kontekst aplikacji
     */
    fun cancelTimerNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_TIMER)
            Timber.d("Anulowano powiadomienie timera")
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas anulowania powiadomienia timera")
        }
    }
}