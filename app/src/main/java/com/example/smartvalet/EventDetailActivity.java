package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONArray;
import org.json.JSONObject;

public class EventDetailActivity extends AppCompatActivity {

    private TextView tvEventName, tvAddress, tvDate, tvHours, tvSpots;
    private Button btnAddStaff;
    private String eventId, eventName, adminEmail, accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        eventId = getIntent().getStringExtra("event_id");
        eventName = getIntent().getStringExtra("event_name");
        adminEmail = getIntent().getStringExtra("admin_email");
        accessToken = getIntent().getStringExtra("access_token");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvEventName = findViewById(R.id.tvEventName);
        tvAddress = findViewById(R.id.tvAddress);
        tvDate = findViewById(R.id.tvDate);
        tvHours = findViewById(R.id.tvHours);
        tvSpots = findViewById(R.id.tvSpots);
        btnAddStaff = findViewById(R.id.btnAddStaff);

        // Don't set event name here, let loadEventDetails handle it
        btnAddStaff.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailActivity.this, AddStaffToEventActivity.class);
            intent.putExtra("event_id", eventId);
            intent.putExtra("event_name", eventName);
            intent.putExtra("admin_email", adminEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        loadEventDetails();
    }

    private void loadEventDetails() {
        new Thread(() -> {
            try {
                JSONArray events = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", "event_id=eq." + eventId, accessToken != null ? accessToken : "");

                runOnUiThread(() -> {
                    if (events.length() > 0) {
                        try {
                            JSONObject event = events.getJSONObject(0);
                            tvEventName.setText(event.optString("event_name", ""));
                            tvAddress.setText("📍 " + event.optString("event_address", ""));
                            tvDate.setText("📅 " + event.optString("event_date", ""));
                            tvHours.setText("⏰ " + event.optInt("hours_of_event", 0) + " Hours");
                            tvSpots.setText("🚗 " + event.optInt("total_vehicle_spots", 0) + " Parking Spots");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}

