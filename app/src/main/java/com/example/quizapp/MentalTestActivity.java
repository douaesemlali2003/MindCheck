package com.example.quizapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MentalTestActivity extends AppCompatActivity {

    private TextView tvTestTitle, tvQuestionCount, tvQuestion;
    private ProgressBar testProgressBar;
    private Button[] optionButtons = new Button[5];
    private String testType;
    private int currentQuestionIndex = 0;
    private int totalScore = 0;

    private String[] questions;
    private String[] options;
    private int[] reversedIndices;
    private AlertDialog activeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mental_test);

        testType = getIntent().getStringExtra("type");
        if (testType == null) testType = "depression";

        initViews();
        setupTestData();
        displayQuestion();
    }

    private void initViews() {
        tvTestTitle = findViewById(R.id.tvTestTitle);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvQuestion = findViewById(R.id.tvQuestion);
        testProgressBar = findViewById(R.id.testProgressBar);

        optionButtons[0] = findViewById(R.id.btnOption1);
        optionButtons[1] = findViewById(R.id.btnOption2);
        optionButtons[2] = findViewById(R.id.btnOption3);
        optionButtons[3] = findViewById(R.id.btnOption4);
        optionButtons[4] = findViewById(R.id.btnOption5);

        for (int i = 0; i < 5; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> handleAnswer(index));
        }
    }

    private void setupTestData() {
        if (testType.equals("depression")) {
            tvTestTitle.setText("Test de Dépression - PHQ-9");
            questions = new String[]{
                    "Peu d'intérêt ou de plaisir à faire les choses",
                    "Se sentir triste, déprimé(e) ou désespéré(e)",
                    "Difficultés à s'endormir ou à rester endormi(e), ou dormir trop",
                    "Se sentir fatigué(e) ou manquer d'énergie",
                    "Avoir peu d'appétit ou manger trop",
                    "Avoir une mauvaise opinion de soi-même",
                    "Difficultés à se concentrer",
                    "Bouger ou parler si lentement que les autres l'ont remarqué, ou au contraire être agité(e)",
                    "Penser qu'il vaudrait mieux mourir ou se faire du mal"
            };
            options = new String[]{"Jamais", "Quelques jours", "Plus de la moitié du temps", "Presque chaque jour"};
            optionButtons[4].setVisibility(View.GONE);
        } else if (testType.equals("anxiety")) {
            tvTestTitle.setText("Test d'Anxiété - GAD-7");
            questions = new String[]{
                    "Se sentir nerveux(se), anxieux(se) ou à bout",
                    "Être incapable d'arrêter de s'inquiéter",
                    "S'inquiéter trop pour différentes choses",
                    "Avoir du mal à se détendre",
                    "Être si agité(e) qu'il est difficile de rester assis(e)",
                    "Être facilement contrarié(e) ou irritable",
                    "Avoir peur que quelque chose de terrible puisse arriver"
            };
            options = new String[]{"Jamais", "Quelques jours", "Plus de la moitié du temps", "Presque chaque jour"};
            optionButtons[4].setVisibility(View.GONE);
        } else {
            tvTestTitle.setText("Test de Stress - PSS");
            questions = new String[]{
                    "Avez-vous été contrarié(e) par un événement inattendu ?",
                    "Avez-vous eu le sentiment de ne pas pouvoir contrôler les choses importantes ?",
                    "Vous êtes-vous senti(e) nerveux(se) et stressé(e) ?",
                    "Vous êtes-vous senti(e) confiant(e) dans votre capacité à gérer vos problèmes ?",
                    "Avez-vous senti que les choses allaient comme vous le vouliez ?",
                    "Avez-vous senti que vous ne pouviez pas faire face à tout ce que vous aviez à faire ?",
                    "Avez-vous été capable de contrôler les irritations dans votre vie ?",
                    "Avez-vous eu le sentiment de maîtriser la situation ?",
                    "Avez-vous été en colère à cause de choses hors de votre contrôle ?",
                    "Avez-vous senti que les difficultés s'accumulaient au point de ne plus pouvoir les surmonter ?"
            };
            options = new String[]{"Jamais", "Rarement", "Parfois", "Souvent", "Très souvent"};
            reversedIndices = new int[]{3, 4, 6, 7};
            optionButtons[4].setVisibility(View.VISIBLE);
        }

        testProgressBar.setMax(questions.length);
        for (int i = 0; i < options.length; i++) {
            optionButtons[i].setText(options[i]);
        }
    }

    private void displayQuestion() {
        tvQuestion.setText(questions[currentQuestionIndex]);
        tvQuestionCount.setText("Question " + (currentQuestionIndex + 1) + "/" + questions.length);
        testProgressBar.setProgress(currentQuestionIndex + 1);
    }

    private void handleAnswer(int answerIndex) {
        int score = answerIndex;

        if (testType.equals("stress") && isReversed(currentQuestionIndex)) {
            score = 4 - answerIndex;
        }

        totalScore += score;
        currentQuestionIndex++;

        if (currentQuestionIndex < questions.length) {
            displayQuestion();
        } else {
            showResult();
        }
    }

    private boolean isReversed(int index) {
        if (reversedIndices == null) return false;
        for (int r : reversedIndices) {
            if (r == index) return true;
        }
        return false;
    }

    private void showResult() {
        String interpretation = "";
        int color = Color.GREEN;
        String fallbackRecommendations = "";

        if (testType.equals("depression")) {
            if (totalScore <= 4) { interpretation = "Minimal"; color = Color.parseColor("#4CAF50"); fallbackRecommendations = "Continuez à prendre soin de vous. Pratiquez la gratitude."; }
            else if (totalScore <= 9) { interpretation = "Léger"; color = Color.parseColor("#FF9800"); fallbackRecommendations = "Surveillez vos symptômes. Parlez-en à un proche."; }
            else if (totalScore <= 14) { interpretation = "Modéré"; color = Color.parseColor("#F44336"); fallbackRecommendations = "Il serait utile de consulter un professionnel de santé."; }
            else if (totalScore <= 19) { interpretation = "Modérément sévère"; color = Color.parseColor("#B71C1C"); fallbackRecommendations = "Consultez rapidement un psychologue ou médecin."; }
            else { interpretation = "Sévère"; color = Color.parseColor("#B71C1C"); fallbackRecommendations = "Action immédiate recommandée. Contactez un service d'aide."; }
        } else if (testType.equals("anxiety")) {
            if (totalScore <= 4) { interpretation = "Minimal"; color = Color.parseColor("#4CAF50"); fallbackRecommendations = "Tout semble normal. Pratiquez la respiration profonde."; }
            else if (totalScore <= 9) { interpretation = "Léger"; color = Color.parseColor("#FF9800"); fallbackRecommendations = "Essayez de réduire les sources de stress quotidiennes."; }
            else if (totalScore <= 14) { interpretation = "Modéré"; color = Color.parseColor("#F44336"); fallbackRecommendations = "Consultez pour apprendre à mieux gérer votre anxiété."; }
            else { interpretation = "Sévère"; color = Color.parseColor("#B71C1C"); fallbackRecommendations = "L'anxiété impacte trop votre vie. Consultez un spécialiste."; }
        } else {
            if (totalScore <= 13) { interpretation = "Faible stress"; color = Color.parseColor("#4CAF50"); fallbackRecommendations = "Votre niveau de stress est sain. Continuez ainsi."; }
            else if (totalScore <= 26) { interpretation = "Stress modéré"; color = Color.parseColor("#FF9800"); fallbackRecommendations = "Accordez-vous plus de pauses et de moments de détente."; }
            else { interpretation = "Stress élevé"; color = Color.parseColor("#F44336"); fallbackRecommendations = "Votre stress est trop élevé. Apprenez des techniques de gestion du stress."; }
        }

        saveResult(interpretation);

        final String finalInterpretation = interpretation;
        final int    finalColor          = color;
        final String finalFallback       = fallbackRecommendations;

        // Montrer un résultat rapide immédiatement, puis enrichir avec l'IA si dispo
        displayResultDialog(buildFallbackMessage(finalInterpretation, finalFallback), finalColor);

        // Appel backend en arrière-plan pour analyse IA (non bloquant)
        TestAnalysisRequest request = new TestAnalysisRequest(testType, totalScore);
        RetrofitClient.getInstance().getApiService().analyzeTest(request)
                .enqueue(new Callback<TestAnalysisResponse>() {
                    @Override
                    public void onResponse(Call<TestAnalysisResponse> call,
                                           Response<TestAnalysisResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        String analysis = response.body().getAnalysis();
                        List<String> recs = response.body().getRecommendations();
                        if ((analysis == null || analysis.isEmpty()) &&
                            (recs == null || recs.isEmpty())) return;

                        StringBuilder msg = new StringBuilder();
                        msg.append("Score : ").append(totalScore)
                           .append("\nInterprétation : ").append(finalInterpretation);
                        if (analysis != null && !analysis.isEmpty())
                            msg.append("\n\nAnalyse IA :\n").append(analysis);
                        if (recs != null && !recs.isEmpty()) {
                            msg.append("\n\nRecommandations :");
                            for (String r : recs) msg.append("\n• ").append(r);
                        }
                        // Mettre à jour le dialog si encore ouvert
                        runOnUiThread(() -> updateDialogMessage(msg.toString()));
                    }

                    @Override
                    public void onFailure(Call<TestAnalysisResponse> call, Throwable t) {
                        // Pas de réseau ou serveur éteint — le dialog fallback est déjà affiché
                    }
                });
    }

    private String buildFallbackMessage(String interpretation, String recommendations) {
        return "Score : " + totalScore + "\nInterprétation : " + interpretation + "\n\n" + recommendations;
    }

    private void displayResultDialog(String message, int buttonColor) {
        if (isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Résultat du Test");
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("Retour à l'accueil", (dialog, which) -> {
            startActivity(new Intent(MentalTestActivity.this, HomeActivity.class));
            finish();
        });

        activeDialog = builder.create();
        activeDialog.show();
        activeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(buttonColor);
    }

    private void updateDialogMessage(String message) {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.setMessage(message);
        }
    }

    private void saveResult(String interpretation) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("testType", testType);
        result.put("score", totalScore);
        result.put("interpretation", interpretation);
        result.put("date", com.google.firebase.Timestamp.now());

        db.collection("test_results")
                .add(result)
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show());
    }
}
