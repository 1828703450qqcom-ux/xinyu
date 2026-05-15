package com.xinyu.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MoodChartView extends View {

    private Paint linePaint, dotPaint, textPaint, gridPaint, fillPaint;
    private List<Float> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private String title = "情绪趋势";

    public MoodChartView(Context context) {
        super(context);
        init();
    }

    public MoodChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#FF8A9B"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#FF8A9B"));
        dotPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#8D6E63"));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F0E8E0"));
        gridPaint.setStrokeWidth(1f);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Float> values, List<String> labels) {
        this.values = values;
        this.labels = labels;
        invalidate();
    }

    public void setTitle(String title) {
        this.title = title;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.size() < 2) {
            textPaint.setTextSize(32f);
            canvas.drawText("记录更多心情后显示趋势", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        float w = getWidth();
        float h = getHeight();
        float padding = 60f;
        float topPadding = 80f;
        float bottomPadding = 80f;

        // Draw title
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(title, padding, topPadding - 20f, textPaint);

        float chartLeft = padding;
        float chartRight = w - padding;
        float chartTop = topPadding + 20f;
        float chartBottom = h - bottomPadding;
        float chartWidth = chartRight - chartLeft;
        float chartHeight = chartBottom - chartTop;

        // Draw grid lines
        for (int i = 0; i <= 4; i++) {
            float y = chartTop + chartHeight * i / 4f;
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
        }

        // Calculate points
        float maxVal = 100f;
        float minVal = 0f;
        float stepX = chartWidth / (values.size() - 1);

        Path linePath = new Path();
        Path fillPath = new Path();
        float firstX = chartLeft;
        float firstY = chartBottom - (values.get(0) - minVal) / (maxVal - minVal) * chartHeight;
        linePath.moveTo(firstX, firstY);
        fillPath.moveTo(firstX, chartBottom);
        fillPath.lineTo(firstX, firstY);

        for (int i = 1; i < values.size(); i++) {
            float x = chartLeft + stepX * i;
            float y = chartBottom - (values.get(i) - minVal) / (maxVal - minVal) * chartHeight;
            linePath.lineTo(x, y);
            fillPath.lineTo(x, y);
        }

        fillPath.lineTo(chartRight, chartBottom);
        fillPath.close();

        // Draw fill
        int fillColor = Color.parseColor("#20FF8A9B");
        fillPaint.setColor(fillColor);
        canvas.drawPath(fillPath, fillPaint);

        // Draw line
        canvas.drawPath(linePath, linePaint);

        // Draw dots and labels
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < values.size(); i++) {
            float x = chartLeft + stepX * i;
            float y = chartBottom - (values.get(i) - minVal) / (maxVal - minVal) * chartHeight;
            canvas.drawCircle(x, y, 8f, dotPaint);

            // Draw white inner dot
            Paint whiteDot = new Paint(Paint.ANTI_ALIAS_FLAG);
            whiteDot.setColor(Color.WHITE);
            canvas.drawCircle(x, y, 4f, whiteDot);

            // Draw label
            if (labels.size() > i) {
                canvas.drawText(labels.get(i), x, chartBottom + 40f, textPaint);
            }
        }
    }
}
