package to.epac.factorycraft.realtimetrainstatus;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class RecentViewedFragment extends Fragment {

    private RecyclerView rvHistory;
    private RecentViewedAdapter adapter;
    private List<SearchHistory> historyList = new ArrayList<>();
    private AppDatabase db;
    private TextView tvDelete;
    private View layoutEmpty;
    private LinearLayout layoutHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent_viewed, container, false);

        db = Room.databaseBuilder(requireContext(), AppDatabase.class, "search_history_db").build();

        tvDelete = view.findViewById(R.id.tv_delete);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        layoutHistory = view.findViewById(R.id.layout_history);

        TextView tvDelete = view.findViewById(R.id.tv_delete);
        tvDelete.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), HistoryDeleteActivity.class);
            startActivity(intent);
        });

        rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new RecentViewedAdapter(historyList);
        rvHistory.setAdapter(adapter);

        loadHistoryData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        loadHistoryData();
    }

    private void loadHistoryData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SearchHistory> data = db.searchHistoryDao().getRecentHistories();

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    historyList.clear();
                    historyList.addAll(data);

                    updateUI(data.isEmpty());
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            layoutHistory.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            layoutHistory.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }


    private static class RecentViewedAdapter extends RecyclerView.Adapter<RecentViewedAdapter.RecentViewedHolder> {
        private List<SearchHistory> list;

        public RecentViewedAdapter(List<SearchHistory> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public RecentViewedHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_viewed, parent, false);
            return new RecentViewedHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecentViewedHolder holder, int position) {
            SearchHistory item = list.get(position);

            holder.tvRouteName.setText(item.originName + " → " + item.destinationName);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            calendar.setTimeInMillis(item.timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("M月d日(E) HH:mm 出發", Locale.TRADITIONAL_CHINESE);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            holder.tvDepartureTime.setText(sdf.format(calendar.getTime()));


            holder.btnGo.setOnClickListener(v -> {
                // 處理 GO 按鈕點擊，通常是重新導航到路線結果頁
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private static class RecentViewedHolder extends RecyclerView.ViewHolder {
            TextView tvRouteName, tvDepartureTime, btnGo;

            private RecentViewedHolder(View itemView) {
                super(itemView);
                tvRouteName = itemView.findViewById(R.id.tv_name);
                tvDepartureTime = itemView.findViewById(R.id.tv_time);
                btnGo = itemView.findViewById(R.id.btn_go);
            }
        }
    }
}