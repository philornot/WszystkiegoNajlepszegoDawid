package com.philornot.siekiera.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.philornot.siekiera.MainActivity
import com.philornot.siekiera.R
import com.philornot.siekiera.config.AppConfig
import com.philornot.siekiera.utils.TimeUtils
import timber.log.Timber
import java.util.TimeZone
import androidx.core.content.edit

/** BroadcastReceiver, który odbiera powiadomienie o odsłonięciu prezentu. */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Czas na odsłonięcie prezentu!")
        showGiftRevealNotification(context)
    }

    private fun showGiftRevealNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Utwórz kanał powiadomień dla Androida 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setContentTitle("Wszystkiego najlepszego") // Tytuł zgodny z wymaganiem
            .setContentText("Twój prezent jest gotowy do otwarcia") // Opis zgodny z wymaganiem
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(pendingIntent)
            .build()

        // Pokaż powiadomienie
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "gift_reveal_channel"
        private const val NOTIFICATION_ID = 1001
    }
}

/** Klasa do planowania powiadomienia o odsłonięciu prezentu. */
object NotificationScheduler {
    // Strefa czasowa Warszawy
    private val WARSAW_TIMEZONE = TimeZone.getTimeZone("Europe/Warsaw")

    // Interfejs dla fabryki PendingIntent, ułatwia mockowanie w testach
    interface PendingIntentFactory {
        fun createBroadcastPendingIntent(
            context: Context,
            requestCode: Int,
            intent: Intent,
            flags: Int,
        ): PendingIntent
    }

    // Implementacja domyślna używająca prawdziwego PendingIntent
    class DefaultPendingIntentFactory : PendingIntentFactory {
        override fun createBroadcastPendingIntent(
            context: Context,
            requestCode: Int,
            intent: Intent,
            flags: Int,
        ): PendingIntent {
            return PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }
    }

    // Domyślna fabryka, którą można podmienić w testach
    var pendingIntentFactory: PendingIntentFactory = DefaultPendingIntentFactory()

    /**
     * Planuje powiadomienie na dzień urodzin.
     *
     * @param context Kontekst aplikacji
     * @param appConfig Konfiguracja aplikacji z datą urodzin
     */
    fun scheduleGiftRevealNotification(context: Context, appConfig: AppConfig) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)

        Timber.d("Rozpoczynam planowanie powiadomienia urodzinowego")

        // Tworzymy PendingIntent, który zostanie wywołany o określonej godzinie
        val pendingIntent = try {
            pendingIntentFactory.createBroadcastPendingIntent(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            Timber.e(e, "Błąd podczas tworzenia PendingIntent")
            return
        }

        // Pobierz datę urodzin z konfiguracji - już jest w strefie czasowej Warszawy
        val calendar = appConfig.getBirthdayDate()

        Timber.d("Planowanie alarmu na datę: ${TimeUtils.formatDate(calendar.time)}")

        // Zaplanuj dokładny alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Dla Androida 12+, sprawdź czy możemy zaplanować dokładne alarmy
            val packageName = context.packageName
            val hasPermission = alarmManager.canScheduleExactAlarms()

            Timber.d("Android 12+ (API ${Build.VERSION.SDK_INT}): Sprawdzanie uprawnień do dokładnych alarmów")
            Timber.d("Pakiet: $packageName, uprawnienie: $hasPermission")

            if (hasPermission) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                    )
                    Timber.d(
                        "Pomyślnie zaplanowano dokładne powiadomienie na ${
                            TimeUtils.formatDate(
                                calendar.time
                            )
                        }"
                    )

                    // Dodatkowa weryfikacja - sprawdź czy alarm został faktycznie zaplanowany
                    // (to wymaga uprawnień administratora, więc może nie działać na wszystkich urządzeniach)
                    if (packageName.contains("debug") && appConfig.isVerboseLoggingEnabled()) {
                        Timber.d("W wersji debug - spróbuj zweryfikować zaplanowane alarmy przez dumpsys (wymaga uprawnień root)")
                    }
                } catch (e: SecurityException) {
                    Timber.e(
                        e,
                        "SecurityException podczas planowania alarmu mimo posiadania uprawnienia canScheduleExactAlarms()"
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Nieznany błąd podczas planowania alarmu")
                }
            } else {
                Timber.w("Nie można zaplanować dokładnych alarmów - brak uprawnień")

                // Zapisz informację, że należy pokazać dialog z prośbą o uprawnienia przy następnym uruchomieniu
                val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
                prefs.edit { putBoolean("show_permissions_request", true) }

                // Zaplanuj alarm niedokładny jako plan awaryjny
                try {
                    Timber.d("Planowanie niedokładnego alarmu jako rozwiązanie awaryjne")
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Nie udało się zaplanować nawet niedokładnego alarmu")
                }
            }
        } else {
            // Dla Androida 11 i starszych
            Timber.d("Android 11 lub starszy (API ${Build.VERSION.SDK_INT}): Planowanie dokładnego alarmu")
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                )
                Timber.d("Zaplanowano dokładne powiadomienie na ${TimeUtils.formatDate(calendar.time)}")
            } catch (e: Exception) {
                Timber.e(e, "Błąd podczas planowania alarmu na starszej wersji Androida")
            }
        }
    }

    /**
     * Metoda kompatybilności wstecznej dla istniejącego kodu.
     *
     * @param context Kontekst aplikacji
     */
    @Deprecated(
        "Użyj wersji z jawnie przekazanym AppConfig",
        ReplaceWith("scheduleGiftRevealNotification(context, appConfig)")
    )
    fun scheduleGiftRevealNotification(context: Context) {
        val appConfig = AppConfig.getInstance(context)
        Timber.d("Używanie przestarzałej metody scheduleGiftRevealNotification bez AppConfig")
        scheduleGiftRevealNotification(context, appConfig)
    }
}
