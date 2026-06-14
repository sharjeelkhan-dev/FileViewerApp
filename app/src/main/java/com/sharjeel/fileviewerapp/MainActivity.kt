package com.sharjeel.fileviewerapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.sharjeel.fileviewerapp.ui.MainScreen
import com.sharjeel.fileviewerapp.ui.theme.FileViewerAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FileViewerAppTheme {
                MainScreen()
            }
        }
    }
}
