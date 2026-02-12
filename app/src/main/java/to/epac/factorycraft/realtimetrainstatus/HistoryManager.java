package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.room.Room;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HistoryManager {
    private final AppDatabase db;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public HistoryManager(Context context) {
        db = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, "search_history_db").build();
    }

    public void saveSearch(String oId, String dId, String oName, String dName) {
        executor.execute(() -> {
            SearchHistoryDao dao = db.searchHistoryDao();
            // 1. 插入新記錄
            dao.insert(new SearchHistory(oId, dId, oName, dName));
            // 2. 清理舊記錄
            dao.deleteOldHistories();
        });
    }

    public void loadHistory(OnHistoryLoadedListener listener) {
        executor.execute(() -> {
            List<SearchHistory> history = db.searchHistoryDao().getRecentHistories();
            // 將結果切換回主執行緒以更新 UI
            new Handler(Looper.getMainLooper()).post(() -> listener.onLoaded(history));
        });
    }

    public interface OnHistoryLoadedListener {
        void onLoaded(List<SearchHistory> history);
    }
}