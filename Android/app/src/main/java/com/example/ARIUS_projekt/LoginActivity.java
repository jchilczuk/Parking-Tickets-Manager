package com.example.ARIUS_projekt;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Activity responsible for handling user login logic.
 * This version uses AsyncTask to perform network operations in the background.
 */
public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity"; // Tag used for logging

    // UI elements for email and password inputs
    private EditText emailInput, passwordInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Load layout for login screen

        // Initialize input fields and buttons
        emailInput = findViewById(R.id.editTextEmail);
        passwordInput = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.buttonLogin);
        Button registerButton = findViewById(R.id.buttonRegister);

        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            // Basic validation for empty fields
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                GeneralUtils.showToast(this, "Wypełnij wszystkie pola");
            } else {
                // Start AsyncTask for logging in
                new LoginTask().execute(email, password);
            }
        });

        // Navigate to RegisterActivity when register button is clicked
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    /**
     * AsyncTask to handle login in the background.
     * This task sends the credentials to the backend, handles the response,
     * and stores user session details locally.
     */
    private class LoginTask extends AsyncTask<String, Void, Boolean> {

        private String token;     // JWT token returned from the server
        private String name;      // First name of the user
        private String surname;   // Last name of the user
        private String email;     // Email entered by the user
        private String errorMsg = "Błąd logowania"; // Default error message

        @Override
        protected Boolean doInBackground(String... params) {
            email = params[0];           // User's email
            String password = params[1]; // User's password

            try {
                // Configure HTTP POST request to the login endpoint
                HttpURLConnection conn = GeneralUtils.getHttpConnection(
                        "http://10.0.2.2:5000/auth/login", // Localhost for emulator
                        "POST", 5000, 30000
                );

                // Build JSON payload for login
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                // Write JSON to request body
                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                // Read response code and body
                int responseCode = conn.getResponseCode();
                InputStream input = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder response = new StringBuilder();
                String line;

                // Read server response line by line
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                conn.disconnect(); // Always disconnect connection after use

                // If login is successful (HTTP 200), extract token and user data
                if (responseCode == 200) {
                    JSONObject obj = new JSONObject(response.toString());
                    name = obj.getString("name");
                    surname = obj.getString("surname");
                    token = obj.getString("access_token");
                    return true;
                } else {
                    // If login failed, parse error message
                    JSONObject errorJson = new JSONObject(response.toString());
                    errorMsg = errorJson.optString("msg", "Błąd logowania");
                    return false;
                }

            } catch (Exception e) {
                // Log any network or parsing errors
                Log.e(TAG, "Login error", e);
                errorMsg = "Błąd serwera"; // Override error message with server error
                return false;
            }
        }

        /**
         * Runs on the main thread after the background task finishes.
         * Shows a success or error message, saves session, and navigates.
         */
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // Store user information and token in SharedPreferences for persistent login
                SharedPreferences.Editor editor = getSharedPreferences("user", MODE_PRIVATE).edit();
                editor.putString("firstName", name);
                editor.putString("lastName", surname);
                editor.putString("email", email);
                editor.putString("token", token);
                editor.apply(); // Asynchronous apply

                // Notify user of success and send FCM token to backend
                GeneralUtils.showToast(LoginActivity.this, "Zalogowano pomyślnie");
                fetchAndSendFcmToken(token); // Send Firebase token

                // Launch main activity and finish login activity
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();

            } else {
                // Show error message on failure
                GeneralUtils.showToast(LoginActivity.this, errorMsg);
            }
        }
    }

    /**
     * Fetches the Firebase Cloud Messaging (FCM) token for push notifications.
     * Once received, it saves the token locally and sends it to the backend server.
     *
     * @param jwtToken JSON Web Token used for authenticating the request.
     */
    private void fetchAndSendFcmToken(String jwtToken) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // Log failure if FCM token couldn't be retrieved
                        Log.w(TAG, "FCM token fetch failed", task.getException());
                        return;
                    }

                    // Successfully received FCM token
                    String fcmToken = task.getResult();
                    SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
                    prefs.edit()
                            .putString("fcm_token", fcmToken)  // Save token
                            .putBoolean("tokenSent", false)    // Mark token as not yet sent
                            .apply();

                    // Send token to backend using a utility method
                    TokenUtils.sendTokenToBackend(this, fcmToken, jwtToken);
                });
    }
}
