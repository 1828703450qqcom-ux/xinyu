package com.xinyu.app.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.xinyu.app.model.Pet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PetView extends View {

    private Paint bodyPaint, earPaint, innerEarPaint, eyePaint, eyeWhitePaint;
    private Paint nosePaint, whiskerPaint, cheekPaint, tailPaint, shadowPaint;
    private Paint levelPaint, namePaint, particlePaint;

    private float bodyCenterX, bodyCenterY;
    private float bodyRadius;
    private float bounceOffset = 0;
    private float tailAngle = 0;
    private float eyeBlinkProgress = 1f;
    private float rotationAngle = 0;
    private boolean isSleeping = false;
    private boolean isAnimating = false;

    private Pet pet;
    private OnPetClickListener listener;
    private OnPetActionListener actionListener;
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();

    public interface OnPetClickListener {
        void onPetClick();
    }

    public interface OnPetActionListener {
        void onFeed();
        void onPlay();
        void onSleep();
        void onPet();
    }

    public PetView(Context context) {
        super(context);
        init();
    }

    public PetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(0xFFFFE0B2);
        bodyPaint.setStyle(Paint.Style.FILL);

        earPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        earPaint.setColor(0xFFFFCC80);
        earPaint.setStyle(Paint.Style.FILL);

        innerEarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerEarPaint.setColor(0xFFFFAB91);
        innerEarPaint.setStyle(Paint.Style.FILL);

        eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setColor(0xFF4A3728);
        eyePaint.setStyle(Paint.Style.FILL);

        eyeWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyeWhitePaint.setColor(Color.WHITE);
        eyeWhitePaint.setStyle(Paint.Style.FILL);

        nosePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nosePaint.setColor(0xFFFF8A9B);
        nosePaint.setStyle(Paint.Style.FILL);

        whiskerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiskerPaint.setColor(0xFFBDBDBD);
        whiskerPaint.setStrokeWidth(1.5f);
        whiskerPaint.setStyle(Paint.Style.STROKE);
        whiskerPaint.setStrokeCap(Paint.Cap.ROUND);

        cheekPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cheekPaint.setColor(0x40FF8A9B);
        cheekPaint.setStyle(Paint.Style.FILL);

        tailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tailPaint.setStyle(Paint.Style.STROKE);
        tailPaint.setStrokeWidth(6f);
        tailPaint.setStrokeCap(Paint.Cap.ROUND);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x20000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        levelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelPaint.setColor(0xFFB39DDB);
        levelPaint.setTextSize(28f);
        levelPaint.setTextAlign(Paint.Align.CENTER);

        namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(0xFF4A3728);
        namePaint.setTextSize(32f);
        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setFakeBoldText(true);

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);

        setOnClickListener(v -> {
            if (listener != null) listener.onPetClick();
            performRandomAction();
        });

        setOnLongClickListener(v -> {
            if (actionListener != null) actionListener.onPet();
            return true;
        });

        startIdleAnimation();
    }

    public void setPet(Pet pet) {
        this.pet = pet;
        if (pet != null) {
            isSleeping = pet.getEnergy() < 20;
            switch (pet.getStage()) {
                case 0: bodyPaint.setColor(0xFFE0E0E0); earPaint.setColor(0xFFBDBDBD); break;
                case 1: bodyPaint.setColor(0xFFFFE0B2); earPaint.setColor(0xFFFFCC80); break;
                case 2: bodyPaint.setColor(0xFFFFCC80); earPaint.setColor(0xFFFFB74D); break;
                case 3: bodyPaint.setColor(0xFFFFB74D); earPaint.setColor(0xFFFFA726); break;
            }
            tailPaint.setColor(bodyPaint.getColor());
        }
        invalidate();
    }

    public void setOnPetClickListener(OnPetClickListener listener) {
        this.listener = listener;
    }

    public void setOnPetActionListener(OnPetActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bodyCenterX = w / 2f;
        bodyCenterY = h * 0.50f;
        bodyRadius = Math.min(w, h) * 0.28f;
        levelPaint.setTextSize(bodyRadius * 0.35f);
        namePaint.setTextSize(bodyRadius * 0.4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pet == null) return;

        float cy = bodyCenterY + bounceOffset;
        float cx = bodyCenterX;

        // Ground shadow — radial gradient for softness
        Paint groundShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        groundShadow.setShader(new RadialGradient(
                cx, cy + bodyRadius * 0.95f,
                bodyRadius * 0.7f,
                new int[]{0x25000000, 0x08000000, 0x00000000},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawOval(
                cx - bodyRadius * 0.7f, cy + bodyRadius * 0.85f,
                cx + bodyRadius * 0.7f, cy + bodyRadius * 1.05f,
                groundShadow);

        // Apply rotation for spin action
        if (rotationAngle != 0) {
            canvas.save();
            canvas.rotate(rotationAngle, cx, cy);
        }

        if (pet.getStage() == 0) {
            drawEgg(canvas, cx, cy);
        } else {
            drawCat(canvas, cx, cy);
        }

        if (rotationAngle != 0) {
            canvas.restore();
        }

        // Name below
        canvas.drawText(pet.getName(), cx, cy + bodyRadius * 1.4f, namePaint);

        // Level badge
        String levelText = "Lv." + pet.getLevel();
        canvas.drawText(levelText, cx, cy - bodyRadius * 1.25f, levelPaint);

        // Status indicators
        drawStatusBars(canvas, cx, cy);

        // Draw particles (hearts, stars, notes)
        drawParticles(canvas);
    }

    // ==================== Actions ====================

    private void performRandomAction() {
        if (isAnimating || isSleeping) return;
        int action = random.nextInt(5);
        switch (action) {
            case 0: startDance(); break;
            case 1: startSpin(); break;
            case 2: startJump(); break;
            case 3: startPurr(); break;
            case 4: startWave(); break;
        }
    }

    /** Dance — bouncy wiggle */
    public void startDance() {
        isAnimating = true;
        spawnParticles(5, Particle.TYPE_HEART);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, -14f, 0f, -10f, 0f, -6f, 0f);
        anim.setDuration(800);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            bounceOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) { isAnimating = false; }
        });
        anim.start();
    }

    /** Spin — 360 degree rotation */
    private void startSpin() {
        isAnimating = true;
        spawnParticles(4, Particle.TYPE_STAR);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 360f);
        anim.setDuration(600);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            rotationAngle = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                rotationAngle = 0;
                isAnimating = false;
            }
        });
        anim.start();
    }

    /** Jump — high leap with squash & stretch */
    private void startJump() {
        isAnimating = true;
        spawnParticles(3, Particle.TYPE_STAR);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, -28f, 0f);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            bounceOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) { isAnimating = false; }
        });
        anim.start();
    }

    /** Purr — gentle vibration + hearts */
    private void startPurr() {
        isAnimating = true;
        spawnParticles(8, Particle.TYPE_HEART);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 2f, -2f, 1.5f, -1.5f, 0f);
        anim.setDuration(1000);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            bounceOffset = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) { isAnimating = false; }
        });
        anim.start();
    }

    /** Wave — side to side tilt */
    private void startWave() {
        isAnimating = true;
        spawnParticles(3, Particle.TYPE_NOTE);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, -12f, 12f, -8f, 8f, 0f);
        anim.setDuration(700);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(a -> {
            rotationAngle = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                rotationAngle = 0;
                isAnimating = false;
            }
        });
        anim.start();
    }

    // ==================== Particles ====================

    private void spawnParticles(int count, int type) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.type = type;
            p.x = bodyCenterX + (random.nextFloat() - 0.5f) * bodyRadius * 2;
            p.y = bodyCenterY - bodyRadius * 0.5f;
            p.vx = (random.nextFloat() - 0.5f) * 3;
            p.vy = -(2 + random.nextFloat() * 3);
            p.life = 1f;
            p.decay = 0.015f + random.nextFloat() * 0.01f;
            p.size = 8 + random.nextFloat() * 8;
            particles.add(p);
        }
        invalidate();
    }

    private void drawParticles(Canvas canvas) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.05f; // gravity
            p.life -= p.decay;

            if (p.life <= 0) {
                it.remove();
                continue;
            }

            int alpha = (int)(p.life * 255);
            particlePaint.setAlpha(alpha);

            switch (p.type) {
                case Particle.TYPE_HEART:
                    particlePaint.setColor(0xFFFF6B8A);
                    drawHeart(canvas, p.x, p.y, p.size * p.life, particlePaint);
                    break;
                case Particle.TYPE_STAR:
                    particlePaint.setColor(0xFFFFD54F);
                    drawStar(canvas, p.x, p.y, p.size * p.life, particlePaint);
                    break;
                case Particle.TYPE_NOTE:
                    particlePaint.setColor(0xFFB39DDB);
                    drawNote(canvas, p.x, p.y, p.size * p.life, particlePaint);
                    break;
            }
        }
        if (!particles.isEmpty()) {
            invalidate(); // keep animating
        }
    }

    private void drawHeart(Canvas canvas, float cx, float cy, float size, Paint paint) {
        Path heart = new Path();
        float s = size * 0.5f;
        heart.moveTo(cx, cy + s * 0.4f);
        heart.cubicTo(cx - s, cy - s * 0.2f, cx - s * 0.5f, cy - s, cx, cy - s * 0.5f);
        heart.cubicTo(cx + s * 0.5f, cy - s, cx + s, cy - s * 0.2f, cx, cy + s * 0.4f);
        canvas.drawPath(heart, paint);
    }

    private void drawStar(Canvas canvas, float cx, float cy, float size, Paint paint) {
        float r = size * 0.5f;
        float inner = r * 0.4f;
        Path star = new Path();
        for (int i = 0; i < 5; i++) {
            double outerAngle = Math.toRadians(-90 + i * 72);
            double innerAngle = Math.toRadians(-90 + i * 72 + 36);
            if (i == 0) star.moveTo(cx + (float)(r * Math.cos(outerAngle)), cy + (float)(r * Math.sin(outerAngle)));
            else star.lineTo(cx + (float)(r * Math.cos(outerAngle)), cy + (float)(r * Math.sin(outerAngle)));
            star.lineTo(cx + (float)(inner * Math.cos(innerAngle)), cy + (float)(inner * Math.sin(innerAngle)));
        }
        star.close();
        canvas.drawPath(star, paint);
    }

    private void drawNote(Canvas canvas, float cx, float cy, float size, Paint paint) {
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("♪", cx, cy, paint);
    }

    // ==================== Egg ====================

    private void drawEgg(Canvas canvas, float cx, float cy) {
        RectF eggRect = new RectF(
                cx - bodyRadius * 0.7f, cy - bodyRadius,
                cx + bodyRadius * 0.7f, cy + bodyRadius * 0.9f);

        // 3D egg — radial gradient
        Paint egg3D = new Paint(Paint.ANTI_ALIAS_FLAG);
        egg3D.setShader(new RadialGradient(
                cx - bodyRadius * 0.15f, cy - bodyRadius * 0.3f,
                bodyRadius * 0.9f,
                new int[]{lighten(bodyPaint.getColor(), 1.2f), bodyPaint.getColor(), darken(bodyPaint.getColor(), 0.8f)},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawOval(eggRect, egg3D);

        // Egg specular highlight
        Paint eggHi = new Paint(Paint.ANTI_ALIAS_FLAG);
        eggHi.setShader(new RadialGradient(
                cx - bodyRadius * 0.2f, cy - bodyRadius * 0.5f,
                bodyRadius * 0.3f,
                new int[]{0x40FFFFFF, 0x00FFFFFF},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawOval(eggRect, eggHi);

        Paint crackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crackPaint.setColor(0xFFBDBDBD);
        crackPaint.setStrokeWidth(2f);
        crackPaint.setStyle(Paint.Style.STROKE);

        canvas.drawLine(cx - bodyRadius * 0.3f, cy - bodyRadius * 0.2f,
                cx + bodyRadius * 0.1f, cy + bodyRadius * 0.1f, crackPaint);
        canvas.drawLine(cx + bodyRadius * 0.1f, cy + bodyRadius * 0.1f,
                cx - bodyRadius * 0.1f, cy + bodyRadius * 0.4f, crackPaint);

        Paint qPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        qPaint.setColor(0xFF9E9E9E);
        qPaint.setTextSize(bodyRadius * 0.6f);
        qPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("?", cx, cy + bodyRadius * 0.2f, qPaint);
    }

    // ==================== Cat ====================

    private void drawCat(Canvas canvas, float cx, float cy) {
        // ========== Tail with gradient ==========
        float tailStartX = cx + bodyRadius * 0.6f;
        float tailStartY = cy + bodyRadius * 0.4f;
        tailPaint.setStrokeWidth(bodyRadius * 0.18f);
        tailPaint.setStrokeCap(Paint.Cap.ROUND);
        Path tailPath = new Path();
        tailPath.moveTo(tailStartX, tailStartY);
        tailPath.cubicTo(
                tailStartX + bodyRadius * 0.8f, tailStartY - bodyRadius * 0.2f + tailAngle,
                tailStartX + bodyRadius * 1.0f, tailStartY - bodyRadius * 0.8f + tailAngle,
                tailStartX + bodyRadius * 0.6f, tailStartY - bodyRadius * 1.0f + tailAngle
        );
        canvas.drawPath(tailPath, tailPaint);
        // Tail tip highlight
        tailPaint.setStrokeWidth(bodyRadius * 0.12f);
        Paint tailHi = new Paint(Paint.ANTI_ALIAS_FLAG);
        tailHi.setColor(0x40FFFFFF);
        tailHi.setStrokeWidth(bodyRadius * 0.08f);
        tailHi.setStrokeCap(Paint.Cap.ROUND);
        tailHi.setStyle(Paint.Style.STROKE);
        canvas.drawPath(tailPath, tailHi);

        // ========== Ears with 3D shading ==========
        float earHeight = bodyRadius * 0.7f;

        // Left ear — gradient from dark bottom to light top
        Path leftEar = new Path();
        leftEar.moveTo(cx - bodyRadius * 0.55f, cy - bodyRadius * 0.5f);
        leftEar.lineTo(cx - bodyRadius * 0.25f, cy - bodyRadius * 0.5f - earHeight);
        leftEar.lineTo(cx + bodyRadius * 0.05f, cy - bodyRadius * 0.5f);
        leftEar.close();
        int earBase = earPaint.getColor();
        int earDark = darken(earBase, 0.85f);
        int earLight = lighten(earBase, 1.15f);
        Paint earGrad = new Paint(Paint.ANTI_ALIAS_FLAG);
        earGrad.setShader(new LinearGradient(
                cx - bodyRadius * 0.25f, cy - bodyRadius * 0.5f - earHeight,
                cx - bodyRadius * 0.25f, cy - bodyRadius * 0.5f,
                earLight, earBase, Shader.TileMode.CLAMP));
        canvas.drawPath(leftEar, earGrad);

        Path leftInnerEar = new Path();
        leftInnerEar.moveTo(cx - bodyRadius * 0.45f, cy - bodyRadius * 0.5f);
        leftInnerEar.lineTo(cx - bodyRadius * 0.25f, cy - bodyRadius * 0.5f - earHeight * 0.65f);
        leftInnerEar.lineTo(cx - bodyRadius * 0.05f, cy - bodyRadius * 0.5f);
        leftInnerEar.close();
        canvas.drawPath(leftInnerEar, innerEarPaint);

        // Right ear
        Path rightEar = new Path();
        rightEar.moveTo(cx + bodyRadius * 0.05f, cy - bodyRadius * 0.5f);
        rightEar.lineTo(cx + bodyRadius * 0.25f, cy - bodyRadius * 0.5f - earHeight);
        rightEar.lineTo(cx + bodyRadius * 0.55f, cy - bodyRadius * 0.5f);
        rightEar.close();
        Paint earGrad2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        earGrad2.setShader(new LinearGradient(
                cx + bodyRadius * 0.25f, cy - bodyRadius * 0.5f - earHeight,
                cx + bodyRadius * 0.25f, cy - bodyRadius * 0.5f,
                earLight, earDark, Shader.TileMode.CLAMP));
        canvas.drawPath(rightEar, earGrad2);

        Path rightInnerEar = new Path();
        rightInnerEar.moveTo(cx + bodyRadius * 0.05f, cy - bodyRadius * 0.5f);
        rightInnerEar.lineTo(cx + bodyRadius * 0.25f, cy - bodyRadius * 0.5f - earHeight * 0.65f);
        rightInnerEar.lineTo(cx + bodyRadius * 0.45f, cy - bodyRadius * 0.5f);
        rightInnerEar.close();
        canvas.drawPath(rightInnerEar, innerEarPaint);

        // ========== Body — 3D sphere with radial gradient ==========
        int bodyBase = bodyPaint.getColor();
        int bodyDark = darken(bodyBase, 0.78f);
        int bodyLight = lighten(bodyBase, 1.25f);
        Paint body3D = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Light source top-left → shadow bottom-right
        body3D.setShader(new RadialGradient(
                cx - bodyRadius * 0.25f, cy - bodyRadius * 0.3f,
                bodyRadius * 1.1f,
                new int[]{bodyLight, bodyBase, bodyDark},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, bodyRadius, body3D);

        // Specular highlight — white oval on top-left of body
        Paint specPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        specPaint.setShader(new RadialGradient(
                cx - bodyRadius * 0.3f, cy - bodyRadius * 0.45f,
                bodyRadius * 0.4f,
                new int[]{0x60FFFFFF, 0x20FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx - bodyRadius * 0.3f, cy - bodyRadius * 0.45f,
                bodyRadius * 0.4f, specPaint);

        // Belly highlight — lighter area on front
        Paint bellyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bellyPaint.setShader(new RadialGradient(
                cx, cy + bodyRadius * 0.2f,
                bodyRadius * 0.6f,
                new int[]{0x30FFFFFF, 0x00FFFFFF},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy + bodyRadius * 0.2f, bodyRadius * 0.6f, bellyPaint);

        // Bottom shadow rim — darkens the very bottom edge
        Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rimPaint.setShader(new RadialGradient(
                cx + bodyRadius * 0.1f, cy + bodyRadius * 0.5f,
                bodyRadius * 0.7f,
                new int[]{0x00000000, 0x15000000},
                new float[]{0.4f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, bodyRadius, rimPaint);

        // ========== Eyes — 3D with depth ==========
        float eyeY = cy - bodyRadius * 0.15f;
        float eyeSpacing = bodyRadius * 0.3f;
        float eyeRadius = bodyRadius * 0.12f;

        if (isSleeping) {
            Paint sleepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            sleepPaint.setColor(0xFF4A3728);
            sleepPaint.setStrokeWidth(3f);
            sleepPaint.setStyle(Paint.Style.STROKE);
            sleepPaint.setStrokeCap(Paint.Cap.ROUND);

            canvas.drawArc(new RectF(cx - eyeSpacing - eyeRadius, eyeY - eyeRadius,
                            cx - eyeSpacing + eyeRadius, eyeY + eyeRadius),
                    0, 180, false, sleepPaint);
            canvas.drawArc(new RectF(cx + eyeSpacing - eyeRadius, eyeY - eyeRadius,
                            cx + eyeSpacing + eyeRadius, eyeY + eyeRadius),
                    0, 180, false, sleepPaint);

            Paint zPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            zPaint.setColor(0xFFB39DDB);
            zPaint.setTextSize(bodyRadius * 0.3f);
            zPaint.setTextAlign(Paint.Align.LEFT);
            float zOffset = (System.currentTimeMillis() % 2000) / 2000f * 20f;
            canvas.drawText("z", cx + bodyRadius * 0.6f, cy - bodyRadius * 0.5f - zOffset, zPaint);
            canvas.drawText("Z", cx + bodyRadius * 0.8f, cy - bodyRadius * 0.8f - zOffset * 0.7f, zPaint);
        } else {
            float eyeW = eyeRadius * 1.5f;

            // Eye shadow (depth under eye)
            Paint eyeShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
            eyeShadow.setShader(new RadialGradient(
                    cx - eyeSpacing, eyeY + 2f, eyeW,
                    0x20000000, 0x00000000, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx - eyeSpacing, eyeY + 2f, eyeW, eyeShadow);
            eyeShadow.setShader(new RadialGradient(
                    cx + eyeSpacing, eyeY + 2f, eyeW,
                    0x20000000, 0x00000000, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx + eyeSpacing, eyeY + 2f, eyeW, eyeShadow);

            // Eye white — gradient for 3D
            Paint eyeWhite3D = new Paint(Paint.ANTI_ALIAS_FLAG);
            eyeWhite3D.setShader(new RadialGradient(
                    cx - eyeSpacing, eyeY, eyeW,
                    new int[]{Color.WHITE, 0xFFE0E0E0},
                    new float[]{0.6f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx - eyeSpacing, eyeY, eyeW, eyeWhite3D);
            eyeWhite3D.setShader(new RadialGradient(
                    cx + eyeSpacing, eyeY, eyeW,
                    new int[]{Color.WHITE, 0xFFE0E0E0},
                    new float[]{0.6f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx + eyeSpacing, eyeY, eyeW, eyeWhite3D);

            // Pupil — with dark gradient for depth
            float pupilRadius = eyeRadius * eyeBlinkProgress;
            Paint pupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pupilPaint.setShader(new RadialGradient(
                    cx - eyeSpacing, eyeY, pupilRadius,
                    new int[]{0xFF2D1B0E, 0xFF4A3728},
                    new float[]{0.5f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx - eyeSpacing, eyeY, pupilRadius, pupilPaint);
            pupilPaint.setShader(new RadialGradient(
                    cx + eyeSpacing, eyeY, pupilRadius,
                    new int[]{0xFF2D1B0E, 0xFF4A3728},
                    new float[]{0.5f, 1f},
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(cx + eyeSpacing, eyeY, pupilRadius, pupilPaint);

            // Eye highlights — large + small for wet look
            if (pupilRadius > 1f) {
                Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                shinePaint.setColor(Color.WHITE);
                // Main highlight — top left
                canvas.drawCircle(cx - eyeSpacing - eyeRadius * 0.2f, eyeY - eyeRadius * 0.35f,
                        eyeRadius * 0.45f, shinePaint);
                canvas.drawCircle(cx + eyeSpacing - eyeRadius * 0.2f, eyeY - eyeRadius * 0.35f,
                        eyeRadius * 0.45f, shinePaint);
                // Small secondary highlight — bottom right
                Paint shineSmall = new Paint(Paint.ANTI_ALIAS_FLAG);
                shineSmall.setColor(0x99FFFFFF);
                canvas.drawCircle(cx - eyeSpacing + eyeRadius * 0.35f, eyeY + eyeRadius * 0.25f,
                        eyeRadius * 0.2f, shineSmall);
                canvas.drawCircle(cx + eyeSpacing + eyeRadius * 0.35f, eyeY + eyeRadius * 0.25f,
                        eyeRadius * 0.2f, shineSmall);
            }
        }

        // ========== Cheeks with glow ==========
        Paint cheekGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        cheekGlow.setShader(new RadialGradient(
                cx - bodyRadius * 0.5f, cy + bodyRadius * 0.15f,
                bodyRadius * 0.18f,
                new int[]{0x55FF8A9B, 0x20FF8A9B, 0x00FF8A9B},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx - bodyRadius * 0.5f, cy + bodyRadius * 0.15f,
                bodyRadius * 0.18f, cheekGlow);
        cheekGlow.setShader(new RadialGradient(
                cx + bodyRadius * 0.5f, cy + bodyRadius * 0.15f,
                bodyRadius * 0.18f,
                new int[]{0x55FF8A9B, 0x20FF8A9B, 0x00FF8A9B},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx + bodyRadius * 0.5f, cy + bodyRadius * 0.15f,
                bodyRadius * 0.18f, cheekGlow);

        // ========== Nose — 3D triangle with highlight ==========
        float noseY = cy + bodyRadius * 0.08f;
        Path nosePath = new Path();
        nosePath.moveTo(cx, noseY - bodyRadius * 0.06f);
        nosePath.lineTo(cx - bodyRadius * 0.06f, noseY + bodyRadius * 0.04f);
        nosePath.lineTo(cx + bodyRadius * 0.06f, noseY + bodyRadius * 0.04f);
        nosePath.close();
        canvas.drawPath(nosePath, nosePaint);
        // Nose highlight
        Paint noseHi = new Paint(Paint.ANTI_ALIAS_FLAG);
        noseHi.setColor(0x50FFFFFF);
        canvas.drawCircle(cx, noseY - bodyRadius * 0.015f, bodyRadius * 0.025f, noseHi);

        // ========== Mouth ==========
        Paint mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mouthPaint.setColor(0xFFBDBDBD);
        mouthPaint.setStrokeWidth(1.5f);
        mouthPaint.setStyle(Paint.Style.STROKE);
        mouthPaint.setStrokeCap(Paint.Cap.ROUND);
        float mouthY = noseY + bodyRadius * 0.08f;
        canvas.drawLine(cx, noseY + bodyRadius * 0.04f,
                cx - bodyRadius * 0.1f, mouthY + bodyRadius * 0.06f, mouthPaint);
        canvas.drawLine(cx, noseY + bodyRadius * 0.04f,
                cx + bodyRadius * 0.1f, mouthY + bodyRadius * 0.06f, mouthPaint);

        // ========== Whiskers ==========
        float whiskerY = cy + bodyRadius * 0.12f;
        canvas.drawLine(cx - bodyRadius * 0.2f, whiskerY - bodyRadius * 0.05f,
                cx - bodyRadius * 0.8f, whiskerY - bodyRadius * 0.12f, whiskerPaint);
        canvas.drawLine(cx - bodyRadius * 0.2f, whiskerY,
                cx - bodyRadius * 0.85f, whiskerY + bodyRadius * 0.02f, whiskerPaint);
        canvas.drawLine(cx - bodyRadius * 0.2f, whiskerY + bodyRadius * 0.05f,
                cx - bodyRadius * 0.8f, whiskerY + bodyRadius * 0.15f, whiskerPaint);
        canvas.drawLine(cx + bodyRadius * 0.2f, whiskerY - bodyRadius * 0.05f,
                cx + bodyRadius * 0.8f, whiskerY - bodyRadius * 0.12f, whiskerPaint);
        canvas.drawLine(cx + bodyRadius * 0.2f, whiskerY,
                cx + bodyRadius * 0.85f, whiskerY + bodyRadius * 0.02f, whiskerPaint);
        canvas.drawLine(cx + bodyRadius * 0.2f, whiskerY + bodyRadius * 0.05f,
                cx + bodyRadius * 0.8f, whiskerY + bodyRadius * 0.15f, whiskerPaint);

        // ========== Paws with 3D shading ==========
        Paint pawGradL = new Paint(Paint.ANTI_ALIAS_FLAG);
        pawGradL.setShader(new RadialGradient(
                cx - bodyRadius * 0.2f, cy + bodyRadius * 0.85f,
                bodyRadius * 0.2f,
                new int[]{lighten(bodyPaint.getColor(), 1.1f), bodyPaint.getColor()},
                new float[]{0.3f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawOval(cx - bodyRadius * 0.35f, cy + bodyRadius * 0.75f,
                cx - bodyRadius * 0.05f, cy + bodyRadius * 0.95f, pawGradL);

        Paint pawGradR = new Paint(Paint.ANTI_ALIAS_FLAG);
        pawGradR.setShader(new RadialGradient(
                cx + bodyRadius * 0.2f, cy + bodyRadius * 0.85f,
                bodyRadius * 0.2f,
                new int[]{lighten(bodyPaint.getColor(), 1.05f), darken(bodyPaint.getColor(), 0.9f)},
                new float[]{0.3f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawOval(cx + bodyRadius * 0.05f, cy + bodyRadius * 0.75f,
                cx + bodyRadius * 0.35f, cy + bodyRadius * 0.95f, pawGradR);

        // Paw toe lines
        Paint toePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        toePaint.setColor(0x30000000);
        toePaint.setStrokeWidth(1.2f);
        toePaint.setStrokeCap(Paint.Cap.ROUND);
        float pawLY = cy + bodyRadius * 0.85f;
        canvas.drawLine(cx - bodyRadius * 0.25f, pawLY, cx - bodyRadius * 0.25f, pawLY + bodyRadius * 0.06f, toePaint);
        canvas.drawLine(cx - bodyRadius * 0.15f, pawLY, cx - bodyRadius * 0.15f, pawLY + bodyRadius * 0.06f, toePaint);
        canvas.drawLine(cx + bodyRadius * 0.15f, pawLY, cx + bodyRadius * 0.15f, pawLY + bodyRadius * 0.06f, toePaint);
        canvas.drawLine(cx + bodyRadius * 0.25f, pawLY, cx + bodyRadius * 0.25f, pawLY + bodyRadius * 0.06f, toePaint);
    }

    // ==================== Status Bars ====================

    private void drawStatusBars(Canvas canvas, float cx, float cy) {
        if (pet.getStage() == 0) return;

        float barWidth = bodyRadius * 1.2f;
        float barHeight = 6f;
        float barY = cy + bodyRadius * 1.6f;
        float barX = cx - barWidth / 2f;

        drawBar(canvas, barX, barY, barWidth, barHeight,
                pet.getHunger() / 100f, 0xFFFF8A9B);
        drawBar(canvas, barX, barY + 12f, barWidth, barHeight,
                pet.getHappiness() / 100f, 0xFFB39DDB);
        drawBar(canvas, barX, barY + 24f, barWidth, barHeight,
                pet.getEnergy() / 100f, 0xFF81C784);
    }

    private void drawBar(Canvas canvas, float x, float y, float width, float height,
                         float progress, int color) {
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0x30000000);
        bgPaint.setStyle(Paint.Style.FILL);
        RectF bgRect = new RectF(x, y, x + width, y + height);
        canvas.drawRoundRect(bgRect, height / 2, height / 2, bgPaint);

        if (progress > 0) {
            Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setColor(color);
            fillPaint.setStyle(Paint.Style.FILL);
            RectF fillRect = new RectF(x, y, x + width * progress, y + height);
            canvas.drawRoundRect(fillRect, height / 2, height / 2, fillPaint);
        }
    }

    // ==================== Idle Animation ====================

    private void startIdleAnimation() {
        ValueAnimator bounceAnim = ValueAnimator.ofFloat(0f, -6f, 0f);
        bounceAnim.setDuration(2000);
        bounceAnim.setRepeatCount(ValueAnimator.INFINITE);
        bounceAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        bounceAnim.addUpdateListener(animation -> {
            bounceOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
        bounceAnim.start();

        ValueAnimator tailAnim = ValueAnimator.ofFloat(-10f, 10f, -10f);
        tailAnim.setDuration(1200);
        tailAnim.setRepeatCount(ValueAnimator.INFINITE);
        tailAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        tailAnim.addUpdateListener(animation -> {
            tailAngle = (float) animation.getAnimatedValue();
        });
        tailAnim.start();

        startBlinkLoop();
    }

    private void startBlinkLoop() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isSleeping) {
                    postDelayed(this, 3000);
                    return;
                }
                ValueAnimator blinkAnim = ValueAnimator.ofFloat(1f, 0.1f, 1f);
                blinkAnim.setDuration(300);
                blinkAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                blinkAnim.addUpdateListener(animation -> {
                    eyeBlinkProgress = (float) animation.getAnimatedValue();
                    invalidate();
                });
                blinkAnim.start();
                postDelayed(this, 3000 + (long)(Math.random() * 2000));
            }
        }, 2000);
    }

    public void setSleeping(boolean sleeping) {
        this.isSleeping = sleeping;
        invalidate();
    }

    // ==================== Color Helpers ====================

    private int darken(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    private int lighten(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.min(1f, hsv[2] * factor);
        hsv[1] = Math.max(0f, hsv[1] * (2f - factor)); // slightly desaturate
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    // ==================== Particle Model ====================

    private static class Particle {
        static final int TYPE_HEART = 0;
        static final int TYPE_STAR = 1;
        static final int TYPE_NOTE = 2;

        int type;
        float x, y, vx, vy, life, decay, size;
    }
}
