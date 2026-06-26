package com.example.smartvalet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.smartvalet.utils.SupabaseClientInstance;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyValetRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ValetRequestAdapter adapter;
    private String customerEmail, accessToken;
    private List<JSONObject> valetRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_valet_requests);

        customerEmail = getIntent().getStringExtra("customer_email");
        accessToken = getIntent().getStringExtra("access_token");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerValetRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ValetRequestAdapter();
        recyclerView.setAdapter(adapter);

        // Setup pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> loadValetRequests());

        loadValetRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadValetRequests();
    }

    private void loadValetRequests() {
        new Thread(() -> {
            try {
                System.out.println("=================================");
                System.out.println("MyValetRequests: Loading all requests for: " + customerEmail);

                // Load ALL request types (valet_service AND car_fetch)
                JSONArray requests = SupabaseClientInstance.getInstance()
                    .selectFromTable("parking_requests",
                        "customer_email=eq." + customerEmail + "&order=created_at.desc",
                        accessToken != null ? accessToken : "");

                System.out.println("MyValetRequests: Found " + requests.length() + " total requests");

                valetRequests.clear();
                for (int i = 0; i < requests.length(); i++) {
                    JSONObject request = requests.getJSONObject(i);
                    String reqType = request.optString("request_type", "");
                    String reqStatus = request.optString("request_status", "");
                    String bookingId = request.optString("booking_id", "");
                    
                    // For car_fetch requests, check if booking is still active
                    if (reqType.equals("car_fetch") && !bookingId.isEmpty()) {
                        try {
                            JSONArray bookings = SupabaseClientInstance.getInstance()
                                .selectFromTable("booked_parking",
                                    "booking_id=eq." + bookingId,
                                    accessToken != null ? accessToken : "");
                            
                            if (bookings.length() > 0) {
                                String bookingStatus = bookings.getJSONObject(0).optString("status", "");
                                // Skip if booking is inactive (car has exited)
                                if (bookingStatus.equals("inactive")) {
                                    System.out.println("MyValetRequests: Skipping car_fetch request - booking is inactive");
                                    continue;
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("MyValetRequests: Error checking booking status: " + e.getMessage());
                        }
                    }
                    
                    valetRequests.add(request);
                    // Debug: Print full details
                    String reqId = request.optString("request_id", "").substring(0, Math.min(8, request.optString("request_id", "").length()));
                    System.out.println("MyValetRequests: Request #" + reqId + " -> Type: " + reqType + ", Status: " + reqStatus);
                }

                System.out.println("=================================");
                
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    adapter.notifyDataSetChanged();
                    if (valetRequests.isEmpty()) {
                        Toast.makeText(this, "No valet parking requests found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Loaded " + valetRequests.size() + " request(s)", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("MyValetRequests: Error loading requests: " + e.getMessage());
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error loading requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private class ValetRequestAdapter extends RecyclerView.Adapter<ValetRequestAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_valet_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            try {
                JSONObject request = valetRequests.get(position);
                
                String requestId = request.optString("request_id", "");
                String status = request.optString("request_status", "pending");
                String requestType = request.optString("request_type", "");
                String createdAt = request.optString("created_at", "");
                String details = request.optString("request_details", "");

                System.out.println("MyValetRequests: Displaying request " + requestId.substring(0, 8) + " - Type: " + requestType + ", Status: " + status);

                holder.tvRequestId.setText("Request #" + requestId.substring(0, 8));
                holder.tvStatus.setText(status.toUpperCase());
                holder.tvDetails.setText(details);

                // Format date
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                    Date date = inputFormat.parse(createdAt);
                    holder.tvDate.setText(outputFormat.format(date));
                } catch (Exception e) {
                    holder.tvDate.setText(createdAt);
                }

                // Status color and actions
                if (status.equalsIgnoreCase("approved") || status.equalsIgnoreCase("assigned")) {
                    // Show APPROVED for both "approved" and "assigned" statuses
                    holder.tvStatus.setText("APPROVED");
                    holder.tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    
                    // Only show "Proceed" button for valet_service (park car) - NOT for car_fetch
                    if (requestType.equals("valet_service")) {
                        holder.btnProceed.setVisibility(View.VISIBLE);
                        holder.btnProceed.setText("Enter Vehicle Details");
                        holder.btnProceed.setOnClickListener(v -> proceedToVehicleEntry(requestId));
                    } else {
                        // For car_fetch, just show a message - no action needed
                        holder.btnProceed.setVisibility(View.GONE);
                    }
                } else if (status.equalsIgnoreCase("pending")) {
                    holder.tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    holder.btnProceed.setVisibility(View.GONE);
                } else if (status.equalsIgnoreCase("rejected")) {
                    holder.tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    holder.btnProceed.setVisibility(View.GONE);
                } else if (status.equalsIgnoreCase("completed")) {
                    holder.tvStatus.setText("COMPLETED");
                    holder.tvStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    holder.btnProceed.setVisibility(View.GONE);
                } else {
                    holder.tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    holder.btnProceed.setVisibility(View.GONE);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return valetRequests.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRequestId, tvStatus, tvDetails, tvDate;
            Button btnProceed;

            ViewHolder(View itemView) {
                super(itemView);
                tvRequestId = itemView.findViewById(R.id.tvRequestId);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvDetails = itemView.findViewById(R.id.tvDetails);
                tvDate = itemView.findViewById(R.id.tvDate);
                btnProceed = itemView.findViewById(R.id.btnProceed);
            }
        }
    }

    private void proceedToVehicleEntry(String requestId) {
        // Navigate to vehicle details entry and payment
        Intent intent = new Intent(this, ValetVehicleEntryActivity.class);
        intent.putExtra("customer_email", customerEmail);
        intent.putExtra("access_token", accessToken);
        intent.putExtra("request_id", requestId);
        startActivity(intent);
    }
}
