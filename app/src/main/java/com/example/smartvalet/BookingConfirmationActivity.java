package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.smartvalet.utils.QRCodeGenerator;
import android.graphics.Bitmap;

public class BookingConfirmationActivity extends AppCompatActivity {

    private ImageView qrCodeImage;
    private TextView tvBookingDetails;
    private Button btnDone;
    private String bookingId, qrCode, eventName;
    private int parkingSpot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        bookingId = getIntent().getStringExtra("booking_id");
        qrCode = getIntent().getStringExtra("qr_code");
        eventName = getIntent().getStringExtra("event_name");
        parkingSpot = getIntent().getIntExtra("parking_spot", 0);

        // Handle null values
        if (qrCode == null || qrCode.isEmpty() || qrCode.equals("null")) {
            qrCode = bookingId; // Use booking ID as fallback
        }
        if (eventName == null || eventName.isEmpty() || eventName.equals("null")) {
            eventName = "Event";
        }

        qrCodeImage = findViewById(R.id.qrCodeImage);
        tvBookingDetails = findViewById(R.id.tvBookingDetails);
        btnDone = findViewById(R.id.btnDone);
        
        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Display booking details
        String spotText = (parkingSpot == 0) ? "To be assigned" : String.valueOf(parkingSpot);
        tvBookingDetails.setText(String.format(
            "Event: %s\nParking Spot: %s\nQR Code: %s\n\nPlease show this QR code at the parking location.",
            eventName, spotText, qrCode
        ));

        // Generate and display QR code
        Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrCode, 400, 400);
        if (qrBitmap != null) {
            qrCodeImage.setImageBitmap(qrBitmap);
        }

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(BookingConfirmationActivity.this, CustomerActivity.class);
            intent.putExtra("customer_email", getIntent().getStringExtra("customer_email"));
            intent.putExtra("access_token", getIntent().getStringExtra("access_token"));
            startActivity(intent);
            finish();
        });
    }
}

