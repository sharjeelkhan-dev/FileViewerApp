package com.sharjeel.fileviewerapp.util

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log
import com.github.junrar.Junrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object ArchiveUtils {
    private const val TAG = "ArchiveUtils"

    suspend fun extractZip(
        zipFile: File, 
        destinationDir: File,
        context: Context,
        onProgress: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting ZIP extraction: ${zipFile.absolutePath} -> ${destinationDir.absolutePath}")
        val scannedPaths = mutableListOf<String>()
        try {
            if (!zipFile.exists() || !zipFile.canRead()) {
                onProgress("Error: Cannot read zip file")
                return@withContext false
            }
            
            if (!destinationDir.exists()) {
                if (!destinationDir.mkdirs()) {
                    onProgress("Error: Cannot create folder")
                    return@withContext false
                }
            }
            
            // Add folder to scanning too
            scannedPaths.add(destinationDir.absolutePath)

            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                var count = 0
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val newFile = File(destinationDir, entry.name)
                    
                    // Security: Zip Slip check
                    val destCanonical = destinationDir.canonicalPath
                    val entryCanonical = newFile.canonicalPath
                    if (!entryCanonical.startsWith(destCanonical + File.separator) && 
                        entryCanonical != destCanonical) {
                        continue
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                        scannedPaths.add(newFile.absolutePath)
                    } else {
                        newFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(newFile).use { output ->
                                val buffer = ByteArray(8192)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                        scannedPaths.add(newFile.absolutePath)
                    }
                    count++
                    if (count % 10 == 0) onProgress("Extracting file $count...")
                }
            }
            
            // Professional: Deep scan for Gallery/System
            if (scannedPaths.isNotEmpty()) {
                // Scan in chunks to avoid intent limits/memory issues for huge archives
                scannedPaths.chunked(100).forEach { chunk ->
                    MediaScannerConnection.scanFile(context, chunk.toTypedArray(), null, null)
                }
            }
            
            Log.d(TAG, "ZIP extraction finished successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "ZIP Error: ${e.message}", e)
            onProgress("Error: ${e.message}")
            false
        }
    }

    suspend fun extractRar(
        rarFile: File, 
        destinationDir: File,
        context: Context,
        onProgress: (String) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting RAR extraction: ${rarFile.absolutePath}")
        val scannedPaths = mutableListOf<String>()
        try {
            if (!rarFile.exists()) {
                onProgress("Error: RAR file not found")
                return@withContext false
            }
            if (!destinationDir.exists()) {
                if (!destinationDir.mkdirs()) {
                    onProgress("Error: Cannot create folder")
                    return@withContext false
                }
            }
            
            scannedPaths.add(destinationDir.absolutePath)
            onProgress("Extracting RAR content...")
            
            Junrar.extract(rarFile, destinationDir)
            
            // Collect all extracted paths for system scan
            destinationDir.walkTopDown().forEach {
                scannedPaths.add(it.absolutePath)
            }
            
            if (scannedPaths.isNotEmpty()) {
                scannedPaths.chunked(100).forEach { chunk ->
                    MediaScannerConnection.scanFile(context, chunk.toTypedArray(), null, null)
                }
            }

            Log.d(TAG, "RAR extraction finished")
            true
        } catch (e: Exception) {
            Log.e(TAG, "RAR Error: ${e.message}", e)
            onProgress("Error: ${e.message}")
            false
        }
    }
}
