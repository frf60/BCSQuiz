package com.faysal.bcsquiz.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("481292178693-mn2s5jjdq0jn0p9a1rkfduhho9ataqr6.apps.googleusercontent.com")
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                
                if (user != null) {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userRef = firestore.collection("users").document(user.uid)
                    val userDoc = userRef.get().await()
                    
                    val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"

                    if (!userDoc.exists()) {
                        // Check how many accounts are registered on this device
                        val existingAccounts = firestore.collection("users")
                            .whereEqualTo("deviceId", deviceId)
                            .get().await()
                        
                        if (existingAccounts.size() >= 2) {
                            return Result.failure(Exception("Maximum 2 accounts allowed per device."))
                        }

                        val newUser = com.faysal.bcsquiz.data.model.User(
                            uid = user.uid,
                            name = user.displayName ?: "User",
                            email = user.email ?: "",
                            phone = user.phoneNumber ?: "",
                            pointsBalance = 50.0, // Login bonus 50.0 points
                            isSubscribed = true, // Owner/Admin is pro by default
                            deviceId = deviceId,
                            role = if (user.email?.lowercase() == "frfaysal6072@gmail.com") "owner" else "user",
                            joinedAt = com.google.firebase.Timestamp.now()
                        )
                        userRef.set(newUser).await()
                    } else {
                        val updates = mutableMapOf<String, Any>(
                            "name" to (user.displayName ?: "User"),
                            "email" to (user.email ?: ""),
                            "deviceId" to deviceId
                        )
                        if (user.email == "frfaysal6072@gmail.com") {
                            updates["role"] = "owner"
                        }
                        userRef.update(updates).await()
                    }
                    Result.success(user)
                } else {
                    Result.failure(Exception("Login failed"))
                }
            } else {
                Result.failure(Exception("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
