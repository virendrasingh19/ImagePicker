package com.positrarx.imagePicker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.positrarx.imagepicker.ImagePicker
import com.positrarx.imagepicker.ImagePicker2

class MainActivity : AppCompatActivity() {

    private lateinit var imagePicker: ImagePicker
    private lateinit var imagePicker2: ImagePicker2
    private lateinit var pickFromCamera: Button
    private lateinit var pickFormGallery: Button
    private lateinit var imageView: ImageView

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_PICKER = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setView()
        clickListener()
        pickImage()
        pickImageInBackgroundThread()
    }

    private fun clickListener() {
        pickFromCamera.setOnClickListener {
            openCamera()
        }

        pickFormGallery.setOnClickListener {
            //imagePicker.pickFromGallery()
            imagePicker2.pickFromGallery()
        }
    }

    private fun setView() {
        pickFromCamera = findViewById(R.id.button)
        pickFormGallery = findViewById(R.id.button2)
        imageView = findViewById(R.id.imageView)
    }

    private fun pickImage() {
        imagePicker =
            ImagePicker(registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val resultCode = result.resultCode
                val data = result.data
                imagePicker.handleResult(resultCode, data, this)
            })

        // Set the listener to handle the picked image URI
        imagePicker.setOnImagePickedListener(object : ImagePicker.OnImagePickedListener {
            override fun onImagePicked(uri: Uri?, bitmap: Bitmap?) {
                // Handle the picked image URI
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else if (uri != null) {
                    // Image was picked successfully
                    // Use the uri to display or process the image
                    imageView.setImageURI(uri)
                } else {
                    // Image picking was canceled or unsuccessful
                    // Handle accordingly
                }
            }
        })
    }

    private fun pickImageInBackgroundThread() {
        imagePicker2 =
            ImagePicker2(
                this,
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    val resultCode = result.resultCode
                    val data = result.data
                    imagePicker2.handleResult(resultCode, data)
                })

        // Set the listener to handle the picked image URI
        imagePicker2.setOnImagePickedListener(object : ImagePicker2.OnImagePickedListener {
            override fun onImagePicked(bitmap: Bitmap?) {
                // Handle the picked image URI
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }else {
                    // Image picking was canceled or unsuccessful
                    // Handle accordingly
                }
            }
        })
    }

    private fun openCamera() {
        // Check camera permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            // Permission already granted, start image picker
            startImagePicker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, start image picker
                startImagePicker()
            } else {
                // Camera permission denied, show a dialog or navigate to app settings
                showPermissionDeniedDialog()
            }
        }
    }

    private fun startImagePicker() {
        // Start image picker
        imagePicker2.pickFromCamera()
    }

    private fun showPermissionDeniedDialog() {
        // Show a dialog explaining why the permission is required and provide an option to navigate to app settings
        // You can customize this method as per your app's requirements
    }

}
