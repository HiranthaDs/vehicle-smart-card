package com.roadping.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "roadping_vehicle_alerts";
    private NotificationHelper() {}

    public static void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Vehicle alerts", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("RoadPing incident and vehicle contact alerts");
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    public static void show(Context context, Models.Alert alert) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        createChannel(context);
        Intent intent = new Intent(context, MainActivity.class).putExtra("open_alerts", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, alert.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("RoadPing · " + alert.registration)
                .setContentText(alert.displayType() + (alert.message.isEmpty() ? "" : " — " + alert.message))
                .setStyle(new android.app.Notification.BigTextStyle().bigText(alert.message.isEmpty() ? alert.displayType() : alert.message))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setCategory(android.app.Notification.CATEGORY_ALARM)
                .build();
        context.getSystemService(NotificationManager.class).notify(alert.id.hashCode(), notification);
    }
}
