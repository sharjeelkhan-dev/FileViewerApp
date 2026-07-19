package com.sharjeel.fileviewerapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    var displayName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) ?: "temp_file"
                    
                    // Ensure extension if missing
                    if (!displayName.contains(".")) {
                        val mime = context.contentResolver.getType(uri)
                        val ext = when (mime) {
                            "application/pdf" -> "pdf"
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
                            "application/msword" -> "doc"
                            "application/vnd.ms-word.document.macroEnabled.12" -> "docm"
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
                            "application/vnd.ms-excel" -> "xls"
                            "application/vnd.ms-excel.sheet.macroEnabled.12" -> "xlsm"
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
                            "application/vnd.ms-powerpoint" -> "ppt"
                            "application/vnd.ms-powerpoint.presentation.macroEnabled.12" -> "pptm"
                            "text/plain" -> "txt"
                            "image/jpeg" -> "jpg"
                            "image/png" -> "png"
                            else -> null
                        }
                        if (ext != null) displayName += ".$ext"
                    }

                    val file = File(context.cacheDir, displayName)
                    try {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                        val outputStream = FileOutputStream(file)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()
                        return file.absolutePath
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return null
    }

    fun getFolderPathFromUri(uri: Uri): String? {
        val path = uri.path ?: return null
        return if (path.contains("primary:")) {
            val subPath = path.substringAfter("primary:")
            Environment.getExternalStorageDirectory().absolutePath + "/" + subPath
        } else {
            // Try to resolve generic document tree paths
            if (path.startsWith("/tree/")) {
                val treeId = path.substringAfter("/tree/").substringBefore(":")
                if (treeId == "primary") {
                    val subPath = path.substringAfter(":")
                    Environment.getExternalStorageDirectory().absolutePath + "/" + subPath
                } else null
            } else null
        }
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isVideoFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp", "ts", "flv", "mpeg", "mpg", "wmv")
    }

    fun isAudioFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in listOf("mp3", "wav", "flac", "opus", "ogg", "aac", "m4a", "mpga", "pcm")
    }

    fun isMediaFile(path: String): Boolean {
        return isVideoFile(path) || isAudioFile(path) || isImageFile(path)
    }

    fun isImageFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    }

    fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> "application/pdf"
            "txt", "csv", "log", "kt", "java", "py", "js" -> "text/plain"
            "html", "htm" -> "text/html"
            "xml" -> "text/xml"
            "json" -> "application/json"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "3gp", "3gpp" -> "video/3gpp"
            "flv" -> "video/x-flv"
            "mpeg", "mpg", "mpe" -> "video/mpeg"
            "wmv" -> "video/x-ms-wmv"
            "mp3" -> "audio/mp3"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "aac" -> "audio/aac"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            "ogg" -> "audio/ogg"
            "mpga" -> "audio/mpeg"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc" -> "application/msword"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "xls" -> "application/vnd.ms-excel"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            else -> "*/*"
        }
    }

    fun shareFile(context: Context, filePath: String) {
        val file = File(filePath)
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val mimeType = getMimeType(filePath)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openWithExternalApp(context: Context, filePath: String) {
        val file = File(filePath)
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val mimeType = getMimeType(filePath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
