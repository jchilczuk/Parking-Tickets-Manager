package com.example.ARIUS_projekt;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for adding a new ticket.
 * This activity provides a form for the user to input ticket details such as date, time,
 * license plate number, location, and an optional photo.
 * The entered data is then sent to a backend server.
 */
public class AddTicketActivity extends AppCompatActivity {

    // UI elements for inputting ticket data and actions.
    EditText dateInput, timeInput, licenseInput, locationInput; // Input fields for ticket details.
    Button saveButton, photoButton; // Buttons for saving the ticket and adding a photo.
    ImageView photoPreview; // ImageView to display a preview of the selected photo.
    Uri imageUri; // URI of the selected image, if any.

    // Request code for picking an image from the gallery.
    static final int REQUEST_IMAGE_PICK = 1;
    // TAG for logging purposes, used to identify logs from this activity.
    private static final String TAG = "AddTicketActivity";

    /**
     * Called when the activity is first created.
     * This method initializes the UI, sets up listeners for UI elements,
     * and configures the ActionBar.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
    @SuppressLint({"ClickableViewAccessibility", "IntentReset"}) // Suppressing lint for onTouch listener and intent type.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity from activity_add_ticket.xml.
        setContentView(R.layout.activity_add_ticket);

        // Initialize UI elements by finding them by their IDs in the layout.
        dateInput = findViewById(R.id.editTextDate);
        timeInput = findViewById(R.id.editTextTime);
        licenseInput = findViewById(R.id.editTextLicense);
        locationInput = findViewById(R.id.editTextLocation);
        saveButton = findViewById(R.id.buttonSaveTicket);
        photoButton = findViewById(R.id.buttonAddPhoto);
        photoPreview = findViewById(R.id.imagePreview);

        // Enable the "Up" button (back arrow) in the ActionBar if an ActionBar is present.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set up listener for the date input field to show DatePickerDialog on focus or touch.
        dateInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker();
        });
        dateInput.setOnTouchListener((v, event) -> {
            // Show DatePickerDialog when the field is touched down.
            if (event.getAction() == MotionEvent.ACTION_DOWN) showDatePicker();
            return true; // Consume the touch event.
        });

        // Set up listener for the time input field to show TimePickerDialog on focus or touch.
        timeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showTimePicker();
        });
        timeInput.setOnTouchListener((v, event) -> {
            // Show TimePickerDialog when the field is touched down.
            if (event.getAction() == MotionEvent.ACTION_DOWN) showTimePicker();
            return true; // Consume the touch event.
        });

        // Set OnClickListener for the save button.
        saveButton.setOnClickListener(v -> {
            // Retrieve and trim text from input fields.
            String date = dateInput.getText().toString().trim();
            String time = timeInput.getText().toString().trim();
            String license = licenseInput.getText().toString().trim();
            String location = locationInput.getText().toString().trim();

            // Validate that all required fields are filled.
            if (date.isEmpty() || time.isEmpty() || license.isEmpty() || location.isEmpty()) {
                GeneralUtils.showToast(AddTicketActivity.this, "Wypełnij wszystkie pola"); // "Fill all fields"
                return; // Stop further execution if validation fails.
            }

            // Convert the selected image URI to a Base64 encoded string, if an image was selected.
            String imageBase64 = imageUri != null ? getBase64FromUri(imageUri) : null;

            // Execute an AsyncTask to send the ticket data to the backend server.
            // Network operations must not be done on the main UI thread.
            new SendTicketTask(date, time, license, location, imageBase64).execute();
        });

        // Set OnClickListener for the photo button to allow the user to pick an image.
        photoButton.setOnClickListener(v -> {
            // Create an Intent to pick an image from the device's external storage (gallery).
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*"); // Specify that only images should be selectable.
            // Start the activity to pick an image and expect a result.
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });
    }

    /**
     * AsyncTask to send the new ticket data to the backend server.
     * This class handles the network operation in a background thread and updates the UI
     * on the main thread based on the outcome.
     */
    private class SendTicketTask extends AsyncTask<Void, Void, Boolean> {

        // Data for the new ticket.
        private final String date, time, license, location, imageBase64;
        // Error message to display if the operation fails. Default message is set.
        private String errorMsg = "Błąd połączenia z serwerem"; // "Server connection error"

        /**
         * Constructor for SendTicketTask.
         * @param date The date of the ticket.
         * @param time The time of the ticket.
         * @param license The license plate number.
         * @param location The location of the incident.
         * @param imageBase64 The Base64 encoded string of the image, or null if no image.
         */
        SendTicketTask(String date, String time, String license, String location, String imageBase64) {
            this.date = date;
            this.time = time;
            this.license = license;
            this.location = location;
            this.imageBase64 = imageBase64;
        }

        /**
         * Performs the network operation in the background.
         * This method constructs a JSON payload with the ticket data and sends it
         * as a POST request to the backend server.
         * @param voids No parameters are used for this background task.
         * @return True if the ticket was successfully sent and a 201 (Created) response was received,
         *         false otherwise.
         */
        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                // Retrieve JWT token from shared preferences.
                SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
                String jwtToken = prefs.getString("token", "");

                // Get a configured connection using GeneralUtils with auth.
                conn = GeneralUtils.getHttpConnectionWithAuth(
                        "http://10.0.2.2:5000/ticket",
                        "POST",
                        5000,
                        15000,
                        jwtToken
                );

                // Create JSON payload.
                JSONObject json = new JSONObject();
                json.put("vehicle_number", license);
                json.put("location", location);
                json.put("date", date);
                json.put("time", time);
                json.put("notified", false);
                json.put("uploaded_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                if (imageBase64 != null) {
                    json.put("image_base64", imageBase64);
                }

                // Send the payload.
                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                // Read response from the server.
                int responseCode = conn.getResponseCode();
                InputStream input = (responseCode < 400) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d(TAG, "Response from server: " + response);

                if (responseCode == 201) {
                    return true;
                } else {
                    JSONObject errorJson = new JSONObject(response.toString());
                    errorMsg = errorJson.optString("msg", "Błąd podczas zapisywania danych");
                    return false;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending ticket", e);
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        /**
         * Executes on the UI thread after the background computation finishes.
         * This method updates the UI based on the success or failure of the SendTicketTask.
         * @param success True if the ticket was successfully sent, false otherwise.
         */
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                GeneralUtils.showToast(AddTicketActivity.this, "Dodano bilet"); // "Ticket added"
                clearForm(); // Clear the input form for the next entry.
            } else {
                GeneralUtils.showToast(AddTicketActivity.this, errorMsg); // Show the specific error message.
            }
        }
    }

    /**
     * Converts the content of a given URI (typically an image) into a Base64 encoded string.
     * This is used to send image data as part of a JSON payload.
     *
     * @param uri The URI of the content (image) to be converted.
     * @return A Base64 encoded string representation of the image, or null if conversion fails.
     */
    private String getBase64FromUri(Uri uri) {
        InputStream inputStream = null;
        try {
            // Get an InputStream from the content URI.
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open InputStream for URI: " + uri);
                return null;
            }

            // Decode the InputStream into a Bitmap.
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode Bitmap from InputStream for URI: " + uri);
                return null;
            }

            // Compress the Bitmap into a JPEG format and write it to a ByteArrayOutputStream.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream); // Compress with 80% quality.
            byte[] byteArray = outputStream.toByteArray(); // Get the byte array.

            // Encode the byte array into a Base64 string without wrapping.
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);

        } catch (Exception e) {
            // Log any exceptions that occur during the conversion process.
            Log.e(TAG, "Error converting image URI to base64", e);
            return null; // Return null if conversion fails.
        } finally {
            // Ensure the InputStream is closed in the finally block.
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream", e);
            }
        }
    }

    /**
     * Clears all input fields in the form and resets the image preview.
     * This is typically called after a ticket has been successfully saved.
     */
    private void clearForm() {
        dateInput.setText("");
        timeInput.setText("");
        licenseInput.setText("");
        locationInput.setText("");
        photoPreview.setImageDrawable(null); // Clear the image preview.
        imageUri = null; // Reset the image URI.

    }

    /**
     * Shows a DatePickerDialog to allow the user to select a date.
     * The selected date is then set in the dateInput EditText in "yyyy-MM-dd" format.
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance(); // Get current date and time.
        // Create and show a DatePickerDialog.
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            // Format the selected date as "yyyy-MM-dd". Month is 0-indexed, so add 1.
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            dateInput.setText(date); // Set the formatted date in the EditText.
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Shows a TimePickerDialog to allow the user to select a time.
     * The selected time is then set in the timeInput EditText in "HH:mm" (24-hour) format.
     */
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance(); // Get current date and time.
        // Create and show a TimePickerDialog.
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            // Format the selected time as "HH:mm".
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            timeInput.setText(time); // Set the formatted time in the EditText.
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show(); // 'true' for 24-hour format.
    }

    /**
     * Handles the result from an activity started for a result (e.g., image picker).
     * This method is called when the image picker activity returns with a selected image.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check if the result is for the image pick request and if it was successful.
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData(); // Get the URI of the selected image.
            photoPreview.setImageURI(imageUri); // Display the selected image in the preview ImageView.
        }
    }

    /**
     * Handles item selections from the ActionBar (e.g., the "Up" button).
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     *         proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Check if the selected item is the "home" button (the "Up" arrow in the ActionBar).
        if (item.getItemId() == android.R.id.home) {
            // If the "Up" button is pressed, simulate a back button press to navigate up in the activity stack.
            onBackPressed();
            return true; // Indicate that the event was handled.
        }
        // If the selected item is not the "home" button, delegate handling to the superclass.
        return super.onOptionsItemSelected(item);
    }
}