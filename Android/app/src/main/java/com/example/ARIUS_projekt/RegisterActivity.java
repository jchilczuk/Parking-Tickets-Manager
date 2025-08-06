package com.example.ARIUS_projekt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * Activity responsible for handling user registration.
 * It collects input, validates fields, and sends a request to the backend server.
 */
public class RegisterActivity extends Activity {

    // Tag used for logging errors and debugging
    private static final String TAG = "RegisterActivity";

    // UI components for user input
    private EditText firstNameInput, lastNameInput, emailInput, passwordInput, confirmPasswordInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout view to the registration screen
        setContentView(R.layout.activity_register);

        // Bind XML input fields and button to Java variables
        firstNameInput = findViewById(R.id.editTextFirstName);
        lastNameInput = findViewById(R.id.editTextLastName);
        emailInput = findViewById(R.id.editTextEmail);
        passwordInput = findViewById(R.id.editTextPassword);
        confirmPasswordInput = findViewById(R.id.editTextConfirmPassword);
        Button registerButton = findViewById(R.id.buttonRegister);

        // Set click listener for registration button
        registerButton.setOnClickListener(v -> {
            // Extract and trim input values from text fields
            String firstName = firstNameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString(); // Passwords shouldn't be trimmed
            String confirmPassword = confirmPasswordInput.getText().toString();

            // Step 1: Validate presence of all fields
            if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                    TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                GeneralUtils.showToast(this, "Uzupełnij wszystkie pola");
                return;
            }

            // Step 2: Confirm that password fields match
            if (!password.equals(confirmPassword)) {
                GeneralUtils.showToast(this, "Hasła nie są takie same");
                return;
            }

            // Step 3: Inputs are valid – start registration task
            new RegisterUserTask(firstName, lastName, email, password).execute();
        });
    }

    /**
     * AsyncTask handles user registration in the background
     * without blocking the main UI thread.
     */
    @SuppressLint("StaticFieldLeak")
    private class RegisterUserTask extends AsyncTask<Void, Void, String> {

        private final String firstName, lastName, email, password;
        private int responseCode = -1; // Used to track HTTP response from server

        /**
         * Constructor to pass user input values into the AsyncTask
         */
        public RegisterUserTask(String firstName, String lastName, String email, String password) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.password = password;
        }

        /**
         * This method runs on a background thread.
         * It performs the HTTP request and returns the server response.
         */
        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Create a POST connection using the general utility method
                HttpURLConnection conn = GeneralUtils.getHttpConnection(
                        "http://10.0.2.2:5000/auth/register", "POST", 5000, 30000
                );

                // Build a JSON object with registration data
                JSONObject json = new JSONObject();
                json.put("name", firstName);
                json.put("surname", lastName);
                json.put("email", email);
                json.put("password", password);

                // Write JSON data to request body
                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                // Capture server response code (e.g., 201 or 400)
                responseCode = conn.getResponseCode();

                // Read response body depending on success or error
                InputStream input = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

                // Return raw server response (expected to be JSON)
                return result.toString();

            } catch (Exception e) {
                // Log any issues that occur during the network request
                Log.e(TAG, "Exception during registration", e);
                return null;
            }
        }

        /**
         * This method runs on the UI thread after background execution is done.
         * It processes the result from the server.
         */
        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                // If server returned null or failed connection
                GeneralUtils.showToast(RegisterActivity.this, "Błąd serwera");
                return;
            }

            try {
                // If registration was successful (201 Created)
                if (responseCode == 201) {
                    GeneralUtils.showToast(RegisterActivity.this, "Zarejestrowano pomyślnie");
                    // Navigate to login screen
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish(); // Close this activity to prevent returning via back button
                } else {
                    // On failure, extract the server's error message
                    JSONObject errorJson = new JSONObject(result);
                    String errorMessage = errorJson.optString("msg", "Błąd podczas rejestracji");
                    GeneralUtils.showToast(RegisterActivity.this, errorMessage);
                }
            } catch (Exception e) {
                // Catch any exceptions thrown while parsing response
                Log.e(TAG, "Error parsing server response", e);
                GeneralUtils.showToast(RegisterActivity.this, "Nieoczekiwany błąd");
            }
        }
    }
}
