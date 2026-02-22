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
import androidx.compose.material.icons.filled.Store
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.R
import com.example.bookmymovie.auth.GoogleAuthHelper
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun TheatreOwnerAuthScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground)
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Theatre Owner Portal", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("BookMyMovie", color = PrimaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBackground,
            contentColor = PrimaryAccent,
            divider = { HorizontalDivider(color = DividerColor) }
        ) {
            Tab(
                selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("Login", color = if (selectedTab == 0) PrimaryAccent else TextSecondary, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("Register", color = if (selectedTab == 1) PrimaryAccent else TextSecondary, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        when (selectedTab) {
            0 -> OwnerLoginTab(navController)
            1 -> OwnerRegisterTab(navController)
        }
    }
}

// ─── Login Tab ────────────────────────────────────────────────────────────────

@Composable
private fun OwnerLoginTab(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val vm: TheatreOwnerViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    if (showForgotDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotDialog = false },
            onSend = { resetEmail ->
                auth.sendPasswordResetEmail(resetEmail)
                    .addOnCompleteListener { task ->
                        Toast.makeText(
                            context,
                            if (task.isSuccessful) "Reset email sent." else "Failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
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
        Text("Welcome Back, Owner", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Sign in to manage your cinema", color = TextSecondary, fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))

        OwnerTextField(value = email, onValueChange = { email = it }, label = "Email")
        Spacer(Modifier.height(14.dp))
        OwnerTextField(
            value = password, onValueChange = { password = it }, label = "Password",
            isPassword = true, passwordVisible = passwordVisible,
            onTogglePassword = { passwordVisible = !passwordVisible }
        )

        Text(
            "Forgot Password?", color = PrimaryAccent, fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.End).padding(top = 10.dp).clickable { showForgotDialog = true }
        )

        Spacer(Modifier.height(28.dp))

        if (isLoading) {
            CircularProgressIndicator(color = PrimaryAccent)
        } else {
            Button(
                onClick = {
                    val trimEmail = email.trim()
                    val trimPass = password.trim()
                    if (trimEmail.isEmpty() || trimPass.isEmpty()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    auth.signInWithEmailAndPassword(trimEmail, trimPass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid ?: run { isLoading = false; return@addOnCompleteListener }
                                vm.checkOwnerStatus(uid) { status ->
                                    isLoading = false
                                    when (status) {
                                        "approved" -> navController.navigate(Screen.TheatreOwnerPanel.route) {
                                            popUpTo(Screen.TheatreOwnerAuth.route) { inclusive = true }
                                        }
                                        "pending" -> {
                                            auth.signOut()
                                            Toast.makeText(context,
                                                "Your registration is pending admin approval. Please wait.",
                                                Toast.LENGTH_LONG).show()
                                        }
                                        "rejected" -> {
                                            auth.signOut()
                                            Toast.makeText(context,
                                                "Your registration was rejected. Contact support.",
                                                Toast.LENGTH_LONG).show()
                                        }
                                        "not_found" -> {
                                            auth.signOut()
                                            Toast.makeText(context,
                                                "No theatre owner account found. Please register.",
                                                Toast.LENGTH_LONG).show()
                                        }
                                        else -> {
                                            auth.signOut()
                                            Toast.makeText(context, "Error checking status.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
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
                Text("Login as Theatre Owner", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))
        OwnerOrDivider()
        Spacer(Modifier.height(24.dp))

        OwnerGoogleButton(isLoading = isGoogleLoading, label = "Continue with Google", onClick = {
            isGoogleLoading = true
            coroutineScope.launch {
                GoogleAuthHelper.signInWithGoogle(
                    context = context,
                    onSuccess = {
                        val uid = auth.currentUser?.uid ?: run { isGoogleLoading = false; return@signInWithGoogle }
                        vm.checkOwnerStatus(uid) { status ->
                            isGoogleLoading = false
                            when (status) {
                                "approved" -> navController.navigate(Screen.TheatreOwnerPanel.route) {
                                    popUpTo(Screen.TheatreOwnerAuth.route) { inclusive = true }
                                }
                                "pending" -> {
                                    auth.signOut()
                                    Toast.makeText(context, "Registration pending admin approval.", Toast.LENGTH_LONG).show()
                                }
                                "rejected" -> {
                                    auth.signOut()
                                    Toast.makeText(context, "Registration was rejected.", Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    auth.signOut()
                                    Toast.makeText(context, "No theatre owner account found.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    onError = { error ->
                        isGoogleLoading = false
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        })
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Register Tab ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerRegisterTab(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var cinemaName by remember { mutableStateOf("") }
    var selectedPlaceId by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var cinemaDropdownExpanded by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val vm: TheatreOwnerViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.loadCinemasFromFirebase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepCharcoal)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Register as Theatre Owner", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Your request will be reviewed by Admin", color = TextSecondary, fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))

        OwnerTextField(value = name, onValueChange = { name = it }, label = "Full Name")
        Spacer(Modifier.height(14.dp))
        OwnerTextField(value = email, onValueChange = { email = it }, label = "Email")
        Spacer(Modifier.height(14.dp))
        // Cinema / Theatre dropdown from Firebase
        ExposedDropdownMenuBox(
            expanded = cinemaDropdownExpanded,
            onExpandedChange = { cinemaDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = cinemaName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Cinema / Theatre Name", color = TextSecondary, fontSize = 13.sp) },
                placeholder = { Text(
                    if (vm.isLoadingCinemas) "Loading cinemas..." else "Select your cinema",
                    color = TextSecondary.copy(alpha = 0.5f)
                ) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cinemaDropdownExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = DividerColor,
                    focusedContainerColor = SecondaryBackground,
                    unfocusedContainerColor = SecondaryBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryAccent,
                    unfocusedLabelColor = TextSecondary
                )
            )
            ExposedDropdownMenu(
                expanded = cinemaDropdownExpanded,
                onDismissRequest = { cinemaDropdownExpanded = false },
                modifier = Modifier.background(CardBackground)
            ) {
                when {
                    vm.isLoadingCinemas -> DropdownMenuItem(
                        text = { Text("Loading cinemas...", color = TextSecondary) },
                        onClick = {}
                    )
                    vm.cinemasList.isEmpty() -> DropdownMenuItem(
                        text = { Text("No cinemas found in database.", color = TextSecondary) },
                        onClick = {}
                    )
                    else -> vm.cinemasList.forEach { cinema ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(cinema.name, color = TextPrimary, fontSize = 14.sp)
                                    if (cinema.address.isNotBlank()) {
                                        Text(cinema.address, color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            },
                            onClick = {
                                cinemaName = cinema.name
                                selectedPlaceId = cinema.placeId
                                cinemaDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        OwnerTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number")
        Spacer(Modifier.height(14.dp))
        OwnerTextField(value = city, onValueChange = { city = it }, label = "City")
        Spacer(Modifier.height(14.dp))
        OwnerTextField(
            value = password, onValueChange = { password = it }, label = "Password",
            isPassword = true, passwordVisible = passwordVisible,
            onTogglePassword = { passwordVisible = !passwordVisible }
        )
        Spacer(Modifier.height(14.dp))
        OwnerTextField(
            value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password",
            isPassword = true, passwordVisible = confirmPasswordVisible,
            onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible }
        )

        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(10.dp), color = PrimaryAccent.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
            Text(
                "📋  After registering, your request will be sent to the Admin for approval. " +
                        "You will be able to login only after your account is approved.",
                color = PrimaryAccent, fontSize = 12.sp,
                modifier = Modifier.padding(12.dp), textAlign = TextAlign.Start
            )
        }

        Spacer(Modifier.height(28.dp))

        if (isLoading) {
            CircularProgressIndicator(color = PrimaryAccent)
        } else {
            Button(
                onClick = {
                    when {
                        name.isBlank() || email.isBlank() || cinemaName.isBlank() || phone.isBlank() || city.isBlank() || password.isBlank() ->
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        password != confirmPassword ->
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        password.length < 6 ->
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        else -> {
                            isLoading = true
                            auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val uid = auth.currentUser?.uid ?: run { isLoading = false; return@addOnCompleteListener }
                                        vm.registerTheatreOwner(
                                            uid = uid,
                                            name = name.trim(),
                                            email = email.trim(),
                                            cinemaName = cinemaName.trim(),
                                            placeId = selectedPlaceId,
                                            phone = phone.trim(),
                                            city = city.trim(),
                                            onSuccess = {
                                                isLoading = false
                                                auth.signOut()
                                                Toast.makeText(context,
                                                    "Registration submitted! Waiting for admin approval.",
                                                    Toast.LENGTH_LONG).show()
                                                navController.popBackStack()
                                            },
                                            onError = { err ->
                                                isLoading = false
                                                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
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
                Text("Submit Registration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))
        OwnerOrDivider()
        Spacer(Modifier.height(24.dp))

        OwnerGoogleButton(isLoading = isGoogleLoading, label = "Register with Google", onClick = {
            isGoogleLoading = true
            coroutineScope.launch {
                GoogleAuthHelper.signInWithGoogle(
                    context = context,
                    onSuccess = {
                        val uid = auth.currentUser?.uid ?: run { isGoogleLoading = false; return@signInWithGoogle }
                        val gName = auth.currentUser?.displayName ?: ""
                        val gEmail = auth.currentUser?.email ?: ""
                        vm.registerTheatreOwner(
                            uid = uid, name = gName, email = gEmail,
                            cinemaName = cinemaName.trim().ifBlank { "My Cinema" },
                            placeId = selectedPlaceId, phone = phone.trim(),
                            city = city.trim(),
                            onSuccess = {
                                isGoogleLoading = false
                                auth.signOut()
                                Toast.makeText(context, "Registration submitted! Waiting for admin approval.", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            },
                            onError = { err ->
                                isGoogleLoading = false
                                Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onError = { error ->
                        isGoogleLoading = false
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        })
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Reusable components ──────────────────────────────────────────────────────

@Composable
internal fun OwnerTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    isPassword: Boolean = false, passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null, tint = TextSecondary
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryAccent, unfocusedBorderColor = DividerColor,
            cursorColor = PrimaryAccent, focusedLabelColor = PrimaryAccent,
            unfocusedLabelColor = TextSecondary, focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground, focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        )
    )
}

@Composable
internal fun OwnerOrDivider() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
        Text("  OR  ", color = TextSecondary, fontSize = 13.sp)
        HorizontalDivider(modifier = Modifier.weight(1f), color = DividerColor)
    }
}

@Composable
internal fun OwnerGoogleButton(isLoading: Boolean, label: String, onClick: () -> Unit) {
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
            Image(painter = painterResource(id = R.drawable.google), contentDescription = "Google", modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}
