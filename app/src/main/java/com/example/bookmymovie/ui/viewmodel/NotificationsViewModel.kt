package com.example.bookmymovie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.firebase.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var notificationsListener: ValueEventListener? = null

    fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true

        notificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notificationsList = mutableListOf<Notification>()
                var unreadCount = 0
                
                for (child in snapshot.children) {
                    val notification = child.getValue(Notification::class.java)
                    if (notification != null) {
                        notificationsList.add(notification)
                        if (!notification.isRead) {
                            unreadCount++
                        }
                    }
                }
                
                // Sort by timestamp descending (newest first)
                notificationsList.sortByDescending { it.timestamp }
                
                _notifications.value = notificationsList
                _unreadCount.value = unreadCount
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        }

        db.child("notifications").child(uid)
            .addValueEventListener(notificationsListener ?: return)
    }

    fun markAsRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("notifications").child(uid).child(notificationId)
            .child("isRead").setValue(true)
    }

    fun deleteNotification(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.child("notifications").child(uid).child(notificationId).removeValue()
    }

    override fun onCleared() {
        super.onCleared()
        val uid = auth.currentUser?.uid ?: return
        if (notificationsListener != null) {
            db.child("notifications").child(uid).removeEventListener(notificationsListener!!)
        }
    }
}
