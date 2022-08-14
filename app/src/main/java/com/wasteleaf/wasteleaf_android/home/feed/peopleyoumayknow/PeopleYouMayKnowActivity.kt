package com.wasteleaf.wasteleaf_android.home.feed.peopleyoumayknow

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.maps.MapsActivity
import com.wasteleaf.wasteleaf_android.home.messages.contact.ContactsActivity
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity
import com.wasteleaf.wasteleaf_android.home.user.profile.YourOwnProfileActivity

class PeopleYouMayKnowActivity : AppCompatActivity() {
    private lateinit var functions: FirebaseFunctions
    private lateinit var adapter: PeopleYouMayKnowAdapter

    private lateinit var chatButton: ImageView
    private lateinit var mapButton: ImageView
    private lateinit var profileButton: ImageView

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var peopleYouMayKnowRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people_you_may_know)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        functions = Firebase.functions

        chatButton = findViewById(R.id.chatButtonPeopleYouMayKnow)
        mapButton = findViewById(R.id.mapButtonPeopleYouMayKnow)
        profileButton = findViewById(R.id.profileButtonPeopleYouMayKnow)

        peopleYouMayKnowRecyclerView = findViewById(R.id.peopleYouMayKnowRecyclerView)

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
    }

    private fun initializeAdapter() {
        functions.getHttpsCallable("getPeopleYouMayKnow").call().continueWith {
            val users = (it.result.data as HashMap<*, *>)["users"] as ArrayList<*>

            adapter = PeopleYouMayKnowAdapter(users)
            peopleYouMayKnowRecyclerView.adapter = adapter
            peopleYouMayKnowRecyclerView.layoutManager = LinearLayoutManager(this)

            deactivateLoadingScreen()
        }
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        peopleYouMayKnowRecyclerView.visibility = VISIBLE
        chatButton.visibility = VISIBLE
        mapButton.visibility = VISIBLE
        profileButton.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    fun goToUserProfile(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }
}