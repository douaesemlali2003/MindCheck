package com.example.quizapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MiniChartView extends View {

    public static class Bar {
        public final float value;
        public final float maxValue;
        public final String label;
        public final int color;

        public Bar(float value, float maxValue, String label, int color) {
            this.value    = value;
            this.maxValue = maxValue;
            this.label    = label;
            this.color    = color;
        }
    }

    private final List<Bar> bars = new ArrayList<>();

    private final Paint barPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final float CORNER_RADIUS  = 12f;
    private static final float LABEL_TEXT_SP  = 10f;
    private static final float VALUE_TEXT_SP  = 11f;

    public MiniChartView(Context ctx) {
        super(ctx);
        init(ctx);
    }

    public MiniChartView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init(ctx);
    }

    public MiniChartView(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        init(ctx);
    }

    private void init(Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;

        trackPaint.setColor(0xFFEDE8DD);

        labelPaint.setColor(0xFF9CA89C);
        labelPaint.setTextSize(LABEL_TEXT_SP * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint.setColor(0xFF2D3B2D);
        valuePaint.setTextSize(VALUE_TEXT_SP * density);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
    }

    public void setBars(List<Bar> data) {
        bars.clear();
        bars.addAll(data);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, (int)(150 * getResources().getDisplayMetrics().density));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bars.isEmpty()) return;

        float density   = getResources().getDisplayMetrics().density;
        float w         = getWidth();
        float h         = getHeight();

        float labelH    = 16f * density;
        float valueH    = 14f * density;
        float gapBottom = labelH + 4f * density;
        float gapTop    = valueH + 6f * density;
        float chartH    = h - gapBottom - gapTop;

        int   n         = bars.size();
        float totalGap  = 8f * density * (n + 1);
        float barW      = (w - totalGap) / n;
        float gapW      = 8f * density;

        for (int i = 0; i < n; i++) {
            Bar   bar    = bars.get(i);
            float ratio  = bar.maxValue > 0 ? Math.min(bar.value / bar.maxValue, 1f) : 0f;
            float left   = gapW + i * (barW + gapW);
            float right  = left + barW;
            float top    = gapTop + chartH * (1f - ratio);
            float bottom = gapTop + chartH;

            // Track (fond gris clair)
            RectF trackRect = new RectF(left, gapTop, right, bottom);
            canvas.drawRoundRect(trackRect, CORNER_RADIUS, CORNER_RADIUS, trackPaint);

            // Bar colorée
            if (ratio > 0f) {
                barPaint.setColor(bar.color);
                RectF barRect = new RectF(left, top, right, bottom);
                canvas.drawRoundRect(barRect, CORNER_RADIUS, CORNER_RADIUS, barPaint);
            }

            // Valeur au-dessus
            String valStr = bar.value > 0 ? String.valueOf((int) bar.value) : "-";
            float cx = left + barW / 2f;
            canvas.drawText(valStr, cx, gapTop - 4f * density, valuePaint);

            // Label en dessous
            canvas.drawText(bar.label, cx, h - 2f * density, labelPaint);
        }
    }
}
