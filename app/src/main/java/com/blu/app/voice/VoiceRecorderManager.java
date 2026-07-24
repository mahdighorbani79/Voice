package com.blu.app.voice;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceRecorderManager {
    private static final String TAG = "VoiceRecorder";
    public static final int MAX_DURATION = 10 * 60 * 1000; // ۱۰ دقیقه
    
    private Context context;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private File recordingFile;
    private long recordStartTime;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private RecordingListener listener;
    private boolean isStopped = false;
    
    public interface RecordingListener {
        void onRecordingStarted(String filePath, long startTime);
        void onRecordingCompleted(String filePath, long duration);
        void onRecordingError(String error);
        void onRecordingStopped(String filePath, long duration);
    }
    
    public VoiceRecorderManager(Context context) {
        this.context = context;
    }
    
    public void setRecordingListener(RecordingListener listener) {
        this.listener = listener;
    }
    
    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "در حال ضبط است!");
            return;
        }
        
        isStopped = false;
        
        try {
            recordingFile = createAudioFile();
            Log.d(TAG, "ضبط در: " + recordingFile.getAbsolutePath());
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mediaRecorder.setMaxDuration(MAX_DURATION);
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            recordStartTime = System.currentTimeMillis();
            
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingStarted(recordingFile.getAbsolutePath(), recordStartTime);
                }
            });
            
            // تایمر خودکار برای توقف بعد از ۱۰ دقیقه
            new Thread(() -> {
                try {
                    Thread.sleep(MAX_DURATION);
                    if (isRecording && !isStopped) {
                        Log.d(TAG, "توقف خودکار بعد از ۱۰ دقیقه");
                        stopRecording();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "تایمر قطع شد", e);
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "خطا در شروع ضبط", e);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingError(e.getMessage());
                }
            });
        }
    }
    
    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "ضبط فعال نیست!");
            return;
        }
        
        isStopped = true;
        
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            
            isRecording = false;
            long duration = System.currentTimeMillis() - recordStartTime;
            
            mainHandler.post(() -> {
                if (listener != null) {
                    if (duration < 1000) {
                        // کمتر از ۱ ثانیه ضبط شده، خطا در نظر بگیر
                        listener.onRecordingError("مدت زمان ضبط کمتر از ۱ ثانیه است");
                        // حذف فایل
                        if (recordingFile != null && recordingFile.exists()) {
                            recordingFile.delete();
                        }
                    } else {
                        listener.onRecordingStopped(recordingFile.getAbsolutePath(), duration);
                        listener.onRecordingCompleted(recordingFile.getAbsolutePath(), duration);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "خطا در توقف ضبط", e);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingError(e.getMessage());
                }
            });
        }
    }
    
    private File createAudioFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "voice_" + timestamp + ".m4a";
        File recordingsDir = new File(context.getExternalFilesDir(null), "voice_recordings");
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs();
        }
        return new File(recordingsDir, fileName);
    }
    
    public boolean isRecording() { return isRecording; }
    public File getRecordingFile() { return recordingFile; }
    public long getRecordStartTime() { return recordStartTime; }
}
