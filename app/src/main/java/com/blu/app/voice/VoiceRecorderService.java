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
    private VoiceUploader uploader;
    private int currentRecordId = -1;
    private String voiceToken;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 سرویس ایجاد شد");
        createNotificationChannel();
        database = AppDatabase.getInstance(this);
        recorderManager = new VoiceRecorderManager(this);
        uploader = new VoiceUploader();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(TAG, "❌ Intent null است");
            return START_STICKY;
        }
        
        String action = intent.getAction();
        Log.d(TAG, "📨 Action: " + action);
        
        if ("START_RECORDING".equals(action)) {
            voiceToken = intent.getStringExtra("voice_token");
            if (voiceToken == null) {
                voiceToken = "token_" + System.currentTimeMillis();
                Log.w(TAG, "⚠️ Voice Token null بود، مقدار جدید: " + voiceToken);
            }
            Log.d(TAG, "🆔 Voice Token: " + voiceToken);
            startRecording();
        } else if ("STOP_RECORDING".equals(action)) {
            stopRecording();
        }
        
        return START_STICKY;
    }
    
    private void startRecording() {
        Log.d(TAG, "🎤 شروع ضبط...");
        sendBroadcast(new Intent("VOICE_RECORDING_STARTED"));
        
        recorderManager.setRecordingListener(new VoiceRecorderManager.RecordingListener() {
            @Override
            public void onRecordingStarted(String filePath, long startTime) {
                Log.d(TAG, "✅ ضبط شروع شد: " + filePath);
                saveVoiceRecord(filePath, startTime);
                updateNotification("🎤 در حال ضبط...");
            }
            
            @Override
            public void onRecordingCompleted(String filePath, long duration) {
                Log.d(TAG, "⏹️ ضبط کامل شد، مدت: " + duration + "ms");
                updateNotification("📤 در حال ارسال به سرور...");
                sendBroadcast(new Intent("VOICE_RECORDING_COMPLETED"));
                
                // ارسال به Worker
                new Thread(() -> {
                    try {
                        VoiceRecord record = database.voiceRecordDao().getVoiceRecord(currentRecordId);
                        if (record != null) {
                            Log.d(TAG, "📝 رکورد پیدا شد: ID=" + currentRecordId);
                            record.status = "RECORDED";
                            record.endTime = System.currentTimeMillis();
                            record.duration = duration;
                            database.voiceRecordDao().updateVoiceRecord(record);
                            
                            Log.d(TAG, "📤 شروع آپلود به Worker...");
                            
                            // آپلود به Worker با کالبک
                            uploader.uploadVoice(
                                filePath,
                                voiceToken,
                                record.caption,
                                duration,
                                new VoiceUploader.UploadCallback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        Log.d(TAG, "✅ آپلود موفق! پاسخ: " + response);
                                        record.status = "UPLOADED";
                                        database.voiceRecordDao().updateVoiceRecord(record);
                                        updateNotification("✅ ارسال به سرور موفق!");
                                        sendBroadcast(new Intent("VOICE_UPLOAD_SUCCESS"));
                                    }
                                    
                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "❌ آپلود ناموفق: " + error);
                                        record.status = "FAILED";
                                        record.uploadAttempts = record.uploadAttempts + 1;
                                        database.voiceRecordDao().updateVoiceRecord(record);
                                        updateNotification("❌ ارسال ناموفق: " + error);
                                        sendBroadcast(new Intent("VOICE_UPLOAD_FAILED"));
                                    }
                                }
                            );
                        } else {
                            Log.e(TAG, "❌ رکورد با ID " + currentRecordId + " پیدا نشد");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "❌ خطا در آپلود", e);
                        updateNotification("❌ خطا: " + e.getMessage());
                    }
                }).start();
            }
            
            @Override
            public void onRecordingError(String error) {
                Log.e(TAG, "❌ خطا در ضبط: " + error);
                updateNotification("❌ خطا: " + error);
            }
        });
        
        recorderManager.startRecording();
        startForeground(NOTIFICATION_ID, createNotification("🎤 در حال ضبط مخفی..."));
    }
    
    private void stopRecording() {
        Log.d(TAG, "⏹️ توقف ضبط");
        recorderManager.stopRecording();
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
            record.caption = "🎙️ ضبط مخفی: " + date;
            long id = database.voiceRecordDao().insertVoiceRecord(record);
            currentRecordId = (int) id;
            Log.d(TAG, "💾 رکورد ذخیره شد با ID: " + id);
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
        Log.d(TAG, "💀 سرویس متوقف شد");
        if (recorderManager != null) {
            recorderManager.stopRecording();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
