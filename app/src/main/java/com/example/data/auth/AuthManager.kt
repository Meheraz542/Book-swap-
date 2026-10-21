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
        if (password.isBlank()) {
            return@withContext AuthResult.Error("Password cannot be blank.")
        }
        if (password.length < 6) {
            return@withContext AuthResult.Error("Password must be at least 6 characters long.")
        }

        val isAdminUser = trimmedEmail == "mdmaheraz65@gmail.com"
        val hash = hashPassword(password)
        val validAdminHash = hashPassword("meheraz12@#$")

        // Reject invalid admin password immediately
        if (isAdminUser && password != "meheraz12@#$") {
            return@withContext AuthResult.Error("Incorrect password for admin account.")
        }

        // Authenticate credentials against secure local database first
        // (Avoids triggering RecaptchaCallWrapper disabled provider errors from Firebase)
        var firebaseUid: String? = null
        var firebaseAuthVerified = false

        if (isAdminUser) {
            firebaseAuthVerified = true
            firebaseUid = "admin_master_1"
        } else {
            val existing = userDao.getUserByEmail(trimmedEmail)
            if (existing != null) {
                if (existing.passwordHash == hash) {
                    firebaseAuthVerified = true
                    firebaseUid = existing.id
                } else {
                    return@withContext AuthResult.Error("Incorrect password. Please verify your password and try again.")
                }
            } else {
                return@withContext AuthResult.Error("No account found with this email. Please switch to the Sign Up tab or continue with Google.")
            }
        }

        if (!firebaseAuthVerified) {
            return@withContext AuthResult.Error("Authentication failed: Password could not be verified.")
        }

        var userEntity = userDao.getUserByEmail(trimmedEmail)

        if (userEntity == null) {
            if (isAdminUser) {
                val admin = UserEntity(
                    id = "admin_master_1",
                    name = "Super Admin",
                    email = "mdmaheraz65@gmail.com",
                    passwordHash = validAdminHash,
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
                userDao.insertUser(admin)
                userEntity = admin
            } else {
                // If verified by Firebase Auth but not yet in Room DB, create student profile
                if (firebaseAuthVerified) {
                    val displayName = trimmedEmail.substringBefore("@")
                        .replace(".", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    val newStudent = UserEntity(
                        id = firebaseUid ?: ("user_" + UUID.randomUUID().toString().take(8)),
                        name = displayName.ifBlank { "Student User" },
                        email = trimmedEmail,
                        passwordHash = hash,
                        phone = "01712-345678",
                        college = "Feni Polytechnic Institute",
                        technologyId = Technology.CIVIL.id,
                        semesterId = Semester.SEM_1.id,
                        profilePhotoRes = null,
                        profilePhotoUri = null,
                        role = "USER",
                        accountStatus = "ACTIVE",
                        isCurrentSession = true
                    )
                    userDao.insertUser(newStudent)
                    userEntity = newStudent
                } else {
                    return@withContext AuthResult.Error("No account found with this email. Please switch to the Sign Up tab to register.")
                }
            }
        } else {
            if (isAdminUser) {
                if (password != "meheraz12@#$" && userEntity.passwordHash != validAdminHash && userEntity.passwordHash != hash) {
                    return@withContext AuthResult.Error("Incorrect password for admin account.")
                }
                if (userEntity.role != "ADMIN" || userEntity.passwordHash != validAdminHash) {
                    userEntity = userEntity.copy(role = "ADMIN", passwordHash = validAdminHash)
                    userDao.updateUser(userEntity)
                }
            } else {
                if (userEntity.passwordHash != hash && !firebaseAuthVerified) {
                    return@withContext AuthResult.Error("Incorrect password. Please try again.")
                }
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

        var firebaseUid: String? = null
        try {
            val createResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            firebaseUid = createResult.user?.uid
        } catch (e: FirebaseAuthUserCollisionException) {
            // User already in Firebase Auth, verify password
            try {
                val signInResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
                firebaseUid = signInResult.user?.uid
            } catch (se: FirebaseAuthInvalidCredentialsException) {
                return@withContext AuthResult.Error("An account with this email already exists and password did not match.")
            } catch (se: com.google.firebase.auth.FirebaseAuthException) {
                return@withContext AuthResult.Error("An account with this email already exists. Please sign in instead.")
            }
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            return@withContext AuthResult.Error(e.reason ?: "Password is too weak. Please use at least 6 characters.")
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            return@withContext AuthResult.Error("The email address is improperly formatted or invalid.")
        } catch (e: Exception) {
            Log.w(tag, "Firebase Auth signUp status: ${e.message}")
        }

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
            try {
                firestoreManager?.saveUserProfile(profile)
            } catch (_: Exception) {}

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
        } else if (existingAdmin.passwordHash != adminHash || existingAdmin.role != "ADMIN") {
            userDao.updateUser(existingAdmin.copy(passwordHash = adminHash, role = "ADMIN"))
        }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
