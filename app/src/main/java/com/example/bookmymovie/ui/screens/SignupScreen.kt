package com.example.bookmymovie.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.bookmymovie.R
import com.example.bookmymovie.auth.GoogleAuthHelper
import com.example.bookmymovie.firebase.User
import com.example.bookmymovie.navigation.Screen
import com.example.bookmymovie.ui.components.AppLogo
import com.example.bookmymovie.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.util.*

// Reusable text field colors for sign-up form
@Composable
private fun premiumFieldColors() = OutlinedTextFieldDefaults.colors(
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

@Composable
fun SignupScreen(navController: NavController) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var countryCode by remember { mutableStateOf("+91") }
    var isCodeExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().getReference("users")
    val coroutineScope = rememberCoroutineScope()

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            dob = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun isPasswordValid(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isLowerCase() }) return false
        if (!password.any { !it.isLetterOrDigit() }) return false
        return true
    }

    val fieldColors = premiumFieldColors()

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
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Create Account",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Fill in your details to get started",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name", color = TextSecondary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = fieldColors
            )
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name", color = TextSecondary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = fieldColors
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(0.3f)) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Code", color = TextSecondary) },
                    shape = RoundedCornerShape(16.dp),
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
                        disabledContainerColor = CardBackground
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
                onValueChange = { phone = it },
                label = { Text("Phone Number", color = TextSecondary) },
                modifier = Modifier.weight(0.7f),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = fieldColors
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            "Gender",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 4.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = gender == "Male",
                onClick = { gender = "Male" },
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryAccent,
                    unselectedColor = TextSecondary
                )
            )
            Text("Male", color = TextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(
                selected = gender == "Female",
                onClick = { gender = "Female" },
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryAccent,
                    unselectedColor = TextSecondary
                )
            )
            Text("Female", color = TextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(
                selected = gender == "Other",
                onClick = { gender = "Other" },
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryAccent,
                    unselectedColor = TextSecondary
                )
            )
            Text("Other", color = TextPrimary, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = { },
            label = { Text("Date of Birth", color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() },
            shape = RoundedCornerShape(16.dp),
            enabled = false,
            trailingIcon = {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.clickable { datePickerDialog.show() }
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = DividerColor,
                disabledTextColor = TextPrimary,
                disabledLabelColor = TextSecondary,
                disabledContainerColor = CardBackground
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = fieldColors
        )
        if (password.isNotEmpty() && !isPasswordValid(password)) {
            Text(
                "Password must be 8+ chars, include uppercase, lowercase, number, and special char.",
                color = SecondaryAccent,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = PrimaryAccent)
        } else {
            Button(
                onClick = {
                    if (firstName.isNotEmpty() && lastName.isNotEmpty() && email.isNotEmpty() &&
                        password.isNotEmpty() && confirmPassword.isNotEmpty() &&
                        gender.isNotEmpty() && phone.isNotEmpty() && dob.isNotEmpty() && city.isNotEmpty()) {

                        if (password == confirmPassword) {
                            if (isPasswordValid(password)) {
                                isLoading = true
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val userId = auth.currentUser?.uid ?: ""
                                            val user = User(
                                                userId = userId,
                                                firstName = firstName,
                                                lastName = lastName,
                                                email = email,
                                                gender = gender,
                                                address = address,
                                                countryCode = countryCode,
                                                phone = phone,
                                                dob = dob,
                                                city = city
                                            )
                                            database.child(userId).setValue(user).addOnCompleteListener { dbTask ->
                                                if (dbTask.isSuccessful) {
                                                    auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                                                        isLoading = false
                                                        if (emailTask.isSuccessful) {
                                                            Toast.makeText(context, "Verification email sent. Please verify and Login.", Toast.LENGTH_LONG).show()
                                                            auth.signOut()
                                                            navController.navigate(Screen.Login.route) {
                                                                popUpTo(Screen.Signup.route) { inclusive = true }
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    isLoading = false
                                                    Toast.makeText(context, "Database error", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "Password does not meet requirements", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Sign Up", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                popUpTo(Screen.Signup.route) { inclusive = true }
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
                    "Sign up with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row {
            Text("Already have an account? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Login",
                color = PrimaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    navController.popBackStack()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text("Are you an Admin? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                "Admin Register",
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
                "Register as Owner",
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
