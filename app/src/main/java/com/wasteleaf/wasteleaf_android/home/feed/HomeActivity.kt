package com.wasteleaf.wasteleaf_android.home.feed

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.wasteleaf.wasteleaf_android.util.Constants
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.peopleyoumayknow.PeopleYouMayKnowActivity
import com.wasteleaf.wasteleaf_android.home.feed.post.comment.CommentsActivity
import com.wasteleaf.wasteleaf_android.home.feed.post.like.LikesActivity
import com.wasteleaf.wasteleaf_android.home.feed.post.PostAdapter
import com.wasteleaf.wasteleaf_android.home.feed.searchuser.SearchUserActivity
import com.wasteleaf.wasteleaf_android.home.maps.MapsActivity
import com.wasteleaf.wasteleaf_android.home.messages.contact.ContactsActivity
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity
import com.wasteleaf.wasteleaf_android.home.user.profile.YourOwnProfileActivity
import com.wasteleaf.wasteleaf_android.login.LoginActivity
import com.wasteleaf.wasteleaf_android.login.PrivacyPolicyActivity
import com.wasteleaf.wasteleaf_android.util.Constants.getEmailPassword
import com.wasteleaf.wasteleaf_android.util.Constants.getPrivacyPolicyStatus

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private lateinit var chatButton: ImageView
    private lateinit var mapButton: ImageView
    private lateinit var profileButton: ImageView

    private lateinit var searchUserButton: ImageView

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var postsLayout: ConstraintLayout
    private lateinit var postsRecyclerView: RecyclerView

    // Check if user is authenticated and redirect them to LoginActivity if they aren't
    override fun onStart() {
        super.onStart()

        val user = auth.currentUser
        if (user == null) {
            val emailPassword = getEmailPassword(this)
            val email = emailPassword.first
            val password = emailPassword.second
            if (email!= null && password != null) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            
                        } else {
                            goToLogin()
                        }
                    }
            } else {
                goToLogin()
            }
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        if (!getPrivacyPolicyStatus(this)) {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
            finish()
        }

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        setDisplayDimensions()

        updateUserToken()

        chatButton = findViewById(R.id.chatButton)
        mapButton = findViewById(R.id.mapButton)
        profileButton = findViewById(R.id.profileButton)

        searchUserButton = findViewById(R.id.searchUserButton)

        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        postsLayout = findViewById(R.id.postsLayout)
        postsRecyclerView = findViewById(R.id.postsRecyclerView)

        // START ANIMATING THE LOADING CIRCLE
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                loadingCircle.rotation += 10
                mainHandler.postDelayed(this, 1)
            }
        })

        functions.getHttpsCallable("getFeed").call().continueWith {
            val posts = (it.result.data as HashMap<*, *>)["posts"] as ArrayList<*>
            val people = (it.result.data as HashMap<*, *>)["people"] as ArrayList<*>

            buildAdapter(posts, people)
            deactivateLoadingScreen()
        }

        chatButton.setOnClickListener {
            val intent = Intent(this, ContactsActivity::class.java)
            startActivity(intent)
        }

        mapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, YourOwnProfileActivity::class.java)
            startActivity(intent)
        }

        searchUserButton.setOnClickListener {
            val intent = Intent(this, SearchUserActivity::class.java)
            startActivity(intent)
        }
    }

    // Update the db with the token of this device
    private fun updateUserToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("ceva", "Fetching FCM registration token failed", task.exception)
            } else {
                val token = task.result
                functions.getHttpsCallable("updateToken").call(hashMapOf("newToken" to token))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildAdapter(posts: ArrayList<*>, people:ArrayList<*>) {
        if (posts.isEmpty()) {
            goToPeopleYouMayKnow()
        } else {

            val adapter = PostAdapter(posts, people)
            postsRecyclerView.adapter = adapter
            postsRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        postsLayout.visibility = VISIBLE
        chatButton.visibility = VISIBLE
        searchUserButton.visibility = VISIBLE
        mapButton.visibility = VISIBLE
        profileButton.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    fun goToUserProfile(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    fun goToComments(postId: String) {
        val intent = Intent(this, CommentsActivity::class.java)
        intent.putExtra("postId", postId)
        startActivity(intent)
    }

    fun goToLikes(postId: String) {
        val intent = Intent(this, LikesActivity::class.java)
        intent.putExtra("postId", postId)
        startActivity(intent)
    }

    private fun goToPeopleYouMayKnow() {
        val intent = Intent(this, PeopleYouMayKnowActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setDisplayDimensions() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        Constants.SCREEN_WIDTH = displayMetrics.widthPixels
        Constants.SCREEN_HEIGHT = displayMetrics.heightPixels
    }
}