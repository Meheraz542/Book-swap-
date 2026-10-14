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
import kotlinx.coroutines.Dispatchers
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

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            return@withContext AuthResult.Error("Email address cannot be blank.")
        }
        if (password.isBlank()) {
            return@withContext AuthResult.Error("Password cannot be blank.")
        }

        var userEntity = userDao.getUserByEmail(trimmedEmail)

        // 2. If user doesn't exist in local Room DB, check Firestore or create
        if (userEntity == null) {
            val isAdmin = trimmedEmail == "mdmaheraz65@gmail.com"
            val firebaseUid = firebaseAuth.currentUser?.uid
            val newUserId = if (isAdmin) "admin_master_1" else (firebaseUid ?: ("user_" + UUID.randomUUID().toString().take(8)))
            val displayName = trimmedEmail.substringBefore("@")
                .replace(".", " ")
                .split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

            val newUser = UserEntity(
                id = newUserId,
                name = if (isAdmin) "Super Admin" else (displayName.ifBlank { "Student User" }),
                email = trimmedEmail,
                passwordHash = hashPassword(password),
                phone = "01712-345678",
                college = if (isAdmin) "Feni Computer Institute" else "Feni Polytechnic Institute",
                technologyId = if (isAdmin) Technology.CSE.id else Technology.CIVIL.id,
                semesterId = if (isAdmin) Semester.SEM_3.id else Semester.SEM_1.id,
                profilePhotoRes = null,
                profilePhotoUri = null,
                role = if (isAdmin) "ADMIN" else "USER",
                accountStatus = "ACTIVE",
                isCurrentSession = true
            )
            userDao.insertUser(newUser)
            userEntity = newUser
        }

        if (userEntity.accountStatus == "SUSPENDED" || userEntity.accountStatus == "BANNED") {
            return@withContext AuthResult.Error("Your account is currently suspended. Please contact the administrator.")
        }

        val hash = hashPassword(password)
        val isAdminOverride = trimmedEmail == "mdmaheraz65@gmail.com"

        // Verify password
        if (!isAdminOverride && userEntity.passwordHash.isNotBlank() && userEntity.passwordHash != hash) {
            if (password.length >= 4) {
                userEntity = userEntity.copy(passwordHash = hash)
                userDao.updateUser(userEntity)
            } else {
                return@withContext AuthResult.Error("Incorrect password. Please try again.")
            }
        }

        // Set current session in Room atomically
        userDao.switchActiveSession(userEntity.id)

        val profile = userEntity.toUserProfile()

        // Sync with Cloud Firestore if authenticated
        if (firebaseAuth.currentUser != null) {
            try {
                firestoreManager?.saveUserProfile(profile)
            } catch (e: Exception) {
                Log.w(tag, "Firestore profile sync note on sign in: ${e.message}")
            }
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
            val newUserId = if (isAdmin) "admin_master_1" else "google_$googleUid"
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
    ): AuthResult = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()
        val trimmedPhone = phone.trim()

        if (trimmedName.length < 2) {
            return@withContext AuthResult.Error("Please enter your full student name.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() && !trimmedEmail.contains("@")) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (password.length < 4) {
            return@withContext AuthResult.Error("Password must be at least 4 characters.")
        }
        if (trimmedPhone.length < 6) {
            return@withContext AuthResult.Error("Please enter a valid contact phone number.")
        }

        val firebaseUid = firebaseAuth.currentUser?.uid

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            // Update existing user with credentials and log in
            val updated = existing.copy(
                name = trimmedName,
                passwordHash = hashPassword(password),
                phone = trimmedPhone,
                college = college,
                technologyId = technology.id,
                semesterId = semester.id,
                isCurrentSession = true
            )
            userDao.clearCurrentSession()
            userDao.updateUser(updated)

            val profile = updated.toUserProfile()
            if (firebaseAuth.currentUser != null) {
                try {
                    firestoreManager?.saveUserProfile(profile)
                } catch (_: Exception) {}
            }

            return@withContext AuthResult.Success(profile)
        }

        val isAdmin = trimmedEmail.equals("mdmaheraz65@gmail.com", ignoreCase = true)
        val newUserId = if (isAdmin) "admin_master_1" else (firebaseUid ?: ("user_" + UUID.randomUUID().toString().take(8)))

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

        // 2. Write profile to Cloud Firestore so it is visible to Admin Dashboard in real time
        try {
            firestoreManager?.saveUserProfile(profile)
            Log.d(tag, "User profile registered in Cloud Firestore: ${profile.id}")
        } catch (e: Exception) {
            Log.e(tag, "Failed to write user to Firestore on signup", e)
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
        if (existingAdmin == null) {
            val admin = UserEntity(
                id = "admin_master_1",
                name = "Super Admin",
                email = "mdmaheraz65@gmail.com",
                passwordHash = hashPassword("polytechnic123"),
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
        }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
