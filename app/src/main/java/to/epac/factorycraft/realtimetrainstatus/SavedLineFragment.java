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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SavedLineFragment extends Fragment {

    private SharedPreferences prefs;
    private static final String KEY_SAVED_LINES = "saved_lines_csv";

    EditAdapter editAdapter;
    SearchAdapter searchAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RelativeLayout layoutEmpty;
    private NestedScrollView vSavedLines;
    private LinearLayout statusContainer;
    private MaterialButton btnRegister;
    private TextView btnEdit;

    private boolean isFetching = false;
    private final ExecutorService crossCheckExecutor = Executors.newFixedThreadPool(10);
    private HRConfig hrConf;

    Runnable updateSelectAll;

    private List<HRConfig.Line> filteredLines = new ArrayList<>();
    List<String> currentSaved = new ArrayList<>();
    Set<String> selectedForDelete = new HashSet<>();

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

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        int currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        int startTime = 1 * 60 + 30; // 01:30
        int endTime = 5 * 60;        // 05:00

        boolean isMaintenanceTime = currentTimeInMinutes >= startTime && currentTimeInMinutes < endTime;

        LinearLayout nthMessage = view.findViewById(R.id.layout_nth);
        if (isMaintenanceTime)
            nthMessage.setVisibility(View.VISIBLE);
        else
            nthMessage.setVisibility(View.GONE);

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


    private List<String> getSavedLinesList() {
        String savedCsv = prefs.getString(KEY_SAVED_LINES, "");
        if (savedCsv.isEmpty()) return new ArrayList<>();

        return new ArrayList<>(Arrays.asList(savedCsv.split(",")));
    }

    private void saveLinesList(List<String> list) {
        prefs.edit().putString(KEY_SAVED_LINES, String.join(",", list)).apply();
    }


    private void showEditBottomSheet() {
        BottomSheetDialog editDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_edit, null);
        editDialog.setContentView(sheetView);

        View parent = (View) sheetView.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);

        int heightInPx = (int) (200 * getResources().getDisplayMetrics().density);
        behavior.setPeekHeight(heightInPx);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        ViewGroup.LayoutParams layoutParams = parent.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        parent.setLayoutParams(layoutParams);


        currentSaved.clear();
        currentSaved.addAll(getSavedLinesList());
        selectedForDelete.clear();


        TextView btnAddLine = sheetView.findViewById(R.id.btn_add_line);
        btnAddLine.setOnClickListener(v -> {
            showSearchBottomSheet(currentSaved, updateSelectAll);
        });
        ImageView rbSelectAll = sheetView.findViewById(R.id.rb_select_all);
        TextView tvSelectAll = sheetView.findViewById(R.id.tv_select_all);
        View.OnClickListener selectAllListener = v -> {
            boolean isCurrentlyAllSelected = (selectedForDelete.size() == currentSaved.size() && !currentSaved.isEmpty());
            selectedForDelete.clear();
            if (!isCurrentlyAllSelected) {
                selectedForDelete.addAll(currentSaved);
            }
            updateSelectAll.run();
        };
        rbSelectAll.setOnClickListener(selectAllListener);
        tvSelectAll.setOnClickListener(selectAllListener);

        MaterialButton btnDelete = sheetView.findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> {
            currentSaved.removeAll(selectedForDelete);
            saveLinesList(currentSaved);
            selectedForDelete.clear();
            updateSelectAll.run();
        });

        RecyclerView rvLines = sheetView.findViewById(R.id.rv_saved_lines);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvLines.getContext(), LinearLayoutManager.VERTICAL);
        rvLines.addItemDecoration(dividerItemDecoration);
        rvLines.setLayoutManager(new LinearLayoutManager(getContext()));

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            editDialog.cancel();
        });

        updateSelectAll = () -> {
            if (editAdapter != null) editAdapter.notifyDataSetChanged();

            boolean isAllSelected = !currentSaved.isEmpty() && selectedForDelete.size() == currentSaved.size();
            tvSelectAll.setText(isAllSelected ? "取消全選" : "全選");

            int colorOnSurface = Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOnSurface);
            int green = ContextCompat.getColor(requireContext(), R.color.button_green);

            rbSelectAll.setImageResource(isAllSelected ? R.drawable.baseline_check_circle_outline_24 : R.drawable.outline_circle_24);
            rbSelectAll.setImageTintList(ColorStateList.valueOf(isAllSelected ? green : colorOnSurface));

            btnDelete.setEnabled(!selectedForDelete.isEmpty());
            btnDelete.setBackgroundColor(selectedForDelete.isEmpty() ? Color.parseColor("#2C2C2C") : green);
        };

        editAdapter = new EditAdapter(currentSaved, selectedForDelete, hrConf, updateSelectAll);
        rvLines.setAdapter(editAdapter);


        // Drag item to reorder
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                int fromPos = from.getAdapterPosition();
                int toPos = to.getAdapterPosition();
                Collections.swap(currentSaved, fromPos, toPos);
                editAdapter.notifyItemMoved(fromPos, toPos);
                saveLinesList(currentSaved);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        touchHelper.attachToRecyclerView(rvLines);


        // Refresh UI, fetch data if not empty, show empty layout if empty
        editDialog.setOnDismissListener(dialog -> {
            refreshUIState();
        });
        editDialog.show();

        // Initialize select all state
        updateSelectAll.run();
    }

    private void showSearchBottomSheet(List<String> currentSaved, Runnable onDataChanged) {
        BottomSheetDialog searchDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_search, null);
        searchDialog.setContentView(sheetView);

        View parent = (View) sheetView.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);

        int heightInPx = (int) (200 * getResources().getDisplayMetrics().density);
        behavior.setPeekHeight(heightInPx);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        ViewGroup.LayoutParams layoutParams = parent.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        parent.setLayoutParams(layoutParams);


        List<HRConfig.Line> allLines = new ArrayList<>();
        for (HRConfig.Line line : hrConf.getLineMap().values()) {
            if (!"HSR".equalsIgnoreCase(line.alias))
                allLines.add(line);
        }
        filteredLines.clear();
        filteredLines.addAll(allLines);


        RecyclerView rvResults = sheetView.findViewById(R.id.rv_search_results);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvResults.getContext(), LinearLayoutManager.VERTICAL);
        rvResults.addItemDecoration(dividerItemDecoration);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        searchAdapter = new SearchAdapter(filteredLines, line -> {
            if (!currentSaved.contains(line.alias)) {
                currentSaved.add(line.alias);
                saveLinesList(currentSaved);
                onDataChanged.run();
            }
            searchDialog.dismiss();
        });
        rvResults.setAdapter(searchAdapter);

        MaterialButton btnClose = sheetView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            searchDialog.cancel();
        });

        EditText etSearch = sheetView.findViewById(R.id.et_line_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                filteredLines.clear();

                if (query.isEmpty()) {
                    filteredLines.addAll(allLines);
                } else {
                    for (HRConfig.Line line : allLines) {
                        if (line.name.toLowerCase().contains(query) ||
                                line.nameEN.toLowerCase().contains(query) ||
                                line.alias.toLowerCase().contains(query)) {
                            filteredLines.add(line);
                        }
                    }
                }
                searchAdapter.notifyDataSetChanged();
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


    private static class EditAdapter extends RecyclerView.Adapter<EditAdapter.ViewHolder> {
        private final List<String> data;
        private final Set<String> selectedSet;
        private final HRConfig hrConf;
        private final Runnable onUpdateUI;

        public EditAdapter(List<String> data, Set<String> selectedSet, HRConfig hrConf, Runnable onUpdateUI) {
            this.data = data;
            this.selectedSet = selectedSet;
            this.hrConf = hrConf;
            this.onUpdateUI = onUpdateUI;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_line, parent, false));

        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String lineCode = data.get(position);
            HRConfig.Line line = hrConf.getLineByAlias(lineCode);
            Context context = holder.itemView.getContext();

            holder.tvLineName.setText(line.name);
            holder.tvLineBadge.setText(line.alias);
            holder.lineColorBadge.setBackgroundColor(Color.parseColor("#" + line.color));

            int colorOnSurface = Utils.getThemeColor(context, com.google.android.material.R.attr.colorOnSurface);
            boolean isSelected = selectedSet.contains(lineCode);

            holder.cbSelect.setImageResource(isSelected ? R.drawable.baseline_check_circle_outline_24 : R.drawable.outline_circle_24);
            holder.cbSelect.setImageTintList(ColorStateList.valueOf(isSelected ? ContextCompat.getColor(context, R.color.button_green) : colorOnSurface));


            holder.itemView.setOnClickListener(v -> {
                if (isSelected) selectedSet.remove(lineCode);
                else selectedSet.add(lineCode);
                onUpdateUI.run();
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView cbSelect;
            View lineColorBadge;
            TextView tvLineBadge;
            TextView tvLineName;
            ImageView ivIndicator;

            private ViewHolder(View v) {
                super(v);
                cbSelect = v.findViewById(R.id.cb_select);
                lineColorBadge = v.findViewById(R.id.line_color_badge);
                tvLineBadge = v.findViewById(R.id.tv_line_code_badge);
                tvLineName = v.findViewById(R.id.tv_line_name);
                ivIndicator = v.findViewById(R.id.iv_indicator);
            }
        }
    }

    private static class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private final List<HRConfig.Line> filteredList;
        private final OnLineSelectedListener listener;

        public interface OnLineSelectedListener {
            void onSelected(HRConfig.Line line);
        }

        public SearchAdapter(List<HRConfig.Line> filteredList, OnLineSelectedListener listener) {
            this.filteredList = filteredList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_line, parent, false));

        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HRConfig.Line line = filteredList.get(position);

            holder.tvLineName.setText(line.name);
            holder.tvLineBadge.setText(line.alias);
            holder.lineColorBadge.setBackgroundColor(Color.parseColor("#" + line.color));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onSelected(line);
            });
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView cbSelect;
            View lineColorBadge;
            TextView tvLineBadge;
            TextView tvLineName;
            ImageView ivIndicator;

            private ViewHolder(View v) {
                super(v);
                cbSelect = v.findViewById(R.id.cb_select);
                lineColorBadge = v.findViewById(R.id.line_color_badge);
                tvLineBadge = v.findViewById(R.id.tv_line_code_badge);
                tvLineName = v.findViewById(R.id.tv_line_name);
                ivIndicator = v.findViewById(R.id.iv_indicator);

                cbSelect.setVisibility(View.GONE);
                ivIndicator.setBackgroundResource(R.drawable.baseline_keyboard_arrow_right_24);
            }
        }
    }
}