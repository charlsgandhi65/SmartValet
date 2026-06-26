package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONArray;
import org.json.JSONObject;

public class StaffViewEventsActivity extends AppCompatActivity {

    private LinearLayout eventsContainer;
    private String staffEmail, accessToken;
    private String assignedEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);

        staffEmail = getIntent().getStringExtra("staff_email");
        accessToken = getIntent().getStringExtra("access_token");

        eventsContainer = findViewById(R.id.eventsContainer);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadAssignedEvents();
    }

    private void loadAssignedEvents() {
        eventsContainer.removeAllViews();
        
        new Thread(() -> {
            try {
                // First get staff info to find assigned event
                JSONArray staffInfo = SupabaseClientInstance.getInstance()
                    .selectFromTable("staff", "email_id=eq." + staffEmail, 
                        accessToken != null ? accessToken : "");

                if (staffInfo.length() > 0) {
                    JSONObject staff = staffInfo.getJSONObject(0);
                    assignedEventId = staff.optString("assigned_event_id", "");
                    
                    if (assignedEventId != null && !assignedEventId.isEmpty()) {
                        // Load assigned event
                        JSONArray events = SupabaseClientInstance.getInstance()
                            .selectFromTable("events", "event_id=eq." + assignedEventId, 
                                accessToken != null ? accessToken : "");

                        runOnUiThread(() -> {
                            if (events.length() > 0) {
                                try {
                                    JSONObject event = events.getJSONObject(0);
                                    createEventCard(event);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                showNoEventMessage();
                            }
                        });
                    } else {
                        runOnUiThread(this::showNoEventMessage);
                    }
                } else {
                    runOnUiThread(this::showNoEventMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showNoEventMessage() {
        TextView noEvents = new TextView(this);
        noEvents.setText("No event assigned. Please contact administrator.");
        noEvents.setTextSize(16);
        noEvents.setPadding(32, 32, 32, 32);
        noEvents.setTextColor(0xFF666666);
        eventsContainer.addView(noEvents);
    }

    private void createEventCard(JSONObject event) {
        try {
            String eventId = event.optString("event_id", "");
            String eventName = event.optString("event_name", "Unnamed Event");
            String address = event.optString("event_address", "");
            String date = event.optString("event_date", "");
            int hours = event.optInt("hours_of_event", 6);
            int spots = event.optInt("total_vehicle_spots", 250);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 24, 24, 24);
            card.setBackgroundResource(R.drawable.input_bg);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);

            TextView tvName = new TextView(this);
            tvName.setText(eventName);
            tvName.setTextSize(20);
            tvName.setTextColor(0xFF7C3AED);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(tvName);

            TextView tvDetails = new TextView(this);
            tvDetails.setText(String.format("📍 %s\n📅 %s\n⏰ %d Hours\n🚗 %d Spots", 
                address, date, hours, spots));
            tvDetails.setTextSize(14);
            tvDetails.setTextColor(0xFF666666);
            tvDetails.setPadding(0, 8, 0, 0);
            card.addView(tvDetails);

            eventsContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

