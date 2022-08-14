package com.wasteleaf.wasteleaf_android.home.feed.post.like

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity

class LikesActivity : AppCompatActivity() {
    private lateinit var functions: FirebaseFunctions
    private lateinit var postId: String
    private lateinit var adapter: LikeAdapter

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var likesRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_likes)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        val intentExtras = intent.extras
        postId = intentExtras?.get("postId") as String

        functions = Firebase.functions

        likesRecyclerView = findViewById(R.id.likesRecyclerView)

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

        initializeAdapter()
    }

    private fun initializeAdapter() {
        val data = hashMapOf(
            "postId" to postId
        )

        functions.getHttpsCallable("getLikesData").call(data).continueWith {
            val users = (it.result.data as HashMap<*, *>)["users"] as ArrayList<*>

            adapter = LikeAdapter(users)
            likesRecyclerView.adapter = adapter
            likesRecyclerView.layoutManager = LinearLayoutManager(this)

            deactivateLoadingScreen()
        }
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        likesRecyclerView.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    fun goToUserProfile(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }
}