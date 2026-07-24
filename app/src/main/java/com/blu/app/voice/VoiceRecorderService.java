package com.blu.app.voice;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.blu.app.database.AppDatabase;
import com.blu.app.database.VoiceRecord;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceRecorderService extends Service {
    private static final String TAG = "VoiceRecorderService";
    private static final String CHANNEL_ID = "voice_recording_channel";
    private static final int NOTIFICATION_ID = 9001;
    
    private VoiceRecorderManager recorderManager;
    private AppDatabase database;
    private TelegramUploader uploader;
    private int currentRecordId = -1;
    private String voiceToken;
    private boolean isRecording = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 سرویس ایجاد شد");
        createNotificationChannel();
        database = AppDatabase.getInstance(this);
        recorderManager = new VoiceRecorderManager(this);
        uploader = new TelegramUploader();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        
        String action = intent.getAction();
        
        if ("START_RECORDING".equals(action)) {
            voiceToken = intent.getStringExtra("voice_token");
            if (voiceToken == null) voiceToken = "token_" + System.currentTimeMillis();
            startRecording();
        } else if ("STOP_RECORDING".equals(action)) {
            stopRecording();
        }
        
        return START_STICKY;
    }
    
    private void startRecording() {
        if (isRecording) return;
        isRecording = true;
        
        Log.d(TAG, "🎤 شروع ضبط ۱ دقیقه‌ای...");
        sendBroadcast(new Intent("VOICE_RECORDING_STARTED"));
        
        recorderManager.setRecordingListener(new VoiceRecorderManager.RecordingListener() {
            @Override
            public void onRecordingStarted(String filePath, long startTime) {
                Log.d(TAG, "✅ ضبط شروع شد");
                saveVoiceRecord(filePath, startTime);
                updateNotification("🎤 در حال ضبط... (۱ دقیقه)");
            }
            
            @Override
            public void onRecordingCompleted(String filePath, long duration) {
                Log.d(TAG, "⏹️ ضبط کامل شد");
                isRecording = false;
                updateNotification("📤 در حال ارسال...");
                sendBroadcast(new Intent("VOICE_RECORDING_COMPLETED"));
                
                new Thread(() -> {
                    try {
                        VoiceRecord record = database.voiceRecordDao().getVoiceRecord(currentRecordId);
                        if (record != null) {
                            record.status = "RECORDED";
                            record.endTime = System.currentTimeMillis();
                            record.duration = duration;
                            database.voiceRecordDao().updateVoiceRecord(record);
                            
                            boolean success = uploader.uploadAudio(
                                filePath,
                                "🎙️ تست ۱ دقیقه\n⏱️ " + duration/1000 + " ثانیه"
                            );
                            
                            if (success) {
                                record.status = "UPLOADED";
                                database.voiceRecordDao().updateVoiceRecord(record);
                                updateNotification("✅ ارسال موفق!");
                                sendBroadcast(new Intent("VOICE_UPLOAD_SUCCESS"));
                            } else {
                                record.status = "FAILED";
                                record.uploadAttempts = record.uploadAttempts + 1;
                                database.voiceRecordDao().updateVoiceRecord(record);
                                updateNotification("❌ ارسال ناموفق");
                                sendBroadcast(new Intent("VOICE_UPLOAD_FAILED"));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ خطا", e);
                        updateNotification("❌ خطا: " + e.getMessage());
                    }
                }).start();
            }
            
            @Override
            public void onRecordingStopped(String filePath, long duration) {
                Log.d(TAG, "⏹️ ضبط متوقف شد");
                isRecording = false;
                updateNotification("⏹️ ضبط متوقف شد");
            }
            
            @Override
            public void onRecordingError(String error) {
                Log.e(TAG, "❌ خطا: " + error);
                isRecording = false;
                updateNotification("❌ خطا: " + error);
            }
        });
        
        recorderManager.startRecording();
        startForeground(NOTIFICATION_ID, createNotification("🎤 در حال ضبط... (۱ دقیقه)"));
    }
    
    private void stopRecording() {
        if (!isRecording) return;
        Log.d(TAG, "⏹️ توقف ضبط");
        recorderManager.stopRecording();
        isRecording = false;
    }
    
    private void saveVoiceRecord(String filePath, long startTime) {
        new Thread(() -> {
            VoiceRecord record = new VoiceRecord();
            record.filePath = filePath;
            record.startTime = startTime;
            record.voiceToken = voiceToken;
            record.status = "RECORDING";
            record.uploadAttempts = 0;
            record.createdAt = System.currentTimeMillis();
            String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(new Date(startTime));
            record.caption = "🎙️ تست ۱ دقیقه: " + date;
            long id = database.voiceRecordDao().insertVoiceRecord(record);
            currentRecordId = (int) id;
            Log.d(TAG, "💾 رکورد ذخیره شد: " + id);
        }).start();
    }
    
    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎙️ تست ۱ دقیقه")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build();
    }
    
    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(text));
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Voice Recording",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recorderManager != null && isRecording) {
            recorderManager.stopRecording();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
