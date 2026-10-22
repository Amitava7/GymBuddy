package com.gymbuddy.app;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** One gym: start a workout, see its history, or start from a saved workout. */
public final class GymActivity extends Screen {

    private long gymId;
    private LinearLayout list;
    private View emptyView;
    private ScrollView scroll;
    private LinearLayout form;
    private EditText templateName;
    private boolean starting;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        gymId = longExtra("gymId");
        header("Gym", true);

        LinearLayout col = Ui.column(this);
        LinearLayout actions = Ui.gap(Ui.row(this), 12);
        Ui.pad(actions, 20, 20, 20, 20);
        LinearLayout newWorkout = actionButton(R.drawable.ic_add_circle, "New Workout", true);
        Ui.touch(newWorkout, 0.8f, new View.OnClickListener() {
            public void onClick(View v) {
                open(NewWorkoutActivity.class, "gymId", gymId);
            }
        });
        LinearLayout history = actionButton(R.drawable.ic_time, "History", false);
        Ui.touch(history, 0.8f, new View.OnClickListener() {
            public void onClick(View v) {
                open(HistoryActivity.class, "gymId", gymId);
            }
        });
        actions.addView(newWorkout, Ui.lp(0, Ui.WRAP, 1f));
        actions.addView(history, Ui.lp(0, Ui.WRAP, 1f));
        col.addView(actions);

        LinearLayout section = Ui.column(this);
        Ui.pad(section, 20, 0, 20, 0);
        LinearLayout sectionHeader = Ui.row(this);
        sectionHeader.addView(Ui.text(this, "Saved Workouts", 18, Ui.TEXT, 700), Ui.lp(0, Ui.WRAP, 1f));
        View add = iconButton(R.drawable.ic_add_circle_outline, 24, Ui.PRIMARY, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                showForm();
            }
        });
        add.setContentDescription("New workout template");
        sectionHeader.addView(add);
        section.addView(sectionHeader, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 14));

        LinearLayout e = Ui.column(this);
        e.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView l = Ui.text(this, "No saved workouts yet", 16, Ui.TEXT_SECONDARY, 600);
        TextView s = Ui.text(this, "Finish a workout or create a template to save it here", 13, Ui.TEXT_LIGHT, 400);
        s.setGravity(Gravity.CENTER);
        e.addView(l, Ui.lp(Ui.WRAP, Ui.WRAP));
        e.addView(s, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 4, 0, 0));
        emptyView = e;
        section.addView(emptyView, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 40, 0, 0));

        list = Ui.column(this);
        scroll = new ScrollView(this);
        scroll.addView(list);
        section.addView(scroll, Ui.lp(Ui.MATCH, 0, 1f));
        col.addView(section, Ui.lp(Ui.MATCH, 0, 1f));
        content.addView(col);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private LinearLayout actionButton(int icon, String label, boolean primary) {
        LinearLayout b = Ui.gap(Ui.row(this), 8);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primary ? Ui.box(this, Ui.PRIMARY, 14) : Ui.box(this, Ui.SURFACE, 14, Ui.BORDER));
        Ui.pad(b, 0, 14, 0, 14);
        int fg = primary ? Ui.BACKGROUND : Ui.PRIMARY;
        b.addView(Ui.icon(this, icon, 20, fg));
        b.addView(Ui.text(this, label, 15, fg, 700));
        return b;
    }

    @Override
    void load() {
        starting = false;
        for (Db.Gym g : db.getGyms()) {
            if (g.id == gymId) title(g.name);
        }
        List<Db.Template> templates = db.getWorkoutTemplates(gymId);
        emptyView.setVisibility(templates.isEmpty() ? View.VISIBLE : View.GONE);
        scroll.setVisibility(templates.isEmpty() ? View.GONE : View.VISIBLE);
        list.removeAllViews();
        for (final Db.Template t : templates) {
            LinearLayout card = Ui.gap(Ui.row(this), 14);
            card.setBackground(Ui.card(this, 14));
            Ui.pad(card, 16, 16, 16, 16);
            card.addView(Ui.icon(this, R.drawable.ic_document_text, 22, Ui.PRIMARY));
            LinearLayout text = Ui.column(this);
            text.addView(Ui.text(this, t.name, 16, Ui.TEXT, 600));
            text.addView(Ui.text(this, "Tap to start", 12, Ui.TEXT_SECONDARY, 400),
                    Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 2, 0, 0));
            card.addView(text, Ui.lp(0, Ui.WRAP, 1f));
            card.addView(Ui.icon(this, R.drawable.ic_play_circle, 26, Ui.SUCCESS));
            Ui.touch(card, 0.7f, new View.OnClickListener() {
                public void onClick(View v) {
                    start(t);
                }
            });
            Ui.longPress(card, new Runnable() {
                public void run() {
                    confirmDelete(t);
                }
            });
            list.addView(card, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        }
    }

    private void start(Db.Template t) {
        if (starting) return;   // a double tap used to start the workout twice
        starting = true;
        long workoutId = startFromTemplate(db, t);
        open(WorkoutActivity.class, "gymId", gymId, "workoutId", workoutId);
    }

    /**
     * Starts a workout with the template's exercises, each pre-filled with the
     * note and sets from the last time this template was done. Other saved
     * workouts with the same exercise have no say in it, and an exercise that
     * is in this one twice gets each slot's own sets back.
     */
    static long startFromTemplate(Db db, Db.Template t) {
        SQLiteDatabase sql = db.getWritableDatabase();
        sql.beginTransaction();
        try {
            long workoutId = db.startWorkout(t.name, t.gymId, t.id);
            Map<Long, Integer> slots = new HashMap<Long, Integer>();
            List<Db.TemplateExercise> list = db.getTemplateExercises(t.id);
            for (int i = 0; i < list.size(); i++) {
                Db.TemplateExercise ex = list.get(i);
                Integer slot = slots.get(ex.exerciseId);
                if (slot == null) slot = 0;
                slots.put(ex.exerciseId, slot + 1);
                long weId = db.addWorkoutExercise(workoutId, ex.exerciseId, i);
                prefill(db, weId, db.getLastWorkoutDataForExercise(ex.exerciseId, t.id, slot));
            }
            sql.setTransactionSuccessful();
            return workoutId;
        } finally {
            sql.endTransaction();
        }
    }

    /** Copies last time's note and sets into a new workout exercise, or one empty set. */
    static void prefill(Db db, long weId, Db.LastData last) {
        if (last != null) {
            if (last.note != null && !last.note.isEmpty()) db.updateWorkoutExerciseNote(weId, last.note);
            if (!last.sets.isEmpty()) {
                for (Db.WorkoutSet s : last.sets) db.addWorkoutSet(weId, s.setNumber, s.kg, s.reps);
                return;
            }
        }
        db.addWorkoutSet(weId, 1, null, null);
    }

    private void confirmDelete(final Db.Template t) {
        alert("Delete Template", "Delete \"" + t.name + "\"?", new String[]{"Cancel", "Delete"},
                null, new Runnable() {
                    public void run() {
                        db.deleteWorkoutTemplate(t.id);
                        load();
                    }
                });
    }

    private void showForm() {
        if (form == null) {
            form = formCard("New Workout Template");
            templateName = Ui.input(this, "Workout name (e.g. Push Day)", 16, 14, 12, Ui.BACKGROUND, false);
            form.addView(templateName, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));
            form.addView(formButtons("Create", new View.OnClickListener() {
                public void onClick(View v) {
                    hideOverlay();
                }
            }, new View.OnClickListener() {
                public void onClick(View v) {
                    create();
                }
            }));
        }
        if (form.getParent() != null) ((ViewGroup) form.getParent()).removeView(form);
        showOverlay(form);
        Ui.showKeyboard(templateName);
    }

    private void create() {
        String n = templateName.getText().toString().trim();
        if (n.isEmpty()) return;
        db.createWorkoutTemplate(n, gymId);
        templateName.setText("");
        hideOverlay();
        load();
    }
}
