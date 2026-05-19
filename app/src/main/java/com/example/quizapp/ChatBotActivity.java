package com.example.quizapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatBotActivity extends AppCompatActivity {

    private static final int REQUEST_SPEECH_INPUT = 100;
    private static final int REQUEST_RECORD_AUDIO = 101;

    private RecyclerView    rvChat;
    private EditText        etMessage;
    private ImageButton     btnSend;
    private LinearLayout    btnMic;
    private ProgressBar     progressBarChat;
    private AvatarView      avatarView;
    private MessageAdapter  adapter;
    private List<Message>   messageList = new ArrayList<>();
    private String          sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        rvChat          = findViewById(R.id.rvChat);
        etMessage       = findViewById(R.id.etMessage);
        btnSend         = findViewById(R.id.btnSend);
        btnMic          = findViewById(R.id.btnMic);
        progressBarChat = findViewById(R.id.progressBarChat);
        avatarView      = findViewById(R.id.avatarView);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        sessionId = (user != null) ? user.getUid() : "anonymous";

        adapter = new MessageAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        addBotMessage("Bonjour ! Je suis ton compagnon MindCheck. Comment te sens-tu aujourd'hui ? Je suis là pour t'écouter sans jugement.");

        // Frappe → LISTENING
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    avatarView.setExpression(AvatarView.Expression.LISTENING);
                } else {
                    avatarView.setExpression(AvatarView.Expression.NEUTRAL);
                }
            }
        });

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                addUserMessage(text);
                etMessage.setText("");
                sendMessageToBackend(text);
            }
        });

        btnMic.setOnClickListener(v -> onMicClicked());
    }

    // ─── Analyse d'émotion ───────────────────────────────────────────────────────

    private AvatarView.Expression analyzeEmotion(String text) {
        if (text == null) return AvatarView.Expression.NEUTRAL;
        String low = text.toLowerCase();

        String[] positive = {
            "bien", "merci", "content", "heureux", "heureuse", "super", "courage",
            "bravo", "parfait", "magnifique", "excellent", "génial", "mieux",
            "sourire", "joie", "positif", "espoir", "calme", "tranquille"
        };
        String[] negative = {
            "triste", "tristesse", "mal", "stress", "stressé", "déprim", "anxiété",
            "anxieux", "peur", "souffr", "douleur", "difficile", "pleur", "larme",
            "seul", "solitude", "désespoir", "perdu", "épuisé", "nul", "horrible"
        };

        for (String w : positive) if (low.contains(w)) return AvatarView.Expression.HAPPY;
        for (String w : negative) if (low.contains(w)) return AvatarView.Expression.SAD;
        return AvatarView.Expression.NEUTRAL;
    }

    // ─── Speech-to-Text ──────────────────────────────────────────────────────────

    private void onMicClicked() {
        Intent probe = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if (getPackageManager().queryIntentActivities(probe, 0).isEmpty()) {
            Toast.makeText(this, "Fonctionnalité non disponible sur cet appareil", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        } else {
            launchSpeechRecognizer();
        }
    }

    private void launchSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        startActivityForResult(intent, REQUEST_SPEECH_INPUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String recognized = results.get(0);
                etMessage.setText(recognized);
                etMessage.setSelection(recognized.length());
                avatarView.setExpression(AvatarView.Expression.LISTENING);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchSpeechRecognizer();
            } else {
                Toast.makeText(this, "Permission micro refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─── Chat ────────────────────────────────────────────────────────────────────

    private void addUserMessage(String text) {
        messageList.add(new Message(text, Message.TYPE_USER));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChat.post(() -> rvChat.smoothScrollToPosition(messageList.size() - 1));
    }

    private void addBotMessage(String text) {
        messageList.add(new Message(text, Message.TYPE_BOT));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChat.post(() -> rvChat.smoothScrollToPosition(messageList.size() - 1));
    }

    private void sendMessageToBackend(String userText) {
        btnSend.setEnabled(false);
        btnMic.setEnabled(false);
        avatarView.setExpression(AvatarView.Expression.THINKING);

        // Afficher immédiatement l'indicateur "en train d'écrire"
        showTypingIndicator();

        ChatRequest request = new ChatRequest(userText, sessionId);
        RetrofitClient.getInstance().getApiService().sendMessage(request)
                .enqueue(new Callback<ChatResponse>() {
                    @Override
                    public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                        removeTypingIndicator();
                        btnSend.setEnabled(true);
                        btnMic.setEnabled(true);
                        String reply = null;
                        if (response.isSuccessful() && response.body() != null) {
                            reply = response.body().getResponse();
                        }
                        if (reply == null || reply.isEmpty()) reply = localFallback(userText);
                        addBotMessage(reply);
                        avatarView.setExpression(analyzeEmotion(reply));
                    }

                    @Override
                    public void onFailure(Call<ChatResponse> call, Throwable t) {
                        removeTypingIndicator();
                        btnSend.setEnabled(true);
                        btnMic.setEnabled(true);
                        String reply = localFallback(userText);
                        addBotMessage(reply);
                        avatarView.setExpression(analyzeEmotion(userText));
                    }
                });
    }

    private void showTypingIndicator() {
        messageList.add(new Message("", Message.TYPE_TYPING));
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChat.post(() -> rvChat.smoothScrollToPosition(messageList.size() - 1));
    }

    private void removeTypingIndicator() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            if (messageList.get(i).getType() == Message.TYPE_TYPING) {
                messageList.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    private String localFallback(String userText) {
        String low = userText.toLowerCase();
        if (containsAny(low, "triste", "tristesse", "pleurer", "déprimé", "malheureux"))
            return "Je suis là pour toi. La tristesse est une émotion valide — veux-tu me raconter ce qui se passe ?";
        if (containsAny(low, "anxieux", "anxiété", "angoisse", "peur", "stressé", "stress"))
            return "Je comprends. Essaie la respiration guidée dans l'app pour t'aider à te recentrer.";
        if (containsAny(low, "bien", "heureux", "content", "joie", "super"))
            return "C'est formidable ! Profite de ces bons moments et prends soin de toi.";
        if (containsAny(low, "fatigué", "épuisé", "sommeil", "nuit"))
            return "Le manque de sommeil affecte vraiment l'humeur. La respiration 4-7-8 peut aider à s'endormir.";
        if (containsAny(low, "seul", "solitude", "isolé"))
            return "Tu n'es pas seul(e) ici. Consulte la section Ressources si tu veux parler à quelqu'un.";
        if (containsAny(low, "bonjour", "salut", "bonsoir", "coucou"))
            return "Bonjour ! Comment vas-tu aujourd'hui ? Je suis là pour t'écouter.";
        String[] defaults = {
            "Je t'écoute. Peux-tu m'en dire plus ?",
            "Merci de partager ça avec moi. Comment te sens-tu en ce moment ?",
            "C'est courageux de parler de ce que tu vis."
        };
        return defaults[(int)(Math.random() * defaults.length)];
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) if (text.contains(k)) return true;
        return false;
    }
}
