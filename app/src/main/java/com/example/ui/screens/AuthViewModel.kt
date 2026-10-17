package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.MarketplaceRepository
import com.example.data.auth.AuthResult
import com.example.model.Semester
import com.example.model.Technology
import com.example.model.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data class Loading(val message: String = "Connecting to Google...") : AuthUiState
    data class Success(val user: UserProfile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * Modern ViewModel managing Firebase Google Sign-In with Jetpack Credential Manager.
 * Replaces legacy email/password authentication with clean, 1-tap Google Authentication.
 */
class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Executes the modern Google Sign-In flow using Jetpack Credential Manager.
     */
    fun signInWithGoogle(
        context: Context,
        college: String = "Feni Polytechnic Institute",
        technology: Technology = Technology.CIVIL,
        semester: Semester = Semester.SEM_1,
        onAuthSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading("Opening Google Sign-In...")
            try {
                val credentialManager = CredentialManager.create(context)
                val serverClientId = context.getString(R.string.default_web_client_id)

                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    _uiState.value = AuthUiState.Loading("Authenticating with Firebase...")

                    // Authenticate with Firebase using Google ID Token
                    val firebaseUser = signInToFirebaseWithIdToken(idToken)

                    // Sync student profile with local & cloud storage
                    val authResult = MarketplaceRepository.performGoogleSignIn(
                        googleUid = firebaseUser.uid,
                        email = firebaseUser.email ?: googleIdTokenCredential.id,
                        displayName = firebaseUser.displayName ?: googleIdTokenCredential.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        college = college,
                        technology = technology,
                        semester = semester
                    )

                    when (authResult) {
                        is AuthResult.Success -> {
                            _uiState.value = AuthUiState.Success(authResult.user)
                            onAuthSuccess()
                        }
                        is AuthResult.Error -> {
                            _uiState.value = AuthUiState.Error(authResult.message)
                        }
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Unsupported credential type received from Google.")
                }
            } catch (e: GetCredentialCancellationException) {
                // User intentionally dismissed or cancelled Google prompt
                Log.d("AuthViewModel", "User cancelled Google Sign-In prompt")
                _uiState.value = AuthUiState.Idle
            } catch (e: GetCredentialException) {
                Log.w("AuthViewModel", "Credential manager failure: ${e.message}", e)
                _uiState.value = AuthUiState.Error("Google Play Services unavailable. Please sign in with your email & password.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
            }
        }
    }

    /**
     * Authenticates with Firebase Auth using the Google ID Token.
     */
    private suspend fun signInToFirebaseWithIdToken(idToken: String): FirebaseUser =
        suspendCancellableCoroutine { continuation ->
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        continuation.resume(user)
                    } else {
                        continuation.resumeWithException(IllegalStateException("Firebase user was null after successful sign-in."))
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }



    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
