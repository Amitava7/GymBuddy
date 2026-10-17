#!/usr/bin/env python3
"""Write app/src/test/resources/sample-backup.json.

A made-up backup in exactly the shape the React Native app exported
(JSON.stringify(backup, null, 2), version 1), with the awkward parts real data
has: ids with gaps, unfinished workouts, workouts without a template, sets with
half kilos and blank fields, custom exercises without tags, a duplicated
template entry, quotes and emoji in names. The unit tests and the emulator
smoke test both import it.

  python3 tools/make_sample_backup.py
"""

import json
import os
import random
from datetime import datetime, timedelta

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(os.path.dirname(HERE), "app", "src", "test", "resources", "sample-backup.json")

SEED = [
    ("Bench Press", None, "chest,barbell"), ("Incline Bench Press", None, "chest,barbell"),
    ("Decline Bench Press", None, "chest,barbell"), ("Dumbbell Bench Press", None, "chest,dumbbell"),
    ("Incline Dumbbell Press", None, "chest,dumbbell"), ("Dumbbell Fly", None, "chest,dumbbell"),
    ("Cable Fly", None, "chest,cable"), ("Chest Press Machine", None, "chest,machine"),
    ("Pec Deck", None, "chest,machine"), ("Push Up", None, "chest,bodyweight"),
    ("Chest Dip", None, "chest,bodyweight"),
    ("Deadlift", "Keep back straight, drive through heels", "back,barbell,legs"),
    ("Romanian Deadlift", None, "back,barbell,legs"), ("Barbell Row", None, "back,barbell"),
    ("Dumbbell Row", None, "back,dumbbell"), ("Pull Up", None, "back,bodyweight"),
    ("Chin Up", None, "back,arms,bodyweight"), ("Lat Pulldown", None, "back,cable,machine"),
    ("Seated Cable Row", None, "back,cable"), ("T-Bar Row", None, "back,machine"),
    ("Face Pull", None, "back,shoulders,cable"), ("Good Morning", None, "back,barbell"),
    ("Back Extension", None, "back,bodyweight"), ("Overhead Press", None, "shoulders,barbell"),
    ("Dumbbell Shoulder Press", None, "shoulders,dumbbell"), ("Arnold Press", None, "shoulders,dumbbell"),
    ("Lateral Raise", None, "shoulders,dumbbell"), ("Front Raise", None, "shoulders,dumbbell"),
    ("Rear Delt Fly", None, "shoulders,dumbbell"), ("Cable Lateral Raise", None, "shoulders,cable"),
    ("Machine Shoulder Press", None, "shoulders,machine"), ("Upright Row", None, "shoulders,barbell"),
    ("Shrugs", None, "shoulders,barbell"), ("Barbell Curl", None, "arms,barbell"),
    ("Dumbbell Curl", None, "arms,dumbbell"), ("Hammer Curl", None, "arms,dumbbell"),
    ("Preacher Curl", None, "arms,machine"), ("Cable Curl", None, "arms,cable"),
    ("Concentration Curl", None, "arms,dumbbell"), ("Close Grip Bench Press", None, "arms,chest,barbell"),
    ("Skull Crusher", None, "arms,barbell"), ("Tricep Pushdown", None, "arms,cable"),
    ("Overhead Tricep Extension", None, "arms,dumbbell"), ("Tricep Dip", None, "arms,bodyweight"),
    ("Diamond Push Up", None, "arms,chest,bodyweight"),
    ("Squat", "Keep knees tracking over toes, chest up", "legs,barbell"),
    ("Front Squat", None, "legs,barbell"), ("Sumo Deadlift", None, "legs,barbell"),
    ("Leg Press", None, "legs,machine"), ("Leg Extension", None, "legs,machine"),
    ("Leg Curl", None, "legs,machine"), ("Calf Raise", None, "legs,machine"),
    ("Lunges", None, "legs,dumbbell"), ("Bulgarian Split Squat", None, "legs,dumbbell"),
    ("Goblet Squat", None, "legs,kettlebell"), ("Hip Thrust", None, "legs,glutes,barbell"),
    ("Glute Bridge", None, "legs,glutes,bodyweight"), ("Step Up", None, "legs,dumbbell"),
    ("Kettlebell Swing", None, "legs,glutes,kettlebell"), ("Plank", None, "core,bodyweight"),
    ("Side Plank", None, "core,bodyweight"), ("Crunch", None, "core,bodyweight"),
    ("Sit Up", None, "core,bodyweight"), ("Leg Raise", None, "core,bodyweight"),
    ("Russian Twist", None, "core,bodyweight"), ("Ab Rollout", None, "core,bodyweight"),
    ("Cable Crunch", None, "core,cable"), ("Hanging Knee Raise", None, "core,bodyweight"),
]

CUSTOM = [
    ("Rope pushdown", None, None),
    ("Wrist roller", "Roll up slowly, then down", None),
    ("Seated \"Y\" raise \U0001F4AA", None, "shoulders"),
    ("Hip abductor", None, None),
    ("Low row", None, None),
]


def stamp(d):
    return d.strftime("%Y-%m-%d %H:%M:%S")


def main():
    rnd = random.Random(7)
    seeded = datetime(2026, 1, 5, 9, 0, 0)
    exercises = [{"id": i + 1, "name": n, "details": d, "tags": t, "created_at": stamp(seeded)}
                 for i, (n, d, t) in enumerate(SEED)]
    eid = len(exercises) + 2   # one deleted custom exercise leaves a gap
    for n, d, t in CUSTOM:
        exercises.append({"id": eid, "name": n, "details": d, "tags": t,
                          "created_at": stamp(seeded + timedelta(days=eid))})
        eid += 1
    by_name = {e["name"]: e["id"] for e in exercises}

    gyms = [
        {"id": 1, "name": "Home Gym", "location": "Downtown", "created_at": "2026-01-05 10:00:00"},
        {"id": 2, "name": "Hotel gym", "location": None, "created_at": "2026-02-10 18:30:00"},
        {"id": 4, "name": "Park", "location": None, "created_at": "2026-03-01 07:15:00"},
    ]
    plans = [
        (1, 1, "Push day", ["Bench Press", "Pec Deck", "Chest Press Machine", "Rope pushdown"]),
        (2, 1, "Pull day", ["Lat Pulldown", "Low row", "Cable Curl", "Wrist roller"]),
        (4, 1, "Legs", ["Leg Extension", "Leg Curl", "Hip abductor", "Hip abductor", "Leg Press"]),
        (5, 2, "Hotel full body", ["Dumbbell Bench Press", "Seated \"Y\" raise \U0001F4AA", "Goblet Squat"]),
        (6, 4, "Outdoor", ["Pull Up", "Push Up", "Plank"]),
    ]
    templates, template_exercises = [], []
    teid = 1
    for tid, gid, name, names in plans:
        templates.append({"id": tid, "name": name, "gym_id": gid,
                          "created_at": stamp(datetime(2026, 1, 5, 10, 0) + timedelta(days=tid * 3))})
        for order, n in enumerate(names):
            template_exercises.append({"id": teid, "template_id": tid, "exercise_id": by_name[n],
                                       "sort_order": order})
            teid += 1
        teid += 1   # gaps, as removing exercises left them

    workouts, workout_exercises, workout_sets = [], [], []
    wid, weid, sid = 1, 1, 1
    day = datetime(2026, 1, 6, 17, 0, 0)
    notes = ["Level 7", "Seat 4", "Slow negatives", None, None, "Grip wide", None]
    for k in range(36):
        tid, gid, name, names = plans[k % len(plans)]
        start = day + timedelta(days=k * 3, minutes=rnd.randint(0, 50), seconds=rnd.randint(0, 59))
        finished = k % 5 != 3   # some abandoned
        duration = rnd.randint(900, 4800) if finished else 0
        template_id = None if k in (2, 9) else tid
        workouts.append({
            "id": wid, "template_id": template_id, "gym_id": gid,
            "name": "Test" if template_id is None else name,
            "started_at": stamp(start),
            "finished_at": stamp(start + timedelta(seconds=duration)) if finished else None,
            "duration_seconds": duration,
        })
        for order, n in enumerate(names):
            workout_exercises.append({"id": weid, "workout_id": wid, "exercise_id": by_name[n],
                                      "note": notes[(k + order) % len(notes)],
                                      "is_completed": 1 if finished and rnd.random() > 0.15 else 0,
                                      "sort_order": order})
            base = rnd.choice([5, 7.5, 10, 12.5, 14.5, 20, 32.5, 45])
            for s in range(1, rnd.randint(2, 4) + 1):
                kg = base + 2.5 * (k // 6)
                kg = int(kg) if kg == int(kg) else kg
                reps = rnd.randint(6, 15)
                if rnd.random() < 0.04:
                    kg = None
                if rnd.random() < 0.03:
                    reps = None
                workout_sets.append({"id": sid, "workout_exercise_id": weid, "set_number": s,
                                     "kg": kg, "reps": reps})
                sid += 1
            weid += 1
            if rnd.random() < 0.1:
                weid += 1
        wid += 1
        if k % 7 == 6:
            wid += 1

    backup = {
        "app": "gymbuddy",
        "version": 1,
        "exported_at": "2026-10-15T22:54:35.271Z",
        "data": {
            "gyms": gyms,
            "exercises": exercises,
            "workout_templates": templates,
            "template_exercises": template_exercises,
            "workouts": workouts,
            "workout_exercises": workout_exercises,
            "workout_sets": workout_sets,
        },
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write(json.dumps(backup, indent=2, ensure_ascii=False))
    print("%s: %d workouts, %d sets" % (OUT, len(workouts), len(workout_sets)))


if __name__ == "__main__":
    main()
