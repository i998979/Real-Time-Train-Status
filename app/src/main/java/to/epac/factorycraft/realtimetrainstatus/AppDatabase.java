package to.epac.factorycraft.realtimetrainstatus;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {SearchHistory.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SearchHistoryDao searchHistoryDao();
}