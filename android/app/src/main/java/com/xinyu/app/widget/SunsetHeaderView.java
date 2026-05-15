package com.xinyu.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class SunsetHeaderView extends View {

    private Paint skyPaint, sunGlowPaint, mountainPaint, cloudPaint;
    private int width, height;
    private float sunCx, sunCy, sunRadius;

    // Soft warm pastel palette — matching the reference
    private static final int SKY_TOP = Color.parseColor("#FFE8EC");     // very light pink
    private static final int SKY_MID = Color.parseColor("#FFD6CC");     // soft peach
    private static final int SKY_BOT = Color.parseColor("#FFCBB8");     // warm apricot
    private static final int MOUNTAIN_FAR = Color.parseColor("#F5C6B8"); // dusty rose, very soft
    private static final int MOUNTAIN_NEAR = Color.parseColor("#EDBBAE"); // slightly deeper
    private static final int CLOUD_COLOR = Color.parseColor("#30FFFFFF"); // translucent white

    public SunsetHeaderView(Context context) {
        super(context);
        init();
    }

    public SunsetHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SunsetHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        skyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mountainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mountainPaint.setStyle(Paint.Style.FILL);
        cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        sunCx = width * 0.5f;
        sunCy = height * 0.52f;
        sunRadius = width * 0.12f;

        // Sky gradient: light pink → peach → warm apricot
        skyPaint.setShader(new LinearGradient(
                0, 0, 0, height,
                new int[]{SKY_TOP, SKY_MID, SKY_BOT},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        ));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Sky gradient background
        canvas.drawRect(0, 0, width, height, skyPaint);

        // 2. Soft sun glow — large, diffused, warm
        sunGlowPaint.setShader(new RadialGradient(
                sunCx, sunCy, sunRadius * 5,
                new int[]{
                        Color.parseColor("#60FFE4CC"),
                        Color.parseColor("#30FFDAB9"),
                        Color.TRANSPARENT
                },
                new float[]{0f, 0.4f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(sunCx, sunCy, sunRadius * 5, sunGlowPaint);

        // 3. Sun body — soft warm circle
        Paint sunBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunBodyPaint.setShader(new RadialGradient(
                sunCx, sunCy, sunRadius,
                new int[]{
                        Color.parseColor("#FFFDF5"),
                        Color.parseColor("#FFF3E0"),
                        Color.parseColor("#FFE8CC")
                },
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(sunCx, sunCy, sunRadius, sunBodyPaint);

        // 4. Soft clouds — very subtle, translucent white
        drawSoftCloud(canvas, width * 0.15f, height * 0.30f, width * 0.22f, 0.20f);
        drawSoftCloud(canvas, width * 0.62f, height * 0.25f, width * 0.28f, 0.16f);
        drawSoftCloud(canvas, width * 0.85f, height * 0.35f, width * 0.18f, 0.14f);

        // 5. Far mountain — gentle rolling hills
        drawGentleHill(canvas, height * 0.68f, MOUNTAIN_FAR, 0.8f, 0.025f);

        // 6. Near mountain — slightly closer
        drawGentleHill(canvas, height * 0.74f, MOUNTAIN_NEAR, 1.2f, 0.03f);

        // 7. Bottom fill to ensure no gap with pet card
        mountainPaint.setColor(MOUNTAIN_NEAR);
        canvas.drawRect(0, height * 0.80f, width, height, mountainPaint);
    }

    private void drawSoftCloud(Canvas canvas, float cx, float cy, float size, float alpha) {
        int a = (int)(alpha * 255);
        cloudPaint.setColor(Color.argb(a, 255, 255, 255));
        // Main body
        canvas.drawOval(cx - size * 0.45f, cy - size * 0.12f,
                cx + size * 0.45f, cy + size * 0.18f, cloudPaint);
        // Left bump
        canvas.drawOval(cx - size * 0.30f, cy - size * 0.28f,
                cx + size * 0.05f, cy + size * 0.05f, cloudPaint);
        // Right bump
        canvas.drawOval(cx + size * 0.0f, cy - size * 0.22f,
                cx + size * 0.35f, cy + size * 0.08f, cloudPaint);
    }

    private void drawGentleHill(Canvas canvas, float baseY, int color, float variation, float amplitude) {
        mountainPaint.setColor(color);
        Path path = new Path();
        path.moveTo(-10, baseY + height * 0.06f);

        int segs = 40;
        float dx = (width + 20) / (float) segs;
        for (int i = 0; i <= segs; i++) {
            float x = -10 + i * dx;
            float y = baseY
                    + (float) Math.sin(i * 0.4 + variation * 3) * height * amplitude
                    + (float) Math.sin(i * 0.9 + variation * 1.5) * height * (amplitude * 0.5f);
            path.lineTo(x, y);
        }
        path.lineTo(width + 10, height + 10);
        path.lineTo(-10, height + 10);
        path.close();
        canvas.drawPath(path, mountainPaint);
    }
}
