package com.example.ARIUS_projekt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity responsible for displaying full details of a specific parking ticket,
 * including metadata (date, location, vehicle number) and its associated image (if available).
 */
public class TicketDetailsActivity extends AppCompatActivity {

    private static final String TAG = "TicketDetailsActivity"; // Logging tag for debug purposes
    private int ticketId; // ID of the ticket retrieved from intent extras

    // UI references
    private TextView textDateTime, textLicense, textLocation, imagePlaceholderText;
    private ImageView ticketImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_details);

        // Bind views from XML layout
        textDateTime = findViewById(R.id.textDateTime);
        textLicense = findViewById(R.id.textLicense);
        textLocation = findViewById(R.id.textLocation);
        ticketImage = findViewById(R.id.ticketImage);
        imagePlaceholderText = findViewById(R.id.imagePlaceholderText);
        Button deleteButton = findViewById(R.id.buttonDelete);

        // Show back arrow in the ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Retrieve ticket ID passed via Intent
        ticketId = getIntent().getIntExtra("ticket_id", -1);
        if (ticketId == -1) {
            // Invalid or missing ID - inform user and exit
            GeneralUtils.showToast(this, "Invalid ticket ID");
            finish();
            return;
        }

        // Begin async ticket detail fetch
        new FetchTicketTask().execute(ticketId);

        // Set delete button action
        deleteButton.setOnClickListener(v -> new DeleteTicketTask().execute(ticketId));
    }

    /**
     * AsyncTask for downloading and parsing ticket details.
     * Runs in the background and updates UI upon completion.
     */
    private class FetchTicketTask extends AsyncTask<Integer, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Integer... params) {
            int id = params[0];
            String token = getSharedPreferences("user", MODE_PRIVATE).getString("token", "");

            try {
                // Authenticated GET request to fetch ticket metadata
                HttpURLConnection conn = GeneralUtils.getHttpConnectionWithAuth(
                        "http://10.0.2.2:5000/ticket/" + id,
                        "GET", 5000, 30000, token
                );

                int code = conn.getResponseCode();
                InputStream input = (code < 400) ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                conn.disconnect();

                if (code == 200) {
                    // Ticket metadata fetched, now fetch image asynchronously
                    fetchImageAsync(id);
                    return new JSONObject(response.toString());
                } else {
                    Log.e(TAG, "Failed to fetch ticket: " + response);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in FetchTicketTask", e);
            }

            return null; // Indicates failure
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null) {
                populateTicketUI(result); // Display ticket info
            } else {
                GeneralUtils.showToast(TicketDetailsActivity.this, "Błąd podczas wczytywania danych");
            }
        }
    }

    /**
     * AsyncTask for sending a DELETE request to remove the ticket.
     * Updates UI depending on server response.
     */
    private class DeleteTicketTask extends AsyncTask<Integer, Void, Boolean> {
        private int ticketId;

        @Override
        protected Boolean doInBackground(Integer... params) {
            this.ticketId = params[0];
            String token = getSharedPreferences("user", MODE_PRIVATE).getString("token", "");

            try {
                HttpURLConnection conn = GeneralUtils.getHttpConnectionWithAuth(
                        "http://10.0.2.2:5000/ticket/" + ticketId,
                        "DELETE", 5000, 30000, token
                );
                int code = conn.getResponseCode();
                conn.disconnect();
                return code == 200 || code == 204; // Server returned OK or No Content
            } catch (Exception e) {
                Log.e(TAG, "Exception in DeleteTicketTask", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // Notify user and exit activity with result
                GeneralUtils.showToast(TicketDetailsActivity.this, "Bilet został usunięty");
                Intent resultIntent = new Intent();
                resultIntent.putExtra("deleted_ticket_id", ticketId);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                GeneralUtils.showToast(TicketDetailsActivity.this, "Błąd podczas usuwania biletu");
            }
        }
    }

    /**
     * Fetches the ticket's image asynchronously using built-in thread pool.
     * Parses Base64 encoded image into Bitmap.
     */
    private void fetchImageAsync(int id) {
        AsyncTask.execute(() -> {
            String token = getSharedPreferences("user", MODE_PRIVATE).getString("token", "");
            try {
                HttpURLConnection conn = GeneralUtils.getHttpConnectionWithAuth(
                        "http://10.0.2.2:5000/ticket/" + id + "/image",
                        "GET", 5000, 30000, token
                );

                int code = conn.getResponseCode();
                InputStream input = (code < 400) ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                if (code == 200) {
                    JSONObject obj = new JSONObject(sb.toString());
                    String imageBase64 = obj.optString("image_base64", "");
                    if (!imageBase64.isEmpty()) {
                        byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        runOnUiThread(() -> {
                            ticketImage.setVisibility(ImageView.VISIBLE);
                            imagePlaceholderText.setVisibility(TextView.GONE);
                            ticketImage.setImageBitmap(bitmap);
                        });
                    } else {
                        runOnUiThread(() -> {
                            ticketImage.setVisibility(ImageView.GONE);
                            imagePlaceholderText.setVisibility(TextView.VISIBLE);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        ticketImage.setVisibility(ImageView.GONE);
                        imagePlaceholderText.setVisibility(TextView.VISIBLE);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching image", e);
                runOnUiThread(() -> {
                    ticketImage.setVisibility(ImageView.GONE);
                    imagePlaceholderText.setVisibility(TextView.VISIBLE);
                });
            }
        });
    }

    /**
     * Updates UI views with values retrieved from ticket JSON.
     */
    @SuppressLint("SetTextI18n")
    private void populateTicketUI(JSONObject obj) {
        try {
            String date = obj.getString("date");
            String time = obj.getString("time");
            String license = obj.getString("vehicle_number");
            String location = obj.getString("location");

            // Format date into localized human-readable format
            String formattedDate = date + ", " + time;
            try {
                Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date);
                if (parsed != null) {
                    formattedDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsed) + ", " + time;
                }
            } catch (Exception e) {
                Log.w(TAG, "Date parse failed: " + date);
            }

            // Set data into TextViews
            textDateTime.setText("Termin ważności: " + formattedDate);
            textLicense.setText("Numer rejestracyjny pojazdu: " + license);
            textLocation.setText("Lokalizacja: " + location);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ticket JSON", e);
            GeneralUtils.showToast(this, "Niepoprawne dane biletu");
        }
    }

    /**
     * Handles ActionBar "Up" navigation (back arrow).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Simulate back button press
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
