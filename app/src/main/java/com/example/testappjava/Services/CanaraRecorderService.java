package com.example.testappjava.Services;

import static com.example.testappjava.Utils.AccessibilityUtil.*;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.example.testappjava.MainActivity;
import com.example.testappjava.Repository.QueryUPIStatus;
import com.example.testappjava.Repository.SaveBankTransaction;
import com.example.testappjava.Repository.UpdateDateForScrapper;
import com.example.testappjava.Utils.AES;
import com.example.testappjava.Utils.CaptureTicker;
import com.example.testappjava.Utils.Config;
import com.example.testappjava.Utils.SharedData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CanaraRecorderService extends AccessibilityService {
    boolean loginOnce = true;
    final CaptureTicker ticker = new CaptureTicker(this::processTickerEvent);
    int appNotOpenCounter = 0;

    @Override
    protected void onServiceConnected() {
        ticker.startChecking();
        super.onServiceConnected();
    }

    private void processTickerEvent() {
        Log.d("Ticker", "Processing Event");
        Log.d("Flags", printAllFlags());
        ticker.setNotIdle();

        if (!SharedData.startedChecking) return;
        if (!MainActivity.isAccessibilityServiceEnabled(this, this.getClass())) {
            return;
        }

        AccessibilityNodeInfo rootNode = getTopMostParentNode(getRootInActiveWindow());
        if (rootNode != null) {
            if (findNodeByPackageName(rootNode, Config.packageName) == null) {
                if (appNotOpenCounter > 4) {
                    Log.d("App Status", "Not Found");
                    relaunchApp();
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    appNotOpenCounter = 0;
                    return;
                }
                appNotOpenCounter++;
            } else {
                Log.d("App Status", "Found");
                rootNode.refresh();
                listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow()));

                checkForSessionExpiry();
                enterPin();

                if ((listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("Passbook"))) {
                    menuButton();
                }

                if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("Select Account Number")) {
                    AccessibilityNodeInfo accountRadioButton = findNodeByClassName(getTopMostParentNode(getRootInActiveWindow()), "android.widget.RadioButton");
                    if (accountRadioButton != null) {
                        accountRadioButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }

                if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("A/C Type")) {
                    AccessibilityNodeInfo accountRadioButton = findNodeByClassName(getTopMostParentNode(getRootInActiveWindow()), "android.widget.RadioButton");
                    if (accountRadioButton != null) {
                        accountRadioButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }

                rootNode.refresh();

                if (listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("DATE") && listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("Withdrawal") && listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("Deposit") && listAllTextsInActiveWindow(getTopMostParentNode(getRootInActiveWindow())).contains("BALANCE")) {
                    readTransactions();
                }
            }
            rootNode.recycle();
        }
    }

    private void relaunchApp() {
        // Might fail not tested
        if (MainActivity.isAccessibilityServiceEnabled(this, this.getClass())) {
            new QueryUPIStatus(() -> {
                Intent intent = getPackageManager().getLaunchIntentForPackage(Config.packageName);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }, () -> {
                Toast.makeText(this, "Scrapper inactive", Toast.LENGTH_SHORT).show();
            }).evaluate();
        }
    }

    public void readTransactions() {
        // Create Response
        JSONArray output = new JSONArray();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        AccessibilityNodeInfo listViewNode = findNodeByClassName(getTopMostParentNode(getRootInActiveWindow()), "android.widget.ListView");
        List<String> unfilteredTransactionInfo = listAllTextsInActiveWindow(listViewNode);

        unfilteredTransactionInfo.removeIf(String::isEmpty);
        unfilteredTransactionInfo.remove("Pull down to refresh latest transaction..");
        unfilteredTransactionInfo.remove("Load older transactions...");

        Log.d("Transaction Output", unfilteredTransactionInfo.toString());


        List<List<String>> subFilteredTransactionsList = new ArrayList<>();
        unfilteredTransactionInfo.removeIf(String::isEmpty);
        for (int i = 0; i < unfilteredTransactionInfo.size(); i += 5) {
            String date = unfilteredTransactionInfo.get(i);
            String Amount = "";
            String balance = unfilteredTransactionInfo.get(i + 3);
            String description = unfilteredTransactionInfo.get(i + 4);


            JSONObject jsonObject = new JSONObject();

            boolean isDate = isDate(date, "dd-MM-yyyy");
            if (isDate) {
                if (unfilteredTransactionInfo.get(i + 1).trim().isEmpty() || unfilteredTransactionInfo.get(i + 1) == null) {
                    Amount = unfilteredTransactionInfo.get(i + 2);
                }
                if (unfilteredTransactionInfo.get(i + 2).trim().isEmpty() || unfilteredTransactionInfo.get(i + 2) == null) {
                    Amount = unfilteredTransactionInfo.get(i + 1);
                    Amount = "-" + Amount;
                }
                try {
                    jsonObject.put("Description", extractUTRFromDesc(description));
                    jsonObject.put("UPIId", getUPIId(description));
                    jsonObject.put("CreatedDate", date);
                    jsonObject.put("Amount", Amount);
                    jsonObject.put("RefNumber", extractUTRFromDesc(description));
                    jsonObject.put("AccountBalance", balance);
                    jsonObject.put("BankName", Config.bankName + Config.bankLoginId);
                    jsonObject.put("BankLoginId", Config.bankLoginId);

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                output.put(jsonObject);
            } else {
                Log.d("Not found", "");
            }
        }
        Log.d("Final Json Output", output.toString());
        if (output.length() > 0) {
            Log.d("API BODY", output.toString());
            JSONObject result = new JSONObject();
            try {
                result.put("Result", AES.encrypt(output.toString()));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            new QueryUPIStatus(() -> {
                new SaveBankTransaction(() -> {
                    Rect outBounds = new Rect();
                    getTopMostParentNode(getRootInActiveWindow()).getBoundsInScreen(outBounds);
                    swipe(outBounds.centerX(), outBounds.centerY(), outBounds.centerX(), 0, 1500);
                }, () -> {
                    Rect outBounds = new Rect();
                    getRootInActiveWindow().getBoundsInScreen(outBounds);
                    int startX = outBounds.width() / 2;
                    int startY = outBounds.height() / 2;
                    int endY = outBounds.height();
                    swipe(startX, startY, startX, endY, 1000);
                }).evaluate(result.toString());
                new UpdateDateForScrapper().evaluate();
            }, () -> {
            }).evaluate();
        }
        ticker.setNotIdle();
    }

    public void menuButton() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        AccessibilityNodeInfo passbookBtn = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Passbook", false, false);
        if (passbookBtn != null) {
            Rect outBounds = new Rect();
            passbookBtn.getBoundsInScreen(outBounds);
            performTap(outBounds.centerX(), outBounds.centerY());

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            AccessibilityNodeInfo rootNode = getTopMostParentNode(getRootInActiveWindow());
            rootNode.refresh();
            AccessibilityNodeInfo accountTypeRadioButton = findNodeByClassName(rootNode, "android.widget.RadioButton");
            if (accountTypeRadioButton != null) {
                accountTypeRadioButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            ticker.setNotIdle();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ticker.setNotIdle();
            rootNode.recycle();
        }
    }

    public void enterPin() {
        AccessibilityNodeInfo mPinTextField = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Enter 4 Digit PBPin", false, false);
        if (mPinTextField != null) {
            Bundle textBundle = new Bundle();
            textBundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, Config.loginPin);
            mPinTextField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, textBundle);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Log.d("Trying TODO: ", "Click on login button 1");
            AccessibilityNodeInfo loginBtn = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "LOGIN", false, true);
            if (loginBtn != null) {
                Log.d("Update: ", "Click on login button 2");
                loginBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                loginBtn.recycle();
            }

            textBundle.clear();
            mPinTextField.recycle();
            ticker.setNotIdle();
        }
    }

    public void checkForSessionExpiry() {
        AccessibilityNodeInfo targetNode1 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Session Expired! Please LOGIN again", true, false);
        AccessibilityNodeInfo targetNode2 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Information", false, false);
        AccessibilityNodeInfo targetNode3 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Please download latest app from playStore", true, false);
        AccessibilityNodeInfo targetNode4 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Unable to process the request,Please try again.", true, false);
        AccessibilityNodeInfo targetNode5 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "MPin cannot be blank.", true, false);
        AccessibilityNodeInfo targetNode6 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "t responding", true, false);
        if (targetNode2 != null) {
            AccessibilityNodeInfo backButtonNode = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Back", false, true);
            if (backButtonNode != null) {
                backButtonNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                backButtonNode.recycle();
                ticker.setNotIdle();
                loginOnce = true;
            }
            targetNode2.recycle();

        }
        if (targetNode1 != null || targetNode4 != null) {
            AccessibilityNodeInfo okButtonNode = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "OK", false, true);
            if (okButtonNode != null) {
                okButtonNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                okButtonNode.recycle();
                loginOnce = false;
                ticker.setNotIdle();
                relaunchApp();
            }

        }

        if (targetNode3 != null) {
            AccessibilityNodeInfo backButtonNode1 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "OK", false, true);
            AccessibilityNodeInfo backButtonNode2 = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "UPDATE NOW", false, true);
            if (backButtonNode1 != null) {
                backButtonNode1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                backButtonNode1.recycle();
                ticker.setNotIdle();
                loginOnce = false;
                relaunchApp();
            }
            if (backButtonNode2 != null) {
                backButtonNode2.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                backButtonNode2.recycle();
                ticker.setNotIdle();
                loginOnce = false;
                relaunchApp();
            }
            targetNode3.recycle();
        }
        if (targetNode5 != null) {
            relaunchApp();
            targetNode5.recycle();
        }
        if (targetNode6 != null) {
            Log.d("Inside", "Close app");
            AccessibilityNodeInfo okButtonNode = findNodeByText(getTopMostParentNode(getRootInActiveWindow()), "Close app", false, true);
            if (okButtonNode != null) {
                okButtonNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                okButtonNode.recycle();
                targetNode6.recycle();
                ticker.setNotIdle();
            }
        }


    }

    private String printAllFlags() {
        StringBuilder result = new StringBuilder();
        // Get the fields of the class
        Field[] fields = getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            try {
                Object value = field.get(this);
                result.append(fieldName).append(": ").append(value).append("\n");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return result.toString();
    }

    public void performTap(int x, int y) {
        Log.d("Accessibility", "Tapping " + x + " and " + y);
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(p, 0, 950));

        GestureDescription gestureDescription = gestureBuilder.build();

        boolean dispatchResult = false;
        dispatchResult = dispatchGesture(gestureDescription, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }
        }, null);
        Log.d("Dispatch Result", String.valueOf(dispatchResult));
    }

    public void swipe(float oldX, float oldY, float newX, float newY, long duration) {
        // Set up the Path by swiping from the old position coordinates to the new position coordinates.
        Path swipePath = new Path();
        swipePath.moveTo(oldX, oldY);
        swipePath.lineTo(newX, newY);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, duration));

        boolean dispatchResult = dispatchGesture(gestureBuilder.build(), null, null);

        try {
            Thread.sleep(duration / 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private String getUPIId(String description) {
        try {
            if (!description.contains("@")) return "";
            String[] split = description.split("/");
            String value = null;
            value = Arrays.stream(split).filter(x -> x.contains("@")).findFirst().orElse(null);
            return value != null ? value : "";
        } catch (Exception ex) {
            Log.d("Exception", ex.getMessage());
            return "";
        }
    }

    private String extractUTRFromDesc(String description) {
        try {
            String[] split = description.split(":");
            String value = null;
            value = Arrays.stream(split).filter(x -> x.length() == 12).findFirst().orElse(null);
            if (value != null) {
                return value + " " + description;
            }
            return description;
        } catch (Exception e) {
            return description;
        }
    }

    private static boolean isDate(String dateString, String dateFormat) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            sdf.setLenient(false); // Disable lenient parsing

            // Try parsing the date
            sdf.parse(dateString);
            return true; // If parsing succeeds, it's a valid date
        } catch (ParseException e) {
            return false; // If parsing fails, it's not a valid date
        }
    }

    // Unused AccessibilityService Callbacks
    @Override
    public void onInterrupt() {

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

}
