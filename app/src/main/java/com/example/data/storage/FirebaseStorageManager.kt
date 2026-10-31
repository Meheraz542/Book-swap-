package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object FirebaseStorageManager {
    private const val TAG = "FirebaseStorageManager"

    /**
     * Compresses the selected image locally to optimize bandwidth,
     * uploads the compressed JPEG bytes to Firebase Cloud Storage under profile_images/{userId}.jpg,
     * retrieves the secure download URL, and returns it.
     * If Firebase Storage is unavailable or offline, gracefully falls back to a locally persisted URI.
     */
    suspend fun compressAndUploadProfileImage(
        context: Context,
        userId: String,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        // Step 1: Compress locally with automatic downscale and 80% JPEG quality
        val compressedBytes = compressImage(context, sourceUri, maxDimension = 512, quality = 80)

        // Always ensure a persistent local fallback copy is saved with unique timestamp
        val localPersistentUri = try {
            val dir = File(context.filesDir, "marketplace_avatars").apply { if (!exists()) mkdirs() }
            val localFile = File(dir, "avatar_${userId}_$timestamp.jpg")
            if (compressedBytes != null) {
                FileOutputStream(localFile).use { it.write(compressedBytes) }
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(localFile).use { output -> input.copyTo(output) }
                }
            }
            Uri.fromFile(localFile).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist local avatar copy", e)
            sourceUri.toString()
        }

        if (compressedBytes == null) {
            Log.w(TAG, "Image compression returned null, using local URI")
            return@withContext localPersistentUri
        }

        // Step 2: Securely upload to Firebase Cloud Storage
        try {
            val storage = try {
                FirebaseStorage.getInstance()
            } catch (ex: Exception) {
                Log.w(TAG, "Default FirebaseStorage.getInstance failed, trying with bucket: ${ex.message}")
                FirebaseStorage.getInstance("gs://studio-4383603118-a2874.firebasestorage.app")
            }
            val storageRef = storage.reference.child("profile_images/${userId}.jpg")
            Log.d(TAG, "Uploading ${compressedBytes.size} bytes to Firebase Storage path: profile_images/${userId}.jpg")
            
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("userId", userId)
                .setCustomMetadata("updatedAt", timestamp.toString())
                .build()

            storageRef.putBytes(compressedBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded profile picture to Firebase Storage: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload failed, falling back to persistent local URI: ${e.message}")
            localPersistentUri
        }
    }

    /**
     * Compresses and uploads book listing photos to Firebase Cloud Storage under book_images/{setId}_{index}_{timestamp}.jpg.
     * Generates a persistent local copy as fallback to guarantee availability.
     */
    suspend fun compressAndUploadBookImage(
        context: Context,
        setId: String,
        photoIndex: Int,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val compressedBytes = compressImage(context, sourceUri, maxDimension = 1024, quality = 80)

        val localPersistentUri = try {
            val dir = File(context.filesDir, "marketplace_books").apply { if (!exists()) mkdirs() }
            val localFile = File(dir, "book_${setId}_${photoIndex}_$timestamp.jpg")
            if (compressedBytes != null) {
                FileOutputStream(localFile).use { it.write(compressedBytes) }
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(localFile).use { output -> input.copyTo(output) }
                }
            }
            Uri.fromFile(localFile).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist local book image copy", e)
            sourceUri.toString()
        }

        if (compressedBytes == null) {
            Log.w(TAG, "Book image compression returned null, using local URI")
            return@withContext localPersistentUri
        }

        try {
            val storage = try {
                FirebaseStorage.getInstance()
            } catch (ex: Exception) {
                FirebaseStorage.getInstance("gs://studio-4383603118-a2874.firebasestorage.app")
            }
            val storageRef = storage.reference.child("book_images/${setId}_${photoIndex}_$timestamp.jpg")
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("setId", setId)
                .setCustomMetadata("updatedAt", timestamp.toString())
                .build()

            storageRef.putBytes(compressedBytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded book image to Firebase Storage: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage book image upload failed, falling back to local URI: ${e.message}")
            localPersistentUri
        }
    }

    /**
     * Compresses and downscales a bitmap locally to minimize network bandwidth.
     */
    fun compressImage(context: Context, sourceUri: Uri, maxDimension: Int = 512, quality: Int = 80): ByteArray? {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                var sampleSize = 1
                val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                while (maxDim / (sampleSize * 2) >= maxDimension) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

                val finalBitmap = if (decoded.width > maxDimension || decoded.height > maxDimension) {
                    val ratio = minOf(maxDimension.toFloat() / decoded.width, maxDimension.toFloat() / decoded.height)
                    val newWidth = (decoded.width * ratio).toInt().coerceAtLeast(1)
                    val newHeight = (decoded.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, newWidth, newHeight, true).also {
                        if (it != decoded) decoded.recycle()
                    }
                } else {
                    decoded
                }

                val out = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                finalBitmap.recycle()
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            null
        }
    }
}
