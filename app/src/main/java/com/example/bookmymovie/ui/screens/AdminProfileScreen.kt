package com.example.bookmymovie.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    val db = FirebaseDatabase.getInstance()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var createdAt by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Editable fields
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    LaunchedEffect(uid) {
        if (uid.isBlank()) return@LaunchedEffect
        db.getReference("admin_users").child(uid).get()
            .addOnSuccessListener { snap ->
                name = snap.child("name").value as? String ?: ""
                email = snap.child("email").value as? String ?: ""
                phone = snap.child("phone").value as? String ?: ""
                role = snap.child("role").value as? String ?: "admin"
                createdAt = snap.child("createdAt").value as? Long ?: 0L
                editName = name
                editPhone = phone
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        auth.signOut()
                        showLogoutDialog = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Logout", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Profile", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    if (!isLoading && !isEditing) {
                        IconButton(onClick = {
                            editName = name
                            editPhone = phone
                            isEditing = true
                        }) {
                            Icon(Icons.Default.Edit, "Edit", tint = PrimaryAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings, null,
                        tint = PrimaryAccent, modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(name.ifBlank { "Admin" }, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        role.replaceFirstChar { it.uppercase() },
                        color = PrimaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isEditing) {
                // Edit mode
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Edit Profile", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))

                    AdminProfileTextField(value = editName, onValueChange = { editName = it }, label = "Full Name", icon = Icons.Default.Person)
                    Spacer(Modifier.height(14.dp))
                    AdminProfileTextField(value = email, onValueChange = {}, label = "Email", icon = Icons.Default.Email, enabled = false)
                    Spacer(Modifier.height(14.dp))
                    AdminProfileTextField(value = editPhone, onValueChange = { editPhone = it }, label = "Phone Number", icon = Icons.Default.Phone)

                    Spacer(Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DividerColor)
                            )
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                if (editName.isBlank()) {
                                    Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSaving = true
                                val updates = mapOf<String, Any>(
                                    "name" to editName.trim(),
                                    "phone" to editPhone.trim()
                                )
                                db.getReference("admin_users").child(uid).updateChildren(updates)
                                    .addOnSuccessListener {
                                        name = editName.trim()
                                        phone = editPhone.trim()
                                        isSaving = false
                                        isEditing = false
                                        Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        isSaving = false
                                        Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // View mode
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Account Details", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    AdminProfileInfoCard(icon = Icons.Default.Person, label = "Name", value = name.ifBlank { "Not set" })
                    AdminProfileInfoCard(icon = Icons.Default.Email, label = "Email", value = email.ifBlank { "Not set" })
                    AdminProfileInfoCard(icon = Icons.Default.Phone, label = "Phone", value = phone.ifBlank { "Not set" })
                    AdminProfileInfoCard(icon = Icons.Default.Badge, label = "Role", value = role.replaceFirstChar { it.uppercase() })
                    AdminProfileInfoCard(
                        icon = Icons.Default.CalendarToday,
                        label = "Account Created",
                        value = if (createdAt > 0L) {
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(createdAt))
                        } else "Unknown"
                    )
                    AdminProfileInfoCard(icon = Icons.Default.Fingerprint, label = "UID", value = uid)
                }

                Spacer(Modifier.height(32.dp))

                // Logout button
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = ErrorRose, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", color = ErrorRose, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AdminProfileInfoCard(icon: ImageVector, label: String, value: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = PrimaryAccent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AdminProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        leadingIcon = { Icon(icon, null, tint = if (enabled) PrimaryAccent else TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryAccent,
            unfocusedBorderColor = DividerColor,
            disabledBorderColor = DividerColor.copy(alpha = 0.5f),
            cursorColor = PrimaryAccent,
            focusedLabelColor = PrimaryAccent,
            unfocusedLabelColor = TextSecondary,
            disabledLabelColor = TextSecondary.copy(alpha = 0.5f),
            focusedContainerColor = SecondaryBackground,
            unfocusedContainerColor = SecondaryBackground,
            disabledContainerColor = SecondaryBackground.copy(alpha = 0.5f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            disabledTextColor = TextSecondary
        )
    )
}
