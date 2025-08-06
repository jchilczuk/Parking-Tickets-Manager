package com.example.ARIUS_projekt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

/**
 * Main screen of the application shown after a successful login.
 * Displays a welcome message and navigation buttons to other parts of the app.
 */
public class MainActivity extends Activity {

    /**
     * Called when the activity is starting. Sets up the UI and button listeners.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the most recent data.
     */
    @SuppressLint("SetTextI18n") // Suppresses warning about hardcoded strings being concatenated in setText()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout file to be displayed in this activity.
        setContentView(R.layout.activity_main);

        // Retrieve user details from SharedPreferences to personalize the welcome message.
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        String firstName = prefs.getString("firstName", "");
        String lastName = prefs.getString("lastName", "");

        // Reference to the TextView that displays the user's welcome message.
        TextView userInfo = findViewById(R.id.userInfoText);
        userInfo.setText("Witaj " + firstName + " " + lastName + "!"); // "Welcome [firstName lastName]!"

        // Set up the logout button
        Button logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(v -> {
            // Clear stored user data (including token and FCM info)
            getSharedPreferences("user", MODE_PRIVATE).edit().clear().apply();

            // Navigate back to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));

            // Finish this activity so the user can't go back to it with the back button
            finish();
        });

        // Set up the button for navigating to the AddTicketActivity
        Button addTicketButton = findViewById(R.id.buttonAddTicket);
        addTicketButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddTicketActivity.class))
        );

        // Set up the button for navigating to the ViewTicketsActivity
        Button viewTicketsButton = findViewById(R.id.buttonViewTickets);
        viewTicketsButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ViewTicketsActivity.class))
        );
    }
}
