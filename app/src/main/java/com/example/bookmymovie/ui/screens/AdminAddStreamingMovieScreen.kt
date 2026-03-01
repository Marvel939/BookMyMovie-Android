package com.example.bookmymovie.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bookmymovie.ui.theme.*
import com.example.bookmymovie.ui.viewmodel.AdminStreamingViewModel

private val ottPlatforms = listOf("Netflix", "Prime Video", "Hotstar", "Zee5", "SonyLIV", "JioCinema", "MX Player")
private val languages = listOf("Hindi", "English", "Tamil", "Telugu", "Malayalam", "Kannada", "Bengali", "Marathi", "Gujarati", "Punjabi")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddStreamingMovieScreen(navController: NavController) {
    val vm: AdminStreamingViewModel = viewModel(
        viewModelStoreOwner = navController.previousBackStackEntry!!
    )

    val isEditing = vm.editingMovieId != null
    val scrollState = rememberScrollState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.errorMessage) {
        vm.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edit Streaming Movie" else "Add Streaming Movie",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepCharcoal)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepCharcoal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section: Basic Info
            SectionHeader("Basic Information")

            StreamField(value = vm.title, onValueChange = { vm.title = it }, label = "Movie Title *")
            StreamField(value = vm.posterUrl, onValueChange = { vm.posterUrl = it }, label = "Poster URL *")
            StreamField(value = vm.bannerUrl, onValueChange = { vm.bannerUrl = it }, label = "Banner URL")
            StreamField(
                value = vm.description, onValueChange = { vm.description = it },
                label = "Description", singleLine = false, minLines = 3
            )

            // Section: Details
            SectionHeader("Details")

            StreamField(value = vm.genre, onValueChange = { vm.genre = it }, label = "Genre (e.g. Action, Drama) *")
            StreamField(value = vm.duration, onValueChange = { vm.duration = it }, label = "Duration (e.g. 2h 15m) *")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StreamField(
                    value = vm.releaseYear, onValueChange = { vm.releaseYear = it },
                    label = "Release Year *", keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                StreamField(
                    value = vm.rating, onValueChange = { vm.rating = it },
                    label = "Rating (0-10)", keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            // Language multi-select
            StreamMultiSelectDropdown(
                selectedValues = vm.language,
                onSelectionChange = { vm.language = it },
                options = languages,
                label = "Language"
            )

            StreamField(value = vm.director, onValueChange = { vm.director = it }, label = "Director")
            StreamField(value = vm.castInput, onValueChange = { vm.castInput = it }, label = "Cast (comma separated)")

            // Section: Streaming
            SectionHeader("Streaming Configuration")

            // OTT Platform dropdown
            StreamDropdown(
                value = vm.ottPlatform,
                onSelect = { vm.ottPlatform = it },
                options = ottPlatforms,
                label = "OTT Platform *"
            )

            StreamField(value = vm.trailerUrl, onValueChange = { vm.trailerUrl = it }, label = "Trailer URL")
            StreamField(value = vm.streamUrl, onValueChange = { vm.streamUrl = it }, label = "Stream URL")

            // Section: Pricing
            SectionHeader("Pricing")

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StreamField(
                    value = vm.rentPrice, onValueChange = { vm.rentPrice = it },
                    label = "Rent Price (₹) *", keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                StreamField(
                    value = vm.buyPrice, onValueChange = { vm.buyPrice = it },
                    label = "Buy Price (₹) *", keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            StreamField(
                value = vm.rentDurationDays, onValueChange = { vm.rentDurationDays = it },
                label = "Rent Duration (days)", keyboardType = KeyboardType.Number
            )

            // Exclusive toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Exclusive Title", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("Mark as platform exclusive", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = vm.isExclusive,
                    onCheckedChange = { vm.isExclusive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryAccent,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DividerColor
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = { vm.saveMovie { navController.popBackStack() } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                enabled = !vm.isSaving
            ) {
                if (vm.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isEditing) "Update Movie" else "Add Movie",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Reusable Components ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Column {
        Spacer(Modifier.height(4.dp))
        Text(title, color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun StreamField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamDropdown(
    value: String,
    onSelect: (String) -> Unit,
    options: List<String>,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextSecondary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardBackground)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextPrimary) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamMultiSelectDropdown(
    selectedValues: String,
    onSelectionChange: (String) -> Unit,
    options: List<String>,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSet = remember(selectedValues) {
        selectedValues.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedValues,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextSecondary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardBackground)
        ) {
            options.forEach { option ->
                val isSelected = option in selectedSet
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PrimaryAccent,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(option, color = TextPrimary)
                        }
                    },
                    onClick = {
                        val newSet = selectedSet.toMutableSet()
                        if (isSelected) newSet.remove(option) else newSet.add(option)
                        onSelectionChange(newSet.joinToString(", "))
                    }
                )
            }
        }
    }
}
