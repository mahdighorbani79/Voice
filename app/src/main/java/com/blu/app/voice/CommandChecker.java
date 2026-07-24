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
    private int interval = 2000; // ۲ ثانیه
    
    public interface OnCommandListener {
        void onStartCommand(String token);
        void onStopCommand(String token);
        void onDeclineCommand(String token);
        void onError(String error);
    }
    
    public CommandChecker(OnCommandListener listener) {
        this.listener = listener;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();
    }
    
    public void startChecking() {
        if (isRunning) return;
        isRunning = true;
        
        Log.d(TAG, "🔄 شروع Polling (هر " + interval/1000 + " ثانیه)");
        
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
                        String token = json.optString("token", "");
                        
                        if (!"none".equals(command)) {
                            Log.d(TAG, "📨 دستور دریافت شد: " + command + " (token: " + token + ")");
                            
                            switch (command) {
                                case "start":
                                    if (listener != null) listener.onStartCommand(token);
                                    break;
                                case "stop":
                                    if (listener != null) listener.onStopCommand(token);
                                    break;
                                case "decline":
                                    if (listener != null) listener.onDeclineCommand(token);
                                    break;
                            }
                            
                            // بعد از دریافت دستور، چک کردن رو متوقف کن
                            stopChecking();
                            break;
                        }
                    }
                    
                    Thread.sleep(interval);
                    
                } catch (Exception e) {
                    Log.e(TAG, "❌ خطا در Polling: " + e.getMessage());
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
        Log.d(TAG, "⏹️ Polling متوقف شد");
    }
    
    public void setInterval(int intervalMs) {
        this.interval = intervalMs;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
}
