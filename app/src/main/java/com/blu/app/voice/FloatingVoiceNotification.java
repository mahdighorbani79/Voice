package com.blu.app.voice;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class FloatingVoiceNotification {
    private AppCompatActivity activity;
    private String voiceToken;
    private FrameLayout container;
    
    public FloatingVoiceNotification(AppCompatActivity activity, String voiceToken) {
        this.activity = activity;
        this.voiceToken = voiceToken;
    }
    
    public void show() {
        container = new FrameLayout(activity);
        container.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        container.setBackgroundColor(Color.argb(150, 0, 0, 0));
        container.setClickable(true);
        container.setFocusable(true);
        
        LinearLayout dialog = new LinearLayout(activity);
        dialog.setOrientation(LinearLayout.VERTICAL);
        dialog.setPadding(50, 40, 50, 40);
        dialog.setElevation(40);
        
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(40);
        shape.setColor(Color.argb(240, 255, 255, 255));
        shape.setStroke(2, Color.parseColor("#80FFFFFF"));
        dialog.setBackground(shape);
        
        FrameLayout.LayoutParams dialogParams = new FrameLayout.LayoutParams(
            (int)(activity.getResources().getDisplayMetrics().widthPixels * 0.85),
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialogParams.gravity = Gravity.CENTER;
        dialog.setLayoutParams(dialogParams);
        
        // آیکون
        TextView icon = new TextView(activity);
        icon.setText("🎙️");
        icon.setTextSize(55);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.bottomMargin = 5;
        dialog.addView(icon, iconParams);
        
        // عنوان
        TextView title = new TextView(activity);
        title.setText("ضبط صدا");
        title.setTextSize(24);
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
        desc.setText("آیا می‌خواهید ضبط صدا شروع شود؟");
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
        
        // دکمه Decline
        Button declineBtn = new Button(activity);
        declineBtn.setText("❌ رد کردن");
        declineBtn.setTextColor(Color.WHITE);
        declineBtn.setBackgroundColor(Color.parseColor("#FF3B30"));
        declineBtn.setPadding(20, 18, 20, 18);
        declineBtn.setElevation(10);
        declineBtn.setAllCaps(false);
        declineBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        
        GradientDrawable declineShape = new GradientDrawable();
        declineShape.setCornerRadius(30);
        declineShape.setColor(Color.parseColor("#FF3B30"));
        declineBtn.setBackground(declineShape);
        
        LinearLayout.LayoutParams declineParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            0.6f
        );
        declineParams.setMargins(0, 0, 15, 0);
        declineBtn.setLayoutParams(declineParams);
        declineBtn.setOnClickListener(v -> removeFromParent());
        
        // دکمه Start
        Button startBtn = new Button(activity);
        startBtn.setText("▶ شروع ضبط");
        startBtn.setTextColor(Color.WHITE);
        startBtn.setPadding(20, 18, 20, 18);
        startBtn.setElevation(10);
        startBtn.setAllCaps(false);
        startBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        
        GradientDrawable startShape = new GradientDrawable();
        startShape.setCornerRadius(30);
        startShape.setColor(Color.parseColor("#34C759"));
        startBtn.setBackground(startShape);
        
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1.4f
        );
        startBtn.setLayoutParams(startParams);
        startBtn.setOnClickListener(v -> {
            startVoiceRecording();
            removeFromParent();
        });
        
        buttonsLayout.addView(declineBtn);
        buttonsLayout.addView(startBtn);
        dialog.addView(buttonsLayout);
        container.addView(dialog);
        
        // انیمیشن
        Animation scaleAnim = new ScaleAnimation(
            0.8f, 1f, 0.8f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(300);
        dialog.startAnimation(scaleAnim);
        
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
        serviceIntent.putExtra("voice_token", voiceToken);
        activity.startService(serviceIntent);
    }
}
