package com.gymbuddy.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One screen of the old navigation stack: the header with its back arrow and
 * title, the content under it, an optional bar pinned to the bottom, and the
 * dimmed overlay the add-forms open in. load() runs every time the screen comes
 * back into view, like useFocusEffect did.
 */
abstract class Screen extends Activity {

    Db db;
    FrameLayout root;
    LinearLayout page;
    /** Where a screen puts its views. Overlays cover this, not the header. */
    FrameLayout content;
    TextView headerTitle;

    private LinearLayout header, headerBar;
    private View topSpacer;
    private View bottomBar;
    private int bottomBarPad;
    private boolean bottomBarFloating;
    private View overlay;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = Db.get(this);
        getWindow().setDecorFitsSystemWindows(false);
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BACKGROUND);
        page = Ui.column(this);
        root.addView(page, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        content = new FrameLayout(this);
        page.addView(content, Ui.lp(Ui.MATCH, 0, 1f));
        setContentView(root);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                applyInsets(insets);
                return insets;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    /** Reads the database and fills the screen. */
    abstract void load();

    // ---------------------------------------------------------------- header

    /** The stack header: #0A0A0A, white bold title, back arrow unless hidden. */
    void header(String title, boolean back) {
        header = Ui.column(this);
        header.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout bar = Ui.row(this);
        if (back) {
            FrameLayout b = new FrameLayout(this);
            b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), null, null));
            b.addView(Ui.icon(this, R.drawable.ic_back, 24, Ui.TEXT),
                    new FrameLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24), Gravity.CENTER));
            b.setContentDescription("Navigate up");
            b.setClickable(true);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            bar.addView(b, Ui.lp(Ui.dp(this, 56), Ui.dp(this, 56)));
        }
        headerTitle = Ui.text(this, title, 20, Ui.TEXT, 700);
        headerTitle.setSingleLine(true);
        headerTitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tp = Ui.lp(0, Ui.WRAP, 1f);
        tp.setMarginStart(Ui.dp(this, 16));
        tp.setMarginEnd(Ui.dp(this, 16));
        bar.addView(headerTitle, tp);
        header.addView(bar, Ui.lp(Ui.MATCH, Ui.dp(this, 56)));
        page.addView(header, 0, Ui.lp(Ui.MATCH, Ui.WRAP));
        headerBar = bar;
    }

    /** Puts a button at the right end of the header. */
    void headerAction(View button) {
        LinearLayout.LayoutParams p = Ui.lp(Ui.WRAP, Ui.WRAP);
        p.setMarginEnd(Ui.dp(this, 16));
        headerBar.addView(button, p);
    }

    void title(String t) {
        if (headerTitle != null) headerTitle.setText(t);
    }

    /** Home has no header, just a 50dp gap at the top. */
    View topSpacer() {
        topSpacer = new View(this);
        topSpacer.setLayoutParams(Ui.lp(Ui.MATCH, Ui.dp(this, 50)));
        return topSpacer;
    }

    /**
     * A full-width bar along the bottom. It runs under the navigation bar,
     * padded {@code padDp} below its content. A floating bar (Finish Workout,
     * Back to Gym) sits over the scrolling content, the way the absolutely
     * positioned buttons did; otherwise it goes under it. A floating bar steps
     * aside while the keyboard is up: the field being typed in scrolls to just
     * above the keyboard, which is exactly where the bar would cover it.
     */
    void bottomBar(View bar, int padDp, boolean floating) {
        bottomBar = bar;
        bottomBarPad = padDp;
        bottomBarFloating = floating;
        if (floating) content.addView(bar, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM));
        else page.addView(bar, Ui.lp(Ui.MATCH, Ui.WRAP));
    }

    private void applyInsets(WindowInsets insets) {
        Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
        if (header != null) header.setPadding(0, bars.top, 0, 0);
        if (topSpacer != null) {
            topSpacer.getLayoutParams().height = Math.max(Ui.dp(this, 50), bars.top);
            topSpacer.requestLayout();
        }
        boolean keyboard = ime > bars.bottom;
        int bottom;
        if (bottomBar != null) {
            bottom = keyboard ? ime : 0;
            int pad = Ui.dp(this, bottomBarPad);
            bottomBar.setPadding(bottomBar.getPaddingLeft(), bottomBar.getPaddingTop(),
                    bottomBar.getPaddingRight(), keyboard ? pad : Math.max(pad, bars.bottom + Ui.dp(this, 10)));
            if (bottomBarFloating) bottomBar.setVisibility(keyboard ? View.GONE : View.VISIBLE);
        } else {
            bottom = Math.max(bars.bottom, ime);
        }
        page.setPadding(bars.left, 0, bars.right, bottom);
    }

    // --------------------------------------------------------------- overlay

    /** The dimmed full-screen layer an add-form sits in, centred. */
    void showOverlay(View form) {
        hideOverlay();
        FrameLayout o = new FrameLayout(this);
        o.setBackgroundColor(Ui.SCRIM);
        o.setClickable(true);   // taps outside the form do nothing, as before
        int p = Ui.dp(this, 24);
        o.setPadding(p, p, p, p);
        o.addView(form, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.CENTER));
        content.addView(o, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        overlay = o;
    }

    void hideOverlay() {
        if (overlay == null) return;
        Ui.hideKeyboard(this);
        content.removeView(overlay);
        overlay = null;
    }

    boolean overlayShown() {
        return overlay != null;
    }

    @Override
    public void onBackPressed() {
        if (overlay != null) {
            hideOverlay();
            return;
        }
        super.onBackPressed();
    }

    /** The form card inside an overlay: title, then whatever goes in it. */
    LinearLayout formCard(String title) {
        LinearLayout f = Ui.column(this);
        f.setBackground(Ui.card(this, 20));
        Ui.pad(f, 24, 24, 24, 24);
        TextView t = Ui.text(this, title, 20, Ui.TEXT, 700);
        f.addView(t, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 0, 0, 16));
        return f;
    }

    /** Cancel / primary buttons at the bottom right of a form. */
    LinearLayout formButtons(String primary, View.OnClickListener cancel, View.OnClickListener save) {
        LinearLayout r = Ui.row(this);
        r.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Ui.gap(r, 12);
        TextView c = Ui.text(this, "Cancel", 16, Ui.TEXT_SECONDARY, 400);
        Ui.pad(c, 20, 12, 20, 12);
        c.setClickable(true);
        c.setOnClickListener(cancel);
        TextView s = Ui.text(this, primary, 16, Ui.BACKGROUND, 700);
        s.setBackground(Ui.box(this, Ui.PRIMARY, 10));
        Ui.pad(s, 24, 12, 24, 12);
        s.setClickable(true);
        s.setOnClickListener(save);
        r.addView(c);
        r.addView(s);
        r.setLayoutParams(Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 8, 0, 0));
        return r;
    }

    // ---------------------------------------------------------------- alerts

    /**
     * React Native's Alert.alert on Android: one button is the positive one;
     * two are negative, positive; three are neutral, negative, positive. Not
     * cancelable by tapping outside.
     */
    AlertDialog alert(String title, String message, String[] labels, final Runnable... actions) {
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        AlertDialog.Builder b = new AlertDialog.Builder(this, night
                ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert);
        b.setTitle(title);
        if (message != null) b.setMessage(message);
        b.setCancelable(false);
        int[] which = labels.length == 1 ? new int[]{DialogInterface.BUTTON_POSITIVE}
                : labels.length == 2 ? new int[]{DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_POSITIVE}
                : new int[]{DialogInterface.BUTTON_NEUTRAL, DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_POSITIVE};
        for (int i = 0; i < labels.length && i < 3; i++) {
            final Runnable action = i < actions.length ? actions[i] : null;
            DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    if (action != null) action.run();
                }
            };
            if (which[i] == DialogInterface.BUTTON_POSITIVE) b.setPositiveButton(labels[i], l);
            else if (which[i] == DialogInterface.BUTTON_NEGATIVE) b.setNegativeButton(labels[i], l);
            else b.setNeutralButton(labels[i], l);
        }
        final AlertDialog d = b.create();
        final int tint = night ? Ui.PRIMARY : 0xFF0F8CB4;
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            public void onShow(DialogInterface x) {
                for (int w : new int[]{DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE,
                        DialogInterface.BUTTON_NEUTRAL}) {
                    Button btn = d.getButton(w);
                    if (btn != null) btn.setTextColor(tint);
                }
            }
        });
        d.show();
        return d;
    }

    // ------------------------------------------------------------ navigation

    void open(Class<?> screen, Object... extras) {
        startActivity(intent(screen, extras));
    }

    Intent intent(Class<?> screen, Object... extras) {
        Intent i = new Intent(this, screen);
        for (int k = 0; k + 1 < extras.length; k += 2) {
            Object v = extras[k + 1];
            if (v instanceof Long) i.putExtra((String) extras[k], (Long) v);
            else if (v != null) i.putExtra((String) extras[k], v.toString());
        }
        return i;
    }

    /** router.replace: the new screen takes this one's place in the stack. */
    void replace(Class<?> screen, Object... extras) {
        startActivity(intent(screen, extras));
        finish();
    }

    /** router.dismissTo('/gym/id'): back down the stack to that gym. */
    void backToGym(long gymId) {
        Intent i = intent(GymActivity.class, "gymId", gymId);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    long longExtra(String key) {
        return getIntent().getLongExtra(key, 0);
    }

    // ---------------------------------------------------------------- pieces

    /** The full-width primary bar with an optional icon: Finish Workout, Back to Gym. */
    LinearLayout primaryBar(int icon, String label, View.OnClickListener click) {
        LinearLayout b = Ui.row(this);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundColor(Ui.PRIMARY);
        Ui.gap(b, 8);
        Ui.pad(b, 0, 18, 0, 34);
        if (icon != 0) b.addView(Ui.icon(this, icon, 20, Ui.BACKGROUND));
        b.addView(Ui.text(this, label, 18, Ui.BACKGROUND, 700));
        return Ui.touch(b, 0.8f, click);
    }

    /** The small rounded button with an icon and an optional label: Home's Exercises, the workout's Reorder. */
    LinearLayout pill(int icon, String label) {
        LinearLayout b = Ui.gap(Ui.row(this), 6);
        b.setBackground(Ui.box(this, Ui.SURFACE, 20, Ui.BORDER));
        Ui.pad(b, 14, 8, 14, 8);
        b.addView(Ui.icon(this, icon, 18, Ui.PRIMARY));
        if (label != null) b.addView(Ui.text(this, label, 13, Ui.PRIMARY, 600));
        return b;
    }

    /** The grey empty-state block: icon, line, smaller line. */
    LinearLayout empty(int icon, String line, String sub, float lineMarginTop) {
        LinearLayout e = Ui.column(this);
        e.setGravity(Gravity.CENTER_HORIZONTAL);
        if (icon != 0) e.addView(Ui.icon(this, icon, 48, Ui.TEXT_LIGHT));
        TextView l = Ui.text(this, line, 16, Ui.TEXT_SECONDARY, 600);
        l.setGravity(Gravity.CENTER);
        e.addView(l, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, lineMarginTop, 0, 0));
        if (sub != null) {
            TextView s = Ui.text(this, sub, 13, Ui.TEXT_LIGHT, 400);
            s.setGravity(Gravity.CENTER);
            e.addView(s, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 4, 0, 0));
        }
        return e;
    }

    ImageView iconButton(int icon, float sizeDp, int color, float activeOpacity, View.OnClickListener click) {
        ImageView i = Ui.icon(this, icon, sizeDp, color);
        return Ui.touch(i, activeOpacity, click);
    }

    // ------------------------------------------------- exercise list pieces

    /** The rounded search field with the magnifier, and a clear button once typed in. */
    EditText searchBar(LinearLayout parent, final Runnable changed) {
        LinearLayout bar = Ui.gap(Ui.row(this), 10);
        bar.setBackground(Ui.box(this, Ui.SURFACE, 12, Ui.BORDER));
        Ui.pad(bar, 14, 0, 14, 0);
        bar.addView(Ui.icon(this, R.drawable.ic_search, 18, Ui.TEXT_LIGHT));
        final EditText input = Ui.input(this, "Search exercises...", 16, 0, 0, 0, false);
        input.setBackground(null);
        Ui.pad(input, 0, 12, 0, 12);
        bar.addView(input, Ui.lp(0, Ui.WRAP, 1f));
        final ImageView clear = iconButton(R.drawable.ic_close_circle, 18, Ui.TEXT_LIGHT, 0.2f,
                new View.OnClickListener() {
                    public void onClick(View v) {
                        input.setText("");
                    }
                });
        clear.setVisibility(View.GONE);
        bar.addView(clear);
        input.addTextChangedListener(new WorkoutActivity.Watcher() {
            public void afterTextChanged(android.text.Editable s) {
                clear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                changed.run();
            }
        });
        parent.addView(bar, Ui.margins(this, Ui.MATCH, Ui.WRAP, 20, 20, 20, 8));
        return input;
    }

    /** The sideways-scrolling muscle and equipment filters. */
    HorizontalScrollView filterChips(final List<String> active, float padV, float chipMarginTop,
                                     float chipMarginBottom, final Runnable changed) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.gap(Ui.row(this), 8);
        Ui.pad(row, 20, padV, 20, padV);
        List<String> all = new ArrayList<String>(Arrays.asList(Db.MUSCLE_TAGS));
        all.addAll(Arrays.asList(Db.EQUIPMENT_TAGS));
        for (final String tag : all) {
            final TextView chip = Ui.chip(this, Ui.capitalize(tag), 13, active.contains(tag));
            Ui.pad(chip, 14, 0, 14, 0);
            Ui.touch(chip, 0.7f, new View.OnClickListener() {
                public void onClick(View v) {
                    if (!active.remove(tag)) active.add(tag);
                    Ui.setChip(chip, active.contains(tag));
                    changed.run();
                }
            });
            row.addView(chip, Ui.margins(this, Ui.WRAP, Ui.dp(this, 30), 0, chipMarginTop, 0, chipMarginBottom));
        }
        hs.addView(row);
        return hs;
    }

    /** Name, then the tag pills, or the details if there are no tags. */
    LinearLayout exerciseText(Db.Exercise e) {
        LinearLayout text = Ui.column(this);
        text.addView(Ui.text(this, e.name, 16, Ui.TEXT, 600));
        if (e.tags != null && !e.tags.isEmpty()) {
            Flow pills = new Flow(this, 4);
            for (String t : e.tags.split(",")) pills.addView(Ui.pill(this, t));
            text.addView(pills, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 4, 0, 0));
        } else if (e.details != null && !e.details.isEmpty()) {
            TextView d = Ui.text(this, e.details, 13, Ui.TEXT_SECONDARY, 400);
            d.setSingleLine(true);
            d.setEllipsize(TextUtils.TruncateAt.END);
            text.addView(d, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 2, 0, 0));
        }
        return text;
    }

    interface Render<T> {
        View view(T item);
    }

    /** A FlatList: no dividers, no selector, content padding that scrolls. */
    ListView listView(float l, float t, float r, float b) {
        ListView lv = new ListView(this);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setSelector(new ColorDrawable(0));
        lv.setCacheColorHint(0);
        lv.setClipToPadding(false);
        Ui.pad(lv, l, t, r, b);
        return lv;
    }

    /** Fills a list; each item is wrapped so it can keep its bottom margin. */
    <T> void setItems(ListView lv, final List<T> items, final float gapBelowDp, final Render<T> render) {
        lv.setAdapter(new BaseAdapter() {
            public int getCount() {
                return items.size();
            }

            public Object getItem(int i) {
                return items.get(i);
            }

            public long getItemId(int i) {
                return i;
            }

            public View getView(int i, View convert, ViewGroup parent) {
                FrameLayout f = new FrameLayout(Screen.this);
                f.setPadding(0, 0, 0, Ui.dp(Screen.this, gapBelowDp));
                f.addView(render.view(items.get(i)), new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
                return f;
            }
        });
    }
}
