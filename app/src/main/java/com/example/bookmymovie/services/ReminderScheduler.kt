package com.example.bookmymovie.services

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.bookmymovie.workers.ShowReminderWorker
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    /**
     * Schedule a reminder notification + WhatsApp message 15 minutes before the show.
     *
     * @param context       Android context
     * @param movieName     Name of the movie
     * @param showDate      Date string in "yyyy-MM-dd" format
     * @param showTime      Time string (e.g. "06:30 PM")
     * @param theaterName   Name of the theater / cinema
     * @param phoneNumber   User's phone number with country code (e.g. "+919876543210")
     * @param seats         Comma-separated seat labels (e.g. "A1, A2, A3")
     */
    fun scheduleShowReminder(
        context: Context,
        movieName: String,
        showDate: String,
        showTime: String,
        theaterName: String,
        phoneNumber: String,
        seats: String
    ) {
        try {
            // Parse the show date and time to calculate delay
            val dateTimeString = "$showDate $showTime"

            // Try multiple date formats since showDate might come as "yyyy-MM-dd" or "dd MMM yyyy"
            val possibleFormats = listOf(
                SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH),
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH),
                SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.ENGLISH),
                SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
            )

            var showTimeMillis: Long? = null
            for (fmt in possibleFormats) {
                try {
                    val parsed = fmt.parse(dateTimeString)
                    if (parsed != null) {
                        showTimeMillis = parsed.time
                        break
                    }
                } catch (_: Exception) { }
            }

            if (showTimeMillis == null) {
                Log.w("ReminderScheduler", "Could not parse show date/time: $dateTimeString")
                return
            }

            // Reminder = 15 minutes before show
            val reminderTimeMillis = showTimeMillis - (15 * 60 * 1000)
            val currentTimeMillis = System.currentTimeMillis()
            val delayMillis = reminderTimeMillis - currentTimeMillis

            if (delayMillis <= 0) {
                Log.w("ReminderScheduler", "Show is already starting or has passed. No reminder scheduled.")
                return
            }

            val inputData = Data.Builder()
                .putString(ShowReminderWorker.KEY_MOVIE_NAME, movieName)
                .putString(ShowReminderWorker.KEY_SHOW_TIME, showTime)
                .putString(ShowReminderWorker.KEY_SHOW_DATE, showDate)
                .putString(ShowReminderWorker.KEY_THEATER_NAME, theaterName)
                .putString(ShowReminderWorker.KEY_PHONE_NUMBER, phoneNumber)
                .putString(ShowReminderWorker.KEY_SEATS, seats)
                .build()

            val reminderWork = OneTimeWorkRequestBuilder<ShowReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("show_reminder_${movieName}_${showTime}")
                .build()

            WorkManager.getInstance(context).enqueue(reminderWork)

            val delayMinutes = delayMillis / (60 * 1000)
            Log.d("ReminderScheduler", "Reminder scheduled for '$movieName' in $delayMinutes minutes")

        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Error scheduling reminder", e)
        }
    }
}
