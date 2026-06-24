package com.sharjeel.fileviewerapp
import com.sharjeel.fileviewerapp.util.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileUtilsTest {

    @Test
    fun formatFileSize_isCorrect() {
        // 0 Bytes check
        assertEquals("0 B", FileUtils.formatFileSize(0))

        // KB check
        assertEquals("1.00 KB", FileUtils.formatFileSize(1024))

        // MB check
        assertEquals("1.50 MB", FileUtils.formatFileSize((1024 * 1024 * 1.5).toLong()))

        // GB check
        assertEquals("2.00 GB", FileUtils.formatFileSize(1024L * 1024 * 1024 * 2))
    }

    @Test
    fun isVideoFile_recognizesExtensions() {
        assertTrue(FileUtils.isVideoFile("movie.mp4"))
        assertTrue(FileUtils.isVideoFile("clip.mkv"))
        assertFalse(FileUtils.isVideoFile("image.jpg"))
        assertFalse(FileUtils.isVideoFile("document.pdf"))
    }

    @Test
    fun isImageFile_recognizesExtensions() {
        assertTrue(FileUtils.isImageFile("photo.jpg"))
        assertTrue(FileUtils.isImageFile("graphic.png"))
        assertFalse(FileUtils.isImageFile("video.mp4"))
    }

    @Test
    fun getMimeType_returnsCorrectTypes() {
        assertEquals("application/pdf", FileUtils.getMimeType("test.pdf"))
        assertEquals("image/jpeg", FileUtils.getMimeType("test.jpg"))
        assertEquals("video/mp4", FileUtils.getMimeType("test.mp4"))
        assertEquals("text/plain", FileUtils.getMimeType("test.txt"))
    }
}