package com.wasteleaf.wasteleaf_android.home.maps.report

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.wasteleaf.wasteleaf_android.util.Constants
import com.wasteleaf.wasteleaf_android.util.Constants.ALL_CATEGORIES_ACTIVITY_REQUEST_CODE
import com.wasteleaf.wasteleaf_android.util.Constants.CAMERA_ACTIVITY_REQUEST_CODE
import com.wasteleaf.wasteleaf_android.util.Constants.CAMERA_PERMISSION_REQUEST_CODE
import com.wasteleaf.wasteleaf_android.util.Constants.getImageFile
import com.wasteleaf.wasteleaf_android.util.Constants.p
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.maps.MapsActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*

class ReportActivity : AppCompatActivity() {
    companion object {
        var category = ""
    }

    private lateinit var functions: FirebaseFunctions
    private val storage = FirebaseStorage.getInstance()

    private lateinit var imageView: ImageView
    private lateinit var takePicButton: Button
    private lateinit var deletePhotoButton: ImageView
    private lateinit var finishButton: Button
    private lateinit var categoriesGridButton: ImageView
    private lateinit var categoryReportText: TextView
    private lateinit var categoryReport: TextView
    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var currentImagePath: String

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        getExtras()
        checkCameraPermission()
        declareAllUIComponents()
        addLogicToAllUIComponents()
    }

    // Get and handle intent's extras - that can be a string which is the chosen category from MapActivity
    private fun getExtras() {
        val intentExtras = intent.extras
        category = intentExtras?.get("category") as String
    }

    // Check if the app can use the camera and if so start the camera intent, otherwise request permission
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            takePhoto()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    // Handle camera request results and start camera intent if granted or start MapActivity if not
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Toast.makeText(this, "No permission, no photo", Toast.LENGTH_SHORT).show()
                onBackPressed()
            }
        }
    }

    private fun declareAllUIComponents() {
        imageView = findViewById(R.id.imageView)
        takePicButton = findViewById(R.id.takePicButton)
        deletePhotoButton = findViewById(R.id.deletePhotoButton)
        finishButton = findViewById(R.id.finishButton)
        categoriesGridButton = findViewById(R.id.categoriesGridButton)
        categoryReportText = findViewById(R.id.categoryReportText)
        categoryReport = findViewById(R.id.categoryReport)
        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)
    }

    private fun addLogicToAllUIComponents() {
        // Show this text if category is already chosen
        if (category != "null" && category != "") {
            categoryReportText.visibility = VISIBLE
            categoryReport.text = category
        }

        // Take a new photo or message to delete the current one if there is a image already taken
        takePicButton.setOnClickListener {
            if (imageView.drawable == null) {
                takePhoto()
            } else {
                Toast.makeText(this, "Delete the current image first", Toast.LENGTH_SHORT).show()
            }
        }

        // Deletes the current photo
        deletePhotoButton.setOnClickListener {
            imageView.setImageResource(0)
        }

        // Start a new intent to AllCategoriesActivity which will return a certain category
        categoriesGridButton.setOnClickListener {
            val intent = Intent(this, AllCategoriesActivity::class.java)
            startActivityForResult(intent, ALL_CATEGORIES_ACTIVITY_REQUEST_CODE)
        }

        // Finish the whole activity. Handle some situations and record report in db if everything is right
        finishButton.setOnClickListener {
            if (imageView.drawable != null) {
                if (category != "null" && category != "") {
                    setLoadingScreenActive()
                    setButtonsInactive()

                    recordInDb(imageView)
                } else {
                    Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AllCategoriesActivity::class.java)
                    startActivityForResult(intent, ALL_CATEGORIES_ACTIVITY_REQUEST_CODE)
                }
            } else {
                Toast.makeText(this, "U have to add a photo", Toast.LENGTH_SHORT).show()
            }
        }

        functions = Firebase.functions
    }

    private fun takePhoto() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            var imageFile: File? = null
            try {
                val pair = getImageFile(this)
                imageFile = pair.first
                currentImagePath = pair.second
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (imageFile != null) {
                val imageUri = FileProvider.getUriForFile(
                    this,
                    "com.wasteleaf.wasteleaf_android.provider",
                    imageFile
                )

                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(cameraIntent, CAMERA_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    // Handle the result from camera intent and from AllCategories intent
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_ACTIVITY_REQUEST_CODE) {
                val f = File(currentImagePath)
                val uri = Uri.fromFile(f)

                launchImageCrop(uri)
            }
            if (requestCode == ALL_CATEGORIES_ACTIVITY_REQUEST_CODE) {
                if (data != null) {
                    category = data.extras?.get("chosenCategory").toString()

                    if (category != "null" && category != "") {
                        categoryReportText.visibility =
                            VISIBLE
                        categoryReport.text = category
                    }
                }
            }
            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                val result = CropImage.getActivityResult(data)
                result.uri?.let { uri ->
                    setImage(uri)
                }
            }
        }
    }

    // Record report in db
    private fun recordInDb(imageView: ImageView) {
        val storageRef = storage.reference

        val randomKey = UUID.randomUUID().toString()
        val imagesRef = storageRef.child(randomKey + p)

        imageView.isDrawingCacheEnabled = true
        imageView.buildDrawingCache()
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data1 = baos.toByteArray()

        imagesRef.putBytes(data1).addOnFailureListener {
            Toast.makeText(this, it.message.toString(), Toast.LENGTH_SHORT).show()
            setButtonsInactive(false)
            setLoadingScreenActive(false)

        }.addOnSuccessListener {
            imagesRef.downloadUrl.addOnCompleteListener {
                val newReport = hashMapOf(
                    Constants.LATITUDE to MapsActivity.yourLocationMarker.position.latitude,
                    Constants.LONGITUDE to MapsActivity.yourLocationMarker.position.longitude,
                    Constants.CATEGORY to category,
                    Constants.PHOTO_REF to it.result.toString(),
                    Constants.REPORTER to FirebaseAuth.getInstance().currentUser!!.uid
                )

                functions.getHttpsCallable("addReport").call(newReport).continueWith {
                    Toast.makeText(
                        this,
                        "You reported successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    onBackPressed()
                }
            }
        }
    }

    private fun launchImageCrop(uri: Uri) {
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.OFF)
            .setAspectRatio(Constants.REPORT_IMAGE_WIDTH, Constants.REPORT_IMAGE_HEIGHT)
            .start(this)
    }

    private fun setImage(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap,
            Constants.REPORT_IMAGE_WIDTH,
            Constants.REPORT_IMAGE_HEIGHT, false)
        imageView.setImageBitmap(scaledBitmap)
    }

    private fun setLoadingScreenActive(ok: Boolean = true) {
        if (ok) {
            loadingScreenLayout.visibility = VISIBLE

            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post(object : Runnable {
                override fun run() {
                    loadingCircle.rotation += 10
                    mainHandler.postDelayed(this, 1)
                }
            })
        } else {
            loadingScreenLayout.visibility = INVISIBLE
        }
    }

    private fun setButtonsInactive(ok: Boolean = true) {
        if (ok) {
            takePicButton.visibility = INVISIBLE
            deletePhotoButton.visibility = INVISIBLE
            categoriesGridButton.visibility = INVISIBLE
            finishButton.visibility = INVISIBLE
        } else {
            takePicButton.visibility = VISIBLE
            deletePhotoButton.visibility = VISIBLE
            categoriesGridButton.visibility = VISIBLE
            finishButton.visibility = VISIBLE
        }
    }
}