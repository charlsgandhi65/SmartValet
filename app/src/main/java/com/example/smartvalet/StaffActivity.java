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

public class StaffActivity extends AppCompatActivity {

    private TextView tvStaffName;
    private Button btnViewEvents, btnViewRequests, btnScanQR, btnManageVehicles;
    private String staffEmail, accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff);

        staffEmail = getIntent().getStringExtra("staff_email");
        accessToken = getIntent().getStringExtra("access_token");

        tvStaffName = findViewById(R.id.tvStaffName);
        btnViewEvents = findViewById(R.id.btnViewEvents);
        btnViewRequests = findViewById(R.id.btnViewRequests);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnManageVehicles = findViewById(R.id.btnManageVehicles);
        
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        // Back arrow
        ImageView imgBackArrow = findViewById(R.id.imgBackArrow);
        imgBackArrow.setOnClickListener(v -> finish());

        if (staffEmail != null) {
            new Thread(() -> {
                try {
                    org.json.JSONArray arr = com.example.smartvalet.utils.SupabaseClientInstance.getInstance()
                        .selectFromTable("staff", "email_id=eq." + staffEmail, accessToken);
                    String name = staffEmail;
                    if (arr.length() > 0) {
                        org.json.JSONObject obj = arr.getJSONObject(0);
                        name = obj.optString("full_name", name);
                    }
                    final String finalName = name;
                    runOnUiThread(() -> tvStaffName.setText("Welcome, " + finalName));
                } catch (Exception ignored) {
                    runOnUiThread(() -> tvStaffName.setText("Welcome, Staff"));
                }
            }).start();
        }

        // Card clicks
        findViewById(R.id.cardViewEvents).setOnClickListener(v -> {
            Intent intent = new Intent(StaffActivity.this, StaffViewEventsActivity.class);
            intent.putExtra("staff_email", staffEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        findViewById(R.id.cardViewRequests).setOnClickListener(v -> {
            Intent intent = new Intent(StaffActivity.this, ViewRequestsActivity.class);
            intent.putExtra("staff_email", staffEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        findViewById(R.id.cardScanQR).setOnClickListener(v -> {
            Intent intent = new Intent(StaffActivity.this, QRScannerActivity.class);
            intent.putExtra("staff_email", staffEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        findViewById(R.id.cardManageVehicles).setOnClickListener(v -> {
            Intent intent = new Intent(StaffActivity.this, ManageVehiclesActivity.class);
            intent.putExtra("staff_email", staffEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        // Go back normally - users can use back button or onBackPressed
        finish();
    }
}

