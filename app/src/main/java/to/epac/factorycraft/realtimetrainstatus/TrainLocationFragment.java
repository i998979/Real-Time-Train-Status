package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
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

public class TrainLocationFragment extends Fragment {
    private RecyclerView rvTrainLoc;
    private TrainLocationAdapter adapter;
    private FrameLayout lineBanner;
    private TabLayout tabLayout;

    private final List<Trip> activeTrips = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<Integer, Integer> stationIdToIndexMap = new HashMap<>();

    private String lineCode;
    private String dataSource;
    private LineConfig lineConfig;

    private boolean isBannerCurrentlyHidden = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_train_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnRefresh = view.findViewById(R.id.btn_refresh);
        btnRefresh.setOnClickListener(v -> {
            fetchData();
        });

        lineBanner = getActivity().findViewById(R.id.line_banner);
        tabLayout = getActivity().findViewById(R.id.tab_layout);

        lineCode = getArguments().getString("LINE_CODE");
        if (lineCode == null) lineCode = "eal";
        dataSource = getArguments().getString("DATA_SOURCE");
        if (dataSource == null) dataSource = "OPENDATA";
        lineConfig = LineConfig.get(requireContext(), lineCode);

        MaterialTextView tvUpDir = view.findViewById(R.id.tv_up_dir);
        MaterialTextView tvDnDir = view.findViewById(R.id.tv_dn_dir);
        if (lineCode.equalsIgnoreCase("ael")) {
            tvUpDir.setText("博覽館方面");
            tvDnDir.setText("香港方面");
        } else if (lineCode.equalsIgnoreCase("drl")) {
            tvUpDir.setText("欣澳方面");
            tvDnDir.setText("迪士尼方面");
        } else if (lineCode.equalsIgnoreCase("eal")) {
            tvUpDir.setText("羅湖/落馬洲方面");
            tvDnDir.setText("金鐘方面");
        } else if (lineCode.equalsIgnoreCase("isl")) {
            tvUpDir.setText("柴灣方面");
            tvDnDir.setText("堅尼地城方面");
        } else if (lineCode.equalsIgnoreCase("ktl")) {
            tvUpDir.setText("調景嶺方面");
            tvDnDir.setText("黃埔方面");
        } else if (lineCode.equalsIgnoreCase("sil")) {
            tvUpDir.setText("海怡半島方面");
            tvDnDir.setText("金鐘方面");
        } else if (lineCode.equalsIgnoreCase("tcl")) {
            tvUpDir.setText("東涌方面");
            tvDnDir.setText("香港方面");
        } else if (lineCode.equalsIgnoreCase("tkl")) {
            tvUpDir.setText("寶琳/康城方面");
            tvDnDir.setText("北角方面");
        } else if (lineCode.equalsIgnoreCase("tml")) {
            tvUpDir.setText("屯門方面");
            tvDnDir.setText("烏溪沙方面");
        } else if (lineCode.equalsIgnoreCase("twl")) {
            tvUpDir.setText("荃灣方面");
            tvDnDir.setText("中環方面");
        }

        stationIdToIndexMap.clear();
        String[] stations = getStationArray();
        for (int i = 0; i < stations.length; i++) {
            int id = Utils.codeToId(requireContext(), lineCode, stations[i]);
            if (id != -1) stationIdToIndexMap.put(id, i);
        }

        rvTrainLoc = view.findViewById(R.id.rv_line);
        rvTrainLoc.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TrainLocationAdapter(requireContext(), lineCode, lineConfig.stationIDs, activeTrips,
                lineConfig.runTimeUpMap, lineConfig.runTimeDnMap, lineConfig.dwellTimeUpMap, lineConfig.dwellTimeDnMap);
        rvTrainLoc.setAdapter(adapter);

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int startTime = 1 * 60 + 30; // 01:30
        int endTime = 5 * 60;        // 05:00

        boolean isMaintenanceTime = currentTimeInMinutes >= startTime && currentTimeInMinutes < endTime;

        LinearLayout nthMessage = view.findViewById(R.id.layout_nth);
        nthMessage.setVisibility(isMaintenanceTime ? View.VISIBLE : View.GONE);


        NestedScrollView scrollView = view.findViewById(R.id.nested_scroll_view);
        int threshold = (int) (50 * getResources().getDisplayMetrics().density);

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > threshold && !isBannerCurrentlyHidden) {
                hideTopBar();
            } else if (scrollY <= threshold && isBannerCurrentlyHidden) {
                showTopBar();
            }
        });

        TextView tvRefreshTime = view.findViewById(R.id.tv_refresh_time);
        tvRefreshTime.setText("");
        fetchData();
    }

    private void hideTopBar() {
        isBannerCurrentlyHidden = true;
        lineBanner.animate()
                .translationY(-lineBanner.getHeight())
                .setDuration(200)
                .start();
        tabLayout.animate()
                .translationY(-lineBanner.getHeight() - tabLayout.getHeight())
                .setDuration(200)
                .start();
    }

    private void showTopBar() {
        isBannerCurrentlyHidden = false;
        lineBanner.animate()
                .translationY(0)
                .setDuration(200)
                .start();
        tabLayout.animate()
                .translationY(0)
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
                    if (getView() == null || !isAdded()) return;

                    TextView tvRefreshTime = getView().findViewById(R.id.tv_refresh_time);

                    activeTrips.clear();
                    activeTrips.addAll(cleanList);
                    Log.d("JR_LOG", "Valid Trains: " + activeTrips.size());
                    for (Trip trip : activeTrips) {
                        Log.d("JR_LOG", (trip.isUp ? "UP" : "DN") + " from "
                                + Utils.idToCode(requireContext(), lineCode, trip.currentStationCode) + " to "
                                + Utils.idToCode(requireContext(), lineCode, trip.nextStationCode) + " towards "
                                + Utils.idToCode(requireContext(), lineCode, trip.destinationStationCode) + " "
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
        int stationCode = Utils.codeToId(requireContext(), lineCode, station);

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

                int destCode = Utils.codeToId(requireContext(), lineCode, tObj.getString("dest"));
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

        rawList.stream().forEach(trip -> Log.d("tagg", trip.isUp + ""));

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
                    + Utils.idToCode(requireContext(), lineCode, trip.currentStationCode) + " to "
                    + Utils.idToCode(requireContext(), lineCode, trip.nextStationCode) + " towards "
                    + Utils.idToCode(requireContext(), lineCode, trip.destinationStationCode) + " "
                    + trip.ttnt);
        }
        for (Trip trip : dnList) {
            Log.d("dnList", (trip.isUp ? "UP" : "DN") + " "
                    + Utils.idToCode(requireContext(), lineCode, trip.currentStationCode) + " to "
                    + Utils.idToCode(requireContext(), lineCode, trip.nextStationCode) + " towards "
                    + Utils.idToCode(requireContext(), lineCode, trip.destinationStationCode) + " "
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
                    + Utils.idToCode(requireContext(), lineCode, trip.currentStationCode) + " to "
                    + Utils.idToCode(requireContext(), lineCode, trip.nextStationCode) + " towards "
                    + Utils.idToCode(requireContext(), lineCode, trip.destinationStationCode) + " "
                    + trip.ttnt);
        }
        for (Trip trip : dnSaved) {
            Log.d("dnSaved", (trip.isUp ? "UP" : "DN") + " "
                    + Utils.idToCode(requireContext(), lineCode, trip.currentStationCode) + " to "
                    + Utils.idToCode(requireContext(), lineCode, trip.nextStationCode) + " towards "
                    + Utils.idToCode(requireContext(), lineCode, trip.destinationStationCode) + " "
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