package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EastRailJRActivity extends AppCompatActivity {
    private RecyclerView rv;
    private JRLineAdapter adapter;

    private final List<Trip> activeTrips = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<Integer, Integer> stationIdToIndexMap = new HashMap<>();

    private String lineCode;
    private String dataSource;
    private LineConfig lineConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_east_rail_jr);

        ImageButton btnClose = findViewById(R.id.btn_close_activity);
        btnClose.setOnClickListener(v -> finish());

        lineCode = getIntent().getStringExtra("LINE_CODE");
        if (lineCode == null) lineCode = "eal";
        dataSource = getIntent().getStringExtra("DATA_SOURCE");
        if (dataSource == null) dataSource = "OPENDATA";
        lineConfig = LineConfig.get(this, lineCode);

        stationIdToIndexMap.clear();
        String[] stations = getStationArray();
        for (int i = 0; i < stations.length; i++) {
            int id = Utils.codeToId(this, lineCode, stations[i]);
            if (id != -1) stationIdToIndexMap.put(id, i);
        }

        rv = findViewById(R.id.rv_line);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JRLineAdapter(this, lineCode, lineConfig.stationIDs, activeTrips,
                lineConfig.runTimeUpMap, lineConfig.runTimeDnMap, lineConfig.dwellTimeUpMap, lineConfig.dwellTimeDnMap);
        rv.setAdapter(adapter);

        startRefreshLoop();
    }

    private void startRefreshLoop() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                fetchData();
                mainHandler.postDelayed(this, 10000);
            }
        });
    }

    private void fetchData() {
        new Thread(() -> {
            try {
                List<Trip> rawList = new ArrayList<>();

                if (dataSource.equalsIgnoreCase("OPENDATA")) {
                    String[] stations = getStationArray();
                    for (String sta : stations) {
                        String raw = download("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line="
                                + lineCode.toUpperCase() + "&sta=" + sta, null);
                        if (raw != null) {
                            JSONObject root = new JSONObject(raw);
                            JSONObject data = root.optJSONObject("data");
                            String key = lineCode.toUpperCase() + "-" + sta;
                            if (data != null && data.has(key)) {
                                rawList.addAll(parseNtJson(data.getJSONObject(key), sta));
                            }
                        }
                    }
                } else {
                    String raw = download(lineConfig.apiUrl, "QkmjCRYvXt6o89UdZAvoXa49543NxOtU2tBhQQDQ");
                    if (raw != null) {
                        rawList.addAll(parseRoctecJson(raw));
                    }
                }

                List<Trip> cleanList = processPhysicsBasedDedup(rawList);
                cleanList.sort(Comparator.comparingLong(t -> t.expectedArrivalTime));

                mainHandler.post(() -> {
                    activeTrips.clear();
                    activeTrips.addAll(cleanList);
                    Log.d("JR_LOG", "Valid Trains: " + activeTrips.size());
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<Trip> parseRoctecJson(String json) throws Exception {
        List<Trip> list = new ArrayList<>();
        JSONArray array;

        // TML format
        if (json.trim().startsWith("{")) {
            JSONObject root = new JSONObject(json);
            array = root.getJSONArray("Items");
        }
        // EAL format
        else {
            array = new JSONArray(json);
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            List<Car> cars = new ArrayList<>();
            if (obj.has("listCars")) {
                JSONArray carArray = obj.getJSONArray("listCars");
                for (int j = 0; j < carArray.length(); j++) {
                    JSONObject c = carArray.getJSONObject(j);
                    cars.add(new Car(c.optInt("carLoad"), c.optInt("passengerCount"),
                            c.optString("carName"), c.optInt("passengerLoad")));
                }
            }

            String trainId = obj.optString("trainId");

            String td = obj.optString("td", "UNKNOWN");
            if (td.equals("UNKNOWN")) td = trainId;

            int doorStatus = 0;
            Object doorObj = obj.opt("doorStatus");
            if (doorObj instanceof Boolean) {
                doorStatus = (Boolean) doorObj ? 1 : 0;
            } else if (doorObj instanceof String) {
                doorStatus = Integer.parseInt((String) doorObj);
            }

            // TODO: TML uses distanceFromCurrentStation instead of targetDistance
            int targetDist = obj.has("targetDistance") ?
                    obj.optInt("targetDistance") :
                    obj.optInt("distanceFromCurrentStation", 0);

            list.add(new Trip(trainId, "", obj.optDouble("trainSpeed", 0.0),
                    obj.optInt("currentStationCode"), obj.optInt("nextStationCode"), obj.optInt("destinationStationCode"),
                    cars, obj.optLong("receivedTime"), obj.optLong("ttl"),
                    doorStatus, td, targetDist, obj.optInt("startDistance", 0)
            ));
        }
        return list;
    }

    private List<Trip> parseNtJson(JSONObject stationJson, String staName) throws Exception {
        List<Trip> list = new ArrayList<>();

        String[] dirs = {"UP", "DOWN"};
        int currentStaCode = Utils.codeToId(this, lineCode, staName);

        for (String dir : dirs) {
            if (!stationJson.has(dir)) continue;
            JSONArray trains = stationJson.getJSONArray(dir);
            if (trains.length() == 0) continue;

            JSONObject tObj = trains.getJSONObject(0);
            long arrivalTime = Utils.convertTimestampToMillis(tObj.getString("time"));
            int destCode = Utils.codeToId(this, lineCode, tObj.getString("dest"));

            Trip t = new Trip(currentStaCode, destCode, arrivalTime, dir, 1,
                    tObj.optString("route", ""), tObj.optInt("ttnt", 0), tObj.optString("timeType", "A"));
            list.add(t);
        }
        return list;
    }

    // TODO
    private List<Trip> processPhysicsBasedDedup(List<Trip> rawList) {
        rawList.sort(Comparator.comparingLong(t -> t.expectedArrivalTime));
        List<Trip> acceptedTrips = new ArrayList<>();

        for (Trip candidate : rawList) {
            boolean isGhost = false;
            Integer candIdx = stationIdToIndexMap.get(candidate.currentStationCode);

            for (Trip existing : acceptedTrips) {
                // 方向相同 (UP/DOWN) 且 目的地相同
                if (candidate.td.equals(existing.td) && candidate.destinationStationCode == existing.destinationStationCode) {
                    Integer existIdx = stationIdToIndexMap.get(existing.currentStationCode);
                    if (candIdx == null || existIdx == null) continue;

                    boolean isUpDirection = candidate.td.contains("UP");

                    // 物理邏輯去重：
                    // 在 UP 方向 (Index 增加)，若 Candidate 在後面的車站 (Index 較大) 卻時間較晚 -> 它是同一班車
                    // 在 DOWN 方向 (Index 減少)，若 Candidate 在後面的車站 (Index 較小) 卻時間較晚 -> 它是同一班車
                    if (isUpDirection) {
                        if (candIdx > existIdx) {
                            isGhost = true;
                            break;
                        }
                    } else {
                        if (candIdx < existIdx) {
                            isGhost = true;
                            break;
                        }
                    }
                }
            }
            if (!isGhost) acceptedTrips.add(candidate);
        }
        return acceptedTrips;
    }

    private String[] getStationArray() {
        int resId = getResources().getIdentifier(lineCode.toLowerCase() + "_station_code", "string", getPackageName());
        return resId != 0 ? getString(resId).split("\\s+") : new String[0];
    }

    private String download(String urlStr, String apiKey) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            if (apiKey != null)
                conn.setRequestProperty("x-api-key", apiKey);
            conn.setConnectTimeout(5000);

            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }

                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }
}