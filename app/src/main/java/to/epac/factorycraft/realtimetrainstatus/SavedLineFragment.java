package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SavedLineFragment extends Fragment {

    private SharedPreferences prefs;
    private static final String KEY_SAVED_LINES = "saved_lines_csv"; // 用逗號分隔的線路碼 (e.g. "EAL,TML")

    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutSavedLines;
    private LinearLayout statusContainer;
    private MaterialButton btnRegisterEmpty;
    private TextView btnEditLines;

    private boolean isFetching = false;
    private final ExecutorService crossCheckExecutor = Executors.newFixedThreadPool(10);
    private HRConfig hrConf;

    // 定義需交叉比對 nexttrain API 的車站
    private static final Map<String, String> CHECK_STATIONS = new HashMap<>() {{
        put("EAL", "TAW");
        put("TML", "HUH");
        put("KTL", "PRE");
        put("AEL", "HOK");
        put("DRL", "SUN");
        put("ISL", "ADM");
        put("TCL", "HOK");
        put("TKL", "TIK");
        put("TWL", "ADM");
        put("SIL", "ADM");
    }};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_line, container, false);

        prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        hrConf = HRConfig.getInstance(getContext());

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutSavedLines = view.findViewById(R.id.layout_saved_lines);
        statusContainer = view.findViewById(R.id.status_container);
        btnRegisterEmpty = view.findViewById(R.id.btn_register_empty);
        btnEditLines = view.findViewById(R.id.btn_edit_lines);

        // 如果點擊空狀態的「登錄」或已儲存列表的「編輯」，都會開啟同一個編輯 BottomSheet (P2 / P4)
        View.OnClickListener openEditSheet = v -> showEditBottomSheet();
        btnRegisterEmpty.setOnClickListener(openEditSheet);
        btnEditLines.setOnClickListener(openEditSheet);

        swipeRefreshLayout.setOnRefreshListener(this::fetchSavedLinesData);

        refreshUIState();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!crossCheckExecutor.isShutdown()) {
            crossCheckExecutor.shutdownNow();
        }
    }

    /**
     * 檢查 SharedPreferences 中的狀態，決定顯示 P1 還是 P5。
     */
    private void refreshUIState() {
        String savedCsv = prefs.getString(KEY_SAVED_LINES, "");
        if (savedCsv.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            layoutSavedLines.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(false); // 沒路線不給下拉刷新
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            layoutSavedLines.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setEnabled(true);
            fetchSavedLinesData(); // 載入資料 (P5)
        }
    }

    /**
     * 讀取暫存於 SharedPreferences 的清單
     */
    private List<String> getSavedLinesList() {
        String savedCsv = prefs.getString(KEY_SAVED_LINES, "");
        if (savedCsv.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(savedCsv.split(",")));
    }

    private void saveLinesList(List<String> list) {
        prefs.edit().putString(KEY_SAVED_LINES, String.join(",", list)).apply();
    }

    // ==========================================
    // UI 邏輯：P2 / P4 編輯路線 BottomSheet
    // ==========================================
    private void showEditBottomSheet() {
        BottomSheetDialog editDialog = new BottomSheetDialog(requireContext());
        // 動態建立 P2 的佈局以減少檔案
        LinearLayout rootLayout = new LinearLayout(getContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(32, 32, 32, 64);

        // --- 頂部 Title 與關閉按鈕 ---
        // (此處省略部分基礎佈局代碼以保持精簡，實際中你可以直接 Inflate 一個寫好的 XML，例如 bottom_sheet_edit.xml)
        TextView title = new TextView(getContext());
        title.setText("編輯常用路線");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 32);
        rootLayout.addView(title);

        // --- P2 中間的「新增路線 (放大鏡)」按鈕 ---
        MaterialCardView btnAdd = new MaterialCardView(getContext());
        btnAdd.setCardBackgroundColor(Color.parseColor("#333333"));
        btnAdd.setRadius(24);
        TextView tvAdd = new TextView(getContext());
        tvAdd.setText("🔍 追加路線");
        tvAdd.setTextColor(Color.WHITE);
        tvAdd.setPadding(48, 32, 48, 32);
        btnAdd.addView(tvAdd);
        rootLayout.addView(btnAdd);

        btnAdd.setOnClickListener(v -> {
            editDialog.dismiss();
            showAddLineBottomSheet(); // 開啟 P3
        });

        // --- P4 的 RecyclerView (支援 Drag and Drop) ---
        RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<String> currentSaved = getSavedLinesList();

        // 極簡內聯 Adapter
        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                TextView tv = new TextView(getContext());
                tv.setPadding(32, 48, 32, 48);
                tv.setTextSize(18);
                tv.setTextColor(Color.WHITE);
                return new RecyclerView.ViewHolder(tv) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                String lineCode = currentSaved.get(position);
                // 這裡可以透過 HRConfig 拿線路名稱，這裡簡化處理
                ((TextView) holder.itemView).setText("≡  " + hrConf.getLineByAlias(lineCode));
            }

            @Override
            public int getItemCount() {
                return currentSaved.size();
            }
        };
        recyclerView.setAdapter(adapter);

        // 加入 Drag & Drop (P4 邏輯)
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                Collections.swap(currentSaved, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                saveLinesList(currentSaved); // 每次排序完即時儲存
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);

        rootLayout.addView(recyclerView);

        editDialog.setContentView(rootLayout);
        editDialog.setOnDismissListener(dialog -> refreshUIState()); // 關閉時重整主畫面
        editDialog.show();
    }

    // ==========================================
    // UI 邏輯：P3 選擇新增路線 BottomSheet
    // ==========================================
    private void showAddLineBottomSheet() {
        BottomSheetDialog addDialog = new BottomSheetDialog(requireContext());
        LinearLayout rootLayout = new LinearLayout(getContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(32, 32, 32, 64);

        TextView title = new TextView(getContext());
        title.setText("選擇想加入的路線");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 32);
        rootLayout.addView(title);

        // 讀取所有可用的路線 (排除 HSR 高鐵)
        List<HRConfig.Line> allLines = new ArrayList<>();
        // 此處假設 HRConfig 有取得所有路線的列表
        for (HRConfig.Line l : hrConf.getAllLines()) {
            if (!l.alias.equals("HSR")) allLines.add(l);
        }

        // 動態產生清單 (取代 Adapter)
        for (HRConfig.Line line : allLines) {
            TextView tv = new TextView(getContext());
            tv.setText(line.name + " (" + line.alias + ")");
            tv.setPadding(32, 48, 32, 48);
            tv.setTextSize(18);

            tv.setOnClickListener(v -> {
                List<String> currentSaved = getSavedLinesList();
                if (!currentSaved.contains(line.alias)) {
                    currentSaved.add(line.alias);
                    saveLinesList(currentSaved);
                    Toast.makeText(getContext(), "已加入: " + line.name, Toast.LENGTH_SHORT).show();
                }
                addDialog.dismiss();
                showEditBottomSheet(); // 返回 P4
            });
            rootLayout.addView(tv);
        }

        addDialog.setContentView(rootLayout);
        addDialog.show();
    }


    // ==========================================
    // API 邏輯：P5 獲取資料並渲染卡片
    // 此段邏輯高度借鑒 TrafficNewsFragment
    // ==========================================
    private void fetchSavedLinesData() {
        if (isFetching) return;
        List<String> savedLines = getSavedLinesList();
        if (savedLines.isEmpty()) {
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        isFetching = true;
        swipeRefreshLayout.setRefreshing(true);

        new Thread(() -> {
            try {
                // 1. 抓取 Tnews 大表
                URL url = new URL("https://tnews.mtr.com.hk/alert/ryg_line_status.json");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    JSONObject root = new JSONObject(response.toString());
                    JSONArray linesArray = root.getJSONObject("ryg_status").getJSONArray("line");

                    // 2. 過濾出使用者儲存的路線
                    JSONArray targetLines = new JSONArray();
                    for (int i = 0; i < linesArray.length(); i++) {
                        JSONObject obj = linesArray.getJSONObject(i);
                        if (savedLines.contains(obj.getString("line_code"))) {
                            targetLines.put(obj);
                        }
                    }

                    // 3. 交叉比對 nexttrain (直接內聯 CountDownLatch 處理)
                    CountDownLatch latch = new CountDownLatch(targetLines.length());
                    for (int i = 0; i < targetLines.length(); i++) {
                        final JSONObject tLine = targetLines.getJSONObject(i);
                        crossCheckExecutor.execute(() -> {
                            try {
                                String code = tLine.getString("line_code").toUpperCase();
                                String sta = CHECK_STATIONS.getOrDefault(code, "ADM"); // 防呆
                                URL url2 = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=" + code + "&sta=" + sta);
                                HttpURLConnection c2 = (HttpURLConnection) url2.openConnection();
                                c2.setConnectTimeout(3000);
                                try (BufferedReader r2 = new BufferedReader(new InputStreamReader(c2.getInputStream()))) {
                                    StringBuilder sb2 = new StringBuilder();
                                    String l2;
                                    while ((l2 = r2.readLine()) != null) sb2.append(l2);
                                    JSONObject rtData = new JSONObject(sb2.toString());

                                    boolean isApiDelay = rtData.optString("isdelay", "N").equals("Y");
                                    if (isApiDelay && tLine.getString("status").equalsIgnoreCase("green")) {
                                        tLine.put("status", "yellow");
                                        tLine.put("messages", "列車服務可能受阻。");
                                    }
                                }
                                c2.disconnect();
                            } catch (Exception ignored) {
                            } finally {
                                latch.countDown();
                            }
                        });
                    }
                    latch.await(6, TimeUnit.SECONDS);

                    // 4. 將抓到的順序依據 SharedPreferences 中的自訂排序 (Drag&Drop的結果) 重新排列
                    JSONArray sortedLines = new JSONArray();
                    for (String sCode : savedLines) {
                        for (int i = 0; i < targetLines.length(); i++) {
                            JSONObject obj = targetLines.getJSONObject(i);
                            if (obj.getString("line_code").equals(sCode)) {
                                sortedLines.put(obj);
                                break;
                            }
                        }
                    }

                    // 5. 更新 UI
                    new Handler(Looper.getMainLooper()).post(() -> renderCards(sortedLines));
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isFetching = false;
                new Handler(Looper.getMainLooper()).post(() -> swipeRefreshLayout.setRefreshing(false));
            }
        }).start();
    }

    /**
     * 重用你提供的 item_line_status.xml 樣式，動態插入到 statusContainer 中。
     */
    private void renderCards(JSONArray lines) {
        if (!isAdded() || getView() == null) return;
        statusContainer.removeAllViews();

        try {
            for (int i = 0; i < lines.length(); i++) {
                JSONObject lineObj = lines.getJSONObject(i);
                String lineCode = lineObj.getString("line_code");
                String lineNameTc = lineObj.getString("line_name_tc");
                String lineColor = lineObj.getString("line_color");
                String status = lineObj.getString("status");

                // 抓取訊息邏輯與 TrafficNewsFragment 一致，此處簡化處理字串提取
                String displayMessage = "沒有任何延誤";
                if (!status.equals("green") && !status.equals("grey")) {
                    displayMessage = lineObj.optString("messages", "有延誤訊息"); // 實務可擴充為複雜解析
                }

                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_line_status, statusContainer, false);

                View vColorBar = itemView.findViewById(R.id.v_line_color_bar);
                View vBadgeLayout = itemView.findViewById(R.id.line_color_badge);
                TextView tvLineCode = itemView.findViewById(R.id.tv_line_code_badge);
                TextView tvLineName = itemView.findViewById(R.id.tv_line);
                TextView tvMessage = itemView.findViewById(R.id.tv_message);
                TextView tvStatus = itemView.findViewById(R.id.tv_status);
                ImageView ivIcon = itemView.findViewById(R.id.iv_status_icon);

                int cInt = Color.parseColor(lineColor);
                vColorBar.setBackgroundColor(cInt);
                vBadgeLayout.setBackgroundColor(cInt);
                tvLineCode.setText(lineCode);
                tvLineName.setText(lineNameTc);
                tvMessage.setText(displayMessage);

                // 狀態 icon 邏輯
                if (status.equalsIgnoreCase("green")) {
                    tvStatus.setText("服務正常");
                    ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                    ivIcon.setColorFilter(Color.parseColor("#49AD7F"));
                } else if (status.equalsIgnoreCase("yellow")) {
                    tvStatus.setText("服務延誤");
                    ivIcon.setImageResource(R.drawable.outline_exclamation_24); // 假設有這個 icon
                    ivIcon.setColorFilter(Color.parseColor("#FFA500"));
                } else {
                    tvStatus.setText("服務受阻");
                    ivIcon.setColorFilter(Color.RED);
                }

                statusContainer.addView(itemView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}