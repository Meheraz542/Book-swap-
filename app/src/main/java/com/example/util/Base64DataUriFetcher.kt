package com.example.util

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

class Base64DataUriFetcher(
    private val dataUri: String,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        return try {
            val base64Data = dataUri.substringAfter("base64,", "")
            if (base64Data.isEmpty()) return null
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            DrawableResult(
                drawable = BitmapDrawable(options.context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        } catch (e: Exception) {
            Log.e("Base64DataUriFetcher", "Failed to decode base64 data URI", e)
            null
        }
    }

    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.startsWith("data:image/") && data.contains("base64,")) {
                return Base64DataUriFetcher(data, options)
            }
            return null
        }
    }
}
