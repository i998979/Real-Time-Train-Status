package to.epac.factorycraft.realtimetrainstatus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        resetUI(h.layoutUp, h.layoutDn);

        for (Trip trip : trips) {
            if (trip.trainSpeed == 0 && trip.currentStationCode == currentStation)
                updateTrainUI(trip, h.layoutUp, h.layoutDn, h.tvIdUp, h.tvIdDn);
        }
    }

    private void bindBranch(BranchViewHolder h, int stationIdx) {
        resetUI(h.upMain, h.dnMain, h.upSpur, h.dnSpur);
        int currentStation = stationCodes[stationIdx];
        int nextStation = stationCodes[stationIdx + 1];

        if (currentStation == 12 && (nextStation == 13 || nextStation == 14))
            h.imgRail.setImageResource(R.drawable.rail_branch_split);
        else if (nextStation == 6 || nextStation == 7)
            h.imgRail.setImageResource(R.drawable.rail_branch_split);
        else
            h.imgRail.setImageResource(R.drawable.rail_branch_merge);

        for (Trip trip : trips) {
            if (trip.trainSpeed > 0) {
                boolean isUp = isUp(trip.td);

                boolean isAtThisSegment = (isUp && trip.nextStationCode == currentStation)
                        || (!isUp && trip.nextStationCode == nextStation);

                if (isAtThisSegment) {
                    if (trip.currentStationCode == 6 || trip.nextStationCode == 6
                            || trip.currentStationCode == 13 || trip.nextStationCode == 13)
                        updateTrainUI(trip, h.upMain, h.dnMain, h.idUpMain, h.idDnMain);
                    if (trip.currentStationCode == 7 || trip.nextStationCode == 7
                            || trip.currentStationCode == 14 || trip.nextStationCode == 14)
                        updateTrainUI(trip, h.upSpur, h.dnSpur, h.idUpSpur, h.idDnSpur);
                }
            }
        }
    }

    private void bindParallel(ParallelViewHolder h, int stationIdx) {
        resetUI(h.upMain, h.dnMain, h.upSpur, h.dnSpur);

        int code = stationCodes[stationIdx];

        if (code == 6 || code == 7) {
            h.tvMain.setText(Utils.getStationName(context, Utils.mapStation(6, "EAL"), true));
            h.tvSpur.setText(Utils.getStationName(context, Utils.mapStation(7, "EAL"), true));

            for (Trip trip : trips) {
                if (trip.trainSpeed == 0) {
                    if (trip.currentStationCode == 6)
                        updateTrainUI(trip, h.upMain, h.dnMain, h.idUpMain, h.idDnMain);
                    if (trip.currentStationCode == 7)
                        updateTrainUI(trip, h.upSpur, h.dnSpur, h.idUpSpur, h.idDnSpur);
                }
            }
        } else if (code == 13 || code == 14) {
            h.tvMain.setText(Utils.getStationName(context, Utils.mapStation(13, "EAL"), true));
            h.tvSpur.setText(Utils.getStationName(context, Utils.mapStation(14, "EAL"), true));

            for (Trip trip : trips) {
                if (trip.trainSpeed == 0) {
                    if (trip.currentStationCode == 13)
                        updateTrainUI(trip, h.upMain, h.dnMain, h.idUpMain, h.idDnMain);
                    if (trip.currentStationCode == 14)
                        updateTrainUI(trip, h.upSpur, h.dnSpur, h.idUpSpur, h.idDnSpur);
                }
            }
        }
    }

    private void bindBetween(BetweenViewHolder h, int stationIdx) {
        resetUI(h.layoutUp, h.layoutDn);

        int currentStation = stationCodes[stationIdx];
        int nextStation = stationCodes[stationIdx + 1];

        for (Trip trip : trips) {
            if (trip.trainSpeed > 0) {
                boolean isUp = isUp(trip.td);
                if (isUp && trip.nextStationCode == currentStation)
                    updateTrainUI(trip, h.layoutUp, h.layoutDn, h.tvIdUp, h.tvIdDn);
                else if (!isUp && trip.nextStationCode == nextStation)
                    updateTrainUI(trip, h.layoutUp, h.layoutDn, h.tvIdUp, h.tvIdDn);
            }
        }
    }

    private boolean isUp(String td) {
        return Character.getNumericValue(td.charAt(td.length() - 1)) % 2 != 0;
    }

    private void updateTrainUI(Trip trip, View up, View dn, TextView tUp, TextView tDn) {
        if (isUp(trip.td)) {
            up.setVisibility(View.VISIBLE);
            tUp.setText(trip.td);
        } else {
            dn.setVisibility(View.VISIBLE);
            tDn.setText(trip.td);
        }
    }

    private void resetUI(View... views) {
        for (View v : views) if (v != null) v.setVisibility(View.INVISIBLE);
    }


    private static class StationViewHolder extends RecyclerView.ViewHolder {
        TextView tvStationName, tvIdUp, tvIdDn;
        LinearLayout layoutUp, layoutDn;

        StationViewHolder(View v) {
            super(v);

            tvStationName = v.findViewById(R.id.tv_station_name);
            layoutUp = v.findViewById(R.id.layout_train_up);
            layoutDn = v.findViewById(R.id.layout_train_dn);
            tvIdUp = v.findViewById(R.id.tv_train_id_up);
            tvIdDn = v.findViewById(R.id.tv_train_id_dn);
        }
    }

    private static class BetweenViewHolder extends RecyclerView.ViewHolder {
        TextView tvIdUp, tvIdDn;
        LinearLayout layoutUp, layoutDn;

        BetweenViewHolder(View v) {
            super(v);

            layoutUp = v.findViewById(R.id.layout_train_up);
            layoutDn = v.findViewById(R.id.layout_train_dn);
            tvIdUp = v.findViewById(R.id.tv_train_id_up);
            tvIdDn = v.findViewById(R.id.tv_train_id_dn);
        }
    }

    private static class BranchViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRail;
        View upMain, dnMain, upSpur, dnSpur;
        TextView idUpMain, idDnMain, idUpSpur, idDnSpur;

        BranchViewHolder(View v) {
            super(v);

            imgRail = v.findViewById(R.id.img_branch_rail);

            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);

            idUpMain = upMain.findViewById(R.id.tv_train_id_up);
            idDnMain = dnMain.findViewById(R.id.tv_train_id_dn);
            idUpSpur = upSpur.findViewById(R.id.tv_train_id_up);
            idDnSpur = dnSpur.findViewById(R.id.tv_train_id_dn);
        }
    }

    private static class ParallelViewHolder extends RecyclerView.ViewHolder {
        View upMain, dnMain, upSpur, dnSpur;
        TextView tvMain, tvSpur;
        TextView idUpMain, idDnMain, idUpSpur, idDnSpur;

        ParallelViewHolder(View v) {
            super(v);

            tvMain = v.findViewById(R.id.tv_station_main);
            tvSpur = v.findViewById(R.id.tv_station_spur);

            upMain = v.findViewById(R.id.train_up_main);
            dnMain = v.findViewById(R.id.train_dn_main);
            upSpur = v.findViewById(R.id.train_up_spur);
            dnSpur = v.findViewById(R.id.train_dn_spur);

            idUpMain = upMain.findViewById(R.id.tv_train_id_up);
            idDnMain = dnMain.findViewById(R.id.tv_train_id_dn);
            idUpSpur = upSpur.findViewById(R.id.tv_train_id_up);
            idDnSpur = dnSpur.findViewById(R.id.tv_train_id_dn);
        }
    }
}