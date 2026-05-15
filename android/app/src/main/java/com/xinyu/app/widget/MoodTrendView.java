package com.xinyu.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MoodTrendView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<Integer> moodValues = new ArrayList<>();
    private List<String> dates = new ArrayList<>();
    private List<String> emojis = new ArrayList<>();

    private static final String[] MOOD_LABELS = {"", "😢", "😟", "😐", "🙂", "😊"};
    private static final int[] MOOD_COLORS = {
        0, 0xFFEF5350, 0xFFFF9800, 0xFFFFEB3B, 0xFF66BB6A, 0xFF42A5F5
    };

    public MoodTrendView(Context context) { this(context, null); }
    public MoodTrendView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public MoodTrendView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setColor(0xFF7986CB);

        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0xFF7986CB);

        textPaint.setColor(Color.parseColor("#4A3728"));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0xFFE0E0E0);

        labelPaint.setColor(Color.parseColor("#B0A89E"));
        labelPaint.setTextSize(24f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Integer> moodValues, List<String> dates, List<String> emojis) {
        this.moodValues = moodValues;
        this.dates = dates;
        this.emojis = emojis;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (moodValues.size() < 2) {
            textPaint.setTextSize(32f);
            canvas.drawText("记录更多心情后查看趋势", getWidth() / 2f, getHeight() / 2f, textPaint);
            textPaint.setTextSize(28f);
            return;
        }

        float w = getWidth();
        float h = getHeight();
        float padding = 50f;
        float chartW = w - padding * 2;
        float chartH = h - padding * 2;

        // Grid lines (5 levels)
        for (int i = 1; i <= 5; i++) {
            float y = padding + chartH * (1f - (i - 1) / 4f);
            canvas.drawLine(padding, y, w - padding, y, gridPaint);
            labelPaint.setTextSize(22f);
            canvas.drawText(MOOD_LABELS[i], padding - 30f, y + 8f, labelPaint);
        }

        // Draw line path
        Path linePath = new Path();
        Path fillPath = new Path();
        float bottomY = padding + chartH;

        for (int i = 0; i < moodValues.size(); i++) {
            float x = padding + chartW * i / (moodValues.size() - 1);
            float ratio = (moodValues.get(i) - 1) / 4f;
            float y = padding + chartH * (1f - ratio);

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, bottomY);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        // Close fill path
        float lastX = padding + chartW;
        fillPath.lineTo(lastX, bottomY);
        fillPath.close();

        // Gradient fill
        fillPaint.setShader(new LinearGradient(0, padding, 0, bottomY,
            0x407986CB, 0x057986CB, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // Draw line
        canvas.drawPath(linePath, linePaint);

        // Draw dots and emoji
        for (int i = 0; i < moodValues.size(); i++) {
            float x = padding + chartW * i / (moodValues.size() - 1);
            float ratio = (moodValues.get(i) - 1) / 4f;
            float y = padding + chartH * (1f - ratio);

            int color = MOOD_COLORS[Math.min(Math.max(moodValues.get(i), 1), 5)];
            dotPaint.setColor(color);
            canvas.drawCircle(x, y, 10f, dotPaint);
            canvas.drawCircle(x, y, 5f, new Paint(Paint.ANTI_ALIAS_FLAG){{ setColor(Color.WHITE); }});

            // Emoji above dot
            if (emojis != null && i < emojis.size()) {
                textPaint.setTextSize(26f);
                canvas.drawText(emojis.get(i), x, y - 22f, textPaint);
            }

            // Date below
            if (dates != null && i < dates.size()) {
                labelPaint.setTextSize(20f);
                canvas.drawText(dates.get(i), x, bottomY + 28f, labelPaint);
            }
        }
    }
}
