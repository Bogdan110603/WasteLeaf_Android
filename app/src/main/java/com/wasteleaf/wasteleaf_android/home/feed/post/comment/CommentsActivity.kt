package com.wasteleaf.wasteleaf_android.home.feed.post.comment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity

class CommentsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    private lateinit var postId: String
    private lateinit var adapter: CommentAdapter

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var writeCommentInputText: EditText
    private lateinit var sendCommentButton: ImageView
    private lateinit var commentsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        val intentExtras = intent.extras
        postId = intentExtras?.get("postId") as String

        functions = Firebase.functions
        auth = FirebaseAuth.getInstance()

        writeCommentInputText = findViewById(R.id.writeCommentInputText)
        sendCommentButton = findViewById(R.id.sendCommentButton)
        commentsRecyclerView = findViewById(R.id.chatRecyclerView)

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

        sendCommentButton.setOnClickListener {
            val textComment = writeCommentInputText.text.toString()
            writeCommentInputText.setText("")

            if (textComment != "") {
                val commentData = hashMapOf(
                    "postId" to postId,
                    "text" to textComment
                )

                functions.getHttpsCallable("addComment").call(commentData).continueWith {
                    initializeAdapter()
                }

                activateLoadingScreen()
            }
        }
    }

    private fun initializeAdapter() {
        val data = hashMapOf(
            "postId" to postId
        )

        functions.getHttpsCallable("getCommentsData").call(data).continueWith {
            val comments = (it.result.data as HashMap<*, *>)["comments"] as ArrayList<*>
            val users = (it.result.data as HashMap<*, *>)["users"] as ArrayList<*>

            adapter = CommentAdapter(comments, users)
            commentsRecyclerView.adapter = adapter
            commentsRecyclerView.layoutManager = LinearLayoutManager(this)

            deactivateLoadingScreen()
        }
    }

    fun goToUserProfile(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun activateLoadingScreen() {
        loadingScreenLayout.visibility = VISIBLE

        writeCommentInputText.visibility = INVISIBLE
        sendCommentButton.visibility = INVISIBLE
        commentsRecyclerView.visibility = INVISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        writeCommentInputText.visibility = VISIBLE
        sendCommentButton.visibility = VISIBLE
        commentsRecyclerView.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }
}