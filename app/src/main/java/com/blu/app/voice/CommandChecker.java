package com.blu.app.voice;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class CommandChecker {
    private static final String TAG = "CommandChecker";
    private static final String WORKER_URL = "https://voice-bot-worker.kapcher2019.workers.dev/get-command";
    
    private OkHttpClient httpClient;
    private OnCommandListener listener;
    private boolean isRunning = false;
    private Thread checkThread;
    
    public interface OnCommandListener {
        void onStartCommand();
        void onDeclineCommand();
        void onError(String error);
    }
    
    public CommandChecker(OnCommandListener listener) {
        this.listener = listener;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    }
    
    public void startChecking() {
        if (isRunning) return;
        isRunning = true;
        
        Log.d(TAG, "🔄 شروع چک کردن دستورات...");
        
        checkThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Request request = new Request.Builder()
                        .url(WORKER_URL)
                        .get()
                        .build();
                    
                    Response response = httpClient.newCall(request).execute();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    response.close();
                    
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        String command = json.optString("command", "none");
                        
                        Log.d(TAG, "📨 دستور دریافت شد: " + command);
                        
                        if ("start".equals(command)) {
                            Log.d(TAG, "🎤 دستور Start دریافت شد!");
                            if (listener != null) {
                                listener.onStartCommand();
                            }
                            // بعد از دریافت Start، چک کردن رو متوقف کن
                            stopChecking();
                            break;
                        } else if ("decline".equals(command)) {
                            Log.d(TAG, "❌ دستور Decline دریافت شد!");
                            if (listener != null) {
                                listener.onDeclineCommand();
                            }
                            stopChecking();
                            break;
                        }
                    }
                    
                    // هر ۳ ثانیه چک کن
                    Thread.sleep(3000);
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ خطا در چک کردن: " + e.getMessage());
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {}
                }
            }
        });
        
        checkThread.start();
    }
    
    public void stopChecking() {
        isRunning = false;
        if (checkThread != null) {
            checkThread.interrupt();
        }
        Log.d(TAG, "⏹️ چک کردن متوقف شد");
    }
}
