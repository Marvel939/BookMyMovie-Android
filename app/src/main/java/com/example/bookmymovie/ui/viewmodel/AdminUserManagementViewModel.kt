package com.example.bookmymovie.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.bookmymovie.firebase.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// ─── ViewModel ────────────────────────────────────────────────────────────────

class AdminUserManagementViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance()
    private val usersRef = db.getReference("users")

    // All users state
    var allUsers by mutableStateOf<List<User>>(emptyList())
        private set

    var isLoadingUsers by mutableStateOf(false)
        private set

    // Selected user for editing
    var selectedUser by mutableStateOf<User?>(null)
        private set

    var isEditingUser by mutableStateOf(false)
        private set

    var isSavingUser by mutableStateOf(false)
        private set

    // Dialogs and status
    var showEditDialog by mutableStateOf(false)
        private set

    var showDeleteDialog by mutableStateOf(false)
        private set

    var actionMessage by mutableStateOf<String?>(null)
        private set

    var actionError by mutableStateOf<String?>(null)
        private set

    // ── Editable fields in dialog ──────────────────────────────────────────

    var editFirstName by mutableStateOf("")
        private set

    var editLastName by mutableStateOf("")
        private set

    var editPhone by mutableStateOf("")
        private set

    var editCity by mutableStateOf("")
        private set

    var editAddress by mutableStateOf("")
        private set

    var editGender by mutableStateOf("")
        private set

    var editDob by mutableStateOf("")
        private set

    var editPermissions by mutableStateOf("standard")
        private set

    // Update functions for editable fields
    fun updateEditFirstName(value: String) {
        editFirstName = value
    }

    fun updateEditLastName(value: String) {
        editLastName = value
    }

    fun updateEditPhone(value: String) {
        editPhone = value
    }

    fun updateEditCity(value: String) {
        editCity = value
    }

    fun updateEditAddress(value: String) {
        editAddress = value
    }

    fun updateEditGender(value: String) {
        editGender = value
    }

    fun updateEditDob(value: String) {
        editDob = value
    }

    fun updateEditPermissions(value: String) {
        editPermissions = value
    }

    // ── Load all users ──────────────────────────────────────────────────────

    fun loadAllUsers() {
        isLoadingUsers = true
        actionError = null

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isLoadingUsers = false
                val users = mutableListOf<User>()

                for (childSnapshot in snapshot.children) {
                    val user = childSnapshot.getValue(User::class.java)
                    if (user != null) {
                        users.add(user)
                    }
                }

                // Sort by creation date (newest first)
                allUsers = users.sortedByDescending { it.createdAt }
            }

            override fun onCancelled(error: DatabaseError) {
                isLoadingUsers = false
                actionError = "Failed to load users: ${error.message}"
                Log.e("AdminUserManagementVM", "Error loading users", error.toException())
            }
        })
    }

    // ── Open edit dialog ────────────────────────────────────────────────────

    fun openEditDialog(user: User) {
        selectedUser = user
        editFirstName = user.firstName
        editLastName = user.lastName
        editPhone = user.phone
        editCity = user.city
        editAddress = user.address
        editGender = user.gender
        editDob = user.dob
        editPermissions = user.permissions
        isEditingUser = true
        showEditDialog = true
        actionError = null
    }

    fun closeEditDialog() {
        showEditDialog = false
        isEditingUser = false
        selectedUser = null
        actionError = null
    }

    // ── Update user details ─────────────────────────────────────────────────

    fun saveUserChanges() {
        val user = selectedUser ?: return

        if (editFirstName.isBlank() || editLastName.isBlank()) {
            actionError = "First name and last name cannot be empty"
            return
        }

        isSavingUser = true
        actionError = null
        actionMessage = null

        val updates = mapOf(
            "firstName" to editFirstName.trim(),
            "lastName" to editLastName.trim(),
            "phone" to editPhone.trim(),
            "city" to editCity.trim(),
            "address" to editAddress.trim(),
            "gender" to editGender.trim(),
            "dob" to editDob.trim(),
            "permissions" to editPermissions
        )

        usersRef.child(user.userId).updateChildren(updates)
            .addOnSuccessListener {
                isSavingUser = false
                actionMessage = "User details updated successfully"
                loadAllUsers() // Refresh list
                closeEditDialog()
            }
            .addOnFailureListener { error ->
                isSavingUser = false
                actionError = "Failed to update user: ${error.message}"
                Log.e("AdminUserManagementVM", "Error updating user", error)
            }
    }

    // ── Block/Unblock user ──────────────────────────────────────────────────

    fun toggleUserBlockStatus(user: User) {
        val newStatus = if (user.status == "active") "blocked" else "active"
        val statusLabel = if (newStatus == "blocked") "blocked" else "unblocked"

        usersRef.child(user.userId).child("status").setValue(newStatus)
            .addOnSuccessListener {
                actionMessage = "User $statusLabel successfully"
                loadAllUsers() // Refresh list
            }
            .addOnFailureListener { error ->
                actionError = "Failed to update user status: ${error.message}"
                Log.e("AdminUserManagementVM", "Error toggling block status", error)
            }
    }

    // ── Soft delete (purge) user ────────────────────────────────────────────

    fun softDeleteUser(userId: String) {
        usersRef.child(userId).child("isDeleted").setValue(true)
            .addOnSuccessListener {
                // Update local state instead of reloading to avoid page refresh
                allUsers = allUsers.map { user ->
                    if (user.userId == userId) user.copy(isDeleted = true) else user
                }
                actionMessage = "User soft deleted successfully"
                closeEditDialog()
            }
            .addOnFailureListener { error ->
                actionError = "Failed to soft delete user: ${error.message}"
                Log.e("AdminUserManagementVM", "Error soft deleting user", error)
            }
    }

    // ── Hard delete user ────────────────────────────────────────────────────

    fun hardDeleteUser(userId: String) {
        val db = FirebaseDatabase.getInstance().reference
        var completedTasks = 0
        val totalTasks = 7 // users, bookings, notifications, wishlists, library, chat, reviews, seats
        
        fun checkCompletion() {
            completedTasks++
            if (completedTasks >= totalTasks) {
                actionMessage = "User and all related data deleted successfully"
                loadAllUsers()
                closeEditDialog()
            }
        }
        
        // 1. Delete user record from users node - this deletes everything including wallet & refunds
        db.child("users").child(userId).removeValue()
            .addOnSuccessListener { checkCompletion() }
            .addOnFailureListener { error ->
                actionError = "Failed to delete user: ${error.message}"
                Log.e("AdminUserManagementVM", "Error deleting user record", error)
                checkCompletion() // Still continue with other deletions
            }
        
        // 2. Delete user bookings
        db.child("bookings").child(userId).removeValue()
            .addOnSuccessListener { checkCompletion() }
            .addOnFailureListener { error ->
                Log.d("AdminUserManagementVM", "Note: No bookings found for user (ok if first-time user)")
                checkCompletion()
            }
        
        // 3. Delete user notifications
        db.child("notifications").child(userId).removeValue()
            .addOnSuccessListener { checkCompletion() }
            .addOnFailureListener { error ->
                Log.d("AdminUserManagementVM", "Note: No notifications found for user (ok if first-time user)")
                checkCompletion()
            }
        
        // 4. Delete user wishlists
        db.child("wishlists").child(userId).removeValue()
            .addOnSuccessListener { checkCompletion() }
            .addOnFailureListener { error ->
                Log.d("AdminUserManagementVM", "Note: No wishlists found for user")
                checkCompletion()
            }
        
        // 5. Delete user library (movies, rentals, purchases)
        db.child("user_library").child(userId).removeValue()
            .addOnSuccessListener { checkCompletion() }
            .addOnFailureListener { error ->
                Log.d("AdminUserManagementVM", "Note: No library items found for user")
                checkCompletion()
            }
        
        // 6. Delete user chat history and reviews
        val reviewDeletionTasks = 2 // chat + reviews
        var reviewTasksCompleted = 0
        
        fun onReviewTaskComplete() {
            reviewTasksCompleted++
            if (reviewTasksCompleted >= reviewDeletionTasks) {
                checkCompletion()
            }
        }
        
        // Delete AI chat history
        db.child("ai_chat_history").child(userId).removeValue()
            .addOnSuccessListener { onReviewTaskComplete() }
            .addOnFailureListener { error ->
                Log.d("AdminUserManagementVM", "Note: No chat history found for user")
                onReviewTaskComplete()
            }
        
        // Delete all reviews by this user across all movies
        db.child("reviews").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    snapshot.children.forEach { movieSnap ->
                        movieSnap.children.forEach { reviewSnap ->
                            val isUserReview = reviewSnap.child("userId").getValue(String::class.java) == userId
                            if (isUserReview) {
                                reviewSnap.ref.removeValue()
                            }
                        }
                    }
                }
                onReviewTaskComplete()
            }
            .addOnFailureListener { error ->
                Log.d("AdminUserManagementVM", "Note: Error deleting reviews (ok if none exist)")
                onReviewTaskComplete()
            }
        
        // 7. Delete all booked seats for this user
        db.child("seats").get()
            .addOnSuccessListener { seatsSnapshot ->
                if (seatsSnapshot.exists()) {
                    // Iterate through places -> screens -> showtimes -> seats
                    seatsSnapshot.children.forEach { placeSnap ->
                        placeSnap.children.forEach { screenSnap ->
                            screenSnap.children.forEach { showtimeSnap ->
                                showtimeSnap.children.forEach { seatSnap ->
                                    val bookedByUid = seatSnap.child("bookedByUid").getValue(String::class.java) ?: ""
                                    if (bookedByUid == userId) {
                                        // Release this seat
                                        seatSnap.child("booked").ref.setValue(false)
                                        seatSnap.child("bookedByUid").ref.setValue("")
                                    }
                                }
                            }
                        }
                    }
                }
                checkCompletion()
            }
            .addOnFailureListener { error ->
                Log.d("AdminUserManagementVM", "Note: Error releasing booked seats (ok if none exist)")
                checkCompletion()
            }
    }

    // ── Update user permissions ─────────────────────────────────────────────

    fun updateUserPermissions(userId: String, newPermission: String) {
        usersRef.child(userId).child("permissions").setValue(newPermission)
            .addOnSuccessListener {
                actionMessage = "User permissions updated successfully"
                loadAllUsers() // Refresh list
            }
            .addOnFailureListener { error ->
                actionError = "Failed to update permissions: ${error.message}"
                Log.e("AdminUserManagementVM", "Error updating permissions", error)
            }
    }

    // ── Restore soft deleted user ───────────────────────────────────────────

    fun restoreSoftDeletedUser(userId: String) {
        usersRef.child(userId).child("isDeleted").setValue(false)
            .addOnSuccessListener {
                // Update local state instead of reloading to avoid page refresh
                allUsers = allUsers.map { user ->
                    if (user.userId == userId) user.copy(isDeleted = false) else user
                }
                actionMessage = "User restored successfully"
            }
            .addOnFailureListener { error ->
                actionError = "Failed to restore user: ${error.message}"
                Log.e("AdminUserManagementVM", "Error restoring user", error)
            }
    }

    // ── Clear messages ──────────────────────────────────────────────────────

    fun clearMessages() {
        actionMessage = null
        actionError = null
    }
}
