package com.example.bookmymovie.model

import com.google.firebase.database.PropertyName

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class OfferApproval(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("offerId")
    val offerId: String = "",
    
    @PropertyName("offer")
    val offer: Offer? = null,
    
    @PropertyName("theatreOwnerId")
    val theatreOwnerId: String = "",
    
    @PropertyName("theatreOwnerName")
    val theatreOwnerName: String = "",
    
    @PropertyName("theatreName")
    val theatreName: String = "",
    
    @PropertyName("createdAt")
    val createdAt: Long = 0L,
    
    @PropertyName("reviewedAt")
    val reviewedAt: Long = 0L,
    
    @PropertyName("status")
    val status: String = ApprovalStatus.PENDING.name,
    
    @PropertyName("adminId")
    val adminId: String = "",
    
    @PropertyName("rejectionReason")
    val rejectionReason: String = "",
    
    @PropertyName("comments")
    val comments: String = ""
) {
    fun getStatusEnum(): ApprovalStatus = ApprovalStatus.valueOf(status)
}
