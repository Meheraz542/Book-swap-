package com.example

import android.app.Application
import android.util.Log
import com.example.data.MarketplaceRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MarketplaceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase()
        MarketplaceRepository.initialize(this)
    }

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
                val apiKey = com.example.BuildConfig.GOOGLE_API_KEY.ifEmpty { "" }
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
