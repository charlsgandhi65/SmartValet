package com.example.smartvalet.utils;

import org.json.JSONObject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Dns;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseClientInstance {
    private static final String SUPABASE_URL = "https://smyeqsobctlhwoqcards.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNteWVxc29iY3RsaHdvcWNhcmRzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI0NTc2ODksImV4cCI6MjA5ODAzMzY4OX0.Dip74zt8DtNbaIuijQvnYsZRo6_LjmThA4F6sh_AyOQ";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    // Custom DNS that falls back to Google DNS if system DNS fails
    private static final Dns CUSTOM_DNS = hostname -> {
        try {
            // Try system DNS first
            return Dns.SYSTEM.lookup(hostname);
        } catch (UnknownHostException e) {
            System.out.println("System DNS failed for " + hostname + ", trying direct resolution");
            // If system DNS fails, try to resolve using all addresses
            return Arrays.asList(InetAddress.getAllByName(hostname));
        }
    };
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .dns(CUSTOM_DNS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    
    private static SupabaseClientInstance instance;

    private SupabaseClientInstance() {}

    public static SupabaseClientInstance getInstance() {
        if (instance == null) {
            instance = new SupabaseClientInstance();
        }
        return instance;
    }

    public JSONObject signUp(String email, String password) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
            // Add data object to request auto-confirmation (no email verification needed)
            JSONObject data = new JSONObject();
            data.put("auto_confirm", true);
            json.put("data", data);
        } catch (org.json.JSONException e) {
            System.err.println("Failed to create signup JSON: " + e.getMessage());
            throw new RuntimeException("Failed to create signup JSON", e);
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
            .url(SUPABASE_URL + "/auth/v1/signup")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            String responseData = response.body().string();
            
            // Log response for debugging (remove in production)
            System.out.println("Supabase Signup Response Code: " + response.code());
            System.out.println("Supabase Signup Response Body: " + responseData);
            
            try {
                JSONObject jsonResponse = new JSONObject(responseData);
                
                // If response code is not success, include it in response
                if (response.code() != 200 && response.code() != 201) {
                    jsonResponse.put("http_code", response.code());
                }
                
                return jsonResponse;
            } catch (org.json.JSONException e) {
                System.err.println("Failed to parse signup response JSON: " + e.getMessage());
                throw new RuntimeException("Failed to parse signup response JSON", e);
            }
        }
    }

    public JSONObject signIn(String email, String password) throws IOException {
        System.out.println("SupabaseClientInstance: Attempting login for: " + email);
        System.out.println("SupabaseClientInstance: URL: " + SUPABASE_URL);
        
        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
        } catch (org.json.JSONException e) {
            System.err.println("Failed to create login JSON: " + e.getMessage());
            throw new RuntimeException("Failed to create login JSON", e);
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
            .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("SupabaseClientInstance: Got response, code: " + response.code());
            okhttp3.ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException("Response body is null");
            }
            String responseData = responseBody.string();
            System.out.println("SupabaseClientInstance: Response data: " + responseData);
            try {
                return new JSONObject(responseData);
            } catch (org.json.JSONException e) {
                System.err.println("Failed to parse login response JSON: " + e.getMessage());
                throw new RuntimeException("Failed to parse login response JSON", e);
            }
        }
    }

    /**
     * Insert data into a Supabase table
     * @param tableName Name of the table (e.g., "customer")
     * @param data JSON object with the data to insert
     * @param accessToken Optional access token for authenticated requests
     * @return JSONObject with the inserted row(s)
     */
    public JSONObject insertIntoTable(String tableName, JSONObject data, String accessToken) throws IOException {
        RequestBody body = RequestBody.create(data.toString(), JSON);
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(SUPABASE_URL + "/rest/v1/" + tableName)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(body);
        
        // Add authorization header if access token is provided
        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        }
        
        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            okhttp3.ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new RuntimeException("Response body is null");
            }
            String responseData = responseBody.string();
            
            System.out.println("Supabase Insert Response Code: " + response.code());
            System.out.println("Supabase Insert Response Body: " + responseData);
            
            // Check if response is successful
            if (response.code() != 200 && response.code() != 201) {
                // Try to parse error message from response
                String errorMessage = responseData;
                try {
                    // Try to extract error message from JSON response
                    JSONObject errorJson = new JSONObject(responseData);
                    errorMessage = errorJson.optString("message", 
                              errorJson.optString("error", 
                              errorJson.optString("details", responseData)));
                } catch (org.json.JSONException e) {
                    // Response is not JSON, use raw response
                    errorMessage = responseData;
                }
                
                throw new RuntimeException("Insert failed (HTTP " + response.code() + "): " + errorMessage);
            }
            
            try {
                // Supabase REST API may return an array or object
                // Try to parse as array first (most common for inserts)
                if (responseData.trim().startsWith("[")) {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(responseData);
                    if (jsonArray.length() > 0) {
                        // Return first element as JSONObject
                        return jsonArray.getJSONObject(0);
                    } else {
                        // Empty array - create success response
                        JSONObject success = new JSONObject();
                        success.put("success", true);
                        return success;
                    }
                } else {
                    // Parse as object
                    return new JSONObject(responseData);
                }
            } catch (org.json.JSONException e) {
                System.err.println("Failed to parse insert response JSON: " + e.getMessage());
                throw new RuntimeException("Failed to parse insert response JSON: " + responseData, e);
            }
        }
    }

    /**
     * Select data from a Supabase table
     * @param tableName Name of the table
     * @param filters Optional filters (e.g., "email_id=eq.user@example.com")
     * @param accessToken Optional access token
     * @return JSONArray with the results
     */
    public org.json.JSONArray selectFromTable(String tableName, String filters, String accessToken) throws IOException {
        System.out.println("========================================");
        System.out.println("SELECT FROM TABLE");
        System.out.println("Table: " + tableName);
        System.out.println("Filters (input): " + filters);
        
        // Build URL - PostgREST filters format: "column=operator.value"
        // Example: "email_id=eq.value" where "eq" is the operator
        // NOTE: Do NOT use quotes around values - PostgREST doesn't need them!
        // CORRECT: email_id=eq.user@example.com
        // WRONG: email_id=eq."user@example.com"
        String url = SUPABASE_URL + "/rest/v1/" + tableName;
        if (filters != null && !filters.isEmpty()) {
            url += "?" + filters;
            System.out.println("URL (with filters): " + url);
        } else {
            System.out.println("selectFromTable: URL: " + url + " (no filters)");
        }
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .get();
        
        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        }
        
        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Response Code: " + response.code());
            System.out.println("Response Message: " + response.message());
            
            okhttp3.ResponseBody responseBody = response.body();
            if (responseBody == null) {
                System.err.println("ERROR: Response body is null!");
                throw new RuntimeException("Response body is null");
            }
            String responseData = responseBody.string();
            System.out.println("Response Data: " + responseData);
            System.out.println("Response Length: " + responseData.length());
            System.out.println("========================================");
            
            if (response.code() != 200) {
                System.err.println("ERROR: Non-200 response code!");
                throw new RuntimeException("Select failed (HTTP " + response.code() + "): " + responseData);
            }
            
            try {
                org.json.JSONArray result = new org.json.JSONArray(responseData);
                System.out.println("Parsed JSONArray with " + result.length() + " items");
                return result;
            } catch (org.json.JSONException e) {
                System.err.println("Failed to parse select response JSON: " + e.getMessage());
                throw new RuntimeException("Failed to parse select response JSON", e);
            }
        }
    }

    /**
     * Update data in a Supabase table
     * @param tableName Name of the table
     * @param filters Filter string (e.g., "event_id=eq.uuid-here")
     * @param data JSON object with fields to update
     * @param accessToken Optional access token
     * @return JSONArray with updated row(s)
     */
    public org.json.JSONArray updateTable(String tableName, String filters, JSONObject data, String accessToken) throws IOException {
        String url = SUPABASE_URL + "/rest/v1/" + tableName;
        if (filters != null && !filters.isEmpty()) {
            url += "?" + filters;
        }
        
        System.out.println("SupabaseClientInstance: UPDATE Request");
        System.out.println("  URL: " + url);
        System.out.println("  Data: " + data.toString());
        System.out.println("  Has Access Token: " + (accessToken != null && !accessToken.isEmpty()));
        
        RequestBody body = RequestBody.create(data.toString(), JSON);
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .patch(body);
        
        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        }
        
        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("SupabaseClientInstance: UPDATE Response Code: " + response.code());
            
            okhttp3.ResponseBody responseBody = response.body();
            if (responseBody == null) {
                // For successful updates, body might be null, return empty array
                if (response.code() == 200 || response.code() == 204) {
                    System.out.println("SupabaseClientInstance: Update successful (empty response)");
                    return new org.json.JSONArray();
                }
                System.out.println("SupabaseClientInstance: ERROR - Response body is null");
                throw new RuntimeException("Response body is null");
            }
            String responseData = responseBody.string();
            System.out.println("SupabaseClientInstance: Response Data: " + responseData);
            
            if (response.code() != 200 && response.code() != 204) {
                System.out.println("SupabaseClientInstance: ERROR - Update failed with code: " + response.code());
                throw new RuntimeException("Update failed (HTTP " + response.code() + "): " + responseData);
            }
            
            try {
                return responseData.trim().isEmpty() ? new org.json.JSONArray() : new org.json.JSONArray(responseData);
            } catch (org.json.JSONException e) {
                System.err.println("Failed to parse update response JSON: " + e.getMessage());
                throw new RuntimeException("Failed to parse update response JSON", e);
            }
        }
    }

    /**
     * Delete data from a Supabase table
     * @param tableName Name of the table
     * @param filters Filter string (e.g., "event_id=eq.uuid-here")
     * @param accessToken Optional access token
     * @return true if successful
     */
    public boolean deleteFromTable(String tableName, String filters, String accessToken) throws IOException {
        String url = SUPABASE_URL + "/rest/v1/" + tableName;
        if (filters != null && !filters.isEmpty()) {
            url += "?" + filters;
        }
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .delete();
        
        if (accessToken != null && !accessToken.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
        }
        
        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() != 200 && response.code() != 204) {
                okhttp3.ResponseBody responseBody = response.body();
                String errorBody = (responseBody != null) ? responseBody.string() : "No error body";
                throw new RuntimeException("Delete failed (HTTP " + response.code() + "): " + errorBody);
            }
            return true;
        }
    }
}