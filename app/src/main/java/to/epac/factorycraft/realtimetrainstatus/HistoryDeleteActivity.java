package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class HistoryDeleteActivity extends AppCompatActivity {

    public static final String EXTRA_HISTORY_TYPE = "history_type";
    public static final int TYPE_ROUTE = 0;
    public static final int TYPE_STATION = 1;

    private HistoryManager historyManager;
    private final List<Object> historyList = new ArrayList<>();

    private int currentType;
    private Set<Integer> selectedIds = new HashSet<>();

    private ImageButton btnClose;
    private CheckBox cbSelectAll;
    private TextView tvSelectAll;
    private RecyclerView rvDeleteHistory;
    private DeleteAdapter rvAdapter;
    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_delete);

        currentType = getIntent().getIntExtra(EXTRA_HISTORY_TYPE, TYPE_ROUTE);
        historyManager = HistoryManager.getInstance(this);

        btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> finish());

        cbSelectAll = findViewById(R.id.cb_select_all);
        tvSelectAll = findViewById(R.id.tv_select_all);
        btnDelete = findViewById(R.id.btn_delete);
        rvDeleteHistory = findViewById(R.id.rv_delete_history);
        rvDeleteHistory.setLayoutManager(new LinearLayoutManager(this));

        rvAdapter = new DeleteAdapter();
        rvDeleteHistory.setAdapter(rvAdapter);

        View.OnClickListener selectAllListener = v -> {
            boolean isCurrentlyAllSelected = (selectedIds.size() == historyList.size() && !historyList.isEmpty());

            selectedIds.clear();
            if (!isCurrentlyAllSelected) {
                for (Object item : historyList) {
                    selectedIds.add(getIdFromItem(item));
                }
            }

            updateUI();
        };
        tvSelectAll.setOnClickListener(selectAllListener);
        cbSelectAll.setOnClickListener(selectAllListener);

        btnDelete.setOnClickListener(v -> {
            List<Integer> idsToDelete = new ArrayList<>(selectedIds);
            if (currentType == TYPE_ROUTE) {
                historyManager.deleteRoutes(idsToDelete, this::onDeleteComplete);
            } else {
                historyManager.deleteStations(idsToDelete, this::onDeleteComplete);
            }
        });

        loadData();
    }

    private void onDeleteComplete() {
        selectedIds.clear();
        loadData();
    }

    private int getIdFromItem(Object item) {
        if (item instanceof SearchHistory) return ((SearchHistory) item).routeId;
        if (item instanceof StationHistory) return ((StationHistory) item).stationId;
        return -1;
    }

    private void loadData() {
        if (currentType == TYPE_ROUTE) {
            historyManager.loadSearchHistory(data -> {
                historyList.clear();
                historyList.addAll(data);
                updateUI();
            });
        } else {
            historyManager.loadStationHistory(data -> {
                historyList.clear();
                historyList.addAll(data);
                updateUI();
            });
        }
    }

    private void updateUI() {
        rvAdapter.notifyDataSetChanged();
        boolean isAllSelected = !historyList.isEmpty() && selectedIds.size() == historyList.size();
        cbSelectAll.setChecked(isAllSelected);

        tvSelectAll.setText(isAllSelected ? "取消全選" : "全選");

        btnDelete.setEnabled(!selectedIds.isEmpty());
        btnDelete.setBackgroundColor(selectedIds.isEmpty() ?
                Color.parseColor("#2C2C2C") : Color.parseColor("#4CAF50"));
    }

    private class DeleteAdapter extends RecyclerView.Adapter<DeleteAdapter.DeleteHistoryHolder> {
        @NonNull
        @Override
        public DeleteHistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DeleteHistoryHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_delete, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull DeleteHistoryHolder holder, int position) {
            Object item = historyList.get(position);
            int id = getIdFromItem(item);

            if (item instanceof SearchHistory) {
                SearchHistory sh = (SearchHistory) item;
                holder.tvName.setText(sh.originName + " → " + sh.destinationName);
            } else {
                StationHistory st = (StationHistory) item;
                holder.tvName.setText(st.stationName);
                holder.tvTime.setVisibility(View.GONE);
                formatTime(holder.tvTime, st.timestamp);
            }

            holder.cbItem.setChecked(selectedIds.contains(id));

            View.OnClickListener toggleListener = v -> {
                if (selectedIds.contains(id)) {
                    selectedIds.remove(id);
                } else {
                    selectedIds.add(id);
                }
                updateUI();
            };
            holder.itemView.setOnClickListener(toggleListener);
            holder.cbItem.setOnClickListener(toggleListener);
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        private void formatTime(TextView tv, long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日(E) HH:mm", Locale.TRADITIONAL_CHINESE);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            tv.setText(sdf.format(cal.getTime()));
        }

        private class DeleteHistoryHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvTime;
            CheckBox cbItem;

            private DeleteHistoryHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_name);
                tvTime = v.findViewById(R.id.tv_time);
                cbItem = v.findViewById(R.id.cb_item);
            }
        }
    }
}