package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONObject;

public class SignUpActivity extends AppCompatActivity {

    private EditText editFirstName, editLastName, editEmail, editPhone, editPassword;
    private Button btnCreateAccount, btnGoToLogin;
    private CheckBox cbShowPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        editFirstName = findViewById(R.id.editFirstName);
        editLastName = findViewById(R.id.editLastName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editPassword = findViewById(R.id.editPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        cbShowPassword = findViewById(R.id.cbShowPassword);

        // Show/Hide password toggle
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

        // Go to Login button
        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Create Account button → insert data into Supabase
        btnCreateAccount.setOnClickListener(v -> {
            String firstName = editFirstName.getText().toString().trim();
            String lastName = editLastName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            // Validation
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Validate phone format (should be exactly 10 digits)
            if (phone.length() != 10 || !phone.matches("[0-9]{10}")) {
                Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Add +91 prefix to phone
            String fullPhone = "+91" + phone;

            // Execute signup in background thread
            final String finalFirstName = firstName;
            final String finalLastName = lastName;
            final String finalEmail = email;
            final String finalPhone = fullPhone;
            final String finalPassword = password;
            
            new Thread(() -> {
                try {
                    JSONObject response = SupabaseClientInstance.getInstance().signUp(finalEmail, finalPassword);
                    
                    // Log full response for debugging
                    System.out.println("Signup Response: " + response.toString());
                    
                    // Check for successful signup (either has access_token or user id)
                    // Also check for user object which contains the id
                    if (response.has("access_token") || response.has("id") || response.has("user")) {
                        // Extract user ID and access token from response
                        String userId = null;
                        String accessToken = response.optString("access_token", "");
                        
                        try {
                            if (response.has("user")) {
                                JSONObject user = response.getJSONObject("user");
                                userId = user.optString("id", "");
                            } else if (response.has("id")) {
                                userId = response.optString("id", "");
                            }
                        } catch (Exception e) {
                            System.out.println("Error extracting user ID: " + e.getMessage());
                        }
                        
                        // Insert into customer table
                        // Note: We'll insert even without userId since the table doesn't require it
                        try {
                            JSONObject customerData = new JSONObject();
                            customerData.put("email_id", finalEmail);
                            customerData.put("password", finalPassword);
                            customerData.put("first_name", finalFirstName);
                            customerData.put("last_name", finalLastName);
                            customerData.put("contact_number", finalPhone);
                            
                            // Insert into customer table
                            // Note: Using empty accessToken since we're using anon key for REST API
                            JSONObject insertResponse = SupabaseClientInstance.getInstance()
                                .insertIntoTable("customer", customerData, "");
                            
                            System.out.println("Customer table insert response: " + insertResponse.toString());
                            
                            runOnUiThread(() -> {
                                Toast.makeText(SignUpActivity.this, "Account created successfully! You can now login.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        } catch (Exception insertError) {
                            System.out.println("Error inserting into customer table: " + insertError.getMessage());
                            insertError.printStackTrace();
                            
                            // Try to extract error details
                            String errorMsg = insertError.getMessage();
                            String detailedError = errorMsg;
                            
                            // Check if error response contains more details
                            if (insertError.getCause() != null) {
                                String causeMsg = insertError.getCause().getMessage();
                                if (causeMsg != null && !causeMsg.isEmpty()) {
                                    detailedError = causeMsg;
                                }
                            }
                            
                            // If still null or empty, use the main error message
                            if (detailedError == null || detailedError.isEmpty()) {
                                detailedError = errorMsg != null ? errorMsg : "Unknown error";
                            }
                            
                            System.out.println("Detailed insert error: " + detailedError);
                            
                            // Make variables final for use in lambda
                            final String finalErrorMsg = errorMsg;
                            final String finalDetailedError = detailedError;
                            
                            // Show detailed error message
                            if (finalErrorMsg != null && (finalErrorMsg.contains("duplicate") || finalErrorMsg.contains("already exists") || finalErrorMsg.contains("violates unique constraint"))) {
                                runOnUiThread(() -> {
                                    Toast.makeText(SignUpActivity.this, "Account created! You can now login with your credentials.", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                });
                            } else if (finalErrorMsg != null && (finalErrorMsg.contains("permission") || finalErrorMsg.contains("RLS") || finalErrorMsg.contains("403"))) {
                                runOnUiThread(() -> {
                                    Toast.makeText(SignUpActivity.this, "Permission denied. Check Supabase RLS policies for 'customer' table.", Toast.LENGTH_LONG).show();
                                });
                            } else {
                                // Show error with details
                                runOnUiThread(() -> {
                                    String displayError;
                                    if (finalDetailedError != null && finalDetailedError.length() > 0) {
                                        if (finalDetailedError.length() > 100) {
                                            displayError = finalDetailedError.substring(0, 100) + "...";
                                        } else {
                                            displayError = finalDetailedError;
                                        }
                                    } else {
                                        displayError = (finalErrorMsg != null) ? finalErrorMsg : "Unknown error occurred";
                                    }
                                    Toast.makeText(SignUpActivity.this, "Customer table insert failed: " + displayError, Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    } else {
                        runOnUiThread(() -> {
                            // Check if it's a duplicate user error
                            String errorMsg = response.optString("msg",
                                    response.optString("error_description",
                                    response.optString("error", "")));
                            
                            if (errorMsg.toLowerCase().contains("already") || errorMsg.toLowerCase().contains("registered") || errorMsg.toLowerCase().contains("exists")) {
                                Toast.makeText(SignUpActivity.this, "This email is already registered. Please login instead.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            } else if (!errorMsg.isEmpty()) {
                                Toast.makeText(SignUpActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(SignUpActivity.this, "Signup failed. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    final String errorMessage = e.getMessage();
                    runOnUiThread(() ->
                            Toast.makeText(SignUpActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show());
                }
            }).start();
        });
    }
}
