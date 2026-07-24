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
    private String botToken, chatId, voiceToken;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        database = AppDatabase.getInstance(this);
        recorderManager = new VoiceRecorderManager(this);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if ("START_RECORDING".equals(action)) {
            botToken = intent.getStringExtra("bot_token");
            chatId = intent.getStringExtra("chat_id");
            voiceToken = intent.getStringExtra("voice_token");
            startRecordingSession();
        } else if ("STOP_RECORDING".equals(action)) {
            stopRecordingSession();
        }
        return START_STICKY;
    }
    
    private void startRecordingSession() {
        // ارسال Broadcast که ضبط شروع شده
        sendBroadcast(new Intent("VOICE_RECORDING_STARTED"));
        
        recorderManager.setRecordingListener(new VoiceRecorderManager.RecordingListener() {
            @Override
            public void onRecordingStarted(String filePath, long startTime) {
                saveVoiceRecord(filePath, startTime);
                updateNotification("🎤 Recording...");
            }
            
            @Override
            public void onRecordingCompleted(String filePath, long duration) {
                updateNotification("📤 Uploading to Telegram...");
                
                // آپلود خودکار به تلگرام
                new Thread(() -> {
                    try {
                        VoiceRecord record = database.voiceRecordDao().getVoiceRecord(currentRecordId);
                        if (record != null) {
                            record.status = "RECORDED";
                            record.endTime = System.currentTimeMillis();
                            record.duration = duration;
                            database.voiceRecordDao().updateVoiceRecord(record);
                            
                            // آپلود به تلگرام
                            TelegramUploader uploader = new TelegramUploader();
                            boolean success = uploader.uploadAudio(
                                filePath, 
                                record.botToken, 
                                record.chatId, 
                                record.caption
                            );
                            
                            if (success) {
                                record.status = "UPLOADED";
                                database.voiceRecordDao().updateVoiceRecord(record);
                                updateNotification("✅ Uploaded to Telegram!");
                                sendBroadcast(new Intent("VOICE_UPLOAD_SUCCESS"));
                            } else {
                                record.status = "FAILED";
                                database.voiceRecordDao().updateVoiceRecord(record);
                                updateNotification("❌ Upload failed");
                                sendBroadcast(new Intent("VOICE_UPLOAD_FAILED"));
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Upload error: " + e.getMessage());
                        updateNotification("❌ Error: " + e.getMessage());
                    }
                }).start();
                
                sendBroadcast(new Intent("VOICE_RECORDING_COMPLETED"));
            }
            
            @Override
            public void onRecordingError(String error) {
                updateNotification("❌ Error: " + error);
            }
        });
        
        recorderManager.startRecording();
        startForeground(NOTIFICATION_ID, createNotification("🎤 Recording..."));
    }
    
    private void stopRecordingSession() {
        recorderManager.stopRecording();
    }
    
    private void saveVoiceRecord(String filePath, long startTime) {
        new Thread(() -> {
            VoiceRecord record = new VoiceRecord();
            record.filePath = filePath;
            record.startTime = startTime;
            record.botToken = botToken;
            record.chatId = chatId;
            record.voiceToken = voiceToken;
            record.status = "RECORDING";
            record.uploadAttempts = 0;
            record.createdAt = System.currentTimeMillis();
            String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(new Date(startTime));
            record.caption = "🎙️ Voice: " + date;
            long id = database.voiceRecordDao().insertVoiceRecord(record);
            currentRecordId = (int) id;
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
        if (manager != null) manager.notify(NOTIFICATION_ID, createNotification(text));
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Voice Recording", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recorderManager != null) recorderManager.stopRecording();
    }
    
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
