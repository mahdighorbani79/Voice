package com.blu.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface VoiceRecordDao {
    @Insert
    long insertVoiceRecord(VoiceRecord record);
    @Update
    void updateVoiceRecord(VoiceRecord record);
    @Query("SELECT * FROM voice_records WHERE id = :id")
    VoiceRecord getVoiceRecord(int id);
    @Query("SELECT * FROM voice_records WHERE status = :status ORDER BY createdAt DESC")
    List<VoiceRecord> getRecordsByStatus(String status);
    @Query("SELECT * FROM voice_records WHERE status != 'UPLOADED' ORDER BY createdAt ASC")
    List<VoiceRecord> getPendingRecords();
    @Query("UPDATE voice_records SET status = :status, uploadAttempts = uploadAttempts + 1, lastUploadAttempt = :timestamp WHERE id = :id")
    void updateUploadStatus(int id, String status, long timestamp);
    @Query("DELETE FROM voice_records WHERE id = :id")
    void deleteVoiceRecord(int id);
    @Query("SELECT COUNT(*) FROM voice_records WHERE status = 'UPLOADING'")
    int getUploadingCount();
}
