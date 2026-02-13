package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.room.Room;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HistoryManager {
    private static HistoryManager instance;

    private final AppDatabase db;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private HistoryManager(Context context) {
        db = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, "search_history_db")
                // Old version database migration
                .fallbackToDestructiveMigrationFrom(1)
                .fallbackToDestructiveMigration()
                .build();
    }

    public static synchronized HistoryManager getInstance(Context context) {
        if (instance == null)
            instance = new HistoryManager(context);

        return instance;
    }

    public void saveRouteSearch(String oId, String dId, String oName, String dName) {
        executor.execute(() -> {
            SearchHistoryDao dao = db.searchHistoryDao();
            dao.insertRoutes(new SearchHistory(oId, dId, oName, dName));
        });
    }

    public void loadSearchHistory(OnHistoryLoadedListener listener) {
        executor.execute(() -> {
            List<SearchHistory> history = db.searchHistoryDao().getRecentRoutes();
            handler.post(() -> {
                listener.onLoaded(history);
            });
        });
    }

    public void saveStationSearch(int stationId, String stationName) {
        executor.execute(() -> {
            SearchHistoryDao dao = db.searchHistoryDao();
            dao.insertStation(new StationHistory(stationId, stationName));

        });
    }

    public void loadStationHistory(OnStationHistoryLoadedListener listener) {
        executor.execute(() -> {
            List<StationHistory> history = db.searchHistoryDao().getRecentStations();
            handler.post(() -> {
                listener.onLoaded(history);
            });
        });
    }

    public interface OnHistoryLoadedListener {
        void onLoaded(List<SearchHistory> history);
    }

    public interface OnStationHistoryLoadedListener {
        void onLoaded(List<StationHistory> history);
    }
}