package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.ImageView;
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

public class RouteListSubFragment extends Fragment {

    private HRConfig hrConf;
    private SharedPreferences prefs;

    private MaterialButton btnClose;
    private MaterialButton btnReturn;

    private TextView tvOrigin;
    private TextView tvDest;
    private TextView tvStart;

    private RecyclerView rvRoutes;
    private RouteAdapter adapter;


    private List<String> alertLineCodes = new ArrayList<>();
    private String trafficNews = "";

    private JSONObject routeData;
    private List<JSONObject> fullRouteList = new ArrayList<>();

    private Calendar startAt = null;

    private int rvHeight = 0;
    private int maxDuration = 30;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_list_bottomsheet, container, false);

        prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        hrConf = HRConfig.getInstance(requireContext());

        btnReturn = view.findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
        btnClose = view.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> {
            if (getParentFragment() instanceof RouteHostBottomSheet) {
                ((RouteHostBottomSheet) getParentFragment()).dismiss();
            }
        });

        String originId = getArguments().getString(RouteSearchFragment.ORIGIN_ID);
        String destId = getArguments().getString(RouteSearchFragment.DEST_ID);

        tvOrigin = view.findViewById(R.id.tv_header_origin);
        tvDest = view.findViewById(R.id.tv_header_dest);
        tvOrigin.setText(hrConf.getStationName(Integer.parseInt(originId)));
        tvDest.setText(hrConf.getStationName(Integer.parseInt(destId)));

        tvStart = view.findViewById(R.id.tv_journey_start);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        SimpleDateFormat sdf = new SimpleDateFormat("M月d日(E) 出發", Locale.TRADITIONAL_CHINESE);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        tvStart.setText(sdf.format(calendar.getTime()));

        if (originId.equals(destId)) {
            view.findViewById(R.id.routes_layout).setVisibility(View.GONE);
            view.findViewById(R.id.not_found_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.btn_back).setOnClickListener(v -> {
                if (isAdded())
                    getParentFragmentManager().popBackStack();
            });
            return view;
        }

        rvRoutes = view.findViewById(R.id.rv_routes);
        rvRoutes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new RouteListSubFragment.RouteAdapter();
        rvRoutes.setAdapter(adapter);

        // Once RecyclerView is drawn, draw background 15mins line
        rvRoutes.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (rvRoutes.getHeight() > 0) {
                    rvHeight = rvRoutes.getHeight();

                    updateBackground();
                    adapter.notifyDataSetChanged();

                    rvRoutes.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

        fetchData(originId, destId);

        return view;
    }


    private void fetchData(String origin, String dest) {
        new Thread(() -> {
            try {
                URL trafficUrl = new URL("https://tnews.mtr.com.hk/alert/ryg_line_status.json");
                HttpURLConnection tConn = (HttpURLConnection) trafficUrl.openConnection();
                tConn.setConnectTimeout(3000);
                tConn.setReadTimeout(3000);

                if (tConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader tReader = new BufferedReader(new InputStreamReader(tConn.getInputStream()));
                    StringBuilder tSb = new StringBuilder();
                    String tLine;
                    while ((tLine = tReader.readLine()) != null) tSb.append(tLine);

                    trafficNews = tSb.toString();

                    JSONObject tJson = new JSONObject(trafficNews);
                    JSONArray lines = tJson.getJSONObject("ryg_status").getJSONArray("line");

                    synchronized (alertLineCodes) {
                        alertLineCodes.clear();
                        for (int i = 0; i < lines.length(); i++) {
                            JSONObject lineObj = lines.getJSONObject(i);
                            String status = lineObj.getString("status").toLowerCase();
                            if (!status.equals("green") && !status.equals("grey") && !status.equals("typhoon")) {
                                alertLineCodes.add(lineObj.getString("line_code"));
                            }
                        }
                    }
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                URL url = new URL(MainActivity.ROUTE_URL + "?o=" + origin + "&d=" + dest + "&lang=C");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                routeData = new JSONObject(sb.toString());


                // Update Tab Layout data, background (if not drawn), adapter data
                requireActivity().runOnUiThread(() -> {
                    List<JSONObject> routes = new ArrayList<>();

                    // Find longest journey time route
                    try {
                        JSONArray data = routeData.getJSONArray("routes");
                        int max = 0;
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject r = data.getJSONObject(i);
                            routes.add(r);
                            if (r.optInt("time") > max) max = r.optInt("time");
                        }
                        maxDuration = (int) Math.ceil(max / 15.0) * 15;
                        if (maxDuration < 30) maxDuration = 30;
                    } catch (Exception e) {
                    }


                    // Set full route list
                    fullRouteList.clear();
                    fullRouteList.addAll(routes);


                    // Calculate journey start time based on first/last train
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
                    int nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                    try {
                        String[] ft = routeData.getJSONObject("firstTrain").getString("time").split(":");
                        String[] lt = routeData.getJSONObject("lastTrain").getString("time").split(":");

                        int firstMins = Integer.parseInt(ft[0]) * 60 + Integer.parseInt(ft[1]);
                        int lastMins = Integer.parseInt(lt[0]) * 60 + Integer.parseInt(lt[1]);

                        boolean isAfterLast;
                        if (lastMins < firstMins)
                            isAfterLast = (nowMins > lastMins && nowMins < firstMins);
                        else
                            isAfterLast = (nowMins > lastMins || nowMins < firstMins);

                        if (isAfterLast) {
                            if (nowMins > lastMins)
                                cal.add(Calendar.DAY_OF_MONTH, 1);
                            cal.set(Calendar.HOUR_OF_DAY, firstMins / 60);
                            cal.set(Calendar.MINUTE, firstMins % 60);
                        }
                    } catch (Exception e) {
                    }
                    startAt = cal;

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
                paint.setColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant));
                paint.setStrokeWidth(Utils.dpToPx(1));

                int w = getBounds().width();
                int startY = Utils.dpToPx(60);
                int endY = rvHeight - Utils.dpToPx(60);
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


    private class RouteAdapter extends RecyclerView.Adapter<RouteListSubFragment.RouteAdapter.ViewHolder> {
        private class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout routeLayout;
            LinearLayout layoutVisualSegments;
            LinearLayout layoutStatusBadges;
            TextView journeyTime, startTime, arriveTime, fare;

            private ViewHolder(View itemView) {
                super(itemView);
                routeLayout = itemView.findViewById(R.id.layout_route);
                layoutVisualSegments = itemView.findViewById(R.id.layout_visual_segments);
                journeyTime = itemView.findViewById(R.id.tv_journey_time);
                startTime = itemView.findViewById(R.id.tv_header_origin);
                arriveTime = itemView.findViewById(R.id.tv_header_dest);
                fare = itemView.findViewById(R.id.tv_fare);
                layoutStatusBadges = itemView.findViewById(R.id.layout_status_badges);
            }
        }

        @NonNull
        @Override
        public RouteListSubFragment.RouteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RouteListSubFragment.RouteAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_visual, parent, false));
        }

        @Override
        public int getItemCount() {
            return fullRouteList.size();
        }

        @Override
        public void onBindViewHolder(@NonNull RouteListSubFragment.RouteAdapter.ViewHolder holder, int position) {
            try {
                JSONObject route = fullRouteList.get(position);

                List<VisualSegment> segments = parsePathToSegments(route.getJSONArray("path"),
                        startAt.get(Calendar.HOUR_OF_DAY), startAt.get(Calendar.MINUTE));

                if (segments.isEmpty()) return;


                // Header & Footer UI Setup
                VisualSegment lastSeg = segments.get(segments.size() - 1);
                VisualSegment firstSeg = segments.get(0);

                int startTotalM = startAt.get(Calendar.HOUR_OF_DAY) * 60 + startAt.get(Calendar.MINUTE);
                int endTotalM = (lastSeg.endH < firstSeg.startH ? lastSeg.endH + 24 : lastSeg.endH) * 60 + lastSeg.endM;
                int journeyTime = endTotalM - startTotalM;

                holder.journeyTime.setText(journeyTime + "分");
                holder.startTime.setText(String.format(Locale.getDefault(), "%02d:%02d", firstSeg.startH, firstSeg.startM));
                holder.arriveTime.setText(String.format(Locale.getDefault(), "%02d:%02d", lastSeg.endH, lastSeg.endM));
                holder.fare.setText("$ " + getFare(route));


                // Apply min time/interchange/fare badge
                holder.layoutStatusBadges.removeAllViews();


                // On route item click: close this bottom sheet and open route detail bottom sheet
                holder.routeLayout.setOnClickListener(v -> {
                    RouteDetailSubFragment detailFrag = new RouteDetailSubFragment();

                    Bundle args = new Bundle();
                    args.putString("route_data", routeData.toString());
                    args.putInt("selected_route", position);
                    args.putString("start_time", String.format(Locale.getDefault(), "%02d:%02d", firstSeg.startH, firstSeg.startM));
                    args.putString("traffic_news", trafficNews);
                    detailFrag.setArguments(args);

                    if (getParentFragment() instanceof RouteHostBottomSheet) {
                        ((RouteHostBottomSheet) getParentFragment()).navigateTo(detailFrag, true);
                    }
                });


                // Draw visual segments
                drawVisualSegments(holder, segments, journeyTime);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void drawVisualSegments(RouteListSubFragment.RouteAdapter.ViewHolder holder, List<VisualSegment> segments, int adjustedTime) {
        int verticalPadding = Utils.dpToPx(14);
        int badgeSize = Utils.dpToPx(32);
        int stationSize = Utils.dpToPx(32);
        int walkIntSize = Utils.dpToPx(36);
        int interchangeSize = Utils.dpToPx(50);

        try {
            //            RecyclerView - Header reserved - Footer reserved
            int totalVisualSpan = rvHeight - Utils.dpToPx(60) - Utils.dpToPx(60);
            float pxPerMin = (float) totalVisualSpan / maxDuration;
            int containerHeight = Math.round(adjustedTime * pxPerMin);

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

            int neededSpaceForCircle = interchangeSize + Utils.dpToPx(10);
            boolean shouldShowInterchange = availableContentPx >= Utils.dpToPx(60);

            int finalAvailableContentPx = availableContentPx - (shouldShowInterchange ? interchangeSize : 0);
            int remainingPx = Math.max(0, finalAvailableContentPx - totalStaticPx);

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
                    heightPx = badgeSize + (showStation ? stationSize : 0) + extraPxPerRide;
                    if (currentRideIndex == rideCount - 1) heightPx += remainderPx;
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

            if (shouldShowInterchange) {
                View interchangeView = createInterchangeCircleView(Math.max(rideCount - 1, 0));

                if (holder.layoutVisualSegments.getParent() instanceof FrameLayout) {
                    FrameLayout container = (FrameLayout) holder.layoutVisualSegments.getParent();

                    FrameLayout.LayoutParams circleParams = new FrameLayout.LayoutParams(interchangeSize, interchangeSize);
                    circleParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    circleParams.topMargin = Utils.dpToPx(5);
                    circleParams.bottomMargin = Utils.dpToPx(5);
                    container.addView(interchangeView, circleParams);
                }
            }

            ViewGroup.LayoutParams segmentParams = holder.layoutVisualSegments.getLayoutParams();
            segmentParams.height = containerHeight;
            holder.layoutVisualSegments.setLayoutParams(segmentParams);

            holder.layoutVisualSegments.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Utils.dpToPx(60));
                }
            });
            holder.layoutVisualSegments.setClipToOutline(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View createInterchangeCircleView(int count) {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorSurface));
        container.setBackground(shape);

        TextView tvLabel = new TextView(getContext());
        tvLabel.setText("轉乘");
        tvLabel.setTextSize(12);
        tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOutline));

        TextView tvCount = new TextView(getContext());
        tvCount.setText(count + "次");
        tvCount.setTextSize(12);
        tvCount.setGravity(Gravity.CENTER);
        tvCount.setTypeface(null, Typeface.BOLD);
        tvCount.setTextColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorSurfaceInverse));

        container.addView(tvLabel);
        container.addView(tvCount);

        return container;
    }

    private View createSegmentView(VisualSegment seg, boolean isLastSegment) {
        int badgeSize = Utils.dpToPx(32);
        int stationSize = Utils.dpToPx(32);
        int walkIntSize = Utils.dpToPx(36);

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
            tvIcon.setTextColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant));
            walkContainer.addView(tvIcon);

            TextView tvWalk = new TextView(getContext());
            tvWalk.setText(seg.duration + "");
            tvWalk.setTextSize(10);
            tvWalk.setTypeface(null, Typeface.BOLD);
            tvWalk.setTextColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOnSurface));

            GradientDrawable bgTvWalk = new GradientDrawable();
            bgTvWalk.setShape(GradientDrawable.RECTANGLE);
            bgTvWalk.setCornerRadius(Utils.dpToPx(4));
            bgTvWalk.setColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceInverse));
            tvWalk.setBackground(bgTvWalk);
            tvWalk.setPadding(Utils.dpToPx(4), Utils.dpToPx(2), Utils.dpToPx(4), Utils.dpToPx(2));
            walkContainer.addView(tvWalk);

            FrameLayout.LayoutParams walkParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, walkIntSize);
            walkParams.gravity = Gravity.CENTER_HORIZONTAL;
            segmentView.addView(walkContainer, walkParams);
        } else {
            int lineColor = Color.parseColor("#" + seg.lineColor);

            View lineBar = new View(getContext());
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(Utils.dpToPx(8), ViewGroup.LayoutParams.MATCH_PARENT);
            barParams.gravity = Gravity.CENTER_HORIZONTAL;
            barParams.bottomMargin = showStation ? stationSize : 0;

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(Utils.dpToPx(8));
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
            badgeBg.setCornerRadius(Utils.dpToPx(4));
            badgeBg.setColor(lineColor);
            badgeContainer.setBackground(badgeBg);

            TextView tvLineCode = new TextView(getContext());
            FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(Utils.dpToPx(26), Utils.dpToPx(26));
            textParams.gravity = Gravity.CENTER;
            tvLineCode.setBackgroundColor(Color.WHITE);
            tvLineCode.setLayoutParams(textParams);

            tvLineCode.setText(seg.lineCode);
            tvLineCode.setTextColor(Color.BLACK);
            tvLineCode.setTextSize(12);
            tvLineCode.setTypeface(null, Typeface.BOLD);
            tvLineCode.setGravity(Gravity.CENTER);
            badgeContainer.addView(tvLineCode);

            segmentView.addView(badgeContainer);

            if (hrConf.isTerminus(seg.lineCode, seg.startNode.optInt("ID"))) {
                TextView tvTerminus = new TextView(getContext());
                int size = Utils.dpToPx(16);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
                params.gravity = Gravity.TOP | Gravity.START;
                params.leftMargin = -Utils.dpToPx(5);
                params.topMargin = -Utils.dpToPx(5);

                tvTerminus.setLayoutParams(params);
                tvTerminus.setText("始");
                tvTerminus.setTextColor(Color.WHITE);
                tvTerminus.setTextSize(9);
                tvTerminus.setGravity(Gravity.CENTER);
                tvTerminus.setTypeface(null, Typeface.BOLD);

                GradientDrawable termBg = new GradientDrawable();
                termBg.setShape(GradientDrawable.RECTANGLE);
                termBg.setCornerRadius(Utils.dpToPx(6));
                termBg.setColor(Color.parseColor("#80D8FF"));
                termBg.setStroke(Utils.dpToPx(1), Color.BLACK);
                tvTerminus.setBackground(termBg);

                badgeContainer.addView(tvTerminus);
            }

            if (alertLineCodes.contains(seg.lineCode)) {
                ImageView ivAlert = new ImageView(getContext());
                int size = Utils.dpToPx(18);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);

                params.gravity = Gravity.TOP | Gravity.END;
                params.rightMargin = -Utils.dpToPx(5);
                params.topMargin = -Utils.dpToPx(5);
                ivAlert.setLayoutParams(params);
                ivAlert.setImageResource(R.drawable.ic_line_warning);

                badgeContainer.addView(ivAlert);
            }
        }

        if (showStation) {
            TextView tvStation = new TextView(getContext());
            tvStation.setText(seg.stationName);
            tvStation.setTextColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant));
            tvStation.setTextSize(14);
            tvStation.setTypeface(null, Typeface.BOLD);
            tvStation.setGravity(Gravity.CENTER);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, stationSize);
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            tvStation.setLayoutParams(params);
            segmentView.addView(tvStation);
        }

        return segmentView;
    }


    private List<VisualSegment> parsePathToSegments(JSONArray path, int startH, int startM) {
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

            int startTotalM = (startH * 60) + startM + rawStartTime + accumulatedDelay;
            seg.startH = (startTotalM / 60) % 24;
            seg.startM = startTotalM % 60;

            seg.duration = (int) (rawDuration * (isWalk ? multiplier : 1.0F));

            int endTotalM = startTotalM + seg.duration;
            seg.endH = (endTotalM / 60) % 24;
            seg.endM = endTotalM % 60;

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

    private float getFare(JSONObject route) {
        float totalFare = 0.0f;

        try {
            String fareType = prefs.getString(MainActivity.KEY_FARE_TYPE, "adult");
            String ticketType = prefs.getString(MainActivity.KEY_TICKET_TYPE, "octopus");

            JSONArray fares = route.getJSONArray("fares");
            for (int i = 0; i < fares.length(); i++) {
                JSONObject fareSection = fares.getJSONObject(i);
                JSONObject fareInfo = fareSection.getJSONObject("fareInfo");
                String fareTitle = fareSection.optString("fareTitle", "");

                if (fareTitle.equals("firstClass")) continue;

                String currentFareType = fareType;

                if (!fareInfo.has(currentFareType)) {
                    if (currentFareType.equals("concessionchild") || currentFareType.equals("concessionchild2")) {
                        currentFareType = "concession";
                    } else {
                        currentFareType = "adult";
                    }
                }


                JSONObject idObj = fareInfo.optJSONObject(currentFareType);
                if (idObj != null) {
                    String fStr = idObj.optString(ticketType, "0");

                    if (!fStr.equals("-") && !fStr.equals("免費"))
                        totalFare += Float.parseFloat(fStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return totalFare;
    }
}