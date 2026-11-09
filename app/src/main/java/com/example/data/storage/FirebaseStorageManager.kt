package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FirebaseStorageManager {
    private const val TAG = "FirebaseStorageManager"

    /**
     * Resolves the configured FirebaseStorage instance.
     */
    private fun getStorageInstance(context: Context): FirebaseStorage {
        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
        } else {
            FirebaseApp.getInstance()
        }

        val bucket = try {
            val resId = context.resources.getIdentifier("google_storage_bucket", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (_: Exception) {
            null
        } ?: app.options.storageBucket?.takeIf { it.isNotBlank() } ?: "studio-4383603118-a2874.firebasestorage.app"

        return try {
            FirebaseStorage.getInstance(app, "gs://$bucket")
        } catch (ex: Exception) {
            Log.w(TAG, "FirebaseStorage with explicit bucket failed, using default: ${ex.message}")
            FirebaseStorage.getInstance(app)
        }
    }

    /**
     * Uploads user profile picture to Firebase Cloud Storage.
     * Throws an IOException on failure so callers know the upload did not succeed.
     * Never returns local file:// or content:// URIs.
     */
    suspend fun uploadOriginalProfileImage(
        context: Context,
        userId: String,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val mimeType = resolveMimeType(context, sourceUri)
        val ext = resolveExtension(context, sourceUri, mimeType)

        val originalBytes = try {
            context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read input stream for avatar: $sourceUri", e)
            null
        } ?: throw IOException("Could not read image data from $sourceUri")

        if (originalBytes.isEmpty()) {
            throw IOException("Avatar image data is empty for $sourceUri")
        }

        try {
            val storage = getStorageInstance(context)
            val storageRef = storage.reference.child("profile_images/${userId}_$timestamp.$ext")
            Log.d(TAG, "Uploading ${originalBytes.size} bytes for avatar to: profile_images/${userId}_$timestamp.$ext")

            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("userId", userId)
                .setCustomMetadata("updatedAt", timestamp.toString())
                .setCustomMetadata("originalFormat", ext)
                .build()

            storageRef.putBytes(originalBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
                throw IOException("Firebase Storage returned non-HTTPS download URL: $downloadUrl")
            }

            Log.d(TAG, "Successfully uploaded avatar to Firebase Storage: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage avatar upload failed", e)
            throw IOException("Failed to upload avatar to Firebase Storage: ${e.localizedMessage}", e)
        }
    }

    /**
     * Uploads a book listing image to Firebase Cloud Storage.
     * Throws an IOException on failure so that callers never save local URIs or broken references.
     * Never returns local file:// or content:// URIs.
     */
    suspend fun uploadOriginalBookImage(
        context: Context,
        setId: String,
        photoIndex: Int,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val mimeType = resolveMimeType(context, sourceUri)
        val ext = resolveExtension(context, sourceUri, mimeType)

        val originalBytes = try {
            context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read input stream for book image: $sourceUri", e)
            null
        } ?: throw IOException("Could not read image data from $sourceUri")

        if (originalBytes.isEmpty()) {
            throw IOException("Book image data is empty for $sourceUri")
        }

        try {
            val storage = getStorageInstance(context)
            val storageRef = storage.reference.child("book_images/${setId}_${photoIndex}_$timestamp.$ext")
            Log.d(TAG, "Uploading ${originalBytes.size} bytes for book image to: book_images/${setId}_${photoIndex}_$timestamp.$ext")

            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("setId", setId)
                .setCustomMetadata("photoIndex", photoIndex.toString())
                .setCustomMetadata("updatedAt", timestamp.toString())
                .setCustomMetadata("originalFormat", ext)
                .build()

            storageRef.putBytes(originalBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
                throw IOException("Firebase Storage returned non-HTTPS download URL: $downloadUrl")
            }

            Log.d(TAG, "Successfully uploaded book image to Firebase Storage: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Storage book photo upload failed: ${e.message}", e)
            throw IOException("Failed to upload book photo to Firebase Storage: ${e.localizedMessage}", e)
        }
    }

    /**
     * Backward-compatible aliases for existing callers to preserve zero-compression contract.
     */
    suspend fun compressAndUploadProfileImage(context: Context, userId: String, sourceUri: Uri): String {
        return uploadOriginalProfileImage(context, userId, sourceUri)
    }

    suspend fun compressAndUploadBookImage(context: Context, setId: String, photoIndex: Int, sourceUri: Uri): String {
        return uploadOriginalBookImage(context, setId, photoIndex, sourceUri)
    }

    private fun resolveMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: when (uri.path?.substringAfterLast('.', "")?.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            else -> "image/jpeg"
        }
    }

    private fun resolveExtension(context: Context, uri: Uri, mimeType: String = resolveMimeType(context, uri)): String {
        return when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic" -> "heic"
            "image/jpeg" -> "jpg"
            else -> {
                val ext = uri.path?.substringAfterLast('.', "")?.lowercase() ?: ""
                if (ext.isNotBlank() && ext.length in 3..4) ext else "jpg"
            }
        }
    }
}
