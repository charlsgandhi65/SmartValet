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

public class ViewRequestsActivity extends AppCompatActivity {

    private LinearLayout requestsContainer;
    private String staffEmail, accessToken, staffId, assignedEventId;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);

        staffEmail = getIntent().getStringExtra("staff_email");
        accessToken = getIntent().getStringExtra("access_token");

        requestsContainer = findViewById(R.id.eventsContainer);
        
        // Hide the FAB since we don't need it for requests
        View fab = findViewById(R.id.fabCreateEvent);
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Load staff info first, which will then load requests
        loadStaffId();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reload if not already loading and if we have assigned event
        if (!isLoading && assignedEventId != null) {
            loadRequests();
        }
    }

    private void loadStaffId() {
        new Thread(() -> {
            try {
                System.out.println("ViewRequests: Loading staff info for: " + staffEmail);
                
                JSONArray staffInfo = SupabaseClientInstance.getInstance()
                    .selectFromTable("staff", "email_id=eq." + staffEmail, 
                        accessToken != null ? accessToken : "");

                if (staffInfo.length() > 0) {
                    JSONObject staff = staffInfo.getJSONObject(0);
                    staffId = staff.optString("staff_id", "");
                    assignedEventId = staff.optString("assigned_event_id", "");
                    
                    System.out.println("ViewRequests: Staff ID: " + staffId + ", Assigned Event: " + assignedEventId);
                    
                    // Now load requests for this event
                    loadRequests();
                } else {
                    System.out.println("ViewRequests: No staff found");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ViewRequests: Error loading staff: " + e.getMessage());
            }
        }).start();
    }

    private void loadRequests() {
        if (isLoading) return;
        isLoading = true;
        runOnUiThread(() -> requestsContainer.removeAllViews());
        
        new Thread(() -> {
            try {
                System.out.println("ViewRequests: Loading requests for event: " + assignedEventId);
                
                // Get two types of requests:
                // 1. Requests with booking_id (car_fetch) - filter by event through bookings
                // 2. Requests without booking_id (valet_service) - show all pending
                
                JSONArray allRequests = new JSONArray();
                
                // First, get car_fetch requests for this event's bookings
                String bookingFilter = "status=eq.active";
                if (assignedEventId != null && !assignedEventId.isEmpty()) {
                    bookingFilter += "&event_id=eq." + assignedEventId;
                }
                
                JSONArray bookings = SupabaseClientInstance.getInstance()
                    .selectFromTable("booked_parking", bookingFilter, 
                        accessToken != null ? accessToken : "");
                
                System.out.println("ViewRequests: Found " + bookings.length() + " bookings for this event");
                
                // Extract booking IDs
                StringBuilder bookingIds = new StringBuilder();
                for (int i = 0; i < bookings.length(); i++) {
                    String bookingId = bookings.getJSONObject(i).optString("booking_id", "");
                    if (!bookingId.isEmpty()) {
                        if (bookingIds.length() > 0) bookingIds.append(",");
                        bookingIds.append(bookingId);
                    }
                }
                
                // Get requests with booking_id (car_fetch)
                if (bookingIds.length() > 0) {
                    System.out.println("ViewRequests: Booking IDs: " + bookingIds.toString());
                    
                    JSONArray bookingRequests = SupabaseClientInstance.getInstance()
                        .selectFromTable("parking_requests", 
                            "request_status=in.(pending,assigned)&booking_id=in.(" + bookingIds.toString() + ")&order=created_at.desc", 
                            accessToken != null ? accessToken : "");
                    
                    System.out.println("ViewRequests: Found " + bookingRequests.length() + " car_fetch requests");
                    
                    // Add to allRequests
                    for (int i = 0; i < bookingRequests.length(); i++) {
                        allRequests.put(bookingRequests.getJSONObject(i));
                    }
                }
                
                // Get valet_service requests (no booking_id yet)
                JSONArray valetRequests = SupabaseClientInstance.getInstance()
                    .selectFromTable("parking_requests", 
                        "request_status=in.(pending,assigned)&request_type=eq.valet_service&booking_id=is.null&order=created_at.desc", 
                        accessToken != null ? accessToken : "");
                
                System.out.println("ViewRequests: Found " + valetRequests.length() + " valet_service requests");
                
                // Add to allRequests
                for (int i = 0; i < valetRequests.length(); i++) {
                    allRequests.put(valetRequests.getJSONObject(i));
                }

                System.out.println("ViewRequests: Total requests: " + allRequests.length());

                runOnUiThread(() -> {
                    isLoading = false;
                    if (allRequests.length() == 0) {
                        TextView noRequests = new TextView(this);
                        noRequests.setText("No pending requests.");
                        noRequests.setTextSize(16);
                        noRequests.setPadding(32, 32, 32, 32);
                        noRequests.setTextColor(0xFF666666);
                        requestsContainer.addView(noRequests);
                    } else {
                        for (int i = 0; i < allRequests.length(); i++) {
                            try {
                                JSONObject request = allRequests.getJSONObject(i);
                                createRequestCard(request);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    isLoading = false;
                    Toast.makeText(this, "Error loading requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void createRequestCard(JSONObject request) {
        try {
            String requestId = request.optString("request_id", "");
            String bookingId = request.optString("booking_id", "");
            String requestType = request.optString("request_type", "");
            String requestStatus = request.optString("request_status", "");
            String customerEmail = request.optString("customer_email", "");

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

            TextView tvDetails = new TextView(this);
            tvDetails.setText(String.format(
                "Type: %s\nStatus: %s\nCustomer: %s",
                requestType, requestStatus, customerEmail
            ));
            tvDetails.setTextSize(16);
            tvDetails.setTextColor(0xFF1F2937);
            tvDetails.setPadding(0, 0, 0, 16);
            card.addView(tvDetails);

            if (requestStatus.equals("pending")) {
                // Show only Accept button for pending requests
                Button btnAccept = new Button(this);
                btnAccept.setText("Accept Request");
                btnAccept.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                btnAccept.setBackgroundColor(0xFF4CAF50);
                btnAccept.setTextColor(0xFFFFFFFF);
                btnAccept.setOnClickListener(v -> acceptRequest(requestId));
                card.addView(btnAccept);
            } else if (requestStatus.equals("assigned")) {
                // Show only Complete button for assigned requests
                Button btnComplete = new Button(this);
                btnComplete.setText("Mark as Completed");
                btnComplete.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT));
                btnComplete.setBackgroundColor(0xFF2196F3);
                btnComplete.setTextColor(0xFFFFFFFF);
                btnComplete.setOnClickListener(v -> completeRequest(requestId));
                card.addView(btnComplete);
            }

            requestsContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void acceptRequest(String requestId) {
        System.out.println("ViewRequests: Accepting request: " + requestId);
        System.out.println("ViewRequests: Staff ID: " + staffId);
        
        new Thread(() -> {
            try {
                JSONObject updateData = new JSONObject();
                updateData.put("request_status", "assigned");
                updateData.put("assigned_staff_id", staffId);

                System.out.println("ViewRequests: Updating request to 'assigned' status");
                org.json.JSONArray result = SupabaseClientInstance.getInstance()
                    .updateTable("parking_requests", "request_id=eq." + requestId, updateData, 
                        accessToken != null ? accessToken : "");
                
                System.out.println("ViewRequests: Update successful, result length: " + result.length());

                runOnUiThread(() -> {
                    Toast.makeText(this, "Request accepted! Reloading...", Toast.LENGTH_SHORT).show();
                    loadRequests();
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ViewRequests: Error accepting request: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void completeRequest(String requestId) {
        new Thread(() -> {
            try {
                JSONObject updateData = new JSONObject();
                updateData.put("request_status", "completed");
                updateData.put("completed_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", 
                    java.util.Locale.US).format(new java.util.Date()));

                SupabaseClientInstance.getInstance()
                    .updateTable("parking_requests", "request_id=eq." + requestId, updateData, 
                        accessToken != null ? accessToken : "");

                runOnUiThread(() -> {
                    Toast.makeText(this, "Request completed", Toast.LENGTH_SHORT).show();
                    loadRequests();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}

