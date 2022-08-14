package com.wasteleaf.wasteleaf_android.home.feed.searchuser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.peopleyoumayknow.PeopleYouMayKnowActivity

class SearchUserAdapter(
    private var users: ArrayList<*>
) : RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder>() {
    private lateinit var context: Context

    inner class SearchUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUserViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_user_item, parent, false)
        return SearchUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchUserViewHolder, position: Int) {
        val user = users[position] as HashMap<*, *>

        val searchUserBackground = holder.itemView.findViewById<ImageView>(R.id.searchUserBackground)
        val userNickname= holder.itemView.findViewById<TextView>(R.id.userNickname)
        val userProfilePicture = holder.itemView.findViewById<ImageView>(R.id.userProfilePicture)

        userNickname.text = user["nickname"] as String
        Glide.with(context)
            .load(user["profilePictureID"] as String)
            .circleCrop().into(userProfilePicture)

        searchUserBackground.setOnClickListener {
            (context as SearchUserActivity).goToUserProfile(user["id"] as String)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}