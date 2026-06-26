package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Random;

public class AddStaffToEventActivity extends AppCompatActivity {

    private EditText editFullName, editContactNumber, editLocation, editAge, editEmail, editPassword;
    private Spinner spinnerExperience;
    private TextView tvSelectedEvent, tvSelectedStaff;
    private Button btnSelectEvent, btnCreateStaff, btnSelectExistingStaff, btnAssignStaff;
    private RadioGroup radioGroupMode;
    private RadioButton radioAssignExisting, radioCreateNew;
    private LinearLayout layoutCreateNewForm, layoutAssignExistingForm;
    private String selectedEventId, selectedEventName, adminEmail, accessToken;
    private String selectedStaffId, selectedStaffName;
    private JSONArray eventsArray, staffArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_staff);

        adminEmail = getIntent().getStringExtra("admin_email");
        accessToken = getIntent().getStringExtra("access_token");
        selectedEventId = getIntent().getStringExtra("event_id");
        selectedEventName = getIntent().getStringExtra("event_name");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Initialize mode selection
        radioGroupMode = findViewById(R.id.radioGroupMode);
        radioAssignExisting = findViewById(R.id.radioAssignExisting);
        radioCreateNew = findViewById(R.id.radioCreateNew);
        layoutCreateNewForm = findViewById(R.id.layoutCreateNewForm);
        layoutAssignExistingForm = findViewById(R.id.layoutAssignExistingForm);

        // Initialize Create New form fields
        editFullName = findViewById(R.id.editFullName);
        editContactNumber = findViewById(R.id.editContactNumber);
        editLocation = findViewById(R.id.editLocation);
        editAge = findViewById(R.id.editAge);
        spinnerExperience = findViewById(R.id.spinnerExperience);
        tvSelectedEvent = findViewById(R.id.tvSelectedEvent);
        btnSelectEvent = findViewById(R.id.btnSelectEvent);
        btnCreateStaff = findViewById(R.id.btnCreateStaff);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);

        // Initialize Assign Existing form fields
        tvSelectedStaff = findViewById(R.id.tvSelectedStaff);
        btnSelectExistingStaff = findViewById(R.id.btnSelectExistingStaff);
        btnAssignStaff = findViewById(R.id.btnAssignStaff);

        // Mode selection listener
        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAssignExisting) {
                layoutAssignExistingForm.setVisibility(View.VISIBLE);
                layoutCreateNewForm.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioCreateNew) {
                layoutCreateNewForm.setVisibility(View.VISIBLE);
                layoutAssignExistingForm.setVisibility(View.GONE);
            }
        });

        // Set default mode
        radioAssignExisting.setChecked(true);

        CheckBox cbShowPassword = findViewById(R.id.cbShowPassword);
        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int start = editPassword.getSelectionStart();
            int end = editPassword.getSelectionEnd();
            if (isChecked) {
                editPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                editPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            editPassword.setSelection(start, end);
        });
        
        // Setup experience spinner
        String[] experienceLevels = {
            "NEW COMER",
            "6 - 12 MONTHS EXPERIENCE",
            "MORE THAN 12 MONTHS EXPERIENCE"
        };
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, experienceLevels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExperience.setAdapter(adapter);

        if (selectedEventId != null && selectedEventName != null) {
            tvSelectedEvent.setText("Event: " + selectedEventName);
            btnSelectEvent.setVisibility(android.view.View.GONE);
        } else {
            tvSelectedEvent.setText("No event selected");
            btnSelectEvent.setOnClickListener(v -> showEventSelector());
        }

        btnSelectExistingStaff.setOnClickListener(v -> showStaffSelector());
        btnAssignStaff.setOnClickListener(v -> assignExistingStaff());
        btnCreateStaff.setOnClickListener(v -> createStaff());

        loadEvents();
        loadAllStaff();
    }

    private void loadEvents() {
        new Thread(() -> {
            try {
                eventsArray = SupabaseClientInstance.getInstance()
                    .selectFromTable("events", "order=event_date.desc", accessToken != null ? accessToken : "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadAllStaff() {
        new Thread(() -> {
            try {
                staffArray = SupabaseClientInstance.getInstance()
                    .selectFromTable("staff", "order=full_name.asc", accessToken != null ? accessToken : "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showStaffSelector() {
        if (staffArray == null || staffArray.length() == 0) {
            Toast.makeText(this, "No staff members available. Please create a new staff member.", Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedEventId == null || selectedEventId.isEmpty()) {
            Toast.makeText(this, "Please select an event first", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Staff Member");
        
        String[] staffNames = new String[staffArray.length()];
        try {
            for (int i = 0; i < staffArray.length(); i++) {
                JSONObject staff = staffArray.getJSONObject(i);
                String name = staff.optString("full_name", "Unknown");
                String experience = staff.optString("experience_level", "");
                String assignedEvent = staff.optString("assigned_event_id", "");
                
                // Show if staff is already assigned
                if (assignedEvent != null && !assignedEvent.isEmpty() && !assignedEvent.equals(selectedEventId)) {
                    staffNames[i] = name + " (Already assigned to another event)";
                } else {
                    staffNames[i] = name + " - " + experience;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        builder.setItems(staffNames, (dialog, which) -> {
            try {
                JSONObject selectedStaff = staffArray.getJSONObject(which);
                selectedStaffId = selectedStaff.optString("staff_id", "");
                selectedStaffName = selectedStaff.optString("full_name", "");
                String assignedEvent = selectedStaff.optString("assigned_event_id", "");
                
                // Warn if staff is already assigned to another event
                if (assignedEvent != null && !assignedEvent.isEmpty() && !assignedEvent.equals(selectedEventId)) {
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Staff Already Assigned")
                        .setMessage(selectedStaffName + " is already assigned to another event. Do you want to reassign them to " + selectedEventName + "?")
                        .setPositiveButton("Yes, Reassign", (d, w) -> {
                            tvSelectedStaff.setText("Selected: " + selectedStaffName);
                        })
                        .setNegativeButton("Cancel", (d, w) -> {
                            selectedStaffId = null;
                            selectedStaffName = null;
                        })
                        .show();
                } else {
                    tvSelectedStaff.setText("Selected: " + selectedStaffName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        builder.show();
    }

    private void assignExistingStaff() {
        if (selectedEventId == null || selectedEventId.isEmpty()) {
            Toast.makeText(this, "Please select an event", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedStaffId == null || selectedStaffId.isEmpty()) {
            Toast.makeText(this, "Please select a staff member", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAssignStaff.setEnabled(false);
        btnAssignStaff.setText("Assigning...");

        new Thread(() -> {
            try {
                // Update staff record with new event assignment
                JSONObject updateData = new JSONObject();
                updateData.put("assigned_event_id", selectedEventId);
                updateData.put("associated_parking", selectedEventName);

                String filter = "staff_id=eq." + selectedStaffId;
                SupabaseClientInstance.getInstance()
                    .updateTable("staff", filter, updateData, accessToken != null ? accessToken : "");

                runOnUiThread(() -> {
                    btnAssignStaff.setEnabled(true);
                    btnAssignStaff.setText("Assign Staff");
                    Toast.makeText(this, selectedStaffName + " assigned to " + selectedEventName + " successfully!", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnAssignStaff.setEnabled(true);
                    btnAssignStaff.setText("Assign Staff");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showEventSelector() {
        if (eventsArray == null || eventsArray.length() == 0) {
            Toast.makeText(this, "No events available. Create an event first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Simple dialog to select event (can be enhanced with AlertDialog)
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Event");
        
        String[] eventNames = new String[eventsArray.length()];
        try {
            for (int i = 0; i < eventsArray.length(); i++) {
                JSONObject event = eventsArray.getJSONObject(i);
                eventNames[i] = event.optString("event_name", "Unnamed Event");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        builder.setItems(eventNames, (dialog, which) -> {
            try {
                JSONObject selectedEvent = eventsArray.getJSONObject(which);
                selectedEventId = selectedEvent.optString("event_id", "");
                selectedEventName = selectedEvent.optString("event_name", "");
                tvSelectedEvent.setText("Event: " + selectedEventName);
                btnSelectEvent.setVisibility(android.view.View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        builder.show();
    }

    private void createStaff() {
        String fullName = editFullName.getText().toString().trim();
        String contactNumber = editContactNumber.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String ageStr = editAge.getText().toString().trim();
        String experience = spinnerExperience.getSelectedItem().toString();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (fullName.isEmpty() || contactNumber.isEmpty() || location.isEmpty() || ageStr.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate contact number is exactly 10 digits
        if (contactNumber.length() != 10 || !contactNumber.matches("\\d{10}")) {
            Toast.makeText(this, "Please enter a valid 10-digit contact number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate age
        try {
            int age = Integer.parseInt(ageStr);
            if (age < 18) {
                Toast.makeText(this, "Age must be at least 18 years", Toast.LENGTH_SHORT).show();
                return;
            }
            if (age > 99) {
                Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedEventId == null || selectedEventId.isEmpty()) {
            Toast.makeText(this, "Please select an event", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(ageStr);
            
            btnCreateStaff.setEnabled(false);
            btnCreateStaff.setText("Creating...");

            // Add +91 prefix to contact number
            final String fullContactNumber = "+91" + contactNumber;

            new Thread(() -> {
                try {
                    // Use admin-provided credentials
                    String loginId = generateLoginId(fullName, contactNumber);
                    
                    // Get event name for associated parking
                    String associatedParking = selectedEventName;

                    // Create staff in Supabase staff table
                    JSONObject staffData = new JSONObject();
                    staffData.put("full_name", fullName);
                    staffData.put("email_id", email);
                    staffData.put("password", password); // Note: In production, hash passwords
                    staffData.put("contact_number", fullContactNumber);
                    staffData.put("location", location);
                    staffData.put("age", age);
                    staffData.put("experience_level", experience);
                    staffData.put("login_id", loginId);
                    staffData.put("associated_parking", associatedParking);
                    staffData.put("assigned_event_id", selectedEventId);

                    JSONObject staffResponse = SupabaseClientInstance.getInstance()
                        .insertIntoTable("staff", staffData, accessToken != null ? accessToken : "");

                    // Also create auth user for staff login
                    try {
                        SupabaseClientInstance.getInstance().signUp(email, password);
                    } catch (Exception authError) {
                        System.out.println("Note: Auth user might already exist or creation failed: " + authError.getMessage());
                    }

                    // Get staff ID from response
                    String staffId = "";
                    if (staffResponse.has("staff_id")) {
                        staffId = staffResponse.optString("staff_id", "");
                    }

                    runOnUiThread(() -> {
                        btnCreateStaff.setEnabled(true);
                        btnCreateStaff.setText("Create Staff");
                        
                        // Show success with credentials
                        String message = String.format(
                            "Staff created successfully!\n\nEmail: %s\nLogin ID: %s\n\nPlease note these credentials.",
                            email, loginId
                        );
                        
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("Staff Created")
                            .setMessage(message)
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        btnCreateStaff.setEnabled(true);
                        btnCreateStaff.setText("Create Staff");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Age must be a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateLoginId(String fullName, String contactNumber) {
        // Generate login ID from name and phone last 4 digits
        String namePart = fullName.toLowerCase().replaceAll("[^a-z]", "").substring(0, Math.min(6, fullName.length()));
        String phonePart = contactNumber.length() >= 4 
            ? contactNumber.substring(contactNumber.length() - 4) 
            : new Random().nextInt(9000) + 1000 + "";
        return namePart + "." + phonePart;
    }

    private String generatePassword() {
        // Generate random 8-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}

