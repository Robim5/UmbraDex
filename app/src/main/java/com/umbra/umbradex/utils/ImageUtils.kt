package com.umbra.umbradex.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import java.io.OutputStream

object ImageUtils {

    // Função que guarda o Bitmap na Galeria
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String) {
        val fos: OutputStream?
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UmbraDex")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if (uri != null) {
                fos = resolver.openOutputStream(uri)
                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                Toast.makeText(context, "Cartão da Equipa guardado na Galeria! 📸", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao guardar imagem: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper para capturar uma View (usado como fallback)
    fun captureView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}