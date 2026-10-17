package com.gymbuddy.app;

import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The theme and a few view factories. No XML layouts: every screen is built in
 * code from these, with the sizes, colours and weights of the React Native
 * styles they replace.
 */
final class Ui {

    // src/constants/colors.ts
    static final int PRIMARY = 0xFF3DC2EC;
    static final int SUCCESS = 0xFF3DC2EC;
    static final int DANGER = 0xFFEF4444;
    static final int WARNING = 0xFFFBBF24;
    static final int BACKGROUND = 0xFF0A0A0A;
    static final int SURFACE = 0xFF161616;
    static final int TEXT = 0xFFF5F5F5;
    static final int TEXT_SECONDARY = 0xFF8A8A8A;
    static final int TEXT_LIGHT = 0xFF4A4A4A;
    static final int BORDER = 0xFF252525;

    // Colors.primary + '15' and friends: React Native reads #RRGGBBAA.
    static final int PRIMARY_15 = 0x153DC2EC;
    static final int PRIMARY_20 = 0x203DC2EC;
    static final int PRIMARY_30 = 0x303DC2EC;
    static final int WARNING_15 = 0x15FBBF24;
    static final int SCRIM = 0xB3000000;   // rgba(0,0,0,0.7)

    static final int MATCH = LinearLayout.LayoutParams.MATCH_PARENT;
    static final int WRAP = LinearLayout.LayoutParams.WRAP_CONTENT;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    // ------------------------------------------------------------------ text

    static Typeface weight(int w) {
        return Typeface.create(Typeface.DEFAULT, w, false);
    }

    static TextView text(Context c, String s, float sizeSp, int color, int weight) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        t.setTypeface(weight(weight));
        return t;
    }

    /** React Native's letterSpacing is in points; Android wants ems of the font size. */
    static TextView spaced(TextView t, float points, float sizeSp) {
        t.setLetterSpacing(points / sizeSp);
        return t;
    }

    static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------- drawables

    static GradientDrawable box(Context c, int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    static GradientDrawable box(Context c, int fill, float radiusDp, int border) {
        GradientDrawable g = box(c, fill, radiusDp);
        g.setStroke(dp(c, 1), border);
        return g;
    }

    /** The common card: surface, 1dp border, rounded. */
    static GradientDrawable card(Context c, float radiusDp) {
        return box(c, SURFACE, radiusDp, BORDER);
    }

    /**
     * A card whose left edge is drawn thicker and in another colour, the way
     * React Native draws borderLeftWidth on a rounded view.
     */
    static final class LeftAccent extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radius, width, left;
        private final int fill, border, accent;
        private final RectF r = new RectF();
        private final Path clip = new Path();

        LeftAccent(Context c, int fill, int border, int accent, float radiusDp, float leftDp) {
            this.fill = fill;
            this.border = border;
            this.accent = accent;
            this.radius = dp(c, radiusDp);
            this.width = dp(c, 1);
            this.left = dp(c, leftDp);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            r.set(b);
            paint.setColor(border);
            canvas.drawRoundRect(r, radius, radius, paint);
            canvas.save();
            canvas.clipRect(b.left, b.top, b.left + radius, b.bottom);
            paint.setColor(accent);
            canvas.drawRoundRect(r, radius, radius, paint);
            canvas.restore();
            r.set(b.left + left, b.top + width, b.right - width, b.bottom - width);
            float inner = Math.max(0, radius - width);
            paint.setColor(fill);
            canvas.drawRoundRect(r, inner, inner, paint);
        }

        @Override
        public void getOutline(android.graphics.Outline outline) {
            outline.setRoundRect(getBounds(), radius);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    // ----------------------------------------------------------------- icons

    static ImageView icon(Context c, int res, float sizeDp, int color) {
        ImageView v = new ImageView(c);
        setIcon(v, res, color);
        int s = dp(c, sizeDp);
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }

    static void setIcon(ImageView v, int res, int color) {
        Drawable d = v.getContext().getDrawable(res).mutate();
        d.setTint(color);
        v.setImageDrawable(d);
    }

    // ------------------------------------------------------------- touchable

    /**
     * TouchableOpacity: fades to {@code activeOpacity} while held. A disabled
     * view sits at {@code disabledAlpha}.
     */
    static <T extends View> T touch(T v, float activeOpacity, View.OnClickListener click) {
        return touch(v, activeOpacity, 1f, click);
    }

    static <T extends View> T touch(T v, float activeOpacity, float disabledAlpha, View.OnClickListener click) {
        StateListAnimator s = new StateListAnimator();
        s.addState(new int[]{-android.R.attr.state_enabled},
                ObjectAnimator.ofFloat(v, View.ALPHA, disabledAlpha).setDuration(0));
        s.addState(new int[]{android.R.attr.state_pressed},
                ObjectAnimator.ofFloat(v, View.ALPHA, activeOpacity).setDuration(60));
        s.addState(new int[0], ObjectAnimator.ofFloat(v, View.ALPHA, 1f).setDuration(150));
        v.setStateListAnimator(s);
        v.setClickable(true);
        if (click != null) v.setOnClickListener(click);
        return v;
    }

    static <T extends View> T longPress(T v, final Runnable r) {
        v.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View x) {
                r.run();
                return true;
            }
        });
        return v;
    }

    // ---------------------------------------------------------------- layout

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    /** React Native's gap: an invisible divider between the children. */
    static LinearLayout gap(LinearLayout l, float dp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0);
        int px = dp(l.getContext(), dp);
        g.setSize(px, px);
        l.setDividerDrawable(g);
        l.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    static LinearLayout.LayoutParams margins(Context c, int w, int h, float l, float t, float r, float b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    static void pad(View v, float l, float t, float r, float b) {
        Context c = v.getContext();
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    static View space(Context c, float wDp, float hDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, wDp), dp(c, hDp)));
        return v;
    }

    // ---------------------------------------------------------------- inputs

    /** A TextInput: own border and padding, no underline, React Native's defaults. */
    static EditText input(Context c, String placeholder, float sizeSp, float padDp, float radiusDp,
                          int fill, boolean multiline) {
        EditText e = new EditText(c);
        e.setBackground(box(c, fill, radiusDp, BORDER));
        int p = dp(c, padDp);
        e.setPadding(p, p, p, p);
        e.setMinHeight(0);
        e.setMinimumHeight(0);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        e.setTextColor(TEXT);
        e.setHint(placeholder);
        e.setHintTextColor(TEXT_LIGHT);
        e.setTypeface(weight(400));
        int type = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;
        if (multiline) {
            e.setInputType(type | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            e.setSingleLine(false);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setInputType(type);
            e.setSingleLine(true);
            e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
        return e;
    }

    static void showKeyboard(final View v) {
        v.requestFocus();
        v.postDelayed(new Runnable() {
            public void run() {
                InputMethodManager imm = v.getContext().getSystemService(InputMethodManager.class);
                if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 150);
    }

    static void hideKeyboard(Activity a) {
        View f = a.getCurrentFocus();
        InputMethodManager imm = a.getSystemService(InputMethodManager.class);
        if (f != null && imm != null) imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
    }

    // ------------------------------------------------------------- pieces

    /** The round-cornered + button pinned bottom right of a list. */
    static View fab(Context c, View.OnClickListener click) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(box(c, PRIMARY, 16));
        f.setElevation(dp(c, 4));
        f.setOutlineAmbientShadowColor(PRIMARY);
        f.setOutlineSpotShadowColor(PRIMARY);
        ImageView plus = icon(c, R.drawable.ic_add, 28, BACKGROUND);
        f.addView(plus, new FrameLayout.LayoutParams(dp(c, 28), dp(c, 28), Gravity.CENTER));
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(c, 56), dp(c, 56),
                Gravity.BOTTOM | Gravity.END);
        p.setMargins(0, 0, dp(c, 24), dp(c, 28));
        f.setLayoutParams(p);
        f.setContentDescription("Add");
        return touch(f, 0.8f, click);
    }

    /** The 44dp (or 40dp) tinted square with an icon in it, at the start of a card. */
    static View iconBox(Context c, int res, float boxDp, float radiusDp, float iconDp) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(box(c, PRIMARY_15, radiusDp));
        ImageView i = icon(c, res, iconDp, PRIMARY);
        f.addView(i, new FrameLayout.LayoutParams(dp(c, iconDp), dp(c, iconDp), Gravity.CENTER));
        f.setLayoutParams(lp(dp(c, boxDp), dp(c, boxDp)));
        return f;
    }

    /** A tag pill under an exercise name. */
    static TextView pill(Context c, String tag) {
        TextView t = text(c, capitalize(tag), 11, PRIMARY, 400);
        t.setBackground(box(c, PRIMARY_15, 10));
        pad(t, 8, 2, 8, 2);
        return t;
    }

    /** A filter or form chip that toggles. */
    static TextView chip(Context c, String label, float sizeSp, boolean on) {
        TextView t = text(c, label, sizeSp, TEXT_SECONDARY, 400);
        t.setGravity(Gravity.CENTER);
        setChip(t, on);
        return t;
    }

    static void setChip(TextView t, boolean on) {
        Context c = t.getContext();
        t.setBackground(on ? box(c, PRIMARY_20, 20, PRIMARY) : box(c, SURFACE, 20, BORDER));
        t.setTextColor(on ? PRIMARY : TEXT_SECONDARY);
        t.setTypeface(weight(on ? 600 : 400));
    }
}
