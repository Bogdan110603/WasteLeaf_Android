package com.wasteleaf.wasteleaf_android.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.sqrt

object Constants {
    // 555 m
    private const val MIN_DISTANCE = 0.005 // min dist si la locationUpdatesBroadcastReceiver
    const val LOCATION_PERMISSION_REQUEST_CODE = 5

    var SCREEN_WIDTH: Int = 0
    var SCREEN_HEIGHT: Int = 0

    const val PROFILE_PICTURE_WIDTH = 480
    const val PROFILE_PICTURE_HEIGHT = 480
    const val DEFAULT_URL = "https://firebasestorage.googleapis.com/v0/b/wasteleaf-web-android-64a33.appspot.com/o/default_profile_image.png?alt=media&token=a9740585-b3ec-417f-8fb7-1932fe1c5db8"

    const val POST_IMAGE_WIDTH = 1080
    const val POST_IMAGE_HEIGHT = 1350
    const val POST_IMAGE_RATIO = POST_IMAGE_WIDTH.toFloat() / POST_IMAGE_HEIGHT.toFloat()

    const val REPORT_IMAGE_WIDTH = 720
    const val REPORT_IMAGE_HEIGHT = 1080

    const val ORGANIC = "organic"
    const val GLASS = "glass"
    const val PAPER = "paper"
    const val PLASTIC = "plastic"
    const val TEXTILE = "textile"
    const val METAL = "metal"
    const val EWASTE = "e-waste"
    const val TIRE = "tire"
    const val DUMPSTER = "dumpster"

    const val LATITUDE = "lat"
    const val LONGITUDE = "lng"
    const val CATEGORY = "category"
    const val REPORTER = "reporter"
    const val PHOTO_REF = "photoRef"

    const val DEFAULT_ZOOM_USER_LOCATION = 15f

    const val MIN_DISTANCE_FOR_SWIPE = 150

    const val newReportNotificationID = "0"
    const val newMessageNotificationID = "1"
    const val newFriendRequestNotificationID = "2"
    const val friendRequestAcceptedNotificationID = "3"
    const val friendRequestDeclinedNotificationID = "4"
    const val newLikeNotificationID = "5"
    const val newCommentNotificationID = "6"
    const val newPostNotificationID = "7"

    const val CHANNEL_ID = "channelID1"
    const val CHANNEL_NAME = "channelName"

    const val BIG_NUMBER = 100000000

    const val p = ".png"

    const val THRESHOLD_USERNAME_BACKGROUND = 180

    const val GALLERY_REQUEST_CODE = 3

    fun getUserByID(users: ArrayList<*>, id: String): HashMap<*, *>? {
        for (user in users) {
            if ((user as HashMap<*, *>)["id"] == id) {
                return user
            }
        }

        return null
    }

    // Help to draw the image of the category as a marker icon
    fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Return the middle point between to LatLng positions
    fun calcMiddlePoint(point1: LatLng, point2: LatLng): LatLng {
        return LatLng(
            (point1.latitude + point2.latitude) / 2,
            (point1.longitude + point2.longitude) / 2
        )
    }

    fun getReportFromArray(arr: ArrayList<*>, marker: Marker): HashMap<*, *>? {
        for (report1 in arr) {
            val report = report1 as HashMap<*, *>
            if (report["id"] as String == marker.title) {
                return report
            }
        }
        return null
    }

    // Calculate the distance between 2 points
    private fun calcDistance(p1: LatLng, p2: LatLng): Double {
        return sqrt((p1.latitude - p2.latitude) * (p1.latitude - p2.latitude) + (p1.longitude - p2.longitude) * (p1.longitude - p2.longitude))
    }

    fun getClosestReport(reportsList: ArrayList<*>, yourLocation: LatLng): HashMap<*, *>? {
        var closestReport: HashMap<*, *>? = null
        var minDist = Double.MAX_VALUE

        for (report in reportsList) {
            val reportData = (report as HashMap<*, *>)["data"] as HashMap<*, *>

            val distance = calcDistance(
                yourLocation,
                LatLng(reportData["lat"] as Double, reportData["lng"] as Double)
            )

            if (distance < minDist && distance < MIN_DISTANCE) {
                minDist = distance
                closestReport = report
            }
        }

        return closestReport
    }

    const val CAMERA_PERMISSION_REQUEST_CODE = 0
    const val CAMERA_ACTIVITY_REQUEST_CODE = 1
    const val ALL_CATEGORIES_ACTIVITY_REQUEST_CODE = 2

    const val CAMERA_UPDATE_ACTIVITY_REQUEST_CODE = 3
    const val CAMERA_UPDATE_PERMISSION_REQUEST_CODE = 4

    @Throws(IOException::class)
    fun getImageFile(context: Context): Pair<File, String> {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageName = "jpg_" + timestamp + "_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val imageFile = File.createTempFile(imageName, ".jpg", storageDir)
        val currentImagePath = imageFile.absolutePath
        return Pair(imageFile, currentImagePath)
    }

    const val PRIVACY_POLICY_URL = "https://pages.flycricket.io/wasteleaf/privacy.html"
    private const val PRIVACY_POLICY_KEY = "privacyPolicyKey"
    private const val PRIVACY_POLICY_STATUS_KEY = "privacyPolicyStatus"

    fun updatePrivacyPolicyStatus(context: Context, status: Boolean) {
        val sharedPref = context.getSharedPreferences(PRIVACY_POLICY_KEY, Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean(PRIVACY_POLICY_STATUS_KEY, status)
            apply()
        }
    }

    fun getPrivacyPolicyStatus(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(PRIVACY_POLICY_KEY, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(PRIVACY_POLICY_STATUS_KEY, false)
    }

    private const val LOGIN_KEY = "loginKey"
    private const val LOGIN_EMAIL_PASSWORD_KEY = "loginEmailPassword"
    const val DELIMITER = "&&&"

    fun saveEmailPassword(context: Context, email: String, password: String) {
        val sharedPref = context.getSharedPreferences(LOGIN_KEY, Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString(LOGIN_EMAIL_PASSWORD_KEY, email + DELIMITER + password)
            apply()
        }
    }

    fun deleteEmailPassword(context: Context) {
        val sharedPref = context.getSharedPreferences(LOGIN_KEY, Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            remove(LOGIN_EMAIL_PASSWORD_KEY)
            apply()
        }
    }

    fun getEmailPassword(context: Context): Pair<String?, String?> {
        val sharedPref = context.getSharedPreferences(LOGIN_KEY, Context.MODE_PRIVATE)
        val text = sharedPref.getString(LOGIN_EMAIL_PASSWORD_KEY, null)?.split(DELIMITER)
        return Pair(text?.get(0), text?.get(1))
    }
}