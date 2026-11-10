package com.roadping.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import java.util.Locale;

public final class Ui {
    public static final int NAVY = Color.rgb(8, 8, 8);
    public static final int NAVY_2 = Color.rgb(17, 17, 17);
    public static final int BLUE = Color.rgb(16, 67, 249);
    public static final int PINK = Color.rgb(255, 52, 84);
    public static final int TEAL = Color.rgb(24, 203, 150);
    public static final int TEAL_DARK = Color.rgb(13, 156, 115);
    public static final int LIME = TEAL;
    public static final int INK = NAVY;
    public static final int MUTED = Color.rgb(104, 104, 104);
    public static final int LINE = Color.rgb(222, 222, 219);
    public static final int SURFACE = Color.rgb(245, 245, 243);
    public static final int WHITE = Color.WHITE;
    public static final int RED = Color.rgb(222, 75, 89);

    private Ui() {}

    public static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }

    public static GradientDrawable background(int color, float radiusDp, Context c) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(c, radiusDp));
        return drawable;
    }

    public static GradientDrawable outlined(int color, float radiusDp, int strokeColor, Context c) {
        GradientDrawable drawable = background(color, radiusDp, c);
        drawable.setStroke(dp(c, 1), strokeColor);
        return drawable;
    }

    public static LinearLayout vertical(Context c, int paddingDp) {
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(c, paddingDp), dp(c, paddingDp), dp(c, paddingDp), dp(c, paddingDp));
        return layout;
    }

    public static TextView text(Context c, String value, float size, int color, boolean bold) {
        TextView view = new TextView(c);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        try { view.setTypeface(c.getResources().getFont(bold ? R.font.inter_semibold : R.font.inter_regular)); }
        catch (Exception ignored) { if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); }
        view.setLineSpacing(dp(c, 1), 1.18f);
        return view;
    }

    public static TextView title(Context c, String value) {
        TextView view = text(c, value.toUpperCase(Locale.ROOT), 38, INK, true);
        try { view.setTypeface(c.getResources().getFont(R.font.anton)); } catch (Exception ignored) {}
        view.setLetterSpacing(-.025f);
        view.setLineSpacing(0, 1.04f);
        return view;
    }
    public static TextView eyebrow(Context c, String value) {
        TextView view = text(c, value.toUpperCase(Locale.ROOT), 10, MUTED, true);
        view.setLetterSpacing(.12f);
        return view;
    }

    public static Button primaryButton(Context c, String label) {
        Button button = new Button(c);
        button.setText(label);
        button.setTextColor(new ColorStateList(new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}}, new int[] {MUTED, INK}));
        button.setTextSize(13);
        button.setTypeface(c.getResources().getFont(R.font.inter_semibold));
        button.setAllCaps(false);
        button.setLetterSpacing(-.01f);
        button.setGravity(Gravity.CENTER);
        button.setBackground(interactive(PINK, 28, PINK, c));
        button.setStateListAnimator(null);
        button.setMinHeight(dp(c, 52));
        button.setMinimumHeight(dp(c, 52));
        button.setPadding(dp(c, 20), dp(c, 12), dp(c, 20), dp(c, 12));
        return button;
    }

    public static Button secondaryButton(Context c, String label) {
        Button button = primaryButton(c, label);
        button.setTextColor(INK);
        button.setBackground(interactive(WHITE, 28, LINE, c));
        return button;
    }

    public static EditText input(Context c, String hint) {
        EditText input = new EditText(c);
        input.setHint(hint);
        input.setHintTextColor(MUTED);
        input.setTextColor(INK);
        input.setTextSize(16);
        input.setTypeface(c.getResources().getFont(R.font.inter_regular));
        input.setSingleLine(true);
        input.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[] {android.R.attr.state_focused}, outlined(WHITE, 12, BLUE, c));
        background.addState(new int[] {}, outlined(WHITE, 12, LINE, c));
        input.setBackground(background);
        input.setMinHeight(dp(c, 56));
        input.setSelectAllOnFocus(false);
        return input;
    }

    public static EditText passwordInput(Context c, String hint) {
        EditText input = input(c, hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return input;
    }

    public static void margin(View view, int left, int top, int right, int bottom) {
        ViewGroup.MarginLayoutParams params;
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        else params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(view.getContext(), left), dp(view.getContext(), top), dp(view.getContext(), right), dp(view.getContext(), bottom));
        view.setLayoutParams(params);
    }

    public static LinearLayout card(Context c, int padding) {
        LinearLayout card = vertical(c, padding);
        card.setBackground(outlined(WHITE, 20, LINE, c));
        return card;
    }

    public static Drawable interactive(int color, float radius, int stroke, Context c) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(26, 8, 8, 8)), outlined(color, radius, stroke, c), background(WHITE, radius, c));
    }

    public static ImageView icon(Context c, String name, int color) {
        ImageView view = new ImageView(c);
        view.setImageDrawable(new LineIcon(name, color));
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private static final class LineIcon extends Drawable {
        private final String name;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LineIcon(String name, int color) { this.name = name; paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.7f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); }
        @Override public void draw(Canvas canvas) {
            int save = canvas.save();
            canvas.translate(getBounds().left, getBounds().top);
            canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
            Path path = new Path();
            switch (name) {
                case "home":
                    path.moveTo(3, 10); path.lineTo(12, 3); path.lineTo(21, 10); path.moveTo(5, 9); path.lineTo(5, 21); path.lineTo(10, 21); path.lineTo(10, 14); path.lineTo(14, 14); path.lineTo(14, 21); path.lineTo(19, 21); path.lineTo(19, 9); break;
                case "car":
                    canvas.drawRoundRect(new RectF(3, 10, 21, 18), 2, 2, paint); path.moveTo(5, 10); path.lineTo(7, 5); path.lineTo(17, 5); path.lineTo(19, 10); path.moveTo(6, 18); path.lineTo(6, 21); path.moveTo(18, 18); path.lineTo(18, 21); path.moveTo(6, 14); path.lineTo(8, 14); path.moveTo(16, 14); path.lineTo(18, 14); break;
                case "location":
                    path.moveTo(12, 22); path.cubicTo(8, 17, 4, 13, 4, 9); path.cubicTo(4, -1, 20, -1, 20, 9); path.cubicTo(20, 13, 16, 17, 12, 22); canvas.drawCircle(12, 9, 2.5f, paint); break;
                case "bell":
                    path.moveTo(4, 17); path.lineTo(6, 14); path.lineTo(6, 9); path.cubicTo(6, 1, 18, 1, 18, 9); path.lineTo(18, 14); path.lineTo(20, 17); path.close(); canvas.drawArc(new RectF(9, 17, 15, 22), 0, 180, false, paint); break;
                case "person":
                    canvas.drawCircle(12, 7, 4, paint); path.moveTo(4, 21); path.lineTo(4, 19); path.cubicTo(4, 11, 20, 11, 20, 19); path.lineTo(20, 21); break;
                case "shield":
                    path.moveTo(12, 2); path.lineTo(21, 6); path.lineTo(20, 14); path.quadTo(18, 20, 12, 23); path.quadTo(6, 20, 4, 14); path.lineTo(3, 6); path.close(); path.moveTo(8, 12); path.lineTo(11, 15); path.lineTo(16, 9); break;
                case "arrow": path.moveTo(5, 12); path.lineTo(19, 12); path.moveTo(13, 6); path.lineTo(19, 12); path.lineTo(13, 18); break;
                case "close": path.moveTo(6, 6); path.lineTo(18, 18); path.moveTo(18, 6); path.lineTo(6, 18); break;
                default: canvas.drawCircle(12, 12, 8, paint); canvas.drawCircle(12, 12, 3, paint);
            }
            canvas.drawPath(path, paint); canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
