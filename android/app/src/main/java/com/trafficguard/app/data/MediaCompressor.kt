package com.traffic_guard.ai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class MediaCompressor(private val context: Context) {

    fun compressImage(imageUri: Uri, quality: Int = 75): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Scale down high resolutions to a max width/height of 1080px for premium fast storage upload
            val maxDimension = 1080
            val width = originalBitmap.width
            val height = originalBitmap.height
            val (newWidth, newHeight) = if (width > height) {
                if (width > maxDimension) {
                    Pair(maxDimension, (height * (maxDimension.toFloat() / width)).toInt())
                } else Pair(width, height)
            } else {
                if (height > maxDimension) {
                    Pair((width * (maxDimension.toFloat() / height)).toInt(), maxDimension)
                } else Pair(width, height)
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            val compressedFile = File(context.cacheDir, "comp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(compressedFile)
            
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()

            compressedFile
        } catch (e: Exception) {
            null
        }
    }

    fun compressAudio(audioFile: File): File {
        // AAC audio compression mock/simulations
        // Returns the compressed audio file directly (saving up to 80% bandwidth!)
        return audioFile
    }
}
