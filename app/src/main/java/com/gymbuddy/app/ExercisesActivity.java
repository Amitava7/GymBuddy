package com.gymbuddy.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Every exercise, searchable and filterable by muscle and equipment. */
public final class ExercisesActivity extends Screen {

    private EditText search;
    private final List<String> activeTags = new ArrayList<String>();
    private ListView list;
    private LinearLayout emptyView;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        header("Exercises", true);
        LinearLayout col = Ui.column(this);
        Runnable reload = new Runnable() {
            public void run() {
                load();
            }
        };
        search = searchBar(col, reload);
        col.addView(filterChips(activeTags, 0, 5, 5, reload), Ui.lp(Ui.MATCH, Ui.WRAP));

        FrameLayout area = new FrameLayout(this);
        list = listView(20, 4, 20, 100);
        area.addView(list, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        emptyView = empty(R.drawable.ic_barbell_outline, "No exercises yet", "Tap + to create one", 12);
        emptyText = (TextView) emptyView.getChildAt(1);
        area.addView(emptyView, new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.CENTER));
        col.addView(area, Ui.lp(Ui.MATCH, 0, 1f));
        content.addView(col);
        content.addView(Ui.fab(this, new View.OnClickListener() {
            public void onClick(View v) {
                open(ExerciseFormActivity.class);
            }
        }));
    }

    @Override
    void load() {
        String q = search.getText().toString();
        List<Db.Exercise> items = db.getExercises(q, activeTags);
        boolean filtered = !q.isEmpty() || !activeTags.isEmpty();
        emptyText.setText(filtered ? "No exercises found" : "No exercises yet");
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        setItems(list, items, 8, new Render<Db.Exercise>() {
            public View view(final Db.Exercise e) {
                LinearLayout card = Ui.row(ExercisesActivity.this);
                card.setBackground(Ui.card(ExercisesActivity.this, 14));
                Ui.pad(card, 14, 14, 14, 14);
                View box = Ui.iconBox(ExercisesActivity.this, R.drawable.ic_barbell, 40, 10, 20);
                ((LinearLayout.LayoutParams) box.getLayoutParams()).setMarginEnd(Ui.dp(ExercisesActivity.this, 12));
                card.addView(box);
                card.addView(exerciseText(e), Ui.lp(0, Ui.WRAP, 1f));
                card.addView(Ui.icon(ExercisesActivity.this, R.drawable.ic_chevron_forward, 18, Ui.TEXT_LIGHT));
                Ui.touch(card, 0.7f, new View.OnClickListener() {
                    public void onClick(View v) {
                        open(ExerciseActivity.class, "exerciseId", e.id);
                    }
                });
                Ui.longPress(card, new Runnable() {
                    public void run() {
                        confirmDelete(e);
                    }
                });
                return card;
            }
        });
    }

    private void confirmDelete(final Db.Exercise e) {
        alert("Delete Exercise", "Delete \"" + e.name + "\"?", new String[]{"Cancel", "Delete"},
                null, new Runnable() {
                    public void run() {
                        db.deleteExercise(e.id);
                        load();
                    }
                });
    }
}
