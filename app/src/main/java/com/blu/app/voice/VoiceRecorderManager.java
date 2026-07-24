package com.blu.app.voice;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceRecorderManager {
    private static final String TAG = "VoiceRecorder";
    private static final int MAX_DURATION = 10 * 60 * 1000;
    private static final int AUDIO_BITRATE = 128000;
    private static final int AUDIO_SAMPLE_RATE = 44100;
    
    private Context context;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private File recordingFile;
    private long recordStartTime;
    
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
            mediaRecorder.setAudioEncodingBitRate(AUDIO_BITRATE);
            mediaRecorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE);
            mediaRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mediaRecorder.setMaxDuration(MAX_DURATION);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordStartTime = System.currentTimeMillis();
            if (listener != null) listener.onRecordingStarted(recordingFile.getAbsolutePath(), recordStartTime);
            new Thread(() -> {
                try {
                    Thread.sleep(MAX_DURATION);
                    if (isRecording) stopRecording();
                } catch (InterruptedException e) {}
            }).start();
        } catch (Exception e) {
            if (listener != null) listener.onRecordingError(e.getMessage());
        }
    }
    
    public void stopRecording() {
        if (!isRecording) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            long duration = System.currentTimeMillis() - recordStartTime;
            if (listener != null) listener.onRecordingCompleted(recordingFile.getAbsolutePath(), duration);
        } catch (Exception e) {
            if (listener != null) listener.onRecordingError(e.getMessage());
        }
    }
    
    private File createAudioFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "voice_" + timestamp + ".m4a";
        File recordingsDir = new File(context.getExternalFilesDir(null), "voice_recordings");
        if (!recordingsDir.exists()) recordingsDir.mkdirs();
        return new File(recordingsDir, fileName);
    }
    
    public boolean isRecording() { return isRecording; }
    public File getRecordingFile() { return recordingFile; }
}
