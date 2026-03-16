package to.epac.factorycraft.realtimetrainstatus;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedRouteFragment extends Fragment {

    private SavedRouteManager savedRouteManager;

    private View emptyLayout;
    private Button btnRegister;

    private View savedLayout;
    private View btnEdit;

    private RecyclerView rvSavedRoutes;
    private SavedRouteAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_route, container, false);

        savedRouteManager = new SavedRouteManager(requireContext());

        emptyLayout = view.findViewById(R.id.layout_empty);
        btnRegister = view.findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(v -> {
            showSearchInputBottomSheet();
        });

        savedLayout = view.findViewById(R.id.layout_saved);
        btnEdit = view.findViewById(R.id.btn_edit);
        btnEdit.setOnClickListener(v -> {
            ModifySavedRoutesBottomSheet bottomSheet = new ModifySavedRoutesBottomSheet();
            bottomSheet.setOnDataChangedListener(() -> {
                updateLayoutState();
            });
            bottomSheet.show(getParentFragmentManager(), "modify_saved_routes");
        });

        rvSavedRoutes = view.findViewById(R.id.rv_saved_routes);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvSavedRoutes.getContext(), LinearLayoutManager.VERTICAL);
        rvSavedRoutes.addItemDecoration(dividerItemDecoration);
        rvSavedRoutes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SavedRouteAdapter(savedRouteManager.getSavedRoutes(), route -> {
            showRouteListBottomSheet(route.getOriginID(), route.getDestID());
        });
        rvSavedRoutes.setAdapter(adapter);

        updateLayoutState();

        return view;
    }


    private void updateLayoutState() {
        List<SavedRouteManager.SavedRoute> routes = savedRouteManager.getSavedRoutes();
        if (routes.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            savedLayout.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            savedLayout.setVisibility(View.VISIBLE);
        }
        adapter.updateRoutes(routes);
    }

    private void showSearchInputBottomSheet() {
        RouteHostBottomSheet hostSheet = new RouteHostBottomSheet();
        hostSheet.setOnRouteAddedListener(() -> {
            updateLayoutState();
        });
        hostSheet.show(getParentFragmentManager(), "route_host_nav");
    }

    private void showRouteListBottomSheet(String originID, String destID) {
        RouteListFragment fragment = new RouteListFragment();

        Bundle args = new Bundle();
        args.putString(RouteSearchFragment.ORIGIN_ID, originID);
        args.putString(RouteSearchFragment.DEST_ID, destID);
        args.putBoolean("hide_tabs", true);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }


    private static class SavedRouteAdapter extends RecyclerView.Adapter<SavedRouteAdapter.ViewHolder> {

        private List<SavedRouteManager.SavedRoute> routes;
        private SavedRouteAdapter.OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(SavedRouteManager.SavedRoute route);
        }

        public SavedRouteAdapter(List<SavedRouteManager.SavedRoute> routes, SavedRouteAdapter.OnItemClickListener listener) {
            this.routes = routes;
            this.listener = listener;
        }

        public void updateRoutes(List<SavedRouteManager.SavedRoute> routes) {
            this.routes = routes;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SavedRouteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_search_card, parent, false);
            return new SavedRouteAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SavedRouteAdapter.ViewHolder holder, int position) {
            SavedRouteManager.SavedRoute route = routes.get(position);
            holder.tvOrigin.setText(route.getOriginName());
            holder.tvDest.setText(route.getDestName());

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(route);
                }
            });
        }

        @Override
        public int getItemCount() {
            return routes.size();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrigin, tvDest;
            LinearLayout segmentCardLayout;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrigin = itemView.findViewById(R.id.tv_origin);
                tvDest = itemView.findViewById(R.id.tv_dest);
                segmentCardLayout = itemView.findViewById(R.id.layout_card_segments);
            }
        }
    }
}