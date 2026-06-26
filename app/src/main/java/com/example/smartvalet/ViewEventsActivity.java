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
import java.util.HashSet;
import java.util.Set;

public class ViewEventsActivity extends AppCompatActivity {

    private LinearLayout eventsContainer;
    private android.widget.EditText editSearch;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabCreateEvent;
    private String adminEmail;
    private String accessToken;
    private org.json.JSONArray cachedEvents;
    private Set<String> displayedEventIds = new HashSet<>(); // Track unique event IDs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);

        adminEmail = getIntent().getStringExtra("admin_email");
        accessToken = getIntent().getStringExtra("access_token");

        eventsContainer = findViewById(R.id.eventsContainer);
        editSearch = findViewById(R.id.editSearch);
        fabCreateEvent = findViewById(R.id.fabCreateEvent);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        fabCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(ViewEventsActivity.this, CreateEventActivity.class);
            intent.putExtra("admin_email", adminEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        loadEvents();

        // Search filter
        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                filterAndRender(s.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents(); // Reload events when returning from CreateEvent
    }

    private void loadEvents() {
        eventsContainer.removeAllViews();
        displayedEventIds.clear(); // Clear tracking set
        
        new Thread(() -> {
            try {
                // Get today's date in YYYY-MM-DD format
                String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(new java.util.Date());
                
                // Load only current and future events (event_date >= today)
                JSONArray events = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", 
                        "event_date=gte." + todayDate + "&order=event_date.desc", 
                        accessToken != null ? accessToken : "");
                cachedEvents = events;

                System.out.println("ViewEvents: Loaded " + events.length() + " current/future events");

                runOnUiThread(() -> {
                    if (events.length() == 0) {
                        TextView noEvents = new TextView(this);
                        noEvents.setText("No upcoming events found. Create your first event!");
                        noEvents.setTextSize(16);
                        noEvents.setPadding(32, 32, 32, 32);
                        noEvents.setTextColor(0xFF666666);
                        eventsContainer.addView(noEvents);
                    } else {
                        // Display each event only once (prevent duplicates)
                        int displayCount = 0;
                        for (int i = 0; i < events.length(); i++) {
                            try {
                                JSONObject event = events.getJSONObject(i);
                                String eventId = event.optString("event_id", "");
                                
                                // Only display if not already shown
                                if (!displayedEventIds.contains(eventId)) {
                                    displayedEventIds.add(eventId);
                                    createEventCard(event);
                                    displayCount++;
                                    System.out.println("ViewEvents: Displayed event " + eventId);
                                } else {
                                    System.out.println("ViewEvents: Skipped duplicate event " + eventId);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("ViewEvents: Displayed " + displayCount + " out of " + events.length() + " events");
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

    private void filterAndRender(String query) {
        eventsContainer.removeAllViews();
        if (cachedEvents == null) return;
        String q = query == null ? "" : query.trim().toLowerCase();
        for (int i = 0; i < cachedEvents.length(); i++) {
            try {
                JSONObject ev = cachedEvents.getJSONObject(i);
                String name = ev.optString("event_name", "").toLowerCase();
                String addr = ev.optString("event_address", "").toLowerCase();
                if (q.isEmpty() || name.contains(q) || addr.contains(q)) {
                    createEventCard(ev);
                }
            } catch (Exception ignored) {}
        }
    }

    private void createEventCard(JSONObject event) {
        try {
            String eventId = event.optString("event_id", "");
            String eventName = event.optString("event_name", "Unnamed Event");
            String address = event.optString("event_address", "");
            String date = event.optString("event_date", "");
            int hours = event.optInt("hours_of_event", 6);
            int spots = event.optInt("total_vehicle_spots", 250);

            // Create card layout
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

            // Top row: name + right icons
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvEventName = new TextView(this);
            tvEventName.setText(eventName);
            tvEventName.setTextSize(20);
            tvEventName.setTextColor(0xFF7C3AED);
            tvEventName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvEventName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            android.widget.ImageButton btnEdit = new android.widget.ImageButton(this);
            btnEdit.setImageResource(R.drawable.ic_edit);
            btnEdit.setBackground(null);
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(ViewEventsActivity.this, EditEventActivity.class);
                intent.putExtra("event_data", event.toString());
                intent.putExtra("admin_email", adminEmail);
                intent.putExtra("access_token", accessToken);
                startActivity(intent);
            });

            android.widget.ImageButton btnDelete = new android.widget.ImageButton(this);
            btnDelete.setImageResource(R.drawable.ic_delete);
            btnDelete.setBackground(null);
            btnDelete.setOnClickListener(v -> deleteEvent(eventId));

            topRow.addView(tvEventName);
            topRow.addView(btnEdit);
            topRow.addView(btnDelete);
            card.addView(topRow);

            // Event details + spots left placeholder
            TextView tvEventDetails = new TextView(this);
            tvEventDetails.setText(String.format("\uD83D\uDCCD %s\n\uD83D\uDCC5 %s\n\u23F0 %d Hours\n\uD83D\uDE97 %d Spots", 
                address, date, hours, spots));
            tvEventDetails.setTextSize(14);
            tvEventDetails.setTextColor(0xFF666666);
            tvEventDetails.setPadding(0, 8, 0, 16);
            card.addView(tvEventDetails);

            // Fetch spots used and update
            new Thread(() -> {
                try {
                    JSONArray bookings = SupabaseClientInstance.getInstance()
                        .selectFromTable("booked_parking", "event_id=eq." + eventId, accessToken != null ? accessToken : "");
                    int used = bookings.length();
                    int left = spots - used;
                    runOnUiThread(() -> {
                        tvEventDetails.setText(String.format("\uD83D\uDCCD %s\n\uD83D\uDCC5 %s\n\u23F0 %d Hours\n\uD83D\uDE97 %d / %d Spots Left",
                            address, date, hours, spots, left));
                    });
                } catch (Exception ignored) {}
            }).start();

            // Whole card click opens details
            card.setOnClickListener(v -> {
                Intent intent = new Intent(ViewEventsActivity.this, EventDetailActivity.class);
                intent.putExtra("event_id", eventId);
                intent.putExtra("event_name", eventName);
                intent.putExtra("admin_email", adminEmail);
                intent.putExtra("access_token", accessToken);
                startActivity(intent);
            });

            eventsContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteEvent(String eventId) {
        new Thread(() -> {
            try {
                boolean deleted = SupabaseClientInstance.getInstance()
                    .deleteFromTable("events", "event_id=eq." + eventId, accessToken != null ? accessToken : "");
                
                runOnUiThread(() -> {
                    if (deleted) {
                        Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                        loadEvents();
                    } else {
                        Toast.makeText(this, "Failed to delete event", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}

