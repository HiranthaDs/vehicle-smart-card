package com.roadping.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TrackingDatabase extends SQLiteOpenHelper {
    public static class Batch {
        public final JSONArray points = new JSONArray();
        public final List<Long> ids = new ArrayList<>();
    }

    public TrackingDatabase(Context context) { super(context, "roadping_tracking.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE queued_points (id INTEGER PRIMARY KEY AUTOINCREMENT, payload TEXT NOT NULL, created_at INTEGER NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public synchronized void enqueue(JSONObject point) {
        ContentValues values = new ContentValues();
        values.put("payload", point.toString());
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("queued_points", null, values);
    }

    public synchronized Batch next(int limit) {
        Batch batch = new Batch();
        try (Cursor cursor = getReadableDatabase().query("queued_points", new String[]{"id", "payload"}, null, null, null, null, "id ASC", String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                try {
                    batch.ids.add(cursor.getLong(0));
                    batch.points.put(new JSONObject(cursor.getString(1)));
                } catch (Exception ignored) {}
            }
        }
        return batch;
    }

    public synchronized void delete(List<Long> ids) {
        if (ids.isEmpty()) return;
        StringBuilder marks = new StringBuilder();
        String[] args = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) { if (i > 0) marks.append(','); marks.append('?'); args[i] = String.valueOf(ids.get(i)); }
        getWritableDatabase().delete("queued_points", "id IN (" + marks + ")", args);
    }

    public synchronized int count() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM queued_points", null)) { return cursor.moveToFirst() ? cursor.getInt(0) : 0; }
    }
}
