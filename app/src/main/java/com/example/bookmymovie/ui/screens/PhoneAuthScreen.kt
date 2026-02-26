package com.example.bookmymovie.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.firebase.User
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.components.AppLogo
import com.example.bookmymovie.ui.theme.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

/**
 * After phone-auth sign-in succeeds, look up the phone number in the "users" node.
 * • If a matching user record is found  → copy it under the new phone-auth UID so
 *   ProfileScreen (which reads /users/<uid>) displays the data correctly.
 * • If no match → sign out and inform the user that the number is not registered.
 */
private fun checkPhoneInDatabaseAndProceed(
    enteredPhone: String,
    auth: FirebaseAuth,
    navController: NavController,
    context: Context,
    onNotRegistered: () -> Unit
) {
    val newUid = auth.currentUser?.uid ?: return
    val usersRef = FirebaseDatabase.getInstance().getReference("users")
    val trimmed = enteredPhone.trim()

    usersRef.get().addOnSuccessListener { snapshot ->
        var foundUser: User? = null
        for (child in snapshot.children) {
            val user = child.getValue(User::class.java)
            if (user != null) {
                val fullPhone = user.countryCode + user.phone
                if (fullPhone == trimmed) {
                    foundUser = user
                    break
                }
            }
        }

        if (foundUser != null) {
            // Copy the existing user record under the phone-auth UID
            val updatedUser = foundUser.copy(userId = newUid)
            usersRef.child(newUid).setValue(updatedUser).addOnCompleteListener {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        } else {
            // Phone number not in database – sign out and notify
            auth.currentUser?.delete()   // remove the orphan phone-auth account
            auth.signOut()
            onNotRegistered()
        }
    }.addOnFailureListener { e ->
        auth.signOut()
        Toast.makeText(context, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
        onNotRegistered()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(navController: NavController) {
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var otpSent by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as Activity
    val auth = FirebaseAuth.getInstance()

    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification (e.g. on the same device) — sign in directly
                isVerifying = true
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            checkPhoneInDatabaseAndProceed(
                                enteredPhone = phoneNumber.trim(),
                                auth = auth,
                                navController = navController,
                                context = context,
                                onNotRegistered = {
                                    isVerifying = false
                                    otpSent = false
                                    otpCode = ""
                                    verificationId = null
                                    statusMessage = "This phone number is not registered. Please register first."
                                    Toast.makeText(
                                        context,
                                        "This phone number is not registered. Please register first.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        } else {
                            isVerifying = false
                            Toast.makeText(
                                context,
                                "Auto sign-in failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isSendingOtp = false
                Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }

            override fun onCodeSent(
                id: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isSendingOtp = false
                verificationId = id
                otpSent = true
                statusMessage = "OTP sent successfully!"
                Toast.makeText(context, "OTP sent to your phone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppLogo()
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Phone Login",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "We'll send an OTP to verify your number",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        // ── Phone Number Field ──────────────────────────────────────────────
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number (e.g. +91XXXXXXXXXX)", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = !otpSent,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = DividerColor,
                cursorColor = PrimaryAccent,
                focusedLabelColor = PrimaryAccent,
                unfocusedLabelColor = TextSecondary,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledBorderColor = DividerColor,
                disabledTextColor = TextSecondary,
                disabledContainerColor = CardBackground,
                disabledLabelColor = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Send OTP / OTP Input ────────────────────────────────────────────
        if (!otpSent) {
            if (isSendingOtp) {
                CircularProgressIndicator(color = PrimaryAccent)
            } else {
                Button(
                    onClick = {
                        val trimmed = phoneNumber.trim()
                        if (trimmed.isEmpty() || !trimmed.startsWith("+")) {
                            Toast.makeText(
                                context,
                                "Enter a valid phone number with country code (e.g. +91...)",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        isSendingOtp = true
                        val options = PhoneAuthOptions.newBuilder(auth)
                            .setPhoneNumber(trimmed)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(activity)
                            .setCallbacks(callbacks)
                            .build()
                        PhoneAuthProvider.verifyPhoneNumber(options)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        "Send OTP",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } else {
            // OTP input
            OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                label = { Text("Enter 6-digit OTP", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = DividerColor,
                    cursorColor = PrimaryAccent,
                    focusedLabelColor = PrimaryAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isVerifying) {
                CircularProgressIndicator(color = PrimaryAccent)
            } else {
                Button(
                    onClick = {
                        if (otpCode.length != 6) {
                            Toast.makeText(context, "Enter a valid 6-digit OTP", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }
                        val vid = verificationId
                        if (vid == null) {
                            Toast.makeText(context, "Please request OTP first", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }
                        isVerifying = true
                        val credential = PhoneAuthProvider.getCredential(vid, otpCode)
                        auth.signInWithCredential(credential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    checkPhoneInDatabaseAndProceed(
                                        enteredPhone = phoneNumber.trim(),
                                        auth = auth,
                                        navController = navController,
                                        context = context,
                                        onNotRegistered = {
                                            isVerifying = false
                                            otpSent = false
                                            otpCode = ""
                                            verificationId = null
                                            statusMessage = "This phone number is not registered. Please register first."
                                            Toast.makeText(
                                                context,
                                                "This phone number is not registered. Please register first.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                } else {
                                    isVerifying = false
                                    Toast.makeText(
                                        context,
                                        "Verification failed: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        "Verify & Login",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend option
            Text(
                text = "Resend OTP",
                color = PrimaryAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    otpSent = false
                    otpCode = ""
                    verificationId = null
                    statusMessage = ""
                }
            )
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = statusMessage,
                color = if (statusMessage.contains("not registered", ignoreCase = true))
                    Color(0xFFFF6B6B) else PrimaryAccent,
                fontSize = 13.sp,
                fontWeight = if (statusMessage.contains("not registered", ignoreCase = true))
                    FontWeight.SemiBold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Text("Back to ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Email Login",
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    navController.popBackStack()
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
