package com.example.smartvalet;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.ImageHelper;
import com.example.smartvalet.utils.SupabaseClientInstance;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile, btnBack;
    private TextView tvName, tvEmail, tvPhone;
    private FloatingActionButton btnChangePhoto;
    private Button btnEditProfile, btnLogout;
    
    private String customerEmail;
    private String accessToken;
    private String currentPhotoBase64;
    
    // Activity result launcher for image picker
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Get customer info from intent
        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        // Initialize views
        imgProfile = findViewById(R.id.imgProfile);
        btnBack = findViewById(R.id.btnBack);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        // Register image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleImageSelected(uri);
                }
            }
        );

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Change photo button
        btnChangePhoto.setOnClickListener(v -> openImagePicker());

        // Edit profile button
        btnEditProfile.setOnClickListener(v -> { Toast.makeText(this, "Edit profile feature coming soon", Toast.LENGTH_SHORT).show(); });

        // Logout button
        btnLogout.setOnClickListener(v -> {
            // Clear session and go to welcome screen
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Load customer data
        loadCustomerData();
    }

    private void openImagePicker() {
        // Launch image picker for gallery
        imagePickerLauncher.launch("image/*");
    }

    private void handleImageSelected(Uri imageUri) {
        // Show loading
        Toast.makeText(this, "Processing image...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                System.out.println("ProfileActivity: Starting image processing for: " + customerEmail);
                
                // Compress and encode image to Base64
                String base64Image = ImageHelper.compressAndEncode(this, imageUri);
                
                if (base64Image == null) {
                    System.out.println("ProfileActivity: Image compression failed");
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                System.out.println("ProfileActivity: Image compressed, size: " + base64Image.length() + " chars");

                // Update profile photo in database
                JSONObject updateData = new JSONObject();
                updateData.put("profile_photo_base64", base64Image);

                System.out.println("ProfileActivity: Updating database for email: " + customerEmail);

                JSONArray result = SupabaseClientInstance.getInstance()
                    .updateTable("customer", 
                        "email_id=eq." + customerEmail,
                        updateData,
                        accessToken != null ? accessToken : "");

                System.out.println("ProfileActivity: Update result length: " + (result != null ? result.length() : "null"));

                if (result != null && result.length() > 0) {
                    System.out.println("ProfileActivity: Photo uploaded successfully");
                    currentPhotoBase64 = base64Image;
                    
                    runOnUiThread(() -> {
                        // Decode and display image
                        Bitmap bitmap = ImageHelper.decodeBase64(base64Image);
                        if (bitmap != null) {
                            imgProfile.setImageBitmap(bitmap);
                            Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    System.out.println("ProfileActivity: Update returned empty result");
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Failed to upload photo - Check RLS policies", Toast.LENGTH_LONG).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ProfileActivity: Error uploading photo: " + e.getMessage());
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void loadCustomerData() {
        new Thread(() -> {
            try {
                System.out.println("ProfileActivity: Loading data for email: " + customerEmail);
                
                // Query customer data - use eq for exact match
                JSONArray customers = SupabaseClientInstance.getInstance()
                    .selectFromTable("customer",
                        "email_id=eq." + customerEmail,
                        accessToken != null ? accessToken : "");

                System.out.println("ProfileActivity: Query result length: " + customers.length());
                
                if (customers.length() > 0) {
                    JSONObject customer = customers.getJSONObject(0);
                    System.out.println("ProfileActivity: Customer data: " + customer.toString());
                    
                    final String firstName = customer.optString("first_name", "");
                    final String lastName = customer.optString("last_name", "");
                    final String fullName = (firstName + " " + lastName).trim();
                    final String email = customer.optString("email_id", customerEmail);
                    final String phone = customer.optString("contact_number", "+91");
                    currentPhotoBase64 = customer.optString("profile_photo_base64", "");

                    System.out.println("ProfileActivity: Displaying - Name: " + fullName + ", Email: " + email);

                    runOnUiThread(() -> {
                        tvName.setText(fullName.isEmpty() ? "User" : fullName);
                        tvEmail.setText(email);
                        tvPhone.setText(phone);

                        // Load profile photo if exists
                        if (currentPhotoBase64 != null && !currentPhotoBase64.isEmpty()) {
                            Bitmap bitmap = ImageHelper.decodeBase64(currentPhotoBase64);
                            if (bitmap != null) {
                                imgProfile.setImageBitmap(bitmap);
                            }
                        }
                    });
                } else {
                    System.out.println("ProfileActivity: No customer found for email: " + customerEmail);
                    runOnUiThread(() -> 
                        Toast.makeText(this, "Customer data not found", Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ProfileActivity: Error loading data: " + e.getMessage());
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

}

