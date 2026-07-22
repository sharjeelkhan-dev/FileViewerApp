package com.sharjeel.fileviewerapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.min
import androidx.core.graphics.createBitmap

object ThumbnailUtils {

    fun getApkIcon(context: Context, apkPath: String): Bitmap? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0) ?: return null
            val appInfo = packageInfo.applicationInfo ?: return null

            appInfo.sourceDir = apkPath
            appInfo.publicSourceDir = apkPath

            val icon = appInfo.loadIcon(packageManager) ?: return null
            val width = if (icon.intrinsicWidth > 0) min(icon.intrinsicWidth, 144) else 96
            val height = if (icon.intrinsicHeight > 0) min(icon.intrinsicHeight, 144) else 96

            // Using ARGB_8888 for clean icon transparency, but with bounded dimensions
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            icon.setBounds(0, 0, canvas.width, canvas.height)
            icon.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun getPdfThumbnail(pdfPath: String, maxDimension: Int = 256): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        return try {
            val file = File(pdfPath)
            if (!file.exists() || file.length() == 0L) return null

            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            if (renderer.pageCount == 0) return null
            page = renderer.openPage(0)
            val scale = min(
                maxDimension.toFloat() / page.width,
                maxDimension.toFloat() / page.height
            )
            val renderWidth = (page.width * scale).toInt().coerceAtLeast(1)
            val renderHeight = (page.height * scale).toInt().coerceAtLeast(1)

            // RGB_565 cuts bitmap RAM usage by 50% compared to ARGB_8888
            val bitmap = createBitmap(renderWidth, renderHeight, Bitmap.Config.RGB_565)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            try {
                page?.close()
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }
}