package com.roadping.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public class SessionManager {
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences("roadping_session", Context.MODE_PRIVATE);
    }

    public void save(String accessToken, String refreshToken, String userId, String email) {
        prefs.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putString("user_id", userId)
                .putString("email", email)
                .apply();
    }

    public String accessToken() { return prefs.getString("access_token", ""); }
    public String refreshToken() { return prefs.getString("refresh_token", ""); }
    public String userId() { return prefs.getString("user_id", ""); }
    public String email() { return prefs.getString("email", ""); }
    public String installationId() {
        String value = prefs.getString("installation_id", "");
        if (value.isEmpty()) { value = UUID.randomUUID().toString(); prefs.edit().putString("installation_id", value).apply(); }
        return value;
    }
    public String trackerDeviceId() { return prefs.getString("tracker_device_id", ""); }
    public String trackingSessionId() { return prefs.getString("tracking_session_id", ""); }
    public String trackingVehicleId() { return prefs.getString("tracking_vehicle_id", ""); }
    public String trackingVehicleName() { return prefs.getString("tracking_vehicle_name", "Vehicle"); }
    public boolean trackerConfigured() { return !trackerDeviceId().isEmpty() && !trackingVehicleId().isEmpty(); }
    public boolean trackingEnabled() { return prefs.getBoolean("tracking_enabled", false); }
    public String appRole() { return prefs.getString("app_role", ""); }
    public void setAppRole(String role) { prefs.edit().putString("app_role", role).apply(); }
    public String displayMode() { return prefs.getString("display_mode", "journey"); }
    public void setDisplayMode(String mode) { prefs.edit().putString("display_mode", mode).apply(); }
    public double lastLatitude() { return Double.longBitsToDouble(prefs.getLong("last_latitude", Double.doubleToRawLongBits(Double.NaN))); }
    public double lastLongitude() { return Double.longBitsToDouble(prefs.getLong("last_longitude", Double.doubleToRawLongBits(Double.NaN))); }
    public float lastSpeedMps() { return prefs.getFloat("last_speed_mps", 0f); }
    public float lastBearing() { return prefs.getFloat("last_bearing", 0f); }
    public float lastAccuracyM() { return prefs.getFloat("last_accuracy_m", 0f); }
    public long lastLocationAt() { return prefs.getLong("last_location_at", 0L); }
    public void setLastLocation(double latitude, double longitude, float speedMps, float bearing, float accuracyM, long recordedAt) {
        prefs.edit()
                .putLong("last_latitude", Double.doubleToRawLongBits(latitude))
                .putLong("last_longitude", Double.doubleToRawLongBits(longitude))
                .putFloat("last_speed_mps", speedMps)
                .putFloat("last_bearing", bearing)
                .putFloat("last_accuracy_m", accuracyM)
                .putLong("last_location_at", recordedAt)
                .apply();
    }
    public void configureTracker(String deviceId, String vehicleId, String vehicleName) { prefs.edit().putString("tracker_device_id", deviceId).putString("tracking_vehicle_id", vehicleId).putString("tracking_vehicle_name", vehicleName).apply(); }
    public void setTrackingSession(String id) { prefs.edit().putString("tracking_session_id", id == null ? "" : id).apply(); }
    public void setTrackingEnabled(boolean enabled) { prefs.edit().putBoolean("tracking_enabled", enabled).apply(); }
    public boolean isSignedIn() { return !accessToken().isEmpty() && !userId().isEmpty(); }
    public long lastAlertCheck() { return prefs.getLong("last_alert_check", 0L); }
    public void setLastAlertCheck(long value) { prefs.edit().putLong("last_alert_check", value).apply(); }
    public void clear() {
        String installation = installationId();
        prefs.edit().clear().putString("installation_id", installation).apply();
    }
}
