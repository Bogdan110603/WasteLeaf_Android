package com.wasteleaf.wasteleaf_android.home.user.profile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import com.wasteleaf.wasteleaf_android.util.Constants.BIG_NUMBER
import com.wasteleaf.wasteleaf_android.util.Constants.POST_IMAGE_RATIO
import com.wasteleaf.wasteleaf_android.util.Constants.SCREEN_WIDTH
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.post.PostActivity
import com.wasteleaf.wasteleaf_android.home.messages.chat.ChatActivity
import com.wasteleaf.wasteleaf_android.util.Constants


class UserProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    private lateinit var storage: FirebaseStorage

    private lateinit var currentUserId: String

    enum class RelationshipStatus {
        FRIEND, REQUEST_SENT, HANDLE_REQUEST, NEUTRAL
    }

    private var relStatus = RelationshipStatus.NEUTRAL
    private lateinit var relationshipId: String

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var userProfileNicknameText: TextView
    private lateinit var userProfilePictureImage: ImageView
    private lateinit var friendsNumberText: TextView
    private lateinit var postsNumberText: TextView
    private lateinit var reportsNumberText: TextView
    private lateinit var collectsNumberText: TextView
    private lateinit var interactButtonIcon: ImageView
    private lateinit var interactButtonBackground: ImageView
    private lateinit var backArrow: ImageView
    private lateinit var friendRequestLayout: ConstraintLayout
    private lateinit var acceptFriendRequestButton: ImageView
    private lateinit var declineFriendRequestButton: ImageView
    private lateinit var postsBackground: ImageView

    override fun onStart() {
        super.onStart()

        if (auth.currentUser!!.uid == currentUserId) {
            val intent = Intent(this, YourOwnProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions
        storage = Firebase.storage

        val extras = intent.extras
        val dataExtras = (extras!!.get("userId") as String).split(Constants.DELIMITER)
        currentUserId = dataExtras[0]
        if (dataExtras.lastIndex != 0) {
            functions.getHttpsCallable("deleteNotification").call(hashMapOf("notificationID" to dataExtras[1]))
        }

        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        userProfileNicknameText = findViewById(R.id.userProfileNicknameText)
        userProfilePictureImage = findViewById(R.id.userProfilePictureImage)
        friendsNumberText = findViewById(R.id.friendsNumberText)
        postsNumberText = findViewById(R.id.postsNumberText)
        reportsNumberText = findViewById(R.id.reportsNumberText)
        collectsNumberText = findViewById(R.id.collectsNumberText)
        interactButtonIcon = findViewById(R.id.interactButtonIcon)
        interactButtonBackground = findViewById(R.id.interactButtonBackground)
        backArrow = findViewById(R.id.backArrow)
        friendRequestLayout = findViewById(R.id.friendRequestLayout)
        acceptFriendRequestButton = findViewById(R.id.acceptFriendRequestButton)
        declineFriendRequestButton = findViewById(R.id.declineFriendRequestButton)
        postsBackground = findViewById(R.id.postsBackground)

        // START ANIMATING THE LOADING CIRCLE
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                loadingCircle.rotation += 10
                mainHandler.postDelayed(this, 1)
            }
        })

        val data = hashMapOf(
            "userId" to currentUserId,
        )

        functions.getHttpsCallable("getUserData").call(data).continueWith {
            val user = (it.result.data as HashMap<*, *>)["user"] as HashMap<*, *>
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

            val relationship = (it.result.data as HashMap<*, *>)["relationship"] as HashMap<*, *>?
            if (relationship == null) {
                relStatus = RelationshipStatus.NEUTRAL
            } else {
                relationshipId = relationship["id"] as String
                val relationshipData = relationship["data"] as HashMap<*, *>

                relStatus = if (relationshipData["status"] as Boolean) {
                    RelationshipStatus.FRIEND
                } else {
                    if ((relationshipData["users"] as ArrayList<*>)[0] == currentUserId) {
                        RelationshipStatus.HANDLE_REQUEST
                    } else {
                        RelationshipStatus.REQUEST_SENT
                    }
                }
            }

            evaluateRelStatus()

            interactButtonIcon.setOnClickListener {
                when(relStatus) {
                    RelationshipStatus.FRIEND -> {
                        val intent = Intent(this, ChatActivity::class.java)
                        intent.putExtra("contactId", currentUserId)
                        startActivity(intent)
                    }
                    RelationshipStatus.REQUEST_SENT -> {
                        // Maybe cancel friend request
                    }
                    RelationshipStatus.HANDLE_REQUEST -> {
                        // Nothing
                    }
                    RelationshipStatus.NEUTRAL -> {
                        val friendRequestData = hashMapOf(
                            "userId" to currentUserId
                        )

                        functions.getHttpsCallable("sendFriendRequest").call(friendRequestData)

                        relStatus = RelationshipStatus.REQUEST_SENT
                        interactButtonIcon.setImageDrawable(resources.getDrawable(R.drawable.pending___))
                    }
                }
            }

            backArrow.setOnClickListener {
                onBackPressed()
            }

            deactivateLoadingScreen()
        }
    }

    private fun evaluateRelStatus() {
        when(relStatus) {
            RelationshipStatus.FRIEND -> {
                interactButtonIcon.setImageDrawable(resources.getDrawable(R.drawable.send_black_24dp_1))
            }
            RelationshipStatus.REQUEST_SENT -> {
                interactButtonIcon.setImageDrawable(resources.getDrawable(R.drawable.pending___))
            }
            RelationshipStatus.HANDLE_REQUEST -> {
                friendRequestLayout.visibility = View.VISIBLE
                interactButtonIcon.visibility = View.INVISIBLE
                interactButtonBackground.visibility = View.INVISIBLE

                interactButtonIcon.setImageDrawable(resources.getDrawable(R.drawable.pending___))

                acceptFriendRequestButton.setOnClickListener {
                    Toast.makeText(this, "ACCEPTED", Toast.LENGTH_SHORT).show()
                    val acceptFriendRequestData = hashMapOf(
                        "relationshipId" to relationshipId,
                    )

                    functions.getHttpsCallable("acceptFriendRequest").call(acceptFriendRequestData)

                    relStatus = RelationshipStatus.FRIEND

                    friendRequestLayout.visibility = View.INVISIBLE

                    interactButtonIcon.visibility = View.VISIBLE
                    interactButtonBackground.visibility = View.VISIBLE
                    interactButtonIcon.setImageDrawable(resources.getDrawable(R.drawable.send_black_24dp_1))
                }

                declineFriendRequestButton.setOnClickListener {
                    Toast.makeText(this, "DECLINED", Toast.LENGTH_SHORT).show()
                    val declineFriendRequestData = hashMapOf(
                        "relationshipId" to relationshipId,
                    )

                    functions.getHttpsCallable("declineFriendRequest").call(declineFriendRequestData)

                    relStatus = RelationshipStatus.NEUTRAL

                    friendRequestLayout.visibility = View.INVISIBLE

                    interactButtonIcon.visibility = View.VISIBLE
                    interactButtonBackground.visibility = View.VISIBLE
                    interactButtonIcon.setImageDrawable(resources.getDrawable(R.drawable.person_add_black_24dp))
                }
            }
            RelationshipStatus.NEUTRAL -> {
                interactButtonIcon.setImageDrawable(resources.getDrawable(R.drawable.person_add_black_24dp))
            }
        }
    }

    private fun addPosts(posts: ArrayList<*>) {
        for (index in posts.indices) {
            createImage(index)

            Glide.with(this).load((posts[index] as HashMap<*, *>)["imageID"])
                .into(findViewById(index + BIG_NUMBER))
        }

        postsBackground.requestLayout()
        val params = postsBackground.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToBottom = posts.size - 1 + BIG_NUMBER

        for (index in posts.indices) {
            findViewById<ImageView>(index + BIG_NUMBER)
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
        image.layoutParams = ConstraintLayout.LayoutParams(
            SCREEN_WIDTH / 3,
            ((SCREEN_WIDTH / 3) / POST_IMAGE_RATIO).toInt()
        )
        constraintLayout.addView(image)

        image.id = index + BIG_NUMBER

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

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = View.INVISIBLE

        interactButtonIcon.visibility = View.VISIBLE
        backArrow.visibility = View.VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }
}