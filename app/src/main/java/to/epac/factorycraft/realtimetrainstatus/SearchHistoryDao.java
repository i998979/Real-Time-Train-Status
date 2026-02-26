package to.epac.factorycraft.realtimetrainstatus;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRoutes(SearchHistory history);

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    List<SearchHistory> getRecentRoutes();

    @Query("DELETE FROM search_history WHERE routeId = :routeId")
    void deleteRouteById(int routeId);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStation(StationHistory history);

    @Query("SELECT * FROM station_history ORDER BY timestamp DESC")
    List<StationHistory> getRecentStations();

    @Query("DELETE FROM station_history WHERE stationId = :stationId")
    void deleteStationById(int stationId);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLine(LineHistory history);

    @Query("SELECT * FROM line_history ORDER BY timestamp DESC")
    List<LineHistory> getRecentLines();

    @Query("DELETE FROM line_history WHERE lineId = :lineId")
    void deleteLineById(int lineId);
}