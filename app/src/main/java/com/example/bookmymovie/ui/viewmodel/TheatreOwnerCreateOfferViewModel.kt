package com.example.bookmymovie.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferApprovalStatus
import com.example.bookmymovie.utils.NotificationHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.bookmymovie.data.repository.MovieRepository
import com.example.bookmymovie.ui.screens.Movie
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID

data class SimpleTheatreData(val id: String, val name: String)

class TheatreOwnerCreateOfferViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance().reference
    
    private val _ownerTheatres = MutableStateFlow<List<SimpleTheatreData>>(emptyList())
    val ownerTheatres: StateFlow<List<SimpleTheatreData>> = _ownerTheatres.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _creationSuccess = MutableStateFlow(false)
    val creationSuccess: StateFlow<Boolean> = _creationSuccess.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    init {
        fetchMovies()
    }

    private fun fetchMovies() {
        viewModelScope.launch {
            try {
                _movies.value = MovieRepository.fetchNowPlaying()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load movies: ${e.message}"
            }
        }
    }

    fun fetchTheatresForOwner(ownerId: String) {
        if (ownerId.isEmpty()) return
        database.child("theatre_owners").child(ownerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val placeId = snapshot.child("placeId").getValue(String::class.java)
                    val cinemaName = snapshot.child("cinemaName").getValue(String::class.java)
                    if (!placeId.isNullOrEmpty() && !cinemaName.isNullOrEmpty()) {
                        _ownerTheatres.value = listOf(SimpleTheatreData(id = placeId, name = cinemaName))
                    } else {
                        _ownerTheatres.value = emptyList()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _errorMessage.value = "Failed to load theatres."
                }
            })
    }

    fun createOffer(context: Context, offer: Offer) {
        _isLoading.value = true
        _errorMessage.value = null
        
        // Finalize offer attributes before save. Theatre owner offers default strictly to PENDING.
        val offerToSave = offer.copy(
            id = if (offer.id.isEmpty()) UUID.randomUUID().toString() else offer.id,
            approvalStatus = OfferApprovalStatus.PENDING.name,
            couponCode = offer.couponCode.uppercase()
        )
        
        database.child("offers").child(offerToSave.id).setValue(offerToSave)
            .addOnSuccessListener {
                _isLoading.value = false
                _creationSuccess.value = true
                
                // Create notification for all users about the new offer (pending approval)
                val notificationHelper = NotificationHelper(context)
                notificationHelper.addOfferNotification(
                    offerTitle = offer.title,
                    offerDescription = offer.description,
                    offerId = offerToSave.id
                )
            }
            .addOnFailureListener {
                _isLoading.value = false
                _errorMessage.value = "Failed to create offer."
            }
    }
}
