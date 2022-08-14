package com.wasteleaf.wasteleaf_android.home.feed.searchuser

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.peopleyoumayknow.PeopleYouMayKnowAdapter
import com.wasteleaf.wasteleaf_android.home.user.profile.UserProfileActivity

class SearchUserActivity : AppCompatActivity() {
    private lateinit var functions: FirebaseFunctions
    private lateinit var adapter: SearchUserAdapter

    private lateinit var searchUsersRecyclerView: RecyclerView
    private lateinit var searchUserText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        functions = Firebase.functions

        searchUsersRecyclerView = findViewById(R.id.searchUsersRecyclerView)
        searchUserText = findViewById(R.id.searchUserText)

        searchUserText.doAfterTextChanged {
            searchUser(it.toString())
        }
    }

    private fun searchUser(searchStr: String) {
        //if (searchStr.length < 3) return
        functions.getHttpsCallable("searchUser").call(hashMapOf("searchStr" to searchStr)).continueWith {
            val users = (it.result.data as HashMap<*, *>)["users"] as ArrayList<*>

            adapter = SearchUserAdapter(users)
            searchUsersRecyclerView.adapter = adapter
            searchUsersRecyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    fun goToUserProfile(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }
}