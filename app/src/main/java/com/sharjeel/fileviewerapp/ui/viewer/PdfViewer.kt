package com.sharjeel.fileviewerapp.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerScreen(
    filePath: String,
    onZoomChanged: (Boolean) -> Unit = {},
    onTap: () -> Unit = {}
) {
    var pageCount by remember { mutableIntStateOf(0) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val extractedTextCache = remember { mutableStateMapOf<Int, String>() }
    val listState = rememberLazyListState()

    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    error = "File does not exist"
                    isLoading = false
                    return@withContext
                }
                val parcelFd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pfd = parcelFd
                val pdfRenderer = PdfRenderer(parcelFd)
                renderer = pdfRenderer
                pageCount = pdfRenderer.pageCount
                isLoading = false
            } catch (e: Exception) {
                error = e.localizedMessage
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                renderer?.close()
                pfd?.close()
                textRecognizer.close()
                extractedTextCache.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val finalBgColor = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(finalBgColor)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else if (error != null) {
            Text(
                text = "Error: $error",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.error
            )
        } else {
            GestureCoordinatedBox(
                onZoomChanged = onZoomChanged,
                onTap = onTap
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
                        top = 80.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(pageCount, key = { index -> "$filePath-$index" }) { index ->
                        PdfPageItem(
                            renderer = renderer,
                            index = index,
                            textCache = extractedTextCache,
                            textRecognizer = textRecognizer
                        )
                    }
                }
            }
            val firstVisibleItem by remember { derivedStateOf { listState.firstVisibleItemIndex } }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                        text = "${firstVisibleItem + 1} / $pageCount",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPageItem(
    renderer: PdfRenderer?,
    index: Int,
    textCache: MutableMap<Int, String>,
    textRecognizer: TextRecognizer
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    var isExtracting by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }

    val extractedText = textCache[index]

    LaunchedEffect(renderer, index) {
        if (renderer == null) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            var pageToRender: PdfRenderer.Page? = null
            var tempBitmap: Bitmap? = null
            try {
                ensureActive()
                synchronized(renderer) {
                    try {
                        pageToRender = renderer.openPage(index)
                    } catch (_: Exception) {
                        return@withContext
                    }
                }

                pageToRender?.let { page ->
                    val scaleFactor = if (page.width > 2000) 1.1f else 1.6f
                    val targetWidth = (page.width * scaleFactor).toInt().coerceAtLeast(1)
                    val targetHeight = (page.height * scaleFactor).toInt().coerceAtLeast(1)

                    tempBitmap = createBitmap(targetWidth, targetHeight)

                    ensureActive()
                    synchronized(renderer) {
                        try {
                            page.render(tempBitmap!!, null,
                                null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                        } catch (_: Exception) {
                            tempBitmap?.recycle()
                            tempBitmap = null
                        }
                    }
                }

                ensureActive()
                if (tempBitmap != null) {
                    bitmap = tempBitmap
                }
                if (!textCache.containsKey(index) && tempBitmap != null) {
                    isExtracting = true
                    val image = InputImage.fromBitmap(tempBitmap, 0)

                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            val fullTextBuilder = StringBuilder()
                            for (block in visionText.textBlocks) {
                                for (line in block.lines) {
                                    fullTextBuilder.append(line.text).append("\n")
                                }
                                fullTextBuilder.append("\n")
                            }
                            val finalResult = fullTextBuilder.toString().trim()
                            if (finalResult.isNotEmpty()) {
                                textCache[index] = finalResult
                            }
                            isExtracting = false
                        }
                        .addOnFailureListener {
                            isExtracting = false
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tempBitmap?.recycle()
                isExtracting = false
            }
        }
    }
    Surface(
        modifier = Modifier
            .widthIn(max = 850.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(4.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            bitmap?.let { b ->
                Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )

                if (!isExtracting && !extractedText.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { showTextDialog = true },
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TextFormat,
                                contentDescription = "Show full page text",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Copied PDF Text Page ${index + 1}", extractedText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Page ${index + 1} complete text copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Instant Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isExtracting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        strokeWidth = 2.dp
                    )
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showTextDialog && !extractedText.isNullOrEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showTextDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${index + 1} Text",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Copied PDF Text Page ${index + 1}", extractedText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            showTextDialog = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp,
                            vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Entire Page")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = extractedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    DisposableEffect(index) {
        onDispose {
            bitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
            bitmap = null
        }
    }
}