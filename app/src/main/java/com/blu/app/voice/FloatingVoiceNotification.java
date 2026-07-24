package com.blu.app.voice;

import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import androidx.appcompat.app.AppCompatActivity;

public class FloatingVoiceNotification {
    private AppCompatActivity activity;
    private String botToken, chatId, voiceToken;
    
    public FloatingVoiceNotification(AppCompatActivity activity, String botToken, String chatId, String voiceToken) {
        this.activity = activity;
        this.botToken = botToken;
        this.chatId = chatId;
        this.voiceToken = voiceToken;
    }
    
    public void show() {
        FrameLayout container = new FrameLayout(activity);
        container.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        container.setBackgroundColor(Color.argb(200, 0, 0, 0));
        
        LinearLayout dialog = new LinearLayout(activity);
        dialog.setOrientation(LinearLayout.VERTICAL);
        dialog.setBackgroundColor(Color.WHITE);
        dialog.setPadding(40, 40, 40, 40);
        dialog.setElevation(20);
        
        FrameLayout.LayoutParams dialogParams = new FrameLayout.LayoutParams(600, FrameLayout.LayoutParams.WRAP_CONTENT);
        dialogParams.gravity = Gravity.CENTER;
        dialog.setLayoutParams(dialogParams);
        
        TextView title = new TextView(activity);
        title.setText("Record Voice");
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = 20;
        dialog.addView(title, titleParams);
        
        LinearLayout buttonsLayout = new LinearLayout(activity);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.CENTER);
        
        Button rejectBtn = new Button(activity);
        rejectBtn.setText("Decline");
        rejectBtn.setTextColor(Color.WHITE);
        rejectBtn.setBackgroundColor(Color.RED);
        LinearLayout.LayoutParams rejectParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        rejectParams.setMargins(0, 0, 15, 0);
        rejectBtn.setLayoutParams(rejectParams);
        rejectBtn.setOnClickListener(v -> removeFromParent(container, activity));
        
        Button startBtn = new Button(activity);
        startBtn.setText("Start");
        startBtn.setTextColor(Color.WHITE);
        startBtn.setBackgroundColor(Color.GREEN);
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        startBtn.setLayoutParams(startParams);
        startBtn.setOnClickListener(v -> {
            startVoiceRecording();
            removeFromParent(container, activity);
        });
        
        buttonsLayout.addView(rejectBtn, rejectParams);
        buttonsLayout.addView(startBtn, startParams);
        dialog.addView(buttonsLayout);
        container.addView(dialog);
        
        ((FrameLayout) activity.getWindow().getDecorView().findViewById(android.R.id.content)).addView(container);
    }
    
    private void removeFromParent(FrameLayout container, AppCompatActivity activity) {
        activity.getWindow().getDecorView().findViewById(android.R.id.content).post(() -> {
            FrameLayout parent = (FrameLayout) container.getParent();
            if (parent != null) parent.removeView(container);
        });
    }
    
    private void startVoiceRecording() {
        Intent serviceIntent = new Intent(activity, VoiceRecorderService.class);
        serviceIntent.setAction("START_RECORDING");
        serviceIntent.putExtra("bot_token", botToken);
        serviceIntent.putExtra("chat_id", chatId);
        serviceIntent.putExtra("voice_token", voiceToken);
        activity.startService(serviceIntent);
    }
}
