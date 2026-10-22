package com.gymbuddy.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Adds an exercise to the running workout, or makes a new one on the spot. */
public final class PickExerciseActivity extends Screen {

    private long workoutId;
    private EditText search, newName;
    private final List<String> activeTags = new ArrayList<String>();
    private ListView list;
    private TextView emptyView;
    private LinearLayout bottom, toggle, createBar;
    private boolean picking;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        workoutId = longExtra("workoutId");
        header("Pick Exercise", true);
        LinearLayout col = Ui.column(this);
        Runnable reload = new Runnable() {
            public void run() {
                load();
            }
        };
        search = searchBar(col, reload);
        col.addView(filterChips(activeTags, 8, 0, 5, reload), Ui.lp(Ui.MATCH, Ui.WRAP));

        FrameLayout area = new FrameLayout(this);
        list = listView(20, 4, 20, 0);
        area.addView(list, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        emptyView = Ui.text(this, "No exercises yet", 15, Ui.TEXT_SECONDARY, 400);
        emptyView.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams ep = new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.TOP);
        ep.topMargin = Ui.dp(this, 44);
        area.addView(emptyView, ep);
        col.addView(area, Ui.lp(Ui.MATCH, 0, 1f));
        content.addView(col);

        bottom = Ui.column(this);
        bottom.setBackgroundColor(Ui.SURFACE);
        View line = new View(this);
        line.setBackgroundColor(Ui.BORDER);
        bottom.addView(line, Ui.lp(Ui.MATCH, Ui.dp(this, 1)));
        toggle = Ui.gap(Ui.row(this), 6);
        toggle.setGravity(Gravity.CENTER);
        Ui.pad(toggle, 16, 15, 16, 0);
        toggle.addView(Ui.icon(this, R.drawable.ic_add, 20, Ui.PRIMARY));
        toggle.addView(Ui.text(this, "Create New Exercise", 15, Ui.PRIMARY, 600));
        Ui.touch(toggle, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                showCreate(true);
            }
        });
        createBar = Ui.gap(Ui.row(this), 10);
        Ui.pad(createBar, 14, 13, 14, 0);
        newName = Ui.input(this, "New exercise name", 16, 12, 10, Ui.BACKGROUND, false);
        newName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int action, android.view.KeyEvent e) {
                if (action == EditorInfo.IME_ACTION_DONE) createAndPick();
                return action == EditorInfo.IME_ACTION_DONE;
            }
        });
        createBar.addView(newName, Ui.lp(0, Ui.WRAP, 1f));
        TextView add = Ui.text(this, "Add", 14, Ui.BACKGROUND, 700);
        add.setBackground(Ui.box(this, Ui.PRIMARY, 10));
        Ui.pad(add, 18, 12, 18, 12);
        createBar.addView(Ui.touch(add, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                createAndPick();
            }
        }));
        createBar.addView(iconButton(R.drawable.ic_close, 24, Ui.TEXT_SECONDARY, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                showCreate(false);
            }
        }));
        bottom.addView(toggle, Ui.lp(Ui.MATCH, Ui.WRAP));
        bottom.addView(createBar, Ui.lp(Ui.MATCH, Ui.WRAP));
        createBar.setVisibility(View.GONE);
        bottomBar(bottom, 30, false);
        Ui.showKeyboard(search);
    }

    private void showCreate(boolean on) {
        toggle.setVisibility(on ? View.GONE : View.VISIBLE);
        createBar.setVisibility(on ? View.VISIBLE : View.GONE);
        if (on) Ui.showKeyboard(newName);
    }

    @Override
    void load() {
        String q = search.getText().toString();
        List<Db.Exercise> items = db.getExercises(q, activeTags);
        emptyView.setText(!q.isEmpty() || !activeTags.isEmpty() ? "No exercises found" : "No exercises yet");
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        setItems(list, items, 8, new Render<Db.Exercise>() {
            public View view(final Db.Exercise e) {
                LinearLayout card = Ui.gap(Ui.row(PickExerciseActivity.this), 12);
                card.setBackground(Ui.card(PickExerciseActivity.this, 14));
                Ui.pad(card, 14, 14, 14, 14);
                card.addView(Ui.icon(PickExerciseActivity.this, R.drawable.ic_add_circle, 22, Ui.SUCCESS));
                card.addView(exerciseText(e), Ui.lp(0, Ui.WRAP, 1f));
                return Ui.touch(card, 0.7f, new View.OnClickListener() {
                    public void onClick(View v) {
                        pick(e.id);
                    }
                });
            }
        });
    }

    /**
     * Adds the exercise at the end with last time's note and sets, then goes
     * back to the workout. Picked a second time, it is a slot of its own.
     */
    private void pick(long exerciseId) {
        if (picking) return;
        picking = true;
        int next = 0, slot = 0;
        for (Db.WorkoutExercise e : db.getWorkoutExercises(workoutId)) {
            next = Math.max(next, e.sortOrder + 1);
            if (e.exerciseId == exerciseId) slot++;
        }
        long weId = db.addWorkoutExercise(workoutId, exerciseId, next);
        Db.Workout w = db.getWorkout(workoutId);
        GymActivity.prefill(db, weId,
                db.getLastWorkoutDataForExercise(exerciseId, w == null ? null : w.templateId, slot));
        Ui.hideKeyboard(this);
        finish();
    }

    private void createAndPick() {
        String n = newName.getText().toString().trim();
        if (n.isEmpty()) return;
        pick(db.createExercise(n, null, null));
    }
}
