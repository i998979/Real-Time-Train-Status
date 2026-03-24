package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeleteSavedStationsSheet extends BottomSheetDialogFragment {

    private SharedPreferences prefs;
    private HRConfig hrConfig;
    private List<String> favIds;
    private final Set<String> selectedIds = new HashSet<>();

    private TextView tvSelectAll;
    private MaterialButton btnDelete;
    private RecyclerView rv;
    private DeleteAdapter adapter;

    private Runnable onDismissListener;

    public void setOnDismissListener(Runnable listener) {
        this.onDismissListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            int displayHeight = getResources().getDisplayMetrics().heightPixels;
            int targetHeight = (int) (displayHeight * 0.9);
            behavior.setPeekHeight(targetHeight);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
            lp.height = targetHeight;
            bottomSheet.setLayoutParams(lp);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delete_saved_stations, container, false);

        prefs = requireActivity().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        hrConfig = HRConfig.getInstance(requireContext());

        String saved = prefs.getString(MainActivity.KEY_FAV_STATIONS, "");
        favIds = saved.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(saved.split(",")));

        tvSelectAll = view.findViewById(R.id.tv_select_all);
        btnDelete = view.findViewById(R.id.btn_delete);
        rv = view.findViewById(R.id.rv_saved_routes);

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());

        adapter = new DeleteAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        tvSelectAll.setOnClickListener(v -> {
            if (selectedIds.size() == favIds.size()) {
                selectedIds.clear();
            } else {
                selectedIds.addAll(favIds);
            }
            updateUIState();
        });

        btnDelete.setOnClickListener(v -> {
            favIds.removeAll(selectedIds);
            prefs.edit().putString(MainActivity.KEY_FAV_STATIONS, TextUtils.join(",", favIds)).apply();
            dismiss();
        });

        updateUIState();
        return view;
    }

    private void updateUIState() {
        adapter.notifyDataSetChanged();

        boolean isAllSelected = !favIds.isEmpty() && selectedIds.size() == favIds.size();
        tvSelectAll.setText(isAllSelected ? "取消全選" : "全選");

        int tintColor = ContextCompat.getColor(requireContext(), isAllSelected ? R.color.button_green : R.color.selector_radio_tint);
        TextViewCompat.setCompoundDrawableTintList(tvSelectAll, ColorStateList.valueOf(tintColor));

        btnDelete.setEnabled(!selectedIds.isEmpty());
        btnDelete.setBackgroundColor(selectedIds.isEmpty() ? Color.parseColor("#2C2C2C") : ContextCompat.getColor(requireContext(), R.color.button_green));
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) onDismissListener.run();
    }

    private class DeleteAdapter extends RecyclerView.Adapter<DeleteAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DeleteAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_delete, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String id = favIds.get(position);
            HRConfig.Station st = hrConfig.getStationById(Integer.parseInt(id));

            holder.tvName.setText(st.name);
            holder.tvTime.setVisibility(View.GONE);

            holder.rbItem.setChecked(selectedIds.contains(id));

            holder.itemView.setOnClickListener(v -> {
                if (selectedIds.contains(id)) selectedIds.remove(id);
                else selectedIds.add(id);
                updateUIState();
            });
        }

        @Override
        public int getItemCount() {
            return favIds.size();
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