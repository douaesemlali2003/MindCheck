package com.example.quizapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {

    private TextView     tvTestCount, tvMoodCount;
    private TextView     tvDepressionResult, tvAnxietyResult, tvStressResult;
    private MiniChartView chartScores, chartMoods;
    private TextView     tvMoodEmpty;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        tvTestCount        = findViewById(R.id.tvTestCount);
        tvMoodCount        = findViewById(R.id.tvMoodCount);
        tvDepressionResult = findViewById(R.id.tvDepressionResult);
        tvAnxietyResult    = findViewById(R.id.tvAnxietyResult);
        tvStressResult     = findViewById(R.id.tvStressResult);
        chartScores        = findViewById(R.id.chartScores);
        chartMoods         = findViewById(R.id.chartMoods);
        tvMoodEmpty        = findViewById(R.id.tvMoodEmpty);

        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        if (userId != null) {
            loadTestResults();
            loadMoodData();
        }
    }

    // ─── Tests ───────────────────────────────────────────────────────────────────

    private void loadTestResults() {
        db.collection("test_results")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        tvTestCount.setText("0");
                        buildScoreChart(new HashMap<>());
                        return;
                    }

                    tvTestCount.setText(String.valueOf(snapshot.size()));

                    // Dernier résultat par type (timestamp le plus récent)
                    Map<String, QueryDocumentSnapshot> latestByType = new HashMap<>();
                    Map<String, com.google.firebase.Timestamp> latestDate = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String type = doc.getString("testType");
                        if (type == null) continue;
                        com.google.firebase.Timestamp ts = doc.getTimestamp("date");
                        com.google.firebase.Timestamp existing = latestDate.get(type);
                        if (existing == null || (ts != null && ts.compareTo(existing) > 0)) {
                            latestByType.put(type, doc);
                            latestDate.put(type, ts);
                        }
                    }

                    displayResult(latestByType.get("depression"), tvDepressionResult);
                    displayResult(latestByType.get("anxiety"),    tvAnxietyResult);
                    displayResult(latestByType.get("stress"),     tvStressResult);
                    buildScoreChart(latestByType);
                })
                .addOnFailureListener(e -> tvTestCount.setText("0"));
    }

    private void displayResult(QueryDocumentSnapshot doc, TextView tv) {
        if (doc == null) { tv.setText("Aucun test effectué"); return; }
        Long score = doc.getLong("score");
        String interp = doc.getString("interpretation");
        com.google.firebase.Timestamp date = doc.getTimestamp("date");
        String dateStr = "";
        if (date != null) {
            dateStr = " · " + new SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    .format(date.toDate());
        }
        tv.setText((score != null ? "Score " + score : "")
                + (interp != null ? " — " + interp : "")
                + dateStr);
    }

    private void buildScoreChart(Map<String, QueryDocumentSnapshot> latest) {
        // Max scores : PHQ-9=27, GAD-7=21, PSS=40
        int[][] config = {
                // {maxScore, low threshold, moderate threshold}
                {27, 4,  9},   // depression
                {21, 4,  9},   // anxiety
                {40, 13, 26},  // stress
        };
        String[] types  = {"depression", "anxiety", "stress"};
        String[] labels = {"PHQ-9", "GAD-7", "PSS"};

        List<MiniChartView.Bar> bars = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            QueryDocumentSnapshot doc = latest.get(types[i]);
            float score = 0f;
            if (doc != null) {
                Long s = doc.getLong("score");
                if (s != null) score = s;
            }
            int color = scoreColor(score, config[i][1], config[i][2]);
            bars.add(new MiniChartView.Bar(score, config[i][0], labels[i], color));
        }
        chartScores.setBars(bars);
    }

    private int scoreColor(float score, int low, int moderate) {
        if (score <= low)     return Color.parseColor("#4CAF50");
        if (score <= moderate) return Color.parseColor("#FF9800");
        return Color.parseColor("#F44336");
    }

    // ─── Humeurs ─────────────────────────────────────────────────────────────────

    private void loadMoodData() {
        db.collection("mood_entries")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        tvMoodCount.setText("0");
                        chartMoods.setVisibility(View.GONE);
                        tvMoodEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Trier par date décroissante, prendre les 7 dernières
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) docs.add(doc);
                    docs.sort((a, b) -> {
                        com.google.firebase.Timestamp ta = a.getTimestamp("date");
                        com.google.firebase.Timestamp tb = b.getTimestamp("date");
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });

                    tvMoodCount.setText(String.valueOf(docs.size()));

                    int limit = Math.min(docs.size(), 7);
                    // Inverser pour afficher du plus ancien au plus récent (gauche → droite)
                    List<QueryDocumentSnapshot> recent = docs.subList(0, limit);
                    List<MiniChartView.Bar> bars = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

                    for (int i = recent.size() - 1; i >= 0; i--) {
                        QueryDocumentSnapshot doc = recent.get(i);
                        Long level = doc.getLong("moodLevel");
                        float mood = (level != null) ? level : 3f;
                        String label = "";
                        com.google.firebase.Timestamp ts = doc.getTimestamp("date");
                        if (ts != null) label = sdf.format(ts.toDate());
                        int color = moodColor((int) mood);
                        bars.add(new MiniChartView.Bar(mood, 5f, label, color));
                    }

                    chartMoods.setBars(bars);
                })
                .addOnFailureListener(e -> {
                    tvMoodCount.setText("0");
                    chartMoods.setVisibility(View.GONE);
                    tvMoodEmpty.setVisibility(View.VISIBLE);
                });
    }

    private int moodColor(int level) {
        switch (level) {
            case 1: return Color.parseColor("#F44336");
            case 2: return Color.parseColor("#FF9800");
            case 3: return Color.parseColor("#FFC107");
            case 4: return Color.parseColor("#8BC34A");
            case 5: return Color.parseColor("#4CAF50");
            default: return Color.parseColor("#9CA89C");
        }
    }
}
