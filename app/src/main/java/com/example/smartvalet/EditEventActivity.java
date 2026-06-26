package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONObject;

public class EditEventActivity extends AppCompatActivity {

    private EditText editEventName, editEventAddress, editEventDate, editHours, editSpots;
    private Button btnUpdateEvent;
    private String eventId, adminEmail, accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event); // Reuse create layout

        adminEmail = getIntent().getStringExtra("admin_email");
        accessToken = getIntent().getStringExtra("access_token");

        String eventDataStr = getIntent().getStringExtra("event_data");
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        editEventName = findViewById(R.id.editEventName);
        editEventAddress = findViewById(R.id.editEventAddress);
        editEventDate = findViewById(R.id.editEventDate);
        editHours = findViewById(R.id.editHours);
        editSpots = findViewById(R.id.editSpots);
        btnUpdateEvent = findViewById(R.id.btnCreateEvent);
        btnUpdateEvent.setText("Update Event");

        // Load event data
        try {
            JSONObject eventData = new JSONObject(eventDataStr);
            eventId = eventData.optString("event_id", "");
            editEventName.setText(eventData.optString("event_name", ""));
            editEventAddress.setText(eventData.optString("event_address", ""));
            editEventDate.setText(eventData.optString("event_date", ""));
            editHours.setText(String.valueOf(eventData.optInt("hours_of_event", 6)));
            editSpots.setText(String.valueOf(eventData.optInt("total_vehicle_spots", 250)));
        } catch (Exception e) {
            Toast.makeText(this, "Error loading event data", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnUpdateEvent.setOnClickListener(v -> updateEvent());
    }

    private void updateEvent() {
        String eventName = editEventName.getText().toString().trim();
        String address = editEventAddress.getText().toString().trim();
        String date = editEventDate.getText().toString().trim();
        String hoursStr = editHours.getText().toString().trim();
        String spotsStr = editSpots.getText().toString().trim();

        if (eventName.isEmpty() || address.isEmpty() || date.isEmpty() || 
            hoursStr.isEmpty() || spotsStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int hours = Integer.parseInt(hoursStr);
            int spots = Integer.parseInt(spotsStr);

            btnUpdateEvent.setEnabled(false);
            btnUpdateEvent.setText("Updating...");

            new Thread(() -> {
                try {
                    JSONObject eventData = new JSONObject();
                    eventData.put("event_name", eventName);
                    eventData.put("event_address", address);
                    eventData.put("event_date", date);
                    eventData.put("hours_of_event", hours);
                    eventData.put("total_vehicle_spots", spots);
                    eventData.put("updated_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(new java.util.Date()));

                    SupabaseClientInstance.getInstance()
                        .updateTable("events", "event_id=eq." + eventId, eventData, accessToken != null ? accessToken : "");

                    runOnUiThread(() -> {
                        btnUpdateEvent.setEnabled(true);
                        btnUpdateEvent.setText("Update Event");
                        Toast.makeText(this, "Event updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        btnUpdateEvent.setEnabled(true);
                        btnUpdateEvent.setText("Update Event");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Hours and Spots must be valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}

