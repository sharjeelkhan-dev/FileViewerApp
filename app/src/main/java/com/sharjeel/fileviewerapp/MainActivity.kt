package com.sharjeel.fileviewerapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.sharjeel.fileviewerapp.ui.MainScreen
import com.sharjeel.fileviewerapp.ui.navigation.NavRoute
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import com.sharjeel.fileviewerapp.util.FileUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var initialRoute by mutableStateOf<NavRoute>(NavRoute.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge layout framework enable karein
        enableEdgeToEdge()

        // System ke default grey contrast overlays ko band karein taake solid custom colors perfectly transparent aur seamless dikhein
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        initialRoute = handleIntent(intent)

        setContent {
            FileViewerAppTheme {
                MainScreen(initialRoute = initialRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initialRoute = handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?): NavRoute {
        if (intent == null) return NavRoute.Home

        val action = intent.action
        val data: Uri? = intent.data

        if (action == Intent.ACTION_VIEW && data != null) {
            val path = FileUtils.getFilePathFromUri(this, data) ?: return NavRoute.Home
            val extension = path.substringAfterLast('.', "").lowercase()
            return NavRoute.Viewer(filePath = path, fileType = extension)
        }

        return NavRoute.Home
    }
}