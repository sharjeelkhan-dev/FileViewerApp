package com.sharjeel.fileviewerapp.util

import com.github.junrar.Junrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object ArchiveUtils {

    suspend fun extractZip(zipFile: File, destinationDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!zipFile.exists() || !zipFile.canRead()) return@withContext false
            
            // Ensure destination exists
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }
            if (!destinationDir.isDirectory) return@withContext false

            val destCanonicalPath = destinationDir.canonicalPath
            
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val newFile = File(destinationDir, entry.name)
                    
                    // Security check: Zip Slip vulnerability
                    val entryCanonicalPath = newFile.canonicalPath
                    if (!entryCanonicalPath.startsWith(destCanonicalPath + File.separator) && 
                        entryCanonicalPath != destCanonicalPath) {
                        continue // Skip entries that try to escape
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(newFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun extractRar(rarFile: File, destinationDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            Junrar.extract(rarFile, destinationDir)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
