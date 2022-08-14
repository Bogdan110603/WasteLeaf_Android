package com.wasteleaf.wasteleaf_android.home.user.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.post.PostActivity
import com.wasteleaf.wasteleaf_android.home.user.notifications.NotificationsActivity
import com.wasteleaf.wasteleaf_android.login.LoginActivity
import com.wasteleaf.wasteleaf_android.util.Constants
import com.wasteleaf.wasteleaf_android.util.Constants.DEFAULT_URL
import com.wasteleaf.wasteleaf_android.util.Constants.GALLERY_REQUEST_CODE
import com.wasteleaf.wasteleaf_android.util.Constants.PROFILE_PICTURE_HEIGHT
import com.wasteleaf.wasteleaf_android.util.Constants.PROFILE_PICTURE_WIDTH
import com.wasteleaf.wasteleaf_android.util.Constants.p
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList

class YourOwnProfileActivity : AppCompatActivity() {
    private lateinit var functions: FirebaseFunctions
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    private lateinit var user: HashMap<*, *>
    private lateinit var notifications: ArrayList<*>

    private var notificationsDataReceived = false
    private var userDataReceived = false

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var unreadNotificationsBackground: ImageView
    private lateinit var unreadNotificationsTextView: TextView
    private lateinit var notificationButton: ImageView
    private lateinit var userProfileNicknameText: TextView
    private lateinit var userProfileNicknameEditText: EditText
    private lateinit var userProfilePictureImage: ImageView
    private lateinit var logOutButton: ImageView
    private lateinit var readyNicknameButton: ImageView
    private lateinit var friendsNumberText: TextView
    private lateinit var postsNumberText: TextView
    private lateinit var reportsNumberText: TextView
    private lateinit var collectsNumberText: TextView
    private lateinit var addPostButton: ImageView
    private lateinit var postsBackground: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_own_profile)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        unreadNotificationsBackground = findViewById(R.id.unreadNotificationsBackground)
        unreadNotificationsTextView = findViewById(R.id.unreadNotificationsTextView)
        notificationButton = findViewById(R.id.notificationButton)
        userProfileNicknameText = findViewById(R.id.userProfileNicknameText)
        userProfileNicknameEditText = findViewById(R.id.userProfileNicknameEditText)
        userProfilePictureImage = findViewById(R.id.userProfilePictureImage)
        logOutButton = findViewById(R.id.logOutButton)
        readyNicknameButton = findViewById(R.id.readyNicknameButton)
        friendsNumberText = findViewById(R.id.friendsNumberText)
        postsNumberText = findViewById(R.id.postsNumberText)
        reportsNumberText = findViewById(R.id.reportsNumberText)
        collectsNumberText = findViewById(R.id.collectsNumberText)
        addPostButton = findViewById(R.id.addPostButton)
        postsBackground = findViewById(R.id.postsBackground)

        // START ANIMATING THE LOADING CIRCLE
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                loadingCircle.rotation += 10
                mainHandler.postDelayed(this, 1)
            }
        })

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions
        storage = Firebase.storage

        val data = hashMapOf(
            "userId" to auth.currentUser!!.uid,
        )

        functions.getHttpsCallable("getUserData").call(data).continueWith {
            user = (it.result.data as HashMap<*, *>)["user"] as HashMap<*, *>
            val posts = (it.result.data as HashMap<*, *>)["posts"] as ArrayList<*>

            userProfileNicknameText.text = user["nickname"] as String

            friendsNumberText.text = (user["friendsNumber"] as Int).toString()
            postsNumberText.text = (user["postsNumber"] as Int).toString()
            reportsNumberText.text = (user["reportsMadeNumber"] as Int).toString()
            collectsNumberText.text = (user["reportsClearedNumber"] as Int).toString()

            Glide.with(this).load(user["profilePictureID"])
                .apply(RequestOptions().override(400, 400))
                .circleCrop().into(userProfilePictureImage)

            addPosts(posts)

            userProfileNicknameText.setOnClickListener {
                readyNicknameButton.visibility = VISIBLE
                userProfileNicknameEditText.visibility = VISIBLE
                userProfileNicknameText.visibility = INVISIBLE
            }

            readyNicknameButton.setOnClickListener {
                val currentNickname = userProfileNicknameEditText.text.toString()
                if (currentNickname.length >= 4) {
                    userProfileNicknameText.text = currentNickname

                    functions.getHttpsCallable("updateNickname")
                        .call(hashMapOf("newNickname" to currentNickname))

                    readyNicknameButton.visibility = INVISIBLE
                    userProfileNicknameEditText.visibility = INVISIBLE
                    userProfileNicknameText.visibility = VISIBLE
                } else {
                    Toast.makeText(this, "The nickname must have at least 4 characters!", LENGTH_SHORT).show()
                }
            }

            userProfilePictureImage.setOnClickListener {
                pickPhotoFromGallery()
            }

            notificationButton.setOnClickListener {
                val intent = Intent(this, NotificationsActivity::class.java)
                intent.putExtra("notifications", notifications)
                startActivity(intent)
            }

            addPostButton.setOnClickListener {
                startMakePostActivity()
            }

            logOutButton.setOnClickListener {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

            if (notificationsDataReceived) {
                deactivateLoadingScreen(posts)
            }

            userDataReceived = true
        }

        functions.getHttpsCallable("getNotifications").call(hashMapOf("userID" to auth.currentUser!!.uid)).continueWith {
            notifications = (it.result.data as HashMap<*, *>)["notifications"] as ArrayList<*>
            if (notifications.isEmpty()) {
                unreadNotificationsBackground.visibility = INVISIBLE
                unreadNotificationsTextView.visibility = INVISIBLE
            } else {
                unreadNotificationsTextView.text = notifications.size.toString()
            }

            if (userDataReceived) {
                deactivateLoadingScreen()
            }

            notificationsDataReceived = true
        }
    }

    private fun startMakePostActivity() {
        val intent = Intent(this, MakePostActivity::class.java)
        startActivity(intent)
    }

    private fun addPosts(posts: ArrayList<*>) {
        for (index in posts.indices) {
            createImage(index)

            Glide.with(this).load((posts[index] as HashMap<*, *>)["imageID"])
                .into(findViewById(index + Constants.BIG_NUMBER))
        }

        postsBackground.requestLayout()
        val params = postsBackground.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToBottom = posts.size - 1 + Constants.BIG_NUMBER

        for (index in posts.indices) {
            findViewById<ImageView>(index + Constants.BIG_NUMBER)
                .setOnClickListener {
                    val intent = Intent(this, PostActivity::class.java)
                    intent.putExtra("postId", (posts[index] as HashMap<*, *>)["id"] as String)
                    startActivity(intent)
                }
        }
    }

    private fun createImage(index: Int) {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.constraintLayout)

        val image = ImageView(this)
        image.visibility = INVISIBLE
        image.layoutParams = ConstraintLayout.LayoutParams(
            Constants.SCREEN_WIDTH / 3,
            ((Constants.SCREEN_WIDTH / 3) / Constants.POST_IMAGE_RATIO).toInt()
        )
        constraintLayout.addView(image)

        image.id = index + Constants.BIG_NUMBER

        if (index == 0) {
            image.requestLayout()
            val params = image.layoutParams as ConstraintLayout.LayoutParams
            params.leftToLeft = postsBackground.id
            params.topToTop = postsBackground.id
            return
        }

        if (index.mod(3) == 0) {
            image.requestLayout()
            val params = image.layoutParams as ConstraintLayout.LayoutParams
            params.leftToLeft = postsBackground.id
            params.topToBottom = image.id - 3
            return
        }

        image.requestLayout()
        val params = image.layoutParams as ConstraintLayout.LayoutParams
        params.leftToRight = image.id - 1
        params.topToTop = image.id - 1
    }

    private fun deactivateLoadingScreen(posts: ArrayList<*>? = null) {
        loadingScreenLayout.visibility = INVISIBLE

        notificationButton.visibility = VISIBLE
        userProfileNicknameText.visibility = VISIBLE
        userProfilePictureImage.visibility = VISIBLE
        addPostButton.visibility = VISIBLE
        logOutButton.visibility = VISIBLE

        if (posts != null) {
            for (index in posts.indices) {
                findViewById<ImageView>(index + Constants.BIG_NUMBER).visibility = VISIBLE
            }
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    private fun pickPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(reqCode, resultCode, data)
        if (resultCode == RESULT_OK && reqCode == GALLERY_REQUEST_CODE) {
            data?.data?.let { uri ->
                launchImageCrop(uri)
            }
        }
        if (resultCode == RESULT_OK && reqCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                result.uri?.let { uri ->
                    setImage(uri)
                }
            }
        }
    }

    private fun launchImageCrop(uri: Uri) {
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.OFF)
            .setAspectRatio(PROFILE_PICTURE_WIDTH, PROFILE_PICTURE_HEIGHT)
            .setCropShape(CropImageView.CropShape.OVAL)
            .start(this)
    }

    private fun setImage(uri: Uri) {
        val storageRef = storage.reference

        val profilePictureId = UUID.randomUUID().toString()
        val imageRef = storageRef.child(profilePictureId + p)

        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            PROFILE_PICTURE_WIDTH,
            PROFILE_PICTURE_HEIGHT,
            false
        )

        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        imageRef.putBytes(data).addOnSuccessListener {
            if (user["profilePictureID"] as String != DEFAULT_URL) {
                storage.getReferenceFromUrl(user["profilePictureID"] as String).delete()
            }

            imageRef.downloadUrl.addOnCompleteListener {
                val url = it.result.toString()
                functions.getHttpsCallable("updateProfilePictureID")
                    .call(hashMapOf("newProfilePictureID" to url))

                Glide.with(this).load(url)
                    .apply(RequestOptions().override(400, 400))
                    .circleCrop().into(userProfilePictureImage)
            }
        }
    }
}