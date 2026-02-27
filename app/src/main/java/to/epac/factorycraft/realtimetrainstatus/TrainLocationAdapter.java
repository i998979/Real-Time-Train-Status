package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import java.util.Map;

public class TrainLocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_STATION = 0;
    private static final int TYPE_BETWEEN = 1;
    private static final int TYPE_PARALLEL = 2;
    private static final int TYPE_BRANCH = 3;

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
            View trainIconView = badge.findViewById(isUp ? R.id.layout_train_up : R.id.layout_train_dn);

            LayerDrawable layers = (LayerDrawable) trainIconView.getBackground().mutate();
            Drawable headerLayer = layers.findDrawableByLayerId(R.id.line_color_layer);
            headerLayer.setTint(lineColor);
            trainIconView.setBackground(layers);

            if (lineCode.equalsIgnoreCase("tml")) {
                ((ImageView) badge.findViewById(R.id.img_train_icon)).setImageResource(R.drawable.sp1900);
                try {
                    if (Integer.parseInt(trip.trainId) >= 397)
                        ((ImageView) badge.findViewById(R.id.img_train_icon)).setImageResource(R.drawable.t1141a);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }


            TextView tvId = badge.findViewById(isUp ? R.id.tv_train_id_up : R.id.tv_train_id_dn);
            if (trip.destinationStationCode == -1 || trip.destinationStationCode == 91 || trip.destinationStationCode == 92) {
                tvId.setText("不載客");
            } else {
                String destName = Utils.getStationName(context, Utils.idToCode(context, trip.destinationStationCode, lineCode), true);
                boolean viaRacecourse = lineCode.equalsIgnoreCase("eal") && trip.td.matches(".*[BGKN].*");
                tvId.setText((viaRacecourse ? "經馬場" : "普通") + " " + destName);
            }

            TextView tvCar = badge.findViewById(isUp ? R.id.tv_car_up : R.id.tv_car_dn);
            tvCar.setText(lineCode.equalsIgnoreCase("eal") ? "9両"
                    : lineCode.equalsIgnoreCase("drl") ? "4両"
                    : lineCode.equalsIgnoreCase("sil") ? "3両" : "8両");

            updateBadgeCrowd(badge, trip);

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

    private void updateBadgeCrowd(View badgeView, Trip trip) {
        double totalPercent = 0;
        int carCount = trip.listCars.size();

        for (int i = 0; i < carCount; i++) {
            Car car = trip.listCars.get(i);
            int capacity = (lineCode.equalsIgnoreCase("eal") && i == 3) ? 150 : 250;
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
        tvArrvTime.setTextColor(0xFF4CAF50);

        tvStaName.setText(Utils.getStationName(context, Utils.idToCode(context, stationCode, lineCode), true));

        if (isLast) {
            bottomHalf.setVisibility(View.INVISIBLE);
        } else {
            bottomHalf.setVisibility(View.VISIBLE);
        }
        container.addView(row);
    }

    private void updateTrainCrowd(LinearLayout layout, Trip trip, View itemView) {
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
                if (lineCode.equalsIgnoreCase("eal")) {
                    trainIcon.setImageResource(R.drawable.r_train);
                } else if (lineCode.equalsIgnoreCase("tml")) {
                    trainIcon.setImageResource(R.drawable.sp1900);
                    try {
                        if (Integer.parseInt(trip.trainId) >= 397)
                            trainIcon.setImageResource(R.drawable.t1141a);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                TextView tvDest = v.findViewById(R.id.tv_destination);
                if (trip.destinationStationCode == -1 || trip.destinationStationCode == 91 || trip.destinationStationCode == 92) {
                    tvDest.setText("不載客列車");
                } else {
                    String destName = Utils.getStationName(context, Utils.idToCode(context, trip.destinationStationCode, lineCode), true);
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
                typeBg.setColor(viaRacecourse ? 0xCD5DE2FF : 0xFF4CAF50);
                tvSvcType.setBackground(typeBg);

                if (trip.isOpenData) {
                    v.findViewById(R.id.tv_train_consist).setVisibility(View.GONE);
                    v.findViewById(R.id.tv_train_number).setVisibility(View.GONE);
                } else {
                    if (lineCode.equalsIgnoreCase("eal")) {
                        int ts = Integer.parseInt(trip.trainId);
                        ((TextView) v.findViewById(R.id.tv_train_consist)).setText(String.format("D%03d/D%03d", ts - 2, ts));
                    } else if (lineCode.equalsIgnoreCase("tml")) {
                        int ts = Integer.parseInt(trip.trainId);
                        if (ts % 2 == 0)
                            ((TextView) v.findViewById(R.id.tv_train_consist)).setText(String.format("D%03d/D%03d", ts - 1, ts));
                        else
                            ((TextView) v.findViewById(R.id.tv_train_consist)).setText(String.format("D%03d/D%03d", ts, ts + 1));
                    }
                    ((TextView) v.findViewById(R.id.tv_train_number)).setText("列車編號：" + trip.td);
                }
                LinearLayout crowdLayout = v.findViewById(R.id.crowd_layout);
                updateTrainCrowd(crowdLayout, trip, v);


                View header = v.findViewById(R.id.station_header);
                ImageView arrow = v.findViewById(R.id.fold_arrow);

                LinearLayout stationRows = v.findViewById(R.id.station_rows);
                View timeLine = v.findViewById(R.id.station_timeline);

                // 將預測時間按時間排序 (由近到遠)
                List<Map.Entry<Integer, Integer>> sortedPredictions = new ArrayList<>(trip.stationPredictions.entrySet());
                sortedPredictions.sort(Map.Entry.comparingByValue());

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

        if (lineCode.equalsIgnoreCase("eal")) {
            char lastChar = trip.td.charAt(trip.td.length() - 1);
            if (Character.isDigit(lastChar)) {
                return (lastChar - '0') % 2 != 0;
            }
        }

        return false;
    }

    private boolean isOutdated(Trip trip) {
        if (!trip.isOpenData) return (System.currentTimeMillis() - trip.receivedTime) > 60000;

        return false;
    }

    private void bindStation(StationViewHolder h, int stationIdx) {
        int code = stationCodes[stationIdx];
        /*FrameLayout container = (FrameLayout) h.railLine.getParent();

        // 1. 移除舊有的轉乘線以利 ViewHolder 重用
        View oldInterchange = container.findViewWithTag("dynamic_interchange");
        if (oldInterchange != null) container.removeView(oldInterchange);

        // 2. 判斷轉乘顏色（紅磡站）
        int interchangeColor = -1;
        if (lineCode.equalsIgnoreCase("eal") && code == 21) {
            interchangeColor = Color.parseColor("#9A3820"); // 屯馬綫棕色
        }

        // 3. 動態插入與精確對齊
        if (interchangeColor != -1) {
            float density = context.getResources().getDisplayMetrics().density;

            ImageView iv = new ImageView(context);
            iv.setTag("dynamic_interchange");

            Drawable curve = ContextCompat.getDrawable(context, R.drawable.interchange_tml_up).mutate();
            curve.setTint(interchangeColor);
            iv.setImageDrawable(curve);

            // 寬度設為 320dp 以確保水平長度足夠
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    (int) (320 * density),
                    (int) (100 * density)
            );

            // 關鍵對齊步驟：
            // Gravity.END 讓 ImageView 的右側貼齊容器右側
            // rightMargin = 10dp 讓 ImageView 向左移 10dp，使其右邊界剛好落在 20dp 容器的中心線 (即主線中心)
            params.gravity = Gravity.END;
            params.rightMargin = (int) (10 * density);

            // 插入到 index 0，確保它在主線 rail_line 和車站圓點的下方
            container.addView(iv, 0, params);
        }*/

        h.tvStation.setText(Utils.getStationName(context, Utils.idToCode(context, code, lineCode), true));
        h.tvStation.setTag(Utils.idToCode(context, code, lineCode));

        View.OnClickListener listener = v -> {
            String stationCode = (String) v.getTag();

            Intent intent = new Intent(v.getContext(), StationActivity.class);
            intent.putExtra("station_code", stationCode);
            v.getContext().startActivity(intent);
        };
        h.tvStation.setOnClickListener(listener);

        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

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
            // Roctec
            else {
                if (isOutdated(trip)) continue;
                if (trip.trainSpeed != 0) continue;

                if (trip.currentStationCode == code) {
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

    private void bindBetween(BetweenViewHolder h, int stationIdx) {
        int currentCode = stationCodes[stationIdx];
        int nextNodeCode = stationCodes[stationIdx + 1];

        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        List<Trip> upTrips = new ArrayList<>();
        List<Trip> dnTrips = new ArrayList<>();

        for (Trip trip : trips) {
            if (trip.isOpenData) {
                if (trip.ttnt <= 0) continue;

                boolean isMatch = false;
                if (isUp(trip)) {
                    if (trip.nextStationCode == currentCode) isMatch = true;
                } else {
                    if (trip.nextStationCode == nextNodeCode) isMatch = true;
                }

                if (isMatch) {
                    if (isUp(trip))
                        upTrips.add(trip);
                    else
                        dnTrips.add(trip);
                }
            } else {
                if (isOutdated(trip)) continue;
                if (!(trip.trainSpeed > 0)) continue;

                if (isUp(trip) && trip.nextStationCode == currentCode && trip.currentStationCode == nextNodeCode)
                    upTrips.add(trip);
                else if (!isUp(trip) && trip.nextStationCode == nextNodeCode && trip.currentStationCode == currentCode)
                    dnTrips.add(trip);
            }
        }

        upTrips.sort((t1, t2) -> t1.isOpenData ? Integer.compare(t1.ttnt, t2.ttnt) : Double.compare(t1.targetDistance, t2.targetDistance));
        dnTrips.sort((t1, t2) -> t1.isOpenData ? Integer.compare(t1.ttnt, t2.ttnt) : Double.compare(t1.targetDistance, t2.targetDistance));

        showTrainBadge(upTrips, h.layoutUp, true);
        showTrainBadge(dnTrips, h.layoutDn, false);
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
            boolean isAtThisSegment = false;

            if (trip.isOpenData) {
                if (trip.ttnt <= 0) continue;

                if (isUp(trip)) {
                    // UP (北行往羅湖/落馬洲): 進入分叉前，目標是 currentSector
                    if (trip.nextStationCode == currentSector)
                        isAtThisSegment = true;
                } else {
                    // DN (南行往金鐘): 進入匯合區間，目標應該是 nextSector (南方的車站)
                    if (trip.nextStationCode == nextSector)
                        isAtThisSegment = true;
                }
            } else {
                if (isOutdated(trip)) continue;
                if (!(trip.trainSpeed > 0)) continue;

                if (isUp(trip)) {
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

    private void bindParallel(ParallelViewHolder h, int stationIdx) {
        int code = stationCodes[stationIdx];
        int mainCode = (code == 6 || code == 7) ? 6 : 13;
        int spurCode = (code == 6 || code == 7) ? 7 : 14;

        h.railLine.setBackgroundTintList(ColorStateList.valueOf(lineColor));
        h.railLine2.setBackgroundTintList(ColorStateList.valueOf(lineColor));

        h.tvMain.setText(Utils.getStationName(context, Utils.idToCode(context, mainCode, lineCode), true));
        h.tvMain.setTag(Utils.idToCode(context, mainCode, lineCode));
        h.tvSpur.setText(Utils.getStationName(context, Utils.idToCode(context, spurCode, lineCode), true));
        h.tvSpur.setTag(Utils.idToCode(context, mainCode, lineCode));

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
            // Roctec
            else {
                if (isOutdated(trip)) continue;
                if (trip.trainSpeed != 0) continue;

                if (trip.currentStationCode == mainCode) {
                    isAtMain = true;
                } else if (trip.currentStationCode == spurCode) {
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

        // TODO: Sort by ttnt for isOpenData, by targetDistance for not isOpenData
        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        upMain.sort(Comparator.comparingDouble(t -> t.targetDistance));
        upSpur.sort(Comparator.comparingDouble(t -> t.targetDistance));
        dnSpur.sort(Comparator.comparingDouble(t -> t.targetDistance));

        showTrainBadge(upMain, h.upMain, true);
        showTrainBadge(dnMain, h.dnMain, false);
        showTrainBadge(upSpur, h.upSpur, true);
        showTrainBadge(dnSpur, h.dnSpur, false);
    }


    private static class StationViewHolder extends RecyclerView.ViewHolder {
        View railLine;
        MaterialTextView tvStation;
        ViewGroup layoutUp, layoutDn;

        StationViewHolder(View v) {
            super(v);
            railLine = v.findViewById(R.id.rail_line);
            tvStation = v.findViewById(R.id.tv_station);
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
        MaterialTextView tvMain, tvSpur;

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