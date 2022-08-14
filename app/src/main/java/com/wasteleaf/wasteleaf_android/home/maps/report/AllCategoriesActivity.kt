package com.wasteleaf.wasteleaf_android.home.maps.report

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wasteleaf.wasteleaf_android.R

class AllCategoriesActivity : AppCompatActivity() {
    private lateinit var closeButton: Button
    private lateinit var organicButton: ImageView
    private lateinit var glassButton: ImageView
    private lateinit var paperButton: ImageView
    private lateinit var plasticButton: ImageView
    private lateinit var textileButton: ImageView
    private lateinit var metalButton: ImageView
    private lateinit var ewasteButton: ImageView
    private lateinit var tireButton: ImageView
    private lateinit var dumpsterButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_categories)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        declareAllUIComponents()
        addLogicToAllUIComponents()
    }

    private fun declareAllUIComponents() {
        closeButton = findViewById(R.id.closeButton)
        organicButton = findViewById(R.id.organicButton)
        glassButton = findViewById(R.id.glassButton)
        paperButton = findViewById(R.id.paperButton)
        plasticButton = findViewById(R.id.plasticButton)
        textileButton = findViewById(R.id.textileButton)
        metalButton = findViewById(R.id.metalButton)
        ewasteButton = findViewById(R.id.ewasteButton)
        tireButton = findViewById(R.id.tireButton)
        dumpsterButton = findViewById(R.id.dumpsterButton)
    }

    private fun addLogicToAllUIComponents() {
        organicButton.setOnClickListener {
            sendDataBack("organic")
        }

        glassButton.setOnClickListener {
            sendDataBack("glass")
        }

        paperButton.setOnClickListener {
            sendDataBack("paper")
        }

        plasticButton.setOnClickListener {
            sendDataBack("plastic")
        }

        textileButton.setOnClickListener {
            sendDataBack("textile")
        }

        metalButton.setOnClickListener {
            sendDataBack("metal")
        }

        ewasteButton.setOnClickListener {
            sendDataBack("e-waste")
        }

        tireButton.setOnClickListener {
            sendDataBack("tire")
        }

        dumpsterButton.setOnClickListener {
            sendDataBack("dumpster")
        }

        closeButton.setOnClickListener {
            onBackPressed()
        }
    }

    // Send a response to ReportActivity with the chosen category
    private fun sendDataBack(category: String) {
        val intent = Intent()
        intent.putExtra("chosenCategory", category)
        setResult(RESULT_OK, intent)
        finish()
    }
}