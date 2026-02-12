package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class HistoryDeleteActivity extends AppCompatActivity {

    private AppDatabase db;

    private List<SearchHistory> historyList = new ArrayList<>();
    private Set<Integer> selectedIds = new HashSet<>();

    private CheckBox cbSelectAll;
    private TextView tvSelectAll;

    private RecyclerView rvDeleteHistory;
    private DeleteAdapter rvAdapter;

    private Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_history);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "search_history_db").build();

        // Finish activity if close
        findViewById(R.id.btn_close).setOnClickListener(v -> {
            finish();
        });

        cbSelectAll = findViewById(R.id.cb_select_all);
        tvSelectAll = findViewById(R.id.tv_select_all);

        rvDeleteHistory = findViewById(R.id.rv_delete_history);
        rvDeleteHistory.setLayoutManager(new LinearLayoutManager(this));
        rvAdapter = new DeleteAdapter();
        rvDeleteHistory.setAdapter(rvAdapter);

        btnDelete = findViewById(R.id.btn_delete);

        // Update selected history on click
        View.OnClickListener selectAllListener = v -> {
            if (v.getId() == R.id.tv_select_all)
                cbSelectAll.setChecked(!cbSelectAll.isChecked());

            selectedIds.clear();
            if (cbSelectAll.isChecked()) {
                tvSelectAll.setText("取消全選");
                for (SearchHistory h : historyList) {
                    selectedIds.add(h.id);
                }
            } else {
                tvSelectAll.setText("全選");
            }
            updateUI();
        };
        tvSelectAll.setOnClickListener(selectAllListener);
        cbSelectAll.setOnClickListener(selectAllListener);

        // Execute deletion
        btnDelete.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                for (Integer id : selectedIds) {
                    db.searchHistoryDao().deleteById(id);
                }

                loadData();
                selectedIds.clear();
                runOnUiThread(this::updateUI);
            });
        });


        loadData();
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SearchHistory> data = db.searchHistoryDao().getRecentHistories();
            runOnUiThread(() -> {
                historyList.clear();
                historyList.addAll(data);
                rvAdapter.notifyDataSetChanged();
                updateUI();
            });
        });
    }

    private void updateUI() {
        rvAdapter.notifyDataSetChanged();
        btnDelete.setEnabled(!selectedIds.isEmpty());
        btnDelete.setBackgroundColor(selectedIds.isEmpty() ? Color.parseColor("#2C2C2C") : Color.parseColor("#4CAF50"));
        cbSelectAll.setChecked(!historyList.isEmpty() && selectedIds.size() == historyList.size());
    }


    private class DeleteAdapter extends RecyclerView.Adapter<DeleteAdapter.DeleteHistoryHolder> {
        @NonNull
        @Override
        public DeleteHistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DeleteHistoryHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_delete, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull DeleteHistoryHolder holder, int position) {
            SearchHistory item = historyList.get(position);
            holder.tvName.setText(item.originName + " → " + item.destinationName);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(item.timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日(E) HH:mm 出發", Locale.TRADITIONAL_CHINESE);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            holder.tvTime.setText(sdf.format(cal.getTime()));

            holder.cbItem.setChecked(selectedIds.contains(item.id));


            View.OnClickListener toggleListener = v -> {
                if (selectedIds.contains(item.id)) {
                    selectedIds.remove(item.id);
                } else {
                    selectedIds.add(item.id);
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