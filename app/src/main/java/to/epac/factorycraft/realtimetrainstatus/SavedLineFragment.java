package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

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
    private static final String KEY_SAVED_LINES = "saved_lines_csv";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RelativeLayout layoutEmpty;
    private NestedScrollView vSavedLines;
    private LinearLayout statusContainer;
    private MaterialButton btnRegister;
    private TextView btnEdit;

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
        layoutEmpty = view.findViewById(R.id.layout_empty);
        vSavedLines = view.findViewById(R.id.v_saved_lines);
        statusContainer = view.findViewById(R.id.status_container);
        btnRegister = view.findViewById(R.id.btn_register);
        btnEdit = view.findViewById(R.id.btn_edit);

        View.OnClickListener openEditSheet = v -> {
            showEditBottomSheet();
        };
        btnRegister.setOnClickListener(openEditSheet);
        btnEdit.setOnClickListener(openEditSheet);

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
        ImageView rbSelectAll = sheetView.findViewById(R.id.rb_select_all);
        TextView tvSelectAll = sheetView.findViewById(R.id.tv_select_all);
        MaterialButton btnDelete = sheetView.findViewById(R.id.btn_delete);
        RecyclerView rvLines = sheetView.findViewById(R.id.rv_saved_lines);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvLines.getContext(), LinearLayoutManager.VERTICAL);
        rvLines.addItemDecoration(dividerItemDecoration);
        rvLines.setLayoutManager(new LinearLayoutManager(getContext()));
        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);

        btnClose.setOnClickListener(v -> {
            editDialog.cancel();
        });

        List<String> currentSaved = getSavedLinesList();
        Set<String> selectedForDelete = new HashSet<>();

        Runnable updateUIState = () -> {
            if (rvLines.getAdapter() != null) rvLines.getAdapter().notifyDataSetChanged();

            boolean isAllSelected = !currentSaved.isEmpty() && selectedForDelete.size() == currentSaved.size();
            tvSelectAll.setText(isAllSelected ? "取消全選" : "全選");
            int colorOnSurface = Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOnSurface);

            rbSelectAll.setImageResource(isAllSelected ? R.drawable.baseline_check_circle_outline_24 : R.drawable.outline_circle_24);
            rbSelectAll.setImageTintList(ColorStateList.valueOf(isAllSelected ? ContextCompat.getColor(requireContext(), R.color.button_green) : colorOnSurface));


            btnDelete.setEnabled(!selectedForDelete.isEmpty());
            int btnColor = selectedForDelete.isEmpty() ? Color.parseColor("#2C2C2C") : ContextCompat.getColor(requireContext(), R.color.button_green);
            btnDelete.setBackgroundColor(btnColor);
        };

        btnAddLine.setOnClickListener(v -> {
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

                ImageView rbItem = holder.itemView.findViewById(R.id.cb_select);
                TextView tvCode = holder.itemView.findViewById(R.id.tv_line_code_badge);
                View badgeBg = holder.itemView.findViewById(R.id.line_color_badge);
                TextView tvName = holder.itemView.findViewById(R.id.tv_line_name);
                ImageView ivDragHandle = holder.itemView.findViewById(R.id.iv_drag_handle);
                ivDragHandle.setVisibility(View.VISIBLE);

                tvName.setText(line.name);
                tvCode.setText(line.alias);
                badgeBg.setBackgroundColor(Color.parseColor("#" + line.color));

                int colorOnSurface = Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOnSurface);

                rbItem.setImageResource(selectedForDelete.contains(lineCode) ? R.drawable.baseline_check_circle_outline_24 : R.drawable.outline_circle_24);
                rbItem.setImageTintList(ColorStateList.valueOf(selectedForDelete.contains(lineCode) ? ContextCompat.getColor(requireContext(), R.color.button_green) : colorOnSurface));


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
                ImageView cbSelect = holder.itemView.findViewById(R.id.cb_select);

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


    private void refreshUIState() {
        String savedCsv = prefs.getString(KEY_SAVED_LINES, "");
        if (savedCsv.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            vSavedLines.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(false);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            vSavedLines.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setEnabled(true);
            fetchSavedLinesData();
        }
    }

    private List<String> getSavedLinesList() {
        String savedCsv = prefs.getString(KEY_SAVED_LINES, "");
        if (savedCsv.isEmpty()) return new ArrayList<>();

        return new ArrayList<>(Arrays.asList(savedCsv.split(",")));
    }

    private void saveLinesList(List<String> list) {
        prefs.edit().putString(KEY_SAVED_LINES, String.join(",", list)).apply();
    }

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

                    JSONArray targetLines = new JSONArray();
                    for (int i = 0; i < linesArray.length(); i++) {
                        JSONObject obj = linesArray.getJSONObject(i);
                        if (savedLines.contains(obj.getString("line_code"))) {
                            targetLines.put(obj);
                        }
                    }

                    CountDownLatch latch = new CountDownLatch(targetLines.length());
                    for (int i = 0; i < targetLines.length(); i++) {
                        final JSONObject tLine = targetLines.getJSONObject(i);
                        crossCheckExecutor.execute(() -> {
                            HttpURLConnection conn = null;
                            try {
                                String lineCode = tLine.getString("line_code").toUpperCase();
                                String sta = MainActivity.NEXTTRAIN_CHECK_STATIONS.get(lineCode);

                                URL url2 = new URL("https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php?line=" + lineCode + "&sta=" + sta);

                                conn = (HttpURLConnection) url2.openConnection();
                                conn.setConnectTimeout(3000);
                                conn.setReadTimeout(3000);

                                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                                    StringBuilder sb = new StringBuilder();
                                    String l;
                                    while ((l = r.readLine()) != null) sb.append(l);

                                    JSONObject rtData = new JSONObject(sb.toString());

                                    boolean isApiDelay = rtData.optString("isdelay", "N").equals("Y");
                                    boolean isTimeBlank = rtData.optString("sys_time", "").equals("-") ||
                                            rtData.optString("curr_time", "").equals("-");

                                    if (isApiDelay || isTimeBlank) {
                                        String currentStatus = tLine.getString("status").toLowerCase();
                                        if (currentStatus.equals("green")) {
                                            tLine.put("status", "yellow");
                                            String original = tLine.optString("messages", "");
                                            String nexttrain = "列車服務可能受阻，詳情請留意官方發出的最新車務資訊。";
                                            tLine.put("messages", !original.isEmpty() ? original : nexttrain);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                            } finally {
                                if (conn != null) conn.disconnect();
                                latch.countDown();
                            }
                        });
                    }

                    latch.await(6, TimeUnit.SECONDS);

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

                    new Handler(Looper.getMainLooper()).post(() -> updateUI(sortedLines));
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


    private void updateUI(JSONArray lines) {
        try {
            statusContainer.removeAllViews();
            for (int i = 0; i < lines.length(); i++) {
                JSONObject lineObj = lines.getJSONObject(i);

                String lineCode = lineObj.getString("line_code");
                String lineNameTc = lineObj.getString("line_name_tc");
                String lineColor = lineObj.getString("line_color");
                String status = lineObj.getString("status");
                String lineSection = "全綫";

                String displayMessage = "沒有任何延誤";
                Object messagesObj = lineObj.opt("messages");

                if (messagesObj instanceof JSONObject) {
                    JSONObject msgObj = ((JSONObject) messagesObj).optJSONObject("message");
                    if (msgObj != null) {
                        String title = msgObj.optString("title_tc", "");
                        String cause = msgObj.optString("cause_tc", "");
                        displayMessage = !title.isEmpty() ? title : cause;

                        JSONObject affectedAreas = msgObj.optJSONObject("affected_areas");
                        if (affectedAreas != null) {
                            JSONObject affectedAreaObj = affectedAreas.optJSONObject("affected_area");

                            if (affectedAreaObj != null) {
                                String stationFr = affectedAreaObj.optString("station_code_fr");
                                String stationTo = affectedAreaObj.optString("station_code_to");

                                if (!stationFr.isEmpty() && !stationTo.isEmpty()) {
                                    lineSection = hrConf.getStationName(Integer.parseInt(stationFr)) + "~"
                                            + hrConf.getStationName(Integer.parseInt(stationTo));
                                }
                            }
                        }
                    }
                } else if (messagesObj instanceof String) {
                    String msgStr = (String) messagesObj;
                    if (!msgStr.isEmpty()) {
                        displayMessage = msgStr;
                    }
                }

                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_line_status, statusContainer, false);

                View vColorBar = itemView.findViewById(R.id.v_line_color_bar);
                View vBadgeLayout = itemView.findViewById(R.id.line_color_badge);
                TextView tvLineCode = itemView.findViewById(R.id.tv_line_code_badge);
                TextView tvLineName = itemView.findViewById(R.id.tv_line);
                TextView tvLineSection = itemView.findViewById(R.id.tv_line_section);
                TextView tvStatus = itemView.findViewById(R.id.tv_status);
                TextView tvMessage = itemView.findViewById(R.id.tv_message);
                ImageView ivIcon = itemView.findViewById(R.id.iv_status_icon);

                int colorInt = Color.parseColor(lineColor);
                vColorBar.setBackgroundColor(colorInt);
                vBadgeLayout.setBackgroundColor(colorInt);
                tvLineCode.setText(lineCode);
                tvLineName.setText(lineNameTc);
                tvLineSection.setText(lineSection);
                tvMessage.setText(displayMessage);

                updateStatusUI(status, tvStatus, ivIcon);

                itemView.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(getActivity(), SavedLineActivity.class);
                    intent.putExtra("line_code", lineCode);
                    intent.putExtra("line_name_tc", lineNameTc);
                    intent.putExtra("line_color", lineColor);
                    intent.putExtra("status", status);
                    intent.putExtra("messages", messagesObj.toString());
                    startActivity(intent);
                });

                statusContainer.addView(itemView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatusUI(String status, TextView tvStatus, ImageView ivIcon) {
        switch (status.toLowerCase()) {
            case "green":
                tvStatus.setText("服務正常");
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.parseColor("#49AD7F"));
                break;
            case "yellow":
                tvStatus.setText("服務延誤");
                ivIcon.setImageResource(R.drawable.outline_exclamation_24);
                ivIcon.setColorFilter(Color.parseColor("#FFA500"));
                break;
            case "red":
                tvStatus.setText("服務受阻");
                ivIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                ivIcon.setColorFilter(Color.parseColor("#FF0000"));
                break;
            case "pink":
                tvStatus.setText("服務延誤或受阻");
                ivIcon.setImageResource(R.drawable.baseline_warning_24);
                ivIcon.setColorFilter(Color.parseColor("#FF69B4"));
                break;
            case "typhoon":
                tvStatus.setText("熱帶氣旋警告信號生效");
                ivIcon.setImageResource(R.drawable.baseline_storm_24);
                ivIcon.setColorFilter(Color.parseColor("#00BCD4"));
                break;
            case "grey":
                tvStatus.setText("非服務時間");
                ivIcon.setImageResource(R.drawable.baseline_trip_origin_24);
                ivIcon.setColorFilter(Color.GRAY);
                break;
        }
    }
}