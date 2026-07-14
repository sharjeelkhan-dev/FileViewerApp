package com.sharjeel.fileviewerapp.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharjeel.fileviewerapp.util.TextExtractionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface TextUiState {
    data object Loading : TextUiState
    data class Success(val pages: List<String>) : TextUiState
    data class Error(val message: String) : TextUiState
}

@Composable
fun TextViewer(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    var uiState by remember { mutableStateOf<TextUiState>(TextUiState.Loading) }
    val listState = rememberLazyListState()

    LaunchedEffect(filePath) {
        uiState = TextUiState.Loading
        uiState = withContext(Dispatchers.IO) {
            try {
                val extractedText = TextExtractionUtils.extractText(filePath)
                if (extractedText == null) {
                    TextUiState.Error("Could not extract text from this file.")
                } else {
                    // Optimized chunking to mimic PDF pages (approx 1800 chars per page)
                    val chunks = extractedText.chunked(1800)
                    TextUiState.Success(chunks.take(1000))
                }
            } catch (e: Exception) {
                TextUiState.Error("Error loading document: ${e.localizedMessage}")
            }
        }
    }

    val finalBgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(finalBgColor)
            // 🎯 FIXED: Smart Tap Detection (Initial Pass)
            // Header ab sirf tab toggle hoga jab aap waqayi tap karenge.
            // Scroll ya Zoom karte waqt header baar-baar nahi aayega.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    var isTap = true
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        // Agar finger touch slop se zyada move ho gayi, to ye tap nahi hai
                        if (event.changes.any { (it.position - down.position).getDistance() > viewConfiguration.touchSlop }) {
                            isTap = false
                        }
                        // Jab finger lift ho jaye
                        if (event.changes.all { !it.pressed }) {
                            if (isTap) onTap()
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is TextUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is TextUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is TextUiState.Success -> {
                GestureCoordinatedBox(
                    onZoomChanged = onZoomChanged,
                    onTap = { /* Handled by outer Box to prevent double-toggle */ }
                ) { scale, offset ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(finalBgColor)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 96.dp, 
                            bottom = 48.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(state.pages.size, key = { index -> "$filePath-$index" }) { index ->
                            TextPageItem(
                                pageText = state.pages[index],
                                index = index
                            )
                        }
                    }
                }

                val firstVisibleItem by remember { derivedStateOf { listState.firstVisibleItemIndex } }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 10.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${firstVisibleItem + 1} / ${state.pages.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TextPageItem(
    pageText: String,
    index: Int
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .widthIn(max = 850.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(4.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Page ${index + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Gray,
                        letterSpacing = 1.5.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                SelectionContainer {
                    Text(
                        text = pageText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.4.sp
                        ),
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied Text Page ${index + 1}", pageText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Page ${index + 1} copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy Page",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
