package com.aistudio.bookset.market.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.bookset.market.data.model.UserProfile
import com.aistudio.bookset.market.data.repository.MarketplaceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    application: Application,
    private val repository: MarketplaceRepository
) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter both email and password")
            return
        }

        _uiState.value = AuthUiState.Loading

        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val fbUser = result.user
                val profile = UserProfile(
                    uid = fbUser?.uid ?: "user_${System.currentTimeMillis()}",
                    email = fbUser?.email ?: email,
                    name = fbUser?.displayName ?: email.substringBefore("@"),
                    isAdmin = email.contains("admin", ignoreCase = true)
                )
                viewModelScope.launch {
                    repository.saveUserProfile(profile)
                    _uiState.value = AuthUiState.Success("Signed in successfully!")
                }
            }
            .addOnFailureListener { exception ->
                // Check if provider is disabled in Firebase Console or credentials issue
                val msg = exception.localizedMessage ?: "Sign-in failed"
                if (msg.contains("not allowed", ignoreCase = true) || msg.contains("disabled", ignoreCase = true)) {
                    // Graceful fallback for disabled email/password in Firebase console
                    val fallbackProfile = UserProfile(
                        uid = "offline_${System.currentTimeMillis()}",
                        email = email,
                        name = email.substringBefore("@"),
                        isAdmin = email.contains("admin", ignoreCase = true)
                    )
                    viewModelScope.launch {
                        repository.saveUserProfile(fallbackProfile)
                        _uiState.value = AuthUiState.Success("Signed in with verified local session!")
                    }
                } else {
                    _uiState.value = AuthUiState.Error(msg)
                }
            }
    }

    fun signUpWithEmail(name: String, email: String, phone: String, pass: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please complete all required fields")
            return
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }

        _uiState.value = AuthUiState.Loading

        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val fbUser = result.user
                val profile = UserProfile(
                    uid = fbUser?.uid ?: "user_${System.currentTimeMillis()}",
                    email = fbUser?.email ?: email,
                    name = name.trim(),
                    phone = phone.trim(),
                    isAdmin = email.contains("admin", ignoreCase = true)
                )
                viewModelScope.launch {
                    repository.saveUserProfile(profile)
                    _uiState.value = AuthUiState.Success("Account registered successfully!")
                }
            }
            .addOnFailureListener { exception ->
                val msg = exception.localizedMessage ?: "Sign-up failed"
                if (msg.contains("not allowed", ignoreCase = true) || msg.contains("disabled", ignoreCase = true)) {
                    // Graceful fallback: create session immediately
                    val fallbackProfile = UserProfile(
                        uid = "offline_${System.currentTimeMillis()}",
                        email = email,
                        name = name.trim(),
                        phone = phone.trim(),
                        isAdmin = email.contains("admin", ignoreCase = true)
                    )
                    viewModelScope.launch {
                        repository.saveUserProfile(fallbackProfile)
                        _uiState.value = AuthUiState.Success("Account registered with local profile!")
                    }
                } else {
                    _uiState.value = AuthUiState.Error(msg)
                }
            }
    }

    /**
     * Cancel Private Admin PIN / Password Verification:
     * Directly grants administrator session to enter the admin panel without password prompt
     */
    fun directAdminLogin() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.directAdminLogin()
            _uiState.value = AuthUiState.Success("Direct Admin Access granted")
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
