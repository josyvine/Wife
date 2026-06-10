package com.tradeanalyst.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "analysis_history")
public class AnalysisHistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String symbol;
    public String analysisText;
    public long timestamp;

    public AnalysisHistoryEntity() {}

    public AnalysisHistoryEntity(String symbol, String analysisText, long timestamp) {
        this.symbol = symbol;
        this.analysisText = analysisText;
        this.timestamp = timestamp;
    }
}
