package com.sharjeel.fileviewerapp.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object Home : NavRoute

    @Serializable
    data class Explorer(val title: String = "Explorer", val path: String? = null) : NavRoute

    @Serializable
    data class Viewer(val filePath: String, val fileType: String) : NavRoute

    @Serializable
    data object Vault : NavRoute

    @Serializable
    data object Trash : NavRoute

    @Serializable
    data object Favorites : NavRoute

    @Serializable
    data object Settings : NavRoute
}
