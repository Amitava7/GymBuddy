package com.gymbuddy.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** flexDirection: 'row', flexWrap: 'wrap' with a gap: children left to right, wrapping. */
final class Flow extends ViewGroup {

    private final int gap;

    Flow(Context c, float gapDp) {
        super(c);
        gap = Ui.dp(c, gapDp);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int max = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
        boolean bounded = MeasureSpec.getMode(widthSpec) != MeasureSpec.UNSPECIFIED;
        int x = 0, y = 0, lineH = 0, widest = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            measureChild(ch, MeasureSpec.makeMeasureSpec(Math.max(0, max), bounded
                    ? MeasureSpec.AT_MOST : MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (x > 0 && bounded && x + w > max) {
                x = 0;
                y += lineH + gap;
                lineH = 0;
            }
            x += w + gap;
            widest = Math.max(widest, x - gap);
            lineH = Math.max(lineH, h);
        }
        int width = bounded && MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(widthSpec) : widest + getPaddingLeft() + getPaddingRight();
        setMeasuredDimension(width, y + lineH + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int max = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, lineH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (x > 0 && x + w > max) {
                x = 0;
                y += lineH + gap;
                lineH = 0;
            }
            ch.layout(getPaddingLeft() + x, getPaddingTop() + y,
                    getPaddingLeft() + x + w, getPaddingTop() + y + h);
            x += w + gap;
            lineH = Math.max(lineH, h);
        }
    }
}
