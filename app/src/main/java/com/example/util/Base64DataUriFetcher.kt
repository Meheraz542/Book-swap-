package com.example.util

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Base64
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Base64DataUriFetcher(
    private val dataUri: String,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.Default) {
        try {
            val base64Part = dataUri.substringAfter("base64,", "")
                .replace("\\s".toRegex(), "")
            if (base64Part.isEmpty()) {
                Log.w("Base64DataUriFetcher", "Empty base64 payload for data URI")
                return@withContext null
            }

            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            if (bytes.isEmpty()) {
                Log.w("Base64DataUriFetcher", "Decoded empty byte array from data URI")
                return@withContext null
            }

            // Decode bounds first so arbitrarily large images don't trigger OOM
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)

            var inSampleSize = 1
            val maxDim = 1440
            if (boundsOpts.outHeight > maxDim || boundsOpts.outWidth > maxDim) {
                val halfHeight = boundsOpts.outHeight / 2
                val halfWidth = boundsOpts.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                    inSampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext null

            DrawableResult(
                drawable = BitmapDrawable(options.context.resources, bitmap),
                isSampled = inSampleSize > 1,
                dataSource = DataSource.DISK
            )
        } catch (e: Exception) {
            Log.e("Base64DataUriFetcher", "Failed to decode base64 data URI", e)
            null
        }
    }

    /**
     * Handles Uri objects produced by Coil's built-in StringMapper (String -> Uri.parse(data))
     * BEFORE ContentUriFetcher attempts to open them via ContentResolver.
     */
    class UriFactory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val raw = data.toString()
            if ((data.scheme.equals("data", ignoreCase = true) || raw.startsWith("data:image/", ignoreCase = true)) &&
                raw.contains("base64,", ignoreCase = true)
            ) {
                return Base64DataUriFetcher(raw, options)
            }
            return null
        }
    }

    /**
     * Handles raw String data URIs directly when StringMapper is bypassed.
     */
    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.startsWith("data:image/", ignoreCase = true) && data.contains("base64,", ignoreCase = true)) {
                return Base64DataUriFetcher(data, options)
            }
            return null
        }
    }
}
