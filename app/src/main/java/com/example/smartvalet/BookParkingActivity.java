package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class BookParkingActivity extends AppCompatActivity {

    private TextView tvEventName, tvEventDate;
    private EditText editVehicleNumber, editVehicleModel, editVehicleColor, editParkingTime;
    private Button btnBookParking;
    private String eventId, eventName, eventDate, customerEmail, accessToken;
    private int selectedHour = -1, selectedMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_parking);

        eventId = getIntent().getStringExtra("event_id");
        eventName = getIntent().getStringExtra("event_name");
        eventDate = getIntent().getStringExtra("event_date");
        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvEventName = findViewById(R.id.tvEventName);
        tvEventDate = findViewById(R.id.tvEventDate);
        editVehicleNumber = findViewById(R.id.editVehicleNumber);
        editVehicleModel = findViewById(R.id.editVehicleModel);
        editVehicleColor = findViewById(R.id.editVehicleColor);
        editParkingTime = findViewById(R.id.editParkingTime);
        btnBookParking = findViewById(R.id.btnBookParking);

        tvEventName.setText("Event: " + eventName);
        tvEventDate.setText("Date: " + eventDate);

        // Make parking time field non-editable and show time picker on click
        editParkingTime.setFocusable(false);
        editParkingTime.setClickable(true);
        editParkingTime.setOnClickListener(v -> showTimePicker());

        btnBookParking.setOnClickListener(v -> bookParking());
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
            (view, hourOfDay, minute) -> {
                selectedHour = hourOfDay;
                selectedMinute = minute;
                
                // Format and display the selected time
                String timeString = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                editParkingTime.setText(timeString);
            }, currentHour, currentMinute, true); // true for 24-hour format

        timePickerDialog.show();
    }

    private void bookParking() {
        String vehicleNumber = editVehicleNumber.getText().toString().trim();
        String vehicleModel = editVehicleModel.getText().toString().trim();
        String vehicleColor = editVehicleColor.getText().toString().trim();
        String parkingTime = editParkingTime.getText().toString().trim();

        if (vehicleNumber.isEmpty() || vehicleModel.isEmpty() || vehicleColor.isEmpty() || parkingTime.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnBookParking.setEnabled(false);
        btnBookParking.setText("Booking...");

        new Thread(() -> {
            try {
                // First, check if parking spots are available
                System.out.println("BookParking: Checking availability for event: " + eventId);
                
                org.json.JSONArray eventCheck = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", "event_id=eq." + eventId, accessToken != null ? accessToken : "");
                
                if (eventCheck.length() > 0) {
                    JSONObject eventObj = eventCheck.getJSONObject(0);
                    int totalSpots = eventObj.optInt("total_vehicle_spots", 0);
                    int occupiedSpots = eventObj.optInt("occupied_spots", 0);
                    int availableSpots = totalSpots - occupiedSpots;
                    
                    System.out.println("BookParking: Total: " + totalSpots + ", Occupied: " + occupiedSpots + ", Available: " + availableSpots);
                    
                    if (availableSpots <= 0) {
                        System.out.println("BookParking: No spots available!");
                        runOnUiThread(() -> {
                            btnBookParking.setEnabled(true);
                            btnBookParking.setText("Book Parking");
                            Toast.makeText(this, "Sorry, " + eventName + " is fully booked. No parking spots available.", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                }
                
                // Generate parking spot number (simple logic - can be enhanced)
                int parkingSpot = (int)(Math.random() * 250) + 1;
                
                // Parse parking time (format: HH:mm)
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                Date startTime = timeFormat.parse(parkingTime);
                if (startTime == null) {
                    throw new Exception("Invalid time format. Use HH:mm");
                }

                // Generate QR code (UUID based)
                String qrCode = UUID.randomUUID().toString();

                // Create booking
                JSONObject bookingData = new JSONObject();
                bookingData.put("customer_email", customerEmail);
                bookingData.put("event_id", eventId);
                bookingData.put("parking_spot_number", parkingSpot);
                bookingData.put("vehicle_number", vehicleNumber);
                bookingData.put("vehicle_model", vehicleModel);
                bookingData.put("vehicle_color", vehicleColor);
                bookingData.put("booking_date", eventDate);
                bookingData.put("start_time", eventDate + "T" + parkingTime + ":00");
                bookingData.put("payment_status", "pending");
                bookingData.put("qr_code", qrCode);
                bookingData.put("status", "active");

                JSONObject bookingResponse = SupabaseClientInstance.getInstance()
                    .insertIntoTable("booked_parking", bookingData, accessToken != null ? accessToken : "");

                String bookingId = "";
                if (bookingResponse.has("booking_id")) {
                    bookingId = bookingResponse.optString("booking_id", "");
                } else if (bookingResponse.has("id")) {
                    bookingId = bookingResponse.optString("id", "");
                }

                // NOTE: Do NOT increment occupied_spots here!
                // PaymentActivity will increment it after successful payment
                // This prevents double-counting (booking + payment = 2x increment)

                // Capture variables as final for lambda
                final String finalBookingId = bookingId;
                final String finalQrCode = qrCode;
                final int finalParkingSpot = parkingSpot;
                final String finalEventName = eventName;
                final String finalCustomerEmail = customerEmail;
                final String finalAccessToken = accessToken;

                // Navigate to payment
                runOnUiThread(() -> {
                    btnBookParking.setEnabled(true);
                    btnBookParking.setText("Book Parking");
                    
                    Intent intent = new Intent(BookParkingActivity.this, PaymentActivity.class);
                    intent.putExtra("booking_id", finalBookingId);
                    intent.putExtra("qr_code", finalQrCode);
                    intent.putExtra("event_id", eventId);
                    intent.putExtra("event_name", finalEventName);
                    intent.putExtra("parking_spot", finalParkingSpot);
                    intent.putExtra("customer_email", finalCustomerEmail);
                    intent.putExtra("access_token", finalAccessToken);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnBookParking.setEnabled(true);
                    btnBookParking.setText("Book Parking");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}

