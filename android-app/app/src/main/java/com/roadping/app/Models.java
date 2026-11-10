package com.roadping.app;

import org.json.JSONObject;

public final class Models {
    private Models() {}

    public static class Vehicle {
        public String id, publicToken, registration, nickname, type, make, model, color, theme;
        public boolean active;

        public static Vehicle fromJson(JSONObject json) {
            Vehicle v = new Vehicle();
            v.id = json.optString("id");
            v.publicToken = json.optString("public_token");
            v.registration = json.optString("registration_number");
            v.nickname = json.optString("nickname");
            v.type = json.optString("vehicle_type", "Car");
            v.make = json.optString("make");
            v.model = json.optString("model");
            v.color = json.optString("color");
            v.theme = json.optString("card_theme", "midnight");
            v.active = json.optBoolean("is_active", true);
            return v;
        }

        public String description() {
            String value = (color + " · " + make + " " + model).trim();
            return value.replaceAll("^·\\s*|\\s*·$", "");
        }
    }

    public static class Alert {
        public String id, type, message, status, createdAt, registration, reporterContact;
        public double latitude, longitude;

        public static Alert fromJson(JSONObject json) {
            Alert a = new Alert();
            a.id = json.optString("id");
            a.type = json.optString("alert_type", "other");
            a.message = json.optString("message");
            a.status = json.optString("status", "new");
            a.createdAt = json.optString("created_at");
            a.reporterContact = json.optString("reporter_contact");
            a.latitude = json.optDouble("latitude", Double.NaN);
            a.longitude = json.optDouble("longitude", Double.NaN);
            JSONObject vehicle = json.optJSONObject("vehicles");
            a.registration = vehicle == null ? "Vehicle" : vehicle.optString("registration_number", "Vehicle");
            return a;
        }

        public String displayType() {
            switch (type) {
                case "blocking_access": return "Blocking access";
                case "parking_issue": return "Parking issue";
                case "lights_on": return "Lights left on";
                case "accident_damage": return "Accident or damage";
                case "vehicle_attention": return "Vehicle needs attention";
                case "emergency": return "Emergency";
                default: return "Road message";
            }
        }
    }

    public static class Trip {
        public String id, status, startedAt, endedAt, registration;
        public double distanceM, maxSpeedMps;
        public int durationSeconds, pointCount;

        public static Trip fromJson(JSONObject json) {
            Trip trip = new Trip();
            trip.id = json.optString("id"); trip.status = json.optString("status");
            trip.startedAt = json.optString("started_at"); trip.endedAt = json.optString("ended_at");
            trip.distanceM = json.optDouble("distance_m"); trip.maxSpeedMps = json.optDouble("max_speed_mps");
            trip.durationSeconds = json.optInt("duration_seconds"); trip.pointCount = json.optInt("point_count");
            JSONObject vehicle = json.optJSONObject("vehicles");
            trip.registration = vehicle == null ? "Vehicle" : vehicle.optString("registration_number", "Vehicle");
            return trip;
        }
    }
}
