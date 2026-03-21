package com.example.bookmymovie.data.repository

import com.example.bookmymovie.model.Offer
import com.example.bookmymovie.model.OfferStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OffersRepository {
    private val database = FirebaseDatabase.getInstance()
    private val offersRef = database.getReference("offers")

    /**
     * Get all approved offers as a real-time Flow
     * Updates automatically when offers are approved/rejected
     */
    fun getApprovedOffers(): Flow<List<Offer>> = callbackFlow {
        val query = offersRef.orderByChild("status").equalTo(OfferStatus.APPROVED.name)
        
        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offers = mutableListOf<Offer>()
                snapshot.children.forEach { child ->
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null) {
                        offers.add(offer.copy(id = child.key ?: ""))
                    }
                }
                trySend(offers.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            query.removeEventListener(listener)
        }
    }

    /**
     * Get approved offers for a specific theatre
     */
    fun getOffersForTheatre(theatreId: String): Flow<List<Offer>> = callbackFlow {
        val query = offersRef
            .orderByChild("theatreId")
            .equalTo(theatreId)
        
        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offers = mutableListOf<Offer>()
                snapshot.children.forEach { child ->
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null && offer.status == OfferStatus.APPROVED.name) {
                        offers.add(offer.copy(id = child.key ?: ""))
                    }
                }
                trySend(offers.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            query.removeEventListener(listener)
        }
    }

    /**
     * Get approved offers for a specific city (including global offers)
     */
    fun getOffersForCity(cityId: String): Flow<List<Offer>> = callbackFlow {
        val listener = offersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offers = mutableListOf<Offer>()
                snapshot.children.forEach { child ->
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null && offer.status == OfferStatus.APPROVED.name) {
                        // Include if: applicable to all cities OR matches city
                        if (offer.applicableToAllCities || offer.cityId == cityId) {
                            offers.add(offer.copy(id = child.key ?: ""))
                        }
                    }
                }
                trySend(offers.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            offersRef.removeEventListener(listener)
        }
    }

    /**
     * Get a specific offer by ID
     */
    suspend fun getOfferById(offerId: String): Offer? {
        return try {
            val snapshot = offersRef.child(offerId).get().await()
            val mapped = snapshot.getValue(Offer::class.java)
            if (mapped == null) return null
            // Some DB exports use `active` field name instead of `isActive` — prefer explicit value when present
            val activeFromDb = snapshot.child("isActive").getValue(Boolean::class.java)
                ?: snapshot.child("active").getValue(Boolean::class.java)
                ?: mapped.isActive
            // Some DB exports provide `statusEnum` or `status` inconsistently; prefer `status` if present
            val statusFromDb = snapshot.child("status").getValue(String::class.java) ?: mapped.status
            mapped.copy(id = offerId, isActive = activeFromDb, status = statusFromDb)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create a new offer (theatre owner creates)
     * Status will be PENDING for admin approval
     */
    suspend fun createOffer(offer: Offer): String {
        return try {
            val newOfferRef = offersRef.push()
            val offerId = newOfferRef.key ?: throw Exception("Failed to generate offer ID")
            val offerWithId = offer.copy(
                id = offerId,
                createdAt = System.currentTimeMillis()
            )
            newOfferRef.setValue(offerWithId).await()
            offerId
        } catch (e: Exception) {
            throw Exception("Failed to create offer: ${e.message}")
        }
    }

    /**
     * Update offer status by admin (approve/reject)
     */
    suspend fun updateOfferStatus(offerId: String, status: OfferStatus): Boolean {
        return try {
            offersRef.child(offerId).child("status").setValue(status.name).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get pending offers for admin approval
     */
    fun getPendingOffers(): Flow<List<Offer>> = callbackFlow {
        val query = offersRef.orderByChild("status").equalTo(OfferStatus.PENDING.name)
        
        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offers = mutableListOf<Offer>()
                snapshot.children.forEach { child ->
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null) {
                        offers.add(offer.copy(id = child.key ?: ""))
                    }
                }
                trySend(offers.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            query.removeEventListener(listener)
        }
    }

    /**
     * Get offers created by a specific theatre owner
     */
    fun getOffersByTheatreOwner(theatreOwnerId: String): Flow<List<Offer>> = callbackFlow {
        val query = offersRef.orderByChild("theatreOwnerId").equalTo(theatreOwnerId)
        
        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offers = mutableListOf<Offer>()
                snapshot.children.forEach { child ->
                    val offer = child.getValue(Offer::class.java)
                    if (offer != null) {
                        offers.add(offer.copy(id = child.key ?: ""))
                    }
                }
                trySend(offers.toList())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            query.removeEventListener(listener)
        }
    }

    /**
     * Delete an offer
     */
    suspend fun deleteOffer(offerId: String): Boolean {
        return try {
            offersRef.child(offerId).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private var instance: OffersRepository? = null

        fun getInstance(): OffersRepository {
            if (instance == null) {
                instance = OffersRepository()
            }
            return instance!!
        }
    }
}
