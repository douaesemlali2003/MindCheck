package com.example.quizapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class Register extends AppCompatActivity {

    EditText etName, etMail, etPassword, etPassword1;
    Button bRegister;
    ProgressBar progressBar;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etName      = findViewById(R.id.etName);
        etMail      = findViewById(R.id.etMail);
        etPassword  = findViewById(R.id.etPassword);
        etPassword1 = findViewById(R.id.etConfirmPass);
        bRegister   = findViewById(R.id.bRegister);
        progressBar = findViewById(R.id.progressBar);

        bRegister.setOnClickListener(v -> {
            String name      = etName.getText().toString().trim();
            String mail      = etMail.getText().toString().trim();
            String password  = etPassword.getText().toString().trim();
            String password1 = etPassword1.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Veuillez entrer votre nom", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(mail)) {
                Toast.makeText(this, "Veuillez entrer votre email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Veuillez entrer un mot de passe", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(password1)) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            bRegister.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(mail, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                            // Enregistrer le nom dans le profil Firebase
                            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            mAuth.getCurrentUser().updateProfile(profile)
                                    .addOnCompleteListener(profileTask -> {
                                        // Déconnecter immédiatement → l'utilisateur doit se connecter
                                        mAuth.signOut();
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(this,
                                                "Compte créé ! Connectez-vous pour continuer.",
                                                Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(Register.this, MainActivity.class));
                                        finish();
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            bRegister.setEnabled(true);
                            String errorMsg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Échec de l'inscription. Réessayez.";
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}