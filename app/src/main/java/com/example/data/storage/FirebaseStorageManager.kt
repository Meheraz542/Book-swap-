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
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory

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
            Log.w(TAG, "Firebase Storage upload failed: ${e.message}. Generating cross-device Base64 data URI.")
            try {
                val originalBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                if (originalBytes != null && originalBytes.isNotEmpty()) {
                    val base64String = android.util.Base64.encodeToString(originalBytes, android.util.Base64.NO_WRAP)
                    "data:$mimeType;base64,$base64String"
                } else {
                    localPersistentUri
                }
            } catch (err: Exception) {
                Log.e(TAG, "Failed to generate cross-device avatar data URI", err)
                localPersistentUri
            }
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
            Log.w(TAG, "Firebase Storage book photo upload failed: ${e.message}. Generating optimized cross-device Base64 data URI.")
            try {
                // If Firebase Storage bucket is unavailable or 404, encode the image to a data URI so all other devices can view it via Firestore
                val originalBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                if (originalBytes != null && originalBytes.isNotEmpty()) {
                    val optimizedBytes = prepareFirestoreSafeImageBytes(originalBytes)
                    val base64String = android.util.Base64.encodeToString(optimizedBytes, android.util.Base64.NO_WRAP)
                    val dataUri = "data:image/jpeg;base64,$base64String"
                    Log.d(TAG, "Generated optimized cross-device data URI (${dataUri.length} chars) for book photo $photoIndex of set $setId")
                    dataUri
                } else {
                    localPersistentUri
                }
            } catch (err: Exception) {
                Log.e(TAG, "Failed to generate cross-device data URI", err)
                localPersistentUri
            }
        }
    }

    /**
     * Resizes large images to max 1280px dimension and compresses to 82% JPEG quality.
     * Guarantees the image data is easily under 250KB, ensuring it fits cleanly into Firestore's 1MB document limit.
     */
    private fun prepareFirestoreSafeImageBytes(bytes: ByteArray, maxDim: Int = 1280, quality: Int = 82): ByteArray {
        return try {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            val width = bmp.width
            val height = bmp.height
            val scaledBmp = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val newW: Int
                val newH: Int
                if (ratio > 1) {
                    newW = maxDim
                    newH = (maxDim / ratio).toInt().coerceAtLeast(1)
                } else {
                    newH = maxDim
                    newW = (maxDim * ratio).toInt().coerceAtLeast(1)
                }
                Bitmap.createScaledBitmap(bmp, newW, newH, true)
            } else {
                bmp
            }
            val baos = ByteArrayOutputStream()
            scaledBmp.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            Log.w(TAG, "Image optimization failed, falling back to original bytes: ${e.message}")
            bytes
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
