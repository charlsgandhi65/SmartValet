package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.QRCodeGenerator;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {

    private TextView tvEventName, tvParkingSpot, tvAmount;
    private RadioGroup radioPaymentMethod;
    private ImageView qrCodeImage;
    private Button btnCompletePayment;
    private String bookingId, qrCode, eventName, eventId, requestId;
    private int parkingSpot;
    private String customerEmail, accessToken;
    private double basePrice = 100.00; // Default, will be fetched from event

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        bookingId = getIntent().getStringExtra("booking_id");
        qrCode = getIntent().getStringExtra("qr_code");
        eventName = getIntent().getStringExtra("event_name");
        eventId = getIntent().getStringExtra("event_id");
        requestId = getIntent().getStringExtra("request_id"); // Get request_id to mark as completed after payment
        parkingSpot = getIntent().getIntExtra("parking_spot", 0);
        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        tvEventName = findViewById(R.id.tvEventName);
        tvParkingSpot = findViewById(R.id.tvParkingSpot);
        tvAmount = findViewById(R.id.tvAmount);
        radioPaymentMethod = findViewById(R.id.radioPaymentMethod);
        qrCodeImage = findViewById(R.id.qrCodeImage);
        btnCompletePayment = findViewById(R.id.btnCompletePayment);

        // Display event name (or "Loading..." if null)
        tvEventName.setText("Event: " + (eventName != null ? eventName : "Loading..."));
        
        // Display parking spot (show "To be assigned" for valet bookings)
        if (parkingSpot == 0) {
            tvParkingSpot.setText("Parking Spot: To be assigned by staff");
        } else {
            tvParkingSpot.setText("Parking Spot: " + parkingSpot);
        }

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Fetch event price and display
        fetchEventPrice();

        // Generate and display QR code
        generateQRCode();

        btnCompletePayment.setOnClickListener(v -> completePayment());
    }

    private void fetchEventPrice() {
        if (eventId == null || eventId.isEmpty()) {
            tvAmount.setText("Amount: ₹" + String.format("%.2f", basePrice));
            return;
        }

        new Thread(() -> {
            try {
                org.json.JSONArray eventArray = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", "event_id=eq." + eventId, 
                        accessToken != null ? accessToken : "");

                if (eventArray.length() > 0) {
                    JSONObject event = eventArray.getJSONObject(0);
                    basePrice = event.optDouble("base_price", 100.00);
                }

                runOnUiThread(() -> {
                    tvAmount.setText("Amount: ₹" + String.format("%.2f", basePrice));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvAmount.setText("Amount: ₹" + String.format("%.2f", basePrice));
                });
            }
        }).start();
    }

    private void generateQRCode() {
        // Simple QR code generation (you can use ZXing library for better implementation)
        // For now, we'll create a simple representation
        try {
            // Generate QR code bitmap (simplified - use ZXing in production)
            Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrCode, 400, 400);
            if (qrBitmap != null) {
                qrCodeImage.setImageBitmap(qrBitmap);
            } else {
                // Fallback: Show QR code as text
                qrCodeImage.setImageDrawable(null);
                TextView qrTextView = new TextView(this);
                qrTextView.setText("QR: " + qrCode.substring(0, Math.min(20, qrCode.length())) + "...");
                // This is a placeholder - implement proper QR generation
            }
        } catch (Exception e) {
            Toast.makeText(this, "QR code will be generated after payment", Toast.LENGTH_SHORT).show();
        }
    }

    private void completePayment() {
        int selectedPaymentId = radioPaymentMethod.getCheckedRadioButtonId();
        if (selectedPaymentId == -1) {
            Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine payment method (removed cash option)
        final String paymentMethod;
        if (selectedPaymentId == R.id.radioCard) {
            paymentMethod = "card";
        } else if (selectedPaymentId == R.id.radioUPI) {
            paymentMethod = "upi";
        } else {
            paymentMethod = "card"; // Default
        }

        btnCompletePayment.setEnabled(false);
        btnCompletePayment.setText("Processing...");

        new Thread(() -> {
            try {
                // Update booking with payment info
                JSONObject paymentData = new JSONObject();
                paymentData.put("payment_status", "paid");
                paymentData.put("payment_amount", basePrice);
                paymentData.put("payment_method", paymentMethod);
                paymentData.put("status", "active"); // Mark booking as active after payment

                SupabaseClientInstance.getInstance()
                    .updateTable("booked_parking", "booking_id=eq." + bookingId, paymentData, 
                        accessToken != null ? accessToken : "");

                // Increment occupied_spots for this event (for valet parking bookings)
                if (eventId != null && !eventId.isEmpty()) {
                    try {
                        System.out.println("PaymentActivity: Incrementing occupied_spots for event: " + eventId);
                        
                        // Get current occupied_spots
                        org.json.JSONArray eventArray = SupabaseClientInstance.getInstance()
                            .selectFromTable("events", "event_id=eq." + eventId, 
                                accessToken != null ? accessToken : "");
                        
                        if (eventArray.length() > 0) {
                            JSONObject eventObj = eventArray.getJSONObject(0);
                            int currentOccupied = eventObj.optInt("occupied_spots", 0);
                            
                            // Update with incremented value
                            JSONObject updateOccupied = new JSONObject();
                            updateOccupied.put("occupied_spots", currentOccupied + 1);
                            
                            SupabaseClientInstance.getInstance()
                                .updateTable("events", "event_id=eq." + eventId, updateOccupied, 
                                    accessToken != null ? accessToken : "");
                            
                            System.out.println("PaymentActivity: Occupied spots updated from " + currentOccupied + " to " + (currentOccupied + 1));
                        }
                    } catch (Exception e) {
                        System.err.println("PaymentActivity: Error updating occupied_spots: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                // Mark valet request as completed (if this booking came from a valet request)
                if (requestId != null && !requestId.isEmpty()) {
                    try {
                        System.out.println("PaymentActivity: Marking valet request as completed: " + requestId);
                        
                        JSONObject updateRequest = new JSONObject();
                        updateRequest.put("request_status", "completed");
                        
                        SupabaseClientInstance.getInstance()
                            .updateTable("parking_requests", "request_id=eq." + requestId, updateRequest, 
                                accessToken != null ? accessToken : "");
                        
                        System.out.println("PaymentActivity: Valet request marked as completed");
                    } catch (Exception e) {
                        System.err.println("PaymentActivity: Error updating valet request: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                runOnUiThread(() -> {
                    btnCompletePayment.setEnabled(true);
                    btnCompletePayment.setText("Complete Payment");
                    
                    Toast.makeText(PaymentActivity.this, "Payment successful! Your booking is confirmed.", Toast.LENGTH_LONG).show();
                    
                    // For valet bookings, show staff details and instructions
                    if (requestId != null && !requestId.isEmpty()) {
                        Intent intent = new Intent(PaymentActivity.this, ValetPaymentSuccessActivity.class);
                        intent.putExtra("request_id", requestId);
                        intent.putExtra("customer_email", customerEmail);
                        intent.putExtra("access_token", accessToken);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // For regular bookings, show QR code confirmation
                        Intent intent = new Intent(PaymentActivity.this, BookingConfirmationActivity.class);
                        intent.putExtra("booking_id", bookingId);
                        intent.putExtra("qr_code", qrCode);
                        intent.putExtra("parking_spot", parkingSpot);
                        intent.putExtra("event_name", eventName);
                        intent.putExtra("customer_email", customerEmail);
                        startActivity(intent);
                        finish();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnCompletePayment.setEnabled(true);
                    btnCompletePayment.setText("Complete Payment");
                    Toast.makeText(PaymentActivity.this, "Payment error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}

