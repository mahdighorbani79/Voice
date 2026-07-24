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
    public static final int MAX_DURATION = 10 * 60 * 1000;
    
    private Context context;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private File recordingFile;
    private long recordStartTime;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private RecordingListener listener;
    
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
        
        try {
            recordingFile = createAudioFile();
            Log.d(TAG, "📁 مسیر فایل: " + recordingFile.getAbsolutePath());
            
            // ✅ تست میکروفون با MediaRecorder
            mediaRecorder = new MediaRecorder();
            
            try {
                // تنظیمات ضبط
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                Log.d(TAG, "✅ AudioSource.MIC تنظیم شد");
            } catch (Exception e) {
                Log.e(TAG, "❌ خطا در setAudioSource: " + e.getMessage());
                // تلاش با AudioSource.VOICE_RECOGNITION
                try {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
                    Log.d(TAG, "✅ AudioSource.VOICE_RECOGNITION تنظیم شد");
                } catch (Exception e2) {
                    Log.e(TAG, "❌ خطا در setAudioSource (تلاش دوم): " + e2.getMessage());
                    // تلاش با AudioSource.DEFAULT
                    try {
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                        Log.d(TAG, "✅ AudioSource.DEFAULT تنظیم شد");
                    } catch (Exception e3) {
                        Log.e(TAG, "❌ خطا در setAudioSource (تلاش سوم): " + e3.getMessage());
                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onRecordingError("خطا در دسترسی به میکروفون: " + e3.getMessage());
                            }
                        });
                        return;
                    }
                }
            }
            
            // تنظیمات فرمت
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mediaRecorder.setMaxDuration(MAX_DURATION);
            
            Log.d(TAG, "✅ MediaRecorder تنظیم شد، شروع آماده‌سازی...");
            
            mediaRecorder.prepare();
            Log.d(TAG, "✅ MediaRecorder.prepare() موفق");
            
            mediaRecorder.start();
            Log.d(TAG, "✅ MediaRecorder.start() موفق - ضبط شروع شد!");
            
            isRecording = true;
            recordStartTime = System.currentTimeMillis();
            
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingStarted(recordingFile.getAbsolutePath(), recordStartTime);
                }
            });
            
            // تایمر خودکار
            new Thread(() -> {
                try {
                    Thread.sleep(MAX_DURATION);
                    if (isRecording) {
                        Log.d(TAG, "⏹️ توقف خودکار بعد از ۱۰ دقیقه");
                        stopRecording();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "تایمر قطع شد", e);
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ خطا در شروع ضبط: " + e.getMessage(), e);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingError("خطا در شروع ضبط: " + e.getMessage());
                }
            });
        }
    }
    
    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "ضبط فعال نیست!");
            return;
        }
        
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            
            isRecording = false;
            long duration = System.currentTimeMillis() - recordStartTime;
            
            Log.d(TAG, "⏹️ ضبط متوقف شد، مدت: " + duration/1000 + " ثانیه");
            
            mainHandler.post(() -> {
                if (listener != null) {
                    if (duration < 1000) {
                        listener.onRecordingError("مدت زمان ضبط کمتر از ۱ ثانیه است");
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
            Log.e(TAG, "❌ خطا در توقف ضبط: " + e.getMessage(), e);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingError("خطا در توقف ضبط: " + e.getMessage());
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
}
