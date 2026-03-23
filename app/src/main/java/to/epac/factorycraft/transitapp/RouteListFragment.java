package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

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

    private HRConfig hrConf;
    private SharedPreferences prefs;

    MaterialButton btnReturn;

    TextView tvOrigin;
    TextView tvDest;
    TextView tvStart;

    private RecyclerView rvRoutes;
    private RouteAdapter adapter;
    private TabLayout tabLayout;


    private List<String> alertLineCodes = new ArrayList<>();
    private String trafficNews = "";

    private JSONObject routeData;
    private List<JSONObject> fullRouteList = new ArrayList<>();
    private List<JSONObject> routeList = new ArrayList<>();

    private int minTime;
    private int minInter;
    private float minFare;

    private Calendar startAt = null;

    private int rvHeight = 0;
    private int maxDuration = 30;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_list, container, false);

        prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        hrConf = HRConfig.getInstance(requireContext());

        btnReturn = view.findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0)
                getParentFragmentManager().popBackStack();
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
        adapter = new RouteAdapter();
        rvRoutes.setAdapter(adapter);

        // Once RecyclerView is drawn, draw background 15mins line
        rvRoutes.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (rvRoutes.getHeight() > 0) {
                    rvHeight = rvRoutes.getHeight();

                    if (!routeList.isEmpty()) {
                        updateBackground();
                        adapter.notifyDataSetChanged();
                    }

                    rvRoutes.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });

        tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterRoutes(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        if (getArguments() != null && getArguments().getBoolean("hide_tabs", false)) {
            tabLayout.setVisibility(View.GONE);
        }

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

                    // Show last train designated route when 1 hour before last train
                    checkAndShowLastTrain();

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


                    // Find min time, min interchange, min fare
                    minTime = Integer.MAX_VALUE;
                    minInter = Integer.MAX_VALUE;
                    minFare = Float.MAX_VALUE;
                    for (JSONObject r : fullRouteList) {
                        try {
                            int journeyTime = getJourneyTime(r.getJSONArray("path"));
                            int inter = r.getInt("interchangeStationsNo");
                            float fare = getFare(r);

                            if (journeyTime < minTime) minTime = journeyTime;
                            if (inter < minInter) minInter = inter;
                            if (fare < minFare) minFare = fare;
                        } catch (Exception e) {
                        }
                    }

                    filterRoutes(tabLayout.getSelectedTabPosition());
                    updateBackground();
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void checkAndShowLastTrain() {
        try {
            JSONObject lastTrain = routeData.getJSONObject("lastTrain");
            String time = lastTrain.getString("time");
            String[] timeParts = time.split(":");
            int lastTrainMinutes = Integer.parseInt(timeParts[0]) * 60 + Integer.parseInt(timeParts[1]);

            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

            int diff = lastTrainMinutes - nowMinutes;
            if (diff >= 0 && diff <= 60) {
                updateNoticeCard();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateNoticeCard() {
        CardView noticeCard = getView().findViewById(R.id.notice_card);
        LinearLayout layoutSegments = getView().findViewById(R.id.layout_card_segments);
        TextView tvTitle = getView().findViewById(R.id.tv_card_title);
        TextView tvRemark = getView().findViewById(R.id.tv_card_remark);
        MaterialButton btnClose = getView().findViewById(R.id.btn_close);

        try {
            JSONObject lastTrain = routeData.getJSONObject("lastTrain");
            String time = lastTrain.getString("time");
            JSONArray interchanges = lastTrain.optJSONArray("interchange");
            JSONArray links = lastTrain.getJSONArray("links");

            tvTitle.setText("尾班車: " + time);

            layoutSegments.removeAllViews();

            String originId = getArguments().getString(RouteSearchFragment.ORIGIN_ID);
            String destId = getArguments().getString(RouteSearchFragment.DEST_ID);

            addStationViewToCard(layoutSegments, hrConf.getStationName(Integer.parseInt(originId)), false);

            int interchangePointer = 0;
            String currentStationId = originId;

            for (int i = 0; i < links.length(); i++) {
                String lineCode = links.getString(i);

                if (interchanges != null && interchangePointer < interchanges.length()) {
                    String nextId = interchanges.getString(interchangePointer);

                    if ((currentStationId.equals("3") && nextId.equals("80")) ||
                            (currentStationId.equals("80") && nextId.equals("3"))) {

                        addStationViewToCard(layoutSegments, hrConf.getStationName(Integer.parseInt(nextId)), true);
                        currentStationId = nextId;
                        interchangePointer++;
                    }
                }

                HRConfig.Line line = hrConf.getLineByAlias(lineCode);
                addTrainSegmentViewToCard(layoutSegments, line);

                if (i < links.length() - 1 && interchanges != null && interchangePointer < interchanges.length()) {
                    if (interchangePointer + 1 < interchanges.length()) {
                        String s1 = interchanges.getString(interchangePointer);
                        String s2 = interchanges.getString(interchangePointer + 1);

                        if ((s1.equals("1") && s2.equals("44")) || (s1.equals("44") && s2.equals("1"))) {
                            addStationViewToCard(layoutSegments, hrConf.getStationName(Integer.parseInt(s1)), false);
                            addStationViewToCard(layoutSegments, hrConf.getStationName(Integer.parseInt(s2)), true);
                            currentStationId = s2;
                            interchangePointer += 2;
                            continue;
                        }
                    }

                    String stationId = interchanges.getString(interchangePointer);
                    addStationViewToCard(layoutSegments, hrConf.getStationName(Integer.parseInt(stationId)), false);
                    currentStationId = stationId;
                    interchangePointer++;
                }
            }

            addStationViewToCard(layoutSegments, hrConf.getStationName(Integer.parseInt(destId)), false);

            btnClose.setOnClickListener(v -> noticeCard.setVisibility(View.GONE));
            noticeCard.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
            noticeCard.setVisibility(View.GONE);
        }
    }

    private void addStationViewToCard(LinearLayout container, String stationName, boolean walk) {
        if (walk) {
            TextView tvWalk = new TextView(requireContext());
            LinearLayout.LayoutParams walkParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            walkParams.gravity = Gravity.CENTER_VERTICAL;
            tvWalk.setLayoutParams(walkParams);

            tvWalk.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_directions_walk_24, 0, 0, 0);
            tvWalk.setCompoundDrawablePadding(0);
            tvWalk.setGravity(Gravity.CENTER);

            tvWalk.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));

            container.addView(tvWalk);
        }

        TextView tv = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        tv.setPadding(Utils.dpToPx(requireContext(), 6), 0, Utils.dpToPx(requireContext(), 6), 0);
        tv.setLayoutParams(params);
        tv.setText(stationName);
        tv.setTextSize(12);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        container.addView(tv);
    }

    private void addTrainSegmentViewToCard(LinearLayout container, HRConfig.Line line) {
        FrameLayout segmentContainer = new FrameLayout(requireContext());
        segmentContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout badgeContainer = new FrameLayout(requireContext());
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(Utils.dpToPx(requireContext(), 32), Utils.dpToPx(requireContext(), 32));
        badgeParams.gravity = Gravity.CENTER;
        badgeContainer.setLayoutParams(badgeParams);

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.RECTANGLE);
        badgeBg.setCornerRadius(Utils.dpToPx(requireContext(), 4));
        badgeBg.setColor(Color.parseColor("#" + line.color));
        badgeContainer.setBackground(badgeBg);

        TextView tvLineCode = new TextView(requireContext());
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(Utils.dpToPx(requireContext(), 26), Utils.dpToPx(requireContext(), 26));
        textParams.gravity = Gravity.CENTER;
        tvLineCode.setBackgroundColor(Color.WHITE);
        tvLineCode.setLayoutParams(textParams);
        tvLineCode.setText(line.alias);
        tvLineCode.setTextColor(Color.BLACK);
        tvLineCode.setTextSize(10);
        tvLineCode.setTypeface(null, Typeface.BOLD);
        tvLineCode.setGravity(Gravity.CENTER);
        badgeContainer.addView(tvLineCode);
        segmentContainer.addView(badgeContainer);

        container.addView(segmentContainer);
    }

    private void updateBackground() {
        if (rvHeight <= 0 || getView() == null) return;

        View bgView = getView().findViewById(R.id.layout_background_lines);
        bgView.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                Paint paint = new Paint();
                paint.setColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant));
                paint.setStrokeWidth(Utils.dpToPx(requireContext(), 1));

                int w = getBounds().width();
                int startY = Utils.dpToPx(requireContext(), 60);
                int endY = rvHeight - Utils.dpToPx(requireContext(), 60);
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

    private int getJourneyTime(JSONArray path) {
        List<VisualSegment> temp = parsePathToSegments(path, 0, 0);
        int total = 0;
        for (VisualSegment s : temp) total += s.duration;
        return total;
    }

    private void filterRoutes(int position) {
        routeList.clear();
        if (position == 0) {
            routeList.addAll(fullRouteList);
        } else {
            JSONObject best = null;
            for (JSONObject r : fullRouteList) {
                if (best == null) {
                    best = r;
                    continue;
                }
                try {
                    if (position == 1) {
                        if (r.getInt("time") < best.getInt("time")) best = r;
                    } else if (position == 2) {
                        if (r.getInt("interchangeStationsNo") < best.getInt("interchangeStationsNo"))
                            best = r;
                    } else if (position == 3) {
                        float rFare = getFare(r);
                        float bFare = getFare(best);
                        if (rFare < bFare) best = r;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (best != null) routeList.add(best);
        }
        adapter.notifyDataSetChanged();
        rvRoutes.scrollToPosition(0);
    }


    private class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {
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

                String[] texts = {"早", "樂", "安"};
                String[] colors = {"#2AB3D4", "#3DBC7F", "#FFAE0C"};
                boolean[] match = {
                        getJourneyTime(route.getJSONArray("path")) == minTime,
                        route.getInt("interchangeStationsNo") == minInter,
                        getFare(route) == minFare
                };

                for (int i = 0; i < texts.length; i++) {
                    if (match[i]) {
                        TextView tv = new TextView(getContext());
                        int size = Utils.dpToPx(requireContext(), 17);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                        lp.setMargins(Utils.dpToPx(requireContext(), 2), 0, Utils.dpToPx(requireContext(), 2), 0);
                        tv.setLayoutParams(lp);

                        tv.setText(texts[i]);
                        tv.setTextColor(Color.WHITE);
                        tv.setTextSize(9);
                        tv.setGravity(Gravity.CENTER);
                        tv.setTypeface(null, Typeface.BOLD);

                        GradientDrawable bg = new GradientDrawable();
                        bg.setShape(GradientDrawable.RECTANGLE);
                        bg.setCornerRadius(Utils.dpToPx(requireContext(), 4));
                        bg.setColor(Color.parseColor(colors[i]));
                        tv.setBackground(bg);

                        holder.layoutStatusBadges.addView(tv);
                    }
                }


                // Apply onClickListener
                holder.routeLayout.setOnClickListener(v -> {
                    RouteDetailFragment detailFragment = new RouteDetailFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("route_data", routeData.toString());
                    bundle.putInt("selected_route", position);
                    bundle.putString("start_time", String.format(Locale.getDefault(), "%02d:%02d",
                            startAt.get(Calendar.HOUR_OF_DAY), startAt.get(Calendar.MINUTE)));
                    bundle.putString("traffic_news", trafficNews);
                    detailFragment.setArguments(bundle);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.main_container, detailFragment)
                            .addToBackStack("DETAIL_PAGE")
                            .commit();
                });


                // Draw visual segments
                drawVisualSegments(holder, segments, journeyTime);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void drawVisualSegments(RouteAdapter.ViewHolder holder, List<VisualSegment> segments, int adjustedTime) {
        int verticalPadding = Utils.dpToPx(requireContext(), 14);
        int badgeSize = Utils.dpToPx(requireContext(), 32);
        int stationSize = Utils.dpToPx(requireContext(), 32);
        int walkIntSize = Utils.dpToPx(requireContext(), 36);
        int interchangeSize = Utils.dpToPx(requireContext(), 50);

        try {
            //            RecyclerView - Header reserved - Footer reserved
            int totalVisualSpan = rvHeight - Utils.dpToPx(requireContext(), 60) - Utils.dpToPx(requireContext(), 60);
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

            int neededSpaceForCircle = interchangeSize + Utils.dpToPx(requireContext(), 10);
            boolean shouldShowInterchange = availableContentPx >= Utils.dpToPx(requireContext(), 60);

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
                    circleParams.topMargin = Utils.dpToPx(requireContext(), 5);
                    circleParams.bottomMargin = Utils.dpToPx(requireContext(), 5);
                    container.addView(interchangeView, circleParams);
                }
            }

            ViewGroup.LayoutParams segmentParams = holder.layoutVisualSegments.getLayoutParams();
            segmentParams.height = containerHeight;
            holder.layoutVisualSegments.setLayoutParams(segmentParams);

            holder.layoutVisualSegments.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), Utils.dpToPx(requireContext(), 60));
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
        int badgeSize = Utils.dpToPx(requireContext(), 32);
        int stationSize = Utils.dpToPx(requireContext(), 32);
        int walkIntSize = Utils.dpToPx(requireContext(), 36);

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
            bgTvWalk.setCornerRadius(Utils.dpToPx(requireContext(), 4));
            bgTvWalk.setColor(Utils.getThemeColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant));
            tvWalk.setBackground(bgTvWalk);
            tvWalk.setPadding(Utils.dpToPx(requireContext(), 4), Utils.dpToPx(requireContext(), 2), Utils.dpToPx(requireContext(), 4), Utils.dpToPx(requireContext(), 2));
            walkContainer.addView(tvWalk);

            FrameLayout.LayoutParams walkParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, walkIntSize);
            walkParams.gravity = Gravity.CENTER_HORIZONTAL;
            segmentView.addView(walkContainer, walkParams);
        } else {
            int lineColor = Color.parseColor("#" + seg.lineColor);

            View lineBar = new View(getContext());
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(Utils.dpToPx(requireContext(), 8), ViewGroup.LayoutParams.MATCH_PARENT);
            barParams.gravity = Gravity.CENTER_HORIZONTAL;
            barParams.bottomMargin = showStation ? stationSize : 0;

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(Utils.dpToPx(requireContext(), 8));
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
            badgeBg.setCornerRadius(Utils.dpToPx(requireContext(), 4));
            badgeBg.setColor(lineColor);
            badgeContainer.setBackground(badgeBg);

            TextView tvLineCode = new TextView(getContext());
            FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(Utils.dpToPx(requireContext(), 26), Utils.dpToPx(requireContext(), 26));
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
                int size = Utils.dpToPx(requireContext(), 16);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
                params.gravity = Gravity.TOP | Gravity.START;
                params.leftMargin = -Utils.dpToPx(requireContext(), 5);
                params.topMargin = -Utils.dpToPx(requireContext(), 5);

                tvTerminus.setLayoutParams(params);
                tvTerminus.setText("始");
                tvTerminus.setTextColor(Color.WHITE);
                tvTerminus.setTextSize(9);
                tvTerminus.setGravity(Gravity.CENTER);
                tvTerminus.setTypeface(null, Typeface.BOLD);

                GradientDrawable termBg = new GradientDrawable();
                termBg.setShape(GradientDrawable.RECTANGLE);
                termBg.setCornerRadius(Utils.dpToPx(requireContext(), 6));
                termBg.setColor(Color.parseColor("#80D8FF"));
                termBg.setStroke(Utils.dpToPx(requireContext(), 1), Color.BLACK);
                tvTerminus.setBackground(termBg);

                badgeContainer.addView(tvTerminus);
            }

            if (alertLineCodes.contains(seg.lineCode)) {
                ImageView ivAlert = new ImageView(getContext());
                int size = Utils.dpToPx(requireContext(), 18);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);

                params.gravity = Gravity.TOP | Gravity.END;
                params.rightMargin = -Utils.dpToPx(requireContext(), 5);
                params.topMargin = -Utils.dpToPx(requireContext(), 5);
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