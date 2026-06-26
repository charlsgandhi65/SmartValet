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

public class ManageStaffActivity extends AppCompatActivity {

    private LinearLayout staffContainer;
    private android.widget.EditText editSearch;
    private android.widget.Spinner spinnerFilter;
    private android.widget.ImageView btnFilter;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddStaff;
    private String adminEmail, accessToken;
    private org.json.JSONArray eventsCache;
    private org.json.JSONArray staffCache;
    private String currentEventId;
    private String selectedFilterRole = "All Positions";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_staff);

        adminEmail = getIntent().getStringExtra("admin_email");
        accessToken = getIntent().getStringExtra("access_token");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            // If viewing staff for an event, go back to events list
            // Otherwise, go back to previous activity
            if (currentEventId != null) {
                currentEventId = null;
                loadEvents();
            } else {
                finish();
            }
        });

        staffContainer = findViewById(R.id.staffContainer);
        editSearch = findViewById(R.id.editSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        btnFilter = findViewById(R.id.btnFilter);
        fabAddStaff = findViewById(R.id.fabAddStaff);

        // Filter button click listener
        btnFilter.setOnClickListener(v -> showFilterDialog());

        fabAddStaff.setOnClickListener(v -> {
            Intent intent = new Intent(ManageStaffActivity.this, AddStaffToEventActivity.class);
            intent.putExtra("admin_email", adminEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        setupFilterSpinner();
        loadEvents();

        editSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (currentEventId == null) renderEvents(); else renderStaffForCurrentEvent();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentEventId == null) loadEvents(); else loadStaffForEvent(currentEventId);
    }

    private void setupFilterSpinner() {
        String[] roles = {"All Positions", "NEW COMER", "6 - 12 MONTHS EXPERIENCE", "MORE THAN 12 MONTHS EXPERIENCE"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);
        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (currentEventId == null) renderEvents(); else renderStaffForCurrentEvent();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadEvents() {
        currentEventId = null;
        staffContainer.removeAllViews();
        new Thread(() -> {
            try {
                eventsCache = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", "order=event_date.desc", accessToken != null ? accessToken : "");
                runOnUiThread(this::renderEvents);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void renderEvents() {
        staffContainer.removeAllViews();
        if (eventsCache == null || eventsCache.length() == 0) {
            TextView noEvents = new TextView(this);
            noEvents.setText("No events found.");
            noEvents.setTextSize(16);
            noEvents.setPadding(32, 32, 32, 32);
            noEvents.setTextColor(0xFF666666);
            staffContainer.addView(noEvents);
            return;
        }
        String q = editSearch.getText() == null ? "" : editSearch.getText().toString().trim().toLowerCase();
        for (int i = 0; i < eventsCache.length(); i++) {
            try {
                org.json.JSONObject ev = eventsCache.getJSONObject(i);
                String name = ev.optString("event_name", "");
                String address = ev.optString("event_address", "");
                if (!q.isEmpty() && !(name.toLowerCase().contains(q) || address.toLowerCase().contains(q))) continue;

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(24, 24, 24, 24);
                card.setBackgroundResource(R.drawable.input_bg);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 16);
                card.setLayoutParams(params);

                TextView tvName = new TextView(this);
                tvName.setText(name);
                tvName.setTextSize(20);
                tvName.setTextColor(0xFF7C3AED);
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                card.addView(tvName);

                TextView tvAddr = new TextView(this);
                tvAddr.setText(address);
                tvAddr.setTextSize(14);
                tvAddr.setTextColor(0xFF666666);
                tvAddr.setPadding(0, 8, 0, 0);
                card.addView(tvAddr);

                final String evId = ev.optString("event_id", "");
                card.setOnClickListener(v -> loadStaffForEvent(evId));
                staffContainer.addView(card);
            } catch (Exception ignored) {}
        }
    }

    private void loadStaffForEvent(String eventId) {
        currentEventId = eventId;
        staffContainer.removeAllViews();
        new Thread(() -> {
            try {
                // Query staff assigned to this specific event (assigned_event_id is UUID, no quotes needed)
                staffCache = SupabaseClientInstance.getInstance()
                    .selectFromTable("staff", "assigned_event_id=eq." + eventId, accessToken != null ? accessToken : "");
                runOnUiThread(this::renderStaffForCurrentEvent);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error loading staff: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void renderStaffForCurrentEvent() {
        staffContainer.removeAllViews();
        String q = editSearch.getText() == null ? "" : editSearch.getText().toString().trim().toLowerCase();
        String role = selectedFilterRole;
        if (staffCache == null || staffCache.length() == 0) {
            TextView noStaff = new TextView(this);
            noStaff.setText("No staff for this event.");
            noStaff.setTextSize(16);
            noStaff.setPadding(32, 32, 32, 32);
            noStaff.setTextColor(0xFF666666);
            staffContainer.addView(noStaff);
            return;
        }
        for (int i = 0; i < staffCache.length(); i++) {
            try {
                JSONObject staff = staffCache.getJSONObject(i);
                String name = staff.optString("full_name", "");
                String position = staff.optString("experience_level", "");
                if (!q.isEmpty() && !name.toLowerCase().contains(q)) continue;
                if (!"All Positions".equals(role) && !position.equalsIgnoreCase(role)) continue;
                createStaffCard(staff);
            } catch (Exception ignored) {}
        }
    }

    private void createStaffCard(JSONObject staff) {
        try {
            String staffId = staff.optString("staff_id", "");
            String name = staff.optString("full_name", "Unknown");
            String email = staff.optString("email_id", "");
            String phone = staff.optString("contact_number", "");
            String location = staff.optString("location", "");
            String experience = staff.optString("experience_level", "");
            String loginId = staff.optString("login_id", "");

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
            tvName.setText(name);
            tvName.setTextSize(18);
            tvName.setTextColor(0xFF7C3AED);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(tvName);

            TextView tvDetails = new TextView(this);
            tvDetails.setText(String.format("📧 %s\n📞 %s\n📍 %s\n🎓 %s\n🔑 %s", 
                email, phone, location, experience, loginId));
            tvDetails.setTextSize(14);
            tvDetails.setTextColor(0xFF666666);
            tvDetails.setPadding(0, 8, 0, 16);
            card.addView(tvDetails);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(name);
            tvTitle.setTextSize(18);
            tvTitle.setTextColor(0xFF7C3AED);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            android.widget.ImageButton btnEdit = new android.widget.ImageButton(this);
            btnEdit.setImageResource(R.drawable.ic_edit);
            btnEdit.setBackground(null);
            // TODO: Implement edit staff flow

            android.widget.ImageButton btnDelete = new android.widget.ImageButton(this);
            btnDelete.setImageResource(R.drawable.ic_delete);
            btnDelete.setBackground(null);
            btnDelete.setOnClickListener(v -> deleteStaff(staffId));

            topRow.addView(tvTitle);
            topRow.addView(btnEdit);
            topRow.addView(btnDelete);
            card.addView(topRow);

            staffContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteStaff(String staffId) {
        new Thread(() -> {
            try {
                boolean deleted = SupabaseClientInstance.getInstance()
                    .deleteFromTable("staff", "staff_id=eq." + staffId, accessToken != null ? accessToken : "");
                
                runOnUiThread(() -> {
                    if (deleted) {
                        Toast.makeText(this, "Staff deleted successfully", Toast.LENGTH_SHORT).show();
                        if (currentEventId != null) {
                            loadStaffForEvent(currentEventId);
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showFilterDialog() {
        String[] roles = {"All Positions", "NEW COMER", "6 - 12 MONTHS EXPERIENCE", "MORE THAN 12 MONTHS EXPERIENCE"};
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Filter by Position");
        builder.setItems(roles, (dialog, which) -> {
            selectedFilterRole = roles[which];
            if (currentEventId == null) {
                renderEvents();
            } else {
                renderStaffForCurrentEvent();
            }
        });
        builder.show();
    }
}
