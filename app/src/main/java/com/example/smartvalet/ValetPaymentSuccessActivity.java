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

public class ValetPaymentSuccessActivity extends AppCompatActivity {

    private TextView tvSuccessMessage, tvStaffName, tvStaffPhone, tvInstructions;
    private Button btnBackToDashboard;
    private String requestId, customerEmail, accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_valet_payment_success);

        requestId = getIntent().getStringExtra("request_id");
        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
        tvStaffName = findViewById(R.id.tvStaffName);
        tvStaffPhone = findViewById(R.id.tvStaffPhone);
        tvInstructions = findViewById(R.id.tvInstructions);
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard);

        // Back to dashboard button
        btnBackToDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(ValetPaymentSuccessActivity.this, CustomerActivity.class);
            intent.putExtra("customer_email", customerEmail);
            intent.putExtra("access_token", accessToken);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Load staff details who accepted the request
        loadStaffDetails();
    }

    private void loadStaffDetails() {
        new Thread(() -> {
            try {
                System.out.println("ValetPaymentSuccess: Loading staff details for request: " + requestId);

                // Get the valet request to find assigned staff
                JSONArray requests = SupabaseClientInstance.getInstance()
                    .selectFromTable("parking_requests",
                        "request_id=eq." + requestId,
                        accessToken != null ? accessToken : "");

                if (requests.length() > 0) {
                    JSONObject request = requests.getJSONObject(0);
                    String staffId = request.optString("assigned_staff_id", "");

                    System.out.println("ValetPaymentSuccess: Assigned staff ID: " + staffId);

                    if (staffId != null && !staffId.isEmpty()) {
                        // Get staff details
                        JSONArray staffArray = SupabaseClientInstance.getInstance()
                            .selectFromTable("staff",
                                "staff_id=eq." + staffId,
                                accessToken != null ? accessToken : "");

                        if (staffArray.length() > 0) {
                            JSONObject staff = staffArray.getJSONObject(0);
                            final String staffName = staff.optString("full_name", "Staff Member");
                            final String staffPhone = staff.optString("contact_number", "N/A");

                            System.out.println("ValetPaymentSuccess: Staff found - " + staffName + ", Phone: " + staffPhone);

                            runOnUiThread(() -> {
                                tvStaffName.setText("Staff Name: " + staffName);
                                tvStaffPhone.setText("Contact: " + staffPhone);
                            });
                        } else {
                            System.out.println("ValetPaymentSuccess: No staff found with ID: " + staffId);
                            runOnUiThread(() -> {
                                tvStaffName.setText("Staff Name: Not assigned yet");
                                tvStaffPhone.setText("Contact: Please check at counter");
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            tvStaffName.setText("Staff Name: Not assigned yet");
                            tvStaffPhone.setText("Contact: Please check at counter");
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        tvStaffName.setText("Staff Name: Not assigned yet");
                        tvStaffPhone.setText("Contact: Please check at counter");
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ValetPaymentSuccess: Error loading staff details: " + e.getMessage());
                runOnUiThread(() -> {
                    tvStaffName.setText("Staff Name: Not available");
                    tvStaffPhone.setText("Contact: Please check at counter");
                    Toast.makeText(this, "Error loading staff details", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back, must use button to go to dashboard
        Toast.makeText(this, "Please use 'Back to Dashboard' button", Toast.LENGTH_SHORT).show();
    }
}
