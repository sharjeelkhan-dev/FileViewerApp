package com.sharjeel.fileviewerapp.ui.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

// Fixed: State encapsulation pattern for rigid exam tracking representation
sealed interface TextUiState {
    object Loading : TextUiState
    data class Success(val lines: List<String>) : TextUiState
    data class Error(val message: String) : TextUiState
}

@Composable
fun TextViewer(filePath: String) {
    var uiState by remember { mutableStateOf<TextUiState>(TextUiState.Loading) }

    // Fixed: Offloaded file IO reading tasks to Dispatchers.IO to maintain 60FPS smoothness
    LaunchedEffect(filePath) {
        uiState = TextUiState.Loading
        uiState = withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    TextUiState.Error("File does not exist.")
                } else {
                    // Using BufferedReader to handle large streams iteratively without memory spikes
                    val linesList = mutableListOf<String>()
                    file.inputStream().use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                            var line: String? = reader.readLine()
                            // Upper boundary limit checking for preview safety (e.g., 5000 lines max)
                            var lineCount = 0
                            while (line != null && lineCount < 5000) {
                                linesList.add(line)
                                line = reader.readLine()
                                lineCount++
                            }
                        }
                    }
                    TextUiState.Success(linesList)
                }
            } catch (e: Exception) {
                TextUiState.Error("Error loading file: ${e.localizedMessage}")
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
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
                // Fixed: LazyColumn handles standard window rendering recycle mechanism seamlessly
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    items(state.lines) { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}