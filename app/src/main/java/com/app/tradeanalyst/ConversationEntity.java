package com.tradeanalyst.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String sender; // "User" or "AI"
    public String message;
    public long timestamp;

    public ConversationEntity() {}

    public ConversationEntity(String sender, String message, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }
}
