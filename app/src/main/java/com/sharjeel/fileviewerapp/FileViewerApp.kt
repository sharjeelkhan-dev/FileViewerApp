package com.sharjeel.fileviewerapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FileViewerApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        Firebase.initialize(context = this)
        
        // Setup App Check Debug Provider
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
        
        Log.d("AppCheck", "App Check Debug Provider initialized. Check Logcat for the debug token to register in Firebase Console.")
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
