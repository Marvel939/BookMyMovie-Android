package com.example.bookmymovie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.example.bookmymovie.location.GoogleGeocoding
import com.example.bookmymovie.location.LocationManager
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.TheatreOwnerViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
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
    var country by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var cinemaDropdownExpanded by remember { mutableStateOf(false) }
    var isCountryExpanded by remember { mutableStateOf(false) }
    var isStateExpanded by remember { mutableStateOf(false) }
    var countryCode by remember { mutableStateOf("+91") }
    var isCodeExpanded by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val vm: TheatreOwnerViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { 
        vm.loadCinemasFromFirebase()
        LocationManager.initialize(context)
        if (LocationManager.hasLocationPermission(context) && LocationManager.isGpsEnabled(context)) {
            coroutineScope.launch {
                isDetectingLocation = true
                val location = LocationManager.getCurrentLocation(context)
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    val locationData = GoogleGeocoding.getAddressFromCoordinates(
                        context,
                        latitude,
                        longitude
                    )
                    if (locationData != null) {
                        address = locationData.address
                        city = locationData.city
                        country = locationData.country
                        state = locationData.state
                    }
                }
                isDetectingLocation = false
            }
        }
    }

    fun validatePhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }
    }

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
        // Cinema / Theatre autocomplete text field
        ExposedDropdownMenuBox(
            expanded = cinemaDropdownExpanded && cinemaName.isNotBlank(),
            onExpandedChange = { /* controlled by text input focus */ }
        ) {
            val filteredCinemas = remember(cinemaName, vm.cinemasList) {
                if (cinemaName.isBlank()) emptyList()
                else vm.cinemasList.filter {
                    it.name.contains(cinemaName, ignoreCase = true)
                }
            }

            OutlinedTextField(
                value = cinemaName,
                onValueChange = { newValue ->
                    cinemaName = newValue
                    selectedPlaceId = "" // reset selection when user types
                    cinemaDropdownExpanded = newValue.isNotBlank()
                },
                label = { Text("Cinema / Theatre Name", color = TextSecondary, fontSize = 13.sp) },
                placeholder = { Text(
                    if (vm.isLoadingCinemas) "Loading cinemas..." else "Type your cinema name",
                    color = TextSecondary.copy(alpha = 0.5f)
                ) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(30.dp),
                singleLine = true,
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

            if (filteredCinemas.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = cinemaDropdownExpanded && cinemaName.isNotBlank(),
                    onDismissRequest = { cinemaDropdownExpanded = false },
                    modifier = Modifier.background(CardBackground)
                ) {
                    filteredCinemas.forEach { cinema ->
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
        
        // Phone Number with Country Code
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(0.3f)) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Code", color = TextSecondary) },
                    shape = RoundedCornerShape(30.dp),
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.clickable { isCodeExpanded = true }
                        )
                    },
                    modifier = Modifier.clickable { isCodeExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DividerColor,
                        unfocusedBorderColor = DividerColor,
                        disabledBorderColor = DividerColor,
                        disabledTextColor = TextPrimary,
                        disabledLabelColor = TextSecondary,
                        disabledContainerColor = SecondaryBackground
                    ),
                    enabled = false
                )
                DropdownMenu(
                    expanded = isCodeExpanded,
                    onDismissRequest = { isCodeExpanded = false },
                    modifier = Modifier.background(CardBackground)
                ) {
                    countryList.forEach { country ->
                        DropdownMenuItem(
                            text = { Text("${country.flag} ${country.name} (${country.code})", color = TextPrimary) },
                            onClick = {
                                countryCode = country.code
                                isCodeExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { newValue ->
                    if (newValue.length <= 10 && newValue.all { it.isDigit() }) {
                        phone = newValue
                        phoneError = if (newValue.isNotEmpty() && newValue.length < 10) "Phone number must be exactly 10 digits" else null
                    }
                },
                label = { Text("Phone Number", color = TextSecondary, fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
                shape = RoundedCornerShape(30.dp),
                singleLine = true,
                isError = phoneError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (phoneError != null) Color.Red else PrimaryAccent,
                    unfocusedBorderColor = DividerColor,
                    focusedContainerColor = SecondaryBackground,
                    unfocusedContainerColor = SecondaryBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = if (phoneError != null) Color.Red else PrimaryAccent,
                    unfocusedLabelColor = TextSecondary
                )
            )
        }
        if (phoneError != null) {
            Text(phoneError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
        Spacer(Modifier.height(14.dp))
        OwnerTextField(value = city, onValueChange = { city = it }, label = "City")
        Spacer(Modifier.height(14.dp))
        
        // Country dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SecondaryBackground, RoundedCornerShape(30.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(30.dp))
                .clickable { isCountryExpanded = !isCountryExpanded }
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (country.isEmpty()) "Country" else country,
                color = if (country.isEmpty()) TextSecondary else TextPrimary,
                fontSize = 16.sp
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp)
            )
            DropdownMenu(
                expanded = isCountryExpanded,
                onDismissRequest = { isCountryExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("India", "United States", "United Kingdom", "Canada", "Australia", "Germany", "France", "Japan", "China", "Brazil").forEach { selectedCountry ->
                    DropdownMenuItem(
                        text = { Text(selectedCountry, color = TextPrimary) },
                        onClick = {
                            country = selectedCountry
                            isCountryExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        
        // State dropdown
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SecondaryBackground, RoundedCornerShape(30.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(30.dp))
                .clickable { isStateExpanded = !isStateExpanded }
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (state.isEmpty()) "State / Province" else state,
                color = if (state.isEmpty()) TextSecondary else TextPrimary,
                fontSize = 16.sp
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp)
            )
            DropdownMenu(
                expanded = isStateExpanded,
                onDismissRequest = { isStateExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                val statesList = when (country) {
                    "India" -> listOf("Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Delhi", "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal")
                    "United States" -> listOf("Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming")
                    "United Kingdom" -> listOf("England", "Scotland", "Wales", "Northern Ireland")
                    "Canada" -> listOf("Alberta", "British Columbia", "Manitoba", "New Brunswick", "Newfoundland and Labrador", "Nova Scotia", "Ontario", "Prince Edward Island", "Quebec", "Saskatchewan")
                    "Australia" -> listOf("New South Wales", "Queensland", "South Australia", "Tasmania", "Victoria", "Western Australia", "Australian Capital Territory", "Northern Territory")
                    else -> listOf()
                }
                statesList.forEach { selectedState ->
                    DropdownMenuItem(
                        text = { Text(selectedState, color = TextPrimary) },
                        onClick = {
                            state = selectedState
                            isStateExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        
        // Address field with Detect Location button
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Address",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                if (!isDetectingLocation) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    isDetectingLocation = true
                                    Toast.makeText(context, "Detecting location...", Toast.LENGTH_SHORT).show()
                                    
                                    var location: android.location.Location? = null
                                    var retryCount = 0
                                    val maxRetries = 2
                                    
                                    // Retry loop - GPS can take 5-10 seconds to acquire fix
                                    while (location == null && retryCount < maxRetries) {
                                        location = LocationManager.getCurrentLocation(context)
                                        if (location == null && retryCount < maxRetries - 1) {
                                            delay(1000) // Wait 1 second between retries
                                        }
                                        retryCount++
                                    }
                                    
                                    if (location != null) {
                                        latitude = location.latitude
                                        longitude = location.longitude
                                        val locationData = GoogleGeocoding.getAddressFromCoordinates(
                                            context,
                                            latitude,
                                            longitude
                                        )
                                        if (locationData != null) {
                                            address = locationData.address
                                            city = locationData.city
                                            country = locationData.country
                                            state = locationData.state
                                            Toast.makeText(context, "Location detected", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Could not get address from location", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Please turn on your location", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error detecting location", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isDetectingLocation = false
                                }
                            }
                        },
                        modifier = Modifier
                            .height(32.dp)
                            .wrapContentWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryAccent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Detect", fontSize = 12.sp)
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PrimaryAccent,
                        strokeWidth = 2.dp
                    )
                }
            }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address", color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
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
        }
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
                        !validatePhoneNumber(phone) ->
                            Toast.makeText(context, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
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
                                            country = country.trim(),
                                            state = state.trim(),
                                            address = address.trim(),
                                            countryCode = countryCode,
                                            latitude = latitude,
                                            longitude = longitude,
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
                            country = country.trim(),
                            state = state.trim(),
                            address = address.trim(),
                            countryCode = countryCode,
                            latitude = latitude,
                            longitude = longitude,
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
