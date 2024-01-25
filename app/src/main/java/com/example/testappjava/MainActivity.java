package com.example.testappjava;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.Toast;

import com.example.testappjava.Repository.QueryUPIStatus;
import com.example.testappjava.Services.CanaraRecorderService;

import java.util.List;

import com.example.testappjava.Utils.Config;
import com.example.testappjava.Utils.SharedData;

public class MainActivity extends AppCompatActivity {

    private EditText editText1, editText2, editText3;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isAccessibilityServiceEnabled(this, CanaraRecorderService.class)) {
            showAccessibilityDialog();
        }

        Intent serviceIntent = new Intent(this, CanaraRecorderService.class);
        startService(serviceIntent);

        editText1 = findViewById(R.id.editText1);
        editText2 = findViewById(R.id.editText2);
        editText3 = findViewById(R.id.editText3);

        sharedPreferences = getSharedPreferences(Config.packageName, MODE_PRIVATE);
        editText1.setText(sharedPreferences.getString("loginId", ""));
        editText2.setText(sharedPreferences.getString("loginPin", ""));
        editText3.setText(sharedPreferences.getString("bankLoginId", ""));

    }

    public void onAppFlowStarted(View view) {
        String text1 = editText1.getText().toString().trim();
        String text2 = editText2.getText().toString().trim();
        String text3 = editText3.getText().toString().trim();

        if (text1.isEmpty() || text2.isEmpty()) {
            Toast.makeText(this, "Both text fields must be filled.", Toast.LENGTH_SHORT).show();
            return;
        }

        Config.loginId = text1;
        Config.loginPin = text2;
        Config.bankLoginId = text3;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("loginId", text1);
        editor.putString("loginPin", text2);
        editor.putString("bankLoginId", text3);
        editor.apply();

        new QueryUPIStatus(() -> {

            SharedData.startedChecking = true;
            Intent intent = getPackageManager().getLaunchIntentForPackage(Config.packageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                SharedData.startedChecking = true;
                runOnUiThread(() -> {
                    Intent serviceIntent;
                    if (!isAccessibilityServiceEnabled(this, CanaraRecorderService.class)) {
                        showAccessibilityDialog();
                    } else {
                        serviceIntent = new Intent(this, CanaraRecorderService.class);
                        startService(serviceIntent);
                    }
                });

            }, 1000);
        }, () -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Scrapper inactive", Toast.LENGTH_LONG).show();
            });
        }).evaluate();


    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> serviceClass) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null) {
            List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
            for (AccessibilityServiceInfo service : enabledServices) {
                ComponentName enabledServiceComponentName = new ComponentName(service.getResolveInfo().serviceInfo.packageName, service.getResolveInfo().serviceInfo.name);
                ComponentName expectedServiceComponentName = new ComponentName(context, serviceClass);
                if (enabledServiceComponentName.equals(expectedServiceComponentName)) {
                    Log.d("App", "Application has accessibility permissions");
                    return true;
                }
            }
        }
        Log.d("App", "Application does not have accessibility permissions");
        return false;
    }

    private void showAccessibilityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accessibility Permission Required");
        builder.setMessage("To use this app, you need to enable Accessibility Service. Go to Settings to enable it?");
        builder.setPositiveButton("Settings", (dialog, which) -> openAccessibilitySettings());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.show();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }
}