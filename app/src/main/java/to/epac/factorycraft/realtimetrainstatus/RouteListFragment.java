package to.epac.factorycraft.realtimetrainstatus;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RouteListFragment extends Fragment {

    private RecyclerView recyclerView;
    private RouteAdapter adapter;
    private List<JSONObject> routeList = new ArrayList<>();
    private String routeData = "";

    private HRConfig hrConf;
    private Calendar startAt = null;

    private int rvHeight = 0;
    private int maxDuration = 30;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_list, container, false);

        hrConf = HRConfig.getInstance(requireContext());

        String oID = getArguments() != null ? getArguments().getString("o") : "1";
        String dID = getArguments() != null ? getArguments().getString("d") : "13";

        MaterialButton btnReturn = view.findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0)
                getParentFragmentManager().popBackStack();
        });

        TextView tvOrigin = view.findViewById(R.id.tv_header_origin);
        TextView tvDest = view.findViewById(R.id.tv_header_dest);
        tvOrigin.setText(hrConf.getStationName(Integer.parseInt(oID)));
        tvDest.setText(hrConf.getStationName(Integer.parseInt(dID)));

        TextView tvStart = view.findViewById(R.id.tv_journey_start);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        SimpleDateFormat sdf = new SimpleDateFormat("M月d日(E) 出發", Locale.TRADITIONAL_CHINESE);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        tvStart.setText(sdf.format(calendar.getTime()));

        if (oID.equals(dID)) {
            view.findViewById(R.id.not_found_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_back).setOnClickListener(v -> {
                if (isAdded())
                    getParentFragmentManager().popBackStack();
            });
            return view;
        }

        recyclerView = view.findViewById(R.id.rv_routes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new RouteAdapter();
        recyclerView.setAdapter(adapter);

        // Once RecyclerView is drawn, draw background 15mins line
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (recyclerView.getHeight() > 0) {
                    rvHeight = recyclerView.getHeight();

                    if (!routeList.isEmpty()) {
                        updateBackground();
                        adapter.notifyDataSetChanged();
                    }

                    recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

        fetchData(oID, dID);
        return view;
    }

    private void fetchData(String origin, String dest) {
        new Thread(() -> {
            try {
                URL url = new URL("https://www.mtr.com.hk/share/customer/jp/api/HRRoutes/?o=" + origin + "&d=" + dest);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                routeData = sb.toString();
                JSONObject data = new JSONObject(routeData);
                JSONArray routes = data.getJSONArray("routes");

                int max = 0;
                List<JSONObject> tempRoutes = new ArrayList<>();
                for (int i = 0; i < routes.length(); i++) {
                    JSONObject r = routes.getJSONObject(i);
                    tempRoutes.add(r);
                    if (r.optInt("time") > max) max = r.optInt("time");
                }
                maxDuration = (int) Math.ceil(max / 15.0) * 15;
                if (maxDuration < 30) maxDuration = 30;

                String firstTrain = data.getJSONObject("firstTrain").getString("time");
                String lastTrain = data.getJSONObject("lastTrain").getString("time");

                Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
                int nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
                int firstMins = Integer.parseInt(firstTrain.split(":")[0]) * 60 + Integer.parseInt(firstTrain.split(":")[1]);
                int lastMins = Integer.parseInt(lastTrain.split(":")[0]) * 60 + Integer.parseInt(lastTrain.split(":")[1]);

                if (lastMins < firstMins) lastMins += 24 * 60;
                boolean isAfterLast = (nowMins > lastMins % 1440 && nowMins < firstMins);

                startAt = (Calendar) now.clone();
                if (isAfterLast) {
                    startAt.set(Calendar.HOUR_OF_DAY, firstMins / 60);
                    startAt.set(Calendar.MINUTE, firstMins % 60);
                }

                requireActivity().runOnUiThread(() -> {
                    routeList.clear();
                    routeList.addAll(tempRoutes);

                    updateBackground();
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateBackground() {
        if (rvHeight <= 0 || getView() == null) return;

        View bgView = getView().findViewById(R.id.layout_background_lines);
        bgView.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#33FFFFFF"));
                paint.setStrokeWidth(dpToPx(1));

                int w = getBounds().width();
                int startY = dpToPx(60);
                int endY = rvHeight - dpToPx(60);
                float availableH = endY - startY;
                float pxPerMin = availableH / maxDuration;

                for (int min = 0; min <= maxDuration; min += 15) {
                    float y = startY + (min * pxPerMin);
                    canvas.drawLine(0, y, w, y, paint);
                }
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return PixelFormat.UNKNOWN;
            }
        });
    }

    private class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {
        private class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout routeLayout;
            LinearLayout layoutVisualSegments;
            TextView journeyTime, startTime, interchangeCount, arriveTime, fare;

            private ViewHolder(View itemView) {
                super(itemView);
                routeLayout = itemView.findViewById(R.id.layout_route);
                layoutVisualSegments = itemView.findViewById(R.id.layout_visual_segments);
                journeyTime = itemView.findViewById(R.id.tv_journey_time);
                startTime = itemView.findViewById(R.id.tv_header_origin);
                interchangeCount = itemView.findViewById(R.id.tv_interchange_count);
                arriveTime = itemView.findViewById(R.id.tv_header_dest);
                fare = itemView.findViewById(R.id.tv_fare);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_visual, parent, false));
        }

        @Override
        public int getItemCount() {
            return routeList.size();
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            try {
                JSONObject route = routeList.get(position);

                holder.routeLayout.setOnClickListener(v -> {
                    RouteDetailFragment detailFragment = new RouteDetailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("route_data", routeData);
                    bundle.putInt("selected_route", position);
                    bundle.putString("start_time", String.format(Locale.getDefault(), "%02d:%02d",
                            startAt.get(Calendar.HOUR_OF_DAY), startAt.get(Calendar.MINUTE)));
                    detailFragment.setArguments(bundle);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.main_container, detailFragment)
                            .addToBackStack("DETAIL_PAGE")
                            .commit();
                });

                // Header & Footer UI Setup
                Calendar cal = (Calendar) startAt.clone();

                holder.journeyTime.setText(route.getInt("time") + "分");
                holder.startTime.setText(String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
                holder.interchangeCount.setText(route.getInt("interchangeStationsNo") + " 次轉乘");

                int arrM_total = cal.get(Calendar.MINUTE) + route.getInt("time");
                holder.arriveTime.setText(String.format(Locale.getDefault(), "%02d:%02d", (cal.get(Calendar.HOUR_OF_DAY) + arrM_total / 60) % 24, arrM_total % 60));

                String fareStr = route.getJSONArray("fares").getJSONObject(0).getJSONObject("fareInfo").getJSONObject("adult").getString("octopus");
                holder.fare.setText("$ " + fareStr);

                // Draw visual segments
                drawVisualSegments(holder, route);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void drawVisualSegments(RouteAdapter.ViewHolder holder, JSONObject route) {
        int verticalPadding = dpToPx(10);
        int badgeSize = dpToPx(32);
        int stationSize = dpToPx(32);
        int walkIntSize = dpToPx(36);

        try {
            //            RecyclerView - Header reserved - Footer reserved
            int totalVisualSpan = rvHeight - dpToPx(60) - dpToPx(60);
            float pxPerMin = (float) totalVisualSpan / maxDuration;
            int containerHeight = Math.round(route.getInt("time") * pxPerMin);

            List<VisualSegment> segments = parsePathToSegments(route.getJSONArray("path"),
                    startAt.get(Calendar.HOUR_OF_DAY), startAt.get(Calendar.MINUTE));

            int rideCount = 0;
            int totalStaticPx = 0;
            int availableContentPx = containerHeight - (verticalPadding * 2);

            for (int i = 0; i < segments.size(); i++) {
                VisualSegment seg = segments.get(i);
                boolean showStation = seg.stationName != null && !seg.stationName.isEmpty() && (i < segments.size() - 1);

                if (seg.isWalk) {
                    totalStaticPx += walkIntSize + (showStation ? stationSize : 0);
                } else {
                    rideCount++;
                    totalStaticPx += badgeSize + (showStation ? stationSize : 0);
                }
            }

            int remainingPx = Math.max(0, availableContentPx - totalStaticPx);
            int extraPxPerRide = (rideCount > 0) ? (remainingPx / rideCount) : 0;
            int remainderPx = (rideCount > 0) ? (remainingPx % rideCount) : 0;

            holder.layoutVisualSegments.removeAllViews();
            holder.layoutVisualSegments.setPadding(0, verticalPadding, 0, verticalPadding);
            holder.layoutVisualSegments.setClipChildren(false);
            holder.layoutVisualSegments.setClipToPadding(false);

            int currentRideIndex = 0;
            for (int i = 0; i < segments.size(); i++) {
                VisualSegment seg = segments.get(i);
                boolean isLast = (i == segments.size() - 1);
                boolean showStation = seg.stationName != null && !seg.stationName.isEmpty() && !isLast;

                int heightPx;
                if (seg.isWalk) {
                    heightPx = walkIntSize + (showStation ? stationSize : 0);
                } else {
                    int staticBase = badgeSize + (showStation ? stationSize : 0);
                    heightPx = staticBase + extraPxPerRide;

                    if (currentRideIndex == rideCount - 1) {
                        heightPx += remainderPx;
                    }
                }

                View segmentView = createSegmentView(seg, isLast);

                if (!seg.isWalk) {
                    View badge = segmentView.findViewWithTag("badge_container");
                    if (badge != null) {
                        int maxTransY = heightPx - (showStation ? stationSize : 0) - badgeSize;

                        // If only RIDE, badge at middle
                        if (rideCount == 1 && segments.size() == 1) {
                            badge.setTranslationY(maxTransY / 2f); // 居中
                        }
                        // If first segment is RIDE, badge at top
                        else if (currentRideIndex == 0) {
                            badge.setTranslationY(0);
                        }
                        // If last segment is RIDE, badge at bottom
                        else if (currentRideIndex == rideCount - 1) {
                            badge.setTranslationY(maxTransY);
                        }
                        // If intermediate segment, badge at middle
                        else {
                            badge.setTranslationY(maxTransY / 2f);
                        }
                    }
                    currentRideIndex++;
                }
                holder.layoutVisualSegments.addView(segmentView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));
            }

            ViewGroup.LayoutParams segmentParams = holder.layoutVisualSegments.getLayoutParams();
            segmentParams.height = containerHeight;
            holder.layoutVisualSegments.setLayoutParams(segmentParams);

            holder.layoutVisualSegments.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(60));
                }
            });
            holder.layoutVisualSegments.setClipToOutline(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View createSegmentView(VisualSegment seg, boolean isLastSegment) {
        int badgeSize = dpToPx(32);
        int stationSize = dpToPx(32);
        int walkIntSize = dpToPx(36);

        FrameLayout segmentView = new FrameLayout(getContext());
        segmentView.setClipChildren(false);
        segmentView.setClipToPadding(false);
        segmentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        boolean showStation = seg.stationName != null && !seg.stationName.isEmpty() && !isLastSegment;

        if (seg.isWalk) {
            LinearLayout walkContainer = new LinearLayout(getContext());
            walkContainer.setOrientation(LinearLayout.HORIZONTAL);
            walkContainer.setGravity(Gravity.CENTER);

            TextView tvIcon = new TextView(getContext());
            tvIcon.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_directions_walk_24, 0, 0, 0);
            tvIcon.setTextSize(10);
            tvIcon.setTextColor(Color.parseColor("#CCCCCC"));
            walkContainer.addView(tvIcon);

            TextView tvWalk = new TextView(getContext());
            tvWalk.setText(seg.duration + "");
            tvWalk.setTextSize(10);
            tvWalk.setTypeface(null, Typeface.BOLD);
            tvWalk.setTextColor(Color.parseColor("#CCCCCC"));

            GradientDrawable bgTvWalk = new GradientDrawable();
            bgTvWalk.setShape(GradientDrawable.RECTANGLE);
            bgTvWalk.setCornerRadius(dpToPx(4));
            bgTvWalk.setColor(Color.parseColor("#000000"));
            tvWalk.setBackground(bgTvWalk);
            tvWalk.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            walkContainer.addView(tvWalk);

            FrameLayout.LayoutParams walkParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, walkIntSize);
            walkParams.gravity = Gravity.CENTER_HORIZONTAL;
            segmentView.addView(walkContainer, walkParams);
        } else {
            int lineColor = Color.parseColor("#" + seg.lineColor);

            View lineBar = new View(getContext());
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(dpToPx(8), ViewGroup.LayoutParams.MATCH_PARENT);
            barParams.gravity = Gravity.CENTER_HORIZONTAL;
            barParams.bottomMargin = showStation ? stationSize : 0;

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(dpToPx(8));
            shape.setColor(lineColor);
            lineBar.setBackground(shape);
            segmentView.addView(lineBar, barParams);

            FrameLayout badgeContainer = new FrameLayout(getContext());
            badgeContainer.setTag("badge_container");
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(badgeSize, badgeSize);
            badgeParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            badgeContainer.setLayoutParams(badgeParams);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(dpToPx(4));
            badgeBg.setColor(lineColor);
            badgeContainer.setBackground(badgeBg);

            TextView tvLineCode = new TextView(getContext());
            FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(dpToPx(26), dpToPx(26));
            textParams.gravity = Gravity.CENTER;
            tvLineCode.setBackgroundColor(Color.WHITE);
            tvLineCode.setLayoutParams(textParams);

            tvLineCode.setText(seg.lineName);
            tvLineCode.setTextColor(Color.BLACK);
            tvLineCode.setTextSize(12);
            tvLineCode.setTypeface(null, Typeface.BOLD);
            tvLineCode.setGravity(Gravity.CENTER);
            badgeContainer.addView(tvLineCode);

            segmentView.addView(badgeContainer);
        }

        if (showStation) {
            TextView tvStation = new TextView(getContext());
            tvStation.setText(seg.stationName);
            tvStation.setTextColor(Color.parseColor("#BBBBBB"));
            tvStation.setTextSize(14);
            tvStation.setTypeface(null, Typeface.BOLD);
            tvStation.setGravity(Gravity.CENTER);

            FrameLayout.LayoutParams stationParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, stationSize);
            stationParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            tvStation.setLayoutParams(stationParams);
            segmentView.addView(tvStation);
        }

        return segmentView;
    }

    private List<VisualSegment> parsePathToSegments(JSONArray path, int startH, int startM) {
        List<VisualSegment> segments = new ArrayList<>();
        if (path == null || path.length() < 2) return segments;

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
            int startTime = startNode.optInt("time");
            int endTime = endNode.optInt("time");
            seg.duration = endTime - startTime;

            int startTotalM = (startH * 60) + startM + startTime;
            seg.startH = (startTotalM / 60) % 24;
            seg.startM = startTotalM % 60;

            int endTotalM = (startH * 60) + startM + endTime;
            seg.endH = (endTotalM / 60) % 24;
            seg.endM = endTotalM % 60;

            // Apply line name and color
            seg.lineName = hrConf.getLineById(startLineID).alias;
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

    private int dpToPx(int dp) {
        return Math.round((float) dp * getResources().getDisplayMetrics().density);
    }
}