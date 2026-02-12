package to.epac.factorycraft.realtimetrainstatus;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SearchHistoryDao {
    @Insert
    void insert(SearchHistory history);

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    List<SearchHistory> getRecentHistories();

    @Query("DELETE FROM search_history WHERE id NOT IN (SELECT id FROM search_history ORDER BY timestamp DESC LIMIT 10)")
    void deleteOldHistories();

    @Query("DELETE FROM search_history WHERE id = :id")
    void deleteById(int id);

}