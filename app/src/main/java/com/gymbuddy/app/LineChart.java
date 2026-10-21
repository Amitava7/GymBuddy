package com.gymbuddy.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A line through every session, oldest on the left, one dot for each time the
 * exercise was done. Sessions are spaced evenly so each one gets its own dot.
 * Dates sit under the first and last session and one in between; a marked
 * session (the last time the weight went up) gets a shaded band from it to the
 * end and its date under it instead. A few gridlines at round values give the
 * scale. Drawn by hand: there is no chart library in the app.
 */
final class LineChart extends View {

    private final double[] values;
    private final String[] dates;
    private final double lo, hi, step;
    private int marked = -1;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint();
    private final Paint band = new Paint();
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    /** {@code dates} has the label for each point. */
    LineChart(Context c, double[] values, String[] dates) {
        super(c);
        this.values = values;
        this.dates = dates;

        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (max == min) {   // a flat line sits in the middle
            double pad = Math.max(1, Math.abs(max) * 0.1);
            min = Math.max(0, min - pad);
            max += pad;
        }
        step = niceStep((max - min) / 3);
        lo = Math.floor(min / step) * step;
        hi = Math.ceil(max / step) * step;

        line.setColor(Ui.PRIMARY);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 2));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        grid.setColor(Ui.BORDER);
        grid.setStrokeWidth(Math.max(1, Ui.dp(c, 1)));
        band.setColor(Ui.PRIMARY_15);
        label.setTypeface(Ui.weight(400));
        label.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10,
                c.getResources().getDisplayMetrics()));
        label.setFontFeatureSettings("tnum");
    }

    int size() {
        return values.length;
    }

    /** Shades from this point to the end and puts its date under it. */
    void mark(int index) {
        marked = index > 0 && index < values.length ? index : -1;
        invalidate();
    }

    int marked() {
        return marked;
    }

    /** 1, 2, 2.5 or 5 times a power of ten, and never under 1 (no 0.5 kg gridlines). */
    static double niceStep(double raw) {
        if (raw <= 1) return 1;
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double f = raw / mag;
        return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10) * mag;
    }

    private float dp(float v) {
        return Ui.dp(getContext(), v);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        Paint.FontMetrics fm = label.getFontMetrics();
        float textH = fm.descent - fm.ascent;

        int ticks = (int) Math.round((hi - lo) / step) + 1;
        String[] tickText = new String[ticks];
        float tickW = 0;
        for (int i = 0; i < ticks; i++) {
            tickText[i] = Fmt.num(Math.round((lo + i * step) * 1000) / 1000.0);
            tickW = Math.max(tickW, label.measureText(tickText[i]));
        }

        float plotLeft = tickW + dp(8);
        float top = textH / 2 + dp(4);
        float bottom = h - textH - dp(8);

        // dots shrink and lose their ring once sessions crowd together
        int n = values.length;
        float ring = dp(2), r = dp(4);
        float left = plotLeft + r + ring, right = w - r - ring;
        float gap = n > 1 ? (right - left) / (n - 1) : right - left;
        if (gap < dp(12)) {
            r = Math.max(dp(1.5f), Math.min(r, gap / 3));
            ring = r >= dp(3) ? ring : 0;
        }

        // since the last increase: a band behind the line, and a hairline where it starts
        if (marked > 0) {
            float mx = x(marked, n, left, right);
            canvas.drawRect(mx - gap / 2, top, w, bottom, band);
            line.setAlpha(0x66);
            line.setStrokeWidth(Math.max(1, dp(1)));
            canvas.drawLine(mx, top, mx, bottom, line);
            line.setAlpha(0xFF);
            line.setStrokeWidth(dp(2));
        }

        // gridlines and their values, right-aligned in the left margin
        label.setColor(Ui.TEXT_SECONDARY);
        label.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i < ticks; i++) {
            float y = y(lo + i * step, top, bottom);
            canvas.drawLine(plotLeft, y, w, y, grid);
            canvas.drawText(tickText[i], tickW, y - (fm.ascent + fm.descent) / 2, label);
        }

        path.reset();
        for (int i = 0; i < n; i++) {
            float x = x(i, n, left, right), y = y(values[i], top, bottom);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        if (n > 1) canvas.drawPath(path, line);
        for (int i = 0; i < n; i++) {
            float x = x(i, n, left, right), y = y(values[i], top, bottom);
            if (ring > 0) {
                dot.setColor(Ui.SURFACE);
                canvas.drawCircle(x, y, r + ring, dot);
            }
            dot.setColor(Ui.PRIMARY);
            canvas.drawCircle(x, y, r, dot);
        }

        // dates under the points, most important first (when the weight last
        // went up, then the latest session), none on top of another
        float baseline = h - fm.descent;
        List<RectF> taken = new ArrayList<RectF>();
        if (marked > 0) dateLabel(canvas, marked, x(marked, n, left, right), 0, baseline, plotLeft, w, taken);
        dateLabel(canvas, n - 1, n == 1 ? x(0, n, left, right) : w, n == 1 ? 0 : 1, baseline, plotLeft, w, taken);
        dateLabel(canvas, 0, plotLeft, -1, baseline, plotLeft, w, taken);
        if (marked < 0 && n > 2) dateLabel(canvas, n / 2, x(n / 2, n, left, right), 0, baseline, plotLeft, w, taken);
    }

    /**
     * Draws a point's date at x: left-aligned (-1), centred (0) or
     * right-aligned (1), kept inside the chart, and skipped if it would run
     * into one already drawn. The marked point's date is brighter.
     */
    private void dateLabel(Canvas canvas, int i, float x, int align, float baseline, float minX, float maxX,
                           List<RectF> taken) {
        String text = dates[i];
        float tw = label.measureText(text);
        float start = align < 0 ? x : align > 0 ? x - tw : x - tw / 2;
        start = Math.max(minX, Math.min(start, maxX - tw));
        RectF rect = new RectF(start - dp(6), 0, start + tw + dp(6), 1);
        for (RectF t : taken) {
            if (RectF.intersects(t, rect)) return;
        }
        taken.add(rect);
        label.setColor(i == marked ? Ui.TEXT : Ui.TEXT_SECONDARY);
        label.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(text, start, baseline, label);
    }

    private static float x(int i, int n, float left, float right) {
        return n == 1 ? (left + right) / 2 : left + i * (right - left) / (n - 1);
    }

    private float y(double v, float top, float bottom) {
        return (float) (bottom - (v - lo) / (hi - lo) * (bottom - top));
    }
}
