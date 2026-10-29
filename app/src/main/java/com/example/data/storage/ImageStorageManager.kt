package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageManager {
    private const val TAG = "ImageStorageManager"
    private const val DIRECTORY_NAME = "marketplace_images"
    private const val AVATARS_DIR = "marketplace_avatars"

    /**
     * Persistently saves an image from a temporary content Uri to app internal storage with automatic compression.
     * Downscales the image to a maximum dimension of 1024px and compresses to 80% JPEG quality,
     * drastically reducing file size while preserving textbook legibility and crisp visual quality.
     */
    suspend fun saveImageLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, DIRECTORY_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val fileName = "book_${UUID.randomUUID()}_${System.currentTimeMillis()}.jpg"
            val targetFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                // Decode bounds first to prevent OutOfMemory on huge camera pictures
                val tempBytes = inputStream.readBytes()
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, boundsOptions)

                val reqMaxDim = 1024
                var sampleSize = 1
                val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                while (maxDim / (sampleSize * 2) >= reqMaxDim) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

                val decodedBitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)
                    ?: throw IllegalStateException("Failed to decode bitmap from stream")

                // Fine downscale if decoded dimensions still exceed target max dimension
                val finalBitmap = if (decodedBitmap.width > reqMaxDim || decodedBitmap.height > reqMaxDim) {
                    val ratio = minOf(reqMaxDim.toFloat() / decodedBitmap.width, reqMaxDim.toFloat() / decodedBitmap.height)
                    val targetW = (decodedBitmap.width * ratio).toInt().coerceAtLeast(1)
                    val targetH = (decodedBitmap.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true).also {
                        if (it != decodedBitmap) decodedBitmap.recycle()
                    }
                } else {
                    decodedBitmap
                }

                FileOutputStream(targetFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                finalBitmap.recycle()
            }

            val persistentUri = Uri.fromFile(targetFile).toString()
            Log.d(TAG, "Successfully compressed and saved image: $persistentUri (size: ${targetFile.length() / 1024} KB)")
            persistentUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving compressed local image, falling back to original uri", e)
            sourceUri.toString()
        }
    }

    /**
     * Persistently saves and compresses a user avatar image to max 512px with 80% JPEG quality.
     */
    suspend fun saveAvatarLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, AVATARS_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "avatar_${UUID.randomUUID()}_$timestamp.jpg"
            val targetFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                val reqMaxDim = 512
                var sampleSize = 1
                val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                while (maxDim / (sampleSize * 2) >= reqMaxDim) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    ?: throw IllegalStateException("Could not decode avatar")

                val finalBitmap = if (decoded.width > reqMaxDim || decoded.height > reqMaxDim) {
                    val ratio = minOf(reqMaxDim.toFloat() / decoded.width, reqMaxDim.toFloat() / decoded.height)
                    val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                    val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                        if (it != decoded) decoded.recycle()
                    }
                } else {
                    decoded
                }

                FileOutputStream(targetFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                finalBitmap.recycle()
            }

            // Include timestamp query parameter to bust Coil cache immediately
            val uriWithCacheBuster = "${Uri.fromFile(targetFile)}?t=$timestamp"
            Log.d(TAG, "Compressed avatar saved to $uriWithCacheBuster (size: ${targetFile.length() / 1024} KB)")
            uriWithCacheBuster
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local avatar", e)
            sourceUri.toString()
        }
    }
}
