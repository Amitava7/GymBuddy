package com.gymbuddy.app;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * The workout in progress: a running clock, one card per exercise with its
 * note and sets, and Finish Workout pinned to the bottom. What you type is
 * saved as you type it. Each exercise's chart opens over the workout, and
 * Reorder in the header moves exercises up and down.
 */
public final class WorkoutActivity extends Screen {

    private long workoutId, gymId;
    private Date startTime;
    private TextView clock;
    private LinearLayout cards;
    private View reorder;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        public void run() {
            updateClock();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        workoutId = longExtra("workoutId");
        gymId = longExtra("gymId");
        header("", false);
        reorder = Ui.touch(pill(R.drawable.ic_swap_vertical_outline, "Reorder"), 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                showReorder();
            }
        });
        reorder.setContentDescription("Reorder exercises");
        headerAction(reorder);

        LinearLayout col = Ui.column(this);
        LinearLayout timer = Ui.gap(Ui.row(this), 6);
        timer.setGravity(Gravity.CENTER);
        timer.setBackgroundColor(Ui.SURFACE);
        Ui.pad(timer, 0, 12, 0, 12);
        timer.addView(Ui.icon(this, R.drawable.ic_time_outline, 18, Ui.PRIMARY));
        clock = Ui.text(this, Fmt.clock(0), 20, Ui.PRIMARY, 700);
        clock.setFontFeatureSettings("tnum");
        // The clock changes every second; telling accessibility services each
        // time keeps them (and UI automation) from ever seeing the screen settle.
        clock.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void sendAccessibilityEventUnchecked(View host, AccessibilityEvent e) {
                if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    super.sendAccessibilityEventUnchecked(host, e);
                }
            }
        });
        timer.addView(clock);
        col.addView(timer, Ui.lp(Ui.MATCH, Ui.WRAP));
        View line = new View(this);
        line.setBackgroundColor(Ui.BORDER);
        col.addView(line, Ui.lp(Ui.MATCH, Ui.dp(this, 1)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = Ui.column(this);
        Ui.pad(body, 20, 20, 20, 100);
        cards = Ui.column(this);
        body.addView(cards, Ui.lp(Ui.MATCH, Ui.WRAP));

        LinearLayout add = Ui.gap(Ui.row(this), 6);
        add.setGravity(Gravity.CENTER);
        GradientDrawable dashed = Ui.box(this, Ui.SURFACE, 14);
        dashed.setStroke(Ui.dp(this, 1), Ui.PRIMARY_30, Ui.dp(this, 3), Ui.dp(this, 3));
        add.setBackground(dashed);
        Ui.pad(add, 0, 18, 0, 18);
        add.addView(Ui.icon(this, R.drawable.ic_add_circle_outline, 20, Ui.PRIMARY));
        add.addView(Ui.text(this, "Add Exercise", 16, Ui.PRIMARY, 600));
        Ui.touch(add, 0.7f, new View.OnClickListener() {
            public void onClick(View v) {
                open(PickExerciseActivity.class, "workoutId", workoutId, "gymId", gymId);
            }
        });
        body.addView(add, Ui.lp(Ui.MATCH, Ui.WRAP));
        scroll.addView(body);
        col.addView(scroll, Ui.lp(Ui.MATCH, 0, 1f));
        content.addView(col);

        bottomBar(primaryBar(R.drawable.ic_checkmark_done, "Finish Workout", new View.OnClickListener() {
            public void onClick(View v) {
                confirmFinish();
            }
        }), 34, true);
    }

    @Override
    void load() {
        Db.Workout w = db.getWorkout(workoutId);
        if (w == null) return;
        title(w.name);
        startTime = Fmt.utc(w.startedAt);
        cards.removeAllViews();
        for (Db.WorkoutExercise ex : db.getWorkoutExercisesWithSets(workoutId)) cards.addView(card(ex));
        reorder.setVisibility(cards.getChildCount() > 1 ? View.VISIBLE : View.GONE);
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void updateClock() {
        long elapsed = startTime == null ? 0 : (System.currentTimeMillis() - startTime.getTime()) / 1000;
        clock.setText(Fmt.clock(Math.max(0, elapsed)));
    }

    // ---------------------------------------------------------------- a card

    private View card(final Db.WorkoutExercise ex) {
        final LinearLayout card = Ui.column(this);
        Ui.pad(card, 16, 16, 16, 16);
        card.setLayoutParams(Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        LinearLayout head = Ui.gap(Ui.row(this), 10);
        final ImageView toggle = Ui.icon(this, R.drawable.ic_ellipse_outline, 26, Ui.TEXT_LIGHT);
        final TextView name = Ui.text(this, ex.name, 17, Ui.TEXT, 700);
        Ui.touch(toggle, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                ex.isCompleted = ex.isCompleted == 0 ? 1 : 0;
                db.toggleWorkoutExercise(ex.id, ex.isCompleted == 1);
                style(card, toggle, name, ex);
            }
        });
        head.addView(toggle);
        head.addView(name, Ui.lp(0, Ui.WRAP, 1f));
        View chart = iconButton(R.drawable.ic_analytics_outline, 22, Ui.PRIMARY, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                showProgress(ex);
            }
        });
        chart.setContentDescription("Progress for " + ex.name);
        head.addView(chart);
        head.addView(iconButton(R.drawable.ic_trash_outline, 20, Ui.DANGER, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                confirmRemove(ex, card);
            }
        }));
        card.addView(head, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));

        EditText note = Ui.input(this, "Add note...", 14, 10, 10, Ui.BACKGROUND, true);
        note.setMinHeight(Ui.dp(this, 36));
        note.setText(ex.note == null ? "" : ex.note);
        note.addTextChangedListener(new Watcher() {
            public void afterTextChanged(Editable s) {
                ex.note = s.toString();
                db.updateWorkoutExerciseNote(ex.id, ex.note);
            }
        });
        card.addView(note, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));

        LinearLayout setsHeader = Ui.row(this);
        Ui.pad(setsHeader, 4, 0, 4, 0);
        for (String h : new String[]{"SET", "KG", "REPS"}) {
            TextView t = Ui.spaced(Ui.text(this, h, 11, Ui.TEXT_LIGHT, 700), 1, 11);
            t.setGravity(Gravity.CENTER);
            setsHeader.addView(t, Ui.lp(0, Ui.WRAP, 1f));
        }
        setsHeader.addView(Ui.space(this, 30, 1));
        card.addView(setsHeader, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 6));

        final LinearLayout rows = Ui.column(this);
        for (Db.WorkoutSet s : ex.sets) rows.addView(setRow(ex, s, rows));
        card.addView(rows, Ui.lp(Ui.MATCH, Ui.WRAP));

        LinearLayout addSet = Ui.gap(Ui.row(this), 4);
        addSet.setGravity(Gravity.CENTER);
        Ui.pad(addSet, 0, 8, 0, 8);
        addSet.addView(Ui.icon(this, R.drawable.ic_add, 16, Ui.PRIMARY));
        addSet.addView(Ui.text(this, "Add Set", 14, Ui.PRIMARY, 600));
        Ui.touch(addSet, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                int next = ex.sets.size() + 1;
                Db.WorkoutSet s = new Db.WorkoutSet();
                s.id = db.addWorkoutSet(ex.id, next, null, null);
                s.workoutExerciseId = ex.id;
                s.setNumber = next;
                ex.sets.add(s);
                rows.addView(setRow(ex, s, rows));
            }
        });
        card.addView(addSet, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 4, 0, 0));

        style(card, toggle, name, ex);
        return card;
    }

    /** Done exercises get the accent left edge, a filled tick and a struck-through name. */
    private void style(LinearLayout card, ImageView toggle, TextView name, Db.WorkoutExercise ex) {
        boolean done = ex.isCompleted == 1;
        card.setBackground(done
                ? new Ui.LeftAccent(this, Ui.SURFACE, Ui.BORDER, Ui.SUCCESS, 14, 3)
                : Ui.card(this, 14));
        Ui.setIcon(toggle, done ? R.drawable.ic_checkmark_circle : R.drawable.ic_ellipse_outline,
                done ? Ui.SUCCESS : Ui.TEXT_LIGHT);
        name.setTextColor(done ? Ui.TEXT_SECONDARY : Ui.TEXT);
        name.setPaintFlags(done ? name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    private View setRow(final Db.WorkoutExercise ex, final Db.WorkoutSet s, final LinearLayout rows) {
        final LinearLayout row = Ui.gap(Ui.row(this), 4);
        TextView number = Ui.text(this, String.valueOf(s.setNumber), 15, Ui.TEXT_SECONDARY, 600);
        number.setGravity(Gravity.CENTER);
        row.addView(number, Ui.lp(0, Ui.WRAP, 1f));
        row.addView(setInput(s, "kg", true), Ui.lp(0, Ui.WRAP, 1f));
        row.addView(setInput(s, "reps", false), Ui.lp(0, Ui.WRAP, 1f));
        row.addView(iconButton(R.drawable.ic_close_circle_outline, 20, Ui.DANGER, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                db.deleteWorkoutSet(s.id);
                ex.sets.remove(s);
                rows.removeView(row);
            }
        }));
        row.setLayoutParams(Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 6));
        return row;
    }

    private EditText setInput(final Db.WorkoutSet s, final String field, boolean decimal) {
        EditText e = Ui.input(this, "0", 16, 10, 8, Ui.BACKGROUND, false);
        e.setGravity(Gravity.CENTER);
        // The number pad takes the phone's own decimal separator; parse() reads either.
        e.setInputType(InputType.TYPE_CLASS_NUMBER | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        Double v = "kg".equals(field) ? s.kg : s.reps;
        e.setText(v == null ? "" : Fmt.num(v));
        e.addTextChangedListener(new Watcher() {
            public void afterTextChanged(Editable t) {
                Double value = parse(t.toString());
                if ("kg".equals(field)) s.kg = value;
                else s.reps = value;
                db.updateWorkoutSet(s.id, field, value);
            }
        });
        return e;
    }

    /** '' is no value; a comma counts as the decimal point. */
    static Double parse(String text) {
        String t = text.trim().replace(',', '.');
        if (t.isEmpty()) return null;
        try {
            double d = Double.parseDouble(t);
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The saved workout loses it too, but only once this workout is finished. */
    private void confirmRemove(final Db.WorkoutExercise ex, final View card) {
        alert("Remove Exercise", "Remove \"" + ex.name + "\" from this workout?",
                new String[]{"Cancel", "Remove"}, null, new Runnable() {
                    public void run() {
                        db.deleteWorkoutExercise(ex.id);
                        cards.removeView(card);
                        reorder.setVisibility(cards.getChildCount() > 1 ? View.VISIBLE : View.GONE);
                    }
                });
    }

    // ------------------------------------------------------------- pop-ups

    /**
     * The exercise's weight and reps lines over the workout, with how long the
     * current weight has lasted, so you can decide whether to go up today.
     */
    private void showProgress(final Db.WorkoutExercise ex) {
        Ui.hideKeyboard(this);
        List<Db.ExerciseSession> history = db.getExerciseHistory(ex.exerciseId);
        LinearLayout f = Ui.column(this);
        f.setBackground(Ui.card(this, 20));
        Ui.pad(f, 20, 20, 20, 20);
        LinearLayout head = Ui.gap(Ui.row(this), 12);
        head.addView(Ui.text(this, ex.name, 20, Ui.TEXT, 700), Ui.lp(0, Ui.WRAP, 1f));
        View close = iconButton(R.drawable.ic_close, 24, Ui.TEXT_SECONDARY, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                hideOverlay();
            }
        });
        close.setContentDescription("Close");
        head.addView(close);
        f.addView(head, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 16));

        View weight = Progress.weight(this, history, 3, false);
        View reps = Progress.reps(this, history, false);
        if (weight == null && reps == null) {
            f.addView(Ui.text(this, history.isEmpty() ? "No finished workouts with this exercise yet."
                    : "No weight or reps logged for this exercise yet.", 14, Ui.TEXT_SECONDARY, 400));
        }
        if (weight != null) f.addView(weight, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 24));
        if (reps != null) f.addView(reps, Ui.lp(Ui.MATCH, Ui.WRAP));
        if (!history.isEmpty()) {
            TextView all = Ui.text(this, "All history", 15, Ui.PRIMARY, 600);
            Ui.pad(all, 0, 12, 0, 4);
            f.addView(Ui.touch(all, 0.2f, new View.OnClickListener() {
                public void onClick(View v) {
                    hideOverlay();
                    open(ExerciseActivity.class, "exerciseId", ex.exerciseId);
                }
            }), Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 8, 0, 0));
        }
        showOverlay(scrolling(f));
    }

    /** Moves exercises up and down; each move is saved and shows on the cards behind at once. */
    private void showReorder() {
        Ui.hideKeyboard(this);
        LinearLayout f = formCard("Reorder Exercises");
        LinearLayout rows = Ui.column(this);
        f.addView(rows, Ui.lp(Ui.MATCH, Ui.WRAP));
        fillReorder(rows);
        LinearLayout end = Ui.row(this);
        end.setGravity(Gravity.END);
        TextView done = Ui.text(this, "Done", 16, Ui.BACKGROUND, 700);
        done.setBackground(Ui.box(this, Ui.PRIMARY, 10));
        Ui.pad(done, 24, 12, 24, 12);
        end.addView(Ui.touch(done, 0.8f, new View.OnClickListener() {
            public void onClick(View v) {
                hideOverlay();
            }
        }));
        f.addView(end, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 8, 0, 0));
        showOverlay(scrolling(f));
    }

    private void fillReorder(final LinearLayout rows) {
        final List<Db.WorkoutExercise> list = db.getWorkoutExercises(workoutId);
        rows.removeAllViews();
        for (int i = 0; i < list.size(); i++) {
            LinearLayout row = Ui.gap(Ui.row(this), 4);
            row.setBackground(Ui.box(this, Ui.BACKGROUND, 12, Ui.BORDER));
            Ui.pad(row, 14, 4, 4, 4);
            TextView number = Ui.text(this, String.valueOf(i + 1), 14, Ui.TEXT_SECONDARY, 600);
            number.setFontFeatureSettings("tnum");
            row.addView(number, Ui.lp(Ui.dp(this, 22), Ui.WRAP));
            TextView name = Ui.text(this, list.get(i).name, 15, Ui.TEXT, 600);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(name, Ui.lp(0, Ui.WRAP, 1f));
            row.addView(move(rows, list, i, -1));
            row.addView(move(rows, list, i, 1));
            rows.addView(row, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
        }
    }

    /** An up (-1) or down (1) arrow; greyed out at the top or bottom. */
    private View move(final LinearLayout rows, final List<Db.WorkoutExercise> list, final int at, final int by) {
        final boolean can = at + by >= 0 && at + by < list.size();
        FrameLayout b = new FrameLayout(this);
        ImageView arrow = Ui.icon(this, R.drawable.ic_chevron_forward, 20, can ? Ui.PRIMARY : Ui.TEXT_LIGHT);
        arrow.setRotation(by < 0 ? -90 : 90);
        b.addView(arrow, new FrameLayout.LayoutParams(Ui.dp(this, 20), Ui.dp(this, 20), Gravity.CENTER));
        b.setLayoutParams(Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        b.setContentDescription((by < 0 ? "Move up: " : "Move down: ") + list.get(at).name);
        b.setEnabled(can);
        return Ui.touch(b, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                if (!can) return;
                List<Long> ids = new ArrayList<Long>();
                for (Db.WorkoutExercise e : list) ids.add(e.id);
                Collections.swap(ids, at, at + by);
                db.reorderWorkoutExercises(ids);
                fillReorder(rows);
                load();
            }
        });
    }

    /** A pop-up taller than the screen scrolls inside the dimmed layer. */
    private View scrolling(View card) {
        ScrollView sv = new ScrollView(this);
        sv.addView(card);
        return sv;
    }

    // --------------------------------------------------------------- finish

    /**
     * Back used to close the screen and leave the workout behind, unfinished
     * and out of sight. It asks first now, with the same choices as Finish.
     */
    @Override
    public void onBackPressed() {
        if (overlayShown()) {
            super.onBackPressed();
            return;
        }
        confirmEnd("Leave Workout?", "This workout is still in progress.");
    }

    private void confirmFinish() {
        confirmEnd("Finish Workout", "Complete this workout?");
    }

    private void confirmEnd(String title, String message) {
        alert(title, message,
                new String[]{"Keep Going", "Cancel Workout", "Finish"},
                null,
                new Runnable() {
                    public void run() {
                        db.deleteWorkout(workoutId);
                        backToGym(gymId);
                    }
                },
                new Runnable() {
                    public void run() {
                        finishWorkout();
                    }
                });
    }

    private void finishWorkout() {
        complete(db, workoutId);
        replace(SummaryActivity.class, "gymId", gymId, "workoutId", workoutId);
    }

    /**
     * Ends the workout. A workout started from scratch becomes a saved
     * workout; one started from a saved workout passes on the exercises added
     * to it or removed from it. The sets and notes go forward too, since the
     * next start copies them from this workout. Workouts finished before it
     * keep their own, and a cancelled workout changes nothing.
     */
    static void complete(Db db, long workoutId) {
        SQLiteDatabase sql = db.getWritableDatabase();
        sql.beginTransaction();
        try {
            db.finishWorkout(workoutId);
            Db.Workout w = db.getWorkout(workoutId);
            if (w != null) {
                List<Long> ids = new ArrayList<Long>();
                for (Db.WorkoutExercise ex : db.getWorkoutExercises(workoutId)) ids.add(ex.exerciseId);
                long t = w.templateId != null ? w.templateId : db.createWorkoutTemplate(w.name, w.gymId);
                db.setTemplateExercises(t, ids);
                if (w.templateId == null) db.setWorkoutTemplate(workoutId, t);
            }
            sql.setTransactionSuccessful();
        } finally {
            sql.endTransaction();
        }
    }

    abstract static class Watcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }
    }
}
