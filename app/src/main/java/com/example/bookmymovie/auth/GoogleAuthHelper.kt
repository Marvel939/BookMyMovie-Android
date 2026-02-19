package com.example.bookmymovie.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.example.bookmymovie.firebase.User
import java.security.MessageDigest
import java.util.UUID

object GoogleAuthHelper {

    private const val WEB_CLIENT_ID = "198210369844-ada9b28e4duvuftgf7dp1t5j9vf8tt8l.apps.googleusercontent.com"

    suspend fun signInWithGoogle(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val credentialManager = CredentialManager.create(context)

            // Clear any previous credential state to force fresh account picker
            credentialManager.clearCredentialState(ClearCredentialStateRequest())

            // Generate a nonce for security
            val rawNonce = UUID.randomUUID().toString()
            val bytes = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
            val nonce = bytes.joinToString("") { "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = FirebaseAuth.getInstance().currentUser
                            if (firebaseUser != null) {
                                val database = FirebaseDatabase.getInstance().getReference("users")
                                database.child(firebaseUser.uid).get()
                                    .addOnSuccessListener { snapshot ->
                                        if (!snapshot.exists()) {
                                            val nameParts = (firebaseUser.displayName ?: "").split(" ", limit = 2)
                                            val user = User(
                                                userId = firebaseUser.uid,
                                                firstName = nameParts.getOrElse(0) { "" },
                                                lastName = nameParts.getOrElse(1) { "" },
                                                email = firebaseUser.email ?: "",
                                                profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
                                            )
                                            database.child(firebaseUser.uid).setValue(user)
                                        }
                                        onSuccess()
                                    }
                                    .addOnFailureListener {
                                        onSuccess()
                                    }
                            } else {
                                onSuccess()
                            }
                        } else {
                            onError(task.exception?.message ?: "Firebase authentication failed")
                        }
                    }
            } else {
                onError("Unexpected credential type")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Google Sign-In failed")
        }
    }
}
