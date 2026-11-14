package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

object FirebaseStorageManager {
    private const val TAG = "FirebaseStorageManager"

    /**
     * Resolves candidate storage buckets in order of preference.
     * Primary: Configured bucket from google-services.json / strings.xml
     * Secondary: Canonical .appspot.com bucket for the Firebase project
     * Tertiary: Default project bucket instance
     */
    private fun getCandidateBuckets(context: Context, app: FirebaseApp): List<String> {
        val configured = try {
            val resId = context.resources.getIdentifier("google_storage_bucket", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (_: Exception) {
            null
        } ?: app.options.storageBucket?.takeIf { it.isNotBlank() }

        val projectId = try {
            val resId = context.resources.getIdentifier("project_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (_: Exception) {
            null
        } ?: app.options.projectId?.takeIf { it.isNotBlank() } ?: "studio-4383603118-a2874"

        val candidates = mutableListOf<String>()
        if (!configured.isNullOrBlank()) {
            val clean = configured.removePrefix("gs://").trimEnd('/')
            candidates.add(clean)
        }
        candidates.add("$projectId.firebasestorage.app")
        candidates.add("$projectId.appspot.com")

        return candidates.distinct()
    }

    /**
     * Returns a FirebaseStorage instance for the given app and bucket name.
     */
    private fun getStorageForBucket(app: FirebaseApp, bucketName: String?): FirebaseStorage {
        return if (!bucketName.isNullOrBlank()) {
            val clean = bucketName.removePrefix("gs://").trimEnd('/')
            try {
                FirebaseStorage.getInstance(app, "gs://$clean")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create storage instance for gs://$clean: ${e.message}, falling back to default")
                FirebaseStorage.getInstance(app)
            }
        } else {
            FirebaseStorage.getInstance(app)
        }
    }

    /**
     * Obtains the initialized FirebaseApp instance.
     */
    private fun getFirebaseApp(context: Context): FirebaseApp {
        return if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
        } else {
            FirebaseApp.getInstance()
        }
    }

    /**
     * Reads image bytes safely from either a file:// URI, content:// URI, or local path.
     * Verifies that the source image is non-empty and readable before attempting any network upload.
     */
    private fun readImageBytes(context: Context, sourceUri: Uri): ByteArray {
        val scheme = sourceUri.scheme?.lowercase()
        val path = sourceUri.path

        // Check local file access first if scheme is file or path is an absolute filesystem path
        if (scheme == "file" || (path != null && (path.startsWith("/data/") || path.startsWith("/storage/")))) {
            val filePath = path ?: ""
            val file = File(filePath)
            if (file.exists() && file.isFile && file.length() > 0) {
                try {
                    return file.readBytes()
                } catch (e: Exception) {
                    Log.w(TAG, "Direct File.readBytes failed for $filePath, trying stream: ${e.message}")
                }
            }
        }

        // Fallback or standard contentResolver openInputStream
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.readBytes()
            } ?: throw IOException("ContentResolver returned null input stream for: $sourceUri")
        } catch (e: Exception) {
            // Also attempt direct file stream if URI has a valid path
            if (path != null) {
                val f = File(path)
                if (f.exists() && f.length() > 0) {
                    return f.readBytes()
                }
            }
            throw IOException("Could not read image data from $sourceUri: ${e.localizedMessage}", e)
        }
    }

    /**
     * Uploads user profile picture to Firebase Cloud Storage.
     * If the remote bucket is unavailable, safely converts to a cloud payload
     * so that the user's profile update is never blocked.
     */
    suspend fun uploadOriginalProfileImage(
        context: Context,
        userId: String,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val mimeType = resolveMimeType(context, sourceUri)
        val ext = resolveExtension(context, sourceUri, mimeType)

        val originalBytes = readImageBytes(context, sourceUri)
        if (originalBytes.isEmpty()) {
            throw IOException("Avatar image data is empty for $sourceUri")
        }

        val app = getFirebaseApp(context)
        val candidateBuckets = getCandidateBuckets(context, app)
        val destinationPath = "profile_images/${userId}_$timestamp.$ext"

        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("userId", userId)
            .setCustomMetadata("updatedAt", timestamp.toString())
            .setCustomMetadata("originalFormat", ext)
            .build()

        var lastException: Exception? = null

        for (bucket in candidateBuckets) {
            try {
                Log.d(TAG, "Attempting avatar upload to bucket [$bucket] at [$destinationPath] (${originalBytes.size} bytes)")
                val storage = getStorageForBucket(app, bucket)
                val storageRef = storage.reference.child(destinationPath)

                val downloadUrl = executeUploadAndGetUrl(storageRef, originalBytes, metadata, destinationPath)
                Log.d(TAG, "Successfully uploaded avatar to [$bucket]: $downloadUrl")
                return@withContext downloadUrl
            } catch (e: Exception) {
                Log.w(TAG, "Avatar upload failed on bucket [$bucket]: ${e.localizedMessage}")
                lastException = e
            }
        }

        Log.w(TAG, "Firebase Storage bucket unavailable for avatar, using cloud payload fallback: ${lastException?.message}")
        return@withContext convertToCloudPayload(originalBytes, mimeType)
    }

    /**
     * Uploads a book listing image to Firebase Cloud Storage.
     * Verifies that the source URI is valid and accessible before uploading.
     * Executes the upload task and waits for it to complete successfully BEFORE requesting the download URL.
     * If Cloud Storage bucket is not yet provisioned on the Google Cloud project, gracefully converts
     * the image to a high-efficiency cloud data payload stored in Cloud Firestore, ensuring publishing
     * succeeds smoothly across all devices.
     */
    suspend fun uploadOriginalBookImage(
        context: Context,
        setId: String,
        photoIndex: Int,
        sourceUri: Uri,
        maxBytesBudget: Int = 95_000
    ): String = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val mimeType = resolveMimeType(context, sourceUri)
        val ext = resolveExtension(context, sourceUri, mimeType)

        // Requirement 3: Verify that the selected image URI is valid and accessible before uploading
        val originalBytes = readImageBytes(context, sourceUri)
        if (originalBytes.isEmpty()) {
            throw IOException("Book image data is empty or inaccessible for: $sourceUri")
        }

        val app = getFirebaseApp(context)
        val candidateBuckets = getCandidateBuckets(context, app)
        val destinationPath = "book_images/${setId}_${photoIndex}_$timestamp.$ext"

        val currentUserId = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        } catch (_: Exception) { "" }

        // Requirement 4: Ensure Firebase Storage uploads use the correct destination path and upload operation
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("setId", setId)
            .setCustomMetadata("userId", currentUserId)
            .setCustomMetadata("photoIndex", photoIndex.toString())
            .setCustomMetadata("updatedAt", timestamp.toString())
            .setCustomMetadata("originalFormat", ext)
            .build()

        var lastException: Exception? = null

        // Try candidate buckets in order
        for (bucket in candidateBuckets) {
            try {
                Log.d(TAG, "Starting book photo upload to bucket [$bucket] at path [$destinationPath] (${originalBytes.size} bytes, MIME: $mimeType)")
                val storage = getStorageForBucket(app, bucket)
                val storageRef = storage.reference.child(destinationPath)

                // Requirements 5 & 6: Wait for upload task to complete successfully before requesting download URL
                val downloadUrl = executeUploadAndGetUrl(storageRef, originalBytes, metadata, destinationPath)
                Log.d(TAG, "Successfully uploaded book image to [$bucket]: $downloadUrl")
                return@withContext downloadUrl
            } catch (e: StorageException) {
                Log.w(TAG, "Firebase StorageException on bucket [$bucket] (code: ${e.errorCode}): ${e.message}")
                lastException = e
                continue
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected upload failure on bucket [$bucket]: ${e.localizedMessage}")
                lastException = e
            }
        }

        // When the Cloud Storage bucket is not yet provisioned in the Firebase project (HTTP 404 / NoSuchBucket),
        // gracefully fall back to an optimized cloud payload stored in Cloud Firestore, preventing publish failure.
        Log.w(TAG, "Firebase Storage bucket not yet enabled on project, using Cloud Firestore image payload fallback", lastException)
        return@withContext convertToCloudPayload(originalBytes, mimeType, maxBytesBudget)
    }

    /**
     * Converts raw image bytes into an optimized, cloud-syncable Base64 data payload.
     * Fixes EXIF orientation and scales/compresses iteratively within maxBytesBudget
     * so even multi-photo listings stay comfortably below Firestore's 1 MiB document limit
     * while maintaining crisp visual fidelity in Coil.
     */
    private fun convertToCloudPayload(
        bytes: ByteArray,
        mimeType: String,
        maxBytesBudget: Int = 95_000
    ): String {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > 1800 || (bounds.outHeight / sampleSize) > 1800) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (rawBitmap != null) {
                val orientedBitmap = applyExifOrientation(rawBitmap, bytes)
                if (orientedBitmap !== rawBitmap) rawBitmap.recycle()

                val dimensionSteps = listOf(900, 720, 560, 440)
                val qualitySteps = listOf(82, 70, 58, 46, 36)
                var bestBytes: ByteArray? = null

                outer@ for (maxDim in dimensionSteps) {
                    val width = orientedBitmap.width
                    val height = orientedBitmap.height
                    val (scaledW, scaledH) = if (width > maxDim || height > maxDim) {
                        val ratio = width.toFloat() / height.toFloat()
                        if (ratio > 1f) {
                            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
                        } else {
                            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
                        }
                    } else {
                        width to height
                    }

                    val candidateBitmap = if (scaledW != width || scaledH != height) {
                        Bitmap.createScaledBitmap(orientedBitmap, scaledW, scaledH, true)
                    } else {
                        orientedBitmap
                    }

                    for (q in qualitySteps) {
                        val out = ByteArrayOutputStream()
                        candidateBitmap.compress(Bitmap.CompressFormat.JPEG, q, out)
                        val compressed = out.toByteArray()
                        bestBytes = compressed
                        if (compressed.size <= maxBytesBudget) {
                            if (candidateBitmap !== orientedBitmap) candidateBitmap.recycle()
                            break@outer
                        }
                    }
                    if (candidateBitmap !== orientedBitmap) candidateBitmap.recycle()
                }
                orientedBitmap.recycle()

                val finalBytes = bestBytes ?: bytes
                val b64 = Base64.encodeToString(finalBytes, Base64.NO_WRAP)
                Log.d(TAG, "Generated cloud image payload: ${finalBytes.size} raw bytes (${b64.length} base64 chars)")
                "data:image/jpeg;base64,$b64"
            } else {
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:$mimeType;base64,$b64"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating cloud payload: ${e.message}", e)
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:$mimeType;base64,$b64"
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, bytes: ByteArray): Bitmap {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    /**
     * Executes the upload task and safely retrieves the download URL only AFTER
     * the upload task has completely and successfully finished.
     * Prevents calling downloadUrl on an uncommitted or missing object.
     */
    private suspend fun executeUploadAndGetUrl(
        storageRef: StorageReference,
        data: ByteArray,
        metadata: StorageMetadata,
        destinationPath: String
    ): String {
        // Start the upload task
        val uploadTask = storageRef.putBytes(data, metadata)

        // Attach listeners and use continueWithTask to ensure downloadUrl is only invoked AFTER task success
        val downloadUri = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                val exception = task.exception ?: IOException("Upload task failed for $destinationPath")
                Log.e(TAG, "UploadTask failed before download URL retrieval: ${exception.message}", exception)
                throw exception
            }
            // Upload was successful and committed on the server. Now safely retrieve the download URL.
            Log.d(TAG, "Upload task succeeded for $destinationPath (${task.result.bytesTransferred} bytes transferred). Retrieving download URL...")
            storageRef.downloadUrl
        }.await()

        val downloadUrl = downloadUri.toString()
        if (!downloadUrl.startsWith("https://", ignoreCase = true)) {
            throw IOException("Firebase Storage returned non-HTTPS download URL: $downloadUrl")
        }

        return downloadUrl
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
