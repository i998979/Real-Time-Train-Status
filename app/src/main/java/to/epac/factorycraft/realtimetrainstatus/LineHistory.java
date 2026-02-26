package to.epac.factorycraft.realtimetrainstatus;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "line_history")
public class LineHistory {
    @PrimaryKey
    public int lineId;

    public String lineName;
    public long timestamp;

    public LineHistory(int lineId, String lineName) {
        this.lineId = lineId;
        this.lineName = lineName;
        this.timestamp = System.currentTimeMillis();
    }
}