package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        View.OnClickListener openEditSheet = v -> {
            showEditBottomSheet();
        };
        btnRegisterEmpty.setOnClickListener(openEditSheet);
        btnEditLines.setOnClickListener(openEditSheet);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchSavedLinesData();
        });

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
            swipeRefreshLayout.setEnabled(false);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            layoutSavedLines.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setEnabled(true);
            fetchSavedLinesData();
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
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_edit, null);
        editDialog.setContentView(sheetView);

        View parent = (View) sheetView.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);

        int heightInPx = (int) (200 * getResources().getDisplayMetrics().density);
        behavior.setPeekHeight(heightInPx);

        ViewGroup.LayoutParams layoutParams = parent.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

        parent.setLayoutParams(layoutParams);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        TextView btnAddLine = sheetView.findViewById(R.id.btn_add_line);
        MaterialRadioButton rbSelectAll = sheetView.findViewById(R.id.rb_select_all);
        TextView tvSelectAll = sheetView.findViewById(R.id.tv_select_all);
        MaterialButton btnDelete = sheetView.findViewById(R.id.btn_delete);
        RecyclerView rvLines = sheetView.findViewById(R.id.rv_saved_lines);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvLines.getContext(), LinearLayoutManager.VERTICAL);
        rvLines.addItemDecoration(dividerItemDecoration);
        rvLines.setLayoutManager(new LinearLayoutManager(getContext()));
        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);

        btnClose.setOnClickListener(v -> editDialog.cancel());

        List<String> currentSaved = getSavedLinesList();
        Set<String> selectedForDelete = new HashSet<>();

        Runnable updateUIState = () -> {
            if (rvLines.getAdapter() != null) rvLines.getAdapter().notifyDataSetChanged();

            boolean isAllSelected = !currentSaved.isEmpty() && selectedForDelete.size() == currentSaved.size();
            rbSelectAll.setChecked(isAllSelected);
            rbSelectAll.setText(isAllSelected ? "取消全選" : "全選");

            btnDelete.setEnabled(!selectedForDelete.isEmpty());
            int btnColor = selectedForDelete.isEmpty() ? Color.parseColor("#2C2C2C") : ContextCompat.getColor(requireContext(), R.color.button_green);
            btnDelete.setBackgroundColor(btnColor);
        };

        btnAddLine.setOnClickListener(v -> {
            editDialog.dismiss();
            showSearchBottomSheet();
        });

        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.item_edit_line, parent, false)) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                String lineCode = currentSaved.get(position);
                HRConfig.Line line = hrConf.getLineByAlias(lineCode);

                MaterialRadioButton rbItem = holder.itemView.findViewById(R.id.cb_select); // 這裡是 item 裡的 RadioButton
                TextView tvCode = holder.itemView.findViewById(R.id.tv_line_code_badge);
                View badgeBg = holder.itemView.findViewById(R.id.line_color_badge);
                TextView tvName = holder.itemView.findViewById(R.id.tv_line_name);

                if (line != null) {
                    tvName.setText(line.name);
                    tvCode.setText(line.alias);
                    badgeBg.setBackgroundColor(Color.parseColor("#" + line.color));
                }

                rbItem.setChecked(selectedForDelete.contains(lineCode));

                // 統一點擊邏輯
                View.OnClickListener toggleListener = v -> {
                    if (selectedForDelete.contains(lineCode)) {
                        selectedForDelete.remove(lineCode);
                    } else {
                        selectedForDelete.add(lineCode);
                    }
                    updateUIState.run();
                };

                holder.itemView.setOnClickListener(toggleListener);
                rbItem.setOnClickListener(toggleListener);

                // 處理拖曳 (ItemTouchHelper 會用到 holder)
            }

            @Override
            public int getItemCount() {
                return currentSaved.size();
            }
        };
        rvLines.setAdapter(adapter);

        View.OnClickListener selectAllListener = v -> {
            boolean isCurrentlyAllSelected = (selectedForDelete.size() == currentSaved.size() && !currentSaved.isEmpty());
            selectedForDelete.clear();
            if (!isCurrentlyAllSelected) {
                selectedForDelete.addAll(currentSaved);
            }
            updateUIState.run();
        };
        rbSelectAll.setOnClickListener(selectAllListener);
        tvSelectAll.setOnClickListener(selectAllListener);

        // --- 刪除事件 ---
        btnDelete.setOnClickListener(v -> {
            currentSaved.removeAll(selectedForDelete);
            saveLinesList(currentSaved);
            selectedForDelete.clear();
            updateUIState.run();
            if (currentSaved.isEmpty()) editDialog.dismiss();
        });

        // 拖曳排序邏輯維持不變，但在 onMove 時要小心處理 List 同步
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                int fromPos = from.getAdapterPosition();
                int toPos = to.getAdapterPosition();
                Collections.swap(currentSaved, fromPos, toPos);
                adapter.notifyItemMoved(fromPos, toPos);
                saveLinesList(currentSaved);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        touchHelper.attachToRecyclerView(rvLines);

        editDialog.setOnDismissListener(dialog -> refreshUIState());
        editDialog.show();

        // 初始化 UI 狀態
        updateUIState.run();
    }


    // ==========================================
    // P3: 搜尋與新增的 BottomSheet (整合 SearchActivity)
    // ==========================================
    private void showSearchBottomSheet() {
        BottomSheetDialog searchDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_search, null);
        searchDialog.setContentView(sheetView);

        View parent = (View) sheetView.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);

        int heightInPx = (int) (200 * getResources().getDisplayMetrics().density);
        behavior.setPeekHeight(heightInPx);

        ViewGroup.LayoutParams layoutParams = parent.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        ;
        parent.setLayoutParams(layoutParams);
        behavior.setSkipCollapsed(true);          // 跳過摺疊狀態，直接進入展開
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED); // 強制設定為展開狀態

        EditText etSearch = sheetView.findViewById(R.id.et_line_search);
        RecyclerView rvResults = sheetView.findViewById(R.id.rv_search_results);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvResults.getContext(), LinearLayoutManager.VERTICAL);
        rvResults.addItemDecoration(dividerItemDecoration);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            searchDialog.cancel();
        });

        // 初始化所有可用路線 (排除高鐵等)
        List<HRConfig.Line> allLines = new ArrayList<>();
        for (HRConfig.Line line : hrConf.getLineMap().values()) {
            if (!"HSR".equalsIgnoreCase(line.alias)) {
                allLines.add(line);
            }
        }

        // 用來顯示的過濾結果
        List<HRConfig.Line> filteredLines = new ArrayList<>(allLines);
        List<String> currentSaved = getSavedLinesList();

        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // 這裡可以重用 item_edit_line.xml，只需隱藏 Checkbox 和 Drag 圖標，顯示箭頭即可
                View v = getLayoutInflater().inflate(R.layout.item_edit_line, parent, false);
                v.findViewById(R.id.cb_select).setVisibility(View.GONE);
                v.findViewById(R.id.iv_drag_handle).setVisibility(View.GONE);
                ((ImageView) v.findViewById(R.id.iv_drag_handle)).setImageResource(R.drawable.baseline_keyboard_arrow_right_24);
                return new RecyclerView.ViewHolder(v) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                HRConfig.Line line = filteredLines.get(position);

                TextView tvCode = holder.itemView.findViewById(R.id.tv_line_code_badge);
                View badgeBg = holder.itemView.findViewById(R.id.line_color_badge);
                TextView tvName = holder.itemView.findViewById(R.id.tv_line_name);
                MaterialRadioButton cbSelect = holder.itemView.findViewById(R.id.cb_select);

                tvName.setText(line.name);
                tvCode.setText(line.alias);
                badgeBg.setBackgroundColor(Color.parseColor("#" + line.color)); // 依據你的 Config 設定顏色

                holder.itemView.setOnClickListener(v -> {
                    if (!currentSaved.contains(line.alias)) {
                        currentSaved.add(line.alias);
                        saveLinesList(currentSaved);
                        // HistoryManager.getInstance(getContext()).saveLineSearch(line.id, line.name); // 若需儲存搜尋歷史
                    }
                    searchDialog.dismiss();
                    showEditBottomSheet(); // 點擊後回到編輯畫面
                });
            }

            @Override
            public int getItemCount() {
                return filteredLines.size();
            }
        };
        rvResults.setAdapter(adapter);

        // 搜尋過濾邏輯 (完全移植自你的 SearchActivity)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                filteredLines.clear();

                if (query.isEmpty()) {
                    filteredLines.addAll(allLines); // 如果為空，顯示全部 (或你想顯示歷史紀錄)
                } else {
                    for (HRConfig.Line line : allLines) {
                        if (line.name.toLowerCase().contains(query) ||
                                (line.nameEN != null && line.nameEN.toLowerCase().contains(query)) ||
                                line.alias.toLowerCase().contains(query)) {
                            filteredLines.add(line);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchDialog.show();
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
                                String sta = MainActivity.NEXTTRAIN_CHECK_STATIONS.getOrDefault(code, "ADM"); // 防呆
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
                TextView tvLineSection = itemView.findViewById(R.id.tv_line_section);
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
                    tvLineSection.setVisibility(View.GONE);
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