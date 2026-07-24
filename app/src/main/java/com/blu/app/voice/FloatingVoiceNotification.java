package com.blu.app.voice;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class FloatingVoiceNotification {
    private AppCompatActivity activity;
    private String botToken, chatId, voiceToken;
    private FrameLayout container;
    
    public FloatingVoiceNotification(AppCompatActivity activity, String botToken, String chatId, String voiceToken) {
        this.activity = activity;
        this.botToken = botToken;
        this.chatId = chatId;
        this.voiceToken = voiceToken;
    }
    
    public void show() {
        container = new FrameLayout(activity);
        container.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        container.setBackgroundColor(Color.argb(180, 0, 0, 0));
        container.setClickable(true);
        
        // دیالوگ شیشه‌ای با افکت
        LinearLayout dialog = new LinearLayout(activity);
        dialog.setOrientation(LinearLayout.VERTICAL);
        dialog.setBackgroundColor(Color.argb(230, 255, 255, 255));
        dialog.setPadding(50, 40, 50, 40);
        dialog.setElevation(30);
        
        // گوشه‌های گرد
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(30);
        shape.setColor(Color.argb(240, 255, 255, 255));
        dialog.setBackground(shape);
        
        FrameLayout.LayoutParams dialogParams = new FrameLayout.LayoutParams(
            (int)(activity.getResources().getDisplayMetrics().widthPixels * 0.85),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialogParams.gravity = Gravity.CENTER;
        dialog.setLayoutParams(dialogParams);
        
        // آیکون میکروفون
        TextView icon = new TextView(activity);
        icon.setText("🎙️");
        icon.setTextSize(50);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.bottomMargin = 10;
        dialog.addView(icon, iconParams);
        
        // عنوان
        TextView title = new TextView(activity);
        title.setText("Record Voice");
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.bottomMargin = 5;
        dialog.addView(title, titleParams);
        
        // توضیحات
        TextView desc = new TextView(activity);
        desc.setText("Start recording voice and send to Telegram");
        desc.setTextSize(14);
        desc.setTextColor(Color.GRAY);
        desc.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.bottomMargin = 25;
        dialog.addView(desc, descParams);
        
        // دکمه‌ها
        LinearLayout buttonsLayout = new LinearLayout(activity);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.CENTER);
        buttonsLayout.setWeightSum(2);
        
        // دکمه Cancel
        Button cancelBtn = new Button(activity);
        cancelBtn.setText("Cancel");
        cancelBtn.setTextColor(Color.WHITE);
        cancelBtn.setBackgroundColor(Color.parseColor("#FF4444"));
        cancelBtn.setPadding(20, 15, 20, 15);
        cancelBtn.setElevation(10);
        cancelBtn.setAllCaps(false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
            0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            0.7f
        );
        cancelParams.setMargins(0, 0, 15, 0);
        cancelBtn.setLayoutParams(cancelParams);
        cancelBtn.setOnClickListener(v -> removeFromParent());
        
        // دکمه Start با رنگ سبز شیشه‌ای
        Button startBtn = new Button(activity);
        startBtn.setText("▶ Start");
        startBtn.setTextColor(Color.WHITE);
        startBtn.setBackgroundColor(Color.parseColor("#34C759"));
        startBtn.setPadding(20, 15, 20, 15);
        startBtn.setElevation(10);
        startBtn.setAllCaps(false);
        
        // افکت شیشه‌ای روی دکمه Start
        GradientDrawable startShape = new GradientDrawable();
        startShape.setCornerRadius(25);
        startShape.setColor(Color.parseColor("#34C759"));
        startBtn.setBackground(startShape);
        
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
            0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            1.3f
        );
        startBtn.setLayoutParams(startParams);
        startBtn.setOnClickListener(v -> {
            startVoiceRecording();
            removeFromParent();
        });
        
        buttonsLayout.addView(cancelBtn);
        buttonsLayout.addView(startBtn);
        dialog.addView(buttonsLayout);
        container.addView(dialog);
        
        // اضافه کردن به صفحه
        View decorView = activity.getWindow().getDecorView();
        if (decorView instanceof FrameLayout) {
            ((FrameLayout) decorView).addView(container);
        }
    }
    
    private void removeFromParent() {
        if (container != null && container.getParent() != null) {
            ((ViewGroup) container.getParent()).removeView(container);
        }
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
