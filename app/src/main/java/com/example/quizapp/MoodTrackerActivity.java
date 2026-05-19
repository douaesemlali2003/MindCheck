package com.example.quizapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodTrackerActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA       = 300;
    private static final int REQUEST_CAMERA_PERM  = 301;
    private static final int REQUEST_GALLERY      = 302;

    private TextView[]   moodViews = new TextView[5];
    private EditText     etNote;
    private Button       btnSaveMood;
    private Button       btnAddPhoto;
    private ImageView    ivMoodPhoto;
    private RecyclerView rvMoodHistory;
    private MoodAdapter  adapter;
    private List<MoodEntry> moodList = new ArrayList<>();

    private int    selectedMood = 0;
    private String currentPhotoBase64 = null;
    private Uri    photoUri;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_tracker);

        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        initViews();
        setupRecyclerView();
        loadMoodHistory();
    }

    private void initViews() {
        moodViews[0] = findViewById(R.id.mood1);
        moodViews[1] = findViewById(R.id.mood2);
        moodViews[2] = findViewById(R.id.mood3);
        moodViews[3] = findViewById(R.id.mood4);
        moodViews[4] = findViewById(R.id.mood5);

        etNote      = findViewById(R.id.etNote);
        btnSaveMood = findViewById(R.id.btnSaveMood);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        ivMoodPhoto = findViewById(R.id.ivMoodPhoto);

        rvMoodHistory = findViewById(R.id.rvMoodHistory);

        for (int i = 0; i < 5; i++) {
            final int moodLevel = i + 1;
            moodViews[i].setOnClickListener(v -> selectMood(moodLevel));
        }

        btnSaveMood.setOnClickListener(v -> saveMood());
        btnAddPhoto.setOnClickListener(v -> onCameraClicked());
    }

    // ─── Caméra ──────────────────────────────────────────────────────────────────

    private void onCameraClicked() {
        new AlertDialog.Builder(this)
                .setTitle("Ajouter une photo")
                .setItems(new String[]{"Prendre une photo", "Choisir depuis la galerie"},
                        (dialog, which) -> {
                            if (which == 0) {
                                requestCameraAndLaunch();
                            } else {
                                launchGallery();
                            }
                        })
                .show();
    }

    private void requestCameraAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERM);
        } else {
            launchCamera();
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this,
                    "com.example.quizapp.provider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Impossible de créer le fichier photo", Toast.LENGTH_SHORT).show();
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Aucune application caméra disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String stamp     = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("MOOD_" + stamp, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        Uri sourceUri = null;
        if (requestCode == REQUEST_CAMERA) {
            // La photo est dans photoUri (écrite par la caméra via FileProvider)
            sourceUri = photoUri;
        } else if (requestCode == REQUEST_GALLERY && data != null) {
            sourceUri = data.getData();
        }

        if (sourceUri == null) return;

        try {
            Bitmap full = BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(sourceUri));
            if (full == null) {
                Toast.makeText(this, "Impossible de lire l'image", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap compressed = scaleBitmap(full, 400);
            ivMoodPhoto.setImageBitmap(compressed);
            ivMoodPhoto.setPadding(0, 0, 0, 0);
            btnAddPhoto.setText("Changer la photo");
            currentPhotoBase64 = bitmapToBase64(compressed);
        } catch (IOException e) {
            Toast.makeText(this, "Erreur lors du chargement de l'image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap scaleBitmap(Bitmap src, int maxSize) {
        int w = src.getWidth(), h = src.getHeight();
        float scale = Math.min((float) maxSize / w, (float) maxSize / h);
        if (scale >= 1f) return src;
        return Bitmap.createScaledBitmap(src, (int)(w * scale), (int)(h * scale), true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 65, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    // ─── Mood ────────────────────────────────────────────────────────────────────

    private void selectMood(int level) {
        selectedMood = level;
        for (int i = 0; i < 5; i++) {
            if (i == level - 1) {
                moodViews[i].setBackgroundResource(R.drawable.mood_selected_bg);
                animateScale(moodViews[i]);
            } else {
                moodViews[i].setBackgroundColor(Color.TRANSPARENT);
                moodViews[i].setScaleX(1.0f);
                moodViews[i].setScaleY(1.0f);
            }
        }
    }

    private void animateScale(View view) {
        ScaleAnimation anim = new ScaleAnimation(
                1.0f, 1.2f, 1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setDuration(200);
        anim.setFillAfter(true);
        view.startAnimation(anim);
    }

    // ─── Sauvegarde ──────────────────────────────────────────────────────────────

    private void saveMood() {
        if (selectedMood == 0) {
            Toast.makeText(this, "Veuillez sélectionner une humeur", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etNote.getText().toString().trim();
        Map<String, Object> entry = new HashMap<>();
        entry.put("userId",    userId);
        entry.put("moodLevel", selectedMood);
        entry.put("note",      note);
        entry.put("date",      Timestamp.now());
        if (currentPhotoBase64 != null) {
            entry.put("photo", currentPhotoBase64);
        }

        btnSaveMood.setEnabled(false);
        db.collection("mood_entries")
                .add(entry)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Humeur enregistrée !", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveMood.setEnabled(true);
                    Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show();
                });
    }

    // ─── Historique ──────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        rvMoodHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MoodAdapter(moodList);
        rvMoodHistory.setAdapter(adapter);
    }

    private void loadMoodHistory() {
        if (userId == null) return;
        db.collection("mood_entries")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    moodList.clear();
                    List<MoodEntry> all = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        MoodEntry e = doc.toObject(MoodEntry.class);
                        all.add(e);
                    }
                    // Trier par date décroissante côté client
                    all.sort((a, b) -> {
                        if (a.getDate() == null) return 1;
                        if (b.getDate() == null) return -1;
                        return b.getDate().compareTo(a.getDate());
                    });
                    int limit = Math.min(all.size(), 7);
                    moodList.addAll(all.subList(0, limit));
                    adapter.notifyDataSetChanged();
                });
    }
}
