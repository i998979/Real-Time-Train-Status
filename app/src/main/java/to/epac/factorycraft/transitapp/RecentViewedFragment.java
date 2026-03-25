package to.epac.factorycraft.transitapp;

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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RecentViewedFragment extends Fragment {

    private List<SearchHistory> historyList = new ArrayList<>();

    private LinearLayout layoutHistory;
    private TextView tvDelete;
    private RecyclerView rvHistory;
    private RecentViewedAdapter adapter;
    private View layoutEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent_viewed, container, false);

        layoutHistory = view.findViewById(R.id.layout_history);

        tvDelete = view.findViewById(R.id.tv_delete);
        tvDelete.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), HistoryDeleteActivity.class);
            intent.putExtra("history_type", HistoryDeleteActivity.TYPE_ROUTE);

            startActivity(intent);
        });

        rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvHistory.getContext(), LinearLayoutManager.VERTICAL);
        rvHistory.addItemDecoration(dividerItemDecoration);
        adapter = new RecentViewedAdapter(historyList, item -> {
            RouteListFragment fragment = new RouteListFragment();

            Bundle args = new Bundle();
            args.putString(RouteSearchFragment.ORIGIN_ID, item.originId);
            args.putString(RouteSearchFragment.DEST_ID, item.destinationId);
            fragment.setArguments(args);

            if (isAdded()) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        rvHistory.setAdapter(adapter);
        layoutEmpty = view.findViewById(R.id.layout_empty);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        loadHistoryData();
    }

    private void loadHistoryData() {
        HistoryManager.getInstance(requireContext()).loadSearchHistory(data -> {
            if (isAdded()) {
                historyList.clear();
                historyList.addAll(data);

                if (data.isEmpty()) {
                    layoutHistory.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    layoutHistory.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);
                }

                adapter.notifyDataSetChanged();
            }
        });
    }


    private static class RecentViewedAdapter extends RecyclerView.Adapter<RecentViewedAdapter.ViewHolder> {
        private List<SearchHistory> list;
        private OnGoClickListener goClickListener;

        public RecentViewedAdapter(List<SearchHistory> list, OnGoClickListener listener) {
            this.list = list;
            this.goClickListener = listener;
        }

        public interface OnGoClickListener {
            void onGo(SearchHistory item);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_viewed, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SearchHistory item = list.get(position);

            holder.tvFrom.setText(item.originName);
            holder.tvTo.setText(item.destinationName);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            calendar.setTimeInMillis(item.timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("M月d日(E) HH:mm 出發", Locale.TRADITIONAL_CHINESE);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            holder.tvTime.setText(sdf.format(calendar.getTime()));


            holder.goLayout.setOnClickListener(v -> {
                if (goClickListener != null) {
                    goClickListener.onGo(item);
                }
            });
            holder.btnGo.setOnClickListener(v -> {
                if (goClickListener != null) {
                    goClickListener.onGo(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout goLayout;
            TextView tvFrom, tvTo, tvTime, btnGo;

            private ViewHolder(View itemView) {
                super(itemView);
                goLayout = itemView.findViewById(R.id.layout_go);
                tvFrom = itemView.findViewById(R.id.tv_from);
                tvTo = itemView.findViewById(R.id.tv_to);
                tvTime = itemView.findViewById(R.id.tv_time);
                btnGo = itemView.findViewById(R.id.btn_go);
            }
        }
    }
}