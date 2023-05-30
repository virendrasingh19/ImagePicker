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

class ImagePicker(private val launcher: ActivityResultLauncher<Intent>) {

    interface OnImagePickedListener {
        fun onImagePicked(uri: Uri?, bitmap: Bitmap?)
    }

    private var listener: OnImagePickedListener? = null

    fun setOnImagePickedListener(listener: OnImagePickedListener?) {
        this.listener = listener
    }

    fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        launcher.launch(intent)
    }

    fun pickFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE)
        launcher.launch(intent)
    }

    fun handleResult(
        resultCode: Int,
        data: Intent?,
        context: Context
    ) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                val extras: Bundle? = data!!.extras
                if (extras != null) {
                    val imageUri = data.data
                    val filePath = getImageFilePathFromUri(context, imageUri)

                    // Load the full-resolution image from the file
                    val imageBitmap = BitmapFactory.decodeFile(filePath)

                    // Set the image bitmap to the ImageView
                    listener?.onImagePicked(null, imageBitmap)
                } else {
                    data.data?.let { uri -> listener?.onImagePicked(uri, null) }
                        ?: listener?.onImagePicked(null, null)
                }
            }
            else -> {
                listener?.onImagePicked(null, null)
            }
        }
    }

    private fun getImageFilePathFromUri(context: Context, uri: Uri?): String? {
        uri?.let { imageUri ->
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(imageUri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    return it.getString(columnIndex)
                }
            }
        }
        return null
    }

}
