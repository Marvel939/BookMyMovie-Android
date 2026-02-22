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
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.R
import com.example.bookmymovie.auth.GoogleAuthHelper
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

@Composable
fun AdminAuthScreen(navController: NavController) {

    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
    ) {
        // ── Admin Header ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Admin Portal",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                "BookMyMovie",
                color = PrimaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Tabs ─────────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBackground,
            contentColor = PrimaryAccent,
            divider = { HorizontalDivider(color = DividerColor) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Login",
                        color = if (selectedTab == 0) PrimaryAccent else TextSecondary,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Register",
                        color = if (selectedTab == 1) PrimaryAccent else TextSecondary,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        // ── Tab Content ───────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> AdminLoginTab(navController)
            1 -> AdminRegisterTab(navController)
        }
    }
}

// ─── Admin Login Tab ─────────────────────────────────────────────────────────

@Composable
private fun AdminLoginTab(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseDatabase.getInstance()
    val coroutineScope = rememberCoroutineScope()

    if (showForgotDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotDialog = false },
            onSend = { resetEmail ->
                auth.sendPasswordResetEmail(resetEmail)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                showForgotDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Welcome Back, Admin",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            "Sign in to your admin account",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        AdminTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email"
        )
        Spacer(Modifier.height(14.dp))

        AdminTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePassword = { passwordVisible = !passwordVisible }
        )

        Text(
            text = "Forgot Password?",
            color = PrimaryAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 10.dp)
                .clickable { showForgotDialog = true }
        )

        Spacer(Modifier.height(28.dp))

        if (isLoading) {
            CircularProgressIndicator(color = PrimaryAccent)
        } else {
            Button(
                onClick = {
                    val trimEmail = email.trim()
                    val trimPassword = password.trim()
                    if (trimEmail.isEmpty() || trimPassword.isEmpty()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    auth.signInWithEmailAndPassword(trimEmail, trimPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid ?: run {
                                    isLoading = false
                                    Toast.makeText(context, "Authentication error", Toast.LENGTH_SHORT).show()
                                    return@addOnCompleteListener
                                }
                                // Verify this user is actually an admin
                                db.getReference("admin_users").child(uid)
                                    .get()
                                    .addOnSuccessListener { snap ->
                                        isLoading = false
                                        if (snap.exists()) {
                                            navController.navigate(Screen.AdminPanel.route) {
                                                popUpTo(Screen.AdminAuth.route) { inclusive = true }
                                            }
                                        } else {
                                            auth.signOut()
                                            Toast.makeText(
                                                context,
                                                "Access denied. This account is not registered as an admin.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Login as Admin", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))
        AdminOrDivider()
        Spacer(Modifier.height(24.dp))

        // Google Sign In
        AdminGoogleButton(
            isLoading = isGoogleLoading,
            onClick = {
                isGoogleLoading = true
                coroutineScope.launch {
                    GoogleAuthHelper.signInWithGoogle(
                        context = context,
                        onSuccess = {
                            val uid = auth.currentUser?.uid ?: run {
                                isGoogleLoading = false
                                return@signInWithGoogle
                            }
                            db.getReference("admin_users").child(uid)
                                .get()
                                .addOnSuccessListener { snap ->
                                    isGoogleLoading = false
                                    if (snap.exists()) {
                                        navController.navigate(Screen.AdminPanel.route) {
                                            popUpTo(Screen.AdminAuth.route) { inclusive = true }
                                        }
                                    } else {
                                        auth.signOut()
                                        Toast.makeText(
                                            context,
                                            "Access denied. Not registered as admin.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                .addOnFailureListener {
                                    isGoogleLoading = false
                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onError = { error ->
                            isGoogleLoading = false
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )
    }
}

// ─── Admin Register Tab ───────────────────────────────────────────────────────

@Composable
private fun AdminRegisterTab(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseDatabase.getInstance()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create Admin Account",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            "Register to manage BookMyMovie",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        AdminTextField(value = name, onValueChange = { name = it }, label = "Full Name")
        Spacer(Modifier.height(14.dp))
        AdminTextField(value = email, onValueChange = { email = it }, label = "Email")
        Spacer(Modifier.height(14.dp))
        AdminTextField(
            value = password, onValueChange = { password = it }, label = "Password",
            isPassword = true, passwordVisible = passwordVisible,
            onTogglePassword = { passwordVisible = !passwordVisible }
        )
        Spacer(Modifier.height(14.dp))
        AdminTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            isPassword = true, passwordVisible = confirmPasswordVisible,
            onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible }
        )

        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = PrimaryAccent.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "⚠  Admin accounts require approval. You will be able to sign in after the account is set up.",
                color = PrimaryAccent,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Start
            )
        }

        Spacer(Modifier.height(28.dp))

        if (isLoading) {
            CircularProgressIndicator(color = PrimaryAccent)
        } else {
            Button(
                onClick = {
                    val trimName = name.trim()
                    val trimEmail = email.trim()
                    val trimPassword = password.trim()
                    when {
                        trimName.isEmpty() || trimEmail.isEmpty() || trimPassword.isEmpty() ->
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        trimPassword != confirmPassword ->
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        trimPassword.length < 6 ->
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        else -> {
                            isLoading = true
                            auth.createUserWithEmailAndPassword(trimEmail, trimPassword)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val uid = auth.currentUser?.uid ?: run {
                                            isLoading = false
                                            return@addOnCompleteListener
                                        }
                                        saveAdminToFirebase(db, uid, trimName, trimEmail) {
                                            isLoading = false
                                            Toast.makeText(
                                                context,
                                                "Admin account created! You can now log in.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            navController.navigate(Screen.AdminPanel.route) {
                                                popUpTo(Screen.AdminAuth.route) { inclusive = true }
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Register as Admin", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))
        AdminOrDivider()
        Spacer(Modifier.height(24.dp))

        // Google Register
        AdminGoogleButton(
            isLoading = isGoogleLoading,
            label = "Register with Google",
            onClick = {
                isGoogleLoading = true
                coroutineScope.launch {
                    GoogleAuthHelper.signInWithGoogle(
                        context = context,
                        onSuccess = {
                            val uid = auth.currentUser?.uid ?: run {
                                isGoogleLoading = false
                                return@signInWithGoogle
                            }
                            val googleName = auth.currentUser?.displayName ?: ""
                            val googleEmail = auth.currentUser?.email ?: ""
                            saveAdminToFirebase(db, uid, googleName, googleEmail) {
                                isGoogleLoading = false
                                Toast.makeText(
                                    context,
                                    "Admin account created with Google!",
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.navigate(Screen.AdminPanel.route) {
                                    popUpTo(Screen.AdminAuth.route) { inclusive = true }
                                }
                            }
                        },
                        onError = { error ->
                            isGoogleLoading = false
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun saveAdminToFirebase(
    db: com.google.firebase.database.FirebaseDatabase,
    uid: String,
    name: String,
    email: String,
    onDone: () -> Unit
) {
    db.getReference("admin_users").child(uid).setValue(
        mapOf(
            "name" to name,
            "email" to email,
            "role" to "admin",
            "createdAt" to System.currentTimeMillis()
        )
    ).addOnCompleteListener { onDone() }
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }
        } else null,
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
}

@Composable
private fun AdminOrDivider() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
        Text("  OR  ", color = TextSecondary, fontSize = 13.sp)
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
    }
}

@Composable
private fun AdminGoogleButton(
    isLoading: Boolean,
    label: String = "Continue with Google",
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, DividerColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBackground),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryAccent, strokeWidth = 2.dp)
        } else {
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Google",
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}
