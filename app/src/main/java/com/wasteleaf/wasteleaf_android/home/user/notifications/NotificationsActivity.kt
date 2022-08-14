package com.wasteleaf.wasteleaf_android.home.user.notifications

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.post.PostActivity
import com.wasteleaf.wasteleaf_android.home.maps.MapsActivity
import com.wasteleaf.wasteleaf_android.home.messages.chat.ChatActivity
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity
import com.wasteleaf.wasteleaf_android.util.Constants.friendRequestAcceptedNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.friendRequestDeclinedNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newCommentNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newFriendRequestNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newLikeNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newMessageNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newPostNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newReportNotificationID

class NotificationsActivity : AppCompatActivity() {
    private val functions = Firebase.functions

    private lateinit var notifications: ArrayList<*>

    private lateinit var adapter: NotificationAdapter

    private lateinit var notificationsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        notificationsRecyclerView = findViewById(R.id.notificationRecyclerView)

        val notificationsExtras = (intent.extras!!.get("notifications") as ArrayList<*>) as MutableList<*>
        for (index in notificationsExtras.indices) {
            ((notificationsExtras[index] as HashMap<*, *>)["data"] as HashMap<Any, Any>)["seen"] = false
        }
        notifications = notificationsExtras as ArrayList<*>

        notifications.sortBy { (((it as HashMap<*, *>)["data"] as HashMap<*, *>)["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong() }
        notifications.reverse()

        adapter = NotificationAdapter(notifications)
        notificationsRecyclerView.adapter = adapter
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    fun goToActivity(index: Int) {
        val notificationsExtras = notifications as MutableList<*>
        ((notificationsExtras[index] as HashMap<*, *>)["data"] as HashMap<Any, Any>)["seen"] = true
        notifications = notificationsExtras as ArrayList<*>

        adapter = NotificationAdapter(notifications)
        notificationsRecyclerView.adapter = adapter
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)

        val notificationData = (notifications[index] as HashMap<*, *>)["data"] as HashMap<*, *>

        var classActivity: Class<*>? = null
        var fieldName: String? = null

        when(notificationData["notificationID"] as String) {
            newReportNotificationID -> {
                classActivity = MapsActivity::class.java
                fieldName = "goToId"
            }
            newMessageNotificationID -> {
                classActivity = ChatActivity::class.java
                fieldName = "contactId"
            }
            newFriendRequestNotificationID -> {
                classActivity = UserProfileActivity::class.java
                fieldName = "userId"
            }
            friendRequestAcceptedNotificationID -> {
                classActivity = UserProfileActivity::class.java
                fieldName = "userId"
            }
            friendRequestDeclinedNotificationID -> {
                classActivity = UserProfileActivity::class.java
                fieldName = "userId"
            }
            newLikeNotificationID -> {
                classActivity = PostActivity::class.java
                fieldName = "postId"
            }
            newCommentNotificationID -> {
                classActivity = PostActivity::class.java
                fieldName = "postId"
            }
            newPostNotificationID -> {
                classActivity = PostActivity::class.java
                fieldName = "postId"
            }
        }

        val intent = Intent(this, classActivity)
        intent.putExtra(fieldName, notificationData["additionalData"] as String)
        startActivity(intent)

        functions.getHttpsCallable("deleteNotification").call(hashMapOf("notificationID" to (notifications[index] as HashMap<*, *>)["id"]))
    }
}