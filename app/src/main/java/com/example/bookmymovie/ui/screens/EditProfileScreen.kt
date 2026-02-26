package com.example.bookmymovie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.example.bookmymovie.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val database = FirebaseDatabase.getInstance().getReference("users").child(userId)
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Editable fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Non-editable (preserved on save)
    var email by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var createdAt by remember { mutableStateOf(0L) }

    // Gender dropdown state
    var genderExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

    // Load current user data
    LaunchedEffect(userId) {
        database.get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            if (user != null) {
                firstName = user.firstName
                lastName = user.lastName
                phone = user.phone
                countryCode = user.countryCode
                gender = user.gender
                dob = user.dob
                city = user.city
                address = user.address
                email = user.email
                profileImageUrl = user.profileImageUrl
                createdAt = user.createdAt
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
            Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Profile", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Email (read-only)
                EditProfileField(
                    label = "Email",
                    value = email,
                    onValueChange = {},
                    enabled = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileField(
                    label = "First Name",
                    value = firstName,
                    onValueChange = { firstName = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileField(
                    label = "Last Name",
                    value = lastName,
                    onValueChange = { lastName = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Country Code + Phone in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditProfileField(
                        label = "Code",
                        value = countryCode,
                        onValueChange = { countryCode = it },
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.weight(0.3f)
                    )
                    EditProfileField(
                        label = "Phone Number",
                        value = phone,
                        onValueChange = { phone = it },
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gender dropdown
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender", color = TextSecondary, fontSize = 13.sp) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = PrimaryAccent,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            disabledContainerColor = CardBackground,
                            focusedLabelColor = PrimaryAccent,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                        containerColor = CardBackground
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = TextPrimary) },
                                onClick = {
                                    gender = option
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileField(
                    label = "Date of Birth",
                    value = dob,
                    onValueChange = { dob = it },
                    placeholder = "DD/MM/YYYY"
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileField(
                    label = "City",
                    value = city,
                    onValueChange = { city = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                EditProfileField(
                    label = "Address",
                    value = address,
                    onValueChange = { address = it },
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Save button
                Button(
                    onClick = {
                        // Validation
                        if (firstName.isBlank()) {
                            Toast.makeText(context, "First name is required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (lastName.isBlank()) {
                            Toast.makeText(context, "Last name is required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSaving = true
                        val updatedUser = User(
                            userId = userId,
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            email = email,
                            gender = gender.trim(),
                            address = address.trim(),
                            countryCode = countryCode.trim(),
                            phone = phone.trim(),
                            dob = dob.trim(),
                            city = city.trim(),
                            profileImageUrl = profileImageUrl,
                            createdAt = createdAt
                        )

                        database.setValue(updatedUser)
                            .addOnSuccessListener {
                                isSaving = false
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                isSaving = false
                                Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    placeholder: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 13.sp) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f), fontSize = 14.sp) }
        } else null,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextSecondary,
            focusedBorderColor = PrimaryAccent,
            unfocusedBorderColor = DividerColor,
            disabledBorderColor = DividerColor.copy(alpha = 0.5f),
            cursorColor = PrimaryAccent,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = CardBackground,
            disabledContainerColor = CardBackground.copy(alpha = 0.7f),
            focusedLabelColor = PrimaryAccent,
            unfocusedLabelColor = TextSecondary,
            disabledLabelColor = TextSecondary.copy(alpha = 0.6f)
        )
    )
}
