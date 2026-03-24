package com.example.bookmymovie.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookmymovie.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponInputField(
    couponCode: String,
    onCouponCodeChange: (String) -> Unit,
    onApplyCoupon: () -> Unit,
    onRemoveCoupon: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    isApplied: Boolean = false,
    discountAmount: Double = 0.0
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isApplied) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = couponCode,
                    onValueChange = onCouponCodeChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter Coupon Code", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = DividerColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onApplyCoupon,
                    enabled = couponCode.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DeepCharcoal)
                    } else {
                        Text("Apply", color = TextPrimary)
                    }
                }
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "'$couponCode' applied",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "You saved ₹${discountAmount.toInt()}!",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onRemoveCoupon) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Coupon", tint = TextPrimary)
                    }
                }
            }
        }
    }
}
