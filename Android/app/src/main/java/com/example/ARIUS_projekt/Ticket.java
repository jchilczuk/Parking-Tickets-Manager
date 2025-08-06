package com.example.ARIUS_projekt;

import java.io.Serializable;

/**
 * Data model representing a traffic ticket.
 * Implements Serializable to allow passing Ticket objects between activities via Intents.
 */
public class Ticket implements Serializable {

    public int id;                  // Unique ticket ID, typically used for referencing tickets in backend systems.
    public String date;            // Expiration date (format: "yyyy-MM-dd").
    public String time;            // Expiration time (format: "HH:mm" or similar).
    public String license;         // License plate number of the vehicle receiving the ticket.
    public String location;        // Location where the ticket was issued.
    public String image_base64;    // Base64-encoded image string (photo of the ticket).

    /**
     * Full constructor used to instantiate a Ticket object with all its data.
     *
     * @param id            Unique identifier for the ticket.
     * @param date          Expiration date.
     * @param time          Expiration time.
     * @param license       Vehicle license plate number.
     * @param location      Location of the offense.
     * @param image_base64  Base64-encoded string of the ticket image.
     */
    public Ticket(int id, String date, String time, String license, String location, String image_base64) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.license = license;
        this.location = location;
        this.image_base64 = image_base64;
    }
}
