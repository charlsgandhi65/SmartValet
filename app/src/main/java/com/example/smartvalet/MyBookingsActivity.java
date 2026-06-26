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

public class MyBookingsActivity extends AppCompatActivity {

    private LinearLayout bookingsContainer;
    private String customerEmail, accessToken;
    private boolean isLoading = false;
    private Set<String> displayedVehicleNumbers = new HashSet<>(); // Track unique vehicle numbers only

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);

        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        System.out.println("MyBookings onCreate: Customer Email = " + customerEmail);
        System.out.println("MyBookings onCreate: Access Token = " + (accessToken != null ? "Present" : "Null"));

        bookingsContainer = findViewById(R.id.eventsContainer);
        
        // Hide the FAB since we don't need it for bookings
        View fab = findViewById(R.id.fabCreateEvent);
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        loadMyBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Don't reload - already loaded in onCreate
        // Uncomment if you want to refresh on resume:
        // loadMyBookings();
    }

    private void loadMyBookings() {
        if (isLoading) return; // Prevent duplicate loading
        isLoading = true;
        
        bookingsContainer.removeAllViews(); // Clear before loading
        displayedVehicleNumbers.clear(); // Clear vehicle tracking
        
        new Thread(() -> {
            try {
                System.out.println("========================================");
                System.out.println("MyBookings: LOADING BOOKINGS");
                System.out.println("MyBookings: Customer Email: " + customerEmail);
                System.out.println("MyBookings: Query Filter: customer_email=eq." + customerEmail);
                System.out.println("========================================");
                
                // Use eq for exact match, not ilike which does pattern matching
                JSONArray bookings = SupabaseClientInstance.getInstance()
                    .selectFromTable("booked_parking", 
                        "customer_email=eq." + customerEmail + "&order=created_at.desc", 
                        accessToken != null ? accessToken : "");

                System.out.println("MyBookings: Raw response length: " + bookings.length());
                
                // Debug: Print all bookings with their customer emails
                for (int i = 0; i < bookings.length(); i++) {
                    try {
                        JSONObject booking = bookings.getJSONObject(i);
                        String bookingEmail = booking.optString("customer_email", "");
                        String bookingId = booking.optString("booking_id", "");
                        String vehicle = booking.optString("vehicle_number", "");
                        
                        System.out.println("----------------------------------------");
                        System.out.println("Booking " + (i+1) + ":");
                        System.out.println("  Booking ID: " + bookingId);
                        System.out.println("  Customer Email: " + bookingEmail);
                        System.out.println("  Vehicle: " + vehicle);
                        System.out.println("  Match? " + bookingEmail.equals(customerEmail));
                        System.out.println("----------------------------------------");
                        
                        // Extra validation: Only add if email matches exactly
                        if (!bookingEmail.equals(customerEmail)) {
                            System.out.println("WARNING: Email mismatch! Expected: " + customerEmail + ", Got: " + bookingEmail);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                System.out.println("========================================");

                runOnUiThread(() -> {
                    if (bookings.length() == 0) {
                        TextView noBookings = new TextView(this);
                        noBookings.setText("No bookings found for: " + customerEmail);
                        noBookings.setTextSize(16);
                        noBookings.setPadding(32, 32, 32, 32);
                        noBookings.setTextColor(0xFF666666);
                        bookingsContainer.addView(noBookings);
                    } else {
                        // Simple approach: Show only the first (most recent) booking for each vehicle
                        // Since we order by created_at.desc, the first occurrence is the most recent
                        int displayCount = 0;
                        for (int i = 0; i < bookings.length(); i++) {
                            try {
                                JSONObject booking = bookings.getJSONObject(i);
                                String bookingEmail = booking.optString("customer_email", "");
                                String bookingId = booking.optString("booking_id", "");
                                String vehicleNumber = booking.optString("vehicle_number", "").trim().toUpperCase();
                                
                                System.out.println("========================================");
                                System.out.println("Checking booking #" + (i+1) + ": " + bookingId);
                                System.out.println("  Vehicle: " + vehicleNumber);
                                System.out.println("  Email match: " + bookingEmail.equals(customerEmail));
                                System.out.println("  Vehicle already shown: " + displayedVehicleNumbers.contains(vehicleNumber));
                                
                                // Only show if: correct email AND vehicle not shown yet
                                if (bookingEmail.equals(customerEmail) && !displayedVehicleNumbers.contains(vehicleNumber)) {
                                    displayedVehicleNumbers.add(vehicleNumber); // Mark vehicle as shown
                                    createBookingCard(booking);
                                    displayCount++;
                                    System.out.println("  ✓ DISPLAYED");
                                } else {
                                    if (!bookingEmail.equals(customerEmail)) {
                                        System.out.println("  ✗ SKIPPED: Wrong email");
                                    } else {
                                        System.out.println("  ✗ SKIPPED: Vehicle " + vehicleNumber + " already displayed");
                                    }
                                }
                                System.out.println("========================================");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        
                        System.out.println("MyBookings: Displayed " + displayCount + " out of " + bookings.length() + " bookings");
                        
                        if (displayCount == 0) {
                            TextView noBookings = new TextView(this);
                            noBookings.setText("No bookings found for: " + customerEmail);
                            noBookings.setTextSize(16);
                            noBookings.setPadding(32, 32, 32, 32);
                            noBookings.setTextColor(0xFF666666);
                            bookingsContainer.addView(noBookings);
                        }
                    }
                    isLoading = false; // Reset flag after loading completes
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading bookings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    isLoading = false; // Reset flag on error too
                });
            }
        }).start();
    }

    private void createBookingCard(JSONObject booking) {
        try {
            String bookingId = booking.optString("booking_id", "");
            String eventId = booking.optString("event_id", "");
            int spot = booking.optInt("parking_spot_number", 0);
            String vehicleNumber = booking.optString("vehicle_number", "");
            String vehicleModel = booking.optString("vehicle_model", "");
            String status = booking.optString("status", "");
            String paymentStatus = booking.optString("payment_status", "");
            String qrCode = booking.optString("qr_code", "");

            // If QR code is null or empty, generate one
            if (qrCode == null || qrCode.isEmpty() || qrCode.equals("null")) {
                qrCode = bookingId; // Use booking ID as QR code
            }

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
                "Spot: %d\nVehicle: %s (%s)\nStatus: %s\nPayment: %s",
                spot, vehicleNumber, vehicleModel, status, paymentStatus
            ));
            tvDetails.setTextSize(16);
            tvDetails.setTextColor(0xFF1F2937);
            tvDetails.setPadding(0, 0, 0, 16);
            card.addView(tvDetails);

            // Check exit status and add appropriate UI
            final String finalQrCode = qrCode;
            checkExitStatusAndAddButton(bookingId, card, eventId, spot, finalQrCode);

            bookingsContainer.addView(card);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkExitStatusAndAddButton(String bookingId, LinearLayout card, 
                                              String eventId, int spot, String qrCode) {
        new Thread(() -> {
            try {
                // Check if vehicle has exited by querying qr_logs
                JSONArray exitScans = SupabaseClientInstance.getInstance()
                    .selectFromTable("qr_logs",
                        "booking_id=eq." + bookingId + "&scan_type=eq.exit",
                        accessToken != null ? accessToken : "");
                
                boolean hasExited = exitScans.length() > 0;
                
                runOnUiThread(() -> {
                    if (hasExited) {
                        // Show thank you message instead of QR button
                        TextView tvThankYou = new TextView(this);
                        tvThankYou.setText("🎉 Thank you for visiting us!! 🎉");
                        tvThankYou.setTextSize(18);
                        tvThankYou.setTextColor(0xFF10B981);
                        tvThankYou.setGravity(android.view.Gravity.CENTER);
                        tvThankYou.setPadding(16, 16, 16, 16);
                        tvThankYou.setBackgroundColor(0xFFD1FAE5);
                        card.addView(tvThankYou);
                    } else {
                        // Show QR button
                        addQRButton(card, bookingId, eventId, spot, qrCode);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> addQRButton(card, bookingId, eventId, spot, qrCode));
            }
        }).start();
    }

    private void addQRButton(LinearLayout card, String bookingId, 
                             String eventId, int spot, String qrCode) {
        Button btnViewQR = new Button(this);
        btnViewQR.setText("View QR Code");
        btnViewQR.setBackgroundColor(0xFF7C3AED);
        btnViewQR.setTextColor(0xFFFFFFFF);
        
        btnViewQR.setOnClickListener(v -> {
            // Fetch event name before opening QR screen
            new Thread(() -> {
                try {
                    String eventName = "Event";
                    if (eventId != null && !eventId.isEmpty()) {
                        JSONArray events = SupabaseClientInstance.getInstance()
                            .selectFromTable("events", "event_id=eq." + eventId, 
                                accessToken != null ? accessToken : "");
                        
                        if (events.length() > 0) {
                            JSONObject event = events.getJSONObject(0);
                            eventName = event.optString("event_name", "Event");
                        }
                    }
                    
                    final String finalEventName = eventName;
                    runOnUiThread(() -> {
                        Intent intent = new Intent(MyBookingsActivity.this, BookingConfirmationActivity.class);
                        intent.putExtra("booking_id", bookingId);
                        intent.putExtra("qr_code", qrCode);
                        intent.putExtra("parking_spot", spot);
                        intent.putExtra("event_name", finalEventName);
                        intent.putExtra("customer_email", customerEmail);
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Intent intent = new Intent(MyBookingsActivity.this, BookingConfirmationActivity.class);
                        intent.putExtra("booking_id", bookingId);
                        intent.putExtra("qr_code", qrCode);
                        intent.putExtra("parking_spot", spot);
                        intent.putExtra("event_name", "Event");
                        intent.putExtra("customer_email", customerEmail);
                        startActivity(intent);
                    });
                }
            }).start();
        });
        card.addView(btnViewQR);
    }
}

