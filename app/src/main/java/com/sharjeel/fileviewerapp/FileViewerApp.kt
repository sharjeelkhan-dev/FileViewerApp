package com.sharjeel.fileviewerapp

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FileViewerApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Firebase App explicitly
        FirebaseApp.initializeApp(this)

        // 2. Install App Check Debug Provider using Java-compatible API
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        Log.d("AppCheck", "App Check Debug Provider initialized successfully.")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}