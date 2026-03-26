package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class TrainLocationActivity extends AppCompatActivity {
    public static Context context;

    private RecyclerView rvTrainLoc;
    private TrainLocationAdapter adapter;
    private FrameLayout lineBanner;

    private final List<Trip> activeTrips = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<Integer, Integer> stationIdToIndexMap = new HashMap<>();

    private String lineCode;
    private String dataSource;
    private LineConfig lineConfig;

    private static final Map<String, String[]> LINE_DIRECTIONS = new HashMap<>() {{
        put("ael", new String[]{"博覽館方面", "香港方面"});
        put("drl", new String[]{"欣澳方面", "迪士尼方面"});
        put("eal", new String[]{"羅湖・落馬洲方面", "金鐘方面"});
        put("isl", new String[]{"柴灣方面", "堅尼地城方面"});
        put("ktl", new String[]{"調景嶺方面", "黃埔方面"});
        put("sil", new String[]{"海怡半島方面", "金鐘方面"});
        put("tcl", new String[]{"東涌方面", "香港方面"});
        put("tkl", new String[]{"寶琳・康城方面", "北角方面"});
        put("tml", new String[]{"屯門方面", "烏溪沙方面"});
        put("twl", new String[]{"荃灣方面", "中環方面"});
    }};

    private boolean isBannerCurrentlyHidden = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_train_location);

        TrainLocationActivity.context = this;

        MaterialButton btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            finish();
        });
        MaterialButton btnGuide = findViewById(R.id.btn_guide);
        btnGuide.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

            View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_train_location_guide_bottom_sheet, null);
            bottomSheetDialog.setContentView(sheetView);

            View parent = (View) sheetView.getParent();
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            int displayHeight = getResources().getDisplayMetrics().heightPixels;
            int targetHeight = (int) (displayHeight * 0.9);
            behavior.setPeekHeight(targetHeight);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            ViewGroup.LayoutParams lp = parent.getLayoutParams();
            lp.height = targetHeight;
            parent.setLayoutParams(lp);

            MaterialButton btnClose1 = sheetView.findViewById(R.id.btn_close);
            btnClose1.setOnClickListener(view -> {
                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.show();
        });

        MaterialButton btnRefresh = findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            fetchData();
        });

        lineBanner = findViewById(R.id.line_banner);

        lineCode = getIntent().getStringExtra("LINE_CODE");
        if (lineCode == null) lineCode = "eal";
        dataSource = getIntent().getStringExtra("DATA_SOURCE");
        if (dataSource == null) dataSource = "OPENDATA";
        lineConfig = LineConfig.get(this, lineCode);

        MaterialTextView tvUpDir = findViewById(R.id.tv_up_dir);
        MaterialTextView tvDnDir = findViewById(R.id.tv_dn_dir);

        String[] dirs = LINE_DIRECTIONS.get(lineCode.toLowerCase());
        tvUpDir.setText(dirs[0]);
        tvDnDir.setText(dirs[1]);

        stationIdToIndexMap.clear();
        String[] stations = getStationArray();
        for (int i = 0; i < stations.length; i++) {
            stationIdToIndexMap.put(Utils.codeToId(this, lineCode, stations[i]), i);
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

        rvTrainLoc = findViewById(R.id.rv_line);
        rvTrainLoc.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrainLocationAdapter(this, lineCode, lineConfig.stationIDs, activeTrips,
                lineConfig.runTimeUpMap, lineConfig.runTimeDnMap, lineConfig.dwellTimeUpMap, lineConfig.dwellTimeDnMap);
        rvTrainLoc.setAdapter(adapter);

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int startTime = 1 * 60 + 30; // 01:30
        int endTime = 5 * 60;        // 05:00

        boolean isMaintenanceTime = currentTimeInMinutes >= startTime && currentTimeInMinutes < endTime;

        LinearLayout nthMessage = findViewById(R.id.layout_nth);
        if (isMaintenanceTime)
            nthMessage.setVisibility(View.VISIBLE);
        else
            nthMessage.setVisibility(View.GONE);


        NestedScrollView scrollView = findViewById(R.id.nested_scroll_view);
        int threshold = (int) (50 * getResources().getDisplayMetrics().density);

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > threshold && !isBannerCurrentlyHidden) {
                hideTopBar();
            } else if (scrollY <= threshold && isBannerCurrentlyHidden) {
                showTopBar();
            }
        });

        TextView tvRefreshTime = findViewById(R.id.tv_refresh_time);
        tvRefreshTime.setText("");
        fetchData();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            View topBarContainer = findViewById(R.id.top_bar_container);
            View btnClose = findViewById(R.id.btn_close);
            View btnRefresh = findViewById(R.id.btn_refresh);
            NestedScrollView scrollView = findViewById(R.id.nested_scroll_view);

            boolean isAtTop = scrollView.getScrollY() <= 5;

            if (isAtTop) {
                showTopBar();
            } else {
                int[] closeLocation = new int[2];
                btnClose.getLocationOnScreen(closeLocation);

                int[] refreshLocation = new int[2];
                btnRefresh.getLocationOnScreen(refreshLocation);

                float x = ev.getRawX();
                float y = ev.getRawY();

                boolean clickedClose = (x >= closeLocation[0] && x <= (closeLocation[0] + btnClose.getWidth()) &&
                        y >= closeLocation[1] && y <= (closeLocation[1] + btnClose.getHeight()));

                boolean clickedRefresh = (x >= refreshLocation[0] && x <= (refreshLocation[0] + btnRefresh.getWidth()) &&
                        y >= refreshLocation[1] && y <= (refreshLocation[1] + btnRefresh.getHeight()));

                if (!clickedClose && !clickedRefresh) {

                    int[] topBarLocation = new int[2];
                    topBarContainer.getLocationOnScreen(topBarLocation);

                    if (!(x >= topBarLocation[0] && x <= (topBarLocation[0] + topBarContainer.getWidth()) &&
                            y >= topBarLocation[1] && y <= (topBarLocation[1] + topBarContainer.getHeight()))) {

                        if (isBannerCurrentlyHidden) {
                            showTopBar();
                        } else {
                            hideTopBar();
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void showTopBar() {
        isBannerCurrentlyHidden = false;
        lineBanner.animate()
                .translationY(0)
                .setDuration(200)
                .start();
    }

    private void hideTopBar() {
        isBannerCurrentlyHidden = true;
        lineBanner.animate()
                .translationY(-lineBanner.getHeight())
                .setDuration(200)
                .start();
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
                }

                List<Trip> cleanList = processPhysicsBasedDedup(rawList);
                cleanList.sort(Comparator.comparingLong(t -> t.time));

                mainHandler.post(() -> {
                    TextView tvRefreshTime = findViewById(R.id.tv_refresh_time);

                    activeTrips.clear();
                    activeTrips.addAll(cleanList);
                    Log.d("JR_LOG", "Valid Trains: " + activeTrips.size());
                    for (Trip trip : activeTrips) {
                        Log.d("JR_LOG", (trip.isUp ? "UP" : "DN") + " from "
                                + Utils.idToCode(this, lineCode, trip.currentStationCode) + " to "
                                + Utils.idToCode(this, lineCode, trip.nextStationCode) + " towards "
                                + Utils.idToCode(this, lineCode, trip.destinationStationCode) + " "
                                + trip.ttnt + " ");
                    }

                    SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日 HH:mm", Locale.getDefault());
                    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                    String currentTime = dateFormat.format(new Date());
                    tvRefreshTime.setText(currentTime);

                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<Trip> parseNtJson(JSONObject stationJson, String station) throws Exception {
        int stationCode = Utils.codeToId(this, lineCode, station);

        List<Trip> list = new ArrayList<>();
        String[] dirs = {"UP", "DOWN"};
        for (String dir : dirs) {
            if (!stationJson.has(dir)) continue;

            JSONArray trains = stationJson.getJSONArray(dir);
            for (int i = 0; i < trains.length(); i++) {
                JSONObject tObj = trains.getJSONObject(i);

                int destCode = Utils.codeToId(this, lineCode, tObj.getString("dest"));
                long time = Utils.convertTimestampToMillis(tObj.getString("time"));
                int ttnt = tObj.optInt("ttnt", 0);
                String route = tObj.optString("route", "");
                String timeType = tObj.optString("timeType", "A");

                // Current Station = Next Station, once the station leaves, its next occurrence is already next station
                Trip trip = new Trip(stationCode, stationCode, destCode, dir, i + 1, time, ttnt, route, timeType);
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
                    + Utils.idToCode(this, lineCode, trip.currentStationCode) + " to "
                    + Utils.idToCode(this, lineCode, trip.nextStationCode) + " towards "
                    + Utils.idToCode(this, lineCode, trip.destinationStationCode) + " "
                    + trip.ttnt);
        }
        for (Trip trip : dnList) {
            Log.d("dnList", (trip.isUp ? "UP" : "DN") + " "
                    + Utils.idToCode(this, lineCode, trip.currentStationCode) + " to "
                    + Utils.idToCode(this, lineCode, trip.nextStationCode) + " towards "
                    + Utils.idToCode(this, lineCode, trip.destinationStationCode) + " "
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
                                if (pending.ttnt > below.ttnt) {
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
                                if (pending.ttnt > below.ttnt) {
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
                    + Utils.idToCode(this, lineCode, trip.currentStationCode) + " to "
                    + Utils.idToCode(this, lineCode, trip.nextStationCode) + " towards "
                    + Utils.idToCode(this, lineCode, trip.destinationStationCode) + " "
                    + trip.ttnt);
        }
        for (Trip trip : dnSaved) {
            Log.d("dnSaved", (trip.isUp ? "UP" : "DN") + " "
                    + Utils.idToCode(this, lineCode, trip.currentStationCode) + " to "
                    + Utils.idToCode(this, lineCode, trip.nextStationCode) + " towards "
                    + Utils.idToCode(this, lineCode, trip.destinationStationCode) + " "
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
        if (lineConfig == null || lineConfig.stationCodes == null) return new String[0];

        return lineConfig.stationCodes;
    }

    private String[] getStationIdArray() {
        if (lineConfig == null || lineConfig.stationIDs == null) return new String[0];

        String[] ids = new String[lineConfig.stationIDs.length];
        for (int i = 0; i < lineConfig.stationIDs.length; i++) {
            ids[i] = String.valueOf(lineConfig.stationIDs[i]);
        }
        return ids;
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