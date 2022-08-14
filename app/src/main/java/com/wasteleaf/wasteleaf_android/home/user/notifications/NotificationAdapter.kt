package com.wasteleaf.wasteleaf_android.home.user.notifications

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.wasteleaf.wasteleaf_android.R
import org.ocpsoft.prettytime.PrettyTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class NotificationAdapter(
    private var notifications: ArrayList<*>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
    private lateinit var context: Context

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position] as HashMap<*, *>
        val notificationData = notification["data"] as HashMap<*, *>

        val notificationBackground = holder.itemView.findViewById<ImageView>(R.id.likeBackground)
        val notificationTitle = holder.itemView.findViewById<TextView>(R.id.notificationTitle)
        val notificationBody = holder.itemView.findViewById<TextView>(R.id.notificationBody)
        val notificationTimestamp = holder.itemView.findViewById<TextView>(R.id.notificationTimestamp)

        notificationTitle.text = notificationData["title"] as String
        notificationBody.text = notificationData["body"] as String

        val timestamp = Date((notificationData["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong() * 1000)
        val prettyTime = PrettyTime(Locale.US)
        val ago = prettyTime.format(timestamp)
        notificationTimestamp.text = ago

        notificationBackground.setOnClickListener {
            (context as NotificationsActivity).goToActivity(position)
        }

        if (notificationData["seen"] as Boolean) {
            ImageViewCompat.setImageTintList(notificationBackground, ColorStateList.valueOf(Color.rgb(175, 175, 175)))
        }
    }

    override fun getItemCount(): Int {
        return notifications.size
    }


}