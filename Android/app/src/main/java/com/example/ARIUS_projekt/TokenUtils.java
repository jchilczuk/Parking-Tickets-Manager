package com.example.ARIUS_projekt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Utility class responsible for sending the Firebase Cloud Messaging (FCM) token
 * to the backend server after successful login.
 * Uses AsyncTask to offload networking work to a background thread.
 */
public class TokenUtils {

    private static final String TAG = "TokenUtils";

    /**
     * Public entry point for initiating the FCM token registration.
     *
     * @param context  The calling context (should use application context internally).
     * @param fcmToken The FCM token to be registered with the backend.
     * @param jwtToken The JWT access token used for authentication.
     */
    public static void sendTokenToBackend(Context context, String fcmToken, String jwtToken) {
        new SendTokenTask(context, fcmToken, jwtToken).execute();
    }

    /**
     * Inner class extending AsyncTask to handle background HTTP communication
     * for sending the FCM token securely and asynchronously.
     */
    private static class SendTokenTask extends AsyncTask<Void, Void, Boolean> {

        private final Context context;
        private final String fcmToken;
        private final String jwtToken;

        /**
         * Constructor that accepts the required context and tokens.
         *
         * @param context  The context (automatically converted to application context).
         * @param fcmToken The FCM token string to be sent.
         * @param jwtToken The authorization token for Bearer authentication.
         */
        SendTokenTask(Context context, String fcmToken, String jwtToken) {
            this.context = context.getApplicationContext(); // Prevent memory leaks
            this.fcmToken = fcmToken;
            this.jwtToken = jwtToken;
        }

        /**
         * Performs the actual network request in the background.
         *
         * @param voids No parameters needed.
         * @return True if the token was sent successfully, false otherwise.
         */
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Create an authenticated HTTP POST connection using GeneralUtils
                HttpURLConnection conn = GeneralUtils.getHttpConnectionWithAuth(
                        "http://10.0.2.2:5000/auth/register_token",
                        "POST",
                        5000,
                        15000,
                        jwtToken
                );

                // Build JSON payload with the FCM token
                JSONObject json = new JSONObject();
                json.put("fcm_token", fcmToken);

                // Write the JSON data to the request output stream
                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                // Log and evaluate the response code
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "FCM token send response code: " + responseCode);
                conn.disconnect();

                // Consider the operation successful if server responded with 2xx
                return responseCode >= 200 && responseCode < 300;

            } catch (Exception e) {
                Log.e(TAG, "Exception while sending FCM token", e);
                return false;
            }
        }

        /**
         * Runs on the main thread after `doInBackground` completes.
         * Updates SharedPreferences if the request was successful.
         *
         * @param success True if token registration succeeded.
         */
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // Save a flag in SharedPreferences indicating successful transmission
                SharedPreferences.Editor editor = context
                        .getSharedPreferences("user", Context.MODE_PRIVATE)
                        .edit();
                editor.putBoolean("tokenSent", true).apply();

                Log.d(TAG, "FCM token marked as sent in SharedPreferences");
            } else {
                Log.w(TAG, "Failed to send FCM token to backend");
            }
        }
    }
}
