package com.tradeanalyst.app;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
    PaperTradeTransaction.class,
    AnalysisHistoryEntity.class,
    ConversationEntity.class,
    PriceAlertEntity.class,
    BacktestEntity.class,      // Registered for Phase 10 Backtesting system tracking
    BacktestStatsEntity.class  // Registered for Phase 10 historical statistical scoring
}, version = 2, exportSchema = false) // Incremented schema version to support backtesting structures
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TradeDao tradeDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "trade_analyst_db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}