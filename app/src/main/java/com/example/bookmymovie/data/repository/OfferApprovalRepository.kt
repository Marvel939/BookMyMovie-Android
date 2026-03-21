package com.example.bookmymovie.data.repository

import com.example.bookmymovie.model.ApprovalStatus
import com.example.bookmymovie.model.OfferApproval
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OfferApprovalRepository {
    private val database = FirebaseDatabase.getInstance()
    private val approvalsRef = database.getReference("offer_approvals")

    /**
     * Get all pending approvals for admin review
     */
    fun getPendingApprovals(): Flow<List<OfferApproval>> = callbackFlow {
        val query = approvalsRef.orderByChild("status").equalTo(ApprovalStatus.PENDING.name)
        
        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val approvals = mutableListOf<OfferApproval>()
                snapshot.children.forEach { child ->
                    val approval = child.getValue(OfferApproval::class.java)
                    if (approval != null) {
                        approvals.add(approval.copy(id = child.key ?: ""))
                    }
                }
                // Sort by creation date (newest first)
                trySend(approvals.sortedByDescending { it.createdAt })
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
     * Get approval history (approved/rejected offers)
     */
    fun getApprovalHistory(): Flow<List<OfferApproval>> = callbackFlow {
        val listener = approvalsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val approvals = mutableListOf<OfferApproval>()
                snapshot.children.forEach { child ->
                    val approval = child.getValue(OfferApproval::class.java)
                    if (approval != null && approval.status != ApprovalStatus.PENDING.name) {
                        approvals.add(approval.copy(id = child.key ?: ""))
                    }
                }
                // Sort by review date (newest first)
                trySend(approvals.sortedByDescending { it.reviewedAt })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose {
            approvalsRef.removeEventListener(listener)
        }
    }

    /**
     * Get approval requests from a specific theatre owner
     */
    fun getApprovalsByTheatreOwner(theatreOwnerId: String): Flow<List<OfferApproval>> = callbackFlow {
        val query = approvalsRef.orderByChild("theatreOwnerId").equalTo(theatreOwnerId)
        
        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val approvals = mutableListOf<OfferApproval>()
                snapshot.children.forEach { child ->
                    val approval = child.getValue(OfferApproval::class.java)
                    if (approval != null) {
                        approvals.add(approval.copy(id = child.key ?: ""))
                    }
                }
                trySend(approvals.sortedByDescending { it.createdAt })
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
     * Get a specific approval by ID
     */
    suspend fun getApprovalById(approvalId: String): OfferApproval? {
        return try {
            val snapshot = approvalsRef.child(approvalId).get().await()
            snapshot.getValue(OfferApproval::class.java)?.copy(id = approvalId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create a new approval request (sent when theatre owner creates an offer)
     */
    suspend fun createApprovalRequest(approval: OfferApproval): String {
        return try {
            val newApprovalRef = approvalsRef.push()
            val approvalId = newApprovalRef.key ?: throw Exception("Failed to generate approval ID")
            val approvalWithId = approval.copy(
                id = approvalId,
                createdAt = System.currentTimeMillis(),
                status = ApprovalStatus.PENDING.name
            )
            newApprovalRef.setValue(approvalWithId).await()
            approvalId
        } catch (e: Exception) {
            throw Exception("Failed to create approval request: ${e.message}")
        }
    }

    /**
     * Approve an offer
     */
    suspend fun approveOffer(approvalId: String, adminId: String, comments: String = ""): Boolean {
        return try {
            val updates = mapOf(
                "status" to ApprovalStatus.APPROVED.name,
                "adminId" to adminId,
                "reviewedAt" to System.currentTimeMillis(),
                "comments" to comments
            )
            approvalsRef.child(approvalId).updateChildren(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reject an offer
     */
    suspend fun rejectOffer(
        approvalId: String,
        adminId: String,
        rejectionReason: String
    ): Boolean {
        return try {
            val updates = mapOf(
                "status" to ApprovalStatus.REJECTED.name,
                "adminId" to adminId,
                "reviewedAt" to System.currentTimeMillis(),
                "rejectionReason" to rejectionReason
            )
            approvalsRef.child(approvalId).updateChildren(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private var instance: OfferApprovalRepository? = null

        fun getInstance(): OfferApprovalRepository {
            if (instance == null) {
                instance = OfferApprovalRepository()
            }
            return instance!!
        }
    }
}
