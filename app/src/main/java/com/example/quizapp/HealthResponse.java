package com.example.quizapp;

import com.google.gson.annotations.SerializedName;

public class HealthResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("ollama")
    private String ollama;

    public String getStatus() { return status; }
    public String getOllama() { return ollama; }
}
