package com.example.ARIUS_projekt;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * General-purpose utility methods used across the application,
 * such as displaying toast messages and configuring HTTP connections.
 */
public class GeneralUtils {

    /**
     * Displays a top-centered short toast message.
     *
     * @param context The context from which this method is called (e.g., Activity).
     * @param message The message to be displayed in the toast.
     */
    public static void showToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();
    }

    /**
     * Creates and configures an HttpURLConnection for general use (e.g., POST or GET).
     * This version does not include an Authorization header.
     *
     * @param endpointUrl        The full URL to connect to (e.g., "<a href="http://10.0.2.2:5000/auth/login">...</a>").
     * @param method             The HTTP method to use ("POST", "GET", etc.).
     * @param connectionTimeout  Connection timeout in milliseconds.
     * @param readTimeout        Read timeout in milliseconds.
     * @return A configured HttpURLConnection instance.
     * @throws IOException If an error occurs during connection setup.
     */
    public static HttpURLConnection getHttpConnection(
            String endpointUrl,
            String method,
            int connectionTimeout,
            int readTimeout
    ) throws IOException {
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(connectionTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");

        // Only enable output if method implies a request body (e.g., POST, PUT)
        if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
            conn.setDoOutput(true);
        }

        return conn;
    }

    /**
     * Creates and configures an HttpURLConnection with a Bearer Authorization header.
     *
     * @param endpointUrl        The full URL to connect to.
     * @param method             The HTTP method to use ("POST", "GET", etc.).
     * @param connectionTimeout  Connection timeout in milliseconds.
     * @param readTimeout        Read timeout in milliseconds.
     * @param jwtToken           The Bearer token for the Authorization header.
     * @return A configured HttpURLConnection instance with Authorization.
     * @throws IOException If an error occurs during connection setup.
     */
    public static HttpURLConnection getHttpConnectionWithAuth(String endpointUrl, String method, int connectionTimeout, int readTimeout, String jwtToken) throws IOException {
        HttpURLConnection conn = getHttpConnection(endpointUrl, method, connectionTimeout, readTimeout);
        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
        return conn;
    }
    /**
     * Reads the response from a connection and parses it as a JSON array.
     *
     * @param conn The HttpURLConnection to read from.
     * @return A JSONArray parsed from the response.
     * @throws Exception If parsing fails.
     */
    public static JSONArray getJsonArray(HttpURLConnection conn) throws Exception {
        InputStream input = conn.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return new JSONArray(response.toString());
    }
}
