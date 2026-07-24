package com.blu.app.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "voice_records")
public class VoiceRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String filePath;
    public long startTime;
    public long endTime;
    public long duration;
    public String status;
    public int uploadAttempts;
    public long lastUploadAttempt;
    public String caption;
    public String voiceToken;
    public String serverResponse;
    public long createdAt;
    public VoiceRecord() {}
}
