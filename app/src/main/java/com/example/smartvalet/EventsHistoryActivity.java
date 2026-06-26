package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class EventsHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private String adminEmail, accessToken;
    private Set<String> displayedEventIds = new HashSet<>();
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);

        adminEmail = getIntent().getStringExtra("admin_email");
        accessToken = getIntent().getStringExtra("access_token");

        historyContainer = findViewById(R.id.eventsContainer);
        
        // Hide FAB
        findViewById(R.id.fabCreateEvent).setVisibility(android.view.View.GONE);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadHistoryEvents();
    }

    private void loadHistoryEvents() {
        if (isLoading) return;
        isLoading = true;
        
        historyContainer.removeAllViews();
        displayedEventIds.clear();
        
        new Thread(() -> {
            try {
                // Get today's date in yyyy-MM-dd format
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                
                // Query events table for past events only (event_date < today)
                JSONArray events = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", 
                        "event_date=lt." + todayDate + "&order=event_date.desc", 
                        accessToken != null ? accessToken : "");

                runOnUiThread(() -> {
                    if (events.length() == 0) {
                        TextView noEvents = new TextView(this);
                        noEvents.setText("No past events found.");
                        noEvents.setTextSize(16);
                        noEvents.setPadding(32, 32, 32, 32);
                        noEvents.setTextColor(0xFF666666);
                        historyContainer.addView(noEvents);
                    } else {
                        for (int i = 0; i < events.length(); i++) {
                            try {
                                JSONObject event = events.getJSONObject(i);
                                String eventId = event.optString("event_id", "");
                                
                                // Skip duplicate event IDs
                                if (!displayedEventIds.contains(eventId)) {
                                    displayedEventIds.add(eventId);
                                    createHistoryCard(event);
                                } else {
                                    System.out.println("SKIPPED: Duplicate past event ID " + eventId);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    isLoading = false;
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isLoading = false;
                });
            }
        }).start();
    }

    private void createHistoryCard(JSONObject event) {
        try {
            String eventId = event.optString("event_id", "");
            String eventName = event.optString("event_name", "Unnamed Event");
            String address = event.optString("event_address", "");
            String date = event.optString("event_date", "");
            int totalSpots = event.optInt("total_vehicle_spots", 0);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(24, 24, 24, 24);
            card.setBackgroundResource(R.drawable.input_bg);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);

            // Event details (left side)
            LinearLayout detailsLayout = new LinearLayout(this);
            detailsLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            );
            detailsLayout.setLayoutParams(detailsParams);

            TextView tvName = new TextView(this);
            tvName.setText(eventName);
            tvName.setTextSize(18);
            tvName.setTextColor(0xFF7C3AED);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            detailsLayout.addView(tvName);

            TextView tvDetails = new TextView(this);
            tvDetails.setText(String.format("📍 %s\n📅 %s\n🚗 %d Total Spots", 
                address, date, totalSpots));
            tvDetails.setTextSize(14);
            tvDetails.setTextColor(0xFF666666);
            tvDetails.setPadding(0, 8, 0, 0);
            detailsLayout.addView(tvDetails);

            card.addView(detailsLayout);

            // Delete button (right side)
            ImageView btnDelete = new ImageView(this);
            btnDelete.setImageResource(R.drawable.ic_delete);
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(40, 40);
            deleteParams.setMargins(16, 0, 0, 0);
            btnDelete.setLayoutParams(deleteParams);
            btnDelete.setOnClickListener(v -> deleteHistoryEvent(eventId, card));
            card.addView(btnDelete);

            historyContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteHistoryEvent(String eventId, LinearLayout card) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Delete this past event? This cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                new Thread(() -> {
                    try {
                        SupabaseClientInstance.getInstance()
                            .deleteFromTable("events", "event_id=eq." + eventId, 
                                accessToken != null ? accessToken : "");

                        runOnUiThread(() -> {
                            historyContainer.removeView(card);
                            displayedEventIds.remove(eventId);
                            Toast.makeText(this, "Event deleted from history", Toast.LENGTH_SHORT).show();
                            
                            // Show message if no events left
                            if (historyContainer.getChildCount() == 0) {
                                TextView noEvents = new TextView(this);
                                noEvents.setText("No past events found.");
                                noEvents.setTextSize(16);
                                noEvents.setPadding(32, 32, 32, 32);
                                noEvents.setTextColor(0xFF666666);
                                historyContainer.addView(noEvents);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error deleting event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }).start();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
