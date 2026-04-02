package com.example.bookmymovie.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bookmymovie.R
import com.example.bookmymovie.firebase.Notification
import com.google.firebase.database.FirebaseDatabase

class NotificationHelper(private val context: Context) {
    private val CHANNEL_ID = "movie_notifications"
    private val NOTIFICATION_ID = 101
    private val db = FirebaseDatabase.getInstance().reference

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Movie Updates"
            val descriptionText = "Get notified about latest movies and offers"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.bookmymovielogo2)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }

    /**
     * Add a new offer notification visible to all users
     */
    fun addOfferNotification(offerTitle: String, offerDescription: String, offerId: String) {
        // Get all users and add the notification to each
        db.child("users").get().addOnSuccessListener { snapshot ->
            for (userSnap in snapshot.children) {
                val userId = userSnap.key ?: continue
                val notificationId = db.child("notifications").child(userId).push().key ?: return@addOnSuccessListener
                
                val notification = Notification(
                    id = notificationId,
                    userId = userId,
                    title = "New Offer: $offerTitle",
                    message = offerDescription,
                    type = "offer",
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    relatedId = offerId
                )
                
                db.child("notifications").child(userId).child(notificationId).setValue(notification)
            }
        }
    }

    /**
     * Add a booking confirmation notification for a user
     */
    fun addBookingNotification(userId: String, movieTitle: String, theatreName: String, showDate: String, bookingId: String) {
        val notificationId = db.child("notifications").child(userId).push().key ?: return
        
        val notification = Notification(
            id = notificationId,
            userId = userId,
            title = "Booking Confirmed",
            message = "Your ticket for $movieTitle at $theatreName on $showDate has been booked successfully.",
            type = "booking",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedId = bookingId
        )
        
        db.child("notifications").child(userId).child(notificationId).setValue(notification)
        showNotification("Booking Confirmed", "Your movie ticket has been booked!")
    }

    /**
     * Add a refund notification for a user
     */
    fun addRefundNotification(userId: String, movieTitle: String, refundAmount: Double, transactionId: String) {
        val notificationId = db.child("notifications").child(userId).push().key ?: return
        
        val notification = Notification(
            id = notificationId,
            userId = userId,
            title = "Refund Processed",
            message = "Your refund of ₹$refundAmount for $movieTitle has been processed and will appear in your account within 3-5 business days.",
            type = "refund",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedId = transactionId
        )
        
        db.child("notifications").child(userId).child(notificationId).setValue(notification)
        showNotification("Refund Processed", "Refund of ₹$refundAmount has been initiated")
    }

    /**
     * Add a general update notification for a user
     */
    fun addUpdateNotification(userId: String, title: String, message: String) {
        val notificationId = db.child("notifications").child(userId).push().key ?: return
        
        val notification = Notification(
            id = notificationId,
            userId = userId,
            title = title,
            message = message,
            type = "update",
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedId = ""
        )
        
        db.child("notifications").child(userId).child(notificationId).setValue(notification)
    }
}
