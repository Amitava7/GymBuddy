package com.gymbuddy.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/** Finished workouts at this gym, newest first. */
public final class HistoryActivity extends Screen {

    private long gymId;
    private ListView list;
    private View emptyView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        gymId = longExtra("gymId");
        header("History", true);
        list = listView(20, 20, 20, 10);
        content.addView(list, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        LinearLayout e = Ui.column(this);
        e.setGravity(Gravity.CENTER_HORIZONTAL);
        e.addView(Ui.icon(this, R.drawable.ic_calendar_outline, 48, Ui.TEXT_LIGHT));
        e.addView(Ui.text(this, "No completed workouts yet", 16, Ui.TEXT_SECONDARY, 400),
                Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 12, 0, 0));
        emptyView = e;
        content.addView(emptyView, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.CENTER));
    }

    @Override
    void load() {
        List<Db.HistoryItem> items = db.getWorkoutHistory(gymId);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        setItems(list, items, 10, new Render<Db.HistoryItem>() {
            public View view(final Db.HistoryItem h) {
                LinearLayout card = Ui.column(HistoryActivity.this);
                card.setBackground(Ui.card(HistoryActivity.this, 14));
                Ui.pad(card, 16, 16, 16, 16);
                LinearLayout top = Ui.row(HistoryActivity.this);
                top.addView(Ui.text(HistoryActivity.this, h.name, 16, Ui.TEXT, 700), Ui.lp(0, Ui.WRAP, 1f));
                top.addView(Ui.text(HistoryActivity.this, Fmt.historyDate(h.startedAt), 12, Ui.TEXT_SECONDARY, 400));
                card.addView(top, Ui.margins(HistoryActivity.this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
                LinearLayout stats = Ui.gap(Ui.row(HistoryActivity.this), 16);
                stats.addView(stat(R.drawable.ic_time_outline, Fmt.durationShort(h.durationSeconds)));
                stats.addView(stat(R.drawable.ic_barbell_outline, h.exerciseCount + " exercises"));
                stats.addView(stat(R.drawable.ic_layers_outline, h.totalSets + " sets"));
                card.addView(stats);
                return Ui.touch(card, 0.7f, new View.OnClickListener() {
                    public void onClick(View v) {
                        open(SummaryActivity.class, "gymId", gymId, "workoutId", h.id);
                    }
                });
            }
        });
    }

    private View stat(int icon, String text) {
        LinearLayout s = Ui.gap(Ui.row(this), 4);
        s.addView(Ui.icon(this, icon, 14, Ui.PRIMARY));
        TextView t = Ui.text(this, text, 13, Ui.TEXT_SECONDARY, 400);
        s.addView(t);
        return s;
    }
}
