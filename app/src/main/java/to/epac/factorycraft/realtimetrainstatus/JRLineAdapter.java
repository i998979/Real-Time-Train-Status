package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

public class JRLineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_STATION = 0;
    private static final int TYPE_BETWEEN = 1;
    private static final int TYPE_PARALLEL = 2;
    private static final int TYPE_BRANCH = 3;

    private final Context context;
    private final int[] stationCodes;
    private final List<Trip> trips;

    public JRLineAdapter(Context context, int[] stationCodes, List<Trip> trips) {
        this.context = context;
        this.stationCodes = stationCodes;
        this.trips = trips;
    }

    @Override
    public int getItemViewType(int position) {
        int idx = position / 2;

        if (position % 2 == 0) {
            int code = stationCodes[idx];

            if (code == 6 || code == 7 || code == 13 || code == 14) return TYPE_PARALLEL;
            return TYPE_STATION;
        } else {
            int curr = stationCodes[idx];
            int next = stationCodes[idx + 1];

            boolean isFotRac = (curr == 5 && (next == 6 || next == 7)) || (next == 5 && (curr == 6 || curr == 7)) ||
                    (curr == 8 && (next == 6 || next == 7)) || (next == 8 && (curr == 6 || curr == 7));

            boolean isBorder = (curr == 12 && (next == 13 || next == 14)) || (next == 12 && (curr == 13 || curr == 14));

            if (isFotRac || isBorder) return TYPE_BRANCH;
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


    private final long[] TRAVEL_SECONDS = {244L, 141L, 297L, 136L, 331L, 195L, 160L, 145L, 267L, 169L, 202L, 228L, 93L};
    private final long LMC_SHS_SECONDS = 469L;

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

            updateTrainBadge(badge, trip);

            if (container instanceof android.widget.FrameLayout) {
                android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) badge.getLayoutParams();
                params.gravity = isUp ? (android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END)
                        : (android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
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

    private void updateTrainBadge(View badgeView, Trip trip) {
        double totalLoadPercentage = 0;
        int carCount = trip.listCars.size();

        for (int i = 0; i < carCount; i++) {
            Car car = trip.listCars.get(i);
            int capacity = (i == 3) ? 150 : 250;
            double load = (double) car.passengerCount / capacity;
            totalLoadPercentage += load;
        }

        double avgPercentage = totalLoadPercentage / carCount;

        View[] boys = {
                badgeView.findViewById(R.id.boy_1),
                badgeView.findViewById(R.id.boy_2),
                badgeView.findViewById(R.id.boy_3),
                badgeView.findViewById(R.id.boy_4),
                badgeView.findViewById(R.id.boy_5)
        };

        int activeColor;
        if (avgPercentage < 0.4) {
            activeColor = 0xFF00FF00;
        } else if (avgPercentage < 0.8) {
            activeColor = 0xFFFFFF00;
        } else {
            activeColor = 0xFFFF0000;
        }

        for (int i = 0; i < 5; i++) {
            double threshold = (i + 1) * 0.2;
            if (avgPercentage >= threshold) {
                boys[i].setBackgroundTintList(ColorStateList.valueOf(activeColor));
            } else {
                boys[i].setBackgroundTintList(ColorStateList.valueOf(0x33AAAAAA));
            }
        }
    }

    private void addStationRow(LinearLayout container, int stationIdx, long currentTime, int minutes, boolean isLast) {
        View row = LayoutInflater.from(context).inflate(R.layout.item_station_row, container, false);

        TextView tvTime = row.findViewById(R.id.tv_arrival_time);
        TextView tvName = row.findViewById(R.id.tv_row_station_name);
        View line = row.findViewById(R.id.view_blue_line);

        tvTime.setText(getTime(currentTime, minutes));
        tvTime.setTextColor(Color.parseColor("#4CAF50"));
        tvName.setText(Utils.getStationName(context, Utils.mapStation(stationCodes[stationIdx], "EAL"), true));

        if (isLast) {
            line.post(() -> {
                ViewGroup.LayoutParams params = line.getLayoutParams();
                params.height = row.getHeight() / 2;
                line.setLayoutParams(params);
            });
        }
        container.addView(row);
    }

    private void populateTimeline(LinearLayout container, Trip trip) {
        container.removeAllViews();

        int nextIdx = -1, destIdx = -1;
        for (int i = 0; i < stationCodes.length; i++) {
            if (stationCodes[i] == trip.nextStationCode) nextIdx = i;
            if (stationCodes[i] == trip.destinationStationCode) destIdx = i;
        }

        if (nextIdx == -1 || destIdx == -1) return;

        long totalSec = trip.ttl;
        while (isUp(trip.td) ? nextIdx >= destIdx : nextIdx <= destIdx) {
            addStationRow(container, nextIdx, System.currentTimeMillis(), (int) (totalSec / 60), nextIdx == destIdx);

            if (nextIdx != destIdx) {
                if (isUp(trip.td)) {
                    totalSec += (stationCodes[destIdx] == 14 && nextIdx == 1) ? LMC_SHS_SECONDS : TRAVEL_SECONDS[nextIdx - 1];
                    nextIdx--;
                } else {
                    totalSec += (trip.currentStationCode == 14 && nextIdx == 0) ? LMC_SHS_SECONDS : TRAVEL_SECONDS[nextIdx];
                    nextIdx++;
                }
            } else break;
        }
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
        TextView tvCrowd = itemView.findViewById(R.id.tv_crowd_level_text);
        tvCrowd.setText(avg < 100 ? "尚有座位" : avg < 200 ? "稍微擁擠" : "非常擁擠");
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

            View btnClose = dialogView.findViewById(R.id.btn_close);
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
        RecyclerView rv = dialogView.findViewById(R.id.rv_trains_list);

        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(new RecyclerView.Adapter<>() {
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

                v.findViewById(R.id.tv_line_name).setBackgroundColor(Color.parseColor(Utils.getColor(context, "EAL")));
                ((TextView) v.findViewById(R.id.tv_train_number)).setText("列車編號：" + trip.td);

                String destName = trip.destinationStationCode == -1 ? "不載客" :
                        Utils.getStationName(context, Utils.mapStation(trip.destinationStationCode, "EAL"), true);
                ((TextView) v.findViewById(R.id.tv_destination)).setText(destName + " 行");

                TextView tvType = v.findViewById(R.id.tv_service_type);
                boolean viaRacecourse = trip.td.matches(".*[BGKN].*");
                tvType.setText(viaRacecourse ? "經馬場" : "普通");
                GradientDrawable typeBg = new GradientDrawable();
                typeBg.setCornerRadius(10f);
                typeBg.setColor(viaRacecourse ? 0xCD5DE2FF : 0xFF4CAF50);
                tvType.setBackground(typeBg);

                LinearLayout crowdContainer = v.findViewById(R.id.layout_cars_container);
                updateTrainDetailsCrowd(crowdContainer, trip, v);

                View timelineContainer = v.findViewById(R.id.layout_stations_timeline);
                ImageView arrow = v.findViewById(R.id.img_fold_arrow);
                v.findViewById(R.id.layout_foldable_header).setOnClickListener(view -> {
                    boolean isVisible = timelineContainer.getVisibility() == View.VISIBLE;
                    timelineContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                    arrow.animate().rotation(isVisible ? 180f : 0f).setDuration(200).start();
                });

                LinearLayout stationRows = v.findViewById(R.id.container_station_rows);
                populateTimeline(stationRows, trip);
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
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(baseTimeMillis + (minutesToAdd * 60 * 1000L));

        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }


    private void bindStation(StationViewHolder h, int stationIdx) {
        int code = stationCodes[stationIdx];
        h.tvStationName.setText(Utils.getStationName(context, Utils.mapStation(code, "EAL"), true));

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

        updateTrainUI(upTrips, h.layoutUp, true);
        updateTrainUI(dnTrips.reversed(), h.layoutDn, false);
    }

    private void bindBranch(BranchViewHolder h, int stationIdx) {
        int currentStation = stationCodes[stationIdx];
        int nextStation = stationCodes[stationIdx + 1];

        if (currentStation == 12 && (nextStation == 13 || nextStation == 14))
            h.imgRail.setImageResource(R.drawable.rail_branch_split);
        else if (nextStation == 6 || nextStation == 7)
            h.imgRail.setImageResource(R.drawable.rail_branch_split);
        else
            h.imgRail.setImageResource(R.drawable.rail_branch_merge);

        List<Trip> upMain = new ArrayList<>(), dnMain = new ArrayList<>();
        List<Trip> upSpur = new ArrayList<>(), dnSpur = new ArrayList<>();

        for (Trip trip : trips) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;
            if (trip.trainSpeed > 0) {
                boolean isUp = isUp(trip.td);
                boolean isAtThisSegment = (isUp && trip.nextStationCode == currentStation) || (!isUp && trip.nextStationCode == nextStation);

                if (isAtThisSegment) {
                    boolean isMain = trip.currentStationCode == 6 || trip.nextStationCode == 6 || trip.currentStationCode == 13 || trip.nextStationCode == 13;
                    boolean isSpur = trip.currentStationCode == 7 || trip.nextStationCode == 7 || trip.currentStationCode == 14 || trip.nextStationCode == 14;

                    if (isMain) {
                        if (isUp) upMain.add(trip);
                        else dnMain.add(trip);
                    }
                    if (isSpur) {
                        if (isUp) upSpur.add(trip);
                        else dnSpur.add(trip);
                    }
                }
            }
        }
        updateTrainUI(upMain, h.upMain, true);
        updateTrainUI(dnMain.reversed(), h.dnMain, false);
        updateTrainUI(upSpur, h.upSpur, true);
        updateTrainUI(dnSpur.reversed(), h.dnSpur, false);
    }

    private void bindParallel(ParallelViewHolder h, int stationIdx) {
        int code = stationCodes[stationIdx];
        int mainCode = (code == 6 || code == 7) ? 6 : 13;
        int spurCode = (code == 6 || code == 7) ? 7 : 14;

        h.tvMain.setText(Utils.getStationName(context, Utils.mapStation(mainCode, "EAL"), true));
        h.tvSpur.setText(Utils.getStationName(context, Utils.mapStation(spurCode, "EAL"), true));

        List<Trip> upMain = new ArrayList<>(), dnMain = new ArrayList<>();
        List<Trip> upSpur = new ArrayList<>(), dnSpur = new ArrayList<>();

        for (Trip trip : trips) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;

            if (trip.trainSpeed == 0) {
                if (trip.currentStationCode == mainCode) {
                    if (isUp(trip.td)) upMain.add(trip);
                    else dnMain.add(trip);
                }
                if (trip.currentStationCode == spurCode) {
                    if (isUp(trip.td))
                        upSpur.add(trip);
                    else
                        dnSpur.add(trip);
                }
            }
        }
        updateTrainUI(upMain, h.upMain, true);
        updateTrainUI(dnMain.reversed(), h.dnMain, false);
        updateTrainUI(upSpur, h.upSpur, true);
        updateTrainUI(dnSpur.reversed(), h.dnSpur, false);
    }

    private void bindBetween(BetweenViewHolder h, int stationIdx) {
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

        updateTrainUI(upTrips, h.layoutUp, true);
        updateTrainUI(dnTrips.reversed(), h.layoutDn, false);
    }


    private static class StationViewHolder extends RecyclerView.ViewHolder {
        TextView tvStationName;
        ViewGroup layoutUp, layoutDn;

        StationViewHolder(View v) {
            super(v);
            tvStationName = v.findViewById(R.id.tv_station_name);
            layoutUp = v.findViewById(R.id.layout_train_up);
            layoutDn = v.findViewById(R.id.layout_train_dn);
        }
    }

    private static class BetweenViewHolder extends RecyclerView.ViewHolder {
        ViewGroup layoutUp, layoutDn;

        BetweenViewHolder(View v) {
            super(v);
            layoutUp = v.findViewById(R.id.layout_train_up);
            layoutDn = v.findViewById(R.id.layout_train_dn);
        }
    }

    private static class BranchViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRail;
        ViewGroup upMain, dnMain, upSpur, dnSpur;

        BranchViewHolder(View v) {
            super(v);
            imgRail = v.findViewById(R.id.img_branch_rail);
            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);
        }
    }

    private static class ParallelViewHolder extends RecyclerView.ViewHolder {
        ViewGroup upMain, dnMain, upSpur, dnSpur;
        TextView tvMain, tvSpur;

        ParallelViewHolder(View v) {
            super(v);
            tvMain = v.findViewById(R.id.tv_station_main);
            tvSpur = v.findViewById(R.id.tv_station_spur);
            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);
        }
    }
}