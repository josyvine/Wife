package com.tradeanalyst.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;
import java.util.Date;

public class ErrorLogConsole {

    public static void show(final Context context, final String title, final String errorMessage, final String stackTrace, final String suggestion) {
        if (context == null) return;
        
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    final Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                    LinearLayout layout = new LinearLayout(context);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setBackgroundColor(Color.parseColor("#090D0A")); // Matching Dark Emerald velvet background
                    layout.setPadding(48, 48, 48, 48);

                    // Header Tag
                    TextView header = new TextView(context);
                    header.setText("SYSTEM TRACE CRASH/ERROR CONSOLE");
                    header.setTextColor(Color.parseColor("#FF5252"));
                    header.setTextSize(20f);
                    header.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    header.setPadding(0, 0, 0, 16);
                    layout.addView(header);

                    // Subtitle / Label
                    TextView errTitle = new TextView(context);
                    errTitle.setText(title);
                    errTitle.setTextColor(Color.WHITE);
                    errTitle.setTextSize(15f);
                    errTitle.setTypeface(android.graphics.Typeface.MONOSPACE);
                    errTitle.setPadding(0, 0, 0, 24);
                    layout.addView(errTitle);

                    // Scrollable log text body
                    ScrollView scrollView = new ScrollView(context);
                    LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
                    scrollView.setLayoutParams(scrollParams);

                    TextView logText = new TextView(context);
                    StringBuilder logBuilder = new StringBuilder();
                    logBuilder.append("=========================================\n");
                    logBuilder.append("          SYSTEM EXCEPTION DIAGNOSTICS   \n");
                    logBuilder.append("=========================================\n\n");
                    logBuilder.append("Timestamp: ").append(new Date().toString()).append("\n");
                    logBuilder.append("Thread ID: ").append(Thread.currentThread().getId()).append("\n\n");
                    logBuilder.append("[TRACE DETAILS]\n").append(errorMessage).append("\n\n");
                    
                    if (stackTrace != null && !stackTrace.isEmpty()) {
                        logBuilder.append("[SYSTEM TRACEBACK]\n").append(stackTrace).append("\n\n");
                    }
                    
                    if (suggestion != null && !suggestion.isEmpty()) {
                        logBuilder.append("💡 SOLUTION RECOMMENDATIONS:\n").append(suggestion).append("\n\n");
                    }
                    logBuilder.append("====================== EOF ==============");

                    logText.setText(logBuilder.toString());
                    logText.setTextColor(Color.parseColor("#80FFCC")); // Terminal neon green text
                    logText.setTypeface(android.graphics.Typeface.MONOSPACE);
                    logText.setTextSize(12f);
                    scrollView.addView(logText);
                    layout.addView(scrollView);

                    // Action buttons
                    Button closeBtn = new Button(context);
                    closeBtn.setText("DISMISS & SAFELY RETURN TO MAIN SCREEN");
                    closeBtn.setTextColor(Color.WHITE);
                    closeBtn.setBackgroundColor(Color.parseColor("#1B2F25"));
                    closeBtn.setPadding(24, 24, 24, 24);
                    LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    btnParams.setMargins(0, 24, 0, 0);
                    closeBtn.setLayoutParams(btnParams);
                    closeBtn.setOnClickListener(new android.view.View.OnClickListener() {
                        @Override
                        public void onClick(android.view.View v) {
                            dialog.dismiss();
                        }
                    });
                    layout.addView(closeBtn);

                    dialog.setContentView(layout);
                    dialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(runnable);
        } else {
            // Best effort handle or log
            runnable.run();
        }
    }
}
