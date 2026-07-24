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
    private static final int MAX_DURATION = 10 * 60 * 1000;
    
    private Context context;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private File recordingFile;
    private long recordStartTime;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface RecordingListener {
        void onRecordingStarted(String filePath, long startTime);
        void onRecordingCompleted(String filePath, long duration);
        void onRecordingError(String error);
    }
    
    private RecordingListener listener;
    
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
            
            // تایمر اتوماتیک برای توقف
            new Thread(() -> {
                try {
                    Thread.sleep(MAX_DURATION);
                    if (isRecording) stopRecording();
                } catch (InterruptedException e) {}
            }).start();
            
        } catch (Exception e) {
            mainHandler.post(() -> {
                if (listener != null) listener.onRecordingError(e.getMessage());
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
            
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingCompleted(recordingFile.getAbsolutePath(), duration);
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> {
                if (listener != null) listener.onRecordingError(e.getMessage());
            });
        }
    }
    
    private File createAudioFile() {
        St     
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onRecordingCompleted(recordingFile.getAbsolutePath(), duration);
                }
            });
        } catch (Exception e) {
            mainHandler.post(() -> {
                if (listener != null) listener.onRecosDir, fileName);
    }
    
    public boolean isRecording() { return isRecording; }
    public File getRecordingFile() { return recordingFile; }
}
