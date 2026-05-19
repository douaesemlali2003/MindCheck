package com.example.quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Déconnexion automatique à chaque démarrage de l'app
        FirebaseAuth.getInstance().signOut();

        setContentView(R.layout.activity_splash);

        Button btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        });
    }
}
