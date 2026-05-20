package com.traffic_guard.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.traffic_guard.ai.data.TrafficGuardApiClient
import com.traffic_guard.ai.data.FcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrafficGuardFCMService : FirebaseMessagingService() {

    private val tag = "TrafficGuardFCMService"
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(tag, "Refreshed FCM registration token: $token")
        uploadFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(tag, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(tag, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "Emergency Alert", it.body ?: "Alert detail received.")
        }

        // Check if message contains a data payload (custom fields).
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(tag, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Emergency SOS"
            val problem = remoteMessage.data["problem"] ?: "A driver needs assistance."
            val reporter = remoteMessage.data["reporter"] ?: "Nearby Driver"
            sendNotification(title, "$reporter needs help: $problem")
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "sos_alerts_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Emergency SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent SOS notifications from nearby drivers"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun uploadFcmToken(token: String) {
        scope.launch {
            try {
                // Instantly sync token with the backend REST api client
                val response = TrafficGuardApiClient.service.postFcmToken(FcmTokenRequest(token))
                Log.i(tag, "Successfully uploaded FCM token to backend: ${response.message}")
            } catch (e: Exception) {
                Log.w(tag, "Failed to upload FCM token to backend: ${e.message}")
            }
        }
    }
}
