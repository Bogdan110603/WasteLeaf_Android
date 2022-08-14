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
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.wasteleaf.wasteleaf_android.util.Constants
import com.wasteleaf.wasteleaf_android.util.Constants.CAMERA_UPDATE_ACTIVITY_REQUEST_CODE
import com.wasteleaf.wasteleaf_android.util.Constants.CAMERA_UPDATE_PERMISSION_REQUEST_CODE
import com.wasteleaf.wasteleaf_android.util.Constants.getImageFile
import com.wasteleaf.wasteleaf_android.util.Constants.p
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity
import org.ocpsoft.prettytime.PrettyTime
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class UpdateReportActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private var currentIndex: Int = 0
    private lateinit var gestureDetector: GestureDetector

    private lateinit var functions: FirebaseFunctions
    private lateinit var auth: FirebaseAuth
    private val storage = FirebaseStorage.getInstance()
    private lateinit var reportId: String
    private lateinit var report: HashMap<*, *>
    private var state: String = "noState"
    private lateinit var currentImagePath: String

    private lateinit var smallUpdateLayout: ConstraintLayout
    private lateinit var openReportLayout: ImageView
    private lateinit var updateReportLayout: ConstraintLayout
    private lateinit var closeUpdateReportLayout: ImageView
    private lateinit var wasteImage: ImageView
    private lateinit var updateTimeAgoText: TextView
    private lateinit var updateReporterNicknameText: TextView
    private lateinit var updateReporterProfilePicture: ImageView
    private lateinit var updateCategoryImage: ImageView
    private lateinit var stillThereButton: ImageView
    private lateinit var allCleanButton: ImageView
    private lateinit var stillThereBackground: ImageView
    private lateinit var allCleanBackground: ImageView
    private lateinit var updateButton: ImageView
    private lateinit var updateReporterBackground: ImageView
    private lateinit var updateRouteBackground: ImageView
    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_report)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        val extras: Bundle? = intent.extras
        reportId = extras?.getSerializable("reportId") as String

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        smallUpdateLayout = findViewById(R.id.smallUpdateLayout)
        openReportLayout = findViewById(R.id.openReportLayout)
        updateReportLayout = findViewById(R.id.updateReportLayout)
        closeUpdateReportLayout = findViewById(R.id.closeUpdateReportLayout)
        wasteImage = findViewById(R.id.wasteImage)
        updateTimeAgoText = findViewById(R.id.updateTimeAgoText)
        updateReporterBackground = findViewById(R.id.updateReporterBackground)
        updateRouteBackground = findViewById(R.id.updateRouteBackground)
        updateReporterNicknameText = findViewById(R.id.updateReporterNicknameText)
        updateReporterProfilePicture = findViewById(R.id.updateReporterProfilePicture)
        updateCategoryImage = findViewById(R.id.updateCategoryImage)
        stillThereButton = findViewById(R.id.stillThereButton)
        allCleanButton = findViewById(R.id.allCleanButton)
        stillThereBackground = findViewById(R.id.stillThereBackground)
        allCleanBackground = findViewById(R.id.allCleanBackground)
        updateButton = findViewById(R.id.updateButton)
        updateReporterBackground = findViewById(R.id.updateReporterBackground)
        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        // START ANIMATING THE LOADING CIRCLE
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                loadingCircle.rotation += 10
                mainHandler.postDelayed(this, 1)
            }
        })

        functions.getHttpsCallable("getReportData").call(hashMapOf("reportId" to reportId)).continueWith {
            report = (it.result.data as HashMap<*, *>)["report"] as HashMap<*, *>
            val user = (it.result.data as HashMap<*, *>)["user"] as HashMap<*, *>

            currentIndex = (report["photoRef"] as ArrayList<*>).size - 1

            updateReporterNicknameText.text = user["nickname"] as String
            Glide.with(this).load(user["profilePictureID"]).circleCrop().into(updateReporterProfilePicture)

            Glide.with(this).load((report["photoRef"] as ArrayList<*>).last()).into(wasteImage)

            val timestamp = Date((report["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong() * 1000)
            val prettyTime = PrettyTime(Locale.getDefault())
            val ago = prettyTime.format(timestamp)
            updateTimeAgoText.text = ago

            when(report["category"] as String) {
                Constants.ORGANIC -> updateCategoryImage.setImageResource(R.drawable.organic_nou)
                Constants.GLASS -> updateCategoryImage.setImageResource(R.drawable.glass_nou)
                Constants.PAPER -> updateCategoryImage.setImageResource(R.drawable.paper_nou)
                Constants.PLASTIC -> updateCategoryImage.setImageResource(R.drawable.plastic_nou)
                Constants.TEXTILE -> updateCategoryImage.setImageResource(R.drawable.textil_bun_nou)
                Constants.METAL -> updateCategoryImage.setImageResource(R.drawable.metal_bun_nou)
                Constants.EWASTE -> updateCategoryImage.setImageResource(R.drawable.ewaste_bun_nou)
                Constants.TIRE -> updateCategoryImage.setImageResource(R.drawable.cauciuc_bun_nou)
                Constants.DUMPSTER -> updateCategoryImage.setImageResource(R.drawable.tomberon_bun_nou)
            }

            deactivateLoadingScreen()
        }

        updateReporterBackground.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("userId", report["reporter"] as String)
            startActivity(intent)
        }

        updateRouteBackground.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?daddr=${report["lat"]},${report["lng"]}")
            )
            startActivity(intent)
        }

        // Set state and textSize of those buttons according to button
        stillThereBackground.setOnClickListener {
            state = "stillThere"
            stillThereButton.scaleX = 1.2f
            stillThereButton.scaleY = 1.2f
            allCleanButton.scaleX = 1f
            allCleanButton.scaleY = 1f

            stillThereBackground.scaleX = 1.2f
            stillThereBackground.scaleY = 1.2f
            allCleanBackground.scaleX = 1f
            allCleanBackground.scaleY = 1f
        }

        // Set state and textSize of those buttons according to button
        allCleanBackground.setOnClickListener {
            state = "allClean"
            allCleanButton.scaleX = 1.2f
            allCleanButton.scaleY = 1.2f
            stillThereButton.scaleX = 1f
            stillThereButton.scaleY = 1f

            stillThereBackground.scaleX = 1f
            stillThereBackground.scaleY = 1f
            allCleanBackground.scaleX = 1.2f
            allCleanBackground.scaleY = 1.2f
        }

        openReportLayout.setOnClickListener {
            smallUpdateLayout.visibility = INVISIBLE
            updateReportLayout.visibility = VISIBLE
        }

        closeUpdateReportLayout.setOnClickListener {
            smallUpdateLayout.visibility = VISIBLE
            updateReportLayout.visibility = INVISIBLE
        }

        // Save the new changes
        updateButton.setOnClickListener {
            Log.d("CEVA", state)
            when (state) {
                "stillThere" -> {
                    takePhoto()

                    activateLoadingScreen()
                }
                "allClean" -> {
                    activateLoadingScreen()

                    for (photoRef in report["photoRef"] as ArrayList<*>) {
                        storage.getReferenceFromUrl(photoRef as String).delete()
                    }

                    val clearReport = hashMapOf(
                        "id" to reportId
                    )
                    functions.getHttpsCallable("deleteReport").call(clearReport).continueWith {
                        onBackPressed()
                    }
                }
                else -> Toast.makeText(this, "Select a state!", Toast.LENGTH_SHORT).show()
            }
        }

        gestureDetector = GestureDetector(this)
    }

    private fun recordInDb() {
        val storageRef = storage.reference

        val randomKey = UUID.randomUUID().toString()
        val imagesRef = storageRef.child(randomKey + p)

        wasteImage.isDrawingCacheEnabled = true
        wasteImage.buildDrawingCache()
        val bitmap = (wasteImage.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data1 = baos.toByteArray()

        imagesRef.putBytes(data1).addOnSuccessListener {
            imagesRef.downloadUrl.addOnCompleteListener {
                stillThereUpdate(it.result.toString())
            }
        }
    }

    private fun stillThereUpdate(newPhotoURL: String) {
        val updateReport = hashMapOf(
            "id" to reportId,
            "reporterId" to report["reporter"] as String,
            "photoRef" to newPhotoURL
        )

        functions.getHttpsCallable("updateReport").call(updateReport).continueWith {
            onBackPressed()
        }
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        openReportLayout.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    // Handle camera request results and start camera intent if granted or do noting otherwise
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_UPDATE_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Toast.makeText(this, "No permission, no photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun takePhoto() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_UPDATE_PERMISSION_REQUEST_CODE
            )
            return
        }

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
                startActivityForResult(cameraIntent, CAMERA_UPDATE_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    // Handle the result from camera intent
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_UPDATE_ACTIVITY_REQUEST_CODE) {
                val f = File(currentImagePath)
                val uri = Uri.fromFile(f)

                launchImageCrop(uri)
            }
            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                val result = CropImage.getActivityResult(data)
                result.uri?.let { uri ->
                    setImage(uri)
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
        wasteImage.setImageBitmap(scaledBitmap)

        recordInDb()
    }

    private fun activateLoadingScreen() {
        loadingScreenLayout.visibility = VISIBLE

        openReportLayout.visibility = INVISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)
    }

    // TODO: Swiping to go through images (just like in ReportDetailsActivity)
    //  doesn't work here. Need to find out what is wrong with GestureDetector

    // Override this method to recognize touch event
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        }
        else {
            super.onTouchEvent(event)
        }
    }

    // All the below methods are GestureDetector.OnGestureListener members
    // Except onFling, all must "return false" if Boolean return type
    // and "return" if no return type
    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent?) {
        return
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
        return
    }

    // Display the image with the currentIndex
    private fun displayImage() {
        Glide.with(this).load((report["photoRef"] as ArrayList<*>)[currentIndex]).into(wasteImage)
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        try {
            val diffX = e2.x - e1.x
            if (abs(diffX) > Constants.MIN_DISTANCE_FOR_SWIPE) {
                if (diffX > 0) {
                    currentIndex--
                    currentIndex = max(0, currentIndex)

                    displayImage()
                }
                else {
                    currentIndex++
                    currentIndex = min((report["photoRef"] as ArrayList<*>).size - 1, currentIndex)

                    displayImage()
                }
            }
        }
        catch (exception: Exception) {
            exception.printStackTrace()
        }
        return true
    }
}