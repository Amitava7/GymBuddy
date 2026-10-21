package com.gymbuddy.app;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * How one exercise has gone: the weight and reps per session as lines, and
 * how long each weight was kept before it went up. The exercise screen shows
 * them as cards; a running workout shows them in a pop-up.
 */
final class Progress {

    private Progress() {
    }

    /** One number per session (the heaviest set, or the most reps), oldest first. Blank sessions are left out. */
    static final class Series {
        final List<Double> values = new ArrayList<Double>();
        final List<String> dates = new ArrayList<String>();
        boolean anyNonZero;
    }

    /**
     * A weight that was a new best, from the session that set it up to the next
     * one that beat it. A lighter session in between (a light day, or a
     * different saved workout) does not end it.
     */
    static final class Step {
        int from, sessions;
        double kg;
        String date, until;   // until is the next increase, or null for the current weight
    }

    /** {@code history} newest first, as Db.getExerciseHistory gives it. */
    static Series series(List<Db.ExerciseSession> history, boolean kg) {
        Series s = new Series();
        for (int i = history.size() - 1; i >= 0; i--) {
            Db.ExerciseSession h = history.get(i);
            Double v = kg ? h.maxKg : h.maxReps;
            if (v == null) continue;
            s.values.add(v);
            s.dates.add(h.workoutDate);
            s.anyNonZero |= v != 0;
        }
        return s;
    }

    /** Every new best weight, oldest first; the last one is the weight now. */
    static List<Step> steps(Series s) {
        List<Step> out = new ArrayList<Step>();
        double best = -Double.MAX_VALUE;
        for (int i = 0; i < s.values.size(); i++) {
            double v = s.values.get(i);
            if (v <= best) continue;
            if (!out.isEmpty()) {
                Step prev = out.get(out.size() - 1);
                prev.sessions = i - prev.from;
                prev.until = s.dates.get(i);
            }
            Step st = new Step();
            st.from = i;
            st.kg = v;
            st.date = s.dates.get(i);
            out.add(st);
            best = v;
        }
        if (!out.isEmpty()) {
            Step now = out.get(out.size() - 1);
            now.sessions = s.values.size() - now.from;
        }
        return out;
    }

    // --------------------------------------------------------------- views

    /**
     * WEIGHT PER SESSION, with the time since the last increase shaded on the
     * line and TIME AT EACH WEIGHT under it: the newest {@code maxSteps}
     * weights, when each started and how many sessions and how long it lasted.
     * Null when no weight was ever logged.
     */
    static LinearLayout weight(Context c, List<Db.ExerciseSession> history, int maxSteps, boolean boxed) {
        Series s = series(history, true);
        if (!s.anyNonZero) return null;
        List<Step> steps = steps(s);
        Step now = steps.get(steps.size() - 1);
        LinearLayout col = section(c, "WEIGHT PER SESSION", s, true, boxed);
        LineChart chart = chart(c, s);
        chart.mark(now.from);
        chart.setContentDescription("Heaviest set over " + sessions(s.values.size()) + ", "
                + (now.from > 0 ? "up to " + amount(now.kg, true) + " on " + Fmt.day(now.date)
                : "at " + amount(now.kg, true) + " since " + Fmt.day(now.date)));
        col.addView(chart, Ui.lp(Ui.MATCH, Ui.dp(c, 150)));

        col.addView(caps(c, "TIME AT EACH WEIGHT"), Ui.margins(c, Ui.WRAP, Ui.WRAP, 0, 18, 0, 8));
        int shown = 0;
        for (int i = steps.size() - 1; i >= 0 && shown < maxSteps; i--, shown++) {
            col.addView(stepRow(c, steps.get(i), i == steps.size() - 1),
                    Ui.margins(c, Ui.MATCH, Ui.WRAP, 0, 0, 0, 6));
        }
        if (steps.size() > shown) {
            int earlier = steps.size() - shown;
            col.addView(Ui.text(c, earlier + (earlier == 1 ? " earlier weight" : " earlier weights"),
                    12, Ui.TEXT_SECONDARY, 400));
        }
        return col;
    }

    /** REPS PER SESSION: the most reps in a set each time. Null when no reps were ever logged. */
    static LinearLayout reps(Context c, List<Db.ExerciseSession> history, boolean boxed) {
        Series s = series(history, false);
        if (!s.anyNonZero) return null;
        LinearLayout col = section(c, "REPS PER SESSION", s, false, boxed);
        LineChart chart = chart(c, s);
        int n = s.values.size();
        double first = s.values.get(0), last = s.values.get(n - 1);
        chart.setContentDescription(n == 1
                ? "Most reps in a set: " + amount(last, false) + ", one session"
                : "Most reps in a set over " + sessions(n) + ", from " + amount(first, false)
                        + " to " + amount(last, false));
        col.addView(chart, Ui.lp(Ui.MATCH, Ui.dp(c, 150)));
        return col;
    }

    /** The title, then the latest number and how far it has moved since the first session. */
    private static LinearLayout section(Context c, String title, Series s, boolean kg, boolean boxed) {
        LinearLayout col = Ui.column(c);
        if (boxed) {
            col.setBackground(Ui.card(c, 14));
            Ui.pad(col, 16, 16, 16, 16);
        }
        col.addView(caps(c, title), Ui.margins(c, Ui.WRAP, Ui.WRAP, 0, 0, 0, 6));
        int n = s.values.size();
        double first = s.values.get(0), last = s.values.get(n - 1);
        LinearLayout now = Ui.gap(new LinearLayout(c), 8);   // baseline-aligned
        now.addView(Ui.text(c, amount(last, kg), 20, Ui.TEXT, 800));
        now.addView(Ui.text(c, change(first, last, kg, n), 12, Ui.TEXT_SECONDARY, 400));
        col.addView(now, Ui.margins(c, Ui.WRAP, Ui.WRAP, 0, 0, 0, 12));
        return col;
    }

    private static LineChart chart(Context c, Series s) {
        int n = s.values.size();
        double[] points = new double[n];
        String[] dates = new String[n];
        for (int i = 0; i < n; i++) {
            points[i] = s.values.get(i);
            dates[i] = Fmt.day(s.dates.get(i));
        }
        return new LineChart(c, points, dates);
    }

    /** Mar 7 | 62.5 kg | 4 sessions · 3 weeks so far. The current weight is in brighter text. */
    private static View stepRow(Context c, Step st, boolean current) {
        int color = current ? Ui.TEXT : Ui.TEXT_SECONDARY;
        LinearLayout row = Ui.gap(Ui.row(c), 8);
        row.addView(Ui.text(c, Fmt.day(st.date), 13, color, 400), Ui.lp(0, Ui.WRAP, 0.9f));
        row.addView(Ui.text(c, amount(st.kg, true), 13, color, 700), Ui.lp(0, Ui.WRAP, 0.8f));
        long days = Fmt.daysBetween(st.date, st.until);
        String held = sessions(st.sessions) + (days >= 1 ? " · " + Fmt.span(days) : "") + (current ? " so far" : "");
        TextView h = Ui.text(c, held, 13, color, 400);
        h.setGravity(Gravity.END);
        h.setSingleLine(true);
        h.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(h, Ui.lp(0, Ui.WRAP, 2f));
        return row;
    }

    private static TextView caps(Context c, String s) {
        return Ui.spaced(Ui.text(c, s, 11, Ui.TEXT_SECONDARY, 700), 1, 11);
    }

    static String sessions(int n) {
        return n == 1 ? "1 session" : n + " sessions";
    }

    /** "62.5 kg", "1 rep", "12 reps". */
    static String amount(double v, boolean kg) {
        return Fmt.num(v) + (kg ? " kg" : v == 1 ? " rep" : " reps");
    }

    /** "+17.5 kg over 12 sessions", "−2 reps over 5 sessions", "first session". */
    static String change(double first, double last, boolean kg, int sessions) {
        if (sessions == 1) return "first session";
        double d = Math.round((last - first) * 100) / 100.0;
        String over = " over " + sessions + " sessions";
        if (d == 0) return "no change" + over;
        return (d > 0 ? "+" : "−") + amount(Math.abs(d), kg) + over;
    }
}
