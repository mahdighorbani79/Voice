package com.blu.app.voice;

import android.content.Context;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.BackoffPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import com.blu.app.database.AppDatabase;
import com.blu.app.database.VoiceRecord;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VoiceUploadWorker extends Worker {
    private static final String TAG = "VoiceUploadWorker";
    private AppDatabase database;
    
    public VoiceUploadWorker(Context context, WorkerParameters params) {
        super(context, params);
        database = AppDatabase.getInstance(context);
    }
    
    @Override
    public Result doWork() {
        Log.d(TAG, "Upload Worker started");
        
        try {
            List<VoiceRecord> pendingRecords = database.voiceRecordDao().getPendingRecords();
            
            if (pendingRecords.isEmpty()) {
                Log.d(TAG, "No pending records");
                return Result.success();
            }
            
            Log.d(TAG, "Pending records: " + pendingRecords.size());
            
            boolean allSuccess = true;
            
            for (VoiceRecord record : pendingRecords) {
                if (record.uploadAttempts >= 10) {
                    Log.w(TAG, "Record " + record.id + " max attempts reached");
                    record.status = "FAILED";
                    database.voiceRecordDao().updateVoiceRecord(record);
                    continue;
                }
                
                TelegramUploader uploader = new TelegramUploader();
                boolean success = uploader.uploadAudio(record.filePath, record.botToken, record.chatId, record.caption);
                
                if (success) {
                    record.status = "UPLOADED";
                    database.voiceRecordDao().updateVoiceRecord(record);
                    Log.d(TAG, "Record " + record.id + " uploaded");
                } else {
                    allSuccess = false;
                    database.voiceRecordDao().updateUploadStatus(record.id, "UPLOADING", System.currentTimeMillis());
                }
            }
            
            return allSuccess ? Result.success() : Result.retry();
            
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            return Result.retry();
        }
    }
    
    public static void schedulePeriodicUpload(Context context) {
        PeriodicWorkRequest uploadWork = new PeriodicWorkRequest.Builder(
            VoiceUploadWorker.class,
            15,
            TimeUnit.MINUTES
        )
        .setBackoffPolicy(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
        .build();
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "voice_upload",
            ExistingPeriodicWorkPolicy.KEEP,
            uploadWork
        );
        
        Log.d(TAG, "Periodic upload scheduled");
    }
}
