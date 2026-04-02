package com.example.bookmymovie.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.repository.MovieRepository
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferCategory
import com.example.bookmymovie.ui.screens.Movie
import com.example.bookmymovie.utils.NotificationHelper
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminCreateOfferViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance().reference

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _theatres = MutableStateFlow<List<CinemaEntry>>(emptyList())
    val theatres: StateFlow<List<CinemaEntry>> = _theatres.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _creationSuccess = MutableStateFlow(false)
    val creationSuccess: StateFlow<Boolean> = _creationSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchMovies()
        fetchTheatres()
    }

    private fun fetchMovies() {
        viewModelScope.launch {
            try {
                _movies.value = MovieRepository.fetchNowPlaying()
            } catch (e: Exception) {
                _errorMessage.value = "Movies Error: ${e.message}"
            }
        }
    }

    private fun fetchTheatres() {
        viewModelScope.launch {
            try {
                val snapshot = database.child("cinemas").get().await()
                val list = mutableListOf<CinemaEntry>()
                snapshot.children.forEach { child ->
                    val name = child.child("name").value as? String ?: ""
                    val placeId = child.child("placeId").value as? String ?: child.key ?: ""
                    if (name.isNotEmpty()) {
                        list.add(CinemaEntry(name = name, placeId = placeId))
                    }
                }
                _theatres.value = list.sortedBy { it.name }
            } catch (e: Exception) {
                _errorMessage.value = "Theatres Error: ${e.message}"
            }
        }
    }

    fun resetSuccess() { _creationSuccess.value = false }

    fun createPlatformOffer(context: Context, offer: Offer) {
        _isLoading.value = true
        val offerId = database.child("offers").push().key ?: return
        val finalOffer = offer.copy(id = offerId)
        
        database.child("offers").child(offerId).setValue(finalOffer)
            .addOnSuccessListener {
                _isLoading.value = false
                _creationSuccess.value = true
                
                // Create notification for all users about the new offer
                val notificationHelper = NotificationHelper(context)
                notificationHelper.addOfferNotification(
                    offerTitle = offer.title,
                    offerDescription = offer.description,
                    offerId = offerId
                )
            }
            .addOnFailureListener {
                _isLoading.value = false
                _errorMessage.value = it.message
            }
    }
}
