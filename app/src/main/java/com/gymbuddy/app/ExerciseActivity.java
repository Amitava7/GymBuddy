package com.gymbuddy.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * One exercise: its details, personal records, how the weight, reps and volume
 * have gone, how long each weight lasted, and every session.
 */
public final class ExerciseActivity extends Screen {

    private long exerciseId;
    private LinearLayout body;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        exerciseId = longExtra("exerciseId");
        header("Exercise Details", true);
        ScrollView scroll = new ScrollView(this);
        body = Ui.column(this);
        Ui.pad(body, 20, 20, 20, 0);
        scroll.addView(body);
        content.addView(scroll, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
    }

    @Override
    void load() {
        Db.Exercise ex = db.getExercise(exerciseId);
        body.removeAllViews();
        if (ex == null) return;
        title(ex.name);
        List<Db.ExerciseSession> history = db.getExerciseHistory(exerciseId);
        Db.Records rec = db.getExerciseRecords(exerciseId);

        LinearLayout head = Ui.row(this);
        head.addView(Ui.text(this, ex.name, 24, Ui.TEXT, 800), Ui.lp(0, Ui.WRAP, 1f));
        View edit = iconButton(R.drawable.ic_create_outline, 22, Ui.PRIMARY, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                open(ExerciseFormActivity.class, "exerciseId", exerciseId);
            }
        });
        edit.setContentDescription("Edit exercise");
        head.addView(edit);
        body.addView(head, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 16));

        if (ex.details != null && !ex.details.isEmpty()) {
            LinearLayout d = Ui.column(this);
            d.setBackground(Ui.card(this, 14));
            Ui.pad(d, 16, 16, 16, 16);
            d.addView(Ui.spaced(Ui.text(this, "DETAILS", 11, Ui.TEXT_SECONDARY, 700), 1, 11),
                    Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 0, 0, 6));
            TextView t = Ui.text(this, ex.details, 15, Ui.TEXT, 400);
            t.setLineHeight(Ui.dp(this, 22));
            d.addView(t);
            body.addView(d, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 20));
        }

        // Shown only when there is a weight or rep count that is not zero.
        if (rec != null && (nonZero(rec.maxKg) || nonZero(rec.maxReps))) {
            LinearLayout section = Ui.column(this);
            section.addView(sectionTitle("Personal Records"));
            LinearLayout row = Ui.gap(Ui.row(this), 10);
            if (rec.maxKg != null) row.addView(record(Fmt.num(rec.maxKg) + " kg", "Max Weight"), Ui.lp(0, Ui.WRAP, 1f));
            if (rec.maxReps != null) row.addView(record(Fmt.num(rec.maxReps), "Max Reps"), Ui.lp(0, Ui.WRAP, 1f));
            if (rec.maxVolume != null) {
                row.addView(record(String.valueOf(Math.round(rec.maxVolume)), "Max Volume"), Ui.lp(0, Ui.WRAP, 1f));
            }
            section.addView(row, Ui.lp(Ui.MATCH, Ui.WRAP));
            body.addView(section, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 20));
        }

        LinearLayout hist = Ui.column(this);
        hist.addView(sectionTitle("History & Trends"));
        if (history.isEmpty()) {
            TextView none = Ui.text(this, "No workout data yet for this exercise", 14, Ui.TEXT_SECONDARY, 400);
            none.setGravity(Gravity.CENTER);
            hist.addView(none, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 20, 0, 0));
        } else {
            View weight = Progress.weight(this, history, 8, true);
            if (weight != null) hist.addView(weight, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 16));
            View reps = Progress.reps(this, history, true);
            if (reps != null) hist.addView(reps, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 16));
            hist.addView(chart(history), Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 16));
            for (Db.ExerciseSession h : history) hist.addView(sessionCard(h));
        }
        body.addView(hist, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 20));
    }

    private static boolean nonZero(Double d) {
        return d != null && d != 0;
    }

    private TextView sectionTitle(String s) {
        TextView t = Ui.text(this, s, 18, Ui.TEXT, 700);
        t.setLayoutParams(Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 0, 0, 12));
        return t;
    }

    private View record(String value, String label) {
        LinearLayout c = Ui.column(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setBackground(Ui.card(this, 14));
        Ui.pad(c, 14, 14, 14, 14);
        c.addView(Ui.icon(this, R.drawable.ic_trophy, 20, Ui.WARNING));
        c.addView(Ui.text(this, value, 20, Ui.TEXT, 800), Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 4, 0, 0));
        c.addView(Ui.text(this, label, 11, Ui.TEXT_SECONDARY, 400), Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 2, 0, 0));
        return c;
    }

    /** VOLUME PER SESSION: the last ten sessions as bars, oldest on the left. */
    private View chart(List<Db.ExerciseSession> history) {
        LinearLayout c = Ui.column(this);
        c.setBackground(Ui.card(this, 14));
        Ui.pad(c, 16, 16, 16, 16);
        c.addView(Ui.spaced(Ui.text(this, "VOLUME PER SESSION", 11, Ui.TEXT_SECONDARY, 700), 1, 11),
                Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 0, 0, 12));
        int n = Math.min(10, history.size());
        double max = 0;
        for (int i = 0; i < n; i++) {
            double v = history.get(i).totalVolume;
            max = Math.max(max, v != 0 ? v : 1);
        }
        LinearLayout bars = Ui.row(this);
        bars.setGravity(Gravity.BOTTOM);
        bars.setMinimumHeight(Ui.dp(this, 110));
        for (int i = n - 1; i >= 0; i--) {
            Db.ExerciseSession h = history.get(i);
            LinearLayout col = Ui.column(this);
            col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            float height = (float) Math.max(h.totalVolume / max * 100, 4);
            View bar = new View(this);
            bar.setBackground(Ui.box(this, Ui.PRIMARY, 4));
            col.addView(bar, Ui.lp(Ui.dp(this, 18), Ui.dp(this, height)));
            TextView label = Ui.text(this, Fmt.shortDate(h.workoutDate).split(",")[0], 8, Ui.TEXT_LIGHT, 400);
            label.setGravity(Gravity.CENTER);
            col.addView(label, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 4, 0, 0));
            bars.addView(col, Ui.lp(0, Ui.WRAP, 1f));
        }
        c.addView(bars, Ui.lp(Ui.MATCH, Ui.WRAP));
        return c;
    }

    private View sessionCard(Db.ExerciseSession h) {
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.card(this, 12));
        Ui.pad(card, 14, 14, 14, 14);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.text(this, Fmt.shortDate(h.workoutDate), 13, Ui.TEXT, 600), Ui.lp(0, Ui.WRAP, 1f));
        top.addView(Ui.text(this, h.workoutName, 13, Ui.TEXT_SECONDARY, 400));
        card.addView(top, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 6));
        LinearLayout stats = Ui.gap(Ui.row(this), 12);
        stats.addView(Ui.text(this, "Max: " + (h.maxKg == null ? "0" : Fmt.num(h.maxKg)) + " kg", 12, Ui.TEXT_SECONDARY, 400));
        stats.addView(Ui.text(this, "Reps: " + (h.maxReps == null ? "0" : Fmt.num(h.maxReps)), 12, Ui.TEXT_SECONDARY, 400));
        stats.addView(Ui.text(this, h.totalSets + " sets", 12, Ui.TEXT_SECONDARY, 400));
        stats.addView(Ui.text(this, "Vol: " + Math.round(h.totalVolume), 12, Ui.TEXT_SECONDARY, 400));
        card.addView(stats);
        card.setLayoutParams(Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
        return card;
    }
}
