package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FirebaseStorageManager {
    private const val TAG = "FirebaseStorageManager"

    /**
     * Stores and uploads user profile pictures without any compression, resizing, downscaling,
     * or format conversion. Exact original image bytes and MIME type are preserved.
     */
    suspend fun uploadOriginalProfileImage(
        context: Context,
        userId: String,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val mimeType = resolveMimeType(context, sourceUri)
        val ext = resolveExtension(context, sourceUri, mimeType)

        // Store exact original bytes to app internal storage as persistent local fallback
        val localPersistentUri = try {
            val dir = File(context.filesDir, "marketplace_avatars").apply { if (!exists()) mkdirs() }
            val localFile = File(dir, "avatar_${userId}_$timestamp.$ext")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(localFile).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(localFile).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist original avatar copy", e)
            sourceUri.toString()
        }

        try {
            val originalBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return@withContext localPersistentUri

            val storage = try {
                FirebaseStorage.getInstance()
            } catch (ex: Exception) {
                Log.w(TAG, "Default FirebaseStorage.getInstance failed, trying with bucket: ${ex.message}")
                FirebaseStorage.getInstance("gs://studio-4383603118-a2874.firebasestorage.app")
            }
            val storageRef = storage.reference.child("profile_images/${userId}_$timestamp.$ext")
            Log.d(TAG, "Uploading ${originalBytes.size} original bytes to Firebase Storage: profile_images/${userId}_$timestamp.$ext")

            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("userId", userId)
                .setCustomMetadata("updatedAt", timestamp.toString())
                .setCustomMetadata("originalFormat", ext)
                .build()

            storageRef.putBytes(originalBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded exact original profile picture to Firebase Storage: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload failed, falling back to local persistent URI: ${e.message}")
            localPersistentUri
        }
    }

    /**
     * Stores and uploads book listing photos to Firebase Cloud Storage without any compression,
     * resizing, or format conversion. Exact original image bytes and MIME type are preserved.
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

        // Store exact original bytes to app internal storage as persistent local fallback
        val localPersistentUri = try {
            val dir = File(context.filesDir, "marketplace_books").apply { if (!exists()) mkdirs() }
            val localFile = File(dir, "book_${setId}_${photoIndex}_$timestamp.$ext")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(localFile).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(localFile).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist original book image copy", e)
            sourceUri.toString()
        }

        try {
            val originalBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return@withContext localPersistentUri

            val storage = try {
                FirebaseStorage.getInstance()
            } catch (ex: Exception) {
                FirebaseStorage.getInstance("gs://studio-4383603118-a2874.firebasestorage.app")
            }
            val storageRef = storage.reference.child("book_images/${setId}_${photoIndex}_$timestamp.$ext")
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("setId", setId)
                .setCustomMetadata("photoIndex", photoIndex.toString())
                .setCustomMetadata("updatedAt", timestamp.toString())
                .setCustomMetadata("originalFormat", ext)
                .build()

            storageRef.putBytes(originalBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded exact original book photo to Firebase Storage: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage book photo upload failed, falling back to local persistent URI: ${e.message}")
            localPersistentUri
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
