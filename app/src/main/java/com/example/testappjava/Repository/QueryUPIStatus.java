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

public class QueryUPIStatus {

    final Runnable callback;
    final Runnable inactiveCallback;
    private String API_URL = Config.baseUrl + "GetUpiStatus?upiId=" + Config.loginId;

    public QueryUPIStatus(Runnable callback, Runnable inactiveCallback) {
        this.callback = callback;
        this.inactiveCallback = inactiveCallback;
    }

    public void evaluate() {

        if (Config.kDebugMode) {
            Log.d("DEBUG", "Application is in Debug Mode");
            callback.run();
            return;
        }


        Log.d("API_URL", API_URL);
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
                    try {
                        JsonObject output = new Gson().fromJson(responseData, JsonObject.class);
                        Log.d("ApiCallTask", "API Response: " + output.toString());
                        if (output.has("Result")) {
                            if (output.get("Result").getAsString().equals("1")) {
                                Log.d("UPI Status", "Active");
                                callback.run();
                            } else {
                                Log.d("UPI Status", "Inactive");
                                inactiveCallback.run();
                            }
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
