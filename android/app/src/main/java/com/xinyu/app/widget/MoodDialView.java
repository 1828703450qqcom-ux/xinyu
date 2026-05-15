package com.xinyu.app.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class MoodDialView extends View {

    public interface OnMoodChangeListener {
        void onMoodChanged(int value);
    }

    private OnMoodChangeListener listener;
    private int currentValue = 3;
    private int minValue = 1;
    private int maxValue = 7;

    private final String[] moodLabels = {"惊讶", "受鼓舞", "平静", "厌恶", "害怕", "其他", "开心"};
    private final String[] moodEmojis = {"😲", "💪", "😌", "😖", "😨", "😶", "😊"};

    private final int[] moodColors = {
            0xFF81D4FA, 0xFFA5D6A7, 0xFFC8E6C9,
            0xFFF48FB1, 0xFFCE93D8, 0xFFD7CCC8, 0xFFFFE082
    };

    private Paint arcPaint, dotPaint, selectedDotPaint, glowPaint;
    private Paint emojiPaint, labelPaint, selectedLabelPaint;
    private Paint bgPaint, centerPaint, centerGlowPaint;

    private float centerX, centerY, radius;
    private float startAngle = -90f;
    private float sweepAngle = 360f;
    private float selectedScale = 1f;
    private float currentAngle = 0f;

    public MoodDialView(Context context) { super(context); init(); }
    public MoodDialView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public MoodDialView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);

        selectedDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedDotPaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emojiPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFFB0A89E);
        labelPaint.setTextSize(30f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        selectedLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedLabelPaint.setColor(0xFF4A3728);
        selectedLabelPaint.setTextSize(38f);
        selectedLabelPaint.setTextAlign(Paint.Align.CENTER);
        selectedLabelPaint.setFakeBoldText(true);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setStyle(Paint.Style.FILL);

        centerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerGlowPaint.setStyle(Paint.Style.FILL);

        startEntryAnimation();
    }

    private void startEntryAnimation() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(600);
        anim.setInterpolator(new OvershootInterpolator());
        anim.addUpdateListener(a -> {
            selectedScale = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) * 0.42f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float segmentAngle = sweepAngle / maxValue;

        // Draw soft background circle
        bgPaint.setColor(0xFFFFF8F0);
        canvas.drawCircle(centerX, centerY, radius + 60, bgPaint);

        // Draw outer decorative ring
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f);
        ringPaint.setColor(0x15FFB6C1);
        canvas.drawCircle(centerX, centerY, radius + 50, ringPaint);

        // Draw colored arc segments
        for (int i = 0; i < maxValue; i++) {
            boolean isSelected = (i == currentValue - 1);
            arcPaint.setStrokeWidth(isSelected ? 28f : 16f);
            arcPaint.setColor(moodColors[i]);
            arcPaint.setAlpha(isSelected ? 255 : 140);
            float segStart = startAngle + (i * segmentAngle);
            float padding = 4f;
            RectF arcRect = new RectF(
                    centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius);
            canvas.drawArc(arcRect, segStart + padding, segmentAngle - padding * 2, false, arcPaint);
        }

        // Draw dots and labels on arc
        for (int i = 0; i < maxValue; i++) {
            float dotAngle = startAngle + (i + 0.5f) * segmentAngle;
            double dotRad = Math.toRadians(dotAngle);
            float dotX = centerX + (float) Math.cos(dotRad) * radius;
            float dotY = centerY + (float) Math.sin(dotRad) * radius;

            boolean isSelected = (i == currentValue - 1);

            if (isSelected) {
                // Glow
                glowPaint.setColor(moodColors[i]);
                glowPaint.setAlpha(50);
                canvas.drawCircle(dotX, dotY, 44f, glowPaint);
                glowPaint.setAlpha(25);
                canvas.drawCircle(dotX, dotY, 60f, glowPaint);

                // Big dot
                selectedDotPaint.setColor(moodColors[i]);
                canvas.drawCircle(dotX, dotY, 24f * selectedScale, selectedDotPaint);
                Paint whiteDot = new Paint(Paint.ANTI_ALIAS_FLAG);
                whiteDot.setColor(Color.WHITE);
                canvas.drawCircle(dotX, dotY, 10f * selectedScale, whiteDot);
            } else {
                dotPaint.setColor(moodColors[i]);
                dotPaint.setAlpha(200);
                canvas.drawCircle(dotX, dotY, 14f, dotPaint);

                // Label outside arc
                float labelRadius = radius + 48f;
                float labelX = centerX + (float) Math.cos(dotRad) * labelRadius;
                float labelY = centerY + (float) Math.sin(dotRad) * labelRadius;
                canvas.drawText(moodEmojis[i] + moodLabels[i], labelX, labelY + 8, labelPaint);
            }
        }

        // Center circle background
        centerPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(centerX, centerY, radius * 0.52f, centerPaint);

        // Center glow
        centerGlowPaint.setColor(moodColors[currentValue - 1]);
        centerGlowPaint.setAlpha(25);
        canvas.drawCircle(centerX, centerY, radius * 0.52f, centerGlowPaint);

        // Center selected emoji (big)
        float emojiSize = 80f * selectedScale;
        emojiPaint.setTextSize(emojiSize);
        canvas.drawText(moodEmojis[currentValue - 1], centerX, centerY - 10, emojiPaint);

        // Center label
        selectedLabelPaint.setColor(moodColors[currentValue - 1]);
        canvas.drawText(moodLabels[currentValue - 1], centerX, centerY + radius * 0.38f, selectedLabelPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateValueFromTouch(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void updateValueFromTouch(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360;

        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance < radius * 0.25f || distance > radius * 1.6f) return;

        float normalizedAngle = (float) ((angle - startAngle + 360) % 360);
        int newValue = Math.round(minValue + (normalizedAngle / sweepAngle) * (maxValue - minValue));
        newValue = Math.max(minValue, Math.min(maxValue, newValue));

        if (newValue != currentValue) {
            currentValue = newValue;

            ValueAnimator anim = ValueAnimator.ofFloat(0.7f, 1f);
            anim.setDuration(200);
            anim.setInterpolator(new OvershootInterpolator());
            anim.addUpdateListener(a -> {
                selectedScale = (float) a.getAnimatedValue();
                invalidate();
            });
            anim.start();

            if (listener != null) listener.onMoodChanged(currentValue);
        }
    }

    public void setOnMoodChangeListener(OnMoodChangeListener listener) { this.listener = listener; }
    public int getValue() { return currentValue; }
    public String getMoodLabel() { return moodLabels[currentValue - 1]; }
    public String getMoodEmoji() { return moodEmojis[currentValue - 1]; }
    public void setValue(int value) { this.currentValue = Math.max(minValue, Math.min(maxValue, value)); invalidate(); }
}
