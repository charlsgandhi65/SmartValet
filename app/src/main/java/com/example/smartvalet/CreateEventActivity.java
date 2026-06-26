package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateEventActivity extends AppCompatActivity {

    private EditText editEventName, editEventAddress, editEventDate, editHours, editSpots;
    private EditText editBasePrice, editExtraChargePerHour;
    private Button btnCreateEvent;
    private String adminEmail;
    private String accessToken;
    private Calendar selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        adminEmail = getIntent().getStringExtra("admin_email");
        accessToken = getIntent().getStringExtra("access_token");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        editEventName = findViewById(R.id.editEventName);
        editEventAddress = findViewById(R.id.editEventAddress);
        editEventDate = findViewById(R.id.editEventDate);
        editHours = findViewById(R.id.editHours);
        editSpots = findViewById(R.id.editSpots);
        editBasePrice = findViewById(R.id.editBasePrice);
        editExtraChargePerHour = findViewById(R.id.editExtraChargePerHour);
        btnCreateEvent = findViewById(R.id.btnCreateEvent);

        // Make date field non-editable and show date picker on click
        editEventDate.setFocusable(false);
        editEventDate.setClickable(true);
        editEventDate.setOnClickListener(v -> showDatePicker());

        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                selectedDate = Calendar.getInstance();
                selectedDate.set(selectedYear, selectedMonth, selectedDay);
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                editEventDate.setText(sdf.format(selectedDate.getTime()));
            }, year, month, day);

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        
        // Set maximum date to 1 month from today
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.MONTH, 1);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());
        
        datePickerDialog.show();
    }

    private void createEvent() {
        String eventName = editEventName.getText().toString().trim();
        String address = editEventAddress.getText().toString().trim();
        String date = editEventDate.getText().toString().trim();
        String hoursStr = editHours.getText().toString().trim();
        String spotsStr = editSpots.getText().toString().trim();
        String basePriceStr = editBasePrice.getText().toString().trim();
        String extraChargeStr = editExtraChargePerHour.getText().toString().trim();

        if (eventName.isEmpty() || address.isEmpty() || date.isEmpty() || 
            hoursStr.isEmpty() || spotsStr.isEmpty() || basePriceStr.isEmpty() || extraChargeStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int hours = Integer.parseInt(hoursStr);
            int spots = Integer.parseInt(spotsStr);
            double basePrice = Double.parseDouble(basePriceStr);
            double extraCharge = Double.parseDouble(extraChargeStr);

            if (basePrice <= 0) {
                Toast.makeText(this, "Base price must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (extraCharge < 0) {
                Toast.makeText(this, "Extra charge cannot be negative", Toast.LENGTH_SHORT).show();
                return;
            }

            btnCreateEvent.setEnabled(false);
            btnCreateEvent.setText("Creating...");

            new Thread(() -> {
                try {
                    JSONObject eventData = new JSONObject();
                    eventData.put("event_name", eventName);
                    eventData.put("event_address", address);
                    eventData.put("event_date", date);
                    eventData.put("hours_of_event", hours);
                    eventData.put("total_vehicle_spots", spots);
                    eventData.put("base_price", basePrice);
                    eventData.put("extra_charge_per_hour", extraCharge);
                    eventData.put("created_by", adminEmail);

                    JSONObject response = SupabaseClientInstance.getInstance()
                        .insertIntoTable("events", eventData, accessToken != null ? accessToken : "");

                    runOnUiThread(() -> {
                        btnCreateEvent.setEnabled(true);
                        btnCreateEvent.setText("Create Event");
                        
                        // Get event_id from response for staff assignment
                        String eventId = "";
                        try {
                            if (response.has("event_id")) {
                                eventId = response.optString("event_id", "");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        final String finalEventId = eventId;
                        
                        // Ask if admin wants to assign staff now
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("Event Created Successfully!")
                            .setMessage("Would you like to assign staff to this event now?")
                            .setPositiveButton("Yes, Assign Staff", (dialog, which) -> {
                                Intent intent = new Intent(CreateEventActivity.this, AddStaffToEventActivity.class);
                                intent.putExtra("admin_email", adminEmail);
                                intent.putExtra("access_token", accessToken);
                                intent.putExtra("event_id", finalEventId);
                                intent.putExtra("event_name", eventName);
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton("No, Later", (dialog, which) -> finish())
                            .show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        btnCreateEvent.setEnabled(true);
                        btnCreateEvent.setText("Create Event");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}

