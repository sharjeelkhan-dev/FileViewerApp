package com.sharjeel.fileviewerapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

object ThumbnailUtils {

    fun getApkIcon(context: Context, apkPath: String): Bitmap? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(apkPath, 0)
            packageInfo?.applicationInfo?.let { appInfo ->
                appInfo.sourceDir = apkPath
                appInfo.publicSourceDir = apkPath
                val icon = appInfo.loadIcon(packageManager)
                val bitmap = Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                icon.setBounds(0, 0, canvas.width, canvas.height)
                icon.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getPdfThumbnail(pdfPath: String): Bitmap? {
        return try {
            val file = File(pdfPath)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
