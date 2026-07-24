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
    public static final int MAX_DURATION = 60 * 1000; // ✅ ۱ دقیقه برای تست
    
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
        if (isRecording) return;
        
        try {
            recordingFile = createAudioFile();
            Log.d(TAG, "📁 مسیر: " + recordingFile.getAbsolutePath());
            
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(32000);
            mediaRecorder.setAudioSamplingRate(16000);
            mediaRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mediaRecorder.setMaxDuration(MAX_DURATION);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            recordStartTime = System.currentTimeMillis();
            
            Log.d(TAG, "✅ ضبط ۱ دقیقه‌ای شروع شد");
            
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingStarted(recordingFile.getAbsolutePath(), recordStartTime);
                }
            });
            
            // تایمر خودکار برای توقف بعد از ۱ دقیقه
            new Thread(() -> {
                try {
                    Thread.sleep(MAX_DURATION + 1000);
                    if (isRecording) {
                        Log.d(TAG, "⏹️ توقف خودکار بعد از ۱ دقیقه");
                        stopRecording();
                    }
                } catch (InterruptedException e) {}
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ خطا", e);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingError(e.getMessage());
                }
            });
        }
    }
    
    public void stopRecording() {
        if (!isRecording) return;
        
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            
            isRecording = false;
            long duration = System.currentTimeMillis() - recordStartTime;
            long fileSize = recordingFile != null ? recordingFile.length() : 0;
            
            Log.d(TAG, "⏹️ ضبط متوقف شد");
            Log.d(TAG, "   - مدت: " + duration/1000 + " ثانیه");
            Log.d(TAG, "   - حجم: " + fileSize/1024 + " KB");
            
            mainHandler.post(() -> {
                if (listener != null) {
                    if (duration < 1000) {
                        listener.onRecordingError("مدت کمتر از ۱ ثانیه");
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
            Log.e(TAG, "❌ خطا", e);
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
}
