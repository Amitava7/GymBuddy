package com.gymbuddy.app;

import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The workout in progress: a running clock, one card per exercise with its
 * note and sets, and Finish Workout pinned to the bottom. What you type is
 * saved as you type it.
 */
public final class WorkoutActivity extends Screen {

    private long workoutId, gymId;
    private Long templateId;
    private Date startTime;
    private TextView clock;
    private LinearLayout cards;
    private List<Db.WorkoutExercise> exercises = new ArrayList<Db.WorkoutExercise>();
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
        templateId = w.templateId;
        exercises = db.getWorkoutExercisesWithSets(workoutId);
        cards.removeAllViews();
        for (Db.WorkoutExercise ex : exercises) cards.addView(card(ex));
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

    private void confirmRemove(final Db.WorkoutExercise ex, final View card) {
        alert("Remove Exercise", "Remove \"" + ex.name + "\" from this workout?",
                new String[]{"Cancel", "Remove"}, null, new Runnable() {
                    public void run() {
                        db.deleteWorkoutExercise(ex.id);
                        if (templateId != null && templateId != 0) {
                            db.removeTemplateExercise(templateId, ex.exerciseId);
                        }
                        exercises.remove(ex);
                        cards.removeView(card);
                    }
                });
    }

    // --------------------------------------------------------------- finish

    private void confirmFinish() {
        alert("Finish Workout", "Complete this workout?",
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

    /**
     * Ends the workout. A workout started from scratch becomes a saved
     * workout; one started from a saved workout adds any new exercises to it.
     */
    private void finishWorkout() {
        db.finishWorkout(workoutId);
        Db.Workout w = db.getWorkout(workoutId);
        if (w != null && w.templateId == null) {
            long t = db.createWorkoutTemplate(w.name, gymId);
            for (Db.WorkoutExercise ex : exercises) db.addTemplateExercise(t, ex.exerciseId, ex.sortOrder);
            db.setWorkoutTemplate(workoutId, t);
        } else if (w != null) {
            Set<Long> have = new HashSet<Long>();
            for (Db.TemplateExercise te : db.getTemplateExercises(w.templateId)) have.add(te.exerciseId);
            for (Db.WorkoutExercise ex : exercises) {
                if (!have.contains(ex.exerciseId)) {
                    db.addTemplateExercise(w.templateId, ex.exerciseId, ex.sortOrder);
                }
            }
        }
        replace(SummaryActivity.class, "gymId", gymId, "workoutId", workoutId);
    }

    abstract static class Watcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }
    }
}
