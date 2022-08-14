package com.wasteleaf.wasteleaf_android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import com.wasteleaf.wasteleaf_android.util.Constants

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID, Constants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lightColor = Color.GREEN
                enableLights(true)
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}