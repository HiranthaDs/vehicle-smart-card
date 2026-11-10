package com.roadping.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Quiet, resolution-independent artwork for the vehicle's digital identity. */
final class CardSurface extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int color;
    private final float radius;

    CardSurface(int color, float radius) { this.color = color; this.radius = radius; }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        RectF area = new RectF(bounds);
        Path clip = new Path();
        clip.addRoundRect(area, radius, radius, Path.Direction.CW);
        int saved = canvas.save();
        canvas.clipPath(clip);
        paint.setStyle(Paint.Style.FILL);
        int light = Color.rgb(Math.min(255, Color.red(color) + 18), Math.min(255, Color.green(color) + 18), Math.min(255, Color.blue(color) + 18));
        paint.setShader(new LinearGradient(0, 0, bounds.width(), bounds.height(), light, color, Shader.TileMode.CLAMP));
        canvas.drawRect(area, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1, bounds.width() / 600f));
        paint.setColor(Color.argb(25, 255, 255, 255));
        float step = bounds.width() / 10f;
        for (int i = 1; i < 12; i++) canvas.drawCircle(bounds.right - bounds.width() * .07f, bounds.top + bounds.height() * .2f, step * i, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.PINK);
        canvas.drawRect(bounds.left, bounds.bottom - Math.max(3, bounds.height() * .018f), bounds.right, bounds.bottom, paint);
        canvas.restoreToCount(saved);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
