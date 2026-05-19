package com.example.quizapp;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TestAnalysisResponse {

    @SerializedName("analysis")
    private String analysis;

    @SerializedName("recommendations")
    private List<String> recommendations;

    public String getAnalysis() { return analysis; }
    public List<String> getRecommendations() { return recommendations; }
}
