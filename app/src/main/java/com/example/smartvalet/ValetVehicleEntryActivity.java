package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

public class ValetVehicleEntryActivity extends AppCompatActivity {

    private EditText etVehicleNumber, etVehicleModel, etVehicleColor;
    private Spinner spinnerEvent;
    private Button btnProceedToPayment;
    private String customerEmail, accessToken, requestId;
    private JSONArray eventsArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_valet_vehicle_entry);

        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");
        requestId = getIntent().getStringExtra("request_id");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etVehicleNumber = findViewById(R.id.etVehicleNumber);
        etVehicleModel = findViewById(R.id.etVehicleModel);
        etVehicleColor = findViewById(R.id.etVehicleColor);
        spinnerEvent = findViewById(R.id.spinnerEvent);
        btnProceedToPayment = findViewById(R.id.btnProceedToPayment);

        btnProceedToPayment.setOnClickListener(v -> validateAndProceed());

        loadEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-enable button when returning from payment (in case user went back)
        btnProceedToPayment.setEnabled(true);
        btnProceedToPayment.setText("Proceed to Payment");
    }

    private void loadEvents() {
        new Thread(() -> {
            try {
                System.out.println("ValetVehicleEntry: Loading available events");

                // Get today's date in YYYY-MM-DD format
                String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(new java.util.Date());

                // Load only current and future events (event_date >= today)
                eventsArray = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", 
                        "event_date=gte." + todayDate + "&order=event_date.asc", 
                        accessToken != null ? accessToken : "");

                System.out.println("ValetVehicleEntry: Found " + eventsArray.length() + " current/future events");

                runOnUiThread(() -> {
                    if (eventsArray.length() == 0) {
                        Toast.makeText(this, "No upcoming events available", Toast.LENGTH_SHORT).show();
                        spinnerEvent.setEnabled(false);
                    } else {
                        String[] eventLabels = new String[eventsArray.length()];
                        try {
                            for (int i = 0; i < eventsArray.length(); i++) {
                                JSONObject event = eventsArray.getJSONObject(i);
                                eventLabels[i] = event.optString("event_name", "") + " - " + 
                                    event.optString("event_date", "");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, eventLabels);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerEvent.setAdapter(adapter);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ValetVehicleEntry: Error loading events: " + e.getMessage());
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void validateAndProceed() {
        String vehicleNumber = etVehicleNumber.getText().toString().trim().toUpperCase();
        String vehicleModel = etVehicleModel.getText().toString().trim();
        String vehicleColor = etVehicleColor.getText().toString().trim();

        if (vehicleNumber.isEmpty()) {
            etVehicleNumber.setError("Vehicle number is required");
            etVehicleNumber.requestFocus();
            return;
        }

        if (vehicleModel.isEmpty()) {
            etVehicleModel.setError("Vehicle model is required");
            etVehicleModel.requestFocus();
            return;
        }

        if (eventsArray == null || eventsArray.length() == 0) {
            Toast.makeText(this, "Please select an event", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedEventIndex = spinnerEvent.getSelectedItemPosition();
        if (selectedEventIndex < 0) {
            Toast.makeText(this, "Please select an event", Toast.LENGTH_SHORT).show();
            return;
        }

        btnProceedToPayment.setEnabled(false);
        btnProceedToPayment.setText("Creating booking...");

        new Thread(() -> {
            try {
                JSONObject selectedEvent = eventsArray.getJSONObject(selectedEventIndex);
                String eventId = selectedEvent.optString("event_id", "");
                String eventName = selectedEvent.optString("event_name", "");
                int totalSpots = selectedEvent.optInt("total_vehicle_spots", 0);
                int occupiedSpots = selectedEvent.optInt("occupied_spots", 0);
                int availableSpots = totalSpots - occupiedSpots;

                System.out.println("ValetVehicleEntry: Event: " + eventName);
                System.out.println("ValetVehicleEntry: Total spots: " + totalSpots + ", Occupied: " + occupiedSpots + ", Available: " + availableSpots);

                // Check if spots are available
                if (availableSpots <= 0) {
                    System.out.println("ValetVehicleEntry: No parking spots available!");
                    runOnUiThread(() -> {
                        btnProceedToPayment.setEnabled(true);
                        btnProceedToPayment.setText("Proceed to Payment");
                        Toast.makeText(this, "Sorry, " + eventName + " is fully booked. No parking spots available.", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                System.out.println("ValetVehicleEntry: Creating booking for valet parking");
                System.out.println("ValetVehicleEntry: Vehicle: " + vehicleNumber);

                // Generate booking ID
                String bookingId = UUID.randomUUID().toString();

                // Create booking data (without payment yet)
                JSONObject bookingData = new JSONObject();
                bookingData.put("booking_id", bookingId);
                bookingData.put("customer_email", customerEmail);
                bookingData.put("event_id", eventId);
                bookingData.put("vehicle_number", vehicleNumber);
                bookingData.put("vehicle_model", vehicleModel);
                bookingData.put("vehicle_color", vehicleColor);
                bookingData.put("parking_spot_number", 0); // Will be assigned by staff
                bookingData.put("booking_date", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
                bookingData.put("start_time", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
                bookingData.put("payment_status", "pending");
                bookingData.put("status", "pending"); // Pending until payment

                // Insert booking
                SupabaseClientInstance.getInstance()
                    .insertIntoTable("booked_parking", bookingData, accessToken != null ? accessToken : "");

                // Update valet request to link booking_id (but keep status as "assigned" until payment)
                JSONObject updateRequest = new JSONObject();
                updateRequest.put("booking_id", bookingId);
                
                SupabaseClientInstance.getInstance()
                    .updateTable("parking_requests", "request_id=eq." + requestId, updateRequest, accessToken != null ? accessToken : "");

                System.out.println("ValetVehicleEntry: Booking created, proceeding to payment");

                // Navigate to payment
                runOnUiThread(() -> {
                    Intent intent = new Intent(ValetVehicleEntryActivity.this, PaymentActivity.class);
                    intent.putExtra("customer_email", customerEmail);
                    intent.putExtra("access_token", accessToken);
                    intent.putExtra("booking_id", bookingId);
                    intent.putExtra("event_id", eventId);
                    intent.putExtra("event_name", eventName); // Pass event name for display
                    intent.putExtra("parking_spot", 0); // Spot will be assigned by staff later
                    intent.putExtra("request_id", requestId); // Pass request_id to mark as completed after payment
                    startActivity(intent);
                    // Finish this activity so payment success goes directly to dashboard
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ValetVehicleEntry: Error creating booking: " + e.getMessage());
                runOnUiThread(() -> {
                    btnProceedToPayment.setEnabled(true);
                    btnProceedToPayment.setText("Proceed to Payment");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
