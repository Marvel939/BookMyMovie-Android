package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.firebase.User
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.AdminUserManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(navController: NavController) {
    val viewModel: AdminUserManagementViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadAllUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "User Management",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadAllUsers() },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = PrimaryAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        containerColor = DeepCharcoal
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepCharcoal,
                contentColor = PrimaryAccent,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Users", color = if (selectedTab == 0) PrimaryAccent else TextSecondary, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Blocked Users", color = if (selectedTab == 1) PrimaryAccent else TextSecondary, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Soft Deleted", color = if (selectedTab == 2) PrimaryAccent else TextSecondary, fontSize = 12.sp) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (viewModel.isLoadingUsers) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            val activeUsers = viewModel.allUsers.filter { !it.isDeleted && it.status == "active" }
                            if (activeUsers.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No active users found", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = activeUsers,
                                        key = { it.userId }
                                    ) { user ->
                                        UserManagementCard(
                                            user = user,
                                            onEdit = { viewModel.openEditDialog(user) },
                                            onBlock = { viewModel.toggleUserBlockStatus(user) },
                                            onSoftDelete = { viewModel.softDeleteUser(user.userId) },
                                            onHardDelete = { viewModel.hardDeleteUser(user.userId) }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            val blockedUsers = viewModel.allUsers.filter { !it.isDeleted && it.status == "blocked" }
                            if (blockedUsers.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No blocked users found", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = blockedUsers,
                                        key = { it.userId }
                                    ) { user ->
                                        UserManagementCard(
                                            user = user,
                                            onEdit = { viewModel.openEditDialog(user) },
                                            onBlock = { viewModel.toggleUserBlockStatus(user) },
                                            onSoftDelete = { viewModel.softDeleteUser(user.userId) },
                                            onHardDelete = { viewModel.hardDeleteUser(user.userId) }
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            val deletedUsers = viewModel.allUsers.filter { it.isDeleted }
                            if (deletedUsers.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No soft deleted users found", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = deletedUsers,
                                        key = { it.userId }
                                    ) { user ->
                                        SoftDeletedUserCard(
                                            user = user,
                                            onRestore = { viewModel.restoreSoftDeletedUser(user.userId) },
                                            onHardDelete = { viewModel.hardDeleteUser(user.userId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Status messages
                if (viewModel.actionMessage != null) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White
                    ) {
                        Text(viewModel.actionMessage ?: "")
                    }
                    LaunchedEffect(viewModel.actionMessage) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearMessages()
                    }
                }

                if (viewModel.actionError != null) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        containerColor = Color(0xFFf44336),
                        contentColor = Color.White
                    ) {
                        Text(viewModel.actionError ?: "")
                    }
                    LaunchedEffect(viewModel.actionError) {
                        kotlinx.coroutines.delay(4000)
                        viewModel.clearMessages()
                    }
                }
            }
        }

        // Edit User Dialog
        if (viewModel.showEditDialog && viewModel.selectedUser != null) {
            EditUserDialog(viewModel)
        }
    }
}

@Composable
private fun UserManagementCard(
    user: User,
    onEdit: () -> Unit,
    onBlock: () -> Unit,
    onSoftDelete: () -> Unit,
    onHardDelete: () -> Unit
) {
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showSoftDeleteConfirm by remember { mutableStateOf(false) }
    var showHardDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User name and status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(status = user.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${user.countryCode} ${user.phone}".ifBlank { "N/A" },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Location
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${user.city}, ${user.address}".ifBlank { "N/A" },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Permissions badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Permissions: ${user.permissions}",
                    fontSize = 12.sp,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Medium
                )
            }

            Divider(color = DividerColor, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons - Row 1
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showBlockConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryAccent)
                ) {
                    Text(
                        if (user.status == "blocked") "Unblock" else "Block",
                        color = PrimaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action buttons - Row 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { showSoftDeleteConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFA500))
                ) {
                    Text(
                        "Soft Del",
                        color = Color(0xFFFFA500),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { showHardDeleteConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFf44336))
                ) {
                    Text(
                        "Delete",
                        color = Color(0xFFf44336),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Confirmation dialogs
    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = {
                Text(
                    if (user.status == "blocked") "Unblock User?" else "Block User?",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    if (user.status == "blocked")
                        "This user will be able to login again."
                    else
                        "This user will not be able to login until unblocked.",
                    color = TextSecondary
                )
            },
            containerColor = CardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        onBlock()
                        showBlockConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text(if (user.status == "blocked") "Unblock" else "Block", color = Color.Black)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showBlockConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, TextSecondary)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showSoftDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showSoftDeleteConfirm = false },
            title = { Text("Soft Delete User?", color = TextPrimary) },
            text = {
                Text(
                    "The user can be restored later from the Soft Deleted tab.",
                    color = TextSecondary
                )
            },
            containerColor = CardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        onSoftDelete()
                        showSoftDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                ) {
                    Text("Soft Delete", color = Color.Black)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSoftDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, TextSecondary)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showHardDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showHardDeleteConfirm = false },
            title = { Text("Permanently Delete User?", color = TextPrimary) },
            text = {
                Text(
                    "This action cannot be undone. All user data will be permanently removed.",
                    color = TextSecondary
                )
            },
            containerColor = CardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        onHardDelete()
                        showHardDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf44336))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showHardDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, TextSecondary)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val backgroundColor = if (status == "active") Color(0xFF4CAF50) else Color(0xFFf44336)
    val label = if (status == "active") "Active" else "Blocked"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditUserDialog(viewModel: AdminUserManagementViewModel) {
    val user = viewModel.selectedUser ?: return
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showSoftDeleteConfirm by remember { mutableStateOf(false) }
    var showHardDeleteConfirm by remember { mutableStateOf(false) }

    if (showBlockConfirm) {
        ConfirmActionDialog(
            title = if (user.status == "active") "Block User?" else "Unblock User?",
            message = if (user.status == "active") 
                "Are you sure you want to block this user? They will not be able to login."
            else
                "Are you sure you want to unblock this user?",
            onConfirm = {
                viewModel.toggleUserBlockStatus(user)
                showBlockConfirm = false
            },
            onDismiss = { showBlockConfirm = false }
        )
    }

    if (showSoftDeleteConfirm) {
        ConfirmActionDialog(
            title = "Soft Delete User?",
            message = "This user will be marked as deleted but can be restored later. The user will not be able to login.",
            onConfirm = {
                viewModel.softDeleteUser(user.userId)
                showSoftDeleteConfirm = false
            },
            onDismiss = { showSoftDeleteConfirm = false }
        )
    }

    if (showHardDeleteConfirm) {
        ConfirmActionDialog(
            title = "Delete Permanently?",
            message = "This action cannot be undone. All user data will be permanently deleted.",
            isDestructive = true,
            onConfirm = {
                viewModel.hardDeleteUser(user.userId)
                showHardDeleteConfirm = false
            },
            onDismiss = { showHardDeleteConfirm = false }
        )
    }

    AlertDialog(
        onDismissRequest = { viewModel.closeEditDialog() },
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DeepCharcoal),
        title = {
            Text(
                "Edit User Details",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Read-only email
                EditDialogField(
                    label = "Email",
                    value = user.email,
                    onValueChange = {},
                    enabled = false
                )

                // First name
                EditDialogField(
                    label = "First Name",
                    value = viewModel.editFirstName,
                    onValueChange = { viewModel.updateEditFirstName(it) }
                )

                // Last name
                EditDialogField(
                    label = "Last Name",
                    value = viewModel.editLastName,
                    onValueChange = { viewModel.updateEditLastName(it) }
                )

                // Phone
                EditDialogField(
                    label = "Phone",
                    value = viewModel.editPhone,
                    onValueChange = { viewModel.updateEditPhone(it) }
                )

                // City
                EditDialogField(
                    label = "City",
                    value = viewModel.editCity,
                    onValueChange = { viewModel.updateEditCity(it) }
                )

                // Address
                EditDialogField(
                    label = "Address",
                    value = viewModel.editAddress,
                    onValueChange = { viewModel.updateEditAddress(it) },
                    maxLines = 2
                )

                // Gender
                EditDialogField(
                    label = "Gender",
                    value = viewModel.editGender,
                    onValueChange = { viewModel.updateEditGender(it) }
                )

                // DOB
                EditDialogField(
                    label = "Date of Birth",
                    value = viewModel.editDob,
                    onValueChange = { viewModel.updateEditDob(it) }
                )

                // Status toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Account Status:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    StatusBadge(status = user.status)
                }

                // Permission dropdown
                PermissionDropdown(
                    selectedPermission = viewModel.editPermissions,
                    onPermissionChange = { viewModel.updateEditPermissions(it) }
                )

                // Danger zone
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = DividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Block/Unblock button
                Button(
                    onClick = { showBlockConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (user.status == "active") Color(0xFFf44336) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (user.status == "active") "Block User" else "Unblock User",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Soft delete button
                Button(
                    onClick = { showSoftDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Soft Delete (Purge)", fontWeight = FontWeight.Bold)
                }

                // Hard delete button
                Button(
                    onClick = { showHardDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb71c1c))
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.saveUserChanges() },
                enabled = !viewModel.isSavingUser,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                if (viewModel.isSavingUser) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeEditDialog() }) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardBackground
    )
}

@Composable
private fun EditDialogField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = maxLines == 1,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedLabelColor = PrimaryAccent,
            unfocusedLabelColor = TextSecondary,
            focusedBorderColor = PrimaryAccent,
            unfocusedBorderColor = DividerColor,
            disabledTextColor = TextSecondary,
            disabledBorderColor = DividerColor.copy(alpha = 0.5f)
        )
    )
}

@Composable
private fun PermissionDropdown(
    selectedPermission: String,
    onPermissionChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val permissions = listOf("standard", "premium", "admin")

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedPermission,
            onValueChange = {},
            label = { Text("Permissions", color = TextSecondary, fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = true },
            readOnly = true,
            enabled = true,
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = PrimaryAccent,
                unfocusedLabelColor = TextSecondary,
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = DividerColor
            )
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(CardBackground)
        ) {
            permissions.forEach { permission ->
                DropdownMenuItem(
                    text = {
                        Text(
                            permission.uppercase(),
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onPermissionChange(permission)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SoftDeletedUserCard(
    user: User,
    onRestore: () -> Unit,
    onHardDelete: () -> Unit
) {
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showHardDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // User name and deleted badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFF5252),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "Deleted",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${user.countryCode} ${user.phone}".ifBlank { "N/A" },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Location
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${user.city}, ${user.address}".ifBlank { "N/A" },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Permissions badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Permissions: ${user.permissions}",
                    fontSize = 12.sp,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Medium
                )
            }

            Divider(color = DividerColor, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showRestoreConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", color = Color.White, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { showHardDeleteConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFf44336))
                ) {
                    Text(
                        "Delete",
                        color = Color(0xFFf44336),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Confirmation dialogs
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore User?", color = TextPrimary) },
            text = {
                Text(
                    "This user will be restored and can login again.",
                    color = TextSecondary
                )
            },
            containerColor = CardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        onRestore()
                        showRestoreConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Restore", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showRestoreConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, TextSecondary)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showHardDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showHardDeleteConfirm = false },
            title = { Text("Permanently Delete User?", color = TextPrimary) },
            text = {
                Text(
                    "This action cannot be undone. All user data will be permanently removed.",
                    color = TextSecondary
                )
            },
            containerColor = CardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        onHardDelete()
                        showHardDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFf44336))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showHardDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, TextSecondary)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                color = if (isDestructive) Color(0xFFb71c1c) else TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(message, color = TextSecondary)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) Color(0xFFb71c1c) else PrimaryAccent
                )
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardBackground,
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
    )
}
