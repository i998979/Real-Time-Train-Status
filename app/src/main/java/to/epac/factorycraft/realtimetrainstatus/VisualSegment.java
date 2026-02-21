package to.epac.factorycraft.realtimetrainstatus;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VisualSegment {
    String lineName;
    String lineColor;
    int duration;
    int startH, startM;
    int endH, endM;
    JSONObject startNode;
    JSONObject endNode;
    List<JSONObject> intermediates = new ArrayList<>();
    int lineID;
    boolean isWalk = false;
    String stationName;
}