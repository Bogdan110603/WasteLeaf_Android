package com.wasteleaf.wasteleaf_android.home.user.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.wasteleaf.wasteleaf_android.util.Constants.GALLERY_REQUEST_CODE
import com.wasteleaf.wasteleaf_android.util.Constants.POST_IMAGE_HEIGHT
import com.wasteleaf.wasteleaf_android.util.Constants.POST_IMAGE_WIDTH
import com.wasteleaf.wasteleaf_android.util.Constants.p
import com.wasteleaf.wasteleaf_android.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class MakePostActivity : AppCompatActivity() {
    private lateinit var functions: FirebaseFunctions
    private val storage = FirebaseStorage.getInstance()
    private lateinit var auth: FirebaseAuth

    private lateinit var postInputText: EditText
    private lateinit var postInputImage: ImageView
    private lateinit var cancelPostButton: ImageView
    private lateinit var publishPostButton: ImageView
    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_post)

        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        postInputText = findViewById(R.id.postInputText)
        postInputImage = findViewById(R.id.postInputImage)
        cancelPostButton = findViewById(R.id.cancelPostButton)
        publishPostButton = findViewById(R.id.publishPostButton)

        publishPostButton.setOnClickListener {
            if (postInputText.text.toString() == "") {
                Toast.makeText(this, "Add some text to your post!", Toast.LENGTH_SHORT).show()
            } else {
                setLoadingScreenActive()

                val storageRef = storage.reference

                val randomKey = UUID.randomUUID().toString()
                val imagesRef = storageRef.child(randomKey + p)

                postInputImage.buildDrawingCache()
                val bitmap = (postInputImage.drawable as BitmapDrawable).bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data1 = baos.toByteArray()

                imagesRef.putBytes(data1).addOnFailureListener {
                    Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
                    setLoadingScreenActive(false)
                }.addOnSuccessListener {
                    imagesRef.downloadUrl.addOnCompleteListener {
                        val newPost = hashMapOf(
                            "text" to postInputText.text.toString(),
                            "imageID" to it.result.toString(),
                        )

                        functions.getHttpsCallable("addPost").call(newPost).continueWith {
                            onBackPressed()
                        }
                    }
                }

            }
        }

        cancelPostButton.setOnClickListener {
            onBackPressed()
        }

        functions = Firebase.functions
        auth = FirebaseAuth.getInstance()

        pickFromGallery()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    launchImageCrop(uri)
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                onBackPressed()
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                result.uri?.let { uri ->
                    setImage(uri)
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                onBackPressed()
            }
        }
    }

    private fun setImage(uri: Uri) {
        Glide.with(this).load(File(uri.path!!))
            .apply(RequestOptions().override(POST_IMAGE_WIDTH, POST_IMAGE_HEIGHT))
            .into(postInputImage)
    }

    private fun launchImageCrop(uri: Uri) {
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.OFF)
            .setAspectRatio(POST_IMAGE_WIDTH, POST_IMAGE_HEIGHT)
            .start(this)
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun setLoadingScreenActive(ok: Boolean = true) {
        if (ok) {
            loadingScreenLayout.visibility = View.VISIBLE

            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post(object : Runnable {
                override fun run() {
                    loadingCircle.rotation += 10
                    mainHandler.postDelayed(this, 1)
                }
            })
        } else {
            loadingScreenLayout.visibility = View.INVISIBLE
        }
    }
}