package com.example.bookmymovie.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmymovie.data.repository.OfferApprovalRepository
import com.example.bookmymovie.data.repository.OffersRepository
import com.example.bookmymovie.model.OfferApproval
import com.example.bookmymovie.model.OfferStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OfferAdminViewModel : ViewModel() {
    
    private val approvalRepository = OfferApprovalRepository.getInstance()
    private val offersRepository = OffersRepository.getInstance()

    // State for pending approvals
    private val _pendingApprovals = MutableStateFlow<List<OfferApproval>>(emptyList())
    val pendingApprovals: StateFlow<List<OfferApproval>> = _pendingApprovals.asStateFlow()

    // State for approval history
    private val _approvalHistory = MutableStateFlow<List<OfferApproval>>(emptyList())
    val approvalHistory: StateFlow<List<OfferApproval>> = _approvalHistory.asStateFlow()

    // State for selected approval for detail view
    private val _selectedApproval = MutableStateFlow<OfferApproval?>(null)
    val selectedApproval: StateFlow<OfferApproval?> = _selectedApproval.asStateFlow()

    // State for loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State for error messages
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    // State for success messages
    private val _successMessage = MutableStateFlow("")
    val successMessage: StateFlow<String> = _successMessage.asStateFlow()

    // State for processing an approval action
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /**
     * Load pending approvals for admin review
     */
    fun loadPendingApprovals() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                approvalRepository.getPendingApprovals().collect { approvals ->
                    _pendingApprovals.value = approvals
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load pending approvals: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load approval history (approved/rejected offers)
     */
    fun loadApprovalHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                approvalRepository.getApprovalHistory().collect { approvals ->
                    _approvalHistory.value = approvals
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load approval history: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Select an approval to view details
     */
    fun selectApproval(approval: OfferApproval) {
        _selectedApproval.value = approval
    }

    /**
     * Clear selected approval
     */
    fun clearSelectedApproval() {
        _selectedApproval.value = null
    }

    /**
     * Approve an offer
     */
    fun approveOffer(
        approvalId: String,
        offerId: String,
        adminId: String,
        comments: String = ""
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = ""
            _successMessage.value = ""
            try {
                // Update offer status to APPROVED in offers table
                offersRepository.updateOfferStatus(offerId, OfferStatus.APPROVED)

                // Update approval status
                approvalRepository.approveOffer(approvalId, adminId, comments)

                _successMessage.value = "Offer approved successfully!"

                // Refresh pending approvals
                loadPendingApprovals()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to approve offer: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Reject an offer
     */
    fun rejectOffer(
        approvalId: String,
        offerId: String,
        adminId: String,
        rejectionReason: String
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = ""
            _successMessage.value = ""
            try {
                // Update offer status to REJECTED in offers table
                offersRepository.updateOfferStatus(offerId, OfferStatus.REJECTED)

                // Update approval status with reason
                approvalRepository.rejectOffer(approvalId, adminId, rejectionReason)

                _successMessage.value = "Offer rejected successfully!"

                // Refresh pending approvals
                loadPendingApprovals()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reject offer: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Get statistics about offers
     */
    fun getOfferStatistics(): Map<String, Any> {
        return mapOf(
            "pendingCount" to _pendingApprovals.value.size,
            "totalApproved" to _approvalHistory.value.count { it.status == "APPROVED" },
            "totalRejected" to _approvalHistory.value.count { it.status == "REJECTED" }
        )
    }

    /**
     * Clear all messages
     */
    fun clearMessages() {
        _errorMessage.value = ""
        _successMessage.value = ""
    }
}
