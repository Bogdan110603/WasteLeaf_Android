package com.wasteleaf.wasteleaf_android.home.messages.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.wasteleaf.wasteleaf_android.R
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class ChatAdapter(
    private var messages: ArrayList<*>,
    private var messagesDaysUntil: ArrayList<String>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val messageText = holder.itemView.findViewById<TextView>(R.id.messageText)
        val messageTimestampLeft = holder.itemView.findViewById<TextView>(R.id.messageTimestampLeft)
        val messageTimestampRight =
            holder.itemView.findViewById<TextView>(R.id.messageTimestampRight)
        val dayNameText = holder.itemView.findViewById<TextView>(R.id.dayNameText)

        val message = messages[position] as HashMap<*, *>

        val timestamp =
            ((message["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong() * 1000)
        val prettyTime = PrettyTime(Locale.US)
        val ago = prettyTime.format(Date(timestamp))
        messageTimestampLeft.text = ago
        messageTimestampRight.text = ago

        messageText.text = message["text"] as String

        if (message["sentByMe"] as Boolean) {
            val cl = holder.itemView.findViewById(R.id.messageConstraintLayout) as ConstraintLayout
            val cs = ConstraintSet()
            cs.clone(cl)
            cs.setHorizontalBias(R.id.messageText, 1.0.toFloat())
            cs.applyTo(cl)

        } else {
            val cl = holder.itemView.findViewById(R.id.messageConstraintLayout) as ConstraintLayout
            val cs = ConstraintSet()
            cs.clone(cl)
            cs.setHorizontalBias(R.id.messageText, 0.0.toFloat())
            cs.applyTo(cl)
        }

        dayNameText.text = messagesDaysUntil[position]

        if (dayNameText.text == "") {
            dayNameText.visibility = GONE
        } else {
            dayNameText.visibility = VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }
}