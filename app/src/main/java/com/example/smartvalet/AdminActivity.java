package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AdminActivity extends AppCompatActivity {

    private TextView tvAdminName;
    private Button btnManageEvents, btnManageStaff, btnViewReports, btnEventsHistory;
    private String adminEmail;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Get admin info from intent
        adminEmail = getIntent().getStringExtra("admin_email");
        accessToken = getIntent().getStringExtra("access_token");

        // Initialize views
        tvAdminName = findViewById(R.id.tvAdminName);
        btnManageEvents = findViewById(R.id.btnManageEvents);
        btnManageStaff = findViewById(R.id.btnManageStaff);
        btnViewReports = findViewById(R.id.btnViewReports);
        btnEventsHistory = findViewById(R.id.btnEventsHistory);
        
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        // Back arrow
        ImageView imgBackArrow = findViewById(R.id.imgBackArrow);
        imgBackArrow.setOnClickListener(v -> finish());
        
        // Also get card views for click handling
        com.google.android.material.card.MaterialCardView cardManageEvents = findViewById(R.id.cardManageEvents);
        com.google.android.material.card.MaterialCardView cardManageStaff = findViewById(R.id.cardManageStaff);
        com.google.android.material.card.MaterialCardView cardViewReports = findViewById(R.id.cardViewReports);
        com.google.android.material.card.MaterialCardView cardEventsHistory = findViewById(R.id.cardEventsHistory);

        // Set admin name (fetch from administration table)
        if (adminEmail != null) {
            new Thread(() -> {
                try {
                    org.json.JSONArray arr = com.example.smartvalet.utils.SupabaseClientInstance.getInstance()
                        .selectFromTable("administration", "email_id=eq." + adminEmail, accessToken);
                    String name = adminEmail;
                    if (arr.length() > 0) {
                        org.json.JSONObject obj = arr.getJSONObject(0);
                        name = obj.optString("full_name", name);
                    }
                    final String finalName = name;
                    runOnUiThread(() -> tvAdminName.setText("Welcome, " + finalName));
                } catch (Exception ignored) {
                    runOnUiThread(() -> tvAdminName.setText("Welcome, Admin"));
                }
            }).start();
        }

        // Card/Button click listeners
        if (cardManageEvents != null) {
            cardManageEvents.setOnClickListener(v -> navigateToEvents());
        }
        btnManageEvents.setOnClickListener(v -> navigateToEvents());
        
        if (cardManageStaff != null) {
            cardManageStaff.setOnClickListener(v -> navigateToStaff());
        }
        btnManageStaff.setOnClickListener(v -> navigateToStaff());
        
        if (cardViewReports != null) {
            cardViewReports.setOnClickListener(v -> {
                Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show();
            });
        }
        btnViewReports.setOnClickListener(v -> {
            Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        if (cardEventsHistory != null) {
            cardEventsHistory.setOnClickListener(v -> navigateToHistory());
        }
        btnEventsHistory.setOnClickListener(v -> navigateToHistory());
    }
    
    private void navigateToEvents() {
        Intent intent = new Intent(AdminActivity.this, ViewEventsActivity.class);
        intent.putExtra("admin_email", adminEmail);
        intent.putExtra("access_token", accessToken);
        startActivity(intent);
    }
    
    private void navigateToStaff() {
        Intent intent = new Intent(AdminActivity.this, ManageStaffActivity.class);
        intent.putExtra("admin_email", adminEmail);
        intent.putExtra("access_token", accessToken);
        startActivity(intent);
    }
    
    private void navigateToHistory() {
        Intent intent = new Intent(AdminActivity.this, EventsHistoryActivity.class);
        intent.putExtra("admin_email", adminEmail);
        intent.putExtra("access_token", accessToken);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // Go back normally - users can use back button or onBackPressed
        finish();
    }
}

