package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class SadhanaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("SadhanaApp", "FirebaseApp initialized in Application.onCreate")
            }
        } catch (e: Exception) {
            Log.e("SadhanaApp", "Error initializing FirebaseApp in Application.onCreate", e)
        }
    }
}
