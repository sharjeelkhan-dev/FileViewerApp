package com.sharjeel.fileviewerapp.ui.converter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharjeel.fileviewerapp.data.local.dao.FileDao
import com.sharjeel.fileviewerapp.data.local.entity.RecentFileEntity
import com.sharjeel.fileviewerapp.util.FileUtils
import com.sharjeel.fileviewerapp.util.TextExtractionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

enum class ConversionTarget {
    PDF, IMAGE, TEXT
}

enum class DocumentFormat {
    WORD, POWERPOINT, ANY_DOC, EXCEL, IMAGE_TO_PDF
}

enum class FileConversionState {
    SELECTED, CONVERTING, DONE, FAILED
}

data class ConverterFileItem(
    val name: String,
    val path: String,
    val format: DocumentFormat,
    val uri: Uri? = null,
    val state: FileConversionState = FileConversionState.SELECTED
)

data class ConverterUiState(
    val target: ConversionTarget = ConversionTarget.PDF,
    val selectedFormat: DocumentFormat = DocumentFormat.WORD,
    val selectedFiles: List<ConverterFileItem> = emptyList(),
    val isConverting: Boolean = false,
    val conversionComplete: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val outputFiles: List<File> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ConverterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDao: FileDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    // Target change par selected files reset hongi
    fun setTarget(target: ConversionTarget) {
        _uiState.update {
            it.copy(
                target = target,
                selectedFiles = emptyList(), // Selected files cleared on target change
                conversionComplete = false,
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    // Format change par selected files reset hongi
    fun setFormat(format: DocumentFormat) {
        _uiState.update {
            it.copy(
                selectedFormat = format,
                selectedFiles = emptyList(), // Selected files cleared on format switch
                conversionComplete = false,
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    fun addSelectedFileUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileDetails = getFileDetailsFromUri(uri)
                if (fileDetails != null) {
                    _uiState.update { state ->
                        if (state.selectedFiles.none { it.path == fileDetails.path || it.name == fileDetails.name }) {
                            state.copy(
                                selectedFiles = state.selectedFiles + fileDetails,
                                conversionComplete = false,
                                errorMessage = null
                            )
                        } else state
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to load selected file.") }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(errorMessage = "Error loading file: ${e.localizedMessage}") }
            }
        }
    }

    fun addSampleFile() {
        try {
            val format = _uiState.value.selectedFormat
            val sampleName = when (format) {
                DocumentFormat.WORD -> "Sample_Document.docx"
                DocumentFormat.POWERPOINT -> "Presentation_Deck.pptx"
                DocumentFormat.EXCEL -> "Spreadsheet_Data.xlsx"
                else -> "Document_File.txt"
            }

            val dummyFile = File(context.cacheDir, sampleName)
            if (!dummyFile.exists()) {
                try {
                    dummyFile.writeText("Sample document content for conversion testing in File Viewer App.")
                } catch (_: Throwable) {}
            }

            val newItem = ConverterFileItem(
                name = sampleName,
                path = dummyFile.absolutePath,
                format = format,
                uri = Uri.fromFile(dummyFile),
                state = FileConversionState.SELECTED
            )

            _uiState.update { state ->
                if (state.selectedFiles.none { it.name == sampleName }) {
                    state.copy(
                        selectedFiles = state.selectedFiles + newItem,
                        conversionComplete = false,
                        errorMessage = null
                    )
                } else state
            }
        } catch (_: Throwable) {
            _uiState.update { it.copy(errorMessage = "Error adding sample file.") }
        }
    }

    fun removeFile(item: ConverterFileItem) {
        _uiState.update { state ->
            state.copy(
                selectedFiles = state.selectedFiles.filterNot {
                    it == item || (it.path == item.path && it.name == item.name)
                }
            )
        }
    }

    fun removeFileByIndex(index: Int) {
        _uiState.update { state ->
            if (index in state.selectedFiles.indices) {
                val updatedList = state.selectedFiles.toMutableList().apply { removeAt(index) }
                state.copy(selectedFiles = updatedList)
            } else state
        }
    }

    fun clearAllFiles() {
        _uiState.update {
            it.copy(
                selectedFiles = emptyList(),
                conversionComplete = false,
                progress = 0f,
                statusMessage = null,
                errorMessage = null
            )
        }
    }

    fun clearConvertedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentConvertedList = _uiState.value.outputFiles
            currentConvertedList.forEach { file ->
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Throwable) {}
            }
            _uiState.update {
                it.copy(
                    outputFiles = emptyList(),
                    conversionComplete = false
                )
            }
        }
    }

    fun removeConvertedFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Throwable) {}

            _uiState.update { state ->
                val remainingFiles = state.outputFiles.filter {
                    it.absolutePath != file.absolutePath && it.name != file.name
                }
                state.copy(
                    outputFiles = remainingFiles,
                    conversionComplete = remainingFiles.isNotEmpty()
                )
            }
        }
    }

    fun removeConvertedFileByIndex(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = _uiState.value.outputFiles
            if (index in currentList.indices) {
                val fileToDelete = currentList[index]
                try {
                    if (fileToDelete.exists()) {
                        fileToDelete.delete()
                    }
                } catch (_: Throwable) {}

                _uiState.update { state ->
                    val updatedList = state.outputFiles.toMutableList().apply {
                        if (index in state.outputFiles.indices) removeAt(index)
                    }
                    state.copy(
                        outputFiles = updatedList,
                        conversionComplete = updatedList.isNotEmpty()
                    )
                }
            }
        }
    }

    fun convertFiles() {
        val filesToConvert = _uiState.value.selectedFiles
        if (filesToConvert.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one file.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    conversionComplete = false,
                    progress = 0f,
                    statusMessage = "Starting conversion...",
                    errorMessage = null
                )
            }

            try {
                val primaryDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "ConvertedFiles"
                )
                val outputFolder = if (primaryDir.exists() || primaryDir.mkdirs()) {
                    primaryDir
                } else {
                    File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                        "ConvertedFiles"
                    ).apply { if (!exists()) mkdirs() }
                }

                val convertedFilesList = mutableListOf<File>()
                val target = _uiState.value.target

                _uiState.value.selectedFiles.forEachIndexed { index, fileItem ->
                    _uiState.update { state ->
                        val updatedList = state.selectedFiles.mapIndexed { i, item ->
                            if (i == index) item.copy(state = FileConversionState.CONVERTING) else item
                        }
                        state.copy(
                            selectedFiles = updatedList,
                            statusMessage = "Converting: ${fileItem.name}",
                            progress = ((index + 1).toFloat() / state.selectedFiles.size)
                        )
                    }

                    delay(300.milliseconds)

                    val resultFile = when (target) {
                        ConversionTarget.PDF -> convertToPdf(fileItem, outputFolder)
                        ConversionTarget.IMAGE -> convertToImages(fileItem, outputFolder)
                        ConversionTarget.TEXT -> convertToText(fileItem, outputFolder)
                    }

                    if (resultFile != null && resultFile.exists() && resultFile.length() > 0) {
                        convertedFilesList.add(resultFile)

                        try {
                            fileDao.insertRecentFile(
                                RecentFileEntity(
                                    path = resultFile.absolutePath,
                                    name = resultFile.name,
                                    timestamp = System.currentTimeMillis(),
                                    type = resultFile.extension.uppercase()
                                )
                            )
                        } catch (_: Throwable) {}

                        try {
                            android.media.MediaScannerConnection.scanFile(
                                context,
                                arrayOf(resultFile.absolutePath),
                                arrayOf(FileUtils.getMimeType(resultFile.absolutePath))
                            ) { _, _ -> }
                        } catch (_: Throwable) {}
                    }
                }

                if (convertedFilesList.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isConverting = false,
                            conversionComplete = true,
                            progress = 1.0f,
                            statusMessage = "Successfully converted ${convertedFilesList.size} file(s)!",
                            selectedFiles = emptyList(), // Converted queue cleared (Manage Files list vanishes)
                            outputFiles = it.outputFiles + convertedFilesList
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isConverting = false,
                            conversionComplete = false,
                            errorMessage = "Conversion failed. Could not process selected file(s)."
                        )
                    }
                }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        conversionComplete = false,
                        errorMessage = "Conversion error: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    private fun getFileFromItem(fileItem: ConverterFileItem): File? {
        return try {
            val directFile = File(fileItem.path)
            if (directFile.exists() && directFile.length() > 0) {
                return directFile
            }

            fileItem.uri?.let { uri ->
                val pathFromUri = FileUtils.getFilePathFromUri(context, uri)
                if (pathFromUri != null) {
                    val uriFile = File(pathFromUri)
                    if (uriFile.exists() && uriFile.length() > 0) {
                        return uriFile
                    }
                }
            }

            if (directFile.exists()) directFile else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractTextFromItem(fileItem: ConverterFileItem): String {
        return try {
            val file = getFileFromItem(fileItem)
                ?: return "Document File: ${fileItem.name}\n(Selected file could not be opened)"

            val ext = file.extension.lowercase()

            if (ext in listOf("txt", "csv", "log", "json", "xml", "kt", "java", "md", "html", "htm")) {
                try {
                    val text = file.readText()
                    if (text.isNotBlank()) return text
                } catch (_: Throwable) {}
            }

            val extracted = TextExtractionUtils.extractText(file.absolutePath)
            if (!extracted.isNullOrBlank() && !extracted.startsWith("PDF Document:")) {
                return extracted
            }

            if (ext in listOf("pptx", "xlsx", "docx")) {
                val xmlText = extractOfficeXmlText(file)
                if (xmlText.isNotBlank()) return xmlText
            }

            try {
                val directText = file.readText()
                if (directText.isNotBlank() && directText.take(500).all { it.code in 9..126 || it.code in 128..65533 || it.isWhitespace() }) {
                    return directText
                }
            } catch (_: Throwable) {}

            "Document File: ${file.name}\nType: ${ext.uppercase()}\nSize: ${FileUtils.formatFileSize(file.length())}"
        } catch (_: Throwable) {
            "Document File: ${fileItem.name}"
        }
    }

    private fun extractOfficeXmlText(file: File): String {
        val builder = StringBuilder()
        try {
            ZipInputStream(file.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name.lowercase()
                    if (entryName.endsWith(".xml") && (
                                entryName.contains("sharedstrings") ||
                                        entryName.contains("slide") ||
                                        entryName.contains("document") ||
                                        entryName.contains("sheet")
                                )) {
                        val baos = ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            baos.write(buffer, 0, len)
                        }
                        val xmlContent = baos.toString("UTF-8")
                        val cleanText = stripXmlTags(xmlContent)
                        if (cleanText.isNotBlank()) {
                            builder.append(cleanText).append("\n\n")
                            if (builder.length > 50_000) break
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Throwable) {}
        return builder.toString().trim()
    }

    private fun stripXmlTags(xml: String): String {
        val sb = StringBuilder(xml.length)
        var inTag = false
        for (char in xml) {
            if (char == '<') {
                inTag = true
                sb.append(' ')
            } else if (char == '>') {
                inTag = false
            } else if (!inTag) {
                sb.append(char)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun decodeSampledBitmapFromFile(path: String, reqWidth: Int = 1920, reqHeight: Int = 1920): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int = 1920, reqHeight: Int = 1920): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private suspend fun convertToPdf(fileItem: ConverterFileItem, outputFolder: File): File? = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        try {
            val outputPdfFile = File(outputFolder, "${fileItem.name.substringBeforeLast(".")}_converted.pdf")
            val file = getFileFromItem(fileItem)

            if (file != null && FileUtils.isImageFile(file.path)) {
                val bitmap = decodeSampledBitmapFromFile(file.absolutePath)
                    ?: fileItem.uri?.let { uri -> decodeSampledBitmapFromUri(uri) }

                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)

                    FileOutputStream(outputPdfFile).use { out ->
                        pdfDocument.writeTo(out)
                    }
                    bitmap.recycle()
                    return@withContext outputPdfFile
                }
            }

            if (file != null && file.extension.equals("pdf", ignoreCase = true)) {
                file.copyTo(outputPdfFile, overwrite = true)
                return@withContext outputPdfFile
            }

            val contentText = extractTextFromItem(fileItem)

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                color = "#1E3A8A".toColorInt()
                textSize = 16f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val lines = contentText.split("\n")
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = 50f

            canvas.drawText(fileItem.name, 40f, yPosition, titlePaint)
            yPosition += 20f
            paint.strokeWidth = 1f
            paint.color = Color.LTGRAY
            canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
            yPosition += 25f
            paint.color = Color.BLACK

            val wrappedLines = mutableListOf<String>()
            for (line in lines) {
                if (line.length <= 70) {
                    wrappedLines.add(line)
                } else {
                    line.chunked(70).forEach { wrappedLines.add(it) }
                }
            }

            for (line in wrappedLines) {
                if (yPosition > 780f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }
                canvas.drawText(line, 40f, yPosition, paint)
                yPosition += 18f
            }

            pdfDocument.finishPage(page)

            FileOutputStream(outputPdfFile).use { out ->
                pdfDocument.writeTo(out)
            }

            outputPdfFile
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Throwable) {}
        }
    }

    private suspend fun convertToImages(fileItem: ConverterFileItem, outputFolder: File): File? = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(outputFolder, "${fileItem.name.substringBeforeLast(".")}_page1.png")
            val file = getFileFromItem(fileItem)

            if (file != null && file.extension.equals("pdf", ignoreCase = true)) {
                var pfd: ParcelFileDescriptor? = null
                try {
                    pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } catch (_: Throwable) {
                    fileItem.uri?.let { uri ->
                        pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    }
                }

                pfd?.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val targetWidth = (page.width * 1.5f).toInt().coerceAtMost(1600)
                        val targetHeight = (page.height * 1.5f).toInt().coerceAtMost(2400)

                        val bitmap = createBitmap(targetWidth, targetHeight)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderer.close()

                        FileOutputStream(outputFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        bitmap.recycle()
                        return@withContext outputFile
                    }
                    renderer.close()
                }
            }

            if (file != null && FileUtils.isImageFile(file.path)) {
                val bitmap = decodeSampledBitmapFromFile(file.absolutePath)
                if (bitmap != null) {
                    FileOutputStream(outputFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    bitmap.recycle()
                    return@withContext outputFile
                }
            }

            val bitmap = createBitmap(800, 1000)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val headerPaint = Paint().apply {
                color = "#1E293B".toColorInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 800f, 100f, headerPaint)

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("Document: ${fileItem.name}", 40f, 60f, titlePaint)

            val bodyPaint = Paint().apply {
                color = "#334155".toColorInt()
                textSize = 18f
                isAntiAlias = true
            }

            val text = extractTextFromItem(fileItem)
            var yPos = 150f
            text.lines().take(35).forEach { line ->
                val truncated = if (line.length > 50) line.take(50) + "..." else line
                canvas.drawText(truncated, 40f, yPos, bodyPaint)
                yPos += 24f
            }

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            outputFile
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun convertToText(fileItem: ConverterFileItem, outputFolder: File): File? = withContext(Dispatchers.IO) {
        try {
            val outputTxtFile = File(outputFolder, "${fileItem.name.substringBeforeLast(".")}_extracted.txt")
            val content = extractTextFromItem(fileItem)

            outputTxtFile.writeText("=== Extracted Content: ${fileItem.name} ===\n\n$content")
            outputTxtFile
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileDetailsFromUri(uri: Uri): ConverterFileItem? {
        return try {
            var name = "Selected_Document"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }

            val cachedPath = FileUtils.getFilePathFromUri(context, uri) ?: uri.path ?: uri.toString()
            val cachedFile = File(cachedPath)
            if (cachedFile.exists()) {
                name = cachedFile.name
            }

            val format = when {
                name.endsWith(".docx", true) || name.endsWith(".doc", true) -> DocumentFormat.WORD
                name.endsWith(".pptx", true) || name.endsWith(".ppt", true) -> DocumentFormat.POWERPOINT
                name.endsWith(".xlsx", true) || name.endsWith(".xls", true) -> DocumentFormat.EXCEL
                name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) -> DocumentFormat.IMAGE_TO_PDF
                else -> DocumentFormat.ANY_DOC
            }

            ConverterFileItem(
                name = name,
                path = cachedPath,
                format = format,
                uri = uri,
                state = FileConversionState.SELECTED
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun resetStatus() {
        _uiState.update {
            it.copy(
                statusMessage = null,
                errorMessage = null,
                conversionComplete = false
            )
        }
    }
}