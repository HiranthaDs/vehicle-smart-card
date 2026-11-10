package com.roadping.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SessionManager session = new SessionManager(context);
        if (!session.isSignedIn() || !session.trackerConfigured() || !session.trackingEnabled()) return;
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        if (android.os.Build.VERSION.SDK_INT >= 29 && context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try { context.startForegroundService(new Intent(context, TrackingForegroundService.class).setAction(TrackingForegroundService.ACTION_START)); }
        catch (Exception ignored) {}
    }
}
