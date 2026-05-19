package com.example.quizapp;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class BreathingActivity extends AppCompatActivity {

    // Techniques : chaque phase en millisecondes, null = phase ignorée
    // Ordre des phases : inspire, retiens, expire, retiens
    private static final long[][] TECHNIQUE_478    = {{4000}, {7000}, {8000}, null};
    private static final long[][] TECHNIQUE_BOX    = {{4000}, {4000}, {4000}, {4000}};
    private static final long[][] TECHNIQUE_COHERE = {{5000}, null,   {5000}, null};

    private static final String[] DESC_478 = {
            "Inspirez...", "Retenez...", "Expirez...", null
    };
    private static final String[] DESC_BOX = {
            "Inspirez...", "Retenez...", "Expirez...", "Retenez..."
    };
    private static final String[] DESC_COHERE = {
            "Inspirez...", null, "Expirez...", null
    };

    private static final String[] INFO_478    = {"La technique 4-7-8 active le système nerveux parasympathique et réduit l'anxiété rapidement. Idéale avant de dormir ou lors d'un pic de stress."};
    private static final String[] INFO_BOX    = {"La respiration carrée (Box Breathing) est utilisée par les forces spéciales pour garder le calme et améliorer la concentration sous pression."};
    private static final String[] INFO_COHERE = {"La cohérence cardiaque à 5-5 synchronise le rythme cardiaque et respiratoire. Pratiquer 3 fois par jour, 5 minutes, réduit le cortisol."};

    private View        breathingCircle;
    private TextView    tvPhase;
    private TextView    tvCountdown;
    private TextView    tvCycles;
    private TextView    tvTechniqueDesc;
    private Button      btnStartBreathing;
    private RadioGroup  rgTechnique;

    private boolean       isRunning   = false;
    private int           cycleCount  = 0;
    private int           phaseIndex  = 0;
    private CountDownTimer currentTimer;

    private long[][]   currentDurations;
    private String[]   currentLabels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breathing);

        breathingCircle  = findViewById(R.id.breathingCircle);
        tvPhase          = findViewById(R.id.tvPhase);
        tvCountdown      = findViewById(R.id.tvCountdown);
        tvCycles         = findViewById(R.id.tvCycles);
        tvTechniqueDesc  = findViewById(R.id.tvTechniqueDesc);
        btnStartBreathing = findViewById(R.id.btnStartBreathing);
        rgTechnique      = findViewById(R.id.rgTechnique);

        rgTechnique.setOnCheckedChangeListener((group, checkedId) -> {
            if (isRunning) stopBreathing();
            updateTechniqueDescription(checkedId);
        });

        btnStartBreathing.setOnClickListener(v -> {
            if (!isRunning) startBreathing();
            else stopBreathing();
        });

        updateTechniqueDescription(R.id.rb478);
    }

    private void updateTechniqueDescription(int checkedId) {
        if (checkedId == R.id.rb478) {
            tvTechniqueDesc.setText(INFO_478[0]);
        } else if (checkedId == R.id.rbBox) {
            tvTechniqueDesc.setText(INFO_BOX[0]);
        } else {
            tvTechniqueDesc.setText(INFO_COHERE[0]);
        }
    }

    // ─── Contrôle ────────────────────────────────────────────────────────────────

    private void startBreathing() {
        int checkedId = rgTechnique.getCheckedRadioButtonId();
        if (checkedId == R.id.rb478) {
            currentDurations = TECHNIQUE_478;
            currentLabels    = DESC_478;
        } else if (checkedId == R.id.rbBox) {
            currentDurations = TECHNIQUE_BOX;
            currentLabels    = DESC_BOX;
        } else {
            currentDurations = TECHNIQUE_COHERE;
            currentLabels    = DESC_COHERE;
        }

        isRunning  = true;
        cycleCount = 0;
        phaseIndex = 0;
        rgTechnique.setEnabled(false);
        setRadioGroupEnabled(false);
        btnStartBreathing.setText("Arrêter");
        tvCycles.setText("Cycles complétés : 0");

        runPhase();
    }

    private void stopBreathing() {
        isRunning = false;
        if (currentTimer != null) currentTimer.cancel();
        breathingCircle.clearAnimation();
        tvPhase.setText("Prêt ?");
        tvCountdown.setText("");
        btnStartBreathing.setText("Commencer");
        setRadioGroupEnabled(true);
    }

    // ─── Cycle de phases ─────────────────────────────────────────────────────────

    private void runPhase() {
        if (!isRunning) return;

        // Saute les phases null (ex: cohérence cardiaque n'a pas de rétention)
        while (phaseIndex < 4 && currentDurations[phaseIndex] == null) {
            phaseIndex++;
        }

        // Fin d'un cycle complet
        if (phaseIndex >= 4) {
            cycleCount++;
            tvCycles.setText("Cycles complétés : " + cycleCount);
            phaseIndex = 0;
            runPhase();
            return;
        }

        long duration = currentDurations[phaseIndex][0];
        String label  = currentLabels[phaseIndex];

        tvPhase.setText(label);
        animateCircle(phaseIndex, duration);
        startCountdown(duration, phaseIndex);
    }

    private void startCountdown(long durationMs, final int phase) {
        if (currentTimer != null) currentTimer.cancel();

        currentTimer = new CountDownTimer(durationMs, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secs = (int) Math.ceil(millisUntilFinished / 1000.0);
                tvCountdown.setText(String.valueOf(secs));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("");
                phaseIndex++;
                runPhase();
            }
        }.start();
    }

    // ─── Animation cercle ────────────────────────────────────────────────────────

    private void animateCircle(int phase, long duration) {
        breathingCircle.clearAnimation();

        float fromScale, toScale;
        // Phase 0 = inspire → grandit, phase 2 = expire → rétrécit, sinon fixe
        if (phase == 0) {
            fromScale = 1.0f; toScale = 1.6f;
        } else if (phase == 2) {
            fromScale = 1.6f; toScale = 1.0f;
        } else {
            return; // rétention : pas d'animation
        }

        ScaleAnimation anim = new ScaleAnimation(
                fromScale, toScale,
                fromScale, toScale,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(duration);
        anim.setFillAfter(true);
        breathingCircle.startAnimation(anim);
    }

    private void setRadioGroupEnabled(boolean enabled) {
        for (int i = 0; i < rgTechnique.getChildCount(); i++) {
            rgTechnique.getChildAt(i).setEnabled(enabled);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBreathing();
    }
}
