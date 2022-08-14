package com.wasteleaf.wasteleaf_android.home.messages.chat

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.util.Constants.DELIMITER
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private lateinit var contactId: String
    private lateinit var messages: ArrayList<*>
    private lateinit var adapter: ChatAdapter

    private lateinit var loadingScreenLayout: ConstraintLayout
    private lateinit var loadingCircle: ImageView

    private lateinit var contactChatName: TextView
    private lateinit var contactChatImage: ImageView
    private lateinit var backgroundContactNickname: ImageView
    private lateinit var writeMessageInputText: EditText
    private lateinit var sendMessageButton: ImageView
    private lateinit var chatRecyclerView: RecyclerView

    companion object {
        var active = false

        private lateinit var instance: ChatActivity
        fun getInstance(): ChatActivity {
            return instance
        }
    }

    override fun onStart() {
        super.onStart()
        active = true
    }

    override fun onStop() {
        super.onStop()
        active = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        instance = this

        window.statusBarColor = ContextCompat.getColor(this, R.color.blue_background)

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        val extras = intent.extras
        val dataExtras = (extras!!.get("contactId") as String).split(DELIMITER)
        contactId = dataExtras[0]
        if (dataExtras.lastIndex != 0) {
            functions.getHttpsCallable("deleteNotification").call(hashMapOf("notificationID" to dataExtras[1]))
        }

        contactChatName = findViewById(R.id.contactChatName)
        contactChatImage = findViewById(R.id.contactChatImage)
        backgroundContactNickname = findViewById(R.id.backgroundContactNickname)
        writeMessageInputText = findViewById(R.id.writeMessageInputText)

        loadingScreenLayout = findViewById(R.id.loadingScreenLayout)
        loadingCircle = findViewById(R.id.loadingCircle)

        sendMessageButton = findViewById(R.id.sendMessageButton)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)

        // START ANIMATING THE LOADING CIRCLE
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                loadingCircle.rotation += 10
                mainHandler.postDelayed(this, 1)
            }
        })

        functions.getHttpsCallable("getMessages").call(hashMapOf("contactId" to contactId))
            .continueWith {
                val contactData = (it.result.data as HashMap<*, *>)["contactData"] as HashMap<*, *>
                messages = (it.result.data as HashMap<*, *>)["messages"] as ArrayList<*>

                adapter = ChatAdapter(messages, generateDaysUntilArray(messages))
                chatRecyclerView.adapter = adapter
                chatRecyclerView.layoutManager = LinearLayoutManager(this)
                (chatRecyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPosition(messages.size - 1)

                contactChatName.text = contactData["nickname"] as String
                Glide.with(this).load(contactData["profilePictureID"] as String)
                    .circleCrop().into(contactChatImage)

                deactivateLoadingScreen()
            }

        sendMessageButton.setOnClickListener {
            val textMessage = writeMessageInputText.text.toString()
            writeMessageInputText.setText("")

            if (textMessage != "") {
                val message: HashMap<*, *> = hashMapOf(
                    "text" to textMessage,
                    "timestamp" to hashMapOf(
                        "_seconds" to System.currentTimeMillis() / 1000
                    ),
                    "sentByMe" to true
                )

                val list = messages.toMutableList()
                list.add(message)
                messages = list as ArrayList<*>

                adapter = ChatAdapter(messages, generateDaysUntilArray(messages))
                chatRecyclerView.adapter = adapter
                chatRecyclerView.layoutManager = LinearLayoutManager(this)
                (chatRecyclerView.layoutManager as LinearLayoutManager)
                    .scrollToPosition(messages.size - 1)

                val messageData = hashMapOf(
                    "contactId" to contactId,
                    "text" to textMessage,
                )

                functions.getHttpsCallable("sendMessage").call(messageData)
            }
        }

        writeMessageInputText.setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                (chatRecyclerView.layoutManager as LinearLayoutManager).scrollToPosition(
                    messages.size - 1
                )
            }, 300)
        }
    }

    private fun generateDaysUntilArray(messages: ArrayList<*>): ArrayList<String> {
        val arr = arrayListOf<String>()
        val today = getTimeFormatted(currentTimeMillis())
        val yesterday = getTimeFormatted(currentTimeMillis() - 24L * 3600L * 1000L)
        messages.forEach { message ->
            val timestamp =
                (((message as HashMap<*, *>)["timestamp"] as HashMap<*, *>)["_seconds"].toString().toLong() * 1000)

            val dayName: String = when(getTimeFormatted(timestamp)) {
                today -> {
                    "Today"
                }
                yesterday -> {
                    "Yesterday"
                }
                else -> {
                    getTimeFormatted(timestamp)
                }
            }

            arr.add(dayName)
        }

        var lastDaysUntil = ""
        arr.forEachIndexed { index, daysUntil ->
            if (lastDaysUntil == daysUntil) {
                arr[index] = ""
            } else {
                lastDaysUntil = daysUntil
            }
        }

        return arr
    }

    private fun getTimeFormatted(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy").format(Date(timestamp))
    }

    private fun deactivateLoadingScreen() {
        loadingScreenLayout.visibility = INVISIBLE

        writeMessageInputText.visibility = VISIBLE
        sendMessageButton.visibility = VISIBLE
        backgroundContactNickname.visibility = VISIBLE

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    fun addMessage(textMessage: String) {
        runOnUiThread {
            val message: HashMap<*, *> = hashMapOf(
                "text" to textMessage,
                "timestamp" to hashMapOf(
                    "_seconds" to currentTimeMillis() / 1000
                ),
                "sentByMe" to false
            )

            val list = messages.toMutableList()
            list.add(message)
            messages = list as ArrayList<*>

            adapter = ChatAdapter(messages, generateDaysUntilArray(messages))
            chatRecyclerView.adapter = adapter
            chatRecyclerView.layoutManager = LinearLayoutManager(this)
            (chatRecyclerView.layoutManager as LinearLayoutManager)
                .scrollToPosition(messages.size - 1)
        }
    }
}
