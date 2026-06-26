package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import android.view.*;
import org.json.JSONObject;
import com.example.smartvalet.utils.SupabaseClientInstance;

public class LoginActivity extends AppCompatActivity {

    LinearLayout roleCustomer, roleStaff, roleAdmin, loginForm;
    EditText editEmail, editPassword;
    CheckBox cbShowPassword;
    Button btnSignIn;
    TextView textSignUp;

    String selectedRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        roleCustomer = findViewById(R.id.roleCustomer);
        roleStaff = findViewById(R.id.roleStaff);
        roleAdmin = findViewById(R.id.roleAdmin);
        loginForm = findViewById(R.id.loginForm);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        cbShowPassword = findViewById(R.id.cbShowPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        textSignUp = findViewById(R.id.textSignUp);

        View.OnClickListener roleClickListener = view -> {
            resetRoles();
            view.setBackgroundResource(R.drawable.role_box_selected);
            loginForm.setVisibility(View.VISIBLE);

            if (view == roleCustomer) {
                selectedRole = "Customer";
                textSignUp.setVisibility(View.VISIBLE);
            } else {
                selectedRole = view == roleStaff ? "Staff" : "Admin";
                textSignUp.setVisibility(View.GONE);
            }
        };

        roleCustomer.setOnClickListener(roleClickListener);
        roleStaff.setOnClickListener(roleClickListener);
        roleAdmin.setOnClickListener(roleClickListener);

        btnSignIn.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim().toLowerCase(); // Convert to lowercase
            String password = editPassword.getText().toString(); // Don't trim password - keep exact

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedRole.isEmpty()) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading (optional - you can add a progress bar later)
            btnSignIn.setEnabled(false);
            btnSignIn.setText("Logging in...");

            // Execute login in background thread
            final String finalEmail = email;
            final String finalPassword = password;
            final String finalSelectedRole = selectedRole;
            
            new Thread(() -> {
                try {
                    // For Admin and Staff, check database directly first (they may not be in Supabase Auth)
                    // For Customer, try database first, then fallback to Supabase Auth
                    if (finalSelectedRole.equals("Admin") || finalSelectedRole.equals("Staff") || finalSelectedRole.equals("Customer")) {
                        String tableName = finalSelectedRole.equals("Admin") ? "administration" : 
                                         (finalSelectedRole.equals("Staff") ? "staff" : "customer");
                        
                        try {
                            // Query the database for the user with matching email (exact match)
                            // Use eq for exact email comparison (NO QUOTES - PostgREST doesn't need them)
                            String filter = "email_id=eq." + finalEmail;
                            System.out.println("=================================");
                            System.out.println("LOGIN ATTEMPT");
                            System.out.println("Role: " + finalSelectedRole);
                            System.out.println("Table: " + tableName);
                            System.out.println("Email entered: '" + finalEmail + "'");
                            System.out.println("Filter: " + filter);
                            System.out.println("=================================");
                            
                            org.json.JSONArray users = SupabaseClientInstance.getInstance()
                                .selectFromTable(tableName, filter, "");
                            
                            System.out.println("Login: Query returned " + users.length() + " user(s)");
                            if (users.length() > 0) {
                                System.out.println("Login: First user data: " + users.getJSONObject(0).toString());
                            }
                            
                            // Find the user with matching email (case-insensitive)
                            org.json.JSONObject matchedUser = null;
                            for (int i = 0; i < users.length(); i++) {
                                org.json.JSONObject user = users.getJSONObject(i);
                                String dbEmail = user.optString("email_id", "").toLowerCase().trim();
                                System.out.println("Login: Checking user " + i + " - email: '" + dbEmail + "'");
                                if (dbEmail.equals(finalEmail.toLowerCase().trim())) {
                                    matchedUser = user;
                                    System.out.println("Login: ✓ MATCHED!");
                                    break;
                                }
                            }
                            
                            if (matchedUser != null) {
                                // User exists in database, verify password
                                String storedPassword = matchedUser.optString("password", "").trim();
                                String storedEmail = matchedUser.optString("email_id", "");
                                
                                System.out.println("=================================");
                                System.out.println("PASSWORD VERIFICATION");
                                System.out.println("Email: " + storedEmail);
                                System.out.println("Stored password: '" + storedPassword + "'");
                                System.out.println("Entered password: '" + finalPassword + "'");
                                System.out.println("Match: " + storedPassword.equals(finalPassword.trim()));
                                System.out.println("=================================");
                                
                                // Compare passwords (plain text comparison - trim whitespace)
                                if (storedPassword.equals(finalPassword.trim())) {
                                    // Password matches - login successful
                                    System.out.println("Login: ✓✓✓ LOGIN SUCCESSFUL ✓✓✓");
                                    runOnUiThread(() -> {
                                        btnSignIn.setEnabled(true);
                                        btnSignIn.setText("Sign In");
                                        Toast.makeText(LoginActivity.this, "Login successful as " + finalSelectedRole + "!", Toast.LENGTH_SHORT).show();
                                        
                                        // Navigate to appropriate activity
                                        Intent intent;
                                        if (finalSelectedRole.equals("Admin")) {
                                            intent = new Intent(LoginActivity.this, AdminActivity.class);
                                            intent.putExtra("admin_email", finalEmail);
                                            intent.putExtra("access_token", ""); // No token needed for DB auth
                                        } else if (finalSelectedRole.equals("Staff")) {
                                            intent = new Intent(LoginActivity.this, StaffActivity.class);
                                            intent.putExtra("staff_email", finalEmail);
                                            intent.putExtra("access_token", ""); // No token needed for DB auth
                                        } else {
                                            // Customer
                                            intent = new Intent(LoginActivity.this, CustomerActivity.class);
                                            intent.putExtra("customer_email", finalEmail);
                                            intent.putExtra("access_token", ""); // No token needed for DB auth
                                        }
                                        startActivity(intent);
                                        finish();
                                    });
                                    return; // Exit thread
                                } else {
                                    // Password doesn't match
                                    System.out.println("Login: ✗✗✗ PASSWORD MISMATCH ✗✗✗");
                                    // For Customer, try Supabase Auth as fallback (for signups)
                                    if (finalSelectedRole.equals("Customer")) {
                                        System.out.println("Login: Database password mismatch for customer, trying Supabase Auth...");
                                        // Continue to Supabase Auth below
                                    } else {
                                        // For Admin/Staff, just show error
                                        System.out.println("Login: Password mismatch!");
                                        System.out.println("Login: Stored: '" + storedPassword + "'");
                                        System.out.println("Login: Entered: '" + finalPassword + "'");
                                        runOnUiThread(() -> {
                                            btnSignIn.setEnabled(true);
                                            btnSignIn.setText("Sign In");
                                            Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
                                        });
                                        return; // Exit thread
                                    }
                                }
                            } else if (!finalSelectedRole.equals("Customer")) {
                                // User doesn't exist in database
                                System.out.println("Login: ✗✗✗ USER NOT FOUND ✗✗✗");
                                System.out.println("Login: No user found with email: " + finalEmail);
                                runOnUiThread(() -> {
                                    btnSignIn.setEnabled(true);
                                    btnSignIn.setText("Sign In");
                                    Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
                                });
                                return; // Exit thread
                            }
                        } catch (Exception e) {
                            System.err.println("=================================");
                            System.err.println("LOGIN ERROR for " + finalSelectedRole);
                            System.err.println("Error: " + e.getMessage());
                            e.printStackTrace();
                            System.err.println("=================================");
                            runOnUiThread(() -> {
                                btnSignIn.setEnabled(true);
                                btnSignIn.setText("Sign In");
                                Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                            return; // Exit thread
                        }
                    }
                    
                    // Customer login fallback - use Supabase Auth (for users created via SignUp)
                    if (finalSelectedRole.equals("Customer")) {
                        System.out.println("Login: Trying Supabase Auth for customer...");
                        JSONObject response = SupabaseClientInstance.getInstance().signIn(finalEmail, finalPassword);

                        runOnUiThread(() -> {
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Sign In");

                            if (response.has("access_token")) {
                                final String accessToken = response.optString("access_token", "");
                                // Extract user email from response, fallback to login email
                                String emailValue = finalEmail;
                                try {
                                    if (response.has("user")) {
                                        JSONObject user = response.getJSONObject("user");
                                        emailValue = user.optString("email", finalEmail);
                                    }
                                } catch (Exception e) {
                                    // Use default email
                                }
                                final String userEmail = emailValue;
                                
                                Toast.makeText(LoginActivity.this, "Login successful as " + finalSelectedRole + "!", Toast.LENGTH_SHORT).show();
                                
                                // Verify customer in customer table, auto-create if missing
                                new Thread(() -> {
                                        try {
                                        org.json.JSONArray customerCheck = SupabaseClientInstance.getInstance()
                                            .selectFromTable("customer", "email_id=eq." + userEmail, "");                                        if (customerCheck.length() > 0) {
                                            // Customer record exists, proceed to login
                                            runOnUiThread(() -> {
                                                Intent customerIntent = new Intent(LoginActivity.this, CustomerActivity.class);
                                                customerIntent.putExtra("customer_email", userEmail);
                                                customerIntent.putExtra("access_token", accessToken);
                                                startActivity(customerIntent);
                                                finish();
                                            });
                                        } else {
                                            // Customer record missing - auto-create it
                                            System.out.println("Login: Customer record missing for " + userEmail + ", creating...");
                                            try {
                                                JSONObject customerData = new JSONObject();
                                                customerData.put("email_id", userEmail);
                                                customerData.put("password", finalPassword);
                                                customerData.put("first_name", userEmail.split("@")[0]); // Use email prefix as temp first name
                                                customerData.put("last_name", ""); // Empty last name
                                                customerData.put("contact_number", "+910000000000"); // Valid placeholder matching +91[10 digits] format
                                                
                                                System.out.println("Login: Attempting to insert customer data: " + customerData.toString());
                                                
                                                JSONObject insertResponse = SupabaseClientInstance.getInstance()
                                                    .insertIntoTable("customer", customerData, accessToken);
                                                
                                                System.out.println("Login: Insert response: " + insertResponse.toString());
                                                System.out.println("Login: Auto-created customer record for " + userEmail);
                                                
                                                runOnUiThread(() -> {
                                                    Toast.makeText(LoginActivity.this, "Welcome! Please update your profile.", Toast.LENGTH_SHORT).show();
                                                    Intent customerIntent = new Intent(LoginActivity.this, CustomerActivity.class);
                                                    customerIntent.putExtra("customer_email", userEmail);
                                                    customerIntent.putExtra("access_token", accessToken);
                                                    startActivity(customerIntent);
                                                    finish();
                                                });
                                            } catch (Exception createError) {
                                                createError.printStackTrace();
                                                System.err.println("Login: Failed to auto-create customer: " + createError.getMessage());
                                                final String errorDetails = createError.getMessage();
                                                runOnUiThread(() -> {
                                                    Toast.makeText(LoginActivity.this, "Error creating profile: " + errorDetails + ". Please contact support.", Toast.LENGTH_LONG).show();
                                                });
                                            }
                                        }
                                    } catch (Exception e) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(LoginActivity.this, "Error verifying customer: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                                    }
                                }).start();
                            } else {
                                String errorMsg = response.optString("msg",
                                        response.optString("error_description",
                                        response.optString("error", "")));
                                
                                if (errorMsg.toLowerCase().contains("invalid") || errorMsg.toLowerCase().contains("credentials")) {
                                    Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
                                } else if (!errorMsg.isEmpty()) {
                                    Toast.makeText(LoginActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Login failed. Please try again.", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");
                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });

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

        textSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void resetRoles() {
        roleCustomer.setBackgroundResource(R.drawable.role_box);
        roleStaff.setBackgroundResource(R.drawable.role_box);
        roleAdmin.setBackgroundResource(R.drawable.role_box);
    }
}
