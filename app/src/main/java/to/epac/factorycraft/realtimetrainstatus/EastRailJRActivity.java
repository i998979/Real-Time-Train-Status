package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
import java.util.List;

public class EastRailJRActivity extends AppCompatActivity {

    private RecyclerView rvLine;
    private JRLineAdapter adapter;
    private List<Trip> activeTrips = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private String lineCode;
    private LineConfig lineConfig;

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

        rvLine = findViewById(R.id.rv_line);
        rvLine.setLayoutManager(new LinearLayoutManager(this));

        ViewCompat.setOnApplyWindowInsetsListener(rvLine, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. 取得線路代碼 (預設 eal)
        lineCode = getIntent().getStringExtra("LINE_CODE");
        if (lineCode == null) lineCode = "tml";

        // 2. 從 Config 類別載入對應數據
        lineConfig = LineConfig.get(lineCode);

        // 3. UI 基礎設定
        setupSystemBars();
        setupBanner();

        ImageButton btnClose = findViewById(R.id.btn_close_activity);
        btnClose.setOnClickListener(v -> finish());

        // 4. 初始化 Adapter (傳入載入後的數據)
        rvLine = findViewById(R.id.rv_line);
        rvLine.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JRLineAdapter(this, lineCode, lineConfig.stationCodes, activeTrips,
                lineConfig.runTimeUpMap, lineConfig.runTimeDnMap,
                lineConfig.dwellTimeUpMap, lineConfig.dwellTimeDnMap);
        rvLine.setAdapter(adapter);

        rvLine.setClipChildren(false);
        rvLine.setClipToPadding(false);

        startRefreshLoop();
    }

    private void setupBanner() {
        TextView tvBannerName = findViewById(R.id.tv_banner_name);
        tvBannerName.setText(Utils.getLineName(lineCode, true));

        FrameLayout lineBanner = findViewById(R.id.line_banner);
        int colorResId = getResources().getIdentifier(lineCode.toLowerCase(), "color", getPackageName());
        int lineColor = getResources().getColor(colorResId, null);
        lineBanner.setBackgroundColor(lineColor);
    }

    private void setupSystemBars() {
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        controller.setAppearanceLightStatusBars(!isDarkMode);
        controller.setAppearanceLightNavigationBars(!isDarkMode);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rv_line), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                URL url = new URL(lineConfig.apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("x-api-key", "QkmjCRYvXt6o89UdZAvoXa49543NxOtU2tBhQQDQ");
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
        JSONArray array;

        // 判斷 JSON 是物件格式 (TML) 還是陣列格式 (EAL)
        if (json.trim().startsWith("{")) {
            JSONObject root = new JSONObject(json);
            array = root.getJSONArray("Items"); // 處理 TML 格式
        } else {
            array = new JSONArray(json); // 處理 EAL 格式
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            List<Car> cars = new ArrayList<>();
            if (obj.has("listCars")) {
                JSONArray carArray = obj.getJSONArray("listCars");
                for (int j = 0; j < carArray.length(); j++) {
                    JSONObject c = carArray.getJSONObject(j);
                    cars.add(new Car(
                            c.optInt("carLoad"),
                            c.optInt("passengerCount"),
                            c.optString("carName"),
                            c.optInt("passengerLoad")
                    ));
                }
            }

            // 欄位兼容性處理
            String trainId = obj.optString("trainId");

            // 方向判斷：優先使用 td，若為 "UNKNOWN" 或不存在，則回退到 trainId
            String td = obj.optString("td", "UNKNOWN");
            if (td.equals("UNKNOWN")) {
                td = trainId;
            }

            // 門狀態判斷：EAL 是 String "0"，TML 是 Boolean false
            int doorStatus = 0;
            Object doorObj = obj.opt("doorStatus");
            if (doorObj instanceof Boolean) {
                doorStatus = (Boolean) doorObj ? 1 : 0;
            } else if (doorObj instanceof String) {
                doorStatus = Integer.parseInt((String) doorObj);
            }

            // 距離欄位兼容
            int targetDist = obj.has("targetDistance") ?
                    obj.optInt("targetDistance") :
                    obj.optInt("distanceFromCurrentStation", 0);

            list.add(new Trip(
                    trainId,
                    "",
                    obj.optDouble("trainSpeed", 0.0),
                    obj.optInt("currentStationCode"),
                    obj.optInt("nextStationCode"),
                    obj.optInt("destinationStationCode"),
                    cars,
                    obj.optLong("receivedTime"),
                    obj.optLong("ttl"),
                    doorStatus,
                    td,
                    targetDist,
                    obj.optInt("startDistance", 0)
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