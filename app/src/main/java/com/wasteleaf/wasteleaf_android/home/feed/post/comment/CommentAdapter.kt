package com.wasteleaf.wasteleaf_android.home.feed.post.comment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.util.Constants.getUserByID
import com.wasteleaf.wasteleaf_android.R
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class CommentAdapter(
    private var comments: ArrayList<*>, private var users: ArrayList<*>
) : RecyclerView.Adapter<CommentAdapter.CommentsViewHolder>() {
    inner class CommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var functions: FirebaseFunctions
    private lateinit var auth: FirebaseAuth
    private lateinit var context: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentsViewHolder {
        context = parent.context
        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val commentText = holder.itemView.findViewById<TextView>(R.id.commentText)
        val commentTimestampRight = holder.itemView.findViewById<TextView>(R.id.commentTimestampRight)
        val commentTimestampLeft = holder.itemView.findViewById<TextView>(R.id.commentTimestampLeft)
        val userNameLeft = holder.itemView.findViewById<TextView>(R.id.userNameLeft)
        val userNameRight = holder.itemView.findViewById<TextView>(R.id.userNameRight)
        val userImageLeft = holder.itemView.findViewById<ImageView>(R.id.userImageLeft)
        val userImageRight = holder.itemView.findViewById<ImageView>(R.id.userImageRight)

        val commentData = (comments[position] as HashMap<*, *>)["data"] as HashMap<*, *>
        val user = getUserByID(users, commentData["userID"] as String) as HashMap<*, *>

        val timestamp = (commentData["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong() * 1000
        val prettyTime = PrettyTime(Locale.US)
        val ago = prettyTime.format(Date(timestamp))
        commentTimestampRight.text = ago
        commentTimestampLeft.text = ago

        commentText.text = commentData["text"] as String

        if (commentData["userID"] == auth.currentUser?.uid) {
            val cl = holder.itemView.findViewById(R.id.commentConstraintLayout) as ConstraintLayout
            val cs = ConstraintSet()
            cs.clone(cl)
            cs.setHorizontalBias(R.id.commentText, 1.0.toFloat())
            cs.applyTo(cl)

            commentTimestampLeft.visibility = INVISIBLE

            commentText.setPadding(5, 10, 5, 10)

            userNameLeft.visibility = GONE
            userImageLeft.visibility = GONE
        } else {
            val cl = holder.itemView.findViewById(R.id.commentConstraintLayout) as ConstraintLayout
            val cs = ConstraintSet()
            cs.clone(cl)
            cs.setHorizontalBias(R.id.commentText, 0.0.toFloat())
            cs.applyTo(cl)

            commentTimestampRight.visibility = INVISIBLE

            userNameRight.visibility = GONE
            userImageRight.visibility = GONE
        }

        val name = user["nickname"] as String
        userNameLeft.text = name
        userNameRight.text = name

        Glide.with(context).load(user["profilePictureID"]).circleCrop().apply(RequestOptions().override(50, 50)).into(userImageLeft)
        Glide.with(context).load(user["profilePictureID"]).circleCrop().apply(RequestOptions().override(50, 50)).into(userImageRight)

        userNameLeft.setOnClickListener {
            (context as CommentsActivity).goToUserProfile(commentData["userID"] as String)
        }
        userNameRight.setOnClickListener {
            (context as CommentsActivity).goToUserProfile(commentData["userID"] as String)
        }

        userImageLeft.setOnClickListener {
            (context as CommentsActivity).goToUserProfile(commentData["userID"] as String)
        }
        userImageRight.setOnClickListener {
            (context as CommentsActivity).goToUserProfile(commentData["userID"] as String)
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }
}