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

            // Kanał dla powiadomień timera
            val timerChannel = NotificationChannel(
                CHANNEL_ID_TIMER, "Powiadomienia timera", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o zakończeniu odliczania timera"
                enableLights(true)
                enableVibration(true)
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
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Buduj powiadomienie
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TIMER)
            .setSmallIcon(R.drawable.notification_timer_icon)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
            .setContentTitle("Lawendowy Timer")
            .setContentText("Odliczanie $minutes minut zakończone!").setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Twoje odliczanie $minutes minut zostało zakończone! Sprawdź, co dalej?")
            ).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Zmienione z HIGH na DEFAULT
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // Zmienione z ALARM na REMINDER
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(pendingIntent)
            .build()

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