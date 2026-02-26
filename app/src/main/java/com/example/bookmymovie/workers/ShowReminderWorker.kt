package com.example.bookmymovie.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bookmymovie.R
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class ShowReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MOVIE_NAME = "movie_name"
        const val KEY_SHOW_TIME = "show_time"
        const val KEY_SHOW_DATE = "show_date"
        const val KEY_THEATER_NAME = "theater_name"
        const val KEY_PHONE_NUMBER = "phone_number"
        const val KEY_SEATS = "seats"
        const val CHANNEL_ID = "show_reminder_channel"
    }

    override suspend fun doWork(): Result {
        val movieName = inputData.getString(KEY_MOVIE_NAME) ?: "Your movie"
        val showTime = inputData.getString(KEY_SHOW_TIME) ?: ""
        val showDate = inputData.getString(KEY_SHOW_DATE) ?: ""
        val theaterName = inputData.getString(KEY_THEATER_NAME) ?: ""
        val phoneNumber = inputData.getString(KEY_PHONE_NUMBER) ?: ""
        val seats = inputData.getString(KEY_SEATS) ?: ""

        // 1. Send local push notification
        sendNotification(movieName, showTime, theaterName)

        // 2. Send WhatsApp reminder via Firebase Cloud Function
        if (phoneNumber.isNotEmpty()) {
            try {
                val data = hashMapOf(
                    "phone" to phoneNumber,
                    "movieName" to movieName,
                    "showDate" to showDate,
                    "showTime" to showTime,
                    "theaterName" to theaterName,
                    "seats" to seats
                )
                Firebase.functions
                    .getHttpsCallable("sendShowReminderWhatsApp")
                    .call(data)
                Log.d("ShowReminder", "WhatsApp reminder triggered for $phoneNumber")
            } catch (e: Exception) {
                Log.e("ShowReminder", "Failed to send WhatsApp reminder", e)
            }
        }

        return Result.success()
    }

    private fun sendNotification(movieName: String, showTime: String, theaterName: String) {
        createNotificationChannel()

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("\uD83C\uDFAC Show starts in 15 minutes!")
            .setContentText("$movieName at $theaterName - $showTime")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Your show for $movieName starts in just 15 minutes!\n" +
                                "Theater: $theaterName\n" +
                                "Show Time: $showTime\n\n" +
                                "Enjoy the movie! \uD83C\uDF7F"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(applicationContext)
                    .notify(notificationId, notification)
            }
        } else {
            NotificationManagerCompat.from(applicationContext)
                .notify(notificationId, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Show Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming movie shows"
            }

            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
