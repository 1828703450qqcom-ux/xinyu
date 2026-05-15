package com.xinyu.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class TrendChartView extends View {

    private Paint linePaint;
    private Paint dotPaint;
    private Paint textPaint;
    private Paint gridPaint;
    private Paint fillPaint;
    private Paint labelPaint;

    private List<Float> scores = new ArrayList<>();
    private List<String> dates = new ArrayList<>();
    private int maxScore = 27;

    public TrendChartView(Context context) {
        super(context);
        init();
    }

    public TrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TrendChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFFFF8A9B);
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(0xFFFF8A9B);
        dotPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF4A3728);
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xFFF0E6DC);
        gridPaint.setStrokeWidth(1f);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFFC4B5A8);
        labelPaint.setTextSize(20f);
        labelPaint.setTextAlign(Paint.Align.RIGHT);
    }

    public void setData(List<Float> scores, List<String> dates, int maxScore) {
        this.scores = scores;
        this.dates = dates;
        this.maxScore = maxScore;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (scores.size() < 2) {
            textPaint.setTextSize(28f);
            textPaint.setColor(0xFFC4B5A8);
            canvas.drawText("完成更多测评后查看趋势", getWidth() / 2f, getHeight() / 2f, textPaint);
            textPaint.setColor(0xFF4A3728);
            textPaint.setTextSize(24f);
            return;
        }

        float padding = 40f;
        float left = padding + 30;
        float right = getWidth() - padding;
        float top = padding;
        float bottom = getHeight() - padding - 10;
        float chartWidth = right - left;
        float chartHeight = bottom - top;

        // Draw grid lines
        for (int i = 0; i <= 4; i++) {
            float y = bottom - (chartHeight * i / 4f);
            canvas.drawLine(left, y, right, y, gridPaint);
            int labelVal = maxScore * i / 4;
            canvas.drawText(String.valueOf(labelVal), left - 8, y + 6, labelPaint);
        }

        // Calculate points
        float[] xPoints = new float[scores.size()];
        float[] yPoints = new float[scores.size()];

        for (int i = 0; i < scores.size(); i++) {
            xPoints[i] = left + (chartWidth * i / (scores.size() - 1));
            yPoints[i] = bottom - (scores.get(i) / maxScore * chartHeight);
        }

        // Draw fill gradient
        Path fillPath = new Path();
        fillPath.moveTo(xPoints[0], bottom);
        for (int i = 0; i < xPoints.length; i++) {
            fillPath.lineTo(xPoints[i], yPoints[i]);
        }
        fillPath.lineTo(xPoints[xPoints.length - 1], bottom);
        fillPath.close();
        fillPaint.setColor(0x20FF8A9B);
        canvas.drawPath(fillPath, fillPaint);

        // Draw line
        Path linePath = new Path();
        linePath.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < xPoints.length; i++) {
            linePath.lineTo(xPoints[i], yPoints[i]);
        }
        canvas.drawPath(linePath, linePaint);

        // Draw dots and labels
        for (int i = 0; i < xPoints.length; i++) {
            // Outer dot
            canvas.drawCircle(xPoints[i], yPoints[i], 8f, dotPaint);
            // Inner white dot
            Paint innerDot = new Paint(Paint.ANTI_ALIAS_FLAG);
            innerDot.setColor(Color.WHITE);
            canvas.drawCircle(xPoints[i], yPoints[i], 4f, innerDot);

            // Score label on top
            textPaint.setTextSize(20f);
            textPaint.setFakeBoldText(true);
            canvas.drawText(String.valueOf((int) scores.get(i).floatValue()), xPoints[i], yPoints[i] - 16, textPaint);

            // Date label at bottom
            if (dates.size() > i) {
                labelPaint.setTextSize(16f);
                canvas.drawText(dates.get(i), xPoints[i], bottom + 24, labelPaint);
            }
        }
        textPaint.setFakeBoldText(false);
    }
}
