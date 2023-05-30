package com.positrarx.imagepicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ImagePicker2(
    private val context: Context, private val launcher: ActivityResultLauncher<Intent>
) {

    interface OnImagePickedListener {
        fun onImagePicked(bitmap: Bitmap?)
    }

    private var listener: OnImagePickedListener? = null

    fun setOnImagePickedListener(listener: OnImagePickedListener?) {
        this.listener = listener
    }

    fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        launchImagePicker(intent)
    }

    fun pickFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        launchImagePicker(intent)
    }

    private fun launchImagePicker(intent: Intent) {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                launcher.launch(intent)
            }
        }
    }

    fun handleResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                val extras: Bundle? = data!!.extras
                if (extras != null) {
                    handleBitmap(extras)
                } else {
                    handleUri(data)
                }
            }
            else -> {
                listener?.onImagePicked(null)
            }
        }
    }

    private fun handleUri(data: Intent) {
        data.data?.let {
            compressAndProcessImage(it, null)
        } ?: listener?.onImagePicked(null)
    }

    private fun handleBitmap(extras: Bundle) {
        val imageBitmap = extras.get("data") as Bitmap?
        if (imageBitmap != null) {
            compressAndProcessImage(null, imageBitmap)
        } else {
            listener?.onImagePicked(null)
        }
    }

    private fun compressAndProcessImage(uri: Uri?, bitmap: Bitmap?) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val compressedFile = createCompressedImageFile()
                // Compress the image
                val compressionResult: Boolean = if (uri != null) {
                    val (success, elapsedTime) = compressImage(context, uri, compressedFile)
                    success
                } else {
                    compressImage(bitmap!!, compressedFile)
                }

                withContext(Dispatchers.Main) {
                    if (compressionResult) {
                        val compressedBitmap =
                            BitmapFactory.decodeFile(compressedFile.absolutePath)
                        listener?.onImagePicked(compressedBitmap)
                    } else {
                        listener?.onImagePicked(null)
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    listener?.onImagePicked(null)
                }
            }
        }
    }

    private fun createCompressedImageFile(): File {
        val storageDir = context.getExternalFilesDir(null)
        return File.createTempFile(
            "compressed_image",
            ".jpg",
            storageDir
        )
    }

}
