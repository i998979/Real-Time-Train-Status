package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class TrainLocationActivity extends AppCompatActivity {
    public static Context context;


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
        setContentView(R.layout.activity_train_location);

        TrainLocationActivity.context = this;

        ImageButton btnClose = findViewById(R.id.btn_close);
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

        ViewGroup lineBanner = findViewById(R.id.line_banner);
        TextView bannerName = lineBanner.findViewById(R.id.tv_banner_name);
        FrameLayout codeBadge = lineBanner.findViewById(R.id.line_color_badge);
        TextView tvCode = lineBanner.findViewById(R.id.tv_line_code_badge);

        bannerName.setText(Utils.getLineName(lineCode, true));
        int colorResId = context.getResources().getIdentifier(this.lineCode.toLowerCase(), "color", context.getPackageName());
        int color = context.getResources().getColor(colorResId, null);
        codeBadge.setBackgroundColor(color);
        tvCode.setText(lineCode.toUpperCase());

        rv = findViewById(R.id.rv_line);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JRLineAdapter(this, lineCode, lineConfig.stationIDs, activeTrips,
                lineConfig.runTimeUpMap, lineConfig.runTimeDnMap, lineConfig.dwellTimeUpMap, lineConfig.dwellTimeDnMap);
        rv.setAdapter(adapter);

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int startTime = 1 * 60 + 30; // 01:30
        int endTime = 5 * 60;        // 05:00

        boolean isMaintenanceTime = currentTimeInMinutes >= startTime && currentTimeInMinutes < endTime;

        LinearLayout nthMessage = findViewById(R.id.nth_message);
        if (isMaintenanceTime)
            nthMessage.setVisibility(View.VISIBLE);
        else
            nthMessage.setVisibility(View.INVISIBLE);


        NestedScrollView scrollView = findViewById(R.id.nested_scroll_view);
        scrollView.post(() -> {
            scrollView.scrollTo(0, rv.getTop());
            rv.requestFocus();
        });

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
                cleanList.sort(Comparator.comparingLong(t -> t.time));

                mainHandler.post(() -> {
                    activeTrips.clear();
                    activeTrips.addAll(cleanList);
                    Log.d("JR_LOG", "Valid Trains: " + activeTrips.size());
                    for (Trip trip : activeTrips) {
                        Log.d("JR_LOG", (trip.isUp ? "UP" : "DN") + " from "
                                + Utils.idToCode(this, trip.currentStationCode, lineCode) + " to "
                                + Utils.idToCode(this, trip.nextStationCode, lineCode) + " towards "
                                + Utils.idToCode(this, trip.destinationStationCode, lineCode) + " "
                                + trip.ttnt + " ");
                    }
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

    private List<Trip> parseNtJson(JSONObject stationJson, String station) throws Exception {
        int stationCode = Utils.codeToId(this, lineCode, station);

        int targetIdx = -1;
        for (int i = 0; i < lineConfig.stationIDs.length; i++) {
            if (lineConfig.stationIDs[i] == stationCode) {
                targetIdx = i;
                break;
            }
        }

        List<Trip> list = new ArrayList<>();
        String[] dirs = {"UP", "DOWN"};
        for (String dir : dirs) {
            if (!stationJson.has(dir)) continue;

            JSONArray trains = stationJson.getJSONArray(dir);
            boolean isUp = dir.equalsIgnoreCase("UP");

            // 2. 預計算物理上的「前一站」：UP (北行) 為 index + 1，DOWN (南行) 為 index - 1
            // 13 12 11 10 9 8 6 5 4 3 2 21 22 23
            // LOW SHS FAN TWO TAP UNI FOT SHT TAW KOT MKK HUH EXC ADM
            int prevIdx = isUp ? targetIdx + 1 : targetIdx - 1;
            int prevStaCode = (targetIdx != -1 && prevIdx >= 0 && prevIdx < lineConfig.stationIDs.length)
                    ? lineConfig.stationIDs[prevIdx] : stationCode;

            for (int i = 0; i < trains.length(); i++) {
                JSONObject tObj = trains.getJSONObject(i);

                int destCode = Utils.codeToId(this, lineCode, tObj.getString("dest"));
                long time = Utils.convertTimestampToMillis(tObj.getString("time"));
                int ttnt = tObj.optInt("ttnt", 0);
                String route = tObj.optString("route", "");
                String timeType = tObj.optString("timeType", "A");

                Trip trip = new Trip(stationCode, stationCode, destCode, dir, i + 1, time, ttnt, route, timeType);
                // Trip trip = new Trip(prevStaCode, stationCode, destCode, dir, i + 1, time, ttnt, route, timeType);

                // If ttnt <= 0, means the train has already arrived at the station
                // if (ttnt <= 0) trip.currentStationCode = stationCode;

                trip.stationPredictions.put(stationCode, ttnt);
                list.add(trip);
            }
        }
        Log.d("parseNtJson", station + ": " + list.size());
        return list;
    }

    private List<Trip> processPhysicsBasedDedup(List<Trip> rawList) {
        List<String> stationOrder = Arrays.asList(getStationIdArray());

        List<Trip> upList = rawList.stream().filter(trip -> trip.isUp).collect(Collectors.toList());
        List<Trip> upSaved = new ArrayList<>();
        List<Trip> dnList = rawList.stream().filter(trip -> !trip.isUp).collect(Collectors.toList());
        List<Trip> dnSaved = new ArrayList<>();

        upList.sort(Comparator
                .comparing((Trip t) -> stationOrder.indexOf(t.currentStationCode + ""))
                .thenComparing((Trip t) -> t.seq)
        );
        dnList.sort(Comparator
                .comparing((Trip t) -> -stationOrder.indexOf(t.currentStationCode + ""))
                .thenComparing((Trip t) -> t.seq)
        );

        for (Trip trip : upList) {
            Log.d("upList", (trip.isUp ? "UP" : "DN") + " "
                    + Utils.idToCode(this, trip.currentStationCode, lineCode) + " to "
                    + Utils.idToCode(this, trip.nextStationCode, lineCode) + " towards "
                    + Utils.idToCode(this, trip.destinationStationCode, lineCode) + " "
                    + trip.ttnt);
        }
        for (Trip trip : dnList) {
            Log.d("dnList", (trip.isUp ? "UP" : "DN") + " "
                    + Utils.idToCode(this, trip.currentStationCode, lineCode) + " to "
                    + Utils.idToCode(this, trip.nextStationCode, lineCode) + " towards "
                    + Utils.idToCode(this, trip.destinationStationCode, lineCode) + " "
                    + trip.ttnt);
        }


        for (int i = 0; i < upList.size(); i++) {
            Trip current = upList.get(i);
            int currentIdx = stationIdToIndexMap.get(current.currentStationCode);

            Trip pending = current;
            int pendingIdx = currentIdx;
            for (int k = i + 1; k < upList.size(); k++) {
                Trip below = upList.get(k);
                int belowIdx = stationIdToIndexMap.get(below.currentStationCode);


                // -- If all conditions met, we use Pending to trace the next shadow -- //
                if (pending.currentStationCode != below.currentStationCode) {
                    if (pending.destinationStationCode == below.destinationStationCode) {
                        if (pending.route.equals(below.route)) {
                            if (Math.abs(pendingIdx - belowIdx) <= 1) {
                                // TODO: Consider travelling time
                                if (pending.ttnt > 0 && below.ttnt > 0 && pending.ttnt > below.ttnt) {
                                    pending = below;
                                    pendingIdx = stationIdToIndexMap.get(pending.currentStationCode);
                                }
                            }
                        }
                    }
                }
            }

            if (!upSaved.contains(pending))
                upSaved.add(pending);
        }

        for (int i = 0; i < dnList.size(); i++) {
            Trip current = dnList.get(i);
            Integer currentIdx = stationIdToIndexMap.get(current.currentStationCode);
            if (currentIdx == null) continue;

            Trip pending = current;
            int pendingIdx = currentIdx;
            for (int k = i + 1; k < dnList.size(); k++) {
                Trip below = dnList.get(k);
                Integer belowIdx = stationIdToIndexMap.get(below.currentStationCode);
                if (belowIdx == null) continue;

                // -- If all conditions met, we use Pending to trace the next shadow -- //
                if (pending.currentStationCode != below.currentStationCode) {
                    if (pending.destinationStationCode == below.destinationStationCode) {
                        if (pending.route.equals(below.route)) {
                            if (Math.abs(pendingIdx - belowIdx) <= 1) {
                                // TODO: Consider travelling time
                                if (pending.ttnt > 0 && below.ttnt > 0 && pending.ttnt > below.ttnt) {
                                    pending = below;
                                    pendingIdx = stationIdToIndexMap.get(pending.currentStationCode);
                                }
                            }
                        }
                    }
                }
            }

            if (!dnSaved.contains(pending))
                dnSaved.add(pending);
        }


        for (Trip trip : upSaved) {
            Log.d("upSaved", (trip.isUp ? "UP" : "DN") + " "
                    + Utils.idToCode(this, trip.currentStationCode, lineCode) + " to "
                    + Utils.idToCode(this, trip.nextStationCode, lineCode) + " towards "
                    + Utils.idToCode(this, trip.destinationStationCode, lineCode) + " "
                    + trip.ttnt);
        }
        for (Trip trip : dnSaved) {
            Log.d("dnSaved", (trip.isUp ? "UP" : "DN") + " "
                    + Utils.idToCode(this, trip.currentStationCode, lineCode) + " to "
                    + Utils.idToCode(this, trip.nextStationCode, lineCode) + " towards "
                    + Utils.idToCode(this, trip.destinationStationCode, lineCode) + " "
                    + trip.ttnt);
        }

        List<Trip> savedTrips = new ArrayList<>();
        savedTrips.addAll(upSaved);
        savedTrips.addAll(dnSaved);

        return savedTrips;
    }

    private long getRunTimeOnlyBetween(int startCode, int endCode, boolean isUp) {
        int startIdx = stationIdToIndexMap.getOrDefault(startCode, -1);
        int endIdx = stationIdToIndexMap.getOrDefault(endCode, -1);
        if (startIdx == -1 || endIdx == -1) return 0;

        int minIdx = Math.min(startIdx, endIdx);
        int maxIdx = Math.max(startIdx, endIdx);
        long totalRunSeconds = 0;

        for (int i = minIdx; i < maxIdx; i++) {
            int code = lineConfig.stationIDs[i];
            int nextCode = lineConfig.stationIDs[i + 1];
            if (isUp) {
                totalRunSeconds += lineConfig.runTimeUpMap.getOrDefault(nextCode, 120L);
            } else {
                totalRunSeconds += lineConfig.runTimeDnMap.getOrDefault(code, 120L);
            }
        }
        return totalRunSeconds;
    }

    private String[] getStationArray() {
        int resId = getResources().getIdentifier(lineCode.toLowerCase() + "_station_code", "string", getPackageName());
        return resId != 0 ? getString(resId).split("\\s+") : new String[0];
    }

    private String[] getStationIdArray() {
        int resId = getResources().getIdentifier(lineCode.toLowerCase() + "_station_id", "string", getPackageName());
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