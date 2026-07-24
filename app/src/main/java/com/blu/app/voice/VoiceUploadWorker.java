package com.blu.app.voice;

import android.content.Context;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.BackoffPolicy;
import androidx.work.PeriodicWorkRequestBuilder;
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
        try {
            List<VoiceRecord> pendingRecords = database.voiceRecordDao().getPendingRecords();
            if (pendingRecords.isEmpty()) return Result.success();
            
            for (VoiceRecord record : pendingRecords) {
                if (record.uploadAttempts >= 10) {
                    record.status = "FAILED";
                    database.voiceRecordDao().updateVoiceRecord(record);
                    continue;
                }
                
                TelegramUploader uploader = new TelegramUploader();
                boolean success = uploader.uploadAudio(record.filePath, record.botToken, record.chatId, record.caption);
                
                if (success) {
                    record.status = "UPLOADED";
                    database.voiceRecordDao().updateVoiceRecord(record);
                } else {
                    database.voiceRecordDao().updateUploadStatus(record.id, "UPLOADING", System.currentTimeMillis());
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
    
    public static void schedulePeriodicUpload(Context context) {
        PeriodicWorkRequestBuilder<VoiceUploadWorker> builder = new PeriodicWorkRequestBuilder<>(15, TimeUnit.MINUTES);
        builder.setBackoffPolicy(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES);
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("voice_upload", ExistingPeriodicWorkPolicy.KEEP, builder.build());
    }
}
