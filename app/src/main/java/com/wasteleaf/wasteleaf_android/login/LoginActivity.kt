package com.wasteleaf.wasteleaf_android.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.wasteleaf.wasteleaf_android.R
import com.wasteleaf.wasteleaf_android.home.feed.HomeActivity
import com.wasteleaf.wasteleaf_android.util.Constants.deleteEmailPassword
import com.wasteleaf.wasteleaf_android.util.Constants.saveEmailPassword

class LoginActivity : AppCompatActivity() {

    private val googleSignInActivityCode: Int = 1

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var functions: FirebaseFunctions

    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var googleSignInButton: SignInButton
    private lateinit var emailInputLogin: TextInputEditText
    private lateinit var passwordInputLogin: EditText

    // Check if user is authenticated and redirect them to HomeActivity if they are
    override fun onStart() {
        super.onStart()

        val user = auth.currentUser
        if (user != null) {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        auth = FirebaseAuth.getInstance()
        functions = Firebase.functions

        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)
        googleSignInButton = findViewById(R.id.googleSigninButton)
        emailInputLogin = findViewById(R.id.emailInputLogin)
        passwordInputLogin = findViewById(R.id.passwordInputLogin)

        deleteEmailPassword(this)

        // Login button
        loginButton.setOnClickListener {
            val email =
                emailInputLogin.text.toString()
            val password = passwordInputLogin.text.toString()

            login(email, password)
        }

        // Register button
        registerButton.setOnClickListener {
            val email =
                emailInputLogin.text.toString()
            val password = passwordInputLogin.text.toString()

            register(email, password)
        }

        googleSignInButton.setOnClickListener {
            googleSighIn()
        }
    }

    private fun login(email: String, password: String) {
        when {
            email == "" -> {
                Toast.makeText(this, "Please enter your email...", Toast.LENGTH_SHORT).show()
            }
            password == "" -> {
                Toast.makeText(this, "Please enter the password...", Toast.LENGTH_SHORT).show()
            }
            else -> {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            goToHomeActivity()

                            saveEmailPassword(this, email, password)
                        } else {
                            Toast.makeText(
                                this,
                                task.exception!!.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }

    private fun register(email: String, password: String) {
        when {
            email == "" -> {
                Toast.makeText(this, "Please enter an email...", Toast.LENGTH_SHORT).show()
            }
            password == "" -> {
                Toast.makeText(this, "Please enter your password...", Toast.LENGTH_SHORT).show()
            }
            else -> {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (task.result.additionalUserInfo!!.isNewUser) {
                                recordNewUserToDB()
                            } else {
                                goToHomeActivity()
                            }
                            saveEmailPassword(this, email, password)
                        } else {
                            Toast.makeText(
                                this,
                                task.exception!!.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }

    // Google authentication
    private fun googleSighIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleSingInIntent = googleSignInClient.signInIntent
        startActivityForResult(googleSingInIntent, googleSignInActivityCode)
    }

    // Handle result from googleSingInIntent
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == googleSignInActivityCode) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                firebaseAuthWithGoogle(task.result.idToken)
            } catch (e: ApiException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (task.result.additionalUserInfo!!.isNewUser) {
                        recordNewUserToDB()
                    } else {
                        goToHomeActivity()
                    }
                } else {
                    Toast.makeText(this, task.exception!!.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun recordNewUserToDB() {
        Log.d("CEVA", "RECORD USER TO DB")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token: String
            if (!task.isSuccessful) {
                Log.w("ceva", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            } else {
                token = task.result
            }

            val user = auth.currentUser!!

            var name = user.displayName
            if (name == null) {
                name = user.email
            }

            val newUserMap = hashMapOf(
                "name" to name,
                "email" to user.email,
                "nickname" to name,
                "token" to token
            )

            functions.getHttpsCallable("addUser").call(newUserMap).continueWith {
                goToHomeActivity()
            }
        }
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}