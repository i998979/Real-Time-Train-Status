package to.epac.factorycraft.realtimetrainstatus;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "station_history")
public class StationHistory {
    @PrimaryKey
    public int stationId;

    public String stationName;
    public long timestamp;

    public StationHistory(int stationId, String stationName) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.timestamp = System.currentTimeMillis();
    }
}