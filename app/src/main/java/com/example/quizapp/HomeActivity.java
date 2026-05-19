package com.example.quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private TextView tvUserName, tvWelcome, tvAvatar;
    private ImageView ivLogout;
    private CardView cardDepression, cardAnxiety, cardStress, cardMood, cardChat, cardBreathing;
    private View btnStats, btnResources;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        tvUserName = findViewById(R.id.tvUserName);
        tvWelcome  = findViewById(R.id.tvWelcome);
        tvAvatar   = findViewById(R.id.tvAvatar);
        ivLogout   = findViewById(R.id.ivLogout);
        
        cardDepression = findViewById(R.id.cardDepression);
        cardAnxiety = findViewById(R.id.cardAnxiety);
        cardStress = findViewById(R.id.cardStress);
        cardMood = findViewById(R.id.cardMood);
        cardChat = findViewById(R.id.cardChat);
        cardBreathing = findViewById(R.id.cardBreathing);

        btnStats = findViewById(R.id.btnStats);
        btnResources = findViewById(R.id.btnResources);

        // Header : prénom + initiales
        if (user != null) {
            String displayName = user.getDisplayName();
            String firstName;
            String initials;

            if (displayName != null && !displayName.trim().isEmpty()) {
                String[] parts = displayName.trim().split("\\s+");
                firstName = parts[0];
                // Initiales : première lettre du prénom + première lettre du nom (si dispo)
                initials = String.valueOf(parts[0].charAt(0)).toUpperCase();
                if (parts.length > 1) {
                    initials += String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
                }
            } else {
                // Fallback sur l'email
                String email = user.getEmail() != null ? user.getEmail() : "?";
                firstName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
                initials = String.valueOf(firstName.charAt(0)).toUpperCase();
            }

            tvWelcome.setText("Bonjour, " + firstName);
            tvAvatar.setText(initials);
        }

        // Logout
        ivLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        });

        // Navigation for Tests
        cardDepression.setOnClickListener(v -> openTest("depression"));
        cardAnxiety.setOnClickListener(v -> openTest("anxiety"));
        cardStress.setOnClickListener(v -> openTest("stress"));

        // Mood Tracker
        cardMood.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MoodTrackerActivity.class));
        });

        cardChat.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ChatBotActivity.class));
        });

        cardBreathing.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, BreathingActivity.class)));

        btnStats.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, StatsActivity.class)));

        btnResources.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ResourcesActivity.class)));
    }

    private void openTest(String type) {
        Intent intent = new Intent(HomeActivity.this, MentalTestActivity.class);
        intent.putExtra("type", type);
        startActivity(intent);
    }
}
