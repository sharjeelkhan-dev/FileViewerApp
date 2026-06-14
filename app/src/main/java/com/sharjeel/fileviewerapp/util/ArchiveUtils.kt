package com.sharjeel.fileviewerapp.util

import com.github.junrar.Junrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ArchiveUtils {

    suspend fun extractZip(zipFile: File, destinationDir: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val newFile = File(destinationDir, entry.name)
                    
                    // Security check: Zip Slip vulnerability
                    if (!newFile.canonicalPath.startsWith(destinationDir.canonicalPath + File.separator)) {
                        throw Exception("Security Error: Archive entry is outside of destination directory")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
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
