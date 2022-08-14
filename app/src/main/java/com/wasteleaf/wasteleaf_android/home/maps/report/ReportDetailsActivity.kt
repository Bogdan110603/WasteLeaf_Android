package com.wasteleaf.wasteleaf_android.home.maps.report

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.util.Constants
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity
import org.ocpsoft.prettytime.PrettyTime
import java.util.*
import kotlin.math.abs


class ReportDetailsActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var functions: FirebaseFunctions

    private var currentIndex: Int = 0
    private lateinit var gestureDetector: GestureDetector

    private var x1: Float = 0.0f
    private var x2: Float = 0.0f

    private lateinit var report: HashMap<*, *>

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var indexPhotoText: TextView
    private lateinit var reporterBackground: ImageView
    private lateinit var reporterNicknameText: TextView
    private lateinit var reporterProfilePicture: ImageView
    private lateinit var routeBackground: ImageView
    private lateinit var imageWaste: ImageView
    private lateinit var category: ImageView
    private lateinit var timestampText: TextView
    private lateinit var closeDetailsButton: ImageView
    private lateinit var reportDetailsLayout: ConstraintLayout
    private lateinit var openDetailsButton: ImageView
    private lateinit var smallReportDetailsLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_details)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        val extras: Bundle? = intent.extras
        val reportId = extras?.getSerializable("reportId") as String

        functions = Firebase.functions

        indexPhotoText = findViewById(R.id.indexPhotoText)
        reporterBackground = findViewById(R.id.reporterBackground)
        reporterNicknameText = findViewById(R.id.reporterNicknameText)
        reporterProfilePicture = findViewById(R.id.reporterProfilePicture)
        routeBackground = findViewById(R.id.routeBackground)
        imageWaste = findViewById(R.id.imageWaste)
        category = findViewById(R.id.categoryImage)
        timestampText = findViewById(R.id.timestampText)
        closeDetailsButton = findViewById(R.id.closeDetailsLayout)
        reportDetailsLayout = findViewById(R.id.reportDetailsLayout)
        openDetailsButton = findViewById(R.id.openReportLayout)
        smallReportDetailsLayout = findViewById(R.id.smallUpdateLayout)

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

            (report["photoRef"] as ArrayList<*>).reverse()

            displayImage()

            reporterNicknameText.text = user["nickname"] as String
            Glide.with(this).load(user["profilePictureID"]).circleCrop().into(reporterProfilePicture)

            val timestamp = Date((report["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong() * 1000)
            val prettyTime = PrettyTime(Locale.US)
            val ago = prettyTime.format(timestamp)
            timestampText.text = ago

            when(report["category"] as String) {
                Constants.ORGANIC -> category.setImageResource(R.drawable.organic_nou)
                Constants.GLASS -> category.setImageResource(R.drawable.glass_nou)
                Constants.PAPER -> category.setImageResource(R.drawable.paper_nou)
                Constants.PLASTIC -> category.setImageResource(R.drawable.plastic_nou)
                Constants.TEXTILE -> category.setImageResource(R.drawable.textil_bun_nou)
                Constants.METAL -> category.setImageResource(R.drawable.metal_bun_nou)
                Constants.EWASTE -> category.setImageResource(R.drawable.ewaste_bun_nou)
                Constants.TIRE -> category.setImageResource(R.drawable.cauciuc_bun_nou)
                Constants.DUMPSTER -> category.setImageResource(R.drawable.tomberon_bun_nou)
            }

            deactivateLoadingScreen()
        }

        reporterBackground.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("userId", report["reporter"] as String)
            startActivity(intent)
        }

        routeBackground.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://maps.google.com/maps?daddr=${report["lat"]},${report["lng"]}")
            )
            startActivity(intent)
        }

        // Close details layout
        closeDetailsButton.setOnClickListener {
            reportDetailsLayout.visibility = View.INVISIBLE
            smallReportDetailsLayout.visibility = View.VISIBLE
        }

        // Open details layout
        openDetailsButton.setOnClickListener {
            reportDetailsLayout.visibility = View.VISIBLE
            smallReportDetailsLayout.visibility = View.INVISIBLE
        }

        gestureDetector = GestureDetector(this, this)
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        openDetailsButton.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event?.action) {
            0 -> x1 = event.x

            1 -> {
                x2 = event.x

                val valueX = x2 - x1

                if (abs(valueX) > Constants.MIN_DISTANCE_FOR_SWIPE) {
                    if (x2 < x1) {
                        if (currentIndex < (report["photoRef"] as ArrayList<*>).size - 1)
                            currentIndex++
                            displayImage()
                    } else {
                        if (currentIndex > 0) {
                            currentIndex--
                            displayImage()
                        }
                    }
                }
            }
        }

        return super.onTouchEvent(event)
    }

    // Display the image with the currentIndex
    private fun displayImage() {
        Glide.with(this).load((report["photoRef"] as ArrayList<*>)[currentIndex]).into(imageWaste)

        indexPhotoText.text = "${currentIndex + 1}/${(report["photoRef"] as ArrayList<*>).size}"
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent?) {

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {

    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }
}