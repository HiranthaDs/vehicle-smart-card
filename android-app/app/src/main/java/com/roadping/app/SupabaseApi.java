package com.roadping.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SupabaseApi {
    public static class ApiException extends Exception {
        public final int statusCode;
        ApiException(int statusCode, String message) { super(message); this.statusCode = statusCode; }
    }

    public JSONObject signIn(String email, String password) throws Exception {
        JSONObject body = new JSONObject().put("email", email).put("password", password);
        return requestObject("POST", "/auth/v1/token?grant_type=password", null, body, null);
    }

    public JSONObject signUp(String name, String email, String phone, String emergency, String password) throws Exception {
        JSONObject metadata = new JSONObject().put("full_name", name).put("phone", phone).put("emergency_contact_phone", emergency);
        JSONObject body = new JSONObject().put("email", email).put("password", password).put("data", metadata);
        return requestObject("POST", "/auth/v1/signup", null, body, null);
    }

    public JSONObject refresh(String refreshToken) throws Exception {
        JSONObject body = new JSONObject().put("refresh_token", refreshToken);
        return requestObject("POST", "/auth/v1/token?grant_type=refresh_token", null, body, null);
    }

    public List<Models.Vehicle> vehicles(String token) throws Exception {
        JSONArray data = requestArray("GET", "/rest/v1/vehicles?select=*&order=created_at.desc", token, null, null);
        List<Models.Vehicle> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) result.add(Models.Vehicle.fromJson(data.getJSONObject(i)));
        return result;
    }

    public List<Models.Alert> alerts(String token) throws Exception {
        JSONArray data = requestArray("GET", "/rest/v1/incident_alerts?select=*,vehicles(registration_number,nickname)&order=created_at.desc&limit=100", token, null, null);
        List<Models.Alert> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) result.add(Models.Alert.fromJson(data.getJSONObject(i)));
        return result;
    }

    public List<Models.Trip> trips(String token) throws Exception {
        JSONArray data = requestArray("GET", "/rest/v1/tracking_sessions?select=*,vehicles(registration_number)&order=started_at.desc&limit=50", token, null, null);
        List<Models.Trip> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) result.add(Models.Trip.fromJson(data.getJSONObject(i)));
        return result;
    }

    public List<Models.TrackerDevice> trackerDevices(String token) throws Exception {
        JSONArray data = requestArray("GET", "/rest/v1/tracker_devices?select=*,vehicles(registration_number,nickname,make,model)&order=last_seen_at.desc", token, null, null);
        List<Models.TrackerDevice> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) result.add(Models.TrackerDevice.fromJson(data.getJSONObject(i)));
        return result;
    }

    public JSONObject registerTracker(String token, String vehicleId, String installationId, String deviceName,
                                      String locationPermission, boolean backgroundPermission, boolean notificationPermission,
                                      boolean batteryIgnored, boolean locationEnabled, boolean networkConnected) throws Exception {
        JSONObject body = new JSONObject()
                .put("p_vehicle_id", vehicleId).put("p_installation_id", installationId)
                .put("p_name", deviceName).put("p_mode", "vehicle_head_unit").put("p_app_version", BuildConfig.VERSION_NAME)
                .put("p_location_permission", locationPermission).put("p_background_permission", backgroundPermission)
                .put("p_notification_permission", notificationPermission).put("p_battery_optimization_ignored", batteryIgnored)
                .put("p_location_services_enabled", locationEnabled).put("p_network_connected", networkConnected);
        return requestObject("POST", "/rest/v1/rpc/register_tracker_device", token, body, null);
    }

    public JSONObject startTrackingSession(String token, String deviceId, String reason) throws Exception {
        return requestObject("POST", "/rest/v1/rpc/start_tracking_session", token,
                new JSONObject().put("p_tracker_device_id", deviceId).put("p_start_reason", reason), null);
    }

    public JSONObject trackerDevice(String token, String deviceId) throws Exception {
        JSONArray data = requestArray("GET", "/rest/v1/tracker_devices?select=*&id=eq." + deviceId + "&limit=1", token, null, null);
        return data.length() == 0 ? new JSONObject() : data.getJSONObject(0);
    }

    public void updateTrackerDisplayMode(String token, String deviceId, String mode) throws Exception {
        requestText("PATCH", "/rest/v1/tracker_devices?id=eq." + deviceId, token, new JSONObject().put("display_mode", mode), "return=minimal");
    }

    public JSONObject ingestLocations(String token, String sessionId, JSONArray points) throws Exception {
        return requestObject("POST", "/rest/v1/rpc/ingest_location_batch", token,
                new JSONObject().put("p_session_id", sessionId).put("p_points", points), null);
    }

    public JSONObject finishTrackingSession(String token, String sessionId, String reason) throws Exception {
        return requestObject("POST", "/rest/v1/rpc/finish_tracking_session", token,
                new JSONObject().put("p_session_id", sessionId).put("p_end_reason", reason), null);
    }

    public JSONObject profile(String token, String userId) throws Exception {
        JSONArray data = requestArray("GET", "/rest/v1/profiles?select=*&id=eq." + userId + "&limit=1", token, null, null);
        return data.length() == 0 ? new JSONObject() : data.getJSONObject(0);
    }

    public Models.Vehicle addVehicle(String token, String ownerId, JSONObject values) throws Exception {
        values.put("owner_id", ownerId);
        JSONArray data = requestArray("POST", "/rest/v1/vehicles", token, values, "return=representation");
        if (data.length() == 0) throw new ApiException(500, "Vehicle was not returned by the server.");
        return Models.Vehicle.fromJson(data.getJSONObject(0));
    }

    public void updateProfile(String token, String userId, JSONObject values) throws Exception {
        requestText("PATCH", "/rest/v1/profiles?id=eq." + userId, token, values, "return=minimal");
    }

    public void updateAlertStatus(String token, String alertId, String status) throws Exception {
        requestText("PATCH", "/rest/v1/incident_alerts?id=eq." + alertId, token, new JSONObject().put("status", status), "return=minimal");
    }

    private JSONObject requestObject(String method, String path, String token, JSONObject body, String prefer) throws Exception {
        String text = requestText(method, path, token, body, prefer);
        return text.isEmpty() ? new JSONObject() : new JSONObject(text);
    }

    private JSONArray requestArray(String method, String path, String token, JSONObject body, String prefer) throws Exception {
        String text = requestText(method, path, token, body, prefer);
        return text.isEmpty() ? new JSONArray() : new JSONArray(text);
    }

    private String requestText(String method, String path, String token, JSONObject body, String prefer) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BuildConfig.SUPABASE_URL + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY);
        connection.setRequestProperty("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        if (prefer != null) connection.setRequestProperty("Prefer", prefer);

        if (body != null) {
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = read(stream);
        connection.disconnect();
        if (status < 200 || status >= 300) throw new ApiException(status, extractMessage(response));
        return response;
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }

    private static String extractMessage(String response) {
        try {
            JSONObject json = new JSONObject(response);
            String message = json.optString("msg");
            if (message.isEmpty()) message = json.optString("message");
            if (message.isEmpty()) message = json.optString("error_description");
            if (message.isEmpty()) message = json.optString("hint");
            return message.isEmpty() ? "Request failed. Please try again." : message;
        } catch (JSONException ignored) {
            return response.isEmpty() ? "Network request failed." : response;
        }
    }
}
