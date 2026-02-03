package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

public class JRLineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_STATION = 0;
    private static final int TYPE_BETWEEN = 1;
    private static final int TYPE_PARALLEL = 2;
    private static final int TYPE_BRANCH = 3;

    private final Context context;

    private final int[] stationCodes;
    private final String lineCode;
    private final int lineColor;
    private final List<Trip> trips;

    private HashMap<Integer, Long> runTimeUpMap;
    private HashMap<Integer, Long> runTimeDnMap;
    private HashMap<Integer, Long> dwellTimeUpMap;
    private HashMap<Integer, Long> dwellTimeDnMap;

    public JRLineAdapter(Context context, String lineCode, int[] stationCodes, List<Trip> trips,
                         HashMap<Integer, Long> runTimeUpMap, HashMap<Integer, Long> runTimeDnMap,
                         HashMap<Integer, Long> dwellTimeUpMap, HashMap<Integer, Long> dwellTimeDnMap) {
        this.context = context;

        this.lineCode = lineCode;
        this.stationCodes = stationCodes;
        int colorResId = context.getResources().getIdentifier(this.lineCode.toLowerCase(), "color", context.getPackageName());
        this.lineColor = context.getResources().getColor(colorResId, null);
        this.trips = trips;

        this.runTimeUpMap = runTimeUpMap;
        this.runTimeDnMap = runTimeDnMap;
        this.dwellTimeUpMap = dwellTimeUpMap;
        this.dwellTimeDnMap = dwellTimeDnMap;
    }

    @Override
    public int getItemViewType(int position) {
        int idx = position / 2;
        if (position % 2 == 0) {
            if (lineCode.equalsIgnoreCase("eal")) {
                int code = stationCodes[idx];
                if (code == 6 || code == 7 || code == 13 || code == 14) return TYPE_PARALLEL;
            }
            return TYPE_STATION;
        } else {
            if (lineCode.equalsIgnoreCase("eal")) {
                int curr = stationCodes[idx];
                int next = stationCodes[idx + 1];

                // 羅湖/落馬洲分叉 (12 與 13/14 之間)
                boolean isBorder = (curr == 12 && (next == 13 || next == 14)) ||
                        (next == 12 && (curr == 13 || curr == 14));

                // 火炭/馬場分叉與匯合 (5 與 6/7 之間，以及 6/7 與 8 之間)
                // 修正：增加 (curr == 6 || curr == 7) && next == 8 的判斷
                boolean isFotRac = (curr == 5 && (next == 6 || next == 7)) ||
                        (next == 5 && (curr == 6 || curr == 7)) ||
                        ((curr == 6 || curr == 7) && next == 8) ||
                        (curr == 8 && (next == 6 || next == 7));

                if (isBorder || isFotRac) return TYPE_BRANCH;
            }
            return TYPE_BETWEEN;
        }
    }

    @Override
    public int getItemCount() {
        return (stationCodes.length * 2) - 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case TYPE_STATION:
                return new StationViewHolder(inflater.inflate(R.layout.item_jr_station, parent, false));
            case TYPE_BRANCH:
                return new BranchViewHolder(inflater.inflate(R.layout.item_jr_branch, parent, false));
            case TYPE_PARALLEL:
                return new ParallelViewHolder(inflater.inflate(R.layout.item_jr_parallel, parent, false));
            default:
                return new BetweenViewHolder(inflater.inflate(R.layout.item_jr_between, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int stationIdx = position / 2;

        if (holder instanceof StationViewHolder) {
            bindStation((StationViewHolder) holder, stationIdx);
        } else if (holder instanceof ParallelViewHolder) {
            bindParallel((ParallelViewHolder) holder, stationIdx);
        } else if (holder instanceof BranchViewHolder) {
            bindBranch((BranchViewHolder) holder, stationIdx);
        } else {
            bindBetween((BetweenViewHolder) holder, stationIdx);
        }
    }


    private void updateTrainUI(List<Trip> tripsAtLocation, ViewGroup container, boolean isUp) {
        container.removeAllViews();

        if (tripsAtLocation.isEmpty()) {
            container.setVisibility(View.INVISIBLE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < tripsAtLocation.size(); i++) {
            Trip trip = tripsAtLocation.get(i);

            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;

            View badge = inflater.inflate(isUp ? R.layout.train_badge_up : R.layout.train_badge_dn, container, false);

            TextView tvId = badge.findViewById(isUp ? R.id.tv_train_id_up : R.id.tv_train_id_dn);
            if (tvId != null) tvId.setText(trip.td);

            updateBadgeLoad(badge, trip);

            if (container instanceof FrameLayout) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) badge.getLayoutParams();
                params.gravity = isUp ? (Gravity.CENTER_VERTICAL | Gravity.END) : (Gravity.CENTER_VERTICAL | Gravity.START);
                badge.setLayoutParams(params);
            }

            float offset = i * 15f;
            badge.setTranslationX(isUp ? -offset : offset);
            badge.setTranslationY(isUp ? -offset : offset);

            badge.setElevation((tripsAtLocation.size() - i) * 5f);

            badge.setOnClickListener(v -> {
                showTrainsDetailDialog(tripsAtLocation);
            });

            container.addView(badge);
        }
    }

    private void updateBadgeLoad(View badgeView, Trip trip) {
        double totalPercent = 0;
        int carCount = trip.listCars.size();

        for (int i = 0; i < carCount; i++) {
            Car car = trip.listCars.get(i);
            int capacity = (i == 3) ? 150 : 250;
            double load = (double) car.passengerCount / capacity;
            totalPercent += load;
        }

        double avgPercent = totalPercent / carCount;

        View[] boys = {
                badgeView.findViewById(R.id.crowd_1),
                badgeView.findViewById(R.id.crowd_2),
                badgeView.findViewById(R.id.crowd_3),
                badgeView.findViewById(R.id.crowd_4),
                badgeView.findViewById(R.id.crowd_5)
        };

        int color;
        if (avgPercent < 0.4) {
            color = 0xFF00FF00;
        } else if (avgPercent < 0.8) {
            color = 0xFFFFFF00;
        } else {
            color = 0xFFFF0000;
        }

        for (int i = 0; i < 5; i++) {
            double threshold = (i + 1) * 0.2;
            if (avgPercent >= threshold) {
                boys[i].setBackgroundTintList(ColorStateList.valueOf(color));
            } else {
                boys[i].setBackgroundTintList(ColorStateList.valueOf(0x33AAAAAA));
            }
        }
    }

    private void addStationRow(LinearLayout container, int stationCode, long currentTime, int minutes, boolean isLast) {
        View row = LayoutInflater.from(context).inflate(R.layout.item_station_row, container, false);

        TextView tvArrvTime = row.findViewById(R.id.tv_arrival_time);
        TextView tvStaName = row.findViewById(R.id.tv_row_station_name);
        View line = row.findViewById(R.id.tv_line);
        line.setBackgroundColor(lineColor);

        tvArrvTime.setText(getTime(currentTime, minutes));
        tvArrvTime.setTextColor(Color.parseColor("#4CAF50"));

        tvStaName.setText(Utils.getStationName(context, Utils.mapStation(stationCode, lineCode), true));

        if (isLast) {
            line.post(() -> {
                ViewGroup.LayoutParams params = line.getLayoutParams();
                params.height = row.getHeight() / 2;
                line.setLayoutParams(params);
            });
        }
        container.addView(row);
    }

    private int populateTimeline(LinearLayout container, Trip trip) {
        container.removeAllViews();
        boolean isUp = isUp(trip.td);
        int destCode = trip.destinationStationCode;

        List<Integer> route = new ArrayList<>();
        boolean shouldAdd = false;

        if (isUp) {
            for (int i = stationCodes.length - 1; i >= 0; i--) {
                int code = stationCodes[i];

                int effectiveCode = (code == 13 && destCode == 14) ? 14 : code;

                if (effectiveCode == trip.nextStationCode) shouldAdd = true;

                if (shouldAdd) {
                    route.add(effectiveCode);
                    if (effectiveCode == destCode) break;
                }
            }
        } else {
            for (int code : stationCodes) {
                if (trip.currentStationCode == 14 && code == 13) continue;
                if (trip.currentStationCode == 13 && code == 14) continue;

                if (code == trip.nextStationCode) shouldAdd = true;

                if (shouldAdd) {
                    route.add(code);
                    if (code == destCode) break;
                }
            }
        }

        if (route.isEmpty()) return 0;


        long nowMillis = System.currentTimeMillis();
        long totalSec;

        if (trip.trainSpeed <= 3.0) {
            totalSec = isUp ? runTimeUpMap.getOrDefault(trip.nextStationCode, 120L)
                    : runTimeDnMap.getOrDefault(trip.currentStationCode, 120L);
        } else {
            totalSec = (long) (trip.targetDistance / (trip.trainSpeed / 3.6));
        }

        for (int k = 0; k < route.size(); k++) {
            int currentCode = route.get(k);
            addStationRow(container, currentCode, nowMillis, (int) (totalSec / 60), k == route.size() - 1);

            if (k < route.size() - 1) {
                int nextCode = route.get(k + 1);

                totalSec += isUp ? dwellTimeUpMap.getOrDefault(currentCode, 35L)
                        : dwellTimeDnMap.getOrDefault(currentCode, 35L);

                totalSec += isUp ? runTimeUpMap.getOrDefault(nextCode, 120L)
                        : runTimeDnMap.getOrDefault(currentCode, 120L);
            }
        }

        return route.size();
    }

    private void updateTrainDetailsCrowd(LinearLayout container, Trip trip, View itemView) {
        container.removeAllViews();

        float density = context.getResources().getDisplayMetrics().density;
        int totalLoad = 0;
        int carCount = trip.listCars.size();

        for (int i = 0; i < carCount; i++) {
            int idx = isUp(trip.td) ? i : (carCount - 1 - i);
            Car car = trip.listCars.get(idx);
            totalLoad += car.passengerCount;

            View carView = new View(context);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams((int) (24 * density), (int) (26 * density));
            p.setMargins((int) (2 * density), 0, (int) (2 * density), 0);
            carView.setLayoutParams(p);

            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(4 * density);

            boolean isFirstClass = (idx == 3);
            int color;
            if (isFirstClass) {
                color = (car.passengerCount < 70) ? 0xFF00FF00 : (car.passengerCount < 150) ? 0xFFFFFF00 : 0xFFFF0000;
                gd.setStroke((int) (3 * density), 0xFFFFA500);
            } else {
                color = (car.passengerCount < 110) ? 0xFF00FF00 : (car.passengerCount < 250) ? 0xFFFFFF00 : 0xFFFF0000;
            }
            gd.setColor(color);
            carView.setBackground(gd);

            container.addView(carView);
        }

        int avg = totalLoad / carCount;
        TextView tvCrowdLvl = itemView.findViewById(R.id.tv_crowd_level);
        tvCrowdLvl.setText(avg < 100 ? "尚有座位" : avg < 200 ? "稍微擁擠" : "非常擁擠");
    }

    private void setupFullHeightBottomSheet(BottomSheetDialog dialog, View dialogView) {
        View bottomSheet = (View) dialogView.getParent();
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.setLayoutParams(layoutParams);

            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);

            ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
            btnClose.setOnClickListener(v -> dialog.dismiss());

            behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dialog.dismiss();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            });
        }
    }

    private void showTrainsDetailDialog(List<Trip> tripsAtLocation) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bottom_sheet_container, null);
        RecyclerView trainsList = dialogView.findViewById(R.id.rv_trains_list);

        trainsList.setLayoutManager(new LinearLayoutManager(context));
        trainsList.setAdapter(new RecyclerView.Adapter<>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                return new RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_train_detail_card, p, false)) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Trip trip = tripsAtLocation.get(position);
                View v = holder.itemView;

                TextView tvLine = v.findViewById(R.id.tv_line_name);
                tvLine.setBackgroundColor(lineColor);
                tvLine.setText(Utils.getLineName(lineCode, true));

                ((TextView) v.findViewById(R.id.tv_train_number)).setText("列車編號：" + trip.td);

                TextView tvDest = v.findViewById(R.id.tv_destination);
                if (trip.destinationStationCode == -1) {
                    tvDest.setText("不載客列車");
                } else {
                    String destName = Utils.getStationName(context, Utils.mapStation(trip.destinationStationCode, lineCode), true);
                    tvDest.setText(destName + " 行");
                }

                TextView tvSvcType = v.findViewById(R.id.tv_service_type);
                boolean viaRacecourse = lineCode.equalsIgnoreCase("eal") && trip.td.matches(".*[BGKN].*");
                tvSvcType.setText(viaRacecourse ? "經馬場" : "普通");
                GradientDrawable typeBg = new GradientDrawable();
                typeBg.setCornerRadius(10f);
                typeBg.setColor(viaRacecourse ? 0xCD5DE2FF : 0xFF4CAF50);
                tvSvcType.setBackground(typeBg);

                LinearLayout crowdContainer = v.findViewById(R.id.crowd_container);
                updateTrainDetailsCrowd(crowdContainer, trip, v);


                View header = v.findViewById(R.id.station_header);
                ImageView arrow = v.findViewById(R.id.fold_arrow);

                LinearLayout stationRows = v.findViewById(R.id.station_rows);
                View timeLine = v.findViewById(R.id.station_timeline);

                int stationCount = populateTimeline(stationRows, trip);
                if (stationCount == 0) {
                    header.setVisibility(View.GONE);
                    timeLine.setVisibility(View.GONE);
                } else {
                    header.setVisibility(View.VISIBLE);

                    if (tripsAtLocation.size() == 1) {
                        timeLine.setVisibility(View.VISIBLE);
                        arrow.setRotation(0f);
                    } else {
                        timeLine.setVisibility(View.GONE);
                        arrow.setRotation(180f);
                    }

                    header.setOnClickListener(view -> {
                        boolean isVisible = timeLine.getVisibility() == View.VISIBLE;

                        timeLine.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                        arrow.animate().rotation(isVisible ? 180f : 0f).setDuration(200).start();
                    });
                }
            }

            @Override
            public int getItemCount() {
                return tripsAtLocation.size();
            }
        });

        dialog.setContentView(dialogView);
        setupFullHeightBottomSheet(dialog, dialogView);
        dialog.show();
    }


    private boolean isUp(String td) {
        return Character.getNumericValue(td.charAt(td.length() - 1)) % 2 != 0;
    }

    private String getTime(long baseTimeMillis, int minutesToAdd) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        cal.setTimeInMillis(baseTimeMillis + (minutesToAdd * 60 * 1000L));

        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }


    private void bindStation(StationViewHolder h, int stationIdx) {
        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        int code = stationCodes[stationIdx];
        h.tvStationName.setText(Utils.getStationName(context, Utils.mapStation(code, lineCode), true));

        List<Trip> upTrips = new ArrayList<>();
        List<Trip> dnTrips = new ArrayList<>();

        for (Trip trip : trips) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;

            if (trip.trainSpeed == 0 && trip.currentStationCode == code) {
                if (isUp(trip.td))
                    upTrips.add(trip);
                else
                    dnTrips.add(trip);
            }
        }

        upTrips.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnTrips.sort(Comparator.comparingDouble(t -> t.targetDistance));

        updateTrainUI(upTrips, h.layoutUp, true);
        updateTrainUI(dnTrips, h.layoutDn, false);
    }

    private void bindBranch(BranchViewHolder h, int stationIdx) {
        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        int currentSector = stationCodes[stationIdx];
        int nextSector = stationCodes[stationIdx + 1];

        if (currentSector == 12 && (nextSector == 13 || nextSector == 14))
            h.imgRail.setImageResource(R.drawable.rail_branch_split);
        else if ((nextSector == 6 || nextSector == 7))
            h.imgRail.setImageResource(R.drawable.rail_branch_split);
        else
            h.imgRail.setImageResource(R.drawable.rail_branch_merge);


        List<Trip> upMain = new ArrayList<>(), dnMain = new ArrayList<>();
        List<Trip> upSpur = new ArrayList<>(), dnSpur = new ArrayList<>();

        for (Trip trip : trips) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;
            if (trip.trainSpeed <= 0) continue;

            boolean isUp = isUp(trip.td);
            boolean isAtThisSegment = false;

            if (isUp) {
                if (currentSector == 13 && nextSector == 12) {
                    if (trip.nextStationCode == 13 || trip.nextStationCode == 14)
                        isAtThisSegment = true;
                } else if (currentSector == 8 && nextSector == 6) {
                    if (trip.nextStationCode == 8) isAtThisSegment = true;
                } else if (currentSector == 6 && nextSector == 5) {
                    if (trip.nextStationCode == 6 || trip.nextStationCode == 7)
                        isAtThisSegment = true;
                }
            } else {
                if (currentSector == 13 && nextSector == 12) {
                    if ((trip.currentStationCode == 13 || trip.currentStationCode == 14) && trip.nextStationCode == 12) {
                        isAtThisSegment = true;
                    }
                } else if (currentSector == 8 && nextSector == 6) {
                    if (trip.currentStationCode == 8 && (trip.nextStationCode == 6 || trip.nextStationCode == 7)) {
                        isAtThisSegment = true;
                    }
                } else if (currentSector == 6 && nextSector == 5) {
                    if ((trip.currentStationCode == 6 || trip.currentStationCode == 7) && trip.nextStationCode == 5) {
                        isAtThisSegment = true;
                    }
                }
            }

            if (isAtThisSegment) {
                boolean isMain = (trip.nextStationCode == 13 || trip.currentStationCode == 13 ||
                        trip.nextStationCode == 6 || trip.currentStationCode == 6);
                if (isMain) {
                    if (isUp)
                        upMain.add(trip);
                    else
                        dnMain.add(trip);
                } else {
                    if (isUp)
                        upSpur.add(trip);
                    else
                        dnSpur.add(trip);
                }
            }
        }

        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        upSpur.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnSpur.sort(Comparator.comparingDouble(t -> t.targetDistance));

        updateTrainUI(upMain, h.upMain, true);
        updateTrainUI(dnMain, h.dnMain, false);
        updateTrainUI(upSpur, h.upSpur, true);
        updateTrainUI(dnSpur, h.dnSpur, false);
    }

    private void bindParallel(ParallelViewHolder h, int stationIdx) {
        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));
        h.railLine2.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        int code = stationCodes[stationIdx];
        int mainCode = (code == 6 || code == 7) ? 6 : 13;
        int spurCode = (code == 6 || code == 7) ? 7 : 14;

        h.tvMain.setText(Utils.getStationName(context, Utils.mapStation(mainCode, lineCode), true));
        h.tvSpur.setText(Utils.getStationName(context, Utils.mapStation(spurCode, lineCode), true));

        List<Trip> upMain = new ArrayList<>(), dnMain = new ArrayList<>();
        List<Trip> upSpur = new ArrayList<>(), dnSpur = new ArrayList<>();

        for (Trip trip : trips) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;

            if (trip.trainSpeed == 0) {
                if (trip.currentStationCode == mainCode) {
                    if (isUp(trip.td))
                        upMain.add(trip);
                    else
                        dnMain.add(trip);
                }
                if (trip.currentStationCode == spurCode) {
                    if (isUp(trip.td))
                        upSpur.add(trip);
                    else
                        dnSpur.add(trip);
                }
            }
        }

        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        upSpur.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnSpur.sort(Comparator.comparingDouble(t -> t.targetDistance));

        updateTrainUI(upMain, h.upMain, true);
        updateTrainUI(dnMain, h.dnMain, false);
        updateTrainUI(upSpur, h.upSpur, true);
        updateTrainUI(dnSpur, h.dnSpur, false);
    }

    private void bindBetween(BetweenViewHolder h, int stationIdx) {
        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        int currentStation = stationCodes[stationIdx];
        int nextStation = stationCodes[stationIdx + 1];

        List<Trip> upTrips = new ArrayList<>();
        List<Trip> dnTrips = new ArrayList<>();

        for (Trip trip : trips) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;

            if (trip.trainSpeed > 0) {
                if (isUp(trip.td) && trip.nextStationCode == currentStation)
                    upTrips.add(trip);
                else if (!isUp(trip.td) && trip.nextStationCode == nextStation)
                    dnTrips.add(trip);
            }
        }

        upTrips.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnTrips.sort(Comparator.comparingDouble(t -> t.targetDistance));

        updateTrainUI(upTrips, h.layoutUp, true);
        updateTrainUI(dnTrips, h.layoutDn, false);
    }


    private static class StationViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        TextView tvStationName;
        ViewGroup layoutUp, layoutDn;

        StationViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            tvStationName = v.findViewById(R.id.tv_station_name);
            layoutUp = v.findViewById(R.id.layout_train_up);
            layoutDn = v.findViewById(R.id.layout_train_dn);
        }
    }

    private static class BetweenViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        ViewGroup layoutUp, layoutDn;

        BetweenViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            layoutUp = v.findViewById(R.id.layout_train_up);
            layoutDn = v.findViewById(R.id.layout_train_dn);
        }
    }

    private static class BranchViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        ImageView imgRail;
        ViewGroup upMain, dnMain, upSpur, dnSpur;

        BranchViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            imgRail = v.findViewById(R.id.img_branch_rail);
            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);
        }
    }

    private static class ParallelViewHolder extends RecyclerView.ViewHolder {
        View railLine, railLine2;
        ViewGroup upMain, dnMain, upSpur, dnSpur;
        TextView tvMain, tvSpur;

        ParallelViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            railLine2 = v.findViewById(R.id.rail_line2);
            tvMain = v.findViewById(R.id.tv_station_main);
            tvSpur = v.findViewById(R.id.tv_station_spur);
            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);
        }
    }
}