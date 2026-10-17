package com.gymbuddy.app;

import android.icu.text.DateFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Dates, durations and numbers, formatted the way the React Native screens
 * showed them. Hermes formats dates through the phone's ICU, so asking ICU for
 * the same skeletons gives the same text.
 */
final class Fmt {

    private Fmt() {
    }

    /** JavaScript's String(number): 5, 12.5. */
    static String num(Double d) {
        return d == null ? "" : Json.number(d);
    }

    static String num(double d) {
        return Json.number(d);
    }

    /** SQLite's datetime('now') text is UTC without a zone: new Date(s + 'Z'). */
    static Date utc(String s) {
        if (s == null) return null;
        for (String p : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"}) {
            SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            f.setLenient(false);
            try {
                return f.parse(s);
            } catch (ParseException ignored) {
                // try the next shape
            }
        }
        return null;
    }

    /** An ISO timestamp such as the backup's exported_at. */
    static Date iso(Object v) {
        if (!(v instanceof String)) return null;
        String s = (String) v;
        for (String p : new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd'T'HH:mm:ss.SSS"}) {
            SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                return f.parse(s);
            } catch (ParseException ignored) {
                // try the next shape
            }
        }
        return utc(s);
    }

    private static String skeleton(Date d, String skeleton, Locale locale) {
        if (d == null) return "Invalid Date";
        return DateFormat.getInstanceForSkeleton(skeleton, locale).format(d);
    }

    /** History list: "Apr 7, 2026, 04:45 PM". */
    static String historyDate(String s) {
        return skeleton(utc(s), "yMMMdhhmm", Locale.US);
    }

    /** Workout summary: "Tuesday, April 7". */
    static String longDay(String s) {
        return skeleton(utc(s), "EEEEMMMMd", Locale.US);
    }

    /** Exercise history: "Apr 7, 2026". */
    static String shortDate(String s) {
        return skeleton(utc(s), "yMMMd", Locale.US);
    }

    /** Date.toLocaleString() in the phone's own locale. */
    static String localDateTime(Date d) {
        return skeleton(d, "yMdjms", Locale.getDefault());
    }

    /** History cards: "1h 12m" or "25m". */
    static String durationShort(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60;
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    /** Summary: "1h 12m" or "25m 35s". */
    static String durationLong(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h > 0 ? h + "h " + m + "m" : m + "m " + s + "s";
    }

    /** The running workout clock: "1:02:09" or "02:09". */
    static String clock(long s) {
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return (h > 0 ? h + ":" : "") + String.format(Locale.US, "%02d:%02d", m, sec);
    }
}
