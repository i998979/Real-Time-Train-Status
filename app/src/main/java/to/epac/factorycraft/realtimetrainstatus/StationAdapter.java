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

    private List<Integer> stationIds = new ArrayList<>();
    private List<String> stationNames = new ArrayList<>();
    private OnStationSelectedListener listener;

    public interface OnStationSelectedListener {
        void onStationSelected(int stationId, String stationName);
    }

    public StationAdapter(OnStationSelectedListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Integer> ids, List<String> names) {
        this.stationIds = new ArrayList<>(ids);
        this.stationNames = new ArrayList<>(names);

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
        String name = stationNames.get(position);
        int id = stationIds.get(position);

        holder.textView.setText(name);

        holder.itemView.setOnClickListener(v -> {
            listener.onStationSelected(id, name);
        });
    }

    @Override
    public int getItemCount() {
        return stationNames.size();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_station_name);
        }
    }
}