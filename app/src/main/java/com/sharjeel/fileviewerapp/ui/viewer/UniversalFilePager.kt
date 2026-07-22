package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sharjeel.fileviewerapp.domain.model.FileModel
import kotlinx.coroutines.flow.distinctUntilChanged
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

    val targetInitialPage = remember(filePlaylist) {
        initialIndex.coerceIn(filePlaylist.indices)
    }

    val pagerState = rememberPagerState(
        initialPage = targetInitialPage,
        pageCount = { filePlaylist.size },
    )
    val scope = rememberCoroutineScope()

    // Tracks if the current active page element (Image/Doc/PDF) is zoomed in
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(targetInitialPage) {
        if (pagerState.currentPage != targetInitialPage) {
            pagerState.scrollToPage(targetInitialPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onIndexChanged(page)
                if (page in filePlaylist.indices) {
                    onFileChanged(filePlaylist[page])
                }
                // Reset zoom state on page change
                isCurrentPageZoomed = false
            }
    }

    // Only allow HorizontalPager horizontal gestures if format is media (Images) AND not zoomed
    val isSwipeAllowed by remember(filePlaylist, pagerState.currentPage, isCurrentPageZoomed) {
        derivedStateOf {
            val currentPage = pagerState.currentPage
            if (currentPage in filePlaylist.indices && !isCurrentPageZoomed) {
                val ext = filePlaylist[currentPage].extension.lowercase()
                ext in listOf("jpg", "png", "webp", "gif", "jpeg")
            } else {
                false
            }
        }
    }

    val pagerBackgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pagerBackgroundColor)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> if (index in filePlaylist.indices) filePlaylist[index].path else index },
            userScrollEnabled = isSwipeAllowed,
            beyondViewportPageCount = 0
        ) { page ->
            if (page in filePlaylist.indices) {
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
    val extension = remember(file.path) { file.extension.lowercase() }

    when (extension) {
        "pdf" -> {
            PdfViewerScreen(
                filePath = file.path,
                onTap = onToggleControls
            )
        }

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

        "docx", "doc" -> {
            DocxViewer(
                filePath = file.path,
                onZoomChanged = onZoomChanged,
                onTap = onToggleControls
            )
        }

        "txt", "log", "json", "xml", "kt", "java", "csv" -> {
            TextViewer(
                filePath = file.path,
                onZoomChanged = onZoomChanged,
                onTap = onToggleControls
            )
        }

        "xlsx", "xls" -> {
            XlsxViewer(
                filePath = file.path,
                onZoomChanged = onZoomChanged,
                onTap = onToggleControls
            )
        }

        "epub" -> {
            EpubViewer(
                filePath = file.path,
                onZoomChanged = onZoomChanged,
                onTap = onToggleControls
            )
        }

        "pptx", "ppt" -> {
            PptxViewer(
                filePath = file.path,
                onZoomChanged = onZoomChanged,
                onTap = onToggleControls
            )
        }

        "html", "htm" -> {
            WebViewViewer(
                filePath = file.path,
                onZoomChanged = onZoomChanged,
                onTap = onToggleControls
            )
        }

        else -> {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onToggleControls() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Unsupported Format: ${file.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}