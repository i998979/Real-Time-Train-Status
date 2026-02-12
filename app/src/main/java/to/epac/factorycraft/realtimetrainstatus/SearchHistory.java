package to.epac.factorycraft.realtimetrainstatus;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_history")
public class SearchHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String originId;
    public String destinationId;
    public String originName;
    public String destinationName;
    public long timestamp;

    public SearchHistory(String originId, String destinationId, String originName, String destinationName) {
        this.originId = originId;
        this.destinationId = destinationId;
        this.originName = originName;
        this.destinationName = destinationName;
        this.timestamp = System.currentTimeMillis();
    }
}