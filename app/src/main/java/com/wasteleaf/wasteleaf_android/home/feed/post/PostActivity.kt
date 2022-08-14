package com.wasteleaf.wasteleaf_android.home.feed.post

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.util.Constants.THRESHOLD_USERNAME_BACKGROUND
import com.wasteleaf.wasteleaf_android.util.Constants.getUserByID
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.post.comment.CommentAdapter
import com.wasteleaf.wasteleaf_android.home.feed.post.like.LikesActivity
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity
import com.wasteleaf.wasteleaf_android.util.Constants
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class PostActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private lateinit var currentPostId: String
    private lateinit var adapter: CommentAdapter

    private lateinit var userNameText: TextView
    private lateinit var userNameBackground: ImageView
    private lateinit var postText: TextView
    private lateinit var postImage: ImageView
    private lateinit var userPictureImage: ImageView
    private lateinit var likeNumberText: TextView
    private lateinit var likeButtonImage: ImageView
    private lateinit var postTimestampText: TextView
    private lateinit var writeCommentInputText: EditText
    private lateinit var sendCommentButton: ImageView
    private lateinit var commentsRecyclerView: RecyclerView

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        val extras = intent.extras
        val dataExtras = (extras!!.get("postId") as String).split(Constants.DELIMITER)
        currentPostId = dataExtras[0]
        if (dataExtras.lastIndex != 0) {
            functions.getHttpsCallable("deleteNotification").call(hashMapOf("notificationID" to dataExtras[1]))
        }

        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        userNameText = findViewById(R.id.userNameText)
        userNameBackground = findViewById(R.id.userNameBackgroundImage)
        postText = findViewById(R.id.postText)
        postImage = findViewById(R.id.postImage)
        userPictureImage = findViewById(R.id.userPictureImage)
        likeNumberText = findViewById(R.id.likeNumberText)
        likeButtonImage = findViewById(R.id.likeButtonImage)
        postTimestampText = findViewById(R.id.postTimestampText)
        writeCommentInputText = findViewById(R.id.writeCommentInputText2)
        sendCommentButton = findViewById(R.id.sendCommentButton2)
        commentsRecyclerView = findViewById(R.id.chatRecyclerView)

        // START ANIMATING THE LOADING CIRCLE
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                loadingCircle.rotation += 10
                mainHandler.postDelayed(this, 1)
            }
        })

        val data = hashMapOf(
            "postId" to currentPostId
        )

        functions.getHttpsCallable("getPostData").call(data).continueWith {
            val postData = (it.result.data as HashMap<*, *>)["post"] as HashMap<*, *>
            Log.d("CEVA", postData.toString())
            val comments = (it.result.data as HashMap<*, *>)["comments"] as ArrayList<*>
            val users = (it.result.data as HashMap<*, *>)["users"] as ArrayList<*>
            val user = getUserByID(users, postData["userID"] as String)!!

            Log.d("CEVA", user.toString())
            Log.d("CEVA", comments.toString())

            val nickname = user["nickname"] as String
            val profilePictureId = user["profilePictureID"] as String

            userNameText.text = nickname
            userNameText.measure(0, 0)
            val textWidth = userNameText.measuredWidth
            userNameBackground.requestLayout()
            userNameBackground.layoutParams.width = textWidth + THRESHOLD_USERNAME_BACKGROUND

            val timestamp = Date(
                ((postData["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong()) * 1000
            )
            val prettyTime = PrettyTime(Locale.US)
            val ago = prettyTime.format(timestamp)
            postTimestampText.text = ago

            postText.text = postData["text"] as String

            Glide.with(this).load(postData["imageID"] as String).into(postImage)
            Glide.with(this).load(profilePictureId).circleCrop().into(userPictureImage)

            likeNumberText.text = (postData["likes"] as ArrayList<*>).size.toString()

            if ((postData["likes"] as ArrayList<*>).contains(auth.currentUser?.uid)) {
                likeButtonImage.setImageResource(R.drawable.thumb_up_alt_black_24dp)
                likeButtonImage.tag = R.drawable.thumb_up_alt_black_24dp

                (likeNumberText.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                    0,
                    45,
                    5,
                    0
                )
            } else {
                likeButtonImage.setImageResource(R.drawable.thumb_up_off_alt_black_24dp)
                likeButtonImage.tag = R.drawable.thumb_up_off_alt_black_24dp

                (likeNumberText.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                    0,
                    30,
                    5,
                    0
                )
            }

            initializeAdapter(comments, users)

            likeButtonImage.setOnClickListener {
                if (likeButtonImage.tag == R.drawable.thumb_up_off_alt_black_24dp) {
                    likeButtonImage.setImageResource(R.drawable.thumb_up_alt_black_24dp)
                    likeButtonImage.tag = R.drawable.thumb_up_alt_black_24dp

                    (likeNumberText.layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                        0,
                        45,
                        5,
                        0
                    )

                    likeNumberText.text = (likeNumberText.text.toString().toInt() + 1).toString()
                    (postData["likes"] as ArrayList<String>).add(auth.currentUser!!.uid)

                    val data1 = hashMapOf(
                        "postId" to currentPostId
                    )
                    functions.getHttpsCallable("addLike").call(data1)
                }
            }

            userNameBackground.setOnClickListener {
                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("userId", user["id"] as String)
                startActivity(intent)
            }

            sendCommentButton.setOnClickListener {
                val textComment = writeCommentInputText.text.toString()
                writeCommentInputText.setText("")

                if (textComment != "") {
                    val commentData = hashMapOf(
                        "postId" to currentPostId,
                        "text" to textComment
                    )

                    functions.getHttpsCallable("addComment").call(commentData).continueWith {
                        functions.getHttpsCallable("getCommentsData")
                            .call(hashMapOf("postId" to currentPostId)).continueWith { it1 ->
                                val comments1 =
                                    (it1.result.data as HashMap<*, *>)["comments"] as ArrayList<*>
                                val users1 =
                                    (it1.result.data as HashMap<*, *>)["users"] as ArrayList<*>
                                initializeAdapter(comments1, users1)
                            }
                    }

                    activateLoadingScreen()
                }
            }

            likeNumberText.setOnClickListener {
                val intent = Intent(this, LikesActivity::class.java)
                intent.putExtra("postId", currentPostId)
                startActivity(intent)
            }
        }
    }

    private fun activateLoadingScreen() {
        loadingScreenLayout.visibility = VISIBLE

        userNameBackground.visibility = INVISIBLE
        likeButtonImage.visibility = INVISIBLE
        sendCommentButton.visibility = INVISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        userNameBackground.visibility = VISIBLE
        likeButtonImage.visibility = VISIBLE
        sendCommentButton.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    private fun initializeAdapter(comments: ArrayList<*>, users: ArrayList<*>) {
        adapter = CommentAdapter(comments, users)
        commentsRecyclerView.adapter = adapter
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)

        deactivateLoadingScreen()
    }
}