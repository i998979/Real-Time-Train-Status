package to.epac.factorycraft.transitapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {SearchHistory.class, StationHistory.class, LineHistory.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SearchHistoryDao searchHistoryDao();
}