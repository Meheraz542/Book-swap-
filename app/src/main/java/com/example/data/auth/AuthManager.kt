package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entities.UserEntity
import com.example.data.local.entities.toUserProfile
import com.example.data.remote.FirebaseFirestoreManager
import com.example.model.Semester
import com.example.model.Technology
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

sealed class AuthResult {
    data class Success(val user: UserProfile) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthManager(
    private val context: Context,
    private val firestoreManager: FirebaseFirestoreManager? = null
) {
    private val tag = "AuthManager"
    private val database = AppDatabase.getInstance(context)
    private val userDao = database.userDao()
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO + NonCancellable) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return@withContext AuthResult.Error("Email address cannot be blank.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() && !trimmedEmail.contains("@")) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (password.isBlank()) {
            return@withContext AuthResult.Error("Password cannot be blank.")
        }
        if (password.length < 6) {
            return@withContext AuthResult.Error("Password must be at least 6 characters long.")
        }

        val isAdminUser = trimmedEmail.equals("mdmaheraz65@gmail.com", ignoreCase = true)
        val hash = hashPassword(password)

        var userEntity = userDao.getUserByEmail(trimmedEmail)

        if (isAdminUser) {
            val adminPasswordHash = userEntity?.passwordHash?.takeIf { it.isNotBlank() } ?: hashPassword("meheraz12@#$")
            if (hash != adminPasswordHash && password != "meheraz12@#$") {
                return@withContext AuthResult.Error("Incorrect password. Please verify your password.")
            }
            val admin = userEntity ?: UserEntity(
                id = "admin_master_1",
                name = "Super Admin",
                email = "mdmaheraz65@gmail.com",
                passwordHash = adminPasswordHash,
                phone = "01712-345678",
                college = "Feni Computer Institute",
                technologyId = Technology.CSE.id,
                semesterId = Semester.SEM_3.id,
                profilePhotoRes = null,
                profilePhotoUri = null,
                role = "ADMIN",
                accountStatus = "ACTIVE",
                isCurrentSession = true
            )
            val updatedAdmin = admin.copy(role = "ADMIN", passwordHash = adminPasswordHash, isCurrentSession = true)
            userDao.insertUser(updatedAdmin)
            userDao.switchActiveSession(updatedAdmin.id)
            userEntity = updatedAdmin
        } else {
            if (userEntity != null && userEntity.passwordHash.isNotBlank() && userEntity.passwordHash != hash) {
                return@withContext AuthResult.Error("Incorrect password. Please verify your password.")
            }
            if (userEntity == null && firestoreManager != null) {
                try {
                    val snap = firestoreManager.firestore.collection("users")
                        .whereEqualTo("email", trimmedEmail).get().await()
                    val doc = snap.documents.firstOrNull()
                    if (doc != null) {
                        val storedHash = doc.getString("passwordHash") ?: ""
                        if (storedHash.isNotBlank() && storedHash != hash) {
                            return@withContext AuthResult.Error("Incorrect password. Please verify your password.")
                        }
                        val name = doc.getString("name") ?: "Student User"
                        val phone = doc.getString("phone") ?: "01712-345678"
                        val college = doc.getString("college") ?: "Feni Polytechnic Institute"
                        val techId = doc.getString("technologyId") ?: "civil"
                        val semId = doc.getString("semesterId") ?: "1"
                        val photoUri = doc.getString("profilePhotoUri")?.takeIf { it.isNotBlank() }
                        val role = doc.getString("role") ?: "USER"
                        val status = doc.getString("accountStatus") ?: "ACTIVE"
                        val entity = UserEntity(
                            id = doc.getString("id") ?: doc.id,
                            name = name,
                            email = trimmedEmail,
                            passwordHash = if (storedHash.isNotBlank()) storedHash else hash,
                            phone = phone,
                            college = college,
                            technologyId = techId,
                            semesterId = semId,
                            profilePhotoRes = null,
                            profilePhotoUri = photoUri,
                            role = role,
                            accountStatus = status,
                            isCurrentSession = true
                        )
                        userDao.insertUser(entity)
                        userEntity = entity
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Firestore lookup exception: ${e.message}")
                }
            }

            if (userEntity == null) {
                return@withContext AuthResult.Error("No registered account found with this email. Please sign up first.")
            } else {
                userDao.updateUser(userEntity.copy(isCurrentSession = true))
            }
        }

        if (userEntity.accountStatus == "SUSPENDED" || userEntity.accountStatus == "BANNED") {
            return@withContext AuthResult.Error("Your account is currently suspended. Please contact the administrator.")
        }

        // Set current session in Room atomically
        userDao.switchActiveSession(userEntity.id)

        var profile = userEntity.toUserProfile()
        if (isAdminUser) {
            profile = profile.copy(role = "ADMIN")
        }

        // Sync with Cloud Firestore
        try {
            firestoreManager?.saveUserProfile(profile)
        } catch (e: Exception) {
            Log.w(tag, "Firestore profile sync note on sign in: ${e.message}")
        }

        AuthResult.Success(profile)
    }

    suspend fun signInWithGoogleUser(
        googleUid: String,
        email: String,
        displayName: String?,
        photoUrl: String?,
        college: String = "Feni Polytechnic Institute",
        technology: Technology = Technology.CIVIL,
        semester: Semester = Semester.SEM_1
    ): AuthResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        val isAdmin = trimmedEmail == "mdmaheraz65@gmail.com"
        var userEntity = userDao.getUserByEmail(trimmedEmail)

        if (userEntity == null) {
            val newUserId = googleUid
            val name = displayName?.ifBlank { null } ?: trimmedEmail.substringBefore("@")
                .replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

            val newUser = UserEntity(
                id = newUserId,
                name = if (isAdmin) "Super Admin" else (name.ifBlank { "Student User" }),
                email = trimmedEmail,
                passwordHash = "",
                phone = "01712-345678",
                college = if (isAdmin) "Feni Computer Institute" else college,
                technologyId = if (isAdmin) Technology.CSE.id else technology.id,
                semesterId = if (isAdmin) Semester.SEM_3.id else semester.id,
                profilePhotoRes = null,
                profilePhotoUri = photoUrl,
                role = if (isAdmin) "ADMIN" else "USER",
                accountStatus = "ACTIVE",
                isCurrentSession = true
            )
            userDao.insertUser(newUser)
            userEntity = newUser
        } else {
            val updated = userEntity.copy(
                name = displayName?.ifBlank { null } ?: userEntity.name,
                profilePhotoUri = photoUrl ?: userEntity.profilePhotoUri,
                isCurrentSession = true,
                technologyId = if (userEntity.technologyId.isBlank()) technology.id else userEntity.technologyId,
                semesterId = if (userEntity.semesterId.isBlank()) semester.id else userEntity.semesterId
            )
            userDao.updateUser(updated)
            userEntity = updated
        }

        userDao.switchActiveSession(userEntity.id)

        val profile = userEntity.toUserProfile()

        // Sync with Cloud Firestore
        try {
            firestoreManager?.saveUserProfile(profile)
        } catch (e: Exception) {
            Log.w(tag, "Firestore profile sync note on Google sign in: ${e.message}")
        }

        AuthResult.Success(profile)
    }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        phone: String,
        college: String,
        technology: Technology,
        semester: Semester
    ): AuthResult = withContext(Dispatchers.IO + NonCancellable) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phone.trim()

        if (trimmedName.length < 2) {
            return@withContext AuthResult.Error("Please enter your full student name.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() && !trimmedEmail.contains("@")) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (password.length < 6) {
            return@withContext AuthResult.Error("Password must be at least 6 characters long.")
        }
        if (trimmedPhone.length < 6) {
            return@withContext AuthResult.Error("Please enter a valid contact phone number.")
        }

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext AuthResult.Error("An account with this email already exists. Please switch to the Sign In tab.")
        }

        if (firestoreManager != null) {
            try {
                val snap = firestoreManager.firestore.collection("users")
                    .whereEqualTo("email", trimmedEmail).get().await()
                if (!snap.isEmpty) {
                    return@withContext AuthResult.Error("An account with this email already exists. Please switch to the Sign In tab.")
                }
            } catch (e: Exception) {
                Log.w(tag, "Firestore existing email check: ${e.message}")
            }
        }

        val isAdmin = trimmedEmail.equals("mdmaheraz65@gmail.com", ignoreCase = true)
        val newUserId = if (isAdmin) "admin_master_1" else ("user_" + UUID.randomUUID().toString().replace("-", "").take(12))

        val newEntity = UserEntity(
            id = newUserId,
            name = trimmedName,
            email = trimmedEmail,
            passwordHash = hashPassword(password),
            phone = trimmedPhone,
            college = college,
            technologyId = technology.id,
            semesterId = semester.id,
            profilePhotoRes = null,
            profilePhotoUri = null,
            role = if (isAdmin) "ADMIN" else "USER",
            accountStatus = "ACTIVE",
            isCurrentSession = true
        )

        userDao.clearCurrentSession()
        userDao.insertUser(newEntity)

        val profile = newEntity.toUserProfile()

        // Sync profile and secure credentials to Cloud Firestore so it is visible across the app and to Admin
        try {
            firestoreManager?.saveUserProfile(profile, passwordHash = newEntity.passwordHash)
            Log.d(tag, "User profile registered and synced to Cloud Firestore: ${profile.id}")
        } catch (e: Exception) {
            Log.w(tag, "Firestore profile sync note on signup: ${e.message}")
        }

        AuthResult.Success(profile)
    }

    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) {}
        userDao.clearCurrentSession()
    }

    suspend fun seedDefaultUsersIfEmpty() = withContext(Dispatchers.IO) {
        val existingAdmin = userDao.getUserByEmail("mdmaheraz65@gmail.com")
        val adminHash = hashPassword("meheraz12@#$")
        if (existingAdmin == null) {
            val admin = UserEntity(
                id = "admin_master_1",
                name = "Super Admin",
                email = "mdmaheraz65@gmail.com",
                passwordHash = adminHash,
                phone = "01712-345678",
                college = "Feni Computer Institute",
                technologyId = Technology.CSE.id,
                semesterId = Semester.SEM_3.id,
                profilePhotoRes = null,
                profilePhotoUri = null,
                role = "ADMIN",
                accountStatus = "ACTIVE",
                isCurrentSession = false
            )
            userDao.insertUser(admin)
        } else {
            userDao.updateUser(existingAdmin.copy(role = "ADMIN"))
        }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
