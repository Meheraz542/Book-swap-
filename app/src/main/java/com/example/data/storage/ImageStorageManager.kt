package com.example.data.storage

import android.content.Context
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
     * Persistently saves the exact original image selected by the user without any compression,
     * downscaling, resizing, or format conversion. Preserves the exact file format and bytes.
     */
    suspend fun saveImageLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }
            val ext = resolveExtension(context, sourceUri)
            val fileName = "book_${UUID.randomUUID()}_${System.currentTimeMillis()}.$ext"
            val targetFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val persistentUri = Uri.fromFile(targetFile).toString()
            Log.d(TAG, "Preserved exact original image without compression: $persistentUri (${targetFile.length() / 1024} KB)")
            persistentUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving original local image, falling back to source URI", e)
            sourceUri.toString()
        }
    }

    /**
     * Persistently saves the exact original user avatar without any compression or resizing.
     */
    suspend fun saveAvatarLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, AVATARS_DIR).apply { if (!exists()) mkdirs() }
            val ext = resolveExtension(context, sourceUri)
            val timestamp = System.currentTimeMillis()
            val fileName = "avatar_${UUID.randomUUID()}_$timestamp.$ext"
            val targetFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val persistentUri = Uri.fromFile(targetFile).toString()
            Log.d(TAG, "Preserved exact original avatar without compression: $persistentUri (${targetFile.length() / 1024} KB)")
            persistentUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving original local avatar", e)
            sourceUri.toString()
        }
    }

    private fun resolveExtension(context: Context, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
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
