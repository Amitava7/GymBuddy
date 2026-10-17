package com.gymbuddy.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Name a fresh workout and start it. */
public final class NewWorkoutActivity extends Screen {

    private EditText name;
    private TextView start;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        final long gymId = longExtra("gymId");
        header("New Workout", true);

        LinearLayout col = Ui.column(this);
        Ui.pad(col, 24, 44, 24, 24);
        TextView label = Ui.spaced(Ui.text(this, "WORKOUT NAME", 13, Ui.TEXT_SECONDARY, 700), 1, 13);
        col.addView(label, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 0, 0, 10));
        name = Ui.input(this, "e.g. Push Day, Leg Day...", 18, 18, 14, Ui.SURFACE, false);
        col.addView(name, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 28));

        start = Ui.text(this, "Start Workout", 18, Ui.BACKGROUND, 700);
        start.setGravity(Gravity.CENTER);
        start.setBackground(Ui.box(this, Ui.PRIMARY, 14));
        Ui.pad(start, 0, 18, 0, 18);
        Ui.touch(start, 0.8f, 0.3f, new View.OnClickListener() {
            public void onClick(View v) {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) return;
                long workoutId = db.startWorkout(n, gymId, null);
                replace(WorkoutActivity.class, "gymId", gymId, "workoutId", workoutId);
            }
        });
        start.setEnabled(false);
        col.addView(start, Ui.lp(Ui.MATCH, Ui.WRAP));
        name.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                start.setEnabled(!s.toString().trim().isEmpty());
            }
        });
        content.addView(col);
        Ui.showKeyboard(name);
    }

    @Override
    void load() {
    }
}
