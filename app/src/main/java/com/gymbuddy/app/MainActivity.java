package com.gymbuddy.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Home: the logo, your gyms, and the way to exercises and backups. */
public final class MainActivity extends Screen {

    private LinearLayout list;
    private View emptyView;
    private LinearLayout form;
    private EditText name, location;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout col = Ui.column(this);
        col.addView(topSpacer());

        LinearLayout hero = Ui.column(this);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        Ui.pad(hero, 0, 28, 0, 20);
        hero.addView(logo());
        TextView tagline = Ui.spaced(Ui.text(this, "TRACK. LIFT. REPEAT.", 13, Ui.TEXT_SECONDARY, 400), 3, 13);
        hero.addView(tagline, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 10, 0, 0));
        col.addView(hero);

        LinearLayout header = Ui.row(this);
        Ui.pad(header, 20, 0, 20, 8);
        header.addView(Ui.text(this, "Your Gyms", 18, Ui.TEXT, 700), Ui.lp(0, Ui.WRAP, 1f));
        LinearLayout actions = Ui.gap(Ui.row(this), 8);
        LinearLayout exercises = pill(R.drawable.ic_barbell_outline, "Exercises");
        Ui.touch(exercises, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                open(ExercisesActivity.class);
            }
        });
        LinearLayout backup = pill(R.drawable.ic_swap_vertical_outline, null);
        backup.setContentDescription("Backup and restore");
        Ui.touch(backup, 0.2f, new View.OnClickListener() {
            public void onClick(View v) {
                open(BackupActivity.class);
            }
        });
        actions.addView(exercises);
        actions.addView(backup);
        header.addView(actions);
        col.addView(header);

        emptyView = empty(R.drawable.ic_fitness_outline, "No gyms yet", "Add your first gym to get started", 14);
        col.addView(emptyView, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 60, 0, 0));

        list = Ui.column(this);
        Ui.pad(list, 20, 8, 20, 0);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        col.addView(scroll, Ui.lp(Ui.MATCH, 0, 1f));

        content.addView(col);
        content.addView(Ui.fab(this, new View.OnClickListener() {
            public void onClick(View v) {
                showForm();
            }
        }));
    }

    /** The barbell mark and GYM BUDDY from src/components/Logo.tsx, large size. */
    private View logo() {
        LinearLayout logo = Ui.gap(Ui.column(this), 14);
        logo.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout mark = Ui.gap(Ui.row(this), 4);
        mark.addView(block(16, 36, 4, Ui.PRIMARY));
        mark.addView(block(40, 12, 6, Ui.TEXT));
        mark.addView(block(16, 36, 4, Ui.PRIMARY));
        logo.addView(mark, Ui.lp(Ui.WRAP, Ui.WRAP));
        LinearLayout words = Ui.gap(Ui.row(this), 6);
        words.addView(Ui.spaced(Ui.text(this, "GYM", 28, Ui.TEXT, 900), 4, 28));
        words.addView(Ui.spaced(Ui.text(this, "BUDDY", 28, Ui.PRIMARY, 900), 4, 28));
        logo.addView(words, Ui.lp(Ui.WRAP, Ui.WRAP));
        return logo;
    }

    private View block(float w, float h, float r, int color) {
        View v = new View(this);
        v.setBackground(Ui.box(this, color, r));
        v.setLayoutParams(Ui.lp(Ui.dp(this, w), Ui.dp(this, h)));
        return v;
    }

    @Override
    void load() {
        List<Db.Gym> gyms = db.getGyms();
        emptyView.setVisibility(gyms.isEmpty() && !overlayShown() ? View.VISIBLE : View.GONE);
        list.removeAllViews();
        for (final Db.Gym g : gyms) {
            LinearLayout card = Ui.row(this);
            card.setBackground(Ui.card(this, 14));
            Ui.pad(card, 16, 16, 16, 16);
            View box = Ui.iconBox(this, R.drawable.ic_business, 44, 12, 24);
            ((LinearLayout.LayoutParams) box.getLayoutParams()).setMarginEnd(Ui.dp(this, 14));
            card.addView(box);
            LinearLayout text = Ui.column(this);
            text.addView(Ui.text(this, g.name, 16, Ui.TEXT, 600));
            if (g.location != null && !g.location.isEmpty()) {
                text.addView(Ui.text(this, g.location, 13, Ui.TEXT_SECONDARY, 400),
                        Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 2, 0, 0));
            }
            card.addView(text, Ui.lp(0, Ui.WRAP, 1f));
            card.addView(Ui.icon(this, R.drawable.ic_chevron_forward, 20, Ui.TEXT_LIGHT));
            Ui.touch(card, 0.7f, new View.OnClickListener() {
                public void onClick(View v) {
                    open(GymActivity.class, "gymId", g.id);
                }
            });
            Ui.longPress(card, new Runnable() {
                public void run() {
                    confirmDelete(g);
                }
            });
            list.addView(card, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        }
    }

    private void confirmDelete(final Db.Gym g) {
        alert("Delete Gym", "Delete \"" + g.name + "\"? This will remove all related workouts.",
                new String[]{"Cancel", "Delete"}, null, new Runnable() {
                    public void run() {
                        db.deleteGym(g.id);
                        load();
                    }
                });
    }

    private void showForm() {
        if (form == null) {
            form = formCard("Add Gym");
            name = Ui.input(this, "Gym name", 16, 14, 12, Ui.BACKGROUND, false);
            location = Ui.input(this, "Location (optional)", 16, 14, 12, Ui.BACKGROUND, false);
            form.addView(name, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));
            form.addView(location, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));
            form.addView(formButtons("Add", new View.OnClickListener() {
                public void onClick(View v) {
                    closeForm();
                }
            }, new View.OnClickListener() {
                public void onClick(View v) {
                    create();
                }
            }));
        }
        if (form.getParent() != null) ((ViewGroup) form.getParent()).removeView(form);
        showOverlay(form);
        emptyView.setVisibility(View.GONE);
        Ui.showKeyboard(name);
    }

    private void closeForm() {
        hideOverlay();
        load();
    }

    private void create() {
        String n = name.getText().toString().trim();
        if (n.isEmpty()) return;
        db.createGym(n, location.getText().toString().trim());
        name.setText("");
        location.setText("");
        closeForm();
    }

    @Override
    public void onBackPressed() {
        if (overlayShown()) {
            closeForm();
            return;
        }
        super.onBackPressed();
    }
}
