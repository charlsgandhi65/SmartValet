package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import java.util.Set;

public class ViewAvailableEventsActivity extends AppCompatActivity {

    private LinearLayout eventsContainer;
    private String customerEmail, accessToken;
    private Set<String> displayedEventIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);

        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        eventsContainer = findViewById(R.id.eventsContainer);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Hide the FAB button (+ sign) - customers shouldn't create events
        View fabCreateEvent = findViewById(R.id.fabCreateEvent);
        if (fabCreateEvent != null) {
            fabCreateEvent.setVisibility(View.GONE);
        }

        loadAvailableEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh events list when returning from booking activity
        loadAvailableEvents();
    }

    private void loadAvailableEvents() {
        eventsContainer.removeAllViews();
        displayedEventIds.clear(); // Clear tracking set
        
        new Thread(() -> {
            try {
                // Get events that are on or after today
                String today = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new Date());
                JSONArray events = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", "event_date=gte." + today + "&order=event_date.asc", 
                        accessToken != null ? accessToken : "");

                System.out.println("ViewAvailableEvents: Loaded " + events.length() + " events from database");

                runOnUiThread(() -> {
                    if (events.length() == 0) {
                        TextView noEvents = new TextView(this);
                        noEvents.setText("No upcoming events available.");
                        noEvents.setTextSize(16);
                        noEvents.setPadding(32, 32, 32, 32);
                        noEvents.setTextColor(0xFF666666);
                        eventsContainer.addView(noEvents);
                    } else {
                        for (int i = 0; i < events.length(); i++) {
                            try {
                                JSONObject event = events.getJSONObject(i);
                                String eventId = event.optString("event_id", "");
                                
                                // Skip duplicate events
                                if (!displayedEventIds.contains(eventId)) {
                                    displayedEventIds.add(eventId);
                                    createEventCard(event);
                                } else {
                                    System.out.println("ViewAvailableEvents: SKIPPED duplicate event ID: " + eventId);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("ViewAvailableEvents: Displayed " + displayedEventIds.size() + " unique events");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void createEventCard(JSONObject event) {
        try {
            String eventId = event.optString("event_id", "");
            String eventName = event.optString("event_name", "Unnamed Event");
            String address = event.optString("event_address", "");
            String date = event.optString("event_date", "");
            int totalSpots = event.optInt("total_vehicle_spots", 250);
            int occupiedSpots = event.optInt("occupied_spots", 0);
            int availableSpots = totalSpots - occupiedSpots;

            System.out.println("ViewAvailableEvents - Event: " + eventName);
            System.out.println("  Total Spots: " + totalSpots);
            System.out.println("  Occupied Spots: " + occupiedSpots);
            System.out.println("  Available Spots: " + availableSpots);

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
            tvDetails.setText(String.format("📍 %s\n📅 %s\n🚗 %d/%d Spots Available", 
                address, date, availableSpots, totalSpots));
            tvDetails.setTextSize(14);
            tvDetails.setTextColor(0xFF666666);
            tvDetails.setPadding(0, 8, 0, 16);
            card.addView(tvDetails);

            Button btnBook = new Button(this);
            
            // Disable booking if no spots available
            if (availableSpots <= 0) {
                btnBook.setText("FULL - No Spots Available");
                btnBook.setBackgroundColor(0xFF666666);
                btnBook.setTextColor(0xFFFFFFFF);
                btnBook.setEnabled(false);
            } else {
                btnBook.setText("Book Parking");
                btnBook.setBackgroundColor(0xFF7C3AED);
                btnBook.setTextColor(0xFFFFFFFF);
                btnBook.setOnClickListener(v -> {
                    Intent intent = new Intent(ViewAvailableEventsActivity.this, BookParkingActivity.class);
                    intent.putExtra("event_id", eventId);
                    intent.putExtra("event_name", eventName);
                    intent.putExtra("event_date", date);
                    intent.putExtra("customer_email", customerEmail);
                    intent.putExtra("access_token", accessToken);
                    startActivity(intent);
                });
            }
            card.addView(btnBook);

            eventsContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

