package com.positrarx.imagepicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*


private const val TAG = "ImageCompressor"

/**
 * Compresses the image located at the given URI and saves the compressed image to a file.
 *
 * @param originalBitmap   The Bitmap of the image to compress.
 * @param file    The file to save the compressed image to.
 * @return True if the image was successfully compressed and saved, false otherwise.
 */
suspend fun compressImage(originalBitmap: Bitmap, file: File): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val options = calculateOptions(originalBitmap)
            val scaledBitmap = createScaledBitmap(originalBitmap, options)
            val rotation = getRotationFromExif(scaledBitmap)
            val rotatedBitmap = if (rotation != 0) {
                rotateBitmap(scaledBitmap, rotation)
            } else {
                originalBitmap
            }
            saveBitmapToFile(rotatedBitmap, file)
            true
        } catch (e: IOException) {
            e.message?.let { Log.d(TAG, it) }
            false
        }
    }
}

private fun calculateOptions(bitmap: Bitmap): BitmapFactory.Options {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        inSampleSize = 1
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().size, this)
    }
    return options
}

private fun createScaledBitmap(bitmap: Bitmap, options: BitmapFactory.Options): Bitmap {
    val actualHeight = options.outHeight
    val actualWidth = options.outWidth

    val maxHeight = 816.0f
    val maxWidth = 612.0f
    var imgRatio = actualWidth.toFloat() / actualHeight
    val maxRatio = maxWidth / maxHeight

    if (actualHeight > maxHeight || actualWidth > maxWidth) {
        imgRatio = if (imgRatio < maxRatio) {
            maxHeight / actualHeight
        } else {
            maxWidth / actualWidth
        }
    }
    val requiredWidth = (actualWidth * imgRatio).toInt()
    val requiredHeight = (actualHeight * imgRatio).toInt()

    return Bitmap.createScaledBitmap(bitmap, requiredWidth, requiredHeight, true)
}

private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        else -> return bitmap // No rotation needed
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Utility function to save the bitmap to a file
private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
    FileOutputStream(file).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 99, outputStream)
        outputStream.flush()
        outputStream.close()
    }
}

/**
 * Retrieves the rotation information of the image from its EXIF data.
 *
 * @param uri     The URI of the image.
 * @return The rotation in degrees.
 */
private fun getRotationFromExif(context: Context, uri: Uri): Int {
    var inputStream: InputStream? = null
    try {
        inputStream = context.contentResolver.openInputStream(uri)
        val exifInterface = ExifInterface(inputStream!!)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> return 90
            ExifInterface.ORIENTATION_ROTATE_180 -> return 180
            ExifInterface.ORIENTATION_ROTATE_270 -> return 270
        }
    } catch (e: IOException) {
        e.message?.let { Log.d(TAG, it) }
    } finally {
        inputStream?.close()
    }
    return 0
}

private fun getRotationFromExif(bitmap: Bitmap): Int {
    var inputStream: InputStream? = null
    try {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        inputStream = ByteArrayInputStream(outputStream.toByteArray())

        val exifInterface = ExifInterface(inputStream)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: IOException) {
        e.message?.let { Log.d(TAG, it) }
    } finally {
        inputStream?.close()
    }

    return 0
}

/**
 * Compresses the image located at the given URI and saves the compressed image to a file.
 *
 * @param context The context.
 * @param uri     The URI of the image to compress.
 * @param file    The file to save the compressed image to.
 * @return True if the image was successfully compressed and saved, false otherwise.
 */
suspend fun compressImage(context: Context, uri: Uri, file: File): Pair<Boolean, Long> =
    withContext(Dispatchers.IO) {
        var success = false
        var elapsedTime: Long = 0

        try {
            val startTime = System.currentTimeMillis()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream)

                // Rotate the image if needed
                val rotation = getRotationFromExif(context, uri)
                val rotatedBitmap = if (rotation != 0) {
                    rotateBitmap(originalBitmap, rotation)
                } else {
                    originalBitmap
                }
                saveBitmapToFile(rotatedBitmap, file)

                success = true
            }

            val endTime = System.currentTimeMillis()
            elapsedTime = endTime - startTime
        } catch (e: IOException) {
            e.message?.let { Log.d(TAG, it) }
        }

        Pair(success, elapsedTime)
    }


