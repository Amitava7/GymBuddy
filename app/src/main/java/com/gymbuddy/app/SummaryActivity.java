package com.gymbuddy.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Workout Complete: the trophy, the totals, and what you did. */
public final class SummaryActivity extends Screen {

    private long workoutId, gymId;
    private LinearLayout body;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        workoutId = longExtra("workoutId");
        gymId = longExtra("gymId");
        header("Workout Complete", false);
        ScrollView scroll = new ScrollView(this);
        body = Ui.column(this);
        Ui.pad(body, 20, 20, 20, 100);
        scroll.addView(body);
        content.addView(scroll, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        bottomBar(primaryBar(0, "Back to Gym", new View.OnClickListener() {
            public void onClick(View v) {
                backToGym(gymId);
            }
        }), 34, true);
    }

    @Override
    void load() {
        Db.Workout w = db.getWorkout(workoutId);
        body.removeAllViews();
        if (w == null) return;
        // Only the exercises you ticked off count.
        List<Db.WorkoutExercise> done = new ArrayList<Db.WorkoutExercise>();
        for (Db.WorkoutExercise ex : db.getWorkoutExercisesWithSets(workoutId)) {
            if (ex.isCompleted != 0) done.add(ex);
        }
        int totalSets = 0;
        double volume = 0;
        for (Db.WorkoutExercise ex : done) {
            totalSets += ex.sets.size();
            for (Db.WorkoutSet s : ex.sets) {
                volume += (s.kg == null ? 0 : s.kg) * (s.reps == null ? 0 : s.reps);
            }
        }

        LinearLayout head = Ui.column(this);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        Ui.pad(head, 0, 24, 0, 24);
        FrameLayout trophy = new FrameLayout(this);
        trophy.setBackground(Ui.box(this, Ui.WARNING_15, 36));
        trophy.addView(Ui.icon(this, R.drawable.ic_trophy, 40, Ui.WARNING),
                new FrameLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40), Gravity.CENTER));
        head.addView(trophy, Ui.lp(Ui.dp(this, 72), Ui.dp(this, 72)));
        TextView title = Ui.text(this, w.name, 24, Ui.TEXT, 800);
        title.setGravity(Gravity.CENTER);
        head.addView(title, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 14, 0, 0));
        head.addView(Ui.text(this, Fmt.longDay(w.startedAt), 14, Ui.TEXT_SECONDARY, 400),
                Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 4, 0, 0));
        body.addView(head, Ui.lp(Ui.MATCH, Ui.WRAP));

        LinearLayout grid = Ui.gap(Ui.column(this), 10);
        LinearLayout r1 = Ui.gap(Ui.row(this), 10), r2 = Ui.gap(Ui.row(this), 10);
        r1.addView(stat(R.drawable.ic_time_outline, Fmt.durationLong(w.durationSeconds), "DURATION"), Ui.lp(0, Ui.WRAP, 1f));
        r1.addView(stat(R.drawable.ic_barbell_outline, String.valueOf(done.size()), "EXERCISES"), Ui.lp(0, Ui.WRAP, 1f));
        r2.addView(stat(R.drawable.ic_layers_outline, String.valueOf(totalSets), "SETS"), Ui.lp(0, Ui.WRAP, 1f));
        r2.addView(stat(R.drawable.ic_fitness_outline, String.valueOf(Math.round(volume)), "VOLUME"), Ui.lp(0, Ui.WRAP, 1f));
        grid.addView(r1, Ui.lp(Ui.MATCH, Ui.WRAP));
        grid.addView(r2, Ui.lp(Ui.MATCH, Ui.WRAP));
        body.addView(grid, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 24));

        for (Db.WorkoutExercise ex : done) {
            LinearLayout card = Ui.column(this);
            card.setBackground(Ui.card(this, 14));
            Ui.pad(card, 16, 16, 16, 16);
            card.addView(Ui.text(this, "✓ " + ex.name, 16, Ui.TEXT, 700),
                    Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 0, 0, 6));
            if (ex.note != null && !ex.note.isEmpty()) {
                TextView note = Ui.text(this, ex.note, 13, Ui.TEXT_SECONDARY, 400);
                note.setTypeface(Typeface.create(Typeface.DEFAULT, 400, true));
                card.addView(note, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 0, 0, 6));
            }
            for (Db.WorkoutSet s : ex.sets) {
                String line = "Set " + s.setNumber + ": " + (s.kg == null ? "0" : Fmt.num(s.kg)) + " kg x "
                        + (s.reps == null ? "0" : Fmt.num(s.reps)) + " reps";
                card.addView(Ui.text(this, line, 14, Ui.TEXT_SECONDARY, 400),
                        Ui.margins(this, Ui.WRAP, Ui.WRAP, 8, 0, 0, 2));
            }
            body.addView(card, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        }
    }

    private View stat(int icon, String value, String label) {
        LinearLayout c = Ui.column(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setBackground(Ui.card(this, 14));
        Ui.pad(c, 16, 16, 16, 16);
        c.addView(Ui.icon(this, icon, 22, Ui.PRIMARY));
        c.addView(Ui.text(this, value, 22, Ui.TEXT, 800), Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 4, 0, 0));
        c.addView(Ui.spaced(Ui.text(this, label, 11, Ui.TEXT_SECONDARY, 400), 1, 11),
                Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 2, 0, 0));
        return c;
    }
}
