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
import android.view.animation.DecelerateInterpolator;

public class MoodMixerView extends View {

    public interface OnMoodMixListener {
        void onMoodMixed(String moodLabel, String moodEmoji, int[] values);
    }

    private OnMoodMixListener listener;

    // 5 sliders: 左边情绪 → 右边情绪
    private final String[] leftLabels = {"😊 开心", "😌 平静", "⚡ 有活力", "💪 自信", "🌟 有希望"};
    private final String[] rightLabels = {"😢 难过", "😰 焦虑", "😴 疲惫", "😟 不安", "😔 失落"};
    private final int[] sliderValues = {50, 50, 50, 50, 50};

    private final int[] leftColors = {0xFFFFD54F, 0xFF81D4FA, 0xFFFF8A65, 0xFFA5D6A7, 0xFFFFE082};
    private final int[] rightColors = {0xFF90CAF9, 0xFFCE93D8, 0xFFBDBDBD, 0xFFEF9A9A, 0xFFB0BEC5};

    private Paint trackPaint, thumbPaint, thumbGlowPaint;
    private Paint labelPaint;
    private Paint bgPaint, resultPaint, resultEmojiPaint;
    private Paint dividerPaint;

    private float sliderWidth;
    private float sliderHeight;
    private float topOffset;
    private int draggingSlider = -1;

    private float animProgress = 0f;

    public MoodMixerView(Context context) { super(context); init(); }
    public MoodMixerView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public MoodMixerView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.FILL);

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setStyle(Paint.Style.FILL);

        thumbGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbGlowPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF888888);
        labelPaint.setTextSize(22f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFFFFF8F0);
        bgPaint.setStyle(Paint.Style.FILL);

        resultPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resultPaint.setColor(0xFF4A3728);
        resultPaint.setTextSize(26f);
        resultPaint.setTextAlign(Paint.Align.CENTER);
        resultPaint.setFakeBoldText(true);

        resultEmojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resultEmojiPaint.setTextAlign(Paint.Align.CENTER);

        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(0x20FFB6C1);
        dividerPaint.setStrokeWidth(1f);

        startEntryAnim();
    }

    private void startEntryAnim() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(900);
        anim.setInterpolator(new DecelerateInterpolator(1.5f));
        anim.addUpdateListener(a -> {
            animProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int count = sliderValues.length;
        sliderWidth = (w - 40f) / count;
        // 滑块占大部分高度，顶部留标签空间，底部留结果空间
        topOffset = h * 0.10f;
        sliderHeight = h * 0.65f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float alpha = Math.min(1f, animProgress * 1.2f);

        // Background
        RectF bgRect = new RectF(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(bgRect, 20, 20, bgPaint);

        float totalWidth = sliderValues.length * sliderWidth;
        float startX = (getWidth() - totalWidth) / 2f;

        for (int i = 0; i < sliderValues.length; i++) {
            float x = startX + i * sliderWidth + sliderWidth / 2f;
            float trackTop = topOffset;
            float trackBottom = topOffset + sliderHeight;
            float thumbY = trackBottom - (sliderValues[i] / 100f) * sliderHeight;

            // Slide-in offset for each slider (staggered)
            float stagger = Math.min(1f, Math.max(0f, animProgress * 2f - i * 0.1f));
            float slideY = (1f - stagger) * 15f;
            float sliderAlpha = stagger;

            canvas.save();
            canvas.translate(0, slideY);
            setAlpha(canvas, sliderAlpha);

            // Track background
            float trackW = 18f;
            trackPaint.setColor(0x18FFB6C1);
            canvas.drawRoundRect(new RectF(x - trackW / 2, trackTop, x + trackW / 2, trackBottom), 9, 9, trackPaint);

            // Filled track
            float filledRatio = sliderValues[i] / 100f;
            int leftC = leftColors[i];
            int rightC = rightColors[i];
            int mixedColor = blendColors(leftC, rightC, filledRatio);

            trackPaint.setShader(new LinearGradient(x, trackBottom, x, thumbY, mixedColor, mixedColor, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(new RectF(x - trackW / 2, thumbY, x + trackW / 2, trackBottom), 9, 9, trackPaint);
            trackPaint.setShader(null);

            // Thumb glow
            thumbGlowPaint.setColor(mixedColor);
            thumbGlowPaint.setAlpha(30);
            canvas.drawCircle(x, thumbY, 20f, thumbGlowPaint);

            // Thumb
            thumbPaint.setColor(mixedColor);
            canvas.drawCircle(x, thumbY, 14f, thumbPaint);
            Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
            inner.setColor(Color.WHITE);
            canvas.drawCircle(x, thumbY, 5f, inner);

            // Labels - top (负面情绪)
            labelPaint.setTextSize(18f);
            labelPaint.setAlpha((int)(255 * sliderAlpha));
            canvas.drawText(rightLabels[i], x, trackTop - 6, labelPaint);

            // Labels - bottom (正面情绪)
            canvas.drawText(leftLabels[i], x, trackBottom + 20, labelPaint);

            // Divider
            if (i < sliderValues.length - 1) {
                dividerPaint.setAlpha((int)(50 * sliderAlpha));
                float divX = x + sliderWidth / 2f;
                canvas.drawLine(divX, trackTop, divX, trackBottom, dividerPaint);
            }

            canvas.restore();
        }

        // Result area - emoji + mood text at bottom
        float resultAlpha = Math.min(1f, Math.max(0f, (animProgress - 0.3f) * 2.5f));
        float resultY = topOffset + sliderHeight + 38;
        String mood = getMoodLabel();
        String emoji = getMoodEmoji();

        int moodColor = getMoodColor();
        thumbGlowPaint.setColor(moodColor);
        thumbGlowPaint.setAlpha((int)(15 * resultAlpha));
        canvas.drawCircle(getWidth() / 2f, resultY + 3, 28f, thumbGlowPaint);

        resultEmojiPaint.setTextSize(40f);
        resultEmojiPaint.setAlpha((int)(255 * resultAlpha));
        canvas.drawText(emoji, getWidth() / 2f, resultY + 15, resultEmojiPaint);

        resultPaint.setColor(moodColor);
        resultPaint.setAlpha((int)(255 * resultAlpha));
        canvas.drawText(mood, getWidth() / 2f, resultY + 44, resultPaint);
    }

    private void setAlpha(Canvas canvas, float alpha) {
        if (alpha < 1f) {
            canvas.saveLayerAlpha(0, 0, getWidth(), getHeight(), (int)(255 * alpha));
        }
    }

    private int blendColors(int c1, int c2, float ratio) {
        float r = (Color.red(c1) * (1 - ratio) + Color.red(c2) * ratio);
        float g = (Color.green(c1) * (1 - ratio) + Color.green(c2) * ratio);
        float b = (Color.blue(c1) * (1 - ratio) + Color.blue(c2) * ratio);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private String getMoodLabel() {
        int avg = 0;
        for (int v : sliderValues) avg += v;
        avg /= sliderValues.length;

        if (avg < 25) return "心情超棒";
        if (avg < 40) return "还不错";
        if (avg < 60) return "平平淡淡";
        if (avg < 75) return "有点低落";
        return "需要抱抱";
    }

    private String getMoodEmoji() {
        int avg = 0;
        for (int v : sliderValues) avg += v;
        avg /= sliderValues.length;

        if (avg < 25) return "😊";
        if (avg < 40) return "😌";
        if (avg < 60) return "😐";
        if (avg < 75) return "😔";
        return "😢";
    }

    private int getMoodColor() {
        int avg = 0;
        for (int v : sliderValues) avg += v;
        avg /= sliderValues.length;

        if (avg < 25) return 0xFFFFD54F;
        if (avg < 40) return 0xFF81D4FA;
        if (avg < 60) return 0xFFBDBDBD;
        if (avg < 75) return 0xFFCE93D8;
        return 0xFF90CAF9;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                draggingSlider = findSlider(x, y);
                if (draggingSlider >= 0) {
                    updateSlider(draggingSlider, y);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                if (draggingSlider >= 0) {
                    updateSlider(draggingSlider, y);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
                draggingSlider = -1;
                if (listener != null) {
                    listener.onMoodMixed(getMoodLabel(), getMoodEmoji(), sliderValues.clone());
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int findSlider(float touchX, float touchY) {
        float totalWidth = sliderValues.length * sliderWidth;
        float startX = (getWidth() - totalWidth) / 2f;
        float trackTop = topOffset;
        float trackBottom = topOffset + sliderHeight;

        for (int i = 0; i < sliderValues.length; i++) {
            float cx = startX + i * sliderWidth + sliderWidth / 2f;
            if (Math.abs(touchX - cx) < sliderWidth / 2f && touchY >= trackTop - 20 && touchY <= trackBottom + 20) {
                return i;
            }
        }
        return -1;
    }

    private void updateSlider(int index, float touchY) {
        float trackTop = topOffset;
        float trackBottom = topOffset + sliderHeight;
        float value = 1f - (touchY - trackTop) / (trackBottom - trackTop);
        value = Math.max(0f, Math.min(1f, value));
        int newValue = Math.round(value * 100);

        if (newValue != sliderValues[index]) {
            sliderValues[index] = newValue;
            invalidate();
        }
    }

    public void setOnMoodMixListener(OnMoodMixListener listener) { this.listener = listener; }
    public int[] getValues() { return sliderValues.clone(); }
}
