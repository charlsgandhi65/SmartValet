package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONArray;
import org.json.JSONObject;

public class ValetRequestActivity extends AppCompatActivity {

    private Spinner spinnerRequestType, spinnerBooking;
    private Button btnSubmitRequest;
    private TextView tvRequestTypeLabel;
    private String customerEmail, accessToken;
    private JSONArray bookingsArray;
    private boolean hasActiveBookings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_valet_request);

        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        spinnerRequestType = findViewById(R.id.spinnerRequestType);
        spinnerBooking = findViewById(R.id.spinnerBooking);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        tvRequestTypeLabel = findViewById(R.id.tvRequestType);

        btnSubmitRequest.setOnClickListener(v -> submitRequest());

        // Load bookings first to determine if customer has parking booked
        loadBookings();
    }

    private void loadBookings() {
        new Thread(() -> {
            try {
                System.out.println("ValetRequest: Loading active bookings for: " + customerEmail);
                
                bookingsArray = SupabaseClientInstance.getInstance()
                    .selectFromTable("booked_parking",
                        "customer_email=eq." + customerEmail + "&status=eq.active",
                        accessToken != null ? accessToken : "");

                System.out.println("ValetRequest: Found " + bookingsArray.length() + " active bookings");

                runOnUiThread(() -> {
                    if (bookingsArray == null || bookingsArray.length() == 0) {
                        // No active bookings - customer hasn't booked parking
                        // Show only valet parking service option
                        hasActiveBookings = false;
                        
                        System.out.println("ValetRequest: No active bookings, showing valet parking option");
                        
                        // Hide booking selection
                        spinnerBooking.setEnabled(false);
                        spinnerBooking.setVisibility(android.view.View.GONE);
                        findViewById(R.id.tvSelectBooking).setVisibility(android.view.View.GONE);
                        
                        // Show only valet service option (park car)
                        String[] requestTypes = {"Park My Car (Valet Service)"};
                        android.widget.ArrayAdapter<String> typeAdapter = new android.widget.ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, requestTypes);
                        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerRequestType.setAdapter(typeAdapter);
                        
                        Toast.makeText(this, "Submit a valet parking request to have staff park your car", Toast.LENGTH_SHORT).show();
                        
                    } else {
                        // Has active bookings - show only car fetch option
                        hasActiveBookings = true;
                        
                        System.out.println("ValetRequest: Customer has active bookings, showing car fetch only");
                        
                        // Hide request type spinner since it's always "car_fetch"
                        spinnerRequestType.setVisibility(android.view.View.GONE);
                        if (tvRequestTypeLabel != null) {
                            tvRequestTypeLabel.setText("Service: Car Fetch (Retrieve your parked vehicle)");
                        }
                        
                        // Populate booking spinner with vehicle numbers
                        String[] bookingLabels = new String[bookingsArray.length()];
                        try {
                            for (int i = 0; i < bookingsArray.length(); i++) {
                                JSONObject booking = bookingsArray.getJSONObject(i);
                                String vehicleNumber = booking.optString("vehicle_number", "Unknown");
                                String vehicleModel = booking.optString("vehicle_model", "");
                                int spotNumber = booking.optInt("parking_spot_number", 0);
                                
                                // Show vehicle number and model
                                bookingLabels[i] = vehicleNumber + 
                                    (vehicleModel.isEmpty() ? "" : " (" + vehicleModel + ")") +
                                    " - Spot " + spotNumber;
                                    
                                System.out.println("ValetRequest: Booking " + i + ": " + bookingLabels[i]);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("ValetRequest: Error parsing bookings: " + e.getMessage());
                        }
                        
                        android.widget.ArrayAdapter<String> bookingAdapter = new android.widget.ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, bookingLabels);
                        bookingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerBooking.setAdapter(bookingAdapter);
                        
                        Toast.makeText(this, "Select your vehicle to request car fetch", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ValetRequest: Error loading bookings: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading bookings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void submitRequest() {
        if (!hasActiveBookings) {
            // Customer wants to park car (valet service) - submit request for staff approval
            btnSubmitRequest.setEnabled(false);
            btnSubmitRequest.setText("Submitting...");

            new Thread(() -> {
                try {
                    System.out.println("ValetRequest: Submitting valet parking request (no booking)");

                    JSONObject requestData = new JSONObject();
                    requestData.put("customer_email", customerEmail);
                    requestData.put("request_type", "valet_service"); // Using valid constraint value
                    requestData.put("request_status", "pending");
                    requestData.put("request_details", "Customer requested valet parking service - awaiting staff approval");
                    requestData.put("booking_id", JSONObject.NULL); // Explicitly set to NULL

                    SupabaseClientInstance.getInstance()
                        .insertIntoTable("parking_requests", requestData, accessToken != null ? accessToken : "");

                    System.out.println("ValetRequest: Valet parking request submitted");

                    runOnUiThread(() -> {
                        btnSubmitRequest.setEnabled(true);
                        btnSubmitRequest.setText("Submit Request");
                        Toast.makeText(this, "Request submitted! Please wait for staff approval. You'll be notified when accepted.", Toast.LENGTH_LONG).show();
                        finish();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("ValetRequest: Error submitting valet parking request: " + e.getMessage());
                    runOnUiThread(() -> {
                        btnSubmitRequest.setEnabled(true);
                        btnSubmitRequest.setText("Submit Request");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
            return;
        }
        
        // Customer has active bookings - car fetch request
        if (bookingsArray == null || bookingsArray.length() == 0) {
            Toast.makeText(this, "No active bookings available", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedBookingIndex = spinnerBooking.getSelectedItemPosition();
        if (selectedBookingIndex < 0) {
            Toast.makeText(this, "Please select your vehicle", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitRequest.setEnabled(false);
        btnSubmitRequest.setText("Submitting...");

        new Thread(() -> {
            try {
                JSONObject selectedBooking = bookingsArray.getJSONObject(selectedBookingIndex);
                String bookingId = selectedBooking.optString("booking_id", "");
                String vehicleNumber = selectedBooking.optString("vehicle_number", "");
                
                // For customers with bookings, always use "car_fetch"
                String requestType = "car_fetch";

                System.out.println("ValetRequest: Submitting car fetch request");
                System.out.println("ValetRequest: Booking ID: " + bookingId);
                System.out.println("ValetRequest: Vehicle: " + vehicleNumber);

                // Check if there's already a pending or assigned request for this booking
                JSONArray existingRequests = SupabaseClientInstance.getInstance()
                    .selectFromTable("parking_requests",
                        "booking_id=eq." + bookingId + "&request_status=in.(pending,assigned)",
                        accessToken != null ? accessToken : "");

                if (existingRequests.length() > 0) {
                    String status = existingRequests.getJSONObject(0).optString("request_status", "");
                    System.out.println("ValetRequest: Request already exists with status: " + status);
                    
                    runOnUiThread(() -> {
                        btnSubmitRequest.setEnabled(true);
                        btnSubmitRequest.setText("Submit Request");
                        if (status.equals("assigned")) {
                            Toast.makeText(this, "Your car fetch request has already been accepted! Staff is bringing your vehicle.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "You already have a pending request for this vehicle. Please wait for staff to accept.", Toast.LENGTH_LONG).show();
                        }
                        finish();
                    });
                    return;
                }

                JSONObject requestData = new JSONObject();
                requestData.put("booking_id", bookingId);
                requestData.put("customer_email", customerEmail);
                requestData.put("request_type", requestType);
                requestData.put("request_status", "pending");
                requestData.put("request_details", "Customer requested car fetch for vehicle: " + vehicleNumber);

                SupabaseClientInstance.getInstance()
                    .insertIntoTable("parking_requests", requestData, accessToken != null ? accessToken : "");

                System.out.println("ValetRequest: Request submitted successfully");

                runOnUiThread(() -> {
                    btnSubmitRequest.setEnabled(true);
                    btnSubmitRequest.setText("Submit Request");
                    Toast.makeText(this, "✓ Request submitted! Your car will be brought to the exit by staff.", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("ValetRequest: Error submitting request: " + e.getMessage());
                runOnUiThread(() -> {
                    btnSubmitRequest.setEnabled(true);
                    btnSubmitRequest.setText("Submit Request");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}