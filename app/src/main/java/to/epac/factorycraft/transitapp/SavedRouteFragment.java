package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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
        private HRConfig hrConf;
        private SharedPreferences prefs;

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

            prefs = parent.getContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
            hrConf = HRConfig.getInstance(parent.getContext());

            return new SavedRouteAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SavedRouteAdapter.ViewHolder holder, int position) {
            SavedRouteManager.SavedRoute route = routes.get(position);
            holder.tvOrigin.setText(route.getOriginName());
            holder.tvDest.setText(route.getDestName());

            View.OnClickListener cardClickListener = v -> {
                if (listener != null) {
                    listener.onItemClick(route);
                }
            };
            holder.itemView.setOnClickListener(cardClickListener);
            holder.segmentCardLayout.setOnClickListener(cardClickListener);

            holder.segmentCardLayout.removeAllViews();
            String jsonStr = route.getRouteJson();
            if (jsonStr != null && !jsonStr.isEmpty()) {
                try {
                    JSONObject data = new JSONObject(jsonStr);
                    List<VisualSegment> segments = parseJsonToSegments(data.getJSONArray("path"));
                    drawHorizontalRoute(holder.segmentCardLayout, route.getOriginName(), segments);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public int getItemCount() {
            return routes.size();
        }

        private List<VisualSegment> parseJsonToSegments(JSONArray path) {
            List<VisualSegment> segments = new ArrayList<>();
            if (path == null || path.length() < 2) return segments;

            // Walk interchange multiplier
            float multiplier = 1.0F;
            String speed = prefs.getString(MainActivity.KEY_WALK_SPEED, "普通");
            switch (speed) {
                case "很慢":
                    multiplier = 1.2F;
                    break;
                case "慢速":
                    multiplier = 1.1F;
                    break;
                case "普通":
                    multiplier = 1.0F;
                    break;
                case "快速":
                    multiplier = 0.9F;
                    break;
            }

            int accumulatedDelay = 0;

            int i = 0;
            while (i < path.length() - 1) {
                JSONObject startNode = path.optJSONObject(i);
                boolean isWalk = startNode.optString("linkType").equals("WALKINTERCHANGE");
                int startLineID = startNode.optInt("lineID");

                // Find segment end
                int j = i;
                while (j < path.length() - 1) {
                    JSONObject currNode = path.optJSONObject(j);
                    boolean currIsWalk = currNode.optString("linkType").equals("WALKINTERCHANGE");
                    int currLineID = currNode.optInt("lineID");

                    // If linkType change, segment ends
                    if (isWalk != currIsWalk || (!isWalk && currLineID != startLineID)) {
                        break;
                    }
                    j++;
                }

                // Construct VisualSegment
                VisualSegment seg = new VisualSegment();
                JSONObject endNode = path.optJSONObject(j);

                seg.startNode = startNode;
                seg.endNode = endNode;
                seg.isWalk = isWalk;
                seg.lineID = startLineID;

                // Calculate start end time and duration
                int rawStartTime = startNode.optInt("time");
                int rawEndTime = endNode.optInt("time");
                int rawDuration = rawEndTime - rawStartTime;

                seg.duration = (int) (rawDuration * (isWalk ? multiplier : 1.0F));

                accumulatedDelay += (seg.duration - rawDuration);

                // Apply line name and color
                seg.lineCode = hrConf.getLineById(startLineID).alias;
                seg.lineColor = hrConf.getLineById(startLineID).color;
                seg.stationName = hrConf.getStationName(endNode.optInt("ID"));

                // Add intermediate stations
                for (int k = i + 1; k < j; k++) {
                    seg.intermediates.add(path.optJSONObject(k));
                }

                segments.add(seg);

                // Set next segment start
                if (j < path.length() - 1)
                    i = j;
                else
                    i = j + 1;
            }

            return segments;
        }

        private void drawHorizontalRoute(FlexboxLayout container, String originName, List<VisualSegment> segments) {
            container.removeAllViews();
            container.setFlexWrap(FlexWrap.WRAP);

            addStationViewToCard(container, originName, false);

            for (VisualSegment seg : segments) {
                if (seg.isWalk) {
                    addStationViewToCard(container, seg.stationName, true);
                } else {
                    addTrainSegmentViewToCard(container, seg.lineCode, seg.lineColor);
                    addStationViewToCard(container, seg.stationName, false);
                }
            }

            container.post(() -> {
                if (container.getFlexLines().size() > 2) {
                    while (container.getFlexLines().size() > 2 && container.getChildCount() > 0) {
                        container.removeViewAt(container.getChildCount() - 1);
                    }
                    TextView tvMore = new TextView(container.getContext());
                    tvMore.setText("...");
                    container.addView(tvMore);
                }
            });
        }

        private void addStationViewToCard(FlexboxLayout container, String stationName, boolean walk) {
            Context context = container.getContext();
            int textColor = Utils.getThemeColor(context, com.google.android.material.R.attr.colorOnSurface);

            if (walk) {
                TextView tvWalk = new TextView(context);
                FlexboxLayout.LayoutParams walkParams = new FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                walkParams.setMargins(0, Utils.dpToPx(context, 2), 0, Utils.dpToPx(context, 2));
                tvWalk.setLayoutParams(walkParams);

                tvWalk.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_directions_walk_24, 0, 0, 0);
                tvWalk.setCompoundDrawablePadding(0);
                tvWalk.setGravity(Gravity.CENTER);
                tvWalk.setCompoundDrawableTintList(ColorStateList.valueOf(textColor));

                container.addView(tvWalk);
            }

            TextView tv = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Utils.dpToPx(context, 32));
            params.gravity = Gravity.CENTER_VERTICAL;
            tv.setPadding(Utils.dpToPx(context, 6), 0, Utils.dpToPx(context, 6), 0);
            tv.setLayoutParams(params);
            tv.setText(stationName);
            tv.setTextSize(12);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(textColor);

            container.addView(tv);
        }

        private void addTrainSegmentViewToCard(FlexboxLayout container, String lineAlias, String lineColor) {
            Context context = container.getContext();

            FrameLayout segmentContainer = new FrameLayout(context);
            FlexboxLayout.LayoutParams segmentParams = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            segmentParams.setMargins(Utils.dpToPx(context, 2), 0, Utils.dpToPx(context, 2), 0);
            segmentContainer.setLayoutParams(segmentParams);

            View line = new View(context);
            FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(
                    Utils.dpToPx(context, 40),
                    Utils.dpToPx(context, 6)
            );
            lineParams.gravity = Gravity.CENTER;
            line.setLayoutParams(lineParams);
            line.setBackgroundColor(Color.parseColor("#" + lineColor));

            GradientDrawable lineBg = new GradientDrawable();
            lineBg.setShape(GradientDrawable.RECTANGLE);
            lineBg.setCornerRadius(Utils.dpToPx(context, 3));
            lineBg.setColor(Color.parseColor("#" + lineColor));
            line.setBackground(lineBg);

            segmentContainer.addView(line);

            FrameLayout badgeContainer = new FrameLayout(context);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                    Utils.dpToPx(context, 32),
                    Utils.dpToPx(context, 32)
            );
            badgeParams.gravity = Gravity.CENTER;
            badgeContainer.setLayoutParams(badgeParams);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(Utils.dpToPx(context, 4));
            badgeBg.setColor(Color.parseColor("#" + lineColor));
            badgeContainer.setBackground(badgeBg);

            TextView tvLineCode = new TextView(context);
            FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                    Utils.dpToPx(context, 26),
                    Utils.dpToPx(context, 26)
            );
            textParams.gravity = Gravity.CENTER;
            tvLineCode.setBackgroundColor(Color.WHITE);
            tvLineCode.setLayoutParams(textParams);
            tvLineCode.setText(lineAlias != null ? lineAlias : "");
            tvLineCode.setTextColor(Color.BLACK);
            tvLineCode.setTextSize(10);
            tvLineCode.setTypeface(null, Typeface.BOLD);
            tvLineCode.setGravity(Gravity.CENTER);

            badgeContainer.addView(tvLineCode);
            segmentContainer.addView(badgeContainer);

            container.addView(segmentContainer);
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrigin, tvDest;
            FlexboxLayout segmentCardLayout;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvOrigin = itemView.findViewById(R.id.tv_origin);
                tvDest = itemView.findViewById(R.id.tv_dest);
                segmentCardLayout = itemView.findViewById(R.id.layout_card_segments);
            }
        }
    }
}