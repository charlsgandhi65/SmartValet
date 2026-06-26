package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QRScannerActivity extends AppCompatActivity {

    private EditText editQRCode;
    private RadioGroup radioScanType;
    private Button btnScan, btnManualEntry;
    private TextView tvScanResult;
    private String staffEmail, accessToken, staffId;
    
    // QR Code Scanner launcher
    private final ActivityResultLauncher<ScanOptions> qrScanLauncher = registerForActivityResult(
        new ScanContract(),
        result -> {
            if (result.getContents() != null) {
                String qrCode = result.getContents();
                editQRCode.setText(qrCode);
                processQRCode(qrCode);
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        staffEmail = getIntent().getStringExtra("staff_email");
        accessToken = getIntent().getStringExtra("access_token");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        editQRCode = findViewById(R.id.editQRCode);
        radioScanType = findViewById(R.id.radioScanType);
        btnScan = findViewById(R.id.btnScan);
        btnManualEntry = findViewById(R.id.btnManualEntry);
        tvScanResult = findViewById(R.id.tvScanResult);

        // Get staff ID
        loadStaffId();

        // Camera-based QR scanning
        btnScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan QR Code");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(true);
            options.setOrientationLocked(false);
            qrScanLauncher.launch(options);
        });

        // Manual entry fallback
        btnManualEntry.setOnClickListener(v -> {
            String qrCode = editQRCode.getText().toString().trim();
            if (qrCode.isEmpty()) {
                Toast.makeText(this, "Please enter QR code", Toast.LENGTH_SHORT).show();
                return;
            }
            processQRCode(qrCode);
        });
    }

    private void loadStaffId() {
        new Thread(() -> {
            try {
                JSONArray staffInfo = SupabaseClientInstance.getInstance()
                    .selectFromTable("staff", "email_id=eq." + staffEmail, 
                        accessToken != null ? accessToken : "");

                if (staffInfo.length() > 0) {
                    JSONObject staff = staffInfo.getJSONObject(0);
                    staffId = staff.optString("staff_id", "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processQRCode(String qrCode) {
        btnScan.setEnabled(false);
        btnManualEntry.setEnabled(false);

        new Thread(() -> {
            try {
                System.out.println("QRScanner: ========================================");
                System.out.println("QRScanner: Processing QR code: '" + qrCode + "'");
                System.out.println("QRScanner: Length: " + qrCode.length());
                System.out.println("QRScanner: ========================================");
                
                JSONArray bookings = new JSONArray();
                
                // STRATEGY 1: Try exact match on qr_code column
                try {
                    System.out.println("QRScanner: Strategy 1 - Exact match on qr_code column");
                    bookings = SupabaseClientInstance.getInstance()
                        .selectFromTable("booked_parking", "qr_code=eq." + qrCode, 
                            accessToken != null ? accessToken : "");
                    System.out.println("QRScanner: Strategy 1 result: " + bookings.length() + " bookings");
                } catch (Exception e) {
                    System.err.println("QRScanner: Strategy 1 error: " + e.getMessage());
                }

                // STRATEGY 2: Try exact match on booking_id
                if (bookings.length() == 0) {
                    try {
                        System.out.println("QRScanner: Strategy 2 - Exact match on booking_id");
                        bookings = SupabaseClientInstance.getInstance()
                            .selectFromTable("booked_parking", "booking_id=eq." + qrCode, 
                                accessToken != null ? accessToken : "");
                        System.out.println("QRScanner: Strategy 2 result: " + bookings.length() + " bookings");
                    } catch (Exception e) {
                        System.err.println("QRScanner: Strategy 2 error: " + e.getMessage());
                    }
                }

                // STRATEGY 3: Load ALL recent bookings and search in memory
                if (bookings.length() == 0) {
                    System.out.println("QRScanner: Strategy 3 - Loading all bookings for manual search");
                    try {
                        // Get all active bookings
                        JSONArray allBookings = SupabaseClientInstance.getInstance()
                            .selectFromTable("booked_parking", "order=created_at.desc&limit=500", 
                                accessToken != null ? accessToken : "");
                        
                        System.out.println("QRScanner: Loaded " + allBookings.length() + " bookings");
                        System.out.println("QRScanner: Searching for matches...");
                        
                        // Search through all bookings
                        for (int i = 0; i < allBookings.length(); i++) {
                            JSONObject booking = allBookings.getJSONObject(i);
                            String bookingId = booking.optString("booking_id", "");
                            String storedQrCode = booking.optString("qr_code", "");
                            String customerEmail = booking.optString("customer_email", "");
                            String vehicleNumber = booking.optString("vehicle_number", "");
                            
                            if (i < 5) { // Show first 5 for debugging
                                System.out.println("  [" + i + "] BookingID: " + bookingId);
                                System.out.println("      QR Code: " + storedQrCode);
                                System.out.println("      Vehicle: " + vehicleNumber);
                            }
                            
                            // Try multiple matching strategies
                            boolean matched = false;
                            String matchType = "";
                            
                            // 1. Exact match on QR code
                            if (!storedQrCode.isEmpty() && storedQrCode.equalsIgnoreCase(qrCode)) {
                                matched = true;
                                matchType = "Exact QR match";
                            }
                            // 2. QR code starts with input (for partial QR codes)
                            else if (!storedQrCode.isEmpty() && storedQrCode.toLowerCase().startsWith(qrCode.toLowerCase())) {
                                matched = true;
                                matchType = "QR starts with input";
                            }
                            // 3. Input starts with QR code (for when full UUID entered but stored is short)
                            else if (!storedQrCode.isEmpty() && qrCode.toLowerCase().startsWith(storedQrCode.toLowerCase())) {
                                matched = true;
                                matchType = "Input starts with QR";
                            }
                            // 4. Exact match on booking ID
                            else if (!bookingId.isEmpty() && bookingId.equalsIgnoreCase(qrCode)) {
                                matched = true;
                                matchType = "Exact BookingID match";
                            }
                            // 5. Booking ID starts with input
                            else if (!bookingId.isEmpty() && bookingId.toLowerCase().startsWith(qrCode.toLowerCase())) {
                                matched = true;
                                matchType = "BookingID starts with input";
                            }
                            // 6. Input starts with booking ID
                            else if (!bookingId.isEmpty() && qrCode.toLowerCase().startsWith(bookingId.toLowerCase())) {
                                matched = true;
                                matchType = "Input starts with BookingID";
                            }
                            
                            if (matched) {
                                bookings = new JSONArray();
                                bookings.put(booking);
                                System.out.println("QRScanner: ✅✅✅ MATCH FOUND! ✅✅✅");
                                System.out.println("QRScanner: Match Type: " + matchType);
                                System.out.println("QRScanner: Booking ID: " + bookingId);
                                System.out.println("QRScanner: Stored QR: " + storedQrCode);
                                System.out.println("QRScanner: Customer: " + customerEmail);
                                System.out.println("QRScanner: Vehicle: " + vehicleNumber);
                                break;
                            }
                        }
                        
                        if (bookings.length() == 0) {
                            System.out.println("QRScanner: ❌ No match found in any booking");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("QRScanner: Strategy 3 error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                final JSONArray finalBookings = bookings;
                runOnUiThread(() -> {
                    btnScan.setEnabled(true);
                    btnManualEntry.setEnabled(true);

                    if (finalBookings.length() == 0) {
                        System.out.println("QRScanner: ========================================");
                        System.out.println("QRScanner: ❌❌❌ FINAL RESULT: NOT FOUND ❌❌❌");
                        System.out.println("QRScanner: ========================================");
                        tvScanResult.setText("❌ QR code not found\n\nSearched for: " + qrCode + "\n\nPlease ensure:\n• The booking exists\n• Payment is completed\n• QR code is correct");
                        tvScanResult.setTextColor(0xFFF44336);
                        return;
                    }

                    try {
                        JSONObject booking = finalBookings.getJSONObject(0);
                        String bookingId = booking.optString("booking_id", "");
                        String scanType = getSelectedScanType();

                        System.out.println("QRScanner: Booking found! ID: " + bookingId);

                        // Check if entry scan is required before exit or car_fetch
                        if (scanType.equals("exit") || scanType.equals("car_fetch")) {
                            checkEntryAndLogScan(bookingId, qrCode, scanType, booking);
                        } else {
                            // For entry and valet_request, log directly
                            logQRScan(bookingId, qrCode, scanType);
                            showSuccessResult(booking);
                        }

                    } catch (Exception e) {
                        System.err.println("QRScanner: Error processing booking: " + e.getMessage());
                        tvScanResult.setText("Error processing booking: " + e.getMessage());
                        tvScanResult.setTextColor(0xFFF44336);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnScan.setEnabled(true);
                    btnManualEntry.setEnabled(true);
                    tvScanResult.setText("Error: " + e.getMessage());
                    tvScanResult.setTextColor(0xFFF44336);
                });
            }
        }).start();
    }

    private String getSelectedScanType() {
        int selectedId = radioScanType.getCheckedRadioButtonId();
        if (selectedId == R.id.radioEntry) {
            return "entry";
        } else if (selectedId == R.id.radioExit) {
            return "exit";
        } else if (selectedId == R.id.radioValetRequest) {
            return "valet_request";
        } else if (selectedId == R.id.radioCarFetch) {
            return "car_fetch";
        }
        return "entry"; // Default
    }

    private void checkEntryAndLogScan(String bookingId, String qrCode, String scanType, JSONObject booking) {
        new Thread(() -> {
            try {
                // Check if entry scan exists
                JSONArray entryScans = SupabaseClientInstance.getInstance()
                    .selectFromTable("qr_logs",
                        "booking_id=eq." + bookingId + "&scan_type=eq.entry",
                        accessToken != null ? accessToken : "");
                
                if (entryScans.length() == 0) {
                    // No entry scan found
                    runOnUiThread(() -> {
                        tvScanResult.setText("⚠️ Cannot scan for " + scanType + "!\n\nThis vehicle has not been scanned for entry yet.\nPlease scan for entry first.");
                        tvScanResult.setTextColor(0xFFF44336);
                    });
                    return;
                }
                
                // Entry scan exists, proceed with logging
                logQRScan(bookingId, qrCode, scanType);
                runOnUiThread(() -> showSuccessResult(booking));
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvScanResult.setText("Error checking entry status: " + e.getMessage());
                    tvScanResult.setTextColor(0xFFF44336);
                });
            }
        }).start();
    }

    private void showSuccessResult(JSONObject booking) {
        try {
            String vehicleInfo = String.format(
                "Vehicle: %s (%s)\nSpot: %d\nStatus: %s",
                booking.optString("vehicle_number", ""),
                booking.optString("vehicle_model", ""),
                booking.optInt("parking_spot_number", 0),
                booking.optString("status", "")
            );
            tvScanResult.setText("✅ QR Code Scanned Successfully!\n\n" + vehicleInfo);
            tvScanResult.setTextColor(0xFF4CAF50);
            
            // Clear QR code field
            editQRCode.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logQRScan(String bookingId, String qrCode, String scanType) {
        new Thread(() -> {
            try {
                // Check for duplicate exit scan
                if (scanType.equals("exit")) {
                    JSONArray existingExits = SupabaseClientInstance.getInstance()
                        .selectFromTable("qr_logs",
                            "booking_id=eq." + bookingId + "&scan_type=eq.exit",
                            accessToken != null ? accessToken : "");
                    
                    if (existingExits.length() > 0) {
                        runOnUiThread(() -> {
                            Toast.makeText(QRScannerActivity.this,
                                "⚠️ This QR code has already been scanned for exit!",
                                Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                }
                
                JSONObject logData = new JSONObject();
                logData.put("booking_id", bookingId);
                logData.put("qr_code", qrCode);
                logData.put("scanned_by_staff_id", staffId);
                logData.put("scan_type", scanType);
                logData.put("scan_timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date()));
                logData.put("vehicle_status", scanType.equals("entry") ? "parked" : "retrieved");

                SupabaseClientInstance.getInstance()
                    .insertIntoTable("qr_logs", logData, accessToken != null ? accessToken : "");

                // If exit scan, update booking status to inactive
                if (scanType.equals("exit")) {
                    JSONObject updateData = new JSONObject();
                    updateData.put("status", "inactive");
                    
                    SupabaseClientInstance.getInstance()
                        .updateTable("booked_parking",
                            "booking_id=eq." + bookingId,
                            updateData,
                            accessToken != null ? accessToken : "");
                    
                    System.out.println("QRScanner: Updated booking " + bookingId + " status to inactive");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

