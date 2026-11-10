package com.roadping.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.Locale;

/** A distraction-free location surface for an in-vehicle display. */
public class PlayerMapView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path secondaryRoad = new Path();
    private final Path mainRoad = new Path();
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private float bearing;

    public PlayerMapView(Context context) {
        super(context);
        setBackgroundColor(Color.rgb(18, 20, 24));
        setContentDescription("Current vehicle location map");
    }

    public void setLocation(double latitude, double longitude, float bearing) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.bearing = bearing;
        setContentDescription(Double.isNaN(latitude) ? "Waiting for current location" : String.format(Locale.ROOT, "Current location %.5f, %.5f", latitude, longitude));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float unit = getResources().getDisplayMetrics().density;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(unit);
        paint.setColor(Color.rgb(39, 42, 48));
        float grid = 48 * unit;
        for (float x = 0; x < width; x += grid) canvas.drawLine(x, 0, x, height, paint);
        for (float y = 0; y < height; y += grid) canvas.drawLine(0, y, width, y, paint);

        secondaryRoad.reset();
        secondaryRoad.moveTo(-20, height * .75f);
        secondaryRoad.cubicTo(width * .24f, height * .52f, width * .40f, height * .82f, width * 1.1f, height * .27f);
        paint.setStrokeWidth(12 * unit);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(54, 58, 65));
        canvas.drawPath(secondaryRoad, paint);

        mainRoad.reset();
        mainRoad.moveTo(width * .08f, height * 1.05f);
        mainRoad.cubicTo(width * .20f, height * .70f, width * .54f, height * .76f, width * .88f, -20);
        paint.setStrokeWidth(22 * unit);
        paint.setColor(Color.rgb(75, 80, 90));
        canvas.drawPath(mainRoad, paint);
        paint.setStrokeWidth(2 * unit);
        paint.setColor(Color.rgb(161, 168, 181));
        canvas.drawPath(mainRoad, paint);

        float cx = width * .55f;
        float cy = height * .53f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(42, 255, 52, 84));
        canvas.drawCircle(cx, cy, 30 * unit, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, 11 * unit, paint);
        paint.setColor(Ui.PINK);
        canvas.drawCircle(cx, cy, 7 * unit, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(12 * unit);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        String coordinate = Double.isNaN(latitude) ? "WAITING FOR GPS" : String.format(Locale.ROOT, "%.5f  %.5f", latitude, longitude);
        canvas.drawText(coordinate, 18 * unit, height - 22 * unit, paint);
        paint.setTextSize(10 * unit);
        paint.setColor(Color.rgb(170, 174, 184));
        canvas.drawText(String.format(Locale.ROOT, "HEADING %.0f°", bearing), 18 * unit, height - 7 * unit, paint);
    }
}
