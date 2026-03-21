package com.example.bookmymovie.data.repository

import com.example.bookmymovie.model.Coupon
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CouponsRepository {
    private val database = FirebaseDatabase.getInstance()
    private val couponsRef = database.getReference("offer_coupons")

    /**
     * Get a coupon by its code
     */
    suspend fun getCouponByCode(couponCode: String): Coupon? {
        return try {
            val snapshot = couponsRef.orderByChild("code").equalTo(couponCode).get().await()
            val child = snapshot.children.firstOrNull() ?: return null
            val mapped = child.getValue(Coupon::class.java)
            // Some DB exports use `active` key instead of `isActive` — prefer explicit child value when present
            val activeFromDb = child.child("isActive").getValue(Boolean::class.java)
                ?: child.child("active").getValue(Boolean::class.java)
                ?: mapped?.isActive
                ?: true
            mapped?.copy(id = child.key ?: "", isActive = activeFromDb)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get a coupon by ID
     */
    suspend fun getCouponById(couponId: String): Coupon? {
        return try {
            val snapshot = couponsRef.child(couponId).get().await()
            val mapped = snapshot.getValue(Coupon::class.java)
            val activeFromDb = snapshot.child("isActive").getValue(Boolean::class.java)
                ?: snapshot.child("active").getValue(Boolean::class.java)
                ?: mapped?.isActive
                ?: true
            mapped?.copy(id = couponId, isActive = activeFromDb)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all coupons for a specific offer
     */
    fun getCouponsByOffer(offerId: String): Flow<List<Coupon>> = callbackFlow {
        val query = couponsRef.orderByChild("offerId").equalTo(offerId)
        
        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val coupons = mutableListOf<Coupon>()
                snapshot.children.forEach { child ->
                    val mapped = child.getValue(Coupon::class.java)
                    if (mapped != null) {
                        val activeFromDb = child.child("isActive").getValue(Boolean::class.java)
                            ?: child.child("active").getValue(Boolean::class.java)
                            ?: mapped.isActive
                        coupons.add(mapped.copy(id = child.key ?: "", isActive = activeFromDb))
                    }
                }
                trySend(coupons.toList())
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
     * Create a new coupon for an offer
     */
    suspend fun createCoupon(coupon: Coupon): String {
        return try {
            val newCouponRef = couponsRef.push()
            val couponId = newCouponRef.key ?: throw Exception("Failed to generate coupon ID")
            val couponWithId = coupon.copy(
                id = couponId,
                createdAt = System.currentTimeMillis()
            )
            newCouponRef.setValue(couponWithId).await()
            couponId
        } catch (e: Exception) {
            throw Exception("Failed to create coupon: ${e.message}")
        }
    }

    /**
     * Redeem a coupon by incrementing redemption count and adding user to used list
     */
    suspend fun redeemCoupon(couponId: String, userId: String): Boolean {
        return try {
            val currentCoupon = getCouponById(couponId) ?: return false
            
            // Increment redemption count
            couponsRef.child(couponId).child("redemptionCount")
                .setValue(currentCoupon.redemptionCount + 1).await()
            
            // Add user to used list
            couponsRef.child(couponId).child("usedByUsers").child(userId)
                .setValue(true).await()
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update coupon redemption count
     */
    suspend fun updateRedemptionCount(couponId: String, count: Int): Boolean {
        return try {
            couponsRef.child(couponId).child("redemptionCount").setValue(count).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a user has already used a coupon
     */
    suspend fun hasUserUsedCoupon(couponId: String, userId: String): Boolean {
        return try {
            val coupon = getCouponById(couponId) ?: return false
            coupon.hasUserAlreadyUsed(userId)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deactivate a coupon
     */
    suspend fun deactivateCoupon(couponId: String): Boolean {
        return try {
            couponsRef.child(couponId).child("isActive").setValue(false).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete a coupon
     */
    suspend fun deleteCoupon(couponId: String): Boolean {
        return try {
            couponsRef.child(couponId).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private var instance: CouponsRepository? = null

        fun getInstance(): CouponsRepository {
            if (instance == null) {
                instance = CouponsRepository()
            }
            return instance!!
        }
    }
}
