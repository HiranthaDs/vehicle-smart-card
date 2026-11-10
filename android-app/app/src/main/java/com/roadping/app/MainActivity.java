package com.roadping.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SupabaseApi api = new SupabaseApi();
    private SessionManager session;
    private FrameLayout content;
    private LinearLayout bottomNav;
    private List<Models.Vehicle> vehicles = new ArrayList<>();
    private List<Models.Alert> alerts = new ArrayList<>();
    private List<Models.Trip> trips = new ArrayList<>();
    private List<Models.TrackerDevice> trackerDevices = new ArrayList<>();
    private JSONObject profile = new JSONObject();
    private Models.Vehicle pendingTrackerVehicle;
    private int currentTab = 0;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable playerTicker;
    private TextView playerClock, playerDate, playerSpeed, playerLocation, playerAccuracy;
    private PlayerMapView playerMap;
    private long lastPlayerModeSync;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Ui.SURFACE);
        window.setNavigationBarColor(Ui.WHITE);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
        session = new SessionManager(this);
        NotificationHelper.createChannel(this);
        if (session.isSignedIn()) {
            if (getIntent().getBooleanExtra("open_alerts", false)) { session.setAppRole("owner"); showApp(3); }
            else showModeChooser();
        }
        else showModeChooser();
    }

    @Override protected void onDestroy() {
        stopPlayerTicker();
        executor.shutdownNow();
        super.onDestroy();
    }

    private void showModeChooser() {
        stopPlayerTicker();
        bottomNav = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        LinearLayout root = Ui.vertical(this, 0); root.setBackgroundColor(Ui.SURFACE);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout page = Ui.vertical(this, 24); page.setPadding(Ui.dp(this, 24), Ui.dp(this, 34), Ui.dp(this, 24), Ui.dp(this, 34));
        TextView brand = Ui.title(this, "ROADPING."); brand.setTextSize(27); page.addView(brand);
        TextView eyebrow = Ui.eyebrow(this, "ONE ACCOUNT / TWO PURPOSES"); Ui.margin(eyebrow, 0, 46, 0, 10); page.addView(eyebrow);
        TextView title = Ui.title(this, "WHERE IS THIS\nAPP RUNNING?"); title.setTextSize(43); page.addView(title);
        TextView copy = Ui.text(this, "Choose the screen in front of you. Your owner phone and vehicle player use the same secure RoadPing account.", 14, Ui.MUTED, false); Ui.margin(copy, 0, 13, 0, 28); page.addView(copy);

        LinearLayout owner = modeChoice("person", "OWNER PHONE", "Manage every vehicle, open QR cards, receive alerts and watch the car player from anywhere.", Ui.BLUE);
        owner.setOnClickListener(v -> { session.setAppRole("owner"); if (session.isSignedIn()) showApp(0); else showLogin(false); }); page.addView(owner);
        LinearLayout player = modeChoice("location", "ANDROID CAR PLAYER", "Connect this head unit to one vehicle, record journeys and show a calm driving display.", Ui.NAVY);
        Ui.margin(player, 0, 12, 0, 0); player.setOnClickListener(v -> { session.setAppRole("player"); if (!session.isSignedIn()) showLogin(false); else if (session.trackerConfigured()) showPlayerDisplay(); else showApp(2); }); page.addView(player);

        TextView note = Ui.text(this, "You can switch modes at any time. Tracking is always visible and can always be stopped.", 11, Ui.MUTED, false); note.setGravity(Gravity.CENTER); Ui.margin(note, 10, 24, 10, 0); page.addView(note);
        scroll.addView(page); root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1)); setRootContent(root);
    }

    private LinearLayout modeChoice(String iconName, String title, String description, int accent) {
        LinearLayout card = Ui.card(this, 22); card.setMinimumHeight(Ui.dp(this, 170)); card.setGravity(Gravity.CENTER_VERTICAL); card.setFocusable(true); card.setClickable(true); card.setBackground(Ui.interactive(Ui.WHITE, 18, Ui.LINE, this));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = Ui.icon(this, iconName, accent); icon.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12)); icon.setBackground(Ui.background(Color.rgb(238, 241, 255), 28, this));
        top.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 54), Ui.dp(this, 54)));
        TextView heading = Ui.title(this, title); heading.setTextSize(24); Ui.margin(heading, 16, 0, 0, 0); top.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView arrow = Ui.text(this, "→", 24, accent, true); top.addView(arrow); card.addView(top);
        TextView detail = Ui.text(this, description, 13, Ui.MUTED, false); detail.setLineSpacing(0, 1.25f); Ui.margin(detail, 0, 19, 0, 0); card.addView(detail);
        card.setContentDescription(title + ". " + description); return card;
    }

    private void showPlayerDisplay() {
        stopPlayerTicker();
        if (!session.trackerConfigured()) { showApp(2); return; }
        session.setAppRole("player"); bottomNav = null;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        LinearLayout root = Ui.vertical(this, 0); root.setBackgroundColor(Color.rgb(10, 11, 14));

        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(Ui.dp(this, 22), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 12));
        LinearLayout identity = Ui.vertical(this, 0); TextView brand = Ui.title(this, "ROADPING."); brand.setTextColor(Ui.WHITE); brand.setTextSize(24); identity.addView(brand);
        TextView role = Ui.text(this, "VEHICLE PLAYER  ·  " + session.trackingVehicleName(), 8, Ui.TEAL, true); role.setLetterSpacing(.11f); Ui.margin(role, 0, 5, 0, 0); identity.addView(role); top.addView(identity, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button owner = Ui.secondaryButton(this, "OWNER MODE"); owner.setMinHeight(Ui.dp(this, 42)); owner.setOnClickListener(v -> showModeChooser()); top.addView(owner); root.addView(top);

        String mode = session.displayMode();
        boolean mapMode = "map".equals(mode);
        boolean sideBySide = mapMode && getResources().getDisplayMetrics().widthPixels > getResources().getDisplayMetrics().heightPixels;
        LinearLayout display = new LinearLayout(this); display.setOrientation(sideBySide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL); display.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 12));
        if (mapMode) {
            playerMap = new PlayerMapView(this); display.addView(playerMap, sideBySide ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.6f) : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 310)));
        } else playerMap = null;

        LinearLayout readout = Ui.vertical(this, 22); readout.setGravity(Gravity.CENTER); readout.setBackground(Ui.background(Color.rgb(18, 20, 24), 18, this));
        TextView status = Ui.text(this, session.trackingEnabled() ? "●  LIVE JOURNEY" : "PLAYER READY", 10, session.trackingEnabled() ? Ui.TEAL : Ui.PINK, true); status.setLetterSpacing(.12f); readout.addView(status);
        playerClock = Ui.title(this, "--:--"); playerClock.setTextColor(Ui.WHITE); playerClock.setTextSize("clock".equals(mode) ? 96 : 46); playerClock.setGravity(Gravity.CENTER); Ui.margin(playerClock, 0, 18, 0, 0); readout.addView(playerClock);
        playerDate = Ui.text(this, "", 13, Color.rgb(169, 173, 184), false); playerDate.setGravity(Gravity.CENTER); readout.addView(playerDate);
        playerSpeed = Ui.title(this, "0"); playerSpeed.setTextColor(Ui.WHITE); playerSpeed.setTextSize("speed".equals(mode) ? 112 : mapMode ? 58 : 78); playerSpeed.setGravity(Gravity.CENTER); Ui.margin(playerSpeed, 0, "clock".equals(mode) ? 34 : 18, 0, 0); readout.addView(playerSpeed);
        TextView speedUnit = Ui.text(this, "KM/H", 10, Ui.TEAL, true); speedUnit.setLetterSpacing(.16f); speedUnit.setGravity(Gravity.CENTER); readout.addView(speedUnit);
        playerLocation = Ui.text(this, "Waiting for GPS location", 13, Ui.WHITE, true); playerLocation.setGravity(Gravity.CENTER); Ui.margin(playerLocation, 0, 28, 0, 4); readout.addView(playerLocation);
        playerAccuracy = Ui.text(this, "Location will appear when tracking starts", 10, Color.rgb(145, 149, 160), false); playerAccuracy.setGravity(Gravity.CENTER); readout.addView(playerAccuracy);
        LinearLayout.LayoutParams readoutParams = sideBySide ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, .85f) : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        if (sideBySide) readoutParams.setMargins(Ui.dp(this, 12), 0, 0, 0); else if (mapMode) readoutParams.setMargins(0, Ui.dp(this, 10), 0, 0);
        display.addView(readout, readoutParams); root.addView(display, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout modes = new LinearLayout(this); modes.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 10));
        String[][] choices = {{"journey", "JOURNEY"}, {"speed", "SPEED"}, {"clock", "CLOCK"}, {"map", "MAP SIDE"}};
        for (String[] choice : choices) {
            Button button = choice[0].equals(mode) ? Ui.primaryButton(this, choice[1]) : Ui.secondaryButton(this, choice[1]);
            if (choice[0].equals(mode)) button.setBackground(Ui.background(Ui.BLUE, 12, this));
            button.setOnClickListener(v -> changePlayerDisplay(choice[0]));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1); params.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0); modes.addView(button, params);
        }
        Button tracking = session.trackingEnabled() ? Ui.secondaryButton(this, "STOP") : Ui.primaryButton(this, "START"); tracking.setOnClickListener(v -> togglePlayerTracking());
        modes.addView(tracking, new LinearLayout.LayoutParams(Ui.dp(this, 92), Ui.dp(this, 48))); root.addView(modes);
        setRootContent(root); startPlayerTicker(); syncPlayerDisplayMode();
    }

    private void changePlayerDisplay(String mode) {
        if (mode.equals(session.displayMode())) return;
        session.setDisplayMode(mode); showPlayerDisplay();
        executor.execute(() -> { try { api.updateTrackerDisplayMode(session.accessToken(), session.trackerDeviceId(), mode); } catch (Exception ignored) {} });
    }

    private void togglePlayerTracking() {
        Intent service = new Intent(this, TrackingForegroundService.class).setAction(session.trackingEnabled() ? TrackingForegroundService.ACTION_STOP : TrackingForegroundService.ACTION_START);
        if (session.trackingEnabled()) startService(service); else startForegroundService(service);
        session.setTrackingEnabled(!session.trackingEnabled()); toast(session.trackingEnabled() ? "Tracking started." : "Trip is being saved."); showPlayerDisplay();
    }

    private void startPlayerTicker() {
        playerTicker = new Runnable() { @Override public void run() { renderPlayerValues(); uiHandler.postDelayed(this, 1000L); } };
        uiHandler.post(playerTicker);
    }

    private void stopPlayerTicker() { if (playerTicker != null) uiHandler.removeCallbacks(playerTicker); playerTicker = null; }

    private void renderPlayerValues() {
        LocalDateTime now = LocalDateTime.now();
        if (playerClock != null) playerClock.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));
        if (playerDate != null) playerDate.setText(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())));
        if (playerSpeed != null) playerSpeed.setText(String.valueOf(Math.round(session.lastSpeedMps() * 3.6f)));
        boolean hasFix = !Double.isNaN(session.lastLatitude()) && !Double.isNaN(session.lastLongitude());
        if (playerLocation != null) playerLocation.setText(hasFix ? String.format(Locale.ROOT, "%.5f, %.5f", session.lastLatitude(), session.lastLongitude()) : "Waiting for GPS location");
        if (playerAccuracy != null) {
            long age = session.lastLocationAt() == 0 ? 0 : Math.max(0, (System.currentTimeMillis() - session.lastLocationAt()) / 1000);
            playerAccuracy.setText(hasFix ? "GPS ±" + Math.round(session.lastAccuracyM()) + " m  ·  updated " + age + "s ago" : "Start tracking to show the current location");
        }
        if (playerMap != null) playerMap.setLocation(session.lastLatitude(), session.lastLongitude(), session.lastBearing());
        if (System.currentTimeMillis() - lastPlayerModeSync > 30000L) syncPlayerDisplayMode();
    }

    private void syncPlayerDisplayMode() {
        if (!session.trackerConfigured() || System.currentTimeMillis() - lastPlayerModeSync < 5000L) return;
        lastPlayerModeSync = System.currentTimeMillis();
        executor.execute(() -> { try {
            String remoteMode = api.trackerDevice(session.accessToken(), session.trackerDeviceId()).optString("display_mode", session.displayMode());
            if (!remoteMode.equals(session.displayMode())) runOnUiThread(() -> { session.setDisplayMode(remoteMode); showPlayerDisplay(); });
        } catch (Exception ignored) {} });
    }

    private void showLogin(boolean signup) {
        stopPlayerTicker();
        LinearLayout root = Ui.vertical(this, 0);
        root.setBackgroundColor(Ui.SURFACE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout body = Ui.vertical(this, 22);
        TextView wordmark = Ui.title(this, "ROADPING."); wordmark.setTextSize(27);
        Ui.margin(wordmark, 0, 4, 0, 22); body.addView(wordmark);

        LinearLayout hero = Ui.vertical(this, 25);
        hero.setBackground(new CardSurface(Ui.NAVY, Ui.dp(this, 22)));
        TextView byline = Ui.text(this, "A LITTLE PEACE OF MIND.", 10, Ui.PINK, true);
        byline.setLetterSpacing(.14f); hero.addView(byline);
        TextView message = Ui.title(this, "YOUR VEHICLE.\nCONNECTED."); message.setTextColor(Ui.WHITE); message.setTextSize(43);
        Ui.margin(message, 0, 20, 0, 14); hero.addView(message);
        TextView introduction = Ui.text(this, "One smart card. A simpler way to stay connected to your vehicle.", 13, Color.rgb(205,205,205), false);
        hero.addView(introduction); body.addView(hero);

        LinearLayout form = Ui.vertical(this, 0);
        form.setFocusableInTouchMode(true);
        TextView eyebrow = Ui.eyebrow(this, signup ? "01 / JOIN ROADPING" : "01 / YOUR OWNER SPACE"); Ui.margin(eyebrow, 0, 30, 0, 10); form.addView(eyebrow);
        TextView title = Ui.title(this, signup ? "LET'S GET STARTED." : "WELCOME BACK."); title.setTextSize(31); form.addView(title);
        TextView subtitle = Ui.text(this, signup ? "Your garage starts here. Add your vehicles after signing up." : "Your vehicles, smart cards and alerts. All together.", 14, Ui.MUTED, false); Ui.margin(subtitle, 0, 10, 0, 25); form.addView(subtitle);
        EditText name = Ui.input(this, "Your full name");
        EditText email = Ui.input(this, "you@example.com"); email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); email.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS);
        EditText phone = Ui.input(this, "+94 77 123 4567"); phone.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText emergency = Ui.input(this, "Emergency contact number"); emergency.setInputType(InputType.TYPE_CLASS_PHONE);
        EditText password = Ui.passwordInput(this, "At least 8 characters"); password.setAutofillHints(signup ? "newPassword" : View.AUTOFILL_HINT_PASSWORD);
        if (signup) { addField(form, "FULL NAME", name); addField(form, "PHONE NUMBER", phone); addField(form, "EMERGENCY CONTACT", emergency); }
        addField(form, "EMAIL ADDRESS", email); addField(form, "PASSWORD", password);
        Button submit = Ui.primaryButton(this, signup ? "Create my account  ->" : "Sign in  ->"); Ui.margin(submit, 0, 6, 0, 12); form.addView(submit);
        Button toggle = Ui.secondaryButton(this, signup ? "Already a member? Sign in" : "New here? Create an account"); form.addView(toggle);
        LinearLayout privacy = new LinearLayout(this); privacy.setGravity(Gravity.CENTER_VERTICAL);
        ImageView lock = Ui.icon(this, "shield", Ui.MUTED); privacy.addView(lock, new LinearLayout.LayoutParams(Ui.dp(this, 18), Ui.dp(this, 18)));
        TextView privacyText = Ui.text(this, "Your number stays private by default.", 11, Ui.MUTED, false); Ui.margin(privacyText, 9, 0, 0, 0); privacy.addView(privacyText);
        Ui.margin(privacy, 5, 24, 5, 12); form.addView(privacy);
        body.addView(form); scroll.addView(body); root.addView(scroll);
        setRootContent(root);

        toggle.setOnClickListener(v -> showLogin(!signup));
        submit.setOnClickListener(v -> {
            if (email.getText().toString().trim().isEmpty() || password.getText().length() < 8 || (signup && name.getText().toString().trim().isEmpty())) { toast("Complete the required fields. Passwords need 8 characters."); return; }
            submit.setEnabled(false); submit.setText(signup ? "CREATING ACCOUNT..." : "SIGNING IN...");
            executor.execute(() -> {
                try {
                    JSONObject response;
                    if (signup) response = api.signUp(name.getText().toString().trim(), email.getText().toString().trim(), phone.getText().toString().trim(), emergency.getText().toString().trim(), password.getText().toString());
                    else response = api.signIn(email.getText().toString().trim(), password.getText().toString());
                    String access = response.optString("access_token");
                    if (access.isEmpty()) throw new Exception("Your account could not be started immediately. RoadPing registration does not require email confirmation.");
                    JSONObject user = response.optJSONObject("user");
                    session.save(access, response.optString("refresh_token"), user == null ? "" : user.optString("id"), email.getText().toString().trim());
                    runOnUiThread(this::openSelectedRole);
                } catch (Exception error) { runOnUiThread(() -> { toast(error.getMessage()); submit.setEnabled(true); submit.setText(signup ? "Create my account  ->" : "Sign in  ->"); }); }
            });
        });
    }

    private void openSelectedRole() {
        if ("player".equals(session.appRole())) {
            if (session.trackerConfigured()) showPlayerDisplay(); else showApp(2);
        } else if ("owner".equals(session.appRole())) showApp(0);
        else showModeChooser();
    }

    private void showApp(int startTab) {
        stopPlayerTicker();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        currentTab = startTab;
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 80);
        scheduleAlertChecks();

        LinearLayout root = Ui.vertical(this, 0);
        root.setBackgroundColor(Ui.SURFACE);
        root.addView(appHeader());
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 5));
        bottomNav.setBackground(Ui.outlined(Ui.WHITE, 0, Ui.LINE, this));
        root.addView(bottomNav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 76)));
        setRootContent(root);
        buildBottomNav();
        showLoading();
        loadAll();
    }

    private View appHeader() {
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(this, 22), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12));
        header.setBackground(Ui.outlined(Ui.SURFACE, 0, Ui.LINE, this));
        LinearLayout brand = Ui.vertical(this, 0);
        TextView logo = Ui.title(this, "ROADPING."); logo.setTextSize(26); brand.addView(logo);
        TextView byline = Ui.text(this, "BY HICH / BUILT AROUND YOU", 7, Ui.MUTED, true); byline.setLetterSpacing(.12f); Ui.margin(byline, 0, 4, 0, 0); brand.addView(byline);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout status = new LinearLayout(this); status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(Ui.dp(this, 12), Ui.dp(this, 9), Ui.dp(this, 12), Ui.dp(this, 9));
        status.setBackground(Ui.background(Ui.WHITE, 24, this));
        ImageView icon = Ui.icon(this, session.trackingEnabled() ? "location" : "shield", Ui.BLUE);
        status.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 16), Ui.dp(this, 16)));
        TextView label = Ui.text(this, session.trackingEnabled() ? "Live" : "Owner", 11, Ui.INK, true);
        Ui.margin(label, 6, 0, 0, 0); status.addView(label); header.addView(status);
        status.setContentDescription("Change device mode"); status.setFocusable(true); status.setOnClickListener(v -> showModeChooser());
        header.setMinimumHeight(Ui.dp(this, 76)); return header;
    }

    private void buildBottomNav() {
        bottomNav.removeAllViews();
        String[] icons = {"home", "car", "location", "bell", "person"};
        String[] labels = {"Home", "Garage", "Live", "Alerts", "Profile"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i; boolean selected = i == currentTab;
            LinearLayout item = Ui.vertical(this, 0); item.setGravity(Gravity.CENTER);
            item.setBackground(Ui.interactive(selected ? Color.rgb(234,239,255) : Ui.WHITE, 20, selected ? Color.rgb(234,239,255) : Ui.WHITE, this));
            ImageView icon = Ui.icon(this, icons[i], selected ? Ui.BLUE : Ui.MUTED);
            item.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22)));
            TextView label = Ui.text(this, labels[i], 10, selected ? Ui.BLUE : Ui.MUTED, true);
            label.setGravity(Gravity.CENTER); Ui.margin(label, 0, 5, 0, 0); item.addView(label);
            item.setContentDescription(labels[i] + (selected ? ", selected" : "")); item.setSelected(selected);
            item.setFocusable(true); item.setOnClickListener(v -> { currentTab = index; buildBottomNav(); renderCurrent(); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            params.setMargins(Ui.dp(this, 3), Ui.dp(this, 2), Ui.dp(this, 3), Ui.dp(this, 2)); bottomNav.addView(item, params);
        }
    }

    private void showLoading() {
        content.removeAllViews();
        LinearLayout holder = Ui.vertical(this, 30);
        holder.setGravity(Gravity.CENTER);
        ProgressBar progress = new ProgressBar(this);
        holder.addView(progress);
        TextView text = Ui.text(this, "Syncing your protected garage…", 14, Ui.MUTED, false);
        Ui.margin(text, 0, 12, 0, 0);
        holder.addView(text);
        content.addView(holder, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void loadAll() {
        executor.execute(() -> {
            try {
                vehicles = api.vehicles(session.accessToken());
                alerts = api.alerts(session.accessToken());
                trips = api.trips(session.accessToken());
                trackerDevices = api.trackerDevices(session.accessToken());
                profile = api.profile(session.accessToken(), session.userId());
                session.setLastAlertCheck(System.currentTimeMillis());
                runOnUiThread(this::renderCurrent);
            } catch (SupabaseApi.ApiException error) {
                if (error.statusCode == 401 && refreshSession()) { loadAll(); return; }
                runOnUiThread(() -> showLoadError(error.getMessage()));
            } catch (Exception error) { runOnUiThread(() -> showLoadError(error.getMessage())); }
        });
    }

    private boolean refreshSession() {
        if (session.refreshToken().isEmpty()) return false;
        try {
            JSONObject response = api.refresh(session.refreshToken());
            JSONObject user = response.optJSONObject("user");
            session.save(response.optString("access_token"), response.optString("refresh_token"), user == null ? session.userId() : user.optString("id"), session.email());
            return true;
        } catch (Exception ignored) { return false; }
    }

    private void showLoadError(String message) {
        content.removeAllViews();
        LinearLayout box = Ui.vertical(this, 28);
        box.setGravity(Gravity.CENTER);
        TextView title = Ui.text(this, "Couldn’t open your garage", 20, Ui.INK, true);
        box.addView(title);
        TextView detail = Ui.text(this, "Please check your connection and try again. Your saved vehicles are still in your account.", 14, Ui.MUTED, false);
        detail.setGravity(Gravity.CENTER);
        Ui.margin(detail, 0, 10, 0, 18);
        box.addView(detail);
        Button retry = Ui.primaryButton(this, "Try again");
        retry.setOnClickListener(v -> { showLoading(); loadAll(); });
        box.addView(retry);
        content.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void renderCurrent() {
        if (content == null) return;
        if (currentTab == 0) showHome();
        else if (currentTab == 1) showVehicles();
        else if (currentTab == 2) showTracking();
        else if (currentTab == 3) showAlerts();
        else showProfile();
    }

    private void showHome() {
        LinearLayout page = page();
        String fullName = profile.optString("full_name", "Owner");
        String firstName = fullName.trim().isEmpty() ? "Owner" : fullName.trim().split(" ")[0];
        page.addView(Ui.eyebrow(this, "01 / YOUR EVERYDAY COMPANION"));
        TextView title = Ui.title(this, "HELLO, " + firstName + ".");
        Ui.margin(title, 0, 10, 0, 10); page.addView(title);
        page.addView(Ui.text(this, "A little less worry. A lot more connection.", 14, Ui.MUTED, false));

        LinearLayout stats = new LinearLayout(this); Ui.margin(stats, 0, 24, 0, 0);
        long active = vehicles.stream().filter(v -> v.active).count();
        stats.addView(statCard("ACTIVE VEHICLES", String.valueOf(active), "car", Ui.BLUE), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 12), 1));
        long unread = alerts.stream().filter(a -> "new".equals(a.status)).count();
        View alertStat = statCard("NEW ALERTS", String.valueOf(unread), "bell", unread > 0 ? Ui.RED : Ui.BLUE);
        alertStat.setOnClickListener(v -> { currentTab = 3; buildBottomNav(); showAlerts(); });
        alertStat.setContentDescription(unread + " new alerts. Open alerts");
        stats.addView(alertStat, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); page.addView(stats);

        sectionHeader(page, "02 / YOUR GARAGE", "MADE FOR YOUR DRIVE.", "Add vehicle", this::showAddVehicleDialog);
        if (vehicles.isEmpty()) page.addView(emptyCard("YOUR FIRST CONNECTION.", "Add your vehicle and we'll make it a smart card."));
        else for (Models.Vehicle vehicle : vehicles.subList(0, Math.min(2, vehicles.size()))) page.addView(vehicleCard(vehicle));
        if (vehicles.size() > 2) {
            Button all = Ui.secondaryButton(this, "View all " + vehicles.size() + " vehicles  ->");
            all.setOnClickListener(v -> { currentTab = 1; buildBottomNav(); showVehicles(); }); page.addView(all);
        }

        LinearLayout tracker = Ui.vertical(this, 24); Ui.margin(tracker, 0, 24, 0, 0);
        tracker.setBackground(new CardSurface(Ui.BLUE, Ui.dp(this, 22)));
        TextView trackerEye = Ui.text(this, "03 / A LITTLE MORE CONNECTED", 9, Color.rgb(214,224,255), true); trackerEye.setLetterSpacing(.12f); tracker.addView(trackerEye);
        TextView trackerTitle = Ui.title(this, session.trackerConfigured() ? (session.trackingEnabled() ? "YOUR JOURNEY.\nLIVE." : "READY WHEN\nYOU ARE.") : "KNOW WHERE.\nWORRY LESS.");
        trackerTitle.setTextColor(Ui.WHITE); trackerTitle.setTextSize(36); Ui.margin(trackerTitle, 0, 17, 0, 12); tracker.addView(trackerTitle);
        TextView trackerCopy = Ui.text(this, session.trackerConfigured() ? session.trackingVehicleName() + " / Your location and journey history, together." : "Connect your vehicle's Android player to keep your journeys in one place.", 13, Color.rgb(223,230,255), false); tracker.addView(trackerCopy);
        Button trackerButton = Ui.primaryButton(this, session.trackerConfigured() ? "Open live tracking  ->" : "Set up vehicle player  ->"); Ui.margin(trackerButton, 0, 22, 0, 0);
        trackerButton.setOnClickListener(v -> { currentTab = 2; buildBottomNav(); showTracking(); }); tracker.addView(trackerButton); page.addView(tracker);

        sectionHeader(page, "04 / RECENT ACTIVITY", "YOUR LATEST PINGS.", null, null);
        if (alerts.isEmpty()) page.addView(emptyCard("ALL QUIET. ALL GOOD.", "When someone scans your card and sends a report, you'll find it here."));
        else for (Models.Alert alert : alerts.subList(0, Math.min(3, alerts.size()))) page.addView(alertRow(alert));

        LinearLayout privacy = new LinearLayout(this); privacy.setGravity(Gravity.CENTER_VERTICAL); Ui.margin(privacy, 0, 24, 0, 0);
        privacy.addView(Ui.icon(this, "shield", Ui.BLUE), new LinearLayout.LayoutParams(Ui.dp(this, 20), Ui.dp(this, 20)));
        TextView privacyText = Ui.text(this, "Your number is " + profile.optString("phone_visibility", "hidden") + ". You're in control.", 14, Ui.MUTED, false);
        Ui.margin(privacyText, 10, 0, 0, 0); privacy.addView(privacyText);
        privacy.setOnClickListener(v -> { currentTab = 4; buildBottomNav(); showProfile(); }); privacy.setMinimumHeight(Ui.dp(this, 48)); page.addView(privacy);
        setPage(page);
    }

    private void showVehicles() {
        LinearLayout page = page();
        page.addView(Ui.eyebrow(this, "YOUR GARAGE"));
        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.title(this, "Vehicles"); head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button add = Ui.primaryButton(this, "+ Add"); add.setMinHeight(Ui.dp(this, 42)); add.setOnClickListener(v -> showAddVehicleDialog()); head.addView(add);
        Ui.margin(head, 0, 6, 0, 6); page.addView(head);
        TextView sub = Ui.text(this, "One home for every vehicle. A smart card for each.", 14, Ui.MUTED, false); Ui.margin(sub, 0, 0, 0, 20); page.addView(sub);
        if (vehicles.isEmpty()) page.addView(emptyCard("Your garage is waiting", "Tap Add to create your first vehicle smart card."));
        else for (Models.Vehicle vehicle : vehicles) page.addView(vehicleCard(vehicle));
        setPage(page);
    }

    private void showTracking() {
        LinearLayout page = page();
        page.addView(Ui.eyebrow(this, "VEHICLE INTELLIGENCE"));
        TextView title = Ui.title(this, "EVERY JOURNEY.\nCONNECTED."); Ui.margin(title, 0, 7, 0, 5); page.addView(title);
        TextView sub = Ui.text(this, "Connect your vehicle's Android player and keep every journey close.", 14, Ui.MUTED, false); Ui.margin(sub, 0, 0, 0, 20); page.addView(sub);

        if ("owner".equals(session.appRole())) { showOwnerTracking(page); setPage(page); return; }

        if (!session.trackerConfigured()) {
            LinearLayout setup = Ui.vertical(this, 24); setup.setBackground(Ui.background(Ui.BLUE, 22, this));
            setup.addView(coloredDisplay("YOUR PLAYER.\nMORE POSSIBILITY.", Ui.WHITE, 35));
            TextView copy = Ui.text(this, "Choose a vehicle, review permissions and start a foreground location service. A persistent Android notification always shows when tracking is active.", 13, Color.rgb(223,230,255), false); Ui.margin(copy, 0, 15, 0, 20); setup.addView(copy);
            String[] steps = {"01 / SELECT ONE VEHICLE", "02 / ALLOW PRECISE LOCATION", "03 / ENABLE BACKGROUND ACCESS", "04 / KEEP THE TRACKING NOTICE VISIBLE"};
            for (String step : steps) { TextView row = Ui.text(this, step, 10, Ui.WHITE, true); row.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14)); row.setBackground(Ui.outlined(Ui.BLUE, 10, Color.rgb(89,119,246), this)); setup.addView(row); }
            Button begin = Ui.secondaryButton(this, "SET UP THIS VEHICLE"); Ui.margin(begin, 0, 20, 0, 0); begin.setOnClickListener(v -> chooseTrackingVehicle()); setup.addView(begin); page.addView(setup);
        } else {
            LinearLayout live = Ui.vertical(this, 22); live.setBackground(Ui.background(Ui.NAVY, 22, this));
            TextView state = Ui.text(this, session.trackingEnabled() ? "LIVE / TRACKING ACTIVE" : "PLAYER READY / NOT TRACKING", 9, session.trackingEnabled() ? Ui.TEAL : Ui.PINK, true); state.setLetterSpacing(.11f); live.addView(state);
            TextView vehicle = coloredDisplay(session.trackingVehicleName(), Ui.WHITE, 36); Ui.margin(vehicle, 0, 15, 0, 5); live.addView(vehicle);
            TextView status = Ui.text(this, session.trackingEnabled() ? "GPS is recorded every few seconds. Offline points remain queued securely on this device." : "Start a new trip when this vehicle is ready to move.", 13, Color.rgb(205,205,215), false); live.addView(status);
            Button toggle = session.trackingEnabled() ? Ui.secondaryButton(this, "STOP AND SAVE TRIP") : Ui.primaryButton(this, "START VEHICLE TRACKING"); Ui.margin(toggle, 0, 20, 0, 0); live.addView(toggle);
            toggle.setOnClickListener(v -> {
                Intent service = new Intent(this, TrackingForegroundService.class).setAction(session.trackingEnabled() ? TrackingForegroundService.ACTION_STOP : TrackingForegroundService.ACTION_START);
                if (session.trackingEnabled()) startService(service); else startForegroundService(service);
                session.setTrackingEnabled(!session.trackingEnabled()); toast(session.trackingEnabled() ? "Tracking started. The persistent status is now visible." : "Trip is being saved and closed."); showTracking();
            });
            Button permissions = Ui.secondaryButton(this, "LOCATION AND BATTERY SETTINGS"); Ui.margin(permissions, 0, 10, 0, 0); permissions.setOnClickListener(v -> openAppSettings()); live.addView(permissions);
            page.addView(live);
        }

        sectionHeader(page, "OWNER HISTORY", "Saved journeys", null, null);
        if (trips.isEmpty()) page.addView(emptyCard("No journeys recorded", "A completed Vehicle Player session will appear here with distance, time and speed."));
        else for (Models.Trip trip : trips) {
            LinearLayout row = Ui.card(this, 16); Ui.margin(row, 0, 0, 0, 10);
            LinearLayout top = new LinearLayout(this); TextView registration = Ui.text(this, trip.registration, 13, Ui.INK, true); top.addView(registration, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); TextView state = Ui.text(this, trip.status.toUpperCase(Locale.ROOT), 8, "active".equals(trip.status) ? Ui.TEAL_DARK : Ui.BLUE, true); top.addView(state); row.addView(top);
            TextView when = Ui.text(this, trip.startedAt.replace("T", " ").replace("Z", " UTC"), 9, Ui.MUTED, false); Ui.margin(when, 0, 6, 0, 14); row.addView(when);
            LinearLayout metrics = new LinearLayout(this);
            metrics.addView(tripMetric("DISTANCE", String.format(Locale.ROOT, "%.1f km", trip.distanceM / 1000)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            metrics.addView(tripMetric("DURATION", (trip.durationSeconds / 60) + " min"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            metrics.addView(tripMetric("MAX SPEED", Math.round(trip.maxSpeedMps * 3.6) + " km/h"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); row.addView(metrics); page.addView(row);
        }
        setPage(page);
    }

    private void showOwnerTracking(LinearLayout page) {
        sectionHeader(page, "OWNER VIEW", "Your vehicle players", "Refresh", () -> { showLoading(); loadAll(); });
        if (trackerDevices.isEmpty()) {
            LinearLayout empty = Ui.card(this, 24);
            empty.addView(coloredDisplay("NO CAR PLAYER\nCONNECTED YET.", Ui.INK, 31));
            TextView copy = Ui.text(this, "Open RoadPing on the Android head unit, choose Android Car Player, sign in with this same account and connect one vehicle.", 13, Ui.MUTED, false); Ui.margin(copy, 0, 14, 0, 0); empty.addView(copy); page.addView(empty);
        } else for (Models.TrackerDevice device : trackerDevices) {
            LinearLayout card = Ui.vertical(this, 20); Ui.margin(card, 0, 0, 0, 14); card.setBackground(Ui.background(Ui.NAVY, 18, this));
            LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout identity = Ui.vertical(this, 0); TextView registration = Ui.title(this, device.registration); registration.setTextColor(Ui.WHITE); registration.setTextSize(31); identity.addView(registration);
            TextView name = Ui.text(this, device.name, 10, Color.rgb(177,181,195), false); Ui.margin(name, 0, 5, 0, 0); identity.addView(name); top.addView(identity, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView state = Ui.text(this, device.tracking ? "● LIVE" : "OFFLINE", 9, device.tracking ? Ui.TEAL : Ui.PINK, true); top.addView(state); card.addView(top);

            LinearLayout metrics = new LinearLayout(this); Ui.margin(metrics, 0, 20, 0, 16);
            metrics.addView(playerMetric("SPEED", Math.round(device.speedMps * 3.6) + " km/h"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            metrics.addView(playerMetric("NETWORK", device.networkConnected ? "Connected" : "Offline"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            metrics.addView(playerMetric("SCREEN", device.displayMode.replace('_', ' ')), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); card.addView(metrics);
            TextView location = Ui.text(this, Double.isNaN(device.latitude) ? "Waiting for the first GPS location" : String.format(Locale.ROOT, "CURRENT LOCATION  %.5f, %.5f", device.latitude, device.longitude), 10, Color.rgb(205,209,221), true); card.addView(location);
            if (!Double.isNaN(device.latitude)) { Button map = Ui.secondaryButton(this, "OPEN CURRENT LOCATION"); Ui.margin(map, 0, 14, 0, 0); map.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + device.latitude + "," + device.longitude + "?q=" + device.latitude + "," + device.longitude + "(" + Uri.encode(device.registration) + ")")))); card.addView(map); }

            TextView displayLabel = Ui.text(this, "PLAYER SCREEN · CHANGE FROM THIS PHONE", 8, Ui.TEAL, true); displayLabel.setLetterSpacing(.09f); Ui.margin(displayLabel, 0, 20, 0, 8); card.addView(displayLabel);
            LinearLayout choices = new LinearLayout(this); String[][] modes = {{"journey", "JOURNEY"}, {"speed", "SPEED"}, {"clock", "CLOCK"}, {"map", "MAP"}};
            for (String[] mode : modes) { Button button = mode[0].equals(device.displayMode) ? Ui.primaryButton(this, mode[1]) : Ui.secondaryButton(this, mode[1]); button.setTextSize(9); button.setOnClickListener(v -> updateRemotePlayerDisplay(device, mode[0])); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1); params.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0); choices.addView(button, params); }
            card.addView(choices); page.addView(card);
        }

        sectionHeader(page, "JOURNEY HISTORY", "Saved trips", null, null);
        if (trips.isEmpty()) page.addView(emptyCard("No journeys recorded", "Completed car-player trips will appear here with distance, duration and maximum speed."));
        else for (Models.Trip trip : trips) {
            LinearLayout row = Ui.card(this, 16); Ui.margin(row, 0, 0, 0, 10);
            LinearLayout top = new LinearLayout(this); TextView registration = Ui.text(this, trip.registration, 13, Ui.INK, true); top.addView(registration, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); TextView state = Ui.text(this, trip.status.toUpperCase(Locale.ROOT), 8, "active".equals(trip.status) ? Ui.TEAL_DARK : Ui.BLUE, true); top.addView(state); row.addView(top);
            TextView when = Ui.text(this, trip.startedAt.replace("T", " ").replace("Z", " UTC"), 9, Ui.MUTED, false); Ui.margin(when, 0, 6, 0, 14); row.addView(when);
            LinearLayout metrics = new LinearLayout(this); metrics.addView(tripMetric("DISTANCE", String.format(Locale.ROOT, "%.1f km", trip.distanceM / 1000)), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); metrics.addView(tripMetric("DURATION", (trip.durationSeconds / 60) + " min"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); metrics.addView(tripMetric("MAX SPEED", Math.round(trip.maxSpeedMps * 3.6) + " km/h"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)); row.addView(metrics); page.addView(row);
        }
    }

    private View playerMetric(String label, String value) {
        LinearLayout box = Ui.vertical(this, 0); box.addView(Ui.text(this, label, 7, Color.rgb(137,142,155), true)); TextView amount = Ui.text(this, value, 13, Ui.WHITE, true); Ui.margin(amount, 0, 5, 0, 0); box.addView(amount); return box;
    }

    private void updateRemotePlayerDisplay(Models.TrackerDevice device, String mode) {
        device.displayMode = mode; toast("Updating " + device.registration + " player screen…"); showTracking();
        executor.execute(() -> { try { api.updateTrackerDisplayMode(session.accessToken(), device.id, mode); runOnUiThread(() -> toast("Car player screen updated.")); } catch (Exception error) { runOnUiThread(() -> { toast(error.getMessage()); loadAll(); }); } });
    }

    private TextView coloredDisplay(String value, int color, float size) { TextView view = Ui.title(this, value); view.setTextColor(color); view.setTextSize(size); return view; }
    private View tripMetric(String label, String value) { LinearLayout box = Ui.vertical(this, 0); box.addView(Ui.text(this, label, 7, Ui.MUTED, true)); TextView amount = Ui.text(this, value, 13, Ui.INK, true); Ui.margin(amount, 0, 4, 0, 0); box.addView(amount); return box; }

    private void chooseTrackingVehicle() {
        if (vehicles.isEmpty()) { toast("Add a vehicle before setting up Vehicle Player Mode."); showAddVehicleDialog(); return; }
        String[] labels = new String[vehicles.size()]; for (int i = 0; i < vehicles.size(); i++) labels[i] = vehicles.get(i).registration + " / " + (vehicles.get(i).nickname.isEmpty() ? vehicles.get(i).description() : vehicles.get(i).nickname);
        new AlertDialog.Builder(this).setTitle("Which vehicle is this player inside?").setItems(labels, (dialog, which) -> showTrackingConsent(vehicles.get(which))).setNegativeButton("Cancel", null).show();
    }

    private void showTrackingConsent(Models.Vehicle vehicle) {
        pendingTrackerVehicle = vehicle;
        new AlertDialog.Builder(this).setTitle("Setup " + vehicle.registration)
                .setMessage("RoadPing will request precise location and notifications. For reliable screen-off and restart tracking, also choose Allow all the time in Android settings and exclude RoadPing from battery optimisation.\n\nNetwork access syncs encrypted owner-only route batches. A persistent notification remains visible while tracking.")
                .setNegativeButton("Not now", null).setPositiveButton("Continue", (dialog, which) -> requestTrackerPermission()).show();
    }

    private void requestTrackerPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101); return; }
        completeTrackerSetup();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) { if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) completeTrackerSetup(); else toast("Precise location is required for Vehicle Player Mode."); }
    }

    private void completeTrackerSetup() {
        Models.Vehicle vehicle = pendingTrackerVehicle; if (vehicle == null) return;
        LocationManager manager = getSystemService(LocationManager.class); boolean locationEnabled = manager != null && (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        if (!locationEnabled) { toast("Turn on Android location services, then return to RoadPing."); startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); }
        executor.execute(() -> {
            try {
                JSONObject device = api.registerTracker(session.accessToken(), vehicle.id, session.installationId(), Build.MANUFACTURER + " " + Build.MODEL,
                        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ? "precise" : "approximate",
                        hasBackgroundLocation(), hasNotificationPermission(), batteryOptimisationIgnored(), locationEnabled, isOnline());
                session.configureTracker(device.optString("id"), vehicle.id, vehicle.registration); session.setTrackingEnabled(true);
                runOnUiThread(() -> {
                    startForegroundService(new Intent(this, TrackingForegroundService.class).setAction(TrackingForegroundService.ACTION_START));
                    toast("Vehicle Player Mode is active for " + vehicle.registration + "."); if ("player".equals(session.appRole())) showPlayerDisplay(); else showTracking();
                    if (!hasBackgroundLocation() || !batteryOptimisationIgnored()) new AlertDialog.Builder(this).setTitle("Finish reliable background setup").setMessage("In Android app settings, set Location to Allow all the time. Then allow RoadPing to run without battery restriction. This lets a head unit resume after power or screen changes.").setNegativeButton("Later", null).setPositiveButton("Open settings", (d, w) -> openAppSettings()).show();
                });
            } catch (Exception error) { runOnUiThread(() -> toast(error.getMessage())); }
        });
    }

    private boolean hasBackgroundLocation() { return Build.VERSION.SDK_INT < 29 || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED; }
    private boolean hasNotificationPermission() { return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED; }
    private boolean batteryOptimisationIgnored() { PowerManager power = getSystemService(PowerManager.class); return power != null && power.isIgnoringBatteryOptimizations(getPackageName()); }
    private boolean isOnline() { ConnectivityManager manager = getSystemService(ConnectivityManager.class); if (manager == null || manager.getActiveNetwork() == null) return false; NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork()); return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET); }
    private void openAppSettings() { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }

    private void showAlerts() {
        LinearLayout page = page();
        page.addView(Ui.eyebrow(this, "OWNER INBOX"));
        TextView title = Ui.title(this, "Road alerts"); Ui.margin(title, 0, 7, 0, 5); page.addView(title);
        TextView sub = Ui.text(this, "A little heads-up, right when you need it. Your vehicle alerts live here.", 14, Ui.MUTED, false); Ui.margin(sub, 0, 0, 0, 20); page.addView(sub);
        if (alerts.isEmpty()) page.addView(emptyCard("No alerts", "New QR reports will appear here and trigger a device notification."));
        else for (Models.Alert alert : alerts) page.addView(alertRow(alert));
        setPage(page);
    }

    private void showProfile() {
        LinearLayout page = page();
        page.addView(Ui.eyebrow(this, "ACCOUNT SETTINGS"));
        TextView title = Ui.title(this, "MAKE IT YOURS."); Ui.margin(title, 0, 7, 0, 5); page.addView(title);
        TextView sub = Ui.text(this, "Your details. Your privacy. You're in control.", 14, Ui.MUTED, false); Ui.margin(sub, 0, 0, 0, 20); page.addView(sub);
        LinearLayout panel = Ui.card(this, 18);
        EditText name = Ui.input(this, "Full name"); name.setText(profile.optString("full_name")); addField(panel, "FULL NAME", name);
        EditText phone = Ui.input(this, "Phone number"); phone.setInputType(InputType.TYPE_CLASS_PHONE); phone.setText(profile.optString("phone")); addField(panel, "PHONE NUMBER", phone);
        EditText emergencyName = Ui.input(this, "Emergency contact name"); emergencyName.setText(profile.optString("emergency_contact_name")); addField(panel, "EMERGENCY CONTACT NAME", emergencyName);
        EditText emergencyPhone = Ui.input(this, "Emergency contact number"); emergencyPhone.setInputType(InputType.TYPE_CLASS_PHONE); emergencyPhone.setText(profile.optString("emergency_contact_phone")); addField(panel, "EMERGENCY CONTACT NUMBER", emergencyPhone);
        TextView privacyLabel = Ui.eyebrow(this, "PHONE VISIBILITY"); Ui.margin(privacyLabel, 0, 5, 0, 7); panel.addView(privacyLabel);
        Spinner privacy = new Spinner(this);
        String[] values = {"Hidden — alerts only", "Masked — a few digits", "Visible — direct calls"};
        privacy.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        String current = profile.optString("phone_visibility", "hidden"); privacy.setSelection("masked".equals(current) ? 1 : "visible".equals(current) ? 2 : 0);
        privacy.setBackground(Ui.outlined(Ui.WHITE, 11, Ui.LINE, this)); privacy.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0); panel.addView(privacy, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        Button save = Ui.primaryButton(this, "Save profile & privacy"); Ui.margin(save, 0, 22, 0, 0); panel.addView(save);
        page.addView(panel);
        Button logout = Ui.secondaryButton(this, "Sign out of RoadPing"); Ui.margin(logout, 0, 14, 0, 0); page.addView(logout);

        save.setOnClickListener(v -> {
            save.setEnabled(false); save.setText("Saving…");
            executor.execute(() -> {
                try {
                    String visibility = privacy.getSelectedItemPosition() == 1 ? "masked" : privacy.getSelectedItemPosition() == 2 ? "visible" : "hidden";
                    JSONObject valuesJson = new JSONObject().put("full_name", name.getText().toString().trim()).put("phone", phone.getText().toString().trim()).put("emergency_contact_name", emergencyName.getText().toString().trim()).put("emergency_contact_phone", emergencyPhone.getText().toString().trim()).put("phone_visibility", visibility);
                    api.updateProfile(session.accessToken(), session.userId(), valuesJson);
                    profile = api.profile(session.accessToken(), session.userId());
                    runOnUiThread(() -> { toast("Profile saved."); showProfile(); });
                } catch (Exception error) { runOnUiThread(() -> { toast(error.getMessage()); save.setEnabled(true); save.setText("Save profile & privacy"); }); }
            });
        });
        logout.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Sign out?").setMessage("Your vehicles, alerts and trip history remain safely stored.").setNegativeButton("Cancel", null).setPositiveButton("Sign out", (d, w) -> { if (session.trackingEnabled()) startService(new Intent(this, TrackingForegroundService.class).setAction(TrackingForegroundService.ACTION_STOP)); session.clear(); showLogin(false); }).show());
        setPage(page);
    }

    private LinearLayout page() {
        LinearLayout page = Ui.vertical(this, 22);
        page.setPadding(Ui.dp(this, 22), Ui.dp(this, 28), Ui.dp(this, 22), Ui.dp(this, 32));
        return page;
    }

    private void setPage(LinearLayout page) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setVerticalScrollBarEnabled(false);
        FrameLayout column = new FrameLayout(this);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels, Ui.dp(this, 680));
        FrameLayout.LayoutParams centered = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        column.addView(page, centered); scroll.addView(column);
        content.removeAllViews();
        content.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void setRootContent(LinearLayout root) {
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                Insets keyboard = insets.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
                if (bottomNav != null && bottomNav.getParent() == root) bottomNav.setVisibility(insets.isVisible(WindowInsets.Type.ime()) ? View.GONE : View.VISIBLE);
                return insets;
            });
        }
        setContentView(root);
        root.requestApplyInsets();
    }

    private View statCard(String label, String value, String icon, int accent) {
        LinearLayout card = Ui.card(this, 17);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView number = Ui.title(this, value); number.setTextSize(37);
        top.addView(number, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(Ui.icon(this, label.contains("VEHICLE") ? "car" : "bell", accent), new LinearLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22)));
        card.addView(top);
        TextView caption = Ui.text(this, label, 9, Ui.MUTED, true); caption.setLetterSpacing(.08f); Ui.margin(caption, 0, 13, 0, 0); card.addView(caption);
        return card;
    }

    private void sectionHeader(LinearLayout page, String eyebrow, String title, String action, Runnable onAction) {
        LinearLayout section = Ui.vertical(this, 0); Ui.margin(section, 0, 30, 0, 14);
        section.addView(Ui.eyebrow(this, eyebrow));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); Ui.margin(row, 0, 8, 0, 0);
        TextView heading = Ui.title(this, title); heading.setTextSize(25);
        row.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (action != null) { Button button = Ui.secondaryButton(this, "+ Add"); button.setTextSize(12); button.setOnClickListener(v -> onAction.run()); row.addView(button); }
        section.addView(row); page.addView(section);
    }

    private View vehicleCard(Models.Vehicle vehicle) {
        LinearLayout card = Ui.card(this, 0); Ui.margin(card, 0, 0, 0, 18); card.setClipToOutline(true);
        LinearLayout art = Ui.vertical(this, 22);
        int color = vehicleColor(vehicle);
        art.setBackground(new CardSurface(color, Ui.dp(this, 18)));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = Ui.title(this, "ROADPING."); brand.setTextColor(Ui.WHITE); brand.setTextSize(21);
        top.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView state = Ui.text(this, vehicle.active ? "ACTIVE" : "PAUSED", 9, vehicle.active ? Color.rgb(180,255,218) : Color.rgb(240,240,240), true);
        state.setLetterSpacing(.08f); top.addView(state); art.addView(top);
        LinearLayout identity = new LinearLayout(this); identity.setGravity(Gravity.CENTER_VERTICAL); Ui.margin(identity, 0, 29, 0, 18);
        LinearLayout details = Ui.vertical(this, 0);
        TextView label = Ui.text(this, "VEHICLE IDENTITY", 8, Color.rgb(206,206,214), true); label.setLetterSpacing(.14f); details.addView(label);
        TextView plate = Ui.title(this, vehicle.registration); plate.setTextColor(Ui.WHITE); plate.setTextSize(32); Ui.margin(plate, 0, 7, 10, 8); details.addView(plate);
        TextView detail = Ui.text(this, vehicle.description().isEmpty() ? vehicle.type : vehicle.description(), 12, Color.rgb(230,230,236), false); details.addView(detail);
        identity.addView(details, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        try {
            ImageView qr = new ImageView(this); qr.setImageBitmap(qrBitmap(BuildConfig.PUBLIC_APP_URL + "/#/v/" + vehicle.publicToken, 240));
            qr.setContentDescription("QR code for " + vehicle.registration); qr.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4)); qr.setBackground(Ui.background(Ui.WHITE, 10, this));
            identity.addView(qr, new LinearLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 76)));
        } catch (Exception ignored) { }
        art.addView(identity);
        TextView foot = Ui.text(this, "SCAN. CONNECT. CARRY ON.", 8, Color.rgb(215,215,222), true); foot.setLetterSpacing(.14f); art.addView(foot);
        card.addView(art);
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL); actions.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        TextView name = Ui.text(this, vehicle.nickname.isEmpty() ? vehicle.type : vehicle.nickname, 13, Ui.INK, true); name.setMaxLines(2); name.setEllipsize(TextUtils.TruncateAt.END);
        actions.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button qr = Ui.secondaryButton(this, "View details →"); qr.setTextSize(12); Ui.margin(qr, 10, 0, 0, 0); qr.setOnClickListener(v -> showVehicleDetails(vehicle)); actions.addView(qr);
        art.setContentDescription("Open details for " + vehicle.registration); art.setFocusable(true); art.setOnClickListener(v -> showVehicleDetails(vehicle));
        art.setForeground(Ui.interactive(Color.TRANSPARENT, 18, Color.TRANSPARENT, this));
        card.addView(actions); return card;
    }

    private int vehicleColor(Models.Vehicle vehicle) {
        if ("ocean".equals(vehicle.theme)) return Ui.BLUE;
        if ("ember".equals(vehicle.theme)) return Color.rgb(77, 20, 39);
        if ("forest".equals(vehicle.theme)) return Color.rgb(12, 63, 52);
        if ("sand".equals(vehicle.theme)) return Color.rgb(127, 105, 69);
        if ("violet".equals(vehicle.theme)) return Color.rgb(93, 54, 201);
        return Ui.NAVY;
    }

    private void showVehicleDetails(Models.Vehicle vehicle) {
        ScrollView scroll = new ScrollView(this); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout panel = Ui.vertical(this, 22); panel.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 12)); scroll.addView(panel);
        TextView eyebrow = Ui.eyebrow(this, vehicle.active ? "ACTIVE SMART VEHICLE ID" : "CARD PAUSED"); panel.addView(eyebrow);
        TextView plate = Ui.title(this, vehicle.registration); plate.setTextSize(46); Ui.margin(plate, 0, 10, 0, 4); panel.addView(plate);
        TextView nickname = Ui.text(this, vehicle.nickname.isEmpty() ? "MY VEHICLE" : vehicle.nickname, 13, Ui.BLUE, true); panel.addView(nickname);
        LinearLayout facts = Ui.vertical(this, 0); Ui.margin(facts, 0, 24, 0, 20);
        addVehicleFact(facts, "TYPE", vehicle.type);
        addVehicleFact(facts, "MAKE & MODEL", (vehicle.make + " " + vehicle.model).trim().isEmpty() ? "Not added" : (vehicle.make + " " + vehicle.model).trim());
        addVehicleFact(facts, "COLOUR", vehicle.color.isEmpty() ? "Not added" : vehicle.color);
        addVehicleFact(facts, "YEAR", vehicle.year == 0 ? "Not added" : String.valueOf(vehicle.year));
        addVehicleFact(facts, "CARD STYLE", vehicle.theme + " / " + vehicle.cardLayout.replace('_', ' ')); panel.addView(facts);
        Button card = Ui.primaryButton(this, "OPEN LARGE QR CARD"); panel.addView(card);
        Button tracking = Ui.secondaryButton(this, "LIVE LOCATION & JOURNEYS"); Ui.margin(tracking, 0, 10, 0, 0); panel.addView(tracking);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(scroll).setNegativeButton("Close", null).create();
        card.setOnClickListener(v -> { dialog.dismiss(); showQrCard(vehicle); });
        tracking.setOnClickListener(v -> { dialog.dismiss(); currentTab = 2; if (bottomNav != null) buildBottomNav(); showTracking(); });
        dialog.show();
    }

    private void addVehicleFact(LinearLayout container, String label, String value) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13)); row.setBackground(Ui.outlined(Color.TRANSPARENT, 0, Ui.LINE, this));
        TextView key = Ui.text(this, label, 9, Ui.MUTED, true); key.setLetterSpacing(.08f); row.addView(key, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView detail = Ui.text(this, value, 13, Ui.INK, true); detail.setGravity(Gravity.END); row.addView(detail); container.addView(row);
    }

    private View alertRow(Models.Alert alert) {
        boolean fresh = "new".equals(alert.status);
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18));
        row.setBackground(Ui.interactive(fresh ? Color.rgb(240,244,255) : Ui.WHITE, 18, fresh ? Color.rgb(212,222,255) : Ui.LINE, this)); Ui.margin(row, 0, 0, 0, 12);
        boolean urgent = alert.type.contains("accident") || alert.type.equals("emergency");
        ImageView icon = Ui.icon(this, "bell", urgent ? Ui.RED : Ui.BLUE);
        row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24)));
        LinearLayout info = Ui.vertical(this, 0); Ui.margin(info, 14, 0, 10, 0);
        TextView title = Ui.text(this, alert.displayType(), 14, Ui.INK, true); info.addView(title);
        TextView detail = Ui.text(this, alert.registration + (alert.message.isEmpty() ? "" : " / " + alert.message), 14, Ui.MUTED, false); detail.setMaxLines(2); detail.setEllipsize(TextUtils.TruncateAt.END); Ui.margin(detail, 0, 6, 0, 0); info.addView(detail);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (fresh) { TextView badge = Ui.text(this, "NEW", 8, Ui.BLUE, true); badge.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6)); badge.setBackground(Ui.background(Ui.WHITE, 20, this)); row.addView(badge); }
        else row.addView(Ui.icon(this, "arrow", Ui.MUTED), new LinearLayout.LayoutParams(Ui.dp(this, 18), Ui.dp(this, 18)));
        row.setFocusable(true); row.setOnClickListener(v -> showAlertDetails(alert)); return row;
    }

    private View emptyCard(String title, String text) {
        LinearLayout card = Ui.card(this, 28); card.setGravity(Gravity.CENTER);
        ImageView icon = Ui.icon(this, "shield", Ui.BLUE); card.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 35), Ui.dp(this, 35)));
        TextView heading = Ui.title(this, title); heading.setTextSize(25); heading.setGravity(Gravity.CENTER); Ui.margin(heading, 0, 20, 0, 9); card.addView(heading);
        TextView copy = Ui.text(this, text, 13, Ui.MUTED, false); copy.setGravity(Gravity.CENTER); card.addView(copy); return card;
    }

    private void showAddVehicleDialog() {
        LinearLayout form = Ui.vertical(this, 16);
        EditText registration = Ui.input(this, "CAB-2048 or 64-1234"); addField(form, "REGISTRATION / MAIN PLATE NUMBER", registration);
        EditText nickname = Ui.input(this, "Daily ride"); addField(form, "NICKNAME", nickname);
        EditText make = Ui.input(this, "Toyota"); addField(form, "MAKE", make);
        EditText model = Ui.input(this, "Corolla"); addField(form, "MODEL", model);
        EditText color = Ui.input(this, "Pearl white"); addField(form, "COLOUR", color);
        Spinner type = new Spinner(this); String[] types = {"Car", "Motorbike", "Van", "Three-wheeler", "Truck", "Bus", "Other"}; type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types)); type.setBackground(Ui.outlined(Ui.WHITE, 11, Ui.LINE, this)); type.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0); form.addView(Ui.eyebrow(this, "VEHICLE TYPE")); Ui.margin(type, 0, 7, 0, 12); form.addView(type, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Add a vehicle").setView(scroll).setNegativeButton("Cancel", null).setPositiveButton("Create card", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (registration.getText().toString().trim().length() < 2) { registration.setError("Registration is required"); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Creating…");
            executor.execute(() -> {
                try {
                    JSONObject values = new JSONObject().put("registration_number", registration.getText().toString().trim().toUpperCase(Locale.ROOT)).put("nickname", nickname.getText().toString().trim()).put("vehicle_type", type.getSelectedItem().toString()).put("make", make.getText().toString().trim()).put("model", model.getText().toString().trim()).put("color", color.getText().toString().trim()).put("card_theme", "midnight").put("card_layout", "qr_focus");
                    Models.Vehicle created = api.addVehicle(session.accessToken(), session.userId(), values); vehicles.add(0, created);
                    runOnUiThread(() -> { dialog.dismiss(); toast("Smart card created."); showFulfillmentChoice(created); renderCurrent(); });
                } catch (Exception error) { runOnUiThread(() -> { toast(error.getMessage()); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Create card"); }); }
            });
        }));
        dialog.show();
    }

    private void showFulfillmentChoice(Models.Vehicle vehicle) {
        new AlertDialog.Builder(this).setTitle("How should this card be made?")
                .setMessage("Print the high-resolution QR card yourself for free, or let Hich Web print, quality check and carefully deliver it within 2-3 working business days for a one-time LKR 1,990.")
                .setNegativeButton("Decide later", null)
                .setNeutralButton("Print myself", (d, w) -> showQrCard(vehicle))
                .setPositiveButton("Hich print / LKR 1,990", (d, w) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PUBLIC_APP_URL + "/#/app/print/" + vehicle.id)))).show();
    }

    private void showQrCard(Models.Vehicle vehicle) {
        try {
            String url = BuildConfig.PUBLIC_APP_URL + "/#/v/" + vehicle.publicToken;
            ScrollView scroll = new ScrollView(this); scroll.setVerticalScrollBarEnabled(false);
            LinearLayout panel = Ui.vertical(this, 22); scroll.addView(panel);
            LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
            TextView heading = Ui.title(this, "YOUR SMART CARD."); heading.setTextSize(27);
            top.addView(heading, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            ImageView close = Ui.icon(this, "close", Ui.INK); close.setPadding(Ui.dp(this, 13), Ui.dp(this, 13), Ui.dp(this, 13), Ui.dp(this, 13)); close.setBackground(Ui.interactive(Ui.WHITE, 24, Ui.LINE, this)); close.setContentDescription("Close smart card"); close.setFocusable(true);
            top.addView(close, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48))); panel.addView(top);
            LinearLayout card = Ui.vertical(this, 24); card.setGravity(Gravity.CENTER); card.setBackground(new CardSurface(vehicleColor(vehicle), Ui.dp(this, 20))); Ui.margin(card, 0, 18, 0, 18);
            TextView brand = Ui.title(this, "ROADPING."); brand.setTextColor(Ui.WHITE); brand.setTextSize(24); card.addView(brand);
            TextView identity = Ui.text(this, "VEHICLE SMART CARD / BY HICH", 8, Color.rgb(212,212,221), true); identity.setLetterSpacing(.12f); Ui.margin(identity, 0, 7, 0, 20); card.addView(identity);
            TextView plate = Ui.title(this, vehicle.registration); plate.setTextColor(Ui.WHITE); plate.setTextSize(36); plate.setGravity(Gravity.CENTER); card.addView(plate);
            TextView details = Ui.text(this, vehicle.description().isEmpty() ? vehicle.type : vehicle.description(), 12, Color.rgb(220,220,230), false); details.setGravity(Gravity.CENTER); Ui.margin(details, 0, 8, 0, 22); card.addView(details);
            int qrSize = Math.min(Ui.dp(this, 310), getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 80));
            ImageView qr = new ImageView(this); qr.setImageBitmap(qrBitmap(url, 720)); qr.setContentDescription("Scan to contact the owner of " + vehicle.registration);
            qr.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8)); qr.setBackground(Ui.background(Ui.WHITE, 14, this)); card.addView(qr, new LinearLayout.LayoutParams(qrSize, qrSize));
            TextView line = Ui.text(this, vehicle.active ? "SCAN TO CONNECT." : "CARD CURRENTLY PAUSED", 10, vehicle.active ? Ui.WHITE : Ui.PINK, true); line.setLetterSpacing(.1f); line.setGravity(Gravity.CENTER); Ui.margin(line, 0, 20, 0, 0); card.addView(line);
            TextView privacy = Ui.text(this, "No app needed. Your number stays private.", 10, Color.rgb(209,209,218), false); privacy.setGravity(Gravity.CENTER); Ui.margin(privacy, 0, 8, 0, 5); card.addView(privacy); panel.addView(card);
            Button share = Ui.primaryButton(this, "Share vehicle link  ->"); panel.addView(share);
            Button open = Ui.secondaryButton(this, "Preview scan page"); Ui.margin(open, 0, 10, 0, 0); panel.addView(open);
            Button print = Ui.secondaryButton(this, "Get a printed card"); Ui.margin(print, 0, 10, 0, 0); panel.addView(print);
            AlertDialog dialog = new AlertDialog.Builder(this).setView(scroll).create();
            close.setOnClickListener(v -> dialog.dismiss());
            share.setOnClickListener(v -> { Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "RoadPing card for " + vehicle.registration + "\n" + url); startActivity(Intent.createChooser(intent, "Share vehicle card")); });
            open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
            print.setOnClickListener(v -> { dialog.dismiss(); showFulfillmentChoice(vehicle); });
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(Ui.background(Ui.SURFACE, 28, this));
                window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 24), Ui.dp(this, 480)), Math.min((int) (getResources().getDisplayMetrics().heightPixels * .88f), Ui.dp(this, 800)));
            }
        } catch (Exception error) { toast("Could not generate QR code."); }
    }

    private Bitmap qrBitmap(String value, int size) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++) for (int y = 0; y < size; y++) bitmap.setPixel(x, y, matrix.get(x, y) ? Ui.NAVY : Ui.WHITE);
        return bitmap;
    }

    private void showAlertDetails(Models.Alert alert) {
        String message = alert.message.isEmpty() ? "No additional details were included." : alert.message;
        new AlertDialog.Builder(this).setTitle(alert.displayType() + " · " + alert.registration).setMessage(message + (alert.reporterContact.isEmpty() ? "" : "\n\nReporter contact: " + alert.reporterContact)).setNegativeButton("Close", null).setPositiveButton("Mark resolved", (d, w) -> executor.execute(() -> {
            try { api.updateAlertStatus(session.accessToken(), alert.id, "resolved"); alert.status = "resolved"; runOnUiThread(() -> { toast("Alert resolved."); renderCurrent(); }); }
            catch (Exception error) { runOnUiThread(() -> toast(error.getMessage())); }
        })).show();
        if ("new".equals(alert.status)) executor.execute(() -> { try { api.updateAlertStatus(session.accessToken(), alert.id, "seen"); alert.status = "seen"; } catch (Exception ignored) {} });
    }

    private void addField(LinearLayout container, String label, EditText input) {
        TextView caption = Ui.eyebrow(this, label); Ui.margin(caption, 0, 0, 0, 7); container.addView(caption); Ui.margin(input, 0, 0, 0, 20); container.addView(input);
    }

    private void scheduleAlertChecks() {
        JobInfo job = new JobInfo.Builder(1042, new ComponentName(this, AlertPollingJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000L)
                .build();
        getSystemService(JobScheduler.class).schedule(job);
    }

    private void toast(String message) { Toast.makeText(this, message == null || message.isEmpty() ? "Something went wrong." : message, Toast.LENGTH_LONG).show(); }
}
