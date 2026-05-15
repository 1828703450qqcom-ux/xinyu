package com.xinyu.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<Float> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private List<Integer> colors = new ArrayList<>();
    private float maxValue = 100f;

    public BarChartView(Context context) { this(context, null); }
    public BarChartView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public BarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        textPaint.setColor(Color.parseColor("#4A3728"));
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(Color.parseColor("#B0A89E"));
        labelPaint.setTextSize(28f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        bgPaint.setColor(Color.parseColor("#F5F0EB"));
    }

    public void setData(List<Float> values, List<String> labels, List<Integer> colors, float maxValue) {
        this.values = values;
        this.labels = labels;
        this.colors = colors;
        this.maxValue = maxValue;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.isEmpty()) return;

        float w = getWidth();
        float h = getHeight();
        float padding = 40f;
        float barAreaWidth = w - padding * 2;
        float barWidth = barAreaWidth / (values.size() * 2f);
        float chartHeight = h - padding * 3;

        for (int i = 0; i < values.size(); i++) {
            float ratio = Math.min(values.get(i) / maxValue, 1f);
            float barHeight = chartHeight * ratio;
            float x = padding + barAreaWidth * (i + 0.5f) / values.size();
            float top = h - padding * 1.5f - barHeight;
            float bottom = h - padding * 1.5f;

            // Background bar
            RectF bgRect = new RectF(x - barWidth, top, x + barWidth, bottom);
            canvas.drawRoundRect(bgRect, 12f, 12f, bgPaint);

            // Colored bar with gradient
            if (i < colors.size()) {
                barPaint.setShader(new LinearGradient(x, top, x, bottom,
                    colors.get(i), shadeColor(colors.get(i), 0.7f), Shader.TileMode.CLAMP));
            }
            RectF barRect = new RectF(x - barWidth, top, x + barWidth, bottom);
            canvas.drawRoundRect(barRect, 12f, 12f, barPaint);
            barPaint.setShader(null);

            // Score text above bar
            textPaint.setTextSize(28f);
            canvas.drawText(String.valueOf((int)(float)values.get(i)), x, top - 16f, textPaint);

            // Label below bar
            labelPaint.setTextSize(24f);
            canvas.drawText(labels.get(i), x, bottom + 36f, labelPaint);
        }
    }

    private int shadeColor(int color, float factor) {
        int r = (int)(Color.red(color) * factor);
        int g = (int)(Color.green(color) * factor);
        int b = (int)(Color.blue(color) * factor);
        return Color.rgb(r, g, b);
    }
}
