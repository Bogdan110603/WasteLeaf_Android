package com.wasteleaf.wasteleaf_android.home.messages.contact

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.messages.chat.ChatActivity
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class ContactAdapter(
    private var contacts: ArrayList<*>
) : RecyclerView.Adapter<ContactAdapter.ContactsViewHolder>() {

    inner class ContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private lateinit var mContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactsViewHolder {
        mContext = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item, parent, false)
        return ContactsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactsViewHolder, position: Int) {
        val contactName = holder.itemView.findViewById<TextView>(R.id.contactName)
        val contactImage = holder.itemView.findViewById<ImageView>(R.id.contactImage)
        val lastMessageText = holder.itemView.findViewById<TextView>(R.id.lastMessageText)
        val timestampLastMessage = holder.itemView.findViewById<TextView>(R.id.timestampLastMessage)
        val contactBackground = holder.itemView.findViewById<ImageView>(R.id.contactBackground)

        val contact = contacts[position] as HashMap<*, *>

        contactName.text = contact["nickname"] as String

        Glide.with(mContext).load(contact["profilePictureID"] as String)
            .circleCrop().into(contactImage)

        lastMessageText.text = contact["text"] as String

        val timestamp = (contact["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong()
        val prettyTime = PrettyTime(Locale.US)
        val ago = prettyTime.format(Date(timestamp * 1000))
        timestampLastMessage.text = ago

        contactBackground.setOnClickListener {
            val intent = Intent(mContext, ChatActivity::class.java)
            intent.putExtra("contactId", contact["id"] as String)
            mContext.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }
}