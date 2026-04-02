package com.example.bookmymovie.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferApprovalStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OfferAdminViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance().reference
    
    private val _pendingOffers = MutableStateFlow<List<Offer>>(emptyList())
    val pendingOffers: StateFlow<List<Offer>> = _pendingOffers.asStateFlow()

    private val _approvedOffers = MutableStateFlow<List<Offer>>(emptyList())
    val approvedOffers: StateFlow<List<Offer>> = _approvedOffers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _historyOffers = MutableStateFlow<List<Offer>>(emptyList())
    val historyOffers: StateFlow<List<Offer>> = _historyOffers.asStateFlow()

    init {
        loadPendingOffers()
        loadApprovedOffers()
        loadHistoryOffers()
    }

    private fun loadPendingOffers() {
        database.child("offers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Offer>()
                for (child in snapshot.children) {
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null && offer.approvalStatus == OfferApprovalStatus.PENDING.name) {
                        list.add(offer)
                    }
                }
                _pendingOffers.value = list
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        })
    }

    fun updateOfferStatus(offerId: String, newStatus: OfferApprovalStatus) {
        database.child("offers").child(offerId).child("approvalStatus").setValue(newStatus.name)
    }

    private fun loadApprovedOffers() {
        database.child("offers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Offer>()
                for (child in snapshot.children) {
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null && offer.approvalStatus == OfferApprovalStatus.APPROVED.name) {
                        list.add(offer)
                    }
                }
                _approvedOffers.value = list.sortedByDescending { it.startDate }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadHistoryOffers() {
        database.child("offers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Offer>()
                for (child in snapshot.children) {
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null && (offer.approvalStatus == OfferApprovalStatus.APPROVED.name || 
                        offer.approvalStatus == OfferApprovalStatus.REJECTED.name)) {
                        list.add(offer)
                    }
                }
                _historyOffers.value = list
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
