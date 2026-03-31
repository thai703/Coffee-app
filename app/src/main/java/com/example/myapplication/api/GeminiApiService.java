package com.example.myapplication.api;

import com.example.myapplication.model.gemini.GeminiRequest;
import com.example.myapplication.model.gemini.GeminiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApiService {
    @POST("v1/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest request);

    @retrofit2.http.GET("v1beta/models")
    Call<com.google.gson.JsonObject> listModels(@Query("key") String apiKey);
}
