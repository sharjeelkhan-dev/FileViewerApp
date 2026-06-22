package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sharjeel.fileviewerapp.domain.model.FileModel
import kotlinx.coroutines.launch


@Composable
fun UniversalFilePager(
    filePlaylist: List<FileModel>,
    initialIndex: Int,
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onFileChanged: (FileModel) -> Unit,
    onIndexChanged: (Int) -> Unit,
) {
    if (filePlaylist.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(filePlaylist.indices),
        pageCount = { filePlaylist.size },
    )
    val scope = rememberCoroutineScope()

    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        onIndexChanged(pagerState.currentPage)
        onFileChanged(filePlaylist[pagerState.currentPage])
        isCurrentPageZoomed = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Pager only enables scrolling when content is NOT zoomed
            userScrollEnabled = !isCurrentPageZoomed,
            // Beyond bounds count set to 0 to save resources for high-res videos
            beyondViewportPageCount = 0
        ) { page ->

            FileContentRenderer(
                file = filePlaylist[page],
                controlsVisible = controlsVisible,
                isActive = pagerState.currentPage == page,
                onZoomChanged = { zoomed ->
                    if (pagerState.currentPage == page) {
                        isCurrentPageZoomed = zoomed
                    }
                },
                onToggleControls = onToggleControls,
                onNext = {
                    if (pagerState.currentPage < filePlaylist.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                onPrevious = {
                    if (pagerState.currentPage > 0) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                }
            )
        }
    }
}

@Composable
fun FileContentRenderer(
    file: FileModel,
    controlsVisible: Boolean,
    isActive: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    onToggleControls: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val context = LocalContext.current
    val extension = remember(file.extension) { file.extension.lowercase() }

    when (extension) {
        "pdf" -> PdfViewerScreen(
            filePath = file.path,
            onZoomChanged = onZoomChanged,
            onTap = onToggleControls
        )

        "jpg", "png", "webp", "gif", "jpeg" -> {
            ImageViewer(
                filePath = file.path,
                onZoomChanged = onZoomChanged,
                onTap = onToggleControls
            )
        }

        "mp4", "mkv", "avi", "webm", "3gp" -> {
            VideoViewer(
                filePath = file.path,
                isVisible = controlsVisible,
                isActive = isActive,
                onNext = onNext,
                onPrevious = onPrevious,
                onTap = onToggleControls
            )
        }

        "mp3", "wav", "flac", "opus", "ogg" -> {
            AudioViewer(
                filePath = file.path,
                isVisible = controlsVisible,
                isActive = isActive,
                onNext = onNext,
                onPrevious = onPrevious,
                onTap = onToggleControls
            )
        }

        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleControls() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Format support fallback view: ${file.name}", color = Color.White)
            }
        }
    }
}
