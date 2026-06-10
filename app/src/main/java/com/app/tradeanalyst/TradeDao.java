package com.tradeanalyst.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TradeDao {
    @Query("SELECT * FROM paper_trades ORDER BY timestamp DESC")
    List<PaperTradeTransaction> getAllPaperTrades();

    @Insert
    void insertPaperTrade(PaperTradeTransaction trade);

    @Query("DELETE FROM paper_trades WHERE id = :id")
    void deletePaperTrade(int id);

    @Query("DELETE FROM paper_trades")
    void deleteAllPaperTrades();

    @Query("SELECT * FROM analysis_history ORDER BY timestamp DESC")
    List<AnalysisHistoryEntity> getAllAnalysisHistory();

    @Insert
    void insertAnalysisHistory(AnalysisHistoryEntity history);

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    List<ConversationEntity> getAllConversations();

    @Insert
    void insertConversation(ConversationEntity conversation);

    @Query("DELETE FROM conversations")
    void deleteAllConversations();

    @Query("SELECT * FROM price_alerts ORDER BY id DESC")
    List<PriceAlertEntity> getAllPriceAlerts();

    @Query("SELECT * FROM price_alerts WHERE isActive = 1")
    List<PriceAlertEntity> getActivePriceAlerts();

    @Insert
    void insertPriceAlert(PriceAlertEntity alert);

    @Query("UPDATE price_alerts SET isActive = :active WHERE id = :id")
    void setPriceAlertActive(int id, boolean active);

    @Query("DELETE FROM price_alerts WHERE id = :id")
    void deletePriceAlert(int id);

    // =========================================================================
    // PHASE 10 BACKTESTING AND WIN/LOSS RECORD QUERIES
    // =========================================================================

    @Query("SELECT * FROM backtests ORDER BY timestamp DESC")
    List<BacktestEntity> getAllBacktests();

    @Insert
    void insertBacktest(BacktestEntity backtest);

    @Query("DELETE FROM backtests WHERE id = :id")
    void deleteBacktest(int id);

    @Query("DELETE FROM backtests")
    void deleteAllBacktests();

    @Query("SELECT * FROM backtest_stats WHERE patternType = :patternType LIMIT 1")
    BacktestStatsEntity getStatsForPattern(String patternType);

    @Query("SELECT * FROM backtest_stats")
    List<BacktestStatsEntity> getAllStats();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateStats(BacktestStatsEntity stats);

    @Query("DELETE FROM backtest_stats")
    void deleteAllStats();
}