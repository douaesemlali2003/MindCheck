package com.example.quizapp;

import com.google.firebase.Timestamp;

public class MoodEntry {
    private String userId;
    private int moodLevel;
    private String note;
    private Timestamp date;
    private String photo; // Base64 JPEG compressé

    public MoodEntry() {} // Required for Firestore

    public MoodEntry(String userId, int moodLevel, String note, Timestamp date, String photo) {
        this.userId = userId;
        this.moodLevel = moodLevel;
        this.note = note;
        this.date = date;
        this.photo = photo;
    }

    public String getUserId()   { return userId; }
    public int getMoodLevel()   { return moodLevel; }
    public String getNote()     { return note; }
    public Timestamp getDate()  { return date; }
    public String getPhoto()    { return photo; }

    public String getEmoji() {
        switch (moodLevel) {
            case 1: return "😢";
            case 2: return "😕";
            case 3: return "😐";
            case 4: return "🙂";
            case 5: return "😄";
            default: return "😐";
        }
    }
}
