package com.example.testappjava.Repository;

import android.util.Log;

import com.example.testappjava.Utils.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateDateForScrapper {

    private String API_URL = Config.baseUrl + "UpdateDateBasedOnUpi?upiId=" + Config.loginId;

    public void evaluate() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(API_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle network or request error
                Log.d("ApiCallTask", "API Response: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JsonObject output = new Gson().fromJson(responseData, JsonObject.class);
                    Log.d("ApiCallTask", "API Response: " + output.toString());
                } else {
                    // Handle the error
                    Log.d("ApiCallTask", "API Response: " + response.body().string());
                }
            }
        });
    }
}
