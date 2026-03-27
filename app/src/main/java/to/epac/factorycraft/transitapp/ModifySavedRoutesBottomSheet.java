package to.epac.factorycraft.transitapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModifySavedRoutesBottomSheet extends BottomSheetDialogFragment {

    private SavedRouteManager savedRouteManager;

    private MaterialButton btnClose;
    private MaterialButton btnSearchAdd;
    private TextView rbSelectAll;

    private RecyclerView rvSavedRoutes;
    private SavedRouteAdapter adapter;

    private MaterialButton btnDelete;

    private List<SavedRouteManager.SavedRoute> savedRoutes;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private boolean isAllSelected = false;


    public interface OnDataChangedListener {
        void onDataChanged();
    }

    private OnDataChangedListener mListener;

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        this.mListener = listener;
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
        return inflater.inflate(R.layout.fragment_modify_saved_routes_bottomsheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        savedRouteManager = new SavedRouteManager(requireContext());
        savedRoutes = savedRouteManager.getSavedRoutes();

        btnClose = view.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            dismiss();
        });
        btnSearchAdd = view.findViewById(R.id.btn_search_add);
        btnSearchAdd.setOnClickListener(v -> {
            showSearchInputBottomSheet();
        });
        rbSelectAll = view.findViewById(R.id.rb_select_all);
        rbSelectAll.setOnClickListener(v -> {
            if (savedRoutes.isEmpty()) return;
            if (selectedPositions.size() == savedRoutes.size()) {
                selectedPositions.clear();
            } else {
                for (int i = 0; i < savedRoutes.size(); i++) {
                    selectedPositions.add(i);
                }
            }
            updateUI();
        });

        rvSavedRoutes = view.findViewById(R.id.rv_saved_routes);
        rvSavedRoutes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SavedRouteAdapter(savedRoutes, selectedPositions, () -> {
            updateUI();
        });
        rvSavedRoutes.setAdapter(adapter);

        btnDelete = view.findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> {
            deleteSelectedRoutes();
        });

        setupItemTouchHelper();

        updateUI();
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                Collections.swap(savedRoutes, fromPosition, toPosition);
                adapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                savedRouteManager.reorderRoutes(savedRoutes);

                if (mListener != null)
                    mListener.onDataChanged();
            }
        });
        itemTouchHelper.attachToRecyclerView(rvSavedRoutes);
    }

    private void refreshData() {
        savedRoutes = savedRouteManager.getSavedRoutes();

        adapter.updateRoutes(savedRoutes);
        if (mListener != null)
            mListener.onDataChanged();

        updateUI();
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();

        isAllSelected = !savedRoutes.isEmpty() && selectedPositions.size() == savedRoutes.size();

        rbSelectAll.setText(isAllSelected ? "取消全選" : "全選");

        int tintColor = ContextCompat.getColor(requireContext(), isAllSelected ? R.color.button_green : R.color.white);
        TextViewCompat.setCompoundDrawableTintList(rbSelectAll, ColorStateList.valueOf(tintColor));

        boolean hasSelection = !selectedPositions.isEmpty();
        btnDelete.setEnabled(hasSelection);

        int deleteBtnColor = hasSelection ?
                ContextCompat.getColor(requireContext(), R.color.button_green) :
                Color.parseColor("#2C2C2C");

        btnDelete.setBackgroundColor(deleteBtnColor);
    }

    private void deleteSelectedRoutes() {
        List<Integer> sortedIndices = new ArrayList<>(selectedPositions);
        sortedIndices.sort(Collections.reverseOrder());

        for (int index : sortedIndices) {
            savedRouteManager.deleteRoute(index);
        }

        selectedPositions.clear();
        refreshData();
    }

    private void showSearchInputBottomSheet() {
        RouteHostBottomSheet hostSheet = new RouteHostBottomSheet();
        hostSheet.setOnRouteAddedListener(() -> {
            refreshData();
        });
        hostSheet.show(getParentFragmentManager(), "route_host_nav");
    }


    private static class SavedRouteAdapter extends RecyclerView.Adapter<SavedRouteAdapter.ViewHolder> {
        private List<SavedRouteManager.SavedRoute> routes;
        private final OnItemClickListener listener;
        private final Set<Integer> selectedPositions;

        public interface OnItemClickListener {
            void onSelectionChanged();
        }

        public SavedRouteAdapter(List<SavedRouteManager.SavedRoute> routes, Set<Integer> selectedPositions, OnItemClickListener listener) {
            this.routes = routes;
            this.selectedPositions = selectedPositions;
            this.listener = listener;
        }

        public void updateRoutes(List<SavedRouteManager.SavedRoute> routes) {
            this.routes = routes;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_route, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SavedRouteManager.SavedRoute route = routes.get(position);
            holder.tvOrigin.setText(route.getOriginName());
            holder.tvDest.setText(route.getDestName());

            boolean isSelected = selectedPositions.contains(position);
            int colorOnSurface = Utils.getThemeColor(holder.itemView.getContext(), com.google.android.material.R.attr.colorOutline);

            holder.cbSelect.setImageResource(isSelected ? R.drawable.baseline_check_circle_24 : R.drawable.baseline_check_circle_outline_24);
            holder.cbSelect.setImageTintList(ColorStateList.valueOf(isSelected ?
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.button_green) : colorOnSurface));

            holder.itemView.setOnClickListener(v -> {
                if (selectedPositions.contains(position)) {
                    selectedPositions.remove(position);
                } else {
                    selectedPositions.add(position);
                }
                listener.onSelectionChanged();
            });
        }

        @Override
        public int getItemCount() {
            return routes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView cbSelect;
            TextView tvOrigin, tvDest;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cbSelect = itemView.findViewById(R.id.cb_select);
                tvOrigin = itemView.findViewById(R.id.tv_origin);
                tvDest = itemView.findViewById(R.id.tv_dest);
            }
        }
    }
}