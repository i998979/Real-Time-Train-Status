package to.epac.factorycraft.realtimetrainstatus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.ViewHolder> {

    private List<Integer> ids = new ArrayList<>();
    private List<String> names = new ArrayList<>();
    private List<String> codes = new ArrayList<>();

    private final OnStationClickListener listener;

    public interface OnStationClickListener {
        void onStationClick(int id, String name, String code);
    }

    public StationAdapter(OnStationClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Integer> newIds, List<String> newNames, List<String> newCodes) {
        this.ids = newIds;
        this.names = newNames;
        this.codes = newCodes;

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int id = ids.get(position);
        String name = names.get(position);
        String code = codes.get(position);

        holder.tvStation.setText(name);
        holder.itemView.setOnClickListener(v -> {
            listener.onStationClick(id, name, code);
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStation;

        private ViewHolder(View itemView) {
            super(itemView);
            tvStation = itemView.findViewById(R.id.tv_station);
        }
    }
}