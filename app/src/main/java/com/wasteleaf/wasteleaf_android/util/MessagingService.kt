package com.wasteleaf.wasteleaf_android.util

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.post.PostActivity
import com.wasteleaf.wasteleaf_android.home.maps.MapsActivity
import com.wasteleaf.wasteleaf_android.home.messages.chat.ChatActivity
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity
import com.wasteleaf.wasteleaf_android.util.Constants.BIG_NUMBER
import com.wasteleaf.wasteleaf_android.util.Constants.CHANNEL_ID
import com.wasteleaf.wasteleaf_android.util.Constants.DELIMITER
import com.wasteleaf.wasteleaf_android.util.Constants.friendRequestAcceptedNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.friendRequestDeclinedNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newCommentNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newFriendRequestNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newLikeNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newMessageNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newPostNotificationID
import com.wasteleaf.wasteleaf_android.util.Constants.newReportNotificationID

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d("MessagingService", "Refreshed token: $token")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)

        val id = p0.data["notificationDBId"] as String

        when (p0.data["notificationID"]) {
            newReportNotificationID -> {
                val notificationTitle = p0.data["title"] as String
                val notificationBody = p0.data["body"] as String
                val notificationAdditionalData = p0.data["additionalData"] as String

                createNotification(
                    notificationTitle, notificationBody, "goToId",
                    notificationAdditionalData, id, MapsActivity::class.java
                )
            }
            newMessageNotificationID -> {
                if (ChatActivity.active) {
                    ChatActivity.getInstance().addMessage(p0.data["body"] as String)
                } else {
                    val notificationTitle = p0.data["title"] as String
                    val notificationBody = p0.data["body"] as String
                    val notificationAdditionalData = p0.data["additionalData"] as String

                    createNotification(
                        notificationTitle, notificationBody, "contactId",
                        notificationAdditionalData, id, ChatActivity::class.java
                    )
                }
            }
            newFriendRequestNotificationID -> {
                val notificationTitle = p0.data["title"] as String
                val notificationBody = p0.data["body"] as String
                val notificationAdditionalData = p0.data["additionalData"] as String

                createNotification(
                    notificationTitle, notificationBody, "userId",
                    notificationAdditionalData, id, UserProfileActivity::class.java
                )
            }
            friendRequestAcceptedNotificationID -> {
                val notificationTitle = p0.data["title"] as String
                val notificationBody = p0.data["body"] as String
                val notificationAdditionalData = p0.data["additionalData"] as String

                createNotification(
                    notificationTitle, notificationBody, "userId",
                    notificationAdditionalData, id, UserProfileActivity::class.java
                )
            }
            friendRequestDeclinedNotificationID -> {
                val notificationTitle = p0.data["title"] as String
                val notificationBody = p0.data["body"] as String
                val notificationAdditionalData = p0.data["additionalData"] as String

                createNotification(
                    notificationTitle, notificationBody, "userId",
                    notificationAdditionalData, id, UserProfileActivity::class.java
                )
            }
            newLikeNotificationID -> {
                val notificationTitle = p0.data["title"] as String
                val notificationBody = p0.data["body"] as String
                val notificationAdditionalData = p0.data["additionalData"] as String

                createNotification(
                    notificationTitle, notificationBody, "postId",
                    notificationAdditionalData, id, PostActivity::class.java
                )
            }
            newCommentNotificationID -> {
                val notificationTitle = p0.data["title"] as String
                val notificationBody = p0.data["body"] as String
                val notificationAdditionalData = p0.data["additionalData"] as String

                createNotification(
                    notificationTitle, notificationBody, "postId",
                    notificationAdditionalData, id, PostActivity::class.java
                )
            }
            newPostNotificationID -> {
                val notificationTitle = p0.data["title"] as String
                val notificationBody = p0.data["body"] as String
                val notificationAdditionalData = p0.data["additionalData"] as String

                createNotification(
                    notificationTitle, notificationBody, "postId",
                    notificationAdditionalData, id, PostActivity::class.java
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createPendingIntent(
        activityClass: Class<*>,
        fieldName: String,
        fieldValue: String,
        notificationDBId: String
    ): PendingIntent {
        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(fieldName, fieldValue + DELIMITER + notificationDBId)
        }
        return PendingIntent.getActivity(this, (10..BIG_NUMBER).random(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createNotification(
        title: String,
        body: String,
        fieldName: String,
        additionalData: String,
        notificationDBId: String,
        activityClass: Class<*>
    ) {
        val resultPendingIntent = createPendingIntent(activityClass, fieldName, additionalData, notificationDBId)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.logo_wasteleaf_1)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(resultPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify((10..BIG_NUMBER).random(), notification)
    }
}