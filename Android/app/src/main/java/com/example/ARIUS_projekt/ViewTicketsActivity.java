package com.example.ARIUS_projekt;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.*;

/**
 * This activity displays a list of parking tickets retrieved from the backend server.
 * Users can filter tickets using several input fields (date, time, license, location).
 * They can also tap on a ticket to view more details or delete it.
 */
public class ViewTicketsActivity extends AppCompatActivity {

    // UI elements
    ListView ticketListView;
    EditText editTextDate, editTextTime, editTextLicense, editTextLocation;
    TextView emptyView;

    // Adapter and data structures for ticket list
    TicketAdapter adapter;
    ArrayList<Ticket> ticketList, filteredList;

    // Constants
    private static final int REQUEST_TICKET_DETAILS = 1;
    private static final String TAG = "ViewTicketsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tickets);

        // Enable the "back arrow" in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views from layout
        ticketListView = findViewById(R.id.listViewTickets);
        editTextDate = findViewById(R.id.editTextDate);
        editTextTime = findViewById(R.id.editTextTime);
        editTextLicense = findViewById(R.id.editTextLicense);
        editTextLocation = findViewById(R.id.editTextLocation);
        emptyView = findViewById(R.id.emptyView);

        // Initialize lists and set adapter
        ticketList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new TicketAdapter(this, filteredList);
        ticketListView.setAdapter(adapter);

        // Fetch ticket list from the server
        fetchTicketsFromBackend();

        // Setup interactive filtering logic for all input fields
        setupFilterInputs();

        // Handle item click to open ticket details
        ticketListView.setOnItemClickListener((parent, view, position, id) -> {
            Ticket ticket = filteredList.get(position);
            Intent intent = new Intent(ViewTicketsActivity.this, TicketDetailsActivity.class);
            intent.putExtra("ticket_id", ticket.id);
            startActivityForResult(intent, REQUEST_TICKET_DETAILS);
        });
    }

    /**
     * Launches the AsyncTask to retrieve the list of tickets from the server.
     */
    private void fetchTicketsFromBackend() {
        new FetchTicketsTask().execute();
    }

    /**
     * AsyncTask to fetch the full ticket list from the backend using JWT authorization.
     * It parses the response into Ticket objects and updates the UI accordingly.
     */
    @SuppressLint("StaticFieldLeak")
    private class FetchTicketsTask extends AsyncTask<Void, Void, List<Ticket>> {
        private int responseCode = -1;

        @Override
        protected List<Ticket> doInBackground(Void... voids) {
            List<Ticket> result = new ArrayList<>();
            try {
                // Get JWT token from shared preferences
                SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
                String jwtToken = prefs.getString("token", "");

                // Build authorized HTTP GET connection
                HttpURLConnection conn = GeneralUtils.getHttpConnectionWithAuth(
                        "http://10.0.2.2:5000/tickets",
                        "GET",
                        5000,
                        30000,
                        jwtToken
                );
                responseCode = conn.getResponseCode();

                // Handle successful response
                if (responseCode >= 200 && responseCode < 300) {
                    JSONArray jsonArray = GeneralUtils.getJsonArray(conn);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        result.add(new Ticket(
                                obj.getInt("id"),
                                obj.getString("date"),
                                obj.getString("time"),
                                obj.getString("vehicle_number"),
                                obj.getString("location"),
                                null // Image not fetched here
                        ));
                    }
                } else {
                    // Read error body and log it
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorMsg = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) errorMsg.append(line);
                        reader.close();
                        Log.e(TAG, "Server error (" + responseCode + "): " + errorMsg);
                    } else {
                        Log.e(TAG, "Server returned error code " + responseCode + " with no body.");
                    }
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching tickets", e);
            }

            return result;
        }

        @Override
        protected void onPostExecute(List<Ticket> tickets) {
            if (responseCode >= 200 && responseCode < 300) {
                // Update full and filtered lists, then refresh UI
                ticketList.clear();
                ticketList.addAll(tickets);
                filteredList.clear();
                filteredList.addAll(tickets);
                adapter.notifyDataSetChanged();
                toggleEmptyView();
            } else {
                Toast.makeText(ViewTicketsActivity.this,
                        "Server error: " + (responseCode > 0 ? responseCode : "Unknown"),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Sets up filter inputs (text + date/time pickers) and attaches listeners for live filtering.
     */
    private void setupFilterInputs() {
        // Disable manual editing; use date picker dialog
        editTextDate.setFocusable(false);
        editTextDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                editTextDate.setText(formattedDate);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Disable manual editing; use time picker dialog
        editTextTime.setFocusable(false);
        editTextTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                editTextTime.setText(formattedTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        // Attach a common TextWatcher to all filters to trigger re-filtering
        TextWatcher filterWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
        };

        editTextDate.addTextChangedListener(filterWatcher);
        editTextTime.addTextChangedListener(filterWatcher);
        editTextLicense.addTextChangedListener(filterWatcher);
        editTextLocation.addTextChangedListener(filterWatcher);
    }

    /**
     * Applies all filters entered by the user to the ticket list and refreshes the view.
     */
    private void applyFilters() {
        // Convert input to lowercase for case-insensitive matching
        String dateFilter = editTextDate.getText().toString().toLowerCase(Locale.getDefault());
        String timeFilter = editTextTime.getText().toString().toLowerCase(Locale.getDefault());
        String licenseFilter = editTextLicense.getText().toString().toLowerCase(Locale.getDefault());
        String locationFilter = editTextLocation.getText().toString().toLowerCase(Locale.getDefault());

        filteredList.clear();
        for (Ticket ticket : ticketList) {
            boolean matchDate = ticket.date.toLowerCase(Locale.getDefault()).contains(dateFilter);
            boolean matchTime = ticket.time.toLowerCase(Locale.getDefault()).contains(timeFilter);
            boolean matchLicense = ticket.license.toLowerCase(Locale.getDefault()).contains(licenseFilter);
            boolean matchLocation = ticket.location.toLowerCase(Locale.getDefault()).contains(locationFilter);

            if (matchDate && matchTime && matchLicense && matchLocation) {
                filteredList.add(ticket);
            }
        }

        adapter.notifyDataSetChanged();
        toggleEmptyView();
    }

    /**
     * Displays or hides the empty view depending on the filtered result size.
     */
    private void toggleEmptyView() {
        if (filteredList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            ticketListView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            ticketListView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Callback for results from TicketDetailsActivity (e.g. when a ticket was deleted).
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TICKET_DETAILS && resultCode == RESULT_OK) {
            Ticket deletedTicket = (Ticket) data.getSerializableExtra("deleted_ticket");
            if (deletedTicket != null) {
                // Remove ticket from both lists
                ticketList.removeIf(t -> t.id == deletedTicket.id);
                filteredList.removeIf(t -> t.id == deletedTicket.id);
                adapter.notifyDataSetChanged();
                toggleEmptyView();
            }
        }
    }

    /**
     * Handles navigation when user presses the back arrow in the ActionBar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Default back navigation
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
