package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;

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
    public static final int TYPE_LINE = 2;

    private HistoryManager historyManager;
    private final List<Object> historyList = new ArrayList<>();

    private int currentType;
    private Set<Integer> selectedIds = new HashSet<>();

    private MaterialButton btnClose;
    private MaterialRadioButton rbSelectAll;
    private TextView tvSelectAll;
    private RecyclerView rvDeleteHistory;
    private DeleteAdapter rvAdapter;
    private MaterialButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_delete);

        currentType = getIntent().getIntExtra(EXTRA_HISTORY_TYPE, TYPE_ROUTE);
        historyManager = HistoryManager.getInstance(this);

        btnClose = findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            finish();
        });

        rbSelectAll = findViewById(R.id.rb_select_all);
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
        rbSelectAll.setOnClickListener(selectAllListener);

        btnDelete.setOnClickListener(v -> {
            List<Integer> idsToDelete = new ArrayList<>(selectedIds);
            switch (currentType) {
                case TYPE_ROUTE:
                    historyManager.deleteRoutes(idsToDelete, this::onDeleteComplete);
                    break;
                case TYPE_LINE:
                    historyManager.deleteLines(idsToDelete, this::onDeleteComplete);
                    break;
                default:
                    historyManager.deleteStations(idsToDelete, this::onDeleteComplete);
                    break;
            }
        });

        loadData();
    }

    private void onDeleteComplete() {
        selectedIds.clear();
        loadData();
    }

    private int getIdFromItem(Object item) {
        if (item instanceof SearchHistory)
            return ((SearchHistory) item).routeId;
        if (item instanceof StationHistory)
            return ((StationHistory) item).stationId;
        if (item instanceof LineHistory)
            return ((LineHistory) item).lineId;
        return -1;
    }

    private void loadData() {
        if (currentType == TYPE_ROUTE) {
            historyManager.loadSearchHistory(data -> {
                historyList.clear();
                historyList.addAll(data);
                updateUI();
            });
        } else if (currentType == TYPE_LINE) {
            historyManager.loadLineHistory(data -> {
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
        rbSelectAll.setChecked(isAllSelected);
        tvSelectAll.setText(isAllSelected ? "取消全選" : "全選");

        btnDelete.setEnabled(!selectedIds.isEmpty());
        btnDelete.setBackgroundColor(selectedIds.isEmpty() ? Color.parseColor("#2C2C2C") : Color.parseColor("#4CAF50"));
    }

    private class DeleteAdapter extends RecyclerView.Adapter<DeleteAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_delete, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object item = historyList.get(position);
            int id = getIdFromItem(item);

            if (item instanceof SearchHistory) {
                SearchHistory sh = (SearchHistory) item;

                holder.tvName.setText(sh.originName + " → " + sh.destinationName);
                formatTime(holder.tvTime, sh.timestamp);
            } else if (item instanceof LineHistory) {
                LineHistory lh = (LineHistory) item;

                holder.tvName.setText(lh.lineName);
                holder.tvTime.setVisibility(View.GONE);
            } else if (item instanceof StationHistory) {
                StationHistory st = (StationHistory) item;

                holder.tvName.setText(st.stationName);
                holder.tvTime.setVisibility(View.GONE);
            }

            holder.rbItem.setChecked(selectedIds.contains(id));

            View.OnClickListener toggleListener = v -> {
                if (selectedIds.contains(id)) {
                    selectedIds.remove(id);
                } else {
                    selectedIds.add(id);
                }
                updateUI();
            };
            holder.itemView.setOnClickListener(toggleListener);
            holder.rbItem.setOnClickListener(toggleListener);
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

        private class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvTime;
            MaterialRadioButton rbItem;

            private ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_name);
                tvTime = v.findViewById(R.id.tv_time);
                rbItem = v.findViewById(R.id.rb_item);
            }
        }
    }
}