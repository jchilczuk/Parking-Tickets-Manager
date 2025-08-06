package com.example.ARIUS_projekt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TicketAdapter is a custom ArrayAdapter designed to display a list of Ticket objects in a ListView.
 * It uses the ViewHolder pattern to optimize performance.
 */
public class TicketAdapter extends ArrayAdapter<Ticket> {

    /**
     * Constructor for the TicketAdapter.
     * @param context The current context (e.g., an Activity or Application context).
     * @param tickets A list of Ticket objects to be displayed in the ListView.
     */
    public TicketAdapter(@NonNull Context context, @NonNull List<Ticket> tickets) {
        // Call the superclass constructor.
        // '0' indicates that we will provide our own layout inside getView().
        super(context, 0, tickets);
    }

    /**
     * Returns the view for an item at the specified position in the list.
     * Implements the ViewHolder pattern to reuse views and reduce calls to findViewById().
     *
     * @param position The position of the item in the data set.
     * @param convertView The recycled view, if available.
     * @param parent The parent view group (typically the ListView).
     * @return The view corresponding to the data at the specified position.
     */
    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.single_row, parent, false);
            holder = new ViewHolder();
            holder.textDateTime = convertView.findViewById(R.id.textDateTime);
            holder.textLicense = convertView.findViewById(R.id.textLicense);
            holder.textLocation = convertView.findViewById(R.id.textLocation);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Ticket ticket = getItem(position);
        if (ticket == null) return convertView;

        // NEW: Format date and combine with time
        String formattedDateTime = ticket.date + ", " + ticket.time;
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsedDate = inputFormat.parse(ticket.date);
            if (parsedDate != null) {
                String datePart = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(parsedDate);
                formattedDateTime = datePart + ", " + ticket.time;
            }
        } catch (ParseException e) {
            e.printStackTrace(); // log failure but fall back to raw strings
        }

        // Populate the view
        holder.textDateTime.setText("Termin ważności: " + formattedDateTime);
        holder.textLicense.setText("Nr rejestracyjny pojazdu: " + ticket.license);
        holder.textLocation.setText("Lokalizacja: " + ticket.location);

        return convertView;
    }

    /**
     * Static inner class to cache the views inside each row item.
     * This avoids repeated calls to findViewById() and improves performance.
     */
    static class ViewHolder {
        TextView textDateTime;
        TextView textLicense;
        TextView textLocation;
    }

}
