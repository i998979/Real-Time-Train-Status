package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EastRailJRActivity extends AppCompatActivity {

    private RecyclerView rvEastRailLine;
    private JRLineAdapter adapter;
    private List<Trip> activeTrips = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String lineCode = "eal";
    // Low Wu -> Admiralty, no Racecourse
    private int[] stationCodes = {13, 12, 11, 10, 9, 8, 6, 5, 4, 3, 2, 21, 22, 23};

    private HashMap<Integer, Long> runTimeUpMap = new HashMap<>();
    private HashMap<Integer, Long> runTimeDnMap = new HashMap<>();
    private HashMap<Integer, Long> dwellTimeUpMap = new HashMap<>();
    private HashMap<Integer, Long> dwellTimeDnMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_east_rail_jr);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        boolean isDarkMode = (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES;

        controller.setAppearanceLightStatusBars(!isDarkMode);
        controller.setAppearanceLightNavigationBars(!isDarkMode);

        rvEastRailLine = findViewById(R.id.rv_east_rail_line);
        rvEastRailLine.setLayoutManager(new LinearLayoutManager(this));

        ViewCompat.setOnApplyWindowInsetsListener(rvEastRailLine, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        runTimeUpMap = new HashMap<>();
        runTimeDnMap = new HashMap<>();
        dwellTimeUpMap = new HashMap<>();
        dwellTimeDnMap = new HashMap<>();

        // --- 北行 (Up: ADM -> LOW/LMC) ---
        // Run Time (行車)
        runTimeUpMap.put(1, 94L);
        runTimeUpMap.put(2, 185L);
        runTimeUpMap.put(3, 211L);
        runTimeUpMap.put(4, 123L);
        runTimeUpMap.put(5, 216L);
        runTimeUpMap.put(6, 99L);
        runTimeUpMap.put(7, 120L);
        runTimeUpMap.put(8, 163L);
        runTimeUpMap.put(9, 303L);
        runTimeUpMap.put(10, 97L);
        runTimeUpMap.put(11, 265L);
        runTimeUpMap.put(12, 106L);
        runTimeUpMap.put(13, 202L);
        runTimeUpMap.put(14, 408L);

        // Dwell Time (停站)
        dwellTimeUpMap.put(23, 47L);
        dwellTimeUpMap.put(1, 44L);
        dwellTimeUpMap.put(2, 44L);
        dwellTimeUpMap.put(3, 47L);
        dwellTimeUpMap.put(4, 47L);
        dwellTimeUpMap.put(5, 35L);
        dwellTimeUpMap.put(6, 35L);
        dwellTimeUpMap.put(7, 35L);
        dwellTimeUpMap.put(8, 35L);
        dwellTimeUpMap.put(9, 35L);
        dwellTimeUpMap.put(10, 35L);
        dwellTimeUpMap.put(11, 35L);
        dwellTimeUpMap.put(12, 47L);

        // --- 南行 (Dn: LOW/LMC -> ADM) ---
        // Run Time (行車)
        runTimeDnMap.put(12, 197L); // 預設羅湖出發
        runTimeDnMap.put(11, 106L);
        runTimeDnMap.put(10, 262L);
        runTimeDnMap.put(9, 96L);
        runTimeDnMap.put(8, 301L);
        runTimeDnMap.put(6, 160L);
        runTimeDnMap.put(7, 120L);
        runTimeDnMap.put(5, 98L);
        runTimeDnMap.put(4, 220L);
        runTimeDnMap.put(3, 125L);
        runTimeDnMap.put(2, 158L);
        runTimeDnMap.put(1, 184L);
        runTimeDnMap.put(23, 93L);

        // Dwell Time (停站)
        dwellTimeDnMap.put(13, 47L);
        dwellTimeDnMap.put(14, 47L);
        dwellTimeDnMap.put(12, 35L);
        dwellTimeDnMap.put(11, 35L);
        dwellTimeDnMap.put(10, 40L);
        dwellTimeDnMap.put(9, 30L);
        dwellTimeDnMap.put(8, 35L);
        dwellTimeDnMap.put(6, 40L);
        dwellTimeDnMap.put(7, 40L);
        dwellTimeDnMap.put(5, 47L);
        dwellTimeDnMap.put(4, 47L);
        dwellTimeDnMap.put(3, 44L);
        dwellTimeDnMap.put(2, 44L);
        dwellTimeDnMap.put(1, 44L);


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            lineCode = extras.getString("LINE_CODE", "eal");
            int[] codes = extras.getIntArray("STATION_CODES");
            if (codes != null) stationCodes = codes;

            if (extras.getSerializable("RUN_TIME_UP_MAP") != null)
                runTimeUpMap = (HashMap<Integer, Long>) extras.getSerializable("RUN_TIME_UP_MAP");
            if (extras.getSerializable("RUN_TIME_DN_MAP") != null)
                runTimeDnMap = (HashMap<Integer, Long>) extras.getSerializable("RUN_TIME_DN_MAP");

            if (extras.getSerializable("DWELL_TIME_UP_MAP") != null)
                dwellTimeUpMap = (HashMap<Integer, Long>) extras.getSerializable("DWELL_TIME_UP_MAP");
            if (extras.getSerializable("DWELL_TIME_DN_MAP") != null)
                dwellTimeDnMap = (HashMap<Integer, Long>) extras.getSerializable("DWELL_TIME_DN_MAP");

        }
        /*lineCode = "tml";
        stationCodes = new int[]{49, 48, 47, 46, 45, 44, 43, 42, 41, 50, 14, 1,
                61, 62, 63, 64, 65, 66, 21, 22, 23, 24, 25, 26, 27, 28, 29};*/

        adapter = new JRLineAdapter(this, lineCode, stationCodes, activeTrips);
        rvEastRailLine.setAdapter(adapter);

        rvEastRailLine.setClipChildren(false);
        rvEastRailLine.setClipToPadding(false);

        TextView tvBannerName = findViewById(R.id.tv_banner_name);
        tvBannerName.setText(Utils.getLineName(lineCode, true));

        FrameLayout lineBanner = findViewById(R.id.line_banner);
        int colorResId = getResources().getIdentifier(this.lineCode.toLowerCase(), "color", getPackageName());
        int lineColor = getResources().getColor(colorResId, null);
        lineBanner.setBackgroundColor(lineColor);


        startRefreshLoop();
    }

    private void startRefreshLoop() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                fetchDataInBackground();
                mainHandler.postDelayed(this, 10000);
            }
        });
    }

    private void fetchDataInBackground() {
        new Thread(() -> {
            try {
                URL url = new URL("LINK");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                List<Trip> trips = parseJson(result.toString());

                mainHandler.post(() -> {
                    activeTrips.clear();
                    activeTrips.addAll(trips);
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                Log.e("JR_LOG", "Fetch Error: " + e.getMessage());
            }
        }).start();
    }

    private List<Trip> parseJson(String json) throws Exception {
        List<Trip> list = new ArrayList<>();
        JSONArray array = new JSONArray(json);
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

            list.add(new Trip(
                    obj.optString("trainId"), "", obj.optDouble("trainSpeed"),
                    obj.optInt("currentStationCode"), obj.optInt("nextStationCode"),
                    obj.optInt("destinationStationCode"), cars, obj.optLong("receivedTime"),
                    obj.optLong("ttl"), obj.optInt("doorStatus"), obj.optString("td"),
                    obj.optInt("targetDistance"), obj.optInt("startDistance")
            ));
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }
}