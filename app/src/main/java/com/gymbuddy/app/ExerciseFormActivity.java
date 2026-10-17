package com.gymbuddy.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** New Exercise / Edit Exercise. */
public final class ExerciseFormActivity extends Screen {

    private long exerciseId;
    private EditText name, details;
    private final List<String> selected = new ArrayList<String>();
    private final List<TextView> chips = new ArrayList<TextView>();
    private TextView save;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        exerciseId = longExtra("exerciseId");
        final boolean edit = exerciseId != 0;
        header(edit ? "Edit Exercise" : "New Exercise", true);

        ScrollView scroll = new ScrollView(this);
        LinearLayout col = Ui.column(this);
        Ui.pad(col, 24, 24, 24, 48);
        col.addView(label("EXERCISE NAME"));
        name = Ui.input(this, "e.g. Bench Press", 16, 16, 14, Ui.SURFACE, false);
        col.addView(name, Ui.lp(Ui.MATCH, Ui.WRAP));
        col.addView(label("DETAILS / INSTRUCTIONS"));
        details = Ui.input(this, "e.g. Lie flat on bench, grip slightly wider than shoulders...",
                16, 16, 14, Ui.SURFACE, true);
        details.setMinHeight(Ui.dp(this, 100));
        details.setMinLines(4);
        col.addView(details, Ui.lp(Ui.MATCH, Ui.WRAP));
        col.addView(label("MUSCLE GROUPS"));
        col.addView(tagRow(Db.MUSCLE_TAGS), Ui.lp(Ui.MATCH, Ui.WRAP));
        col.addView(label("EQUIPMENT"));
        col.addView(tagRow(Db.EQUIPMENT_TAGS), Ui.lp(Ui.MATCH, Ui.WRAP));

        save = Ui.text(this, (edit ? "Update" : "Create") + " Exercise", 17, Ui.BACKGROUND, 700);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.box(this, Ui.PRIMARY, 14));
        Ui.pad(save, 0, 16, 0, 16);
        Ui.touch(save, 0.8f, 0.3f, new View.OnClickListener() {
            public void onClick(View v) {
                save(edit);
            }
        });
        save.setEnabled(false);
        col.addView(save, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 32, 0, 0));
        name.addTextChangedListener(new WorkoutActivity.Watcher() {
            public void afterTextChanged(Editable s) {
                save.setEnabled(!s.toString().trim().isEmpty());
            }
        });
        scroll.addView(col);
        content.addView(scroll);

        if (edit) {
            Db.Exercise ex = db.getExercise(exerciseId);
            if (ex != null) {
                name.setText(ex.name);
                details.setText(ex.details == null ? "" : ex.details);
                if (ex.tags != null) {
                    for (String t : ex.tags.split(",")) if (!t.isEmpty()) selected.add(t);
                }
                for (TextView c : chips) Ui.setChip(c, selected.contains((String) c.getTag()));
            }
        }
        Ui.showKeyboard(name);
    }

    private TextView label(String s) {
        TextView t = Ui.spaced(Ui.text(this, s, 14, Ui.TEXT_SECONDARY, 600), 1, 14);
        t.setLayoutParams(Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 16, 0, 8));
        return t;
    }

    private View tagRow(String[] tags) {
        Flow row = new Flow(this, 8);
        for (final String tag : Arrays.asList(tags)) {
            final TextView chip = Ui.chip(this, Ui.capitalize(tag), 14, false);
            chip.setTag(tag);
            Ui.pad(chip, 14, 8, 14, 8);
            Ui.touch(chip, 0.7f, new View.OnClickListener() {
                public void onClick(View v) {
                    if (!selected.remove(tag)) selected.add(tag);
                    Ui.setChip(chip, selected.contains(tag));
                }
            });
            chips.add(chip);
            row.addView(chip);
        }
        return row;
    }

    private void save(boolean edit) {
        String n = name.getText().toString().trim();
        if (n.isEmpty()) return;
        String d = details.getText().toString().trim();
        String tags = selected.isEmpty() ? null : TextUtils.join(",", selected);
        if (edit) db.updateExercise(exerciseId, n, d, tags);
        else db.createExercise(n, d, tags);
        Ui.hideKeyboard(this);
        finish();
    }

    @Override
    void load() {
    }
}
