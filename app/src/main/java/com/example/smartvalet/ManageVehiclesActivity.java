package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONArray;
import org.json.JSONObject;

public class ManageVehiclesActivity extends AppCompatActivity {

    private LinearLayout vehiclesContainer;
    private EditText editSearchVehicle;
    private Button btnSearch;
    private String staffEmail, accessToken, assignedEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);

        staffEmail = getIntent().getStringExtra("staff_email");
        accessToken = getIntent().getStringExtra("access_token");

        vehiclesContainer = findViewById(R.id.eventsContainer);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Hide the FAB button (it's for creating events, not needed here)
        com.google.android.material.floatingactionbutton.FloatingActionButton fabCreateEvent = 
            findViewById(R.id.fabCreateEvent);
        if (fabCreateEvent != null) {
            fabCreateEvent.setVisibility(View.GONE);
        }

        // Add search functionality
        LinearLayout searchLayout = new LinearLayout(this);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchLayout.setPadding(0, 0, 0, 16);

        editSearchVehicle = new EditText(this);
        editSearchVehicle.setHint("Search by vehicle number or spot");
        editSearchVehicle.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        editSearchVehicle.setPadding(12, 12, 12, 12);
        editSearchVehicle.setBackgroundResource(R.drawable.input_bg);

        btnSearch = new Button(this);
        btnSearch.setText("Search");
        btnSearch.setBackgroundColor(0xFF7C3AED);
        btnSearch.setTextColor(0xFFFFFFFF);
        btnSearch.setOnClickListener(v -> searchVehicles());

        searchLayout.addView(editSearchVehicle);
        searchLayout.addView(btnSearch);

        vehiclesContainer.addView(searchLayout);

        // Load assigned event first, THEN load vehicles
        loadAssignedEventId();
    }

    private void loadAssignedEventId() {
        new Thread(() -> {
            try {
                System.out.println("ManageVehicles: Loading assigned event for staff: " + staffEmail);
                
                JSONArray staffInfo = SupabaseClientInstance.getInstance()
                    .selectFromTable("staff", "email_id=eq." + staffEmail, 
                        accessToken != null ? accessToken : "");

                if (staffInfo.length() > 0) {
                    JSONObject staff = staffInfo.getJSONObject(0);
                    assignedEventId = staff.optString("assigned_event_id", "");
                    System.out.println("ManageVehicles: Assigned event ID: " + assignedEventId);
                    
                    // Now load vehicles for this event
                    loadVehicles();
                } else {
                    System.out.println("ManageVehicles: No staff found with email: " + staffEmail);
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Staff not found", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ManageVehicles: Error loading staff info: " + e.getMessage());
            }
        }).start();
    }

    private void loadVehicles() {
        runOnUiThread(() -> vehiclesContainer.removeAllViews());
        
        new Thread(() -> {
            try {
                System.out.println("ManageVehicles: Loading parked vehicles for event: " + assignedEventId);
                
                if (assignedEventId == null || assignedEventId.isEmpty()) {
                    runOnUiThread(() -> {
                        TextView noEvent = new TextView(this);
                        noEvent.setText("You are not assigned to any event yet.");
                        noEvent.setTextSize(16);
                        noEvent.setPadding(32, 32, 32, 32);
                        noEvent.setTextColor(0xFFF44336);
                        vehiclesContainer.addView(noEvent);
                    });
                    return;
                }
                
                // Get all QR logs where vehicles have entered but not exited for this event
                // We need to find bookings that have an "entry" scan but no "exit" scan
                String qrFilter = "scan_type=eq.entry&select=booking_id";
                JSONArray entryScans = SupabaseClientInstance.getInstance()
                    .selectFromTable("qr_logs", qrFilter + "&order=scan_timestamp.desc", 
                        accessToken != null ? accessToken : "");
                
                System.out.println("ManageVehicles: Found " + entryScans.length() + " entry scans");
                
                // Get exit scans to exclude them
                String exitFilter = "scan_type=eq.exit&select=booking_id";
                JSONArray exitScans = SupabaseClientInstance.getInstance()
                    .selectFromTable("qr_logs", exitFilter, 
                        accessToken != null ? accessToken : "");
                
                System.out.println("ManageVehicles: Found " + exitScans.length() + " exit scans");
                
                // Create set of booking IDs that have exited
                java.util.Set<String> exitedBookings = new java.util.HashSet<>();
                for (int i = 0; i < exitScans.length(); i++) {
                    try {
                        JSONObject scan = exitScans.getJSONObject(i);
                        String bookingId = scan.optString("booking_id", "");
                        if (!bookingId.isEmpty()) {
                            exitedBookings.add(bookingId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                // Get unique booking IDs that have entered but not exited
                java.util.Set<String> activeBookingIds = new java.util.HashSet<>();
                for (int i = 0; i < entryScans.length(); i++) {
                    try {
                        JSONObject scan = entryScans.getJSONObject(i);
                        String bookingId = scan.optString("booking_id", "");
                        if (!bookingId.isEmpty() && !exitedBookings.contains(bookingId)) {
                            activeBookingIds.add(bookingId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                System.out.println("ManageVehicles: Found " + activeBookingIds.size() + " currently parked vehicles");
                
                // Now fetch the actual booking details for active bookings in this event
                if (activeBookingIds.isEmpty()) {
                    runOnUiThread(() -> {
                        TextView noVehicles = new TextView(this);
                        noVehicles.setText("No vehicles currently parked at this event.");
                        noVehicles.setTextSize(16);
                        noVehicles.setPadding(32, 32, 32, 32);
                        noVehicles.setTextColor(0xFF666666);
                        vehiclesContainer.addView(noVehicles);
                    });
                    return;
                }
                
                // Fetch all booking details
                for (String bookingId : activeBookingIds) {
                    try {
                        String bookingFilter = "booking_id=eq." + bookingId;
                        JSONArray bookingData = SupabaseClientInstance.getInstance()
                            .selectFromTable("booked_parking", bookingFilter, 
                                accessToken != null ? accessToken : "");
                        
                        if (bookingData.length() > 0) {
                            JSONObject booking = bookingData.getJSONObject(0);
                            String eventId = booking.optString("event_id", "");
                            
                            // Only show vehicles for this staff's assigned event
                            if (eventId.equals(assignedEventId)) {
                                // Fetch customer details using customer_email (not customer_id)
                                String customerEmail = booking.optString("customer_email", "");
                                String customerName = "Unknown";
                                String customerPhone = "Unknown";
                                
                                System.out.println("ManageVehicles: Fetching customer for email: " + customerEmail);
                                
                                if (!customerEmail.isEmpty()) {
                                    try {
                                        JSONArray customerData = SupabaseClientInstance.getInstance()
                                            .selectFromTable("customer", "email_id=eq." + customerEmail, 
                                                accessToken != null ? accessToken : "");
                                        
                                        System.out.println("ManageVehicles: Customer query returned " + customerData.length() + " results");
                                        
                                        if (customerData.length() > 0) {
                                            JSONObject customer = customerData.getJSONObject(0);
                                            
                                            // Customer table has first_name and last_name, not full_name
                                            String firstName = customer.optString("first_name", "");
                                            String lastName = customer.optString("last_name", "");
                                            customerName = (firstName + " " + lastName).trim();
                                            
                                            // If name is empty, try falling back to full_name (just in case)
                                            if (customerName.isEmpty()) {
                                                customerName = customer.optString("full_name", "Unknown");
                                            }
                                            
                                            customerPhone = customer.optString("contact_number", "Unknown");
                                            System.out.println("ManageVehicles: Found customer: " + customerName + " | " + customerPhone);
                                        } else {
                                            System.out.println("ManageVehicles: No customer found for email: " + customerEmail);
                                        }
                                    } catch (Exception e) {
                                        System.err.println("ManageVehicles: Error fetching customer: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                } else {
                                    System.out.println("ManageVehicles: No customer_email in booking");
                                }
                                
                                // Add customer info to booking object
                                booking.put("customer_name", customerName);
                                booking.put("customer_phone", customerPhone);
                                
                                final JSONObject finalBooking = booking;
                                runOnUiThread(() -> createVehicleCard(finalBooking));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error fetching booking: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading vehicles: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void searchVehicles() {
        String searchTerm = editSearchVehicle.getText().toString().trim();
        if (searchTerm.isEmpty()) {
            loadVehicles();
            return;
        }

        runOnUiThread(() -> vehiclesContainer.removeAllViews());
        
        new Thread(() -> {
            try {
                System.out.println("ManageVehicles: Searching for: " + searchTerm);
                
                if (assignedEventId == null || assignedEventId.isEmpty()) {
                    runOnUiThread(() -> {
                        TextView noEvent = new TextView(this);
                        noEvent.setText("You are not assigned to any event yet.");
                        noEvent.setTextSize(16);
                        noEvent.setPadding(32, 32, 32, 32);
                        noEvent.setTextColor(0xFFF44336);
                        vehiclesContainer.addView(noEvent);
                    });
                    return;
                }

                // Get all QR logs where vehicles have entered but not exited
                String qrFilter = "scan_type=eq.entry&select=booking_id";
                JSONArray entryScans = SupabaseClientInstance.getInstance()
                    .selectFromTable("qr_logs", qrFilter, accessToken != null ? accessToken : "");
                
                String exitFilter = "scan_type=eq.exit&select=booking_id";
                JSONArray exitScans = SupabaseClientInstance.getInstance()
                    .selectFromTable("qr_logs", exitFilter, accessToken != null ? accessToken : "");
                
                // Create set of exited bookings
                java.util.Set<String> exitedBookings = new java.util.HashSet<>();
                for (int i = 0; i < exitScans.length(); i++) {
                    exitedBookings.add(exitScans.getJSONObject(i).optString("booking_id", ""));
                }
                
                // Get active booking IDs
                java.util.Set<String> activeBookingIds = new java.util.HashSet<>();
                for (int i = 0; i < entryScans.length(); i++) {
                    String bookingId = entryScans.getJSONObject(i).optString("booking_id", "");
                    if (!bookingId.isEmpty() && !exitedBookings.contains(bookingId)) {
                        activeBookingIds.add(bookingId);
                    }
                }

                int[] count = {0};
                
                // Search through active bookings
                for (String bookingId : activeBookingIds) {
                    try {
                        JSONArray bookingData = SupabaseClientInstance.getInstance()
                            .selectFromTable("booked_parking", "booking_id=eq." + bookingId, 
                                accessToken != null ? accessToken : "");
                        
                        if (bookingData.length() > 0) {
                            JSONObject booking = bookingData.getJSONObject(0);
                            String eventId = booking.optString("event_id", "");
                            
                            // Only search in this staff's event
                            if (eventId.equals(assignedEventId)) {
                                String vehicleNumber = booking.optString("vehicle_number", "");
                                String spot = String.valueOf(booking.optInt("parking_spot_number", 0));
                                
                                // Check if matches search term
                                if (vehicleNumber.toLowerCase().contains(searchTerm.toLowerCase()) || 
                                    spot.contains(searchTerm)) {
                                    
                                    // Fetch customer details using customer_email
                                    String customerEmail = booking.optString("customer_email", "");
                                    if (!customerEmail.isEmpty()) {
                                        try {
                                            JSONArray customerData = SupabaseClientInstance.getInstance()
                                                .selectFromTable("customer", "email_id=eq." + customerEmail, 
                                                    accessToken != null ? accessToken : "");
                                            if (customerData.length() > 0) {
                                                JSONObject customer = customerData.getJSONObject(0);
                                                
                                                // Customer table has first_name and last_name
                                                String firstName = customer.optString("first_name", "");
                                                String lastName = customer.optString("last_name", "");
                                                String fullName = (firstName + " " + lastName).trim();
                                                
                                                if (fullName.isEmpty()) {
                                                    fullName = customer.optString("full_name", "Unknown");
                                                }
                                                
                                                booking.put("customer_name", fullName);
                                                booking.put("customer_phone", customer.optString("contact_number", "Unknown"));
                                            } else {
                                                booking.put("customer_name", "Unknown");
                                                booking.put("customer_phone", "Unknown");
                                            }
                                        } catch (Exception e) {
                                            booking.put("customer_name", "Unknown");
                                            booking.put("customer_phone", "Unknown");
                                        }
                                    } else {
                                        booking.put("customer_name", "Unknown");
                                        booking.put("customer_phone", "Unknown");
                                    }
                                    
                                    final JSONObject finalBooking = booking;
                                    runOnUiThread(() -> createVehicleCard(finalBooking));
                                    count[0]++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                if (count[0] == 0) {
                    runOnUiThread(() -> {
                        TextView noResults = new TextView(this);
                        noResults.setText("No vehicles found matching: " + searchTerm);
                        noResults.setTextSize(16);
                        noResults.setPadding(32, 32, 32, 32);
                        noResults.setTextColor(0xFF666666);
                        vehiclesContainer.addView(noResults);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error searching: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void createVehicleCard(JSONObject booking) {
        try {
            String bookingId = booking.optString("booking_id", "");
            int spot = booking.optInt("parking_spot_number", 0);
            String vehicleNumber = booking.optString("vehicle_number", "N/A");
            String vehicleModel = booking.optString("vehicle_model", "N/A");
            String vehicleColor = booking.optString("vehicle_color", "N/A");
            String customerName = booking.optString("customer_name", "Unknown");
            String customerPhone = booking.optString("customer_phone", "Unknown");
            String bookingType = booking.optString("booking_type", "regular");

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

            // Title with booking type badge
            TextView tvTitle = new TextView(this);
            String badge = bookingType.equals("valet") ? "🚗 VALET SERVICE" : "📅 BOOKED";
            tvTitle.setText(badge);
            tvTitle.setTextSize(14);
            tvTitle.setTextColor(bookingType.equals("valet") ? 0xFF7C3AED : 0xFF059669);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setPadding(0, 0, 0, 12);
            card.addView(tvTitle);

            // Vehicle details
            TextView tvVehicle = new TextView(this);
            tvVehicle.setText(String.format(
                "🚗 Vehicle: %s\n" +
                "   Model: %s | Color: %s",
                vehicleNumber, vehicleModel, vehicleColor
            ));
            tvVehicle.setTextSize(16);
            tvVehicle.setTextColor(0xFF1F2937);
            tvVehicle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvVehicle.setPadding(0, 0, 0, 12);
            card.addView(tvVehicle);

            // Spot number
            TextView tvSpot = new TextView(this);
            tvSpot.setText(String.format("🅿️ Parking Spot: %d", spot));
            tvSpot.setTextSize(15);
            tvSpot.setTextColor(0xFF7C3AED);
            tvSpot.setTypeface(null, android.graphics.Typeface.BOLD);
            tvSpot.setPadding(0, 0, 0, 12);
            card.addView(tvSpot);

            // Customer details
            TextView tvCustomer = new TextView(this);
            tvCustomer.setText(String.format(
                "👤 Customer: %s\n" +
                "📱 Phone: %s",
                customerName, customerPhone
            ));
            tvCustomer.setTextSize(14);
            tvCustomer.setTextColor(0xFF4B5563);
            tvCustomer.setPadding(0, 0, 0, 16);
            card.addView(tvCustomer);

            // Status indicator
            TextView tvStatus = new TextView(this);
            tvStatus.setText("✅ Currently Parked");
            tvStatus.setTextSize(13);
            tvStatus.setTextColor(0xFF059669);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding(12, 8, 12, 8);
            tvStatus.setBackgroundColor(0xFFD1FAE5);
            card.addView(tvStatus);

            vehiclesContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

