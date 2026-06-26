package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.ImageHelper;
import com.example.smartvalet.utils.SupabaseClientInstance;
import java.util.Calendar;

public class CustomerActivity extends AppCompatActivity {

    private TextView tvCustomerName;
    private Button btnViewEvents, btnMyBookings, btnValetRequest;
    private ImageView imgProfile;
    private String customerEmail;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        tvCustomerName = findViewById(R.id.tvCustomerName);
        btnViewEvents = findViewById(R.id.btnViewEvents);
        btnMyBookings = findViewById(R.id.btnMyBookings);
        btnValetRequest = findViewById(R.id.btnValetRequest);
        imgProfile = findViewById(R.id.btnBack);
        
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> openProfile());
        
        // Back arrow
        ImageView imgBackArrow = findViewById(R.id.imgBackArrow);
        imgBackArrow.setOnClickListener(v -> finish());

        if (customerEmail != null) {
            // Get greeting based on time
            String greeting = getGreeting();
            
            System.out.println("========================================");
            System.out.println("CustomerActivity: LOADING GREETING");
            System.out.println("CustomerActivity: Customer Email: " + customerEmail);
            System.out.println("CustomerActivity: Access Token: " + (accessToken != null ? "Present" : "Null"));
            System.out.println("========================================");
            
            // Fetch customer name and show it
            new Thread(() -> {
                try {
                org.json.JSONArray customers = SupabaseClientInstance.getInstance()
                        .selectFromTable("customer", "email_id=eq." + customerEmail, accessToken);
                    
                    System.out.println("CustomerActivity: Query result length: " + customers.length());
                    
                    String name = customerEmail;
                    String photoBase64 = null;
                    if (customers.length() > 0) {
                        org.json.JSONObject obj = customers.getJSONObject(0);
                        System.out.println("CustomerActivity: Full customer data: " + obj.toString());
                        
                        // Check what fields are available
                        System.out.println("CustomerActivity: Has first_name? " + obj.has("first_name"));
                        System.out.println("CustomerActivity: Has last_name? " + obj.has("last_name"));
                        System.out.println("CustomerActivity: Has full_name? " + obj.has("full_name"));
                        
                        String firstName = obj.optString("first_name", "");
                        String lastName = obj.optString("last_name", "");
                        
                        // Fallback to full_name if first_name is empty (for backwards compatibility)
                        if (firstName.isEmpty() && obj.has("full_name")) {
                            String fullName = obj.optString("full_name", "");
                            if (!fullName.isEmpty()) {
                                // Split full_name to get first name
                                firstName = fullName.split(" ")[0];
                                System.out.println("CustomerActivity: Using full_name, extracted: " + firstName);
                            }
                        }
                        
                        name = firstName.isEmpty() ? customerEmail.split("@")[0] : firstName;
                        photoBase64 = obj.optString("profile_photo_base64", "");
                        
                        System.out.println("CustomerActivity: firstName: '" + firstName + "'");
                        System.out.println("CustomerActivity: lastName: '" + lastName + "'");
                        System.out.println("CustomerActivity: Final display name: '" + name + "'");
                    } else {
                        System.out.println("CustomerActivity: No customer found for email: " + customerEmail);
                    }
                    final String finalName = name;
                    final String finalPhoto = photoBase64;
                    runOnUiThread(() -> {
                        String displayText = greeting + ", " + finalName;
                        System.out.println("CustomerActivity: Setting greeting: " + displayText);
                        tvCustomerName.setText(displayText);
                        
                        // Load profile photo if exists
                        if (finalPhoto != null && !finalPhoto.isEmpty()) {
                            Bitmap bitmap = ImageHelper.decodeBase64(finalPhoto);
                            if (bitmap != null && imgProfile != null) {
                                imgProfile.setImageBitmap(bitmap);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("CustomerActivity: Error loading data: " + e.getMessage());
                    runOnUiThread(() -> tvCustomerName.setText(greeting + ", Customer"));
                }
            }).start();
        }

        // Card clicks
        findViewById(R.id.cardViewEvents).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerActivity.this, ViewAvailableEventsActivity.class);
            intent.putExtra("customer_email", customerEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        findViewById(R.id.cardMyBookings).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerActivity.this, MyBookingsActivity.class);
            intent.putExtra("customer_email", customerEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        findViewById(R.id.cardValetRequest).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerActivity.this, ValetRequestActivity.class);
            intent.putExtra("customer_email", customerEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });

        findViewById(R.id.cardMyValetRequests).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerActivity.this, MyValetRequestsActivity.class);
            intent.putExtra("customer_email", customerEmail);
            intent.putExtra("access_token", accessToken);
            startActivity(intent);
        });
    }

    private String getGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        
        // 12am to 11:59am = Good Morning
        if (hourOfDay >= 0 && hourOfDay < 12) {
            return "Good Morning";
        }
        // 12pm to 4pm = Good Afternoon
        else if (hourOfDay >= 12 && hourOfDay <= 16) {
            return "Good Afternoon";
        }
        // 4:01pm to 11:59pm = Good Evening
        else {
            return "Good Evening";
        }
    }
    
    private void openProfile() {
        Intent intent = new Intent(CustomerActivity.this, ProfileActivity.class);
        intent.putExtra("customer_email", customerEmail);
        intent.putExtra("access_token", accessToken);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // Go back normally - users can use back button or onBackPressed
        finish();
    }
}

