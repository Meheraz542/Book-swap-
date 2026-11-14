package com.example

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.data.MarketplaceRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MarketplaceApplication : Application(), ImageLoaderFactory {
    private val singletonImageLoader: ImageLoader by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("marketplace_image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .components {
                add(com.example.util.Base64DataUriFetcher.UriFactory())
                add(com.example.util.Base64DataUriFetcher.Factory())
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        initFirebase()
        coil.Coil.setImageLoader(this)
        MarketplaceRepository.initialize(this)
    }

    override fun newImageLoader(): ImageLoader = singletonImageLoader

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app != null) {
                    Log.d("MarketplaceApp", "FirebaseApp initialized via default resources: ${app.name}")
                    return
                }
            } else {
                return
            }
        } catch (e: Exception) {
            Log.w("MarketplaceApp", "Standard FirebaseApp.initializeApp failed, attempting fallback options: ${e.message}")
        }

        // Robust fallback with explicit FirebaseOptions from google-services.json
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val apiKey = com.example.BuildConfig.GOOGLE_API_KEY.ifEmpty { "AIzaSyD2RDBEXhKVXADbPD-SsM0h5YPNgf9ntE4" }
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:986504877917:android:1f76f3f3338ce3a1eabab4")
                    .setApiKey(apiKey)
                    .setProjectId("studio-4383603118-a2874")
                    .setStorageBucket("studio-4383603118-a2874.firebasestorage.app")
                    .setGcmSenderId("986504877917")
                    .build()
                val app = FirebaseApp.initializeApp(this, options)
                Log.d("MarketplaceApp", "FirebaseApp initialized via explicit options: ${app.name}")
            }
        } catch (e: Exception) {
            Log.e("MarketplaceApp", "Critical error initializing FirebaseApp with explicit options", e)
        }
    }
}

