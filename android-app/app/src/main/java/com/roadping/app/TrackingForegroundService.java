package com.roadping.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import org.json.JSONObject;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrackingForegroundService extends Service implements LocationListener {
    public static final String ACTION_START = "com.roadping.app.START_TRACKING";
    public static final String ACTION_STOP = "com.roadping.app.STOP_TRACKING";
    public static final String CHANNEL_ID = "roadping_live_tracking";
    private static final int NOTIFICATION_ID = 2048;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    private SupabaseApi api;
    private SessionManager session;
    private TrackingDatabase database;
    private LocationManager locations;
    private long lastSequence;

    @Override public void onCreate() {
        super.onCreate();
        api = new SupabaseApi(); session = new SessionManager(this); database = new TrackingDatabase(this);
        locations = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { stopTrip("stopped_from_notification"); return START_NOT_STICKY; }
        if (!session.isSignedIn() || !session.trackerConfigured()) { stopSelf(); return START_NOT_STICKY; }
        Notification notification = notification(0, "Starting GPS and secure trip sync", database.count());
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        else startForeground(NOTIFICATION_ID, notification);
        session.setTrackingEnabled(true);
        worker.execute(() -> {
            try {
                if (session.trackingSessionId().isEmpty()) {
                    JSONObject trip = callWithRefresh(() -> api.startTrackingSession(session.accessToken(), session.trackerDeviceId(), "vehicle_player_start"));
                    session.setTrackingSession(trip.optString("id"));
                }
                startLocationUpdates();
                flushQueue();
            } catch (Exception error) { updateNotification(0, "Waiting for network: " + safe(error.getMessage()), database.count()); startLocationUpdates(); }
        });
        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { stopSelf(); return; }
        try { locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this); } catch (Exception ignored) {}
        try { locations.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 15f, this); } catch (Exception ignored) {}
    }

    @Override public void onLocationChanged(Location location) {
        if (!session.trackingEnabled()) return;
        long now = System.currentTimeMillis();
        lastSequence = Math.max(lastSequence + 1, now);
        try {
            JSONObject point = new JSONObject()
                    .put("sequence_number", lastSequence).put("recorded_at", Instant.ofEpochMilli(location.getTime()).toString())
                    .put("latitude", location.getLatitude()).put("longitude", location.getLongitude())
                    .put("speed_mps", location.hasSpeed() ? Math.max(0, location.getSpeed()) : 0)
                    .put("bearing", location.hasBearing() ? location.getBearing() : JSONObject.NULL)
                    .put("altitude_m", location.hasAltitude() ? location.getAltitude() : JSONObject.NULL)
                    .put("accuracy_m", location.hasAccuracy() ? location.getAccuracy() : JSONObject.NULL)
                    .put("battery_percent", batteryPercent()).put("network_type", networkType())
                    .put("is_mock", Build.VERSION.SDK_INT >= 31 ? location.isMock() : location.isFromMockProvider());
            database.enqueue(point);
            updateNotification(location.hasSpeed() ? Math.round(location.getSpeed() * 3.6f) : 0, isOnline() ? "Live location connected" : "Offline - route saved on this player", database.count());
            flushQueue();
        } catch (Exception ignored) {}
    }

    private void flushQueue() {
        if (!isOnline() || session.trackingSessionId().isEmpty() || !flushing.compareAndSet(false, true)) return;
        worker.execute(() -> {
            try {
                while (true) {
                    TrackingDatabase.Batch batch = database.next(50);
                    if (batch.ids.isEmpty()) break;
                    try {
                        callWithRefresh(() -> api.ingestLocations(session.accessToken(), session.trackingSessionId(), batch.points));
                        database.delete(batch.ids);
                    } catch (SupabaseApi.ApiException error) {
                        if (error.statusCode >= 400 && error.statusCode < 500 && error.getMessage() != null && error.getMessage().contains("Active trip")) {
                            JSONObject trip = callWithRefresh(() -> api.startTrackingSession(session.accessToken(), session.trackerDeviceId(), "service_recovery"));
                            session.setTrackingSession(trip.optString("id"));
                            continue;
                        }
                        throw error;
                    }
                }
            } catch (Exception error) { updateNotification(0, "Route queued securely for sync", database.count()); }
            finally { flushing.set(false); }
        });
    }

    private void stopTrip(String reason) {
        session.setTrackingEnabled(false);
        try { locations.removeUpdates(this); } catch (Exception ignored) {}
        worker.execute(() -> {
            try {
                if (!session.trackingSessionId().isEmpty() && isOnline()) {
                    TrackingDatabase.Batch batch;
                    while (!(batch = database.next(50)).ids.isEmpty()) { api.ingestLocations(session.accessToken(), session.trackingSessionId(), batch.points); database.delete(batch.ids); }
                    api.finishTrackingSession(session.accessToken(), session.trackingSessionId(), reason);
                }
            } catch (Exception ignored) {}
            session.setTrackingSession("");
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
        });
    }

    private interface ApiCall<T> { T run() throws Exception; }
    private <T> T callWithRefresh(ApiCall<T> call) throws Exception {
        try { return call.run(); }
        catch (SupabaseApi.ApiException error) {
            if (error.statusCode != 401 || session.refreshToken().isEmpty()) throw error;
            JSONObject response = api.refresh(session.refreshToken()); JSONObject user = response.optJSONObject("user");
            session.save(response.optString("access_token"), response.optString("refresh_token"), user == null ? session.userId() : user.optString("id"), session.email());
            return call.run();
        }
    }

    private Notification notification(int speedKmh, String status, int queued) {
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, 80, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, TrackingForegroundService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 81, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = speedKmh + " km/h - " + status + (queued > 0 ? " - " + queued + " queued" : "");
        return new Notification.Builder(this, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("RoadPing tracking " + session.trackingVehicleName()).setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text + "\nTracking remains visible and can be stopped at any time."))
                .setContentIntent(openPending).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(null, "STOP TRACKING", stopPending).build()).build();
    }

    private void updateNotification(int speed, String status, int queued) { getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, notification(speed, status, queued)); }
    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Live vehicle tracking", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Persistent status while a vehicle player shares its location"); channel.setShowBadge(false);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    private boolean isOnline() {
        ConnectivityManager manager = getSystemService(ConnectivityManager.class); if (manager == null || manager.getActiveNetwork() == null) return false;
        NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork()); return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
    private String networkType() { return isOnline() ? "connected" : "offline"; }
    private int batteryPercent() { BatteryManager battery = getSystemService(BatteryManager.class); return battery == null ? -1 : battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY); }
    private String safe(String value) { return value == null ? "connection unavailable" : value.replace('\n', ' '); }

    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) { updateNotification(0, "Location services are off", database.count()); }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { try { locations.removeUpdates(this); } catch (Exception ignored) {} worker.shutdown(); database.close(); super.onDestroy(); }
}
