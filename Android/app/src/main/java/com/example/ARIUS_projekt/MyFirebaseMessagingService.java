package com.example.ARIUS_projekt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Custom Firebase Messaging Service to handle FCM token creation and message reception.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "ticket_alerts"; // Notification channel ID
    private static final String TAG = "FCM"; // Tag for logging

    /**
     * Called when a new FCM registration token is generated.
     * @param token The new FCM token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token); // Log the new token for debugging

        // Save the token locally using SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        prefs.edit()
                .putString("fcm_token", token)  // Save the new token
                .putBoolean("tokenSent", false) // Mark as not sent to backend yet
                .apply();

        // Retrieve JWT token for authentication
        String jwtToken = prefs.getString("token", "");
        if (!jwtToken.isEmpty()) {
            // Send the FCM token to your backend if user is logged in
            TokenUtils.sendTokenToBackend(this, token, jwtToken);
        }
    }

    /**
     * Called when an FCM message is received.
     * @param remoteMessage The message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Extract notification title and body from the message
        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : "Alert";
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : "";

        // Display the notification to the user
        showNotification(title, body);
    }

    /**
     * Builds and shows a notification using NotificationCompat.
     * Handles backward compatibility with Android O and above.
     * @param title The notification title.
     * @param body The notification message body.
     */
    private void showNotification(String title, String body) {
        // Get the system NotificationManager
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // For Android 8.0 (API 26) and above, create a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ticket Alerts", // Channel name visible in app settings
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel); // Register the channel
        }

        // Intent to launch MainActivity when the user taps the notification
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE // Ensures security on modern Android versions
        );

        // Build the notification using NotificationCompat for backward compatibility
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)                      // Set title
                .setContentText(body)                        // Set message body
                .setSmallIcon(R.drawable.ic_notification)    // Set small icon (must exist in resources)
                .setAutoCancel(true)                         // Automatically remove when tapped
                .setContentIntent(pendingIntent);            // Set the intent to launch on click

        // Display the notification with a unique ID
        manager.notify(1001, builder.build());
    }
}
