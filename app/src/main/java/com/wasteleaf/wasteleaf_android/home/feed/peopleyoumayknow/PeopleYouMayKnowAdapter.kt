package com.wasteleaf.wasteleaf_android.home.feed.peopleyoumayknow

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wasteleaf.wasteleaf_android.R

class PeopleYouMayKnowAdapter(
    private var users: ArrayList<*>
) : RecyclerView.Adapter<PeopleYouMayKnowAdapter.PeopleWouMayKnowViewHolder>() {
    private lateinit var context: Context

    inner class PeopleWouMayKnowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeopleWouMayKnowViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.person_you_may_know_item, parent, false)
        return PeopleWouMayKnowViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeopleWouMayKnowViewHolder, position: Int) {
        val user = users[position] as HashMap<*, *>

        val likeBackground = holder.itemView.findViewById<ImageView>(R.id.likeBackground)
        val userNickname= holder.itemView.findViewById<TextView>(R.id.userNickname)
        val userProfilePicture = holder.itemView.findViewById<ImageView>(R.id.userProfilePicture)

        userNickname.text = user["nickname"] as String
        Glide.with(context)
            .load(user["profilePictureID"] as String)
            .circleCrop().into(userProfilePicture)

        likeBackground.setOnClickListener {
            (context as PeopleYouMayKnowActivity).goToUserProfile(user["id"] as String)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }


}