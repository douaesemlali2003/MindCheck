package com.example.quizapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.Random;

public class AvatarView extends View {

    public enum Expression { NEUTRAL, HAPPY, SAD, THINKING, LISTENING }

    // ─── Couleurs ────────────────────────────────────────────────────────────────
    private static final int COLOR_BG      = 0xFF3D5A3D;  // fond vert foncé
    private static final int COLOR_WHITE   = 0xFFFFFFFF;
    private static final int COLOR_BEIGE   = 0xFFF5F0E8;
    private static final int COLOR_PUPIL   = 0xFF2D3B2D;

    // ─── Paints ──────────────────────────────────────────────────────────────────
    private final Paint bgPaint       = fill(COLOR_BG);
    private final Paint whitePaint    = fill(COLOR_WHITE);
    private final Paint pupilPaint    = fill(COLOR_PUPIL);
    private final Paint antennaPaint  = stroke(COLOR_WHITE, 0);
    private final Paint mouthPaint    = stroke(COLOR_WHITE, 0);
    private final Paint shinePaint    = fill(0xAAFFFFFF);

    // ─── État animé ──────────────────────────────────────────────────────────────
    private float eyeOpenness   = 1f;   // 0 = fermé, 1 = ouvert
    private float mouthCurve    = 0f;   // -1 grimace, 0 ligne, +1 sourire
    private float mouthOpenness = 0f;   // 0 fermé, 1 ovale
    private float pupilOffsetX  = 0f;   // décalage pupille X
    private float pupilOffsetY  = 0f;   // décalage pupille Y
    private float antennaScale  = 1f;   // pulse antenne
    private float eyeScale      = 1f;   // yeux plus grands (LISTENING)

    // Cibles
    private float tMouthCurve    = 0f;
    private float tMouthOpenness = 0f;
    private float tPupilOffsetX  = 0f;
    private float tPupilOffsetY  = 0f;
    private float tEyeScale      = 1f;

    private Expression current = Expression.NEUTRAL;
    private ValueAnimator expressionAnim;
    private ValueAnimator antennaAnim;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random  random  = new Random();

    // ─── Constructeurs ───────────────────────────────────────────────────────────
    public AvatarView(Context c)                          { super(c);       init(); }
    public AvatarView(Context c, AttributeSet a)          { super(c, a);    init(); }
    public AvatarView(Context c, AttributeSet a, int d)   { super(c, a, d); init(); }

    private void init() {
        scheduleNextBlink();
        startAntennaPulse();
    }

    // ─── API publique ────────────────────────────────────────────────────────────
    public void setExpression(Expression expr) {
        if (current == expr) return;
        current = expr;

        switch (expr) {
            case HAPPY:
                tMouthCurve = 1f; tMouthOpenness = 0f;
                tPupilOffsetX = 0f; tPupilOffsetY = 0f;
                tEyeScale = 0.75f; // yeux en demi-lune (clignote en souriant)
                break;
            case SAD:
                tMouthCurve = -1f; tMouthOpenness = 0f;
                tPupilOffsetX = 0f; tPupilOffsetY = 0.1f;
                tEyeScale = 1f;
                break;
            case THINKING:
                tMouthCurve = 0f; tMouthOpenness = 0.4f; // petit "o"
                tPupilOffsetX = 0.4f; tPupilOffsetY = -0.35f; // regard en haut à droite
                tEyeScale = 1f;
                break;
            case LISTENING:
                tMouthCurve = 0.3f; tMouthOpenness = 0.6f; // ovale ouvert
                tPupilOffsetX = 0f; tPupilOffsetY = 0f;
                tEyeScale = 1.15f; // yeux légèrement agrandis
                break;
            default: // NEUTRAL
                tMouthCurve = 0f; tMouthOpenness = 0f;
                tPupilOffsetX = 0f; tPupilOffsetY = 0f;
                tEyeScale = 1f;
                break;
        }
        animateToTarget();
    }

    // ─── Transition expression ───────────────────────────────────────────────────
    private void animateToTarget() {
        if (expressionAnim != null) expressionAnim.cancel();

        final float fMC = mouthCurve, fMO = mouthOpenness;
        final float fPX = pupilOffsetX, fPY = pupilOffsetY, fES = eyeScale;

        expressionAnim = ValueAnimator.ofFloat(0f, 1f);
        expressionAnim.setDuration(220);
        expressionAnim.setInterpolator(new DecelerateInterpolator());
        expressionAnim.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            mouthCurve    = lerp(fMC, tMouthCurve,    t);
            mouthOpenness = lerp(fMO, tMouthOpenness, t);
            pupilOffsetX  = lerp(fPX, tPupilOffsetX,  t);
            pupilOffsetY  = lerp(fPY, tPupilOffsetY,  t);
            eyeScale      = lerp(fES, tEyeScale,       t);
            invalidate();
        });
        expressionAnim.start();
    }

    // ─── Clignotement ────────────────────────────────────────────────────────────
    private void scheduleNextBlink() {
        long delay = 3000L + random.nextInt(3000);
        handler.postDelayed(blinkRunnable, delay);
    }

    private final Runnable blinkRunnable = new Runnable() {
        @Override public void run() {
            ValueAnimator close = ValueAnimator.ofFloat(1f, 0f);
            close.setDuration(70);
            close.addUpdateListener(a -> { eyeOpenness = (float) a.getAnimatedValue(); invalidate(); });
            close.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    ValueAnimator open = ValueAnimator.ofFloat(0f, 1f);
                    open.setDuration(70);
                    open.addUpdateListener(a -> { eyeOpenness = (float) a.getAnimatedValue(); invalidate(); });
                    open.addListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator animation) { scheduleNextBlink(); }
                    });
                    open.start();
                }
            });
            close.start();
        }
    };

    // ─── Pulse antenne ───────────────────────────────────────────────────────────
    private void startAntennaPulse() {
        antennaAnim = ValueAnimator.ofFloat(1f, 1.15f);
        antennaAnim.setDuration(800);
        antennaAnim.setRepeatCount(ValueAnimator.INFINITE);
        antennaAnim.setRepeatMode(ValueAnimator.REVERSE);
        antennaAnim.setInterpolator(new OvershootInterpolator(1f));
        antennaAnim.addUpdateListener(a -> { antennaScale = (float) a.getAnimatedValue(); invalidate(); });
        antennaAnim.start();
    }

    // ─── Dessin ──────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w  = getWidth();
        float h  = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r  = Math.min(w, h) * 0.46f;

        // Fond cercle vert
        canvas.drawCircle(cx, cy, r, bgPaint);

        // Épaisseurs proportionnelles
        antennaPaint.setStrokeWidth(r * 0.07f);
        antennaPaint.setStrokeCap(Paint.Cap.ROUND);
        mouthPaint.setStrokeWidth(r * 0.07f);
        mouthPaint.setStrokeCap(Paint.Cap.ROUND);

        drawAntenna(canvas, cx, cy, r);
        drawEyes(canvas, cx, cy, r);
        drawMouth(canvas, cx, cy, r);
    }

    // ─── Antenne ─────────────────────────────────────────────────────────────────
    private void drawAntenna(Canvas canvas, float cx, float cy, float r) {
        float baseX   = cx;
        float baseY   = cy - r * 0.72f;
        float stemLen = r * 0.28f;
        float tipY    = baseY - stemLen;

        // Tige verticale
        canvas.drawLine(baseX, baseY, baseX, tipY, antennaPaint);

        // Boule au sommet (pulse)
        float ballR = r * 0.09f * antennaScale;
        canvas.save();
        canvas.scale(antennaScale, antennaScale, baseX, tipY);
        canvas.drawCircle(baseX, tipY, ballR / antennaScale, whitePaint);
        canvas.restore();
    }

    // ─── Yeux ────────────────────────────────────────────────────────────────────
    private void drawEyes(Canvas canvas, float cx, float cy, float r) {
        float spread = r * 0.30f;
        float eyeR   = r * 0.145f * eyeScale;
        float pupilR = eyeR * 0.52f;
        float eyeY   = cy - r * 0.08f;

        for (int side : new int[]{-1, 1}) {
            float ex = cx + side * spread;

            canvas.save();
            // Clignotement : compression verticale
            float scaleY = current == Expression.HAPPY
                    ? Math.max(0.05f, eyeOpenness * 0.55f)   // demi-lune quand HAPPY
                    : Math.max(0.05f, eyeOpenness);
            canvas.scale(1f, scaleY, ex, eyeY);

            // Blanc de l'œil
            canvas.drawCircle(ex, eyeY, eyeR, whitePaint);

            // Pupille avec décalage
            float px = ex + pupilOffsetX * eyeR;
            float py = eyeY + pupilOffsetY * eyeR;
            // Clamp la pupille dans l'œil
            float maxOff = eyeR - pupilR;
            float dx = px - ex, dy = py - eyeY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > maxOff) { px = ex + dx / dist * maxOff; py = eyeY + dy / dist * maxOff; }

            canvas.drawCircle(px, py, pupilR, pupilPaint);

            // Reflet
            canvas.drawCircle(px + pupilR * 0.35f, py - pupilR * 0.35f, pupilR * 0.25f, shinePaint);

            canvas.restore();
        }
    }

    // ─── Bouche ──────────────────────────────────────────────────────────────────
    private void drawMouth(Canvas canvas, float cx, float cy, float r) {
        float mouthY = cy + r * 0.35f;
        float halfW  = r * 0.28f;

        if (mouthOpenness > 0.05f) {
            // Ovale ouvert (LISTENING / THINKING)
            float ow = halfW * (0.5f + 0.5f * mouthOpenness);
            float oh = r * 0.05f + r * 0.12f * mouthOpenness;
            RectF oval = new RectF(cx - ow, mouthY - oh, cx + ow, mouthY + oh);
            // Contour blanc
            whitePaint.setStyle(Paint.Style.STROKE);
            whitePaint.setStrokeWidth(r * 0.07f);
            canvas.drawOval(oval, whitePaint);
            whitePaint.setStyle(Paint.Style.FILL);

            // Si sourire aussi → arc en plus
            if (mouthCurve > 0.1f) {
                drawMouthArc(canvas, cx, mouthY + oh * 0.5f, halfW * 0.8f);
            }
            return;
        }

        // Arc simple (NEUTRAL, HAPPY, SAD)
        drawMouthArc(canvas, cx, mouthY, halfW);
    }

    private void drawMouthArc(Canvas canvas, float cx, float mouthY, float halfW) {
        float ctrlY = mouthY - mouthCurve * halfW * 0.7f;
        Path path = new Path();
        path.moveTo(cx - halfW, mouthY);
        path.quadTo(cx, ctrlY, cx + halfW, mouthY);
        canvas.drawPath(path, mouthPaint);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    private static Paint stroke(int color, float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        p.setStrokeCap(Paint.Cap.ROUND);
        return p;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacksAndMessages(null);
        if (expressionAnim != null) expressionAnim.cancel();
        if (antennaAnim    != null) antennaAnim.cancel();
    }
}
