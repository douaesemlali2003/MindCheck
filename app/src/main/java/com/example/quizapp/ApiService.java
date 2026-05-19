package com.example.quizapp;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("chat")
    Call<ChatResponse> sendMessage(@Body ChatRequest request);

    @POST("analyze-test")
    Call<TestAnalysisResponse> analyzeTest(@Body TestAnalysisRequest request);

    @GET("health")
    Call<HealthResponse> checkHealth();
}
