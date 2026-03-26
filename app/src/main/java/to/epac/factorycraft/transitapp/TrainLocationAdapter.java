package to.epac.factorycraft.transitapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TrainLocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_STATION = 0;
    private static final int TYPE_PARALLEL = 1;
    private static final int TYPE_PARALLEL_LEFT_STATION = 2;
    private static final int TYPE_BETWEEN = 3;
    private static final int TYPE_BRANCH = 4;
    private static final int TYPE_PARALLEL_BETWEEN = 5;

    private final Context context;

    private final int[] stationCodes;
    private final String lineCode;
    private final int lineColor;
    private final List<Trip> trips;

    private final HashMap<Integer, Long> runTimeUpMap;
    private final HashMap<Integer, Long> runTimeDnMap;
    private final HashMap<Integer, Long> dwellTimeUpMap;
    private final HashMap<Integer, Long> dwellTimeDnMap;

    public TrainLocationAdapter(Context context, String lineCode, int[] stationCodes, List<Trip> trips,
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
            int code = stationCodes[idx];

            if (lineCode.equalsIgnoreCase("eal")) {
                // LOW/LMC FOT/RAC
                if (code == 6 || code == 7 || code == 13 || code == 14) return TYPE_PARALLEL;
            }
            if (lineCode.equalsIgnoreCase("tkl")) {
                // POA/LHP
                if (code == 131 || code == 132) return TYPE_PARALLEL;
                //  HAH
                if (code == 133) return TYPE_PARALLEL_LEFT_STATION;
            }
            return TYPE_STATION;
        } else {
            if (lineCode.equalsIgnoreCase("eal")) {
                int curr = stationCodes[idx];
                int next = stationCodes[idx + 1];

                boolean isBorder = (curr == 12 && (next == 13 || next == 14)) ||
                        (next == 12 && (curr == 13 || curr == 14));

                boolean isFotRac = (curr == 5 && (next == 6 || next == 7)) ||
                        (next == 5 && (curr == 6 || curr == 7)) ||
                        ((curr == 6 || curr == 7) && next == 8) ||
                        (curr == 8 && (next == 6 || next == 7));

                if (isBorder || isFotRac) return TYPE_BRANCH;
            } else if (lineCode.equalsIgnoreCase("tkl")) {
                int curr = stationCodes[idx];
                int next = stationCodes[idx + 1];

                if ((curr == 132 && next == 133) || curr == 133 && next == 132)
                    return TYPE_PARALLEL_BETWEEN;

                if (((curr == 131 || curr == 133) && next == 134) || curr == 134 && (next == 131 || next == 133))
                    return TYPE_BRANCH;
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
            case TYPE_PARALLEL:
                return new ParallelViewHolder(inflater.inflate(R.layout.item_jr_parallel, parent, false));
            case TYPE_PARALLEL_LEFT_STATION:
                return new ParallelLeftStationViewHolder(inflater.inflate(R.layout.item_jr_parallel_left_station, parent, false));
            case TYPE_BETWEEN:
                return new BetweenViewHolder(inflater.inflate(R.layout.item_jr_between, parent, false));
            case TYPE_BRANCH:
                return new BranchViewHolder(inflater.inflate(R.layout.item_jr_branch, parent, false));
            case TYPE_PARALLEL_BETWEEN:
                return new ParallelBetweenViewHolder(inflater.inflate(R.layout.item_jr_parallel_between, parent, false));
            default:
                return new BetweenViewHolder(inflater.inflate(R.layout.item_jr_between, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int stationIdx = position / 2;

        if (holder instanceof StationViewHolder) {
            bindStation((StationViewHolder) holder, stationIdx);
        }
        if (holder instanceof ParallelViewHolder) {
            bindParallel((ParallelViewHolder) holder, stationIdx);
        }
        if (holder instanceof ParallelLeftStationViewHolder) {
            bindParallelLeftStation((ParallelLeftStationViewHolder) holder, stationIdx);
        }
        if (holder instanceof BetweenViewHolder) {
            bindBetween((BetweenViewHolder) holder, stationIdx);
        }
        if (holder instanceof BranchViewHolder) {
            bindBranch((BranchViewHolder) holder, stationIdx);
        }
        if (holder instanceof ParallelBetweenViewHolder) {
            bindParallelBetween((ParallelBetweenViewHolder) holder, stationIdx);
        }
    }


    private void showTrainBadge(List<Trip> tripsAtLocation, ViewGroup container, boolean isUp) {
        container.removeAllViews();

        if (tripsAtLocation.isEmpty()) {
            container.setVisibility(View.INVISIBLE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < Math.min(3, tripsAtLocation.size()); i++) {
            Trip trip = tripsAtLocation.get(i);

            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;

            View badge = inflater.inflate(isUp ? R.layout.train_badge_up : R.layout.train_badge_dn, container, false);
            View badgeView = badge.findViewById(isUp ? R.id.train_up : R.id.train_dn);

            LayerDrawable layers = (LayerDrawable) badgeView.getBackground().mutate();
            Drawable headerLayer = layers.findDrawableByLayerId(R.id.line_color_layer);
            headerLayer.setTint(lineColor);
            badgeView.setBackground(layers);

            ImageView trainIcon = badge.findViewById(R.id.img_train_icon);
            switch (lineCode.toLowerCase()) {
                case "eal":
                    trainIcon.setImageResource(R.drawable.r_train);
                    break;
                case "tml":
                    trainIcon.setImageResource(R.drawable.sp1900);
                    try {
                        if (trip.trainId != null && Integer.parseInt(trip.trainId) >= 397) {
                            trainIcon.setImageResource(R.drawable.t1141a);
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    break;
                case "ael":
                    trainIcon.setImageResource(R.drawable.ael_train);
                    break;
                case "drl":
                    trainIcon.setImageResource(R.drawable.drl_train);
                    break;
                case "tcl":
                    trainIcon.setImageResource(R.drawable.caf_train);
                    break;
                case "sil":
                    trainIcon.setImageResource(R.drawable.s_train);
                    break;
                case "ktl":
                case "isl":
                case "tkl":
                case "twl":
                default:
                    trainIcon.setImageResource(R.drawable.m_train);
                    break;
            }

            TextView tvId = badge.findViewById(R.id.tv_train_id);
            if (trip.destinationStationCode == -1 || trip.destinationStationCode == 91 || trip.destinationStationCode == 92) {
                tvId.setText("不載客");
            } else {
                String destName = Utils.getStationName(Utils.idToCode(lineCode, trip.destinationStationCode), true);
                boolean viaRacecourse = lineCode.equalsIgnoreCase("eal") && trip.td.matches(".*[BGKN].*");
                tvId.setText((viaRacecourse ? "經馬場" : "普通") + "・" + destName);
            }

            TextView tvCar = badge.findViewById(R.id.tv_car);
            tvCar.setText(lineCode.equalsIgnoreCase("eal") ? "9両"
                    : lineCode.equalsIgnoreCase("drl") ? "4両"
                    : lineCode.equalsIgnoreCase("sil") ? "3両" : "8両");


            float offset = i * 15f;
            badge.setTranslationX(isUp ? -offset : offset);
            badge.setTranslationY(isUp ? -offset : offset);

            if (container instanceof FrameLayout) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) badge.getLayoutParams();
                params.gravity = isUp ? (Gravity.CENTER_VERTICAL | Gravity.END) : (Gravity.CENTER_VERTICAL | Gravity.START);
                badge.setLayoutParams(params);
            }

            badge.setElevation((tripsAtLocation.size() - i) * 5f);

            badge.setOnClickListener(v -> {
                showTrainsDetailDialog(tripsAtLocation);
            });

            container.addView(badge);
        }
    }

    // TODO: If DN does not terminate at ADM, it still show time to ADM
    private int updateTimeline(LinearLayout container, Trip trip) {
        container.removeAllViews();

        boolean isViaRacecourse = lineCode.equalsIgnoreCase("eal") &&
                (trip.td.matches(".*[BGKN].*") || trip.destinationStationCode == 7);

        List<Integer> route = new ArrayList<>();
        boolean shouldAdd = false;

        if (isUp(trip)) {
            for (int i = stationCodes.length - 1; i >= 0; i--) {
                int code = stationCodes[i];

                int displayCode = code;
                if (isViaRacecourse && code == 6) displayCode = 7;
                if (trip.destinationStationCode == 14 && code == 13) displayCode = 14;

                if (displayCode == trip.nextStationCode) shouldAdd = true;
                if (shouldAdd) {
                    route.add(displayCode);
                    if (displayCode == trip.destinationStationCode) break;
                }
            }
        } else {
            for (int code : stationCodes) {
                int displayCode = code;
                if (isViaRacecourse && code == 6) displayCode = 7;

                if (displayCode == trip.nextStationCode) shouldAdd = true;
                if (shouldAdd) {
                    route.add(displayCode);
                    if (displayCode == trip.destinationStationCode) break;
                }
            }
        }

        if (route.isEmpty()) return 0;


        long nowMillis = !trip.isOpenData ? System.currentTimeMillis() : trip.time;
        long totalSec;

        if (trip.trainSpeed <= 3.0) {
            totalSec = isUp(trip) ? runTimeUpMap.getOrDefault(trip.nextStationCode, 120L)
                    : runTimeDnMap.getOrDefault(trip.currentStationCode, 120L);
        } else {
            totalSec = (long) (trip.targetDistance / (trip.trainSpeed / 3.6));
        }

        for (int k = 0; k < route.size(); k++) {
            int currentCode = route.get(k);
            addTimelineStation(container, currentCode, nowMillis, (int) (totalSec / 60), k == route.size() - 1);

            if (k < route.size() - 1) {
                int nextCode = route.get(k + 1);

                totalSec += isUp(trip) ? dwellTimeUpMap.getOrDefault(currentCode, 35L)
                        : dwellTimeDnMap.getOrDefault(currentCode, 35L);

                totalSec += isUp(trip) ? runTimeUpMap.getOrDefault(nextCode, 120L)
                        : runTimeDnMap.getOrDefault(currentCode, 120L);
            }
        }

        return route.size();
    }

    private void addTimelineStation(LinearLayout container, int stationCode, long currentTime, int minutes, boolean isLast) {
        View row = LayoutInflater.from(context).inflate(R.layout.item_station_row, container, false);

        TextView tvArrvTime = row.findViewById(R.id.tv_arrival_time);
        TextView tvStaName = row.findViewById(R.id.tv_row_station_name);
        View topHalf = row.findViewById(R.id.line_half_top);
        View bottomHalf = row.findViewById(R.id.line_half_bottom);

        topHalf.setBackgroundColor(lineColor);
        bottomHalf.setBackgroundColor(lineColor);

        tvArrvTime.setText(Instant.ofEpochMilli(currentTime + minutes * 60000L)
                .atZone(ZoneId.of("GMT+8"))
                .format(DateTimeFormatter.ofPattern("HH:mm")));
        tvArrvTime.setTextColor(ContextCompat.getColor(context, R.color.button_green));

        tvStaName.setText(Utils.getStationName(context, Utils.idToCode(context, lineCode, stationCode), true));

        if (isLast) {
            bottomHalf.setVisibility(View.INVISIBLE);
        } else {
            bottomHalf.setVisibility(View.VISIBLE);
        }
        container.addView(row);
    }

    private void updateTrainCrowd(ViewGroup layout, Trip trip, View itemView) {
        LinearLayout crowdContainer = layout.findViewById(R.id.crowd_container);
        crowdContainer.removeAllViews();

        TextView tvCrowdLvl = itemView.findViewById(R.id.tv_crowd_level);

        float density = context.getResources().getDisplayMetrics().density;
        int totalLoad = 0;
        int carCount = trip.listCars.size();

        if (carCount == 0) {
            tvCrowdLvl.setGravity(Gravity.CENTER);
            tvCrowdLvl.setText("未能提供混雜情報");
            TextView tvCrowdMsg = new TextView(context);
            tvCrowdMsg.setGravity(Gravity.CENTER);
            tvCrowdMsg.setTextSize(12);
            tvCrowdMsg.setText("※本班車未提供混雜情報");
            layout.addView(tvCrowdMsg);
        } else {
            for (int i = 0; i < carCount; i++) {
                int idx = isUp(trip) ? i : (carCount - 1 - i);
                Car car = trip.listCars.get(idx);
                totalLoad += car.passengerCount;

                View carView = new View(context);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams((int) (24 * density), (int) (26 * density));
                p.setMargins((int) (2 * density), 0, (int) (2 * density), 0);
                carView.setLayoutParams(p);

                GradientDrawable gd = new GradientDrawable();
                gd.setCornerRadius(4 * density);

                boolean isFirstClass = lineCode.equalsIgnoreCase("eal") && idx == 3;
                int color;
                if (isFirstClass) {
                    color = (car.passengerCount < 70) ? 0xFF00FF00 : (car.passengerCount < 150) ? 0xFFFFFF00 : 0xFFFF0000;
                    gd.setStroke((int) (3 * density), 0xFFFFA500);
                } else {
                    color = (car.passengerCount < 110) ? 0xFF00FF00 : (car.passengerCount < 250) ? 0xFFFFFF00 : 0xFFFF0000;
                }
                gd.setColor(color);
                carView.setBackground(gd);

                crowdContainer.addView(carView);
            }
            int avg = totalLoad / carCount;
            tvCrowdLvl.setText(avg < 100 ? "尚有座位" : avg < 200 ? "稍微擁擠" : "非常擁擠");
        }
    }

    private void showTrainsDetailDialog(List<Trip> tripsAtLocation) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_train_detail, null);
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

                TextView tvLine = v.findViewById(R.id.tv_line);
                tvLine.setBackgroundColor(lineColor);
                tvLine.setText(Utils.getLineName(lineCode, true));

                ImageView trainIcon = v.findViewById(R.id.img_train_icon);
                switch (lineCode.toLowerCase()) {
                    case "eal":
                        trainIcon.setImageResource(R.drawable.r_train);
                        break;
                    case "tml":
                        trainIcon.setImageResource(R.drawable.sp1900);
                        try {
                            if (trip.trainId != null && Integer.parseInt(trip.trainId) >= 397) {
                                trainIcon.setImageResource(R.drawable.t1141a);
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "ael":
                        trainIcon.setImageResource(R.drawable.ael_train);
                        break;
                    case "drl":
                        trainIcon.setImageResource(R.drawable.drl_train);
                        break;
                    case "tcl":
                        trainIcon.setImageResource(R.drawable.caf_train);
                        break;
                    case "sil":
                        trainIcon.setImageResource(R.drawable.s_train);
                        break;
                    case "ktl":
                    case "isl":
                    case "tkl":
                    case "twl":
                    default:
                        trainIcon.setImageResource(R.drawable.m_train);
                        break;
                }

                TextView tvDest = v.findViewById(R.id.tv_destination);
                if (trip.destinationStationCode == -1 || trip.destinationStationCode == 91 || trip.destinationStationCode == 92) {
                    tvDest.setText("不載客列車");
                } else {
                    String destName = Utils.getStationName(Utils.idToCode(lineCode, trip.destinationStationCode), true);
                    tvDest.setText(destName + " 行");
                }

                TextView tvCarCount = v.findViewById(R.id.tv_car_count);
                tvCarCount.setText(lineCode.equalsIgnoreCase("eal") ? "9両"
                        : lineCode.equalsIgnoreCase("drl") ? "4両"
                        : lineCode.equalsIgnoreCase("sil") ? "3両" : "8両");

                TextView tvSvcType = v.findViewById(R.id.tv_service_type);
                boolean viaRacecourse = lineCode.equalsIgnoreCase("eal") && trip.td.matches(".*[BGKN].*");
                tvSvcType.setText(viaRacecourse ? "經馬場" : "普通");
                GradientDrawable typeBg = new GradientDrawable();
                typeBg.setCornerRadius(10f);
                typeBg.setColor(viaRacecourse ? 0x2C6483FF : ContextCompat.getColor(context, R.color.button_green));
                tvSvcType.setBackground(typeBg);

                if (trip.isOpenData) {
                    v.findViewById(R.id.tv_train_consist).setVisibility(View.GONE);
                    v.findViewById(R.id.tv_train_number).setVisibility(View.GONE);
                }
                CardView crowdLayout = v.findViewById(R.id.crowd_layout);
                updateTrainCrowd(crowdLayout, trip, v);


                View header = v.findViewById(R.id.station_header);
                ImageView arrow = v.findViewById(R.id.fold_arrow);

                LinearLayout stationRows = v.findViewById(R.id.station_rows);
                View timeLine = v.findViewById(R.id.station_timeline);

                int stationCount = updateTimeline(stationRows, trip);
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
        setupDialog(dialog, dialogView);
        dialog.show();
    }

    private void setupDialog(BottomSheetDialog dialog, View dialogView) {
        View bottomSheet = (View) dialogView.getParent();
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.setLayoutParams(layoutParams);

            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);

            MaterialButton btnClose = dialogView.findViewById(R.id.btn_close);
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


    private boolean isUp(Trip trip) {
        if (trip.isOpenData) return trip.isUp;

        return false;
    }

    private void bindStation(StationViewHolder h, int stationIdx) {
        int code = stationCodes[stationIdx];

        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        h.tvStation.setText(Utils.getStationName(Utils.idToCode(lineCode, code), true));
        h.tvStation.setTag(Utils.idToCode(lineCode, code));
        h.tvStation.setOnClickListener(v -> {
            String stationCode = (String) v.getTag();

            Intent intent = new Intent(v.getContext(), StationActivity.class);
            intent.putExtra("station_code", stationCode);
            v.getContext().startActivity(intent);
        });


        List<Trip> upTrips = new ArrayList<>();
        List<Trip> dnTrips = new ArrayList<>();
        for (Trip trip : trips) {
            // Next Train
            if (trip.isOpenData) {
                if (trip.ttnt > 0) continue;

                if (trip.nextStationCode == code) {
                    if (isUp(trip))
                        upTrips.add(trip);
                    else
                        dnTrips.add(trip);
                }
            }
        }
        upTrips.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnTrips.sort(Comparator.comparingDouble(t -> t.targetDistance));

        showTrainBadge(upTrips, h.layoutUp, true);
        showTrainBadge(dnTrips, h.layoutDn, false);
    }

    private void bindParallel(ParallelViewHolder h, int stationIdx) {
        int code = stationCodes[stationIdx];
        int mainCode = code;
        if (lineCode.equalsIgnoreCase("eal")) mainCode = (code == 6 || code == 7) ? 6 : 13;
        int spurCode = code;
        if (lineCode.equalsIgnoreCase("eal")) spurCode = (code == 6 || code == 7) ? 7 : 14;
        if (lineCode.equalsIgnoreCase("tkl")) spurCode = 131;

        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        h.tvMain.setText(Utils.getStationName(Utils.idToCode(lineCode, mainCode), true));
        h.tvMain.setTag(Utils.idToCode(lineCode, mainCode));
        h.tvSpur.setText(Utils.getStationName(Utils.idToCode(lineCode, spurCode), true));
        h.tvSpur.setTag(Utils.idToCode(lineCode, spurCode));

        View.OnClickListener listener = v -> {
            String stationCode = (String) v.getTag();
            Intent intent = new Intent(v.getContext(), StationActivity.class);
            intent.putExtra("station_code", stationCode);
            v.getContext().startActivity(intent);
        };
        h.tvMain.setOnClickListener(listener);
        h.tvSpur.setOnClickListener(listener);


        List<Trip> upMain = new ArrayList<>(), dnMain = new ArrayList<>();
        List<Trip> upSpur = new ArrayList<>(), dnSpur = new ArrayList<>();
        for (Trip trip : trips) {
            boolean isAtMain = false;
            boolean isAtSpur = false;

            // Next Train
            if (trip.isOpenData) {
                if (trip.ttnt > 0) continue;

                if (trip.nextStationCode == mainCode) {
                    isAtMain = true;
                } else if (trip.nextStationCode == spurCode) {
                    isAtSpur = true;
                }
            }

            if (isAtMain) {
                if (isUp(trip))
                    upMain.add(trip);
                else
                    dnMain.add(trip);
            } else if (isAtSpur) {
                if (isUp(trip))
                    upSpur.add(trip);
                else
                    dnSpur.add(trip);
            }
        }

        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        upSpur.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnSpur.sort(Comparator.comparingDouble(t -> t.targetDistance));

        showTrainBadge(upMain, h.upMain, true);
        showTrainBadge(dnMain, h.dnMain, false);
        showTrainBadge(upSpur, h.upSpur, true);
        showTrainBadge(dnSpur, h.dnSpur, false);
    }

    private void bindParallelLeftStation(ParallelLeftStationViewHolder h, int stationIdx) {
        int code = stationCodes[stationIdx];

        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        h.tvMain.setText(Utils.getStationName(Utils.idToCode(lineCode, code), true));
        h.tvMain.setTag(Utils.idToCode(lineCode, code));
        h.tvMain.setOnClickListener(v -> {
            String stationCode = (String) v.getTag();
            Intent intent = new Intent(v.getContext(), StationActivity.class);
            intent.putExtra("station_code", stationCode);
            v.getContext().startActivity(intent);
        });


        List<Trip> upMain = new ArrayList<>(), dnMain = new ArrayList<>();
        for (Trip trip : trips) {
            // Next Train
            if (trip.isOpenData) {
                if (trip.ttnt > 0) continue;
            }

            if (trip.nextStationCode == code) {
                if (isUp(trip))
                    upMain.add(trip);
                else
                    dnMain.add(trip);
            }
        }

        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnMain.sort(Comparator.comparingDouble(t -> t.targetDistance));

        showTrainBadge(upMain, h.upMain, true);
        showTrainBadge(dnMain, h.dnMain, false);
    }

    private void bindBetween(BetweenViewHolder h, int stationIdx) {
        int currCode = stationCodes[stationIdx];
        int nextCode = stationCodes[stationIdx + 1];

        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        List<Trip> upTrips = new ArrayList<>();
        List<Trip> dnTrips = new ArrayList<>();

        for (Trip trip : trips) {
            if (trip.isOpenData) {
                if (trip.ttnt <= 0) continue;

                boolean isMatch = false;
                if (isUp(trip)) {
                    if (trip.nextStationCode == currCode) isMatch = true;
                } else {
                    if (trip.nextStationCode == nextCode) isMatch = true;
                }

                if (isMatch) {
                    if (isUp(trip))
                        upTrips.add(trip);
                    else
                        dnTrips.add(trip);
                }
            }
        }

        upTrips.sort((t1, t2) -> t1.isOpenData ? Integer.compare(t1.ttnt, t2.ttnt) : Double.compare(t1.targetDistance, t2.targetDistance));
        dnTrips.sort((t1, t2) -> t1.isOpenData ? Integer.compare(t1.ttnt, t2.ttnt) : Double.compare(t1.targetDistance, t2.targetDistance));

        showTrainBadge(upTrips, h.layoutUp, true);
        showTrainBadge(dnTrips, h.layoutDn, false);
    }

    private void bindBranch(BranchViewHolder h, int stationIdx) {
        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        int currCode = stationCodes[stationIdx];
        int nextCode = stationCodes[stationIdx + 1];

        if (currCode == 12 && (nextCode == 13 || nextCode == 14)) {
            h.railLine.setBackgroundResource(R.drawable.rail_branch_split);
        } else if ((nextCode == 6 || nextCode == 7)) {
            h.railLine.setBackgroundResource(R.drawable.rail_branch_split);
        } else {
            h.railLine.setBackgroundResource(R.drawable.rail_branch_merge);
        }


        List<Trip> upMain = new ArrayList<>(), dnMain = new ArrayList<>();
        List<Trip> upSpur = new ArrayList<>(), dnSpur = new ArrayList<>();

        for (Trip trip : trips) {
            boolean isAtThisSegment = false;

            if (trip.isOpenData) {
                if (trip.ttnt <= 0) continue;

                if (isUp(trip)) {
                    if (trip.nextStationCode == currCode)
                        isAtThisSegment = true;
                } else {
                    if (trip.nextStationCode == nextCode)
                        isAtThisSegment = true;
                }
            }


            if (isAtThisSegment) {
                boolean viaRacecourse = lineCode.equalsIgnoreCase("eal") && trip.td.matches(".*[BGKN].*");
                if (!viaRacecourse) {
                    if (isUp(trip))
                        upMain.add(trip);
                    else
                        dnMain.add(trip);
                } else {
                    if (isUp(trip))
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

        showTrainBadge(upMain, h.upMain, true);
        showTrainBadge(dnMain, h.dnMain, false);
        showTrainBadge(upSpur, h.upSpur, true);
        showTrainBadge(dnSpur, h.dnSpur, false);
    }

    private void bindParallelBetween(ParallelBetweenViewHolder h, int stationIdx) {
        int currCode = stationCodes[stationIdx];
        int nextCode = stationCodes[stationIdx + 1];

        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        List<Trip> upMain = new ArrayList<>(), dnMain = new ArrayList<>();

        for (Trip trip : trips) {
            if (trip.isOpenData) {
                if (trip.ttnt <= 0) continue;

                boolean isMatch = false;
                if (isUp(trip)) {
                    if (trip.nextStationCode == currCode) isMatch = true;
                } else {
                    if (trip.nextStationCode == nextCode) isMatch = true;
                }

                if (isMatch) {
                    if (isUp(trip))
                        upMain.add(trip);
                    else
                        dnMain.add(trip);
                }
            }
        }

        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnMain.sort(Comparator.comparingDouble(t -> t.targetDistance));

        showTrainBadge(upMain, h.upMain, true);
        showTrainBadge(dnMain, h.dnMain, false);
    }


    private static class StationViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        MaterialTextView tvStation;
        ViewGroup layoutUp, layoutDn;

        StationViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            tvStation = v.findViewById(R.id.tv_station);
            layoutUp = v.findViewById(R.id.train_up);
            layoutDn = v.findViewById(R.id.train_dn);
        }
    }

    private static class ParallelViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        ViewGroup upMain, dnMain, upSpur, dnSpur;
        MaterialTextView tvMain, tvSpur;

        ParallelViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            tvMain = v.findViewById(R.id.tv_station_main);
            tvSpur = v.findViewById(R.id.tv_station_spur);
            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);
        }
    }

    private static class ParallelLeftStationViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        ViewGroup upMain, dnMain, upSpur, dnSpur;
        MaterialTextView tvMain;

        ParallelLeftStationViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            tvMain = v.findViewById(R.id.tv_station_main);
            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);
        }
    }

    private static class BetweenViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        ViewGroup layoutUp, layoutDn;

        BetweenViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            layoutUp = v.findViewById(R.id.train_up);
            layoutDn = v.findViewById(R.id.train_dn);
        }
    }

    private static class BranchViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        ViewGroup upMain, dnMain, upSpur, dnSpur;

        BranchViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);
        }
    }

    private static class ParallelBetweenViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        ViewGroup upMain, dnMain, upSpur, dnSpur;

        ParallelBetweenViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);
        }
    }
}