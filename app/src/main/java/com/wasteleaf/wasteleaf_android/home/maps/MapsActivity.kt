package com.wasteleaf.wasteleaf_android.home.maps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.ImageView
import android.widget.TableRow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.util.Constants
import com.wasteleaf.wasteleaf_android.util.Constants.LOCATION_PERMISSION_REQUEST_CODE
import com.wasteleaf.wasteleaf_android.util.Constants.bitmapFromVector
import com.wasteleaf.wasteleaf_android.util.Constants.calcMiddlePoint
import com.wasteleaf.wasteleaf_android.util.Constants.getClosestReport
import com.wasteleaf.wasteleaf_android.util.Constants.getReportFromArray
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.databinding.ActivityMapsBinding
import com.wasteleaf.wasteleaf_android.home.maps.report.ReportActivity
import com.wasteleaf.wasteleaf_android.home.maps.report.ReportDetailsActivity
import com.wasteleaf.wasteleaf_android.home.maps.report.UpdateReportActivity
import java.util.concurrent.TimeUnit

class MapsActivity : AppCompatActivity(), GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var goToIdMarker: Marker? = null
    private var goToId: String? = null
    private lateinit var reportList: ArrayList<*>
    private lateinit var yourLocation: LatLng
    private var closestReport: HashMap<*, *>? = null

    companion object {
        lateinit var reportListObj: ArrayList<*>
        lateinit var yourLocationMarker: Marker
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private lateinit var functions: FirebaseFunctions
    private lateinit var auth: FirebaseAuth

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var reportButton: Button
    private lateinit var categoriesRow: TableRow
    private lateinit var screenButton: ImageView
    private lateinit var organicShortcut: ImageView
    private lateinit var glassShortcut: ImageView
    private lateinit var paperShortcut: ImageView
    private lateinit var plasticShortcut: ImageView
    private lateinit var wasteNearbyLayout: ConstraintLayout
    private lateinit var closeWasteNearbyNotificationButton: ImageView
    private lateinit var showAllNotificationButton: ImageView
    private lateinit var wasteNearbyBigLayout: ConstraintLayout
    private lateinit var closeWasteNearbyBigNotificationButton: ImageView
    private lateinit var showLessNotificationButton: ImageView
    private lateinit var wasteNearbyImage: ImageView
    private lateinit var settingsButton: ImageView
    private lateinit var optionsButton: ImageView
    private lateinit var parentLayout: ConstraintLayout

    private lateinit var binding: ActivityMapsBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val extras = intent.extras
        if (extras != null) {
            val dataExtras = (extras.get("goToId") as String).split(Constants.DELIMITER)
            goToId = dataExtras[0]
            if (dataExtras.lastIndex != 0) {
                functions.getHttpsCallable("deleteNotification")
                    .call(hashMapOf("notificationID" to dataExtras[1]))
            }
        }

        checkLocationPermission()

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

        reportButton = findViewById(R.id.reportButton)
        categoriesRow = findViewById(R.id.categoriesRow)
        screenButton = findViewById(R.id.screenButton)
        organicShortcut = findViewById(R.id.organicShortcut)
        glassShortcut = findViewById(R.id.glassShortcut)
        paperShortcut = findViewById(R.id.paperShortcut)
        plasticShortcut = findViewById(R.id.plasticShortcut)
        wasteNearbyLayout = findViewById(R.id.wasteNearbyLayout)
        closeWasteNearbyNotificationButton = findViewById(R.id.closeNotificationButton)
        showAllNotificationButton = findViewById(R.id.showAllNotificationButton)
        wasteNearbyBigLayout = findViewById(R.id.wasteNearbyBigLayout)
        closeWasteNearbyBigNotificationButton = findViewById(R.id.closeWasteNearbyBigButton)
        showLessNotificationButton = findViewById(R.id.showLessNotificationButton)
        wasteNearbyImage = findViewById(R.id.wasteNearbyImage)
        settingsButton = findViewById(R.id.settingsButton)
        optionsButton = findViewById(R.id.optionsButton)
        parentLayout = findViewById(R.id.parentLayout)

        functions.getHttpsCallable("getReportsData").call().continueWith {
            reportList = (it.result.data as HashMap<*, *>)["reports"] as ArrayList<*>
            reportListObj = reportList

            tryToRunAddAllMarkersToMap()
        }

        // ReportButton tap - report with no category
        reportButton.setOnClickListener {
            reportWithCategory("null")
        }

        // ReportButton hold - show categories and the screenButton which is going to record
        // any taps on the screen in order to make categories to disappear
        reportButton.setOnLongClickListener {
            categoriesRow.visibility = VISIBLE
            screenButton.visibility = VISIBLE
            true
        }

        // Report with organic as the chosen category
        organicShortcut.setOnClickListener {
            reportWithCategory("organic")
        }

        // Report with glass as the chosen category
        glassShortcut.setOnClickListener {
            reportWithCategory("glass")
        }

        // Report with paper as the chosen category
        paperShortcut.setOnClickListener {
            reportWithCategory("paper")
        }

        // Report with plastic as the chosen category
        plasticShortcut.setOnClickListener {
            reportWithCategory("plastic")
        }

        // ScreenButton which is going to record any taps on the screen
        // in order to make categories and itself to disappear
        screenButton.setOnClickListener {
            categoriesRow.visibility = INVISIBLE
            screenButton.visibility = INVISIBLE
        }

//         Start UpdateReportActivity
        wasteNearbyLayout.setOnClickListener {
            val intent = Intent(this, UpdateReportActivity::class.java)
            intent.putExtra("reportId", closestReport?.get("id") as String)
            startActivity(intent)
        }

        wasteNearbyBigLayout.setOnClickListener {
            val intent = Intent(this, UpdateReportActivity::class.java)
            intent.putExtra("reportId", closestReport?.get("id") as String)
            startActivity(intent)
        }

        // Close WasteNearby alert
        closeWasteNearbyNotificationButton.setOnClickListener {
            wasteNearbyLayout.visibility = INVISIBLE
            setSettingsAndOptionsButtonTopToParent()
        }

        closeWasteNearbyBigNotificationButton.setOnClickListener {
            wasteNearbyBigLayout.visibility = INVISIBLE
            setSettingsAndOptionsButtonTopToParent()
        }

        showAllNotificationButton.setOnClickListener {
            wasteNearbyLayout.visibility = INVISIBLE
            wasteNearbyBigLayout.visibility = VISIBLE
            setSettingsAndOptionsButtonTopToWasteNearbyLayout(wasteNearbyBigLayout.id)

            Glide.with(this)
                .load(((closestReport!!["data"] as HashMap<*, *>)["photoRef"] as ArrayList<*>).last())
                .into(wasteNearbyImage)
        }

        showLessNotificationButton.setOnClickListener {
            wasteNearbyLayout.visibility = VISIBLE
            wasteNearbyBigLayout.visibility = INVISIBLE
            setSettingsAndOptionsButtonTopToWasteNearbyLayout(wasteNearbyLayout.id)
        }
    }

    // Check if the app can use the location and if so get user's coordinates, otherwise request permission
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkLocationPermission() {
        locationRequest = LocationRequest()
        locationRequest.interval = TimeUnit.SECONDS.toMillis(30)
        locationRequest.fastestInterval = TimeUnit.SECONDS.toMillis(5)
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLocation()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Handle location request results and get user's location if granted or close app
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                onStart()
                getLocation()
            } else {
                Toast.makeText(this, "No permission, no map", Toast.LENGTH_SHORT).show()
                onBackPressed()
            }
        }
    }

    // Retrieve user location
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.locationAvailability.addOnSuccessListener {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { pos ->
                if (pos != null) {
                    yourLocation = LatLng(pos.latitude, pos.longitude)
                    tryToRunAddAllMarkersToMap()
                } else {
                    Toast.makeText(
                        this,
                        "Location is not available! Open Google Maps beforehand",
                        Toast.LENGTH_SHORT
                    ).show()
                    onBackPressed()
                }
            }.addOnFailureListener { error ->
                Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSettingsAndOptionsButtonTopToParent() {
        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(parentLayout)
        mConstraintSet.clear(settingsButton.id, ConstraintSet.TOP)
        mConstraintSet.connect(
            settingsButton.id,
            ConstraintSet.TOP,
            parentLayout.id,
            ConstraintSet.TOP
        )
        mConstraintSet.clear(optionsButton.id, ConstraintSet.TOP)
        mConstraintSet.connect(
            optionsButton.id,
            ConstraintSet.TOP,
            parentLayout.id,
            ConstraintSet.TOP
        )
        mConstraintSet.applyTo(parentLayout)
    }

    private fun setSettingsAndOptionsButtonTopToWasteNearbyLayout(id: Int) {
        val mConstraintSet = ConstraintSet()
        mConstraintSet.clone(parentLayout)
        mConstraintSet.clear(settingsButton.id, ConstraintSet.TOP)
        mConstraintSet.connect(settingsButton.id, ConstraintSet.TOP, id, ConstraintSet.BOTTOM)
        mConstraintSet.clear(optionsButton.id, ConstraintSet.TOP)
        mConstraintSet.connect(optionsButton.id, ConstraintSet.TOP, id, ConstraintSet.BOTTOM)
        mConstraintSet.applyTo(parentLayout)
    }

    // Start ReportActivity passing the category as an extra string
    private fun reportWithCategory(category: String) {
        val intent = Intent(this, ReportActivity::class.java)
        intent.putExtra("category", category)
        startActivity(intent)

        categoriesRow.visibility = INVISIBLE
        screenButton.visibility = INVISIBLE
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setInfoWindowAdapter(CustomInfoWindowClass(this))
        mMap.setOnInfoWindowClickListener(this)

        tryToRunAddAllMarkersToMap()
    }

    private fun tryToRunAddAllMarkersToMap() {
        if (this::mMap.isInitialized && this::yourLocation.isInitialized && this::reportList.isInitialized) {
            addAllMarkersToMap()
        }
    }

    private fun addAllMarkersToMap() {
        for (i: Int in 0 until reportList.size) {
            val reportId = (reportList[i] as HashMap<*, *>)["id"] as String
            val reportData = (reportList[i] as HashMap<*, *>)["data"] as HashMap<*, *>
            var markerIcon = 0

            when (reportData["category"]) {
                Constants.PAPER -> markerIcon = R.drawable.paper_mica
                Constants.ORGANIC -> markerIcon = R.drawable.organic_mica
                Constants.GLASS -> markerIcon = R.drawable.glass_mica
                Constants.PLASTIC -> markerIcon = R.drawable.plastic_mica
                Constants.DUMPSTER -> markerIcon = R.drawable.tomberon_mic
                Constants.TIRE -> markerIcon = R.drawable.cauciuc_mic
                Constants.TEXTILE -> markerIcon = R.drawable.textil_mic
                Constants.METAL -> markerIcon = R.drawable.metal_mic
                Constants.EWASTE -> markerIcon = R.drawable.ewaste_mic
            }

            val reportLocation =
                LatLng(reportData["lat"] as Double, reportData["lng"] as Double)
            val currentMarker = mMap.addMarker(
                MarkerOptions()
                    .position(reportLocation)
                    .title(reportId)
                    .icon(bitmapFromVector(this, markerIcon))
            )

            if (reportId == goToId) {
                goToIdMarker = currentMarker
            }
        }

        yourLocationMarker = mMap.addMarker(
            MarkerOptions()
                .position(yourLocation)
                .title("yourMarker")
        )!!

        closestReport = getClosestReport(reportList, yourLocation)

        if (closestReport != null) {
            wasteNearbyLayout.visibility = VISIBLE
            setSettingsAndOptionsButtonTopToWasteNearbyLayout(wasteNearbyLayout.id)
        }

        if (goToId != null) {
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    goToIdMarker!!.position, 16f
                )
            )
            goToIdMarker!!.showInfoWindow()

            wasteNearbyLayout.visibility = INVISIBLE
            setSettingsAndOptionsButtonTopToParent()
        } else {
            if (closestReport == null) {
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        yourLocation,
                        Constants.DEFAULT_ZOOM_USER_LOCATION
                    )
                )
            } else {
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        calcMiddlePoint(
                            yourLocation,
                            LatLng(
                                (closestReport!!["data"] as HashMap<*, *>)["lat"] as Double,
                                (closestReport!!["data"] as HashMap<*, *>)["lng"] as Double
                            )
                        ), 16f
                    )
                )
            }
        }

        deactivateLoadingScreen()
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        reportButton.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    override fun onInfoWindowClick(marker: Marker) {
        val report = getReportFromArray(reportList, marker)
        if (report != null) {
            val intent = Intent(this, ReportDetailsActivity::class.java)
            intent.putExtra("reportId", report["id"] as String)
            startActivity(intent)
        }
    }
}