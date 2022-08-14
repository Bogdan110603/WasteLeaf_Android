package com.wasteleaf.wasteleaf_android.home.feed.post

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.wasteleaf.wasteleaf_android.util.Constants.THRESHOLD_USERNAME_BACKGROUND
import com.wasteleaf.wasteleaf_android.util.Constants.getUserByID
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.HomeActivity
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class PostAdapter(
    private var posts: ArrayList<*>, private var users: ArrayList<*>
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions
    private lateinit var storage: FirebaseStorage
    private lateinit var context: Context

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        context = parent.context
        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions
        storage = Firebase.storage
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position] as HashMap<*, *>
        val postId = post["id"] as String
        val postData = post["data"] as HashMap<*, *>
        val user = getUserByID(users, postData["userID"] as String)!!

        val userNameText = holder.itemView.findViewById<TextView>(R.id.userNameText)
        val userNameBackground = holder.itemView.findViewById<ImageView>(R.id.userNameBackgroundImage)
        val postText = holder.itemView.findViewById<TextView>(R.id.postText)
        val postImage = holder.itemView.findViewById<ImageView>(R.id.postImage)
        val userPictureImage = holder.itemView.findViewById<ImageView>(R.id.userPictureImage)
        val likeNumberText = holder.itemView.findViewById<TextView>(R.id.likeNumberText)
        val likeButtonImage = holder.itemView.findViewById<ImageView>(R.id.likeButtonImage)
        val postCommentImage = holder.itemView.findViewById<ImageView>(R.id.postCommentsImage)
        val postTimestampText = holder.itemView.findViewById<TextView>(R.id.postTimestampText)

        val nickname = (user["data"] as HashMap<*, *>)["nickname"].toString()
        val profilePictureId = (user["data"] as HashMap<*, *>)["profilePictureID"].toString()

        userNameText.text = nickname
        userNameText.measure(0, 0)
        val textWidth = userNameText.measuredWidth
        userNameBackground.requestLayout()
        userNameBackground.layoutParams.width = textWidth + THRESHOLD_USERNAME_BACKGROUND

        val timestamp = Date(((postData["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong() * 1000))
        val prettyTime = PrettyTime(Locale.US)
        val ago = prettyTime.format(timestamp)
        postTimestampText.text = ago

        postText.text = postData["text"].toString()

        Glide.with(context).load(postData["imageID"]).into(postImage)
        Glide.with(context).load(profilePictureId).circleCrop().into(userPictureImage)

        likeNumberText.text = (postData["likes"] as ArrayList<*>).size.toString()

        if ((postData["likes"] as ArrayList<*>).contains(auth.currentUser?.uid)) {
            likeButtonImage.setImageResource(R.drawable.thumb_up_alt_black_24dp)
            likeButtonImage.tag = R.drawable.thumb_up_alt_black_24dp

            (likeNumberText.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 45, 5, 0)
        } else {
            likeButtonImage.setImageResource(R.drawable.thumb_up_off_alt_black_24dp)
            likeButtonImage.tag = R.drawable.thumb_up_off_alt_black_24dp

            (likeNumberText.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 30, 5, 0)
        }

        likeButtonImage.setOnClickListener {
            if (likeButtonImage.tag == R.drawable.thumb_up_off_alt_black_24dp) {
                likeButtonImage.setImageResource(R.drawable.thumb_up_alt_black_24dp)
                likeButtonImage.tag = R.drawable.thumb_up_alt_black_24dp

                (likeNumberText.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 45, 5, 0)

                likeNumberText.text = (likeNumberText.text.toString().toInt() + 1).toString()
                (postData["likes"] as ArrayList<String>).add(auth.currentUser!!.uid)

                val data = hashMapOf(
                    "postId" to postId
                )
                functions.getHttpsCallable("addLike").call(data)
            }
        }

        userNameBackground.setOnClickListener {
            (context as HomeActivity).goToUserProfile(user["id"] as String)
        }

        postCommentImage.setOnClickListener {
            (context as HomeActivity).goToComments(postId)
        }

        likeNumberText.setOnClickListener {
            (context as HomeActivity).goToLikes(postId)
        }
    }

    override fun getItemCount(): Int {
        return posts.size
    }
}