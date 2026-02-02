package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
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
            int current = stationCodes[idx];
            int next = stationCodes[idx + 1];

            if (isBranchArea(current, next)) return TYPE_BRANCH;
            return TYPE_BETWEEN;
        }
    }

    private boolean isBranchArea(int s1, int s2) {
        boolean isFotRac = (s1 == 5 && (s2 == 6 || s2 == 7)) || (s2 == 5 && (s1 == 6 || s1 == 7)) ||
                (s1 == 8 && (s2 == 6 || s2 == 7)) || (s2 == 8 && (s1 == 6 || s1 == 7));

        boolean isBorder = (s1 == 12 && (s2 == 13 || s2 == 14)) || (s2 == 12 && (s1 == 13 || s1 == 14));

        return isFotRac || isBorder;
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
            bindStation((StationViewHolder) holder, stationCodes[stationIdx]);
        } else if (holder instanceof ParallelViewHolder) {
            bindParallel((ParallelViewHolder) holder, stationIdx);
        } else if (holder instanceof BranchViewHolder) {
            bindBranch((BranchViewHolder) holder, stationIdx);
        } else {
            bindBetween((BetweenViewHolder) holder, stationIdx);
        }
    }

    private void bindStation(StationViewHolder h, int currentStation) {
        h.tvStationName.setText(Utils.getStationName(context, Utils.mapStation(currentStation, "EAL"), true));

        List<Trip> upTrips = new ArrayList<>();
        List<Trip> dnTrips = new ArrayList<>();

        for (Trip trip : trips.reversed()) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;
            if (trip.trainSpeed == 0 && trip.currentStationCode == currentStation) {
                if (isUp(trip.td)) upTrips.add(trip);
                else dnTrips.add(trip);
            }
        }

        updateTrainUI(upTrips, h.layoutUp, true);
        updateTrainUI(dnTrips, h.layoutDn, false);
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

        for (Trip trip : trips.reversed()) {
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
        updateTrainUI(dnMain, h.dnMain, false);
        updateTrainUI(upSpur, h.upSpur, true);
        updateTrainUI(dnSpur, h.dnSpur, false);
    }

    private void bindParallel(ParallelViewHolder h, int stationIdx) {
        int code = stationCodes[stationIdx];
        int mainCode = (code == 6 || code == 7) ? 6 : 13;
        int spurCode = (code == 6 || code == 7) ? 7 : 14;

        h.tvMain.setText(Utils.getStationName(context, Utils.mapStation(mainCode, "EAL"), true));
        h.tvSpur.setText(Utils.getStationName(context, Utils.mapStation(spurCode, "EAL"), true));

        List<Trip> upM = new ArrayList<>(), dnM = new ArrayList<>();
        List<Trip> upS = new ArrayList<>(), dnS = new ArrayList<>();

        for (Trip trip : trips.reversed()) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;
            if (trip.trainSpeed == 0) {
                if (trip.currentStationCode == mainCode) {
                    if (isUp(trip.td)) upM.add(trip);
                    else dnM.add(trip);
                }
                if (trip.currentStationCode == spurCode) {
                    if (isUp(trip.td)) upS.add(trip);
                    else dnS.add(trip);
                }
            }
        }
        updateTrainUI(upM, h.upMain, true);
        updateTrainUI(dnM, h.dnMain, false);
        updateTrainUI(upS, h.upSpur, true);
        updateTrainUI(dnS, h.dnSpur, false);
    }

    private void bindBetween(BetweenViewHolder h, int stationIdx) {
        int currentStation = stationCodes[stationIdx];
        int nextStation = stationCodes[stationIdx + 1];

        List<Trip> upTrips = new ArrayList<>();
        List<Trip> dnTrips = new ArrayList<>();

        for (Trip trip : trips.reversed()) {
            if (System.currentTimeMillis() / 1000 - trip.receivedTime / 1000 > 60) continue;
            if (trip.trainSpeed > 0) {
                boolean isUp = isUp(trip.td);
                if (isUp && trip.nextStationCode == currentStation) upTrips.add(trip);
                else if (!isUp && trip.nextStationCode == nextStation) dnTrips.add(trip);
            }
        }
        updateTrainUI(upTrips, h.layoutUp, true);
        updateTrainUI(dnTrips, h.layoutDn, false);
    }

    private boolean isUp(String td) {
        return Character.getNumericValue(td.charAt(td.length() - 1)) % 2 != 0;
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

            if (container instanceof android.widget.FrameLayout) {
                android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) badge.getLayoutParams();
                params.gravity = isUp ? (android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END)
                        : (android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
                badge.setLayoutParams(params);
            }

            float offset = i * 35f;
            badge.setTranslationX(isUp ? -offset : offset);

            badge.setElevation((tripsAtLocation.size() - i) * 5f);

            badge.setOnClickListener(v -> {
                showTrainsDetailDialog(tripsAtLocation);
            });

            container.addView(badge);
        }
    }

    private void showTrainsDetailDialog(List<Trip> tripsAtLocation) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bottom_sheet_container, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rv_trains_list);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new RecyclerView.Adapter<>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_train_detail_card, parent, false)) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                Trip trip = tripsAtLocation.get(position);
                View view = holder.itemView;
                float density = context.getResources().getDisplayMetrics().density;

                TextView tvLine = view.findViewById(R.id.tv_line_name);
                tvLine.setBackgroundColor(Color.parseColor(Utils.getColor(context, "EAL")));
                tvLine.setText("東鐵綫");

                TextView tvType = view.findViewById(R.id.tv_service_type);
                GradientDrawable typeBg = new GradientDrawable();
                typeBg.setCornerRadius(10f);
                boolean viaRacecourse = trip.td.matches(".*[BGKN].*");
                if (viaRacecourse) {
                    tvType.setText("經馬場");
                    typeBg.setColor(0xCD5DE2FF);
                } else {
                    tvType.setText("普通");
                    typeBg.setColor(0xFF4CAF50);
                }
                tvType.setBackground(typeBg);

                TextView tvCarCount = view.findViewById(R.id.tv_car_count);
                GradientDrawable carCountBg = new GradientDrawable();
                carCountBg.setCornerRadius(10f);
                carCountBg.setColor(0xFF444444);
                tvCarCount.setText("9両");
                tvCarCount.setBackground(carCountBg);

                ((TextView) view.findViewById(R.id.tv_train_number)).setText("列車編號：" + trip.td);

                String dest = trip.destinationStationCode == -1 ? "不載客列車" :
                        Utils.getStationName(context, Utils.mapStation(trip.destinationStationCode, "EAL"), true) + " 行";
                ((TextView) view.findViewById(R.id.tv_destination)).setText(dest);

                TextView tvStatus = view.findViewById(R.id.tv_delay_status);
                tvStatus.setText(trip.trainSpeed == 0 ? "站內停車中" : "準時運行");
                GradientDrawable statusBg = new GradientDrawable();
                statusBg.setCornerRadius(100f);
                statusBg.setColor(trip.trainSpeed == 0 ? 0xFF757575 : 0xFF4CAF50);

                tvStatus.setBackground(statusBg);

                LinearLayout container = view.findViewById(R.id.layout_cars_container);
                container.removeAllViews();
                int totalLoad = 0;

                for (int i = 0; i < trip.listCars.size(); i++) {
                    int idx = isUp(trip.td) ? i : (trip.listCars.size() - 1 - i);
                    Car car = trip.listCars.get(idx);
                    totalLoad += car.passengerCount;

                    View carView = new View(context);
                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams((int) (24 * density), (int) (26 * density));
                    p.setMargins((int) (2 * density), 0, (int) (2 * density), 0);
                    carView.setLayoutParams(p);

                    GradientDrawable gd = new GradientDrawable();
                    gd.setCornerRadius(4 * density);

                    int count = car.passengerCount;
                    boolean isFirst = (idx == 3);
                    int color = (isFirst ? (count < 70 ? 0xFF00FF00 : count < 150 ? 0xFFFFFF00 : 0xFFFF0000) :
                            (count < 110 ? 0xFF00FF00 : count < 250 ? 0xFFFFFF00 : 0xFFFF0000));
                    gd.setColor(color);
                    if (isFirst) gd.setStroke((int) (3 * density), 0xFFFFA500);

                    carView.setBackground(gd);
                    container.addView(carView);
                }

                int avg = totalLoad / trip.listCars.size();
                ((TextView) view.findViewById(R.id.tv_crowd_level_text))
                        .setText(avg < 100 ? "座位有空" : avg < 200 ? "稍微擁擠" : "非常擁擠");
            }

            @Override
            public int getItemCount() {
                return tripsAtLocation.size();
            }
        });

        dialog.setContentView(dialogView);

        View btnClose = dialogView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        View bs = (View) dialogView.getParent();
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bs);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bs.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        behavior.setSkipCollapsed(true);

        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onSlide(@NonNull View view, float offset) {
                if (offset > 0.5f && behavior.getState() == BottomSheetBehavior.STATE_SETTLING)
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

            @Override
            public void onStateChanged(@NonNull View view, int state) {
                if (state == BottomSheetBehavior.STATE_HIDDEN) dialog.dismiss();
                if (state == BottomSheetBehavior.STATE_SETTLING && view.getTop() < view.getHeight() * 0.1)
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
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