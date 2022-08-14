package com.wasteleaf.wasteleaf_android.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.ContextCompat
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.HomeActivity
import com.wasteleaf.wasteleaf_android.util.Constants.PRIVACY_POLICY_URL
import com.wasteleaf.wasteleaf_android.util.Constants.updatePrivacyPolicyStatus

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        webView = findViewById(R.id.webView)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)

        webView.loadUrl(PRIVACY_POLICY_URL)

        acceptButton.setOnClickListener {
            updatePrivacyPolicyStatus(this, true)

            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        rejectButton.setOnClickListener {
            updatePrivacyPolicyStatus(this, false)
            Toast.makeText(
                this,
                "You have to accept the privacy policy in order to use the app!",
                LENGTH_SHORT
            ).show()
            onBackPressed()
        }
    }
}