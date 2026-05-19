package com.example.quizapp;

import com.google.gson.annotations.SerializedName;

public class TestAnalysisRequest {

    @SerializedName("test_type")
    private String testType;

    @SerializedName("score")
    private int score;

    public TestAnalysisRequest(String testType, int score) {
        this.testType = testType;
        this.score = score;
    }

    public String getTestType() { return testType; }
    public int getScore() { return score; }
}
