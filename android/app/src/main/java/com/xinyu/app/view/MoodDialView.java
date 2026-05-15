package com.xinyu.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MoodDialView extends View {

    public interface OnMoodChangeListener {
        void onMoodChanged(int moodValue, String label, String emoji);
    }

    private OnMoodChangeListener listener;
    private int currentMood = 3; // 1-5, default 平静
    private float currentAngle = 135f; // 0-270 degrees

    private final Paint bgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private static final float START_ANGLE = 135f;
    private static final float SWEEP_ANGLE = 270f;
    private static final String[] EMOJIS = {"😢", "😰", "😐", "😊", "😄"};
    private static final String[] LABELS = {"难过", "焦虑", "平静", "开心", "超棒"};
    private static final int[] COLORS = {
        0xFF64748B, // 难过 - gray
        0xFFF59E0B, // 焦虑 - amber
        0xFF94A3B8, // 平静 - slate
        0xFF10B981, // 开心 - green
        0xFF6366F1, // 超棒 - indigo
    };

    public MoodDialView(Context context) {
        super(context);
        init();
    }

    public MoodDialView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoodDialView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgArcPaint.setStyle(Paint.Style.STROKE);
        bgArcPaint.setStrokeWidth(20f);
        bgArcPaint.setColor(0xFFE2E8F0);
        bgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(20f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        pointerPaint.setStyle(Paint.Style.FILL);
        pointerPaint.setColor(0xFF6366F1);

        centerPaint.setStyle(Paint.Style.FILL);

        textPaint.setTextSize(40f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(0xFF1E293B);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(cx, cy) - 30f;

        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // 背景圆弧
        canvas.drawArc(arcRect, START_ANGLE, SWEEP_ANGLE, false, bgArcPaint);

        // 彩色进度弧
        float progressSweep = (currentAngle / 270f) * SWEEP_ANGLE;
        int color = COLORS[currentMood - 1];
        progressPaint.setColor(color);
        canvas.drawArc(arcRect, START_ANGLE, progressSweep, false, progressPaint);

        // 指针线
        float rad = (float) Math.toRadians(START_ANGLE + (currentAngle / 270f) * SWEEP_ANGLE);
        float innerR = radius * 0.35f;
        float outerR = radius - 20f;
        float px1 = cx + (float) Math.sin(rad) * innerR;
        float cy1 = cy - (float) Math.cos(rad) * innerR;
        float px2 = cx + (float) Math.sin(rad) * outerR;
        float cy2 = cy - (float) Math.cos(rad) * outerR;

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(color);
        linePaint.setStrokeWidth(4f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(px1, cy1, px2, cy2, linePaint);

        // 中心圆点
        centerPaint.setColor(color);
        canvas.drawCircle(cx, cy, 10f, centerPaint);
        centerPaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, 5f, centerPaint);

        // Emoji
        textPaint.setTextSize(radius * 0.35f);
        textPaint.setColor(0xFF1E293B);
        canvas.drawText(EMOJIS[currentMood - 1], cx, cy + radius * 0.15f, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - cx;
                float dy = event.getY() - cy;
                float angle = (float) Math.toDegrees(Math.atan2(dx, -dy));
                if (angle < 0) angle += 360;
                if (angle > 270) angle = angle > 315 ? 0 : 270;
                angle = Math.max(0, Math.min(270, angle));

                currentAngle = angle;
                int mood = Math.round((angle / 270f) * 4) + 1;
                mood = Math.max(1, Math.min(5, mood));

                if (mood != currentMood) {
                    currentMood = mood;
                    if (listener != null) {
                        listener.onMoodChanged(currentMood, LABELS[currentMood - 1], EMOJIS[currentMood - 1]);
                    }
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setMoodValue(int value) {
        currentMood = Math.max(1, Math.min(5, value));
        currentAngle = ((currentMood - 1) / 4f) * 270f;
        invalidate();
    }

    public int getMoodValue() {
        return currentMood;
    }

    public String getMoodLabel() {
        return LABELS[currentMood - 1];
    }

    public String getMoodEmoji() {
        return EMOJIS[currentMood - 1];
    }

    public void setOnMoodChangeListener(OnMoodChangeListener listener) {
        this.listener = listener;
    }
}
