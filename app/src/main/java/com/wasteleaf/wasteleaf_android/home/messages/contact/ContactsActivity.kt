package com.wasteleaf.wasteleaf_android.home.messages.contact

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.View.GONE
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.R

class ContactsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private lateinit var hintTextView: TextView
    private lateinit var contactsRecyclerView: RecyclerView

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        hintTextView = findViewById(R.id.hintTextView)
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView)

        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        // START ANIMATING THE LOADING CIRCLE
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                loadingCircle.rotation += 10
                mainHandler.postDelayed(this, 1)
            }
        })

        functions.getHttpsCallable("getContacts").call().continueWith { it ->
            val contacts = (it.result.data as HashMap<*, *>)["contactsData"] as ArrayList<*>
            contacts.sortBy { contact ->
                ((contact as HashMap<*, *>)["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong()
            }
            contacts.reverse()

            val adapter = ContactAdapter(contacts)
            contactsRecyclerView.adapter = adapter
            contactsRecyclerView.layoutManager = LinearLayoutManager(this)

            deactivateLoadingScreen()

            if (contacts.size != 0) {
                hintTextView.visibility = GONE
            }
        }
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = View.INVISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }
}