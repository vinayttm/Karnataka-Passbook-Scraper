package com.example.testappjava.Repository;

import android.util.Log;

import com.example.testappjava.Utils.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SaveBankTransaction {
    private final String API_URL = Config.baseUrl + "SaveMobilebankTransaction";

    final Runnable loadMoreCallback, listReachedCallback;

    public SaveBankTransaction(Runnable loadMoreCallback, Runnable listReachedCallback) {
        this.listReachedCallback = listReachedCallback;
        this.loadMoreCallback = loadMoreCallback;
    }

    public void evaluate(String body) {
        Log.d("API_URL", API_URL);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(body, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
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
                    try {
                        JsonObject output = new Gson().fromJson(responseData, JsonObject.class);
                        Log.d("ApiCallTask", "API Response: " + output.toString());
                        if (output.get("ErrorMessage").getAsString().contains("Already exists")) {
                            listReachedCallback.run();
                        } else {
                            loadMoreCallback.run();
                        }
                    } catch (Exception ignored) {
                    }

                } else {
                    // Handle the error
                    Log.d("ApiCallTask", "API Response: " + response.body().string());
                }
            }
        });
    }
}
