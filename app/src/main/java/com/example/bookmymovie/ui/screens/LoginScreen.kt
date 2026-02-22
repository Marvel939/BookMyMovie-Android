package com.example.bookmymovie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.R
import com.example.bookmymovie.auth.GoogleAuthHelper
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.components.AppLogo
import com.example.bookmymovie.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPasswordDialog = false },
            onSend = { resetEmail ->
                auth.sendPasswordResetEmail(resetEmail)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to send email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                showForgotPasswordDialog = false
            }
        )
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
        AppLogo()
        Spacer(modifier = Modifier.height(48.dp))

        // Welcome text
        Text(
            text = "Welcome Back",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Sign in to continue",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
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
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = TextSecondary
                    )
                }
            },
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

        Text(
            text = "Forgot Password?",
            color = PrimaryAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 10.dp)
                .clickable { showForgotPasswordDialog = true }
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = PrimaryAccent)
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Login", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
            Text(
                text = "  OR  ",
                color = TextSecondary,
                fontSize = 13.sp
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
        }

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedButton(
            onClick = {
                isGoogleLoading = true
                coroutineScope.launch {
                    GoogleAuthHelper.signInWithGoogle(
                        context = context,
                        onSuccess = {
                            isGoogleLoading = false
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onError = { error ->
                            isGoogleLoading = false
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, DividerColor),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBackground),
            enabled = !isGoogleLoading
        ) {
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PrimaryAccent,
                    strokeWidth = 2.dp
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Google",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Continue with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row {
            Text("Don't have an account? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Sign Up",
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text("Are you an Admin? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Admin Login",
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.AdminAuth.route)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text("Theatre Owner? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Owner Login",
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.TheatreOwnerAuth.route)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset Password", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter your email address to receive a password reset link.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = TextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = PrimaryAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SecondaryBackground,
                        unfocusedContainerColor = SecondaryBackground
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(email) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Send", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardBackground,
        shape = RoundedCornerShape(20.dp)
    )
}
